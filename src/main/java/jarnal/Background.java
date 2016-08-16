package jarnal;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.BufferedImage;

//To compile without jpedal
//comment out the next three lines
//remove or comment out the JbgsPdf class at the bottom of the file
//uncomment the dummy JbgsPdf class immediately above it
import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;

/**
 * This class represents the source of a backround image sequence.
 * Mainly, it corresponds to an InputStream.
 * For undo operations, an identifier can be requested which may
 * support a reconstruction of the image sequence.
 */
class JbgsSource {
	private String bgfname = "";
	private byte bgstream[] = null;
	private boolean repeating = false;

	/**
	 * Create a background image sequence source.
	 * Normally, the file name is interpreted as the
	 * name of a local file and the image sequence is read from that
	 * file. If Jarnal.getInstance().isApplet, the byte array is used instead. If it
	 * is null, the file name is interpreted as an URL and the Information
	 * is read from the server into the byte array.
	 * @param fname the name of the file to read background information
	 * from.
	 * @param arr the byte array to use as alternative source for
	 * background information in the applet case.
	 */
	JbgsSource(String fname, byte[] arr) {
		bgfname = fname;
		if("".equals(fname)) return;
		try{
			if(Jarnal.getInstance().isApplet){
				if(arr == null){
					HtmlPost hp = new HtmlPost(fname, null, null, null, null, false);
					bgstream = hp.pipeBytes();
					if(bgstream == null) throw(new IOException());
				}
				else bgstream = arr;
			}	
		}
		catch(Exception ex){ex.printStackTrace();}
	}

	void reset(String fname){
		bgfname = fname;
	}

	byte [] getBuffer() { return bgstream; }

	/**
	 * Get an identifier for recreating this JbgsSource.
	 * The file name used when creating this JbgsSource is returned. If the
	 * background information was provided by the byte array, the file name
	 * is not sufficient for recreating this JbgsSource.
	 * @return the file name for recreating this JbgsSource.
	 */
	String getName() {
		if (bgfname == null) return "none";
		else return bgfname;
	}

	String createHandle(){
		File fpath = new File(getName());
		return "background" + (new Random()).nextInt() + "." + fpath.getName();
	}

	/**
	 * Get an InputStream for reading the content.
	 * @return an InputStream to read the background image
	 * information from this JbgsSource.
	 */
	InputStream getInputStream() {
		if ("".equals(bgfname)) return null;
		try {
			if (bgstream != null) return new ByteArrayInputStream(bgstream);
			else return new FileInputStream(bgfname);
		}
		catch(IOException ex){ex.printStackTrace(); return null;}
	}
}

/**
 * This class represents a sequence of background images.
 * The sequence is indexed beginning with 0.
 * Additionally, the original source of the image sequence is
 * available as a JbgsSource.
 */
abstract class Background {
	public static boolean cacheOn = true;
	public static LinkedList globalbgCache = new LinkedList();
	public static int globalbgCacheLimit = -1;
	public JbgsSource src = null;
	public boolean isRepeating = false;
	public int astate = -1; //1 is active, 0 is inactive
	public boolean scanstate = false; //for scanning to find inactive backgrounds
	public boolean isPdf = false;
	public float internalScale = 1.0f;
	static public boolean useGS = false;
	static public boolean silentGS = false;

	Background(JbgsSource s) { src = s; }

	/**
	 * Create a new Jbgs for a given source.
	 * The source is analyzed and a Jbgs according to its content is
	 * returned.
	 * @param s the background image source
	 * @return a Jbgs for s.
	 */
	static Background create(JbgsSource s) {
		InputStream h = s.getInputStream();
		if (h == null) return new JbgsDefault(s);
		boolean ispdf = false;
		byte[] barr = new byte[4];
		try {
			if (h.read(barr) == 4)
				if ("%PDF".equals(new String(barr)))
					ispdf = true;
		}
		catch(IOException ex){ex.printStackTrace();}
		if (ispdf) {
			if (!Pages.jpedalAvailable()) {
				System.out.println
					("Jpedal not available - cannot display PDF background");
				return new JbgsDefault(s);
			}
			else {
				//LogWriter.setupLogFile(true,1,"","v",false);
				//LogWriter.log_name =  "jpedal-log.txt";
				return new JbgsPdf(s);
			}
		}
		else {
			Background jd = new JbgsDefault(s);
			((JbgsDefault) jd).noReaderMsg = false;
			jd.make();
			((JbgsDefault) jd).noReaderMsg = true;
			String src = s.getName();
			if(!((JbgsDefault) jd).bgLoad) {
				int ev = 0;
				try{
					Runtime rt = Runtime.getRuntime();
					String est = Jarnal.getInstance().pdfconverter;
					String trg = src + ".pdf";
					String estev[] = new String[3];
					estev[0] = Jarnal.getInstance().pdfconverter;
					estev[1] = "-invisible";
					estev[2] = "macro:///Standard.Convert.ConvertToPdf(" + src + "," + trg +")";
					Process ps = rt.exec(estev);
					java.util.Timer jst = new java.util.Timer();
					jst.schedule(new waitTimer(ps), 30000);
					//InputStream is = ps.getInputStream();
					//InputStreamReader isr = new InputStreamReader(is);
            				//BufferedReader br = new BufferedReader(isr);
					//String line;
					//while ((line = br.readLine()) != null) System.out.println(line);
					ps.waitFor();
					jst.cancel();
					ev = ps.exitValue();
					s.reset(trg);
					h = s.getInputStream();
					if (h == null) return new JbgsDefault(s);
					ispdf = false;
					barr = new byte[4];
					try {
						if (h.read(barr) == 4)
						if ("%PDF".equals(new String(barr)))
						ispdf = true;
					}
					catch(IOException ex){ex.printStackTrace();}
					if(ispdf) jd = new JbgsPdf(s);
					else ev = 1;
				}
				catch(Exception ex){ex.printStackTrace(); ev = 1;}
				if(ev !=0) {
					jd = new JbgsDefault(s);
					Jarnal.getInstance().getJrnlTimerListener().setMessage("No background reader found for: " + src, "Problem Loading Background");
				}
			}
			return jd;
		}
	}

	void notifyAddImage(int i){
		if(globalbgCache.size() > 0){
			int jj = -1;
			for(int ii = 0; ii < globalbgCache.size(); ii++){
				memoryMan mm = (memoryMan) globalbgCache.get(ii);
				if((mm.bg == this) && (mm.pg == i)) jj = ii;
			}
			if(jj > -1) {
				globalbgCache.remove(jj);
				globalbgCache.add(new memoryMan(null, this, i));
				return;
			}
		}
		memoryMan.testMem();
		boolean constrained = true;
		if(globalbgCacheLimit == -1) constrained = false;
		if(constrained && globalbgCache.size() < globalbgCacheLimit) constrained = false;			
		if(constrained && (globalbgCache.size() > 0)){
			memoryMan mm = (memoryMan) globalbgCache.get(0);
			mm.bg.removeCachedImage(mm.pg);
			globalbgCache.remove(0);
		}
		globalbgCache.add(new memoryMan(null, this, i));
	}

	void removeAllCache(){
		ListIterator iter = globalbgCache.listIterator();
      		while (iter.hasNext()){
			memoryMan mm = (memoryMan) iter.next();
			if(mm.bg == this) iter.remove();
		}
	}

	abstract void removeCachedImage(int i);

	/**
	 * Get the source of this Jbgs.
	 */
	JbgsSource getSource() { return src; }

	/**
	 * The size of this sequence.
	 * @return the number of background images in this sequence.
	 */
	int size(){return 0;}

	/**
	 * Get the image for a given index.
	 * @param i the index
	 * @param s the scaling factor. 1.0 means natural size.
	 * @return an Image representing the i-th image in the
	 * sequence, scaled by s. If i is larger or equal to the size of
	 * this sequence, or if it is negative, the result is null. 
	 */
	abstract Image getScaledBg(int i, float s, int bq, int bgfade, Color cc);

	abstract void make();
	abstract void unmake();
	abstract void clearCache();
	
	int getHeight(int i){return -1;}
	int getWidth(int i){return -1;}
	void setOutline(Out outline, String bghandle){}

	public String getInfo(){
		String ans = "File: " + getSource().getName();
		ans = ans + "\nNo Information Available";
		return ans;
	}

	public boolean haveBg(int i){
		i = getPage(i);
		if(i >= size()) return false;
		if(i < 0) return false;
		return true;
	}

	public void initRep(){
		if(size() == 1) isRepeating = true;
	}

	public int getPage(int i){
		if(!isRepeating) return i;
		if(size() == 0) return i;
		int ans = i % size();
		return ans;
	}

	public String getConf(boolean withSource){
		return getConf(withSource, null, false);
	}

	public static String relativePath(String cwd, String name){
		String nn = null;
		String cd = null;
		try{
			nn = (new File(name)).getCanonicalPath();
			cd = (new File(cwd)).getCanonicalPath() + File.separator;	
		}
		catch(Exception ex){System.err.println(ex); return null;}
		int n = 1;
		while((n < nn.length()) && (n < cd.length()) && nn.substring(0,n).equals(cd.substring(0,n)))n++;
		if(n == 1) return null;
		String sn = (new File(nn.substring(0,n) + "abc")).getParent();
		if(sn == null) return null;
		n = sn.length();
		if(sn.substring(sn.length() - 1).equals(File.separator)) n--;
		nn = nn.substring(n + 1);
		if(cd.length() > n + 1) cd = cd.substring(n + 1);
		else cd = null;
		sn = nn;
		while(cd != null){
			sn =".." + File.separator + sn;
			cd = (new File(cd)).getParent();
		}
		return sn;
	}

	public String getConf(boolean withSource, String cwd, boolean portableBgs){
		String s = "isRepeating=" + isRepeating + "\n";
		s = s + "astate=" + astate + "\n";
		float temp = internalScale;
		if(isPdf && portableBgs) temp = 2 * internalScale;
		s = s + "internalScale=" + temp + "\n";
		if(withSource) {
			s = s + "source=" + getSource().getName() + "\n";
			String sn = relativePath(cwd, getSource().getName());
			if(sn != null) s = s + "rsource=" + sn + "\n";
		}
		return s;
	}

	public boolean setConf(String y){
		String z = Tools.getLine(y, "isRepeating"); 
		if(z != null){
			if(z.equals("true")) isRepeating = true;
			else isRepeating = false;
		}
		z = Tools.getLine(y, "internalScale");
		if(z != null){
			internalScale = Float.parseFloat(z);
		}
		//if it isn't in the conf, it is an older file, and we have to do a make
		boolean doMake = true;
		z = Tools.getLine(y, "astate"); 
		if(z != null){
			doMake = false;
			int aa = Integer.parseInt(z);
			if((aa == 0) && (astate == -1)) astate = 0;
			if((aa == 1) && (astate != 1)) doMake = true;
		}
		return doMake;	
	}

	public BufferedImage rewriteBI(Image res, float s, int bq, int bgfade, Color cc){
		if(res == null) return null;
		int type = BufferedImage.TYPE_3BYTE_BGR;
		boolean flag = false;
		BufferedImage g = null;
		try{
			g = new BufferedImage((int)(s * res.getWidth(null)), (int)(s * res.getHeight(null)), type);
		}
		catch(Error er){
			clearCache();
			flag = true;
		}
		if(flag) g = new BufferedImage((int)(s * res.getWidth(null)), (int)(s * res.getHeight(null)), type);
		if(g == null) return null;			
		Graphics2D g2 = g.createGraphics();
		if(bq == 2) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		if(bq == 1) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		AffineTransform at = new AffineTransform();
		at.scale(s, s);
		if(bgfade != 0){
			//g2.setColor(Color.white);
			g2.setColor(cc);
			g2.fillRect(0, 0, g.getWidth(), g.getHeight());
			float test = (100.0f - bgfade)/100.0f;
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, test));
		}
		g2.drawImage(res, at, null);
		if(bgfade != 0)g2.setComposite(AlphaComposite.SrcOver);
		g2.dispose();
		return g;
	}

	static public String getSource(String y){
		return Tools.getLine(y, "source"); 
	}

	public void writeBg(Graphics2D g2, AffineTransform at, int pi, int print, int bq, float fscale, int bgfade, Color cc){
		Image im;
		if(print == 1){
			at.scale(0.5, 0.5);
			im = getScaledBg(pi, 2.0f  * fscale, bq, bgfade, cc);
			g2.drawImage(im, at, null);
			return;
		}
		else im = getScaledBg(pi,fscale, bq, bgfade, cc);
		if(im != null) {
			Rectangle r = g2.getClipBounds();
			//you wouldn't think that it would be necessary to clip the source by hand, but it is a lot faster
			if((r != null)){
				BufferedImage test = null;
				boolean done = false;
				if(!at.equals(new AffineTransform())){
					try{
						AffineTransform bt = at.createInverse();
						Rectangle rr = bt.createTransformedShape(r).getBounds();
						int iw = ((BufferedImage) im).getWidth() - (int)rr.getX() - rr.width;
						int ih = ((BufferedImage) im).getHeight() - (int) rr.getY() - rr.height;
						if(iw > 0) iw = 0;
						if(ih > 0) ih = 0;
						test = ((BufferedImage) im).getSubimage((int)rr.getX(), (int)rr.getY(), (int) (rr.getWidth()) + iw, (int)(rr.getHeight()) + ih);			
						at.translate(rr.getX(), rr.getY());
						done = true;	
					}
					catch(Exception ex){ex.printStackTrace();}	
				}
				try{
					if(!done){
						int iw = ((BufferedImage) im).getWidth() - (int)r.getX() - r.width;
						int ih = ((BufferedImage) im).getHeight() - (int) r.getY() - r.height;
						int rw = r.width;
						int rh = r.height;
						if(iw < 0) rw = rw + iw;
						if(ih < 0) rh = rh + ih;
						int ix = (int)r.getX();
						int iy = (int)r.getY();
						if(ix < 0) {
							rw = rw + ix;
							ix = 0;
						}
						if(iy < 0) {
							rh = rh + iy;
							iy = 0;
						}
						test = ((BufferedImage) im).getSubimage(ix, iy, rw, rh);
						at.translate(ix, iy);
					}
					g2.drawImage(test, at, null);
				}
				catch(Exception ex){ex.printStackTrace(); System.err.println("" + r.getX() + " " + r.getY()); g2.drawImage(im, at, null);}
				//had to fix some code here because the rectangle was going out of bounds. Left the error catching routine in case there are other problems with the clipping to prevent data loss
			}
			else g2.drawImage(im, at, null);
		}
	}
}

/**
 * This class implements Jbgs by reading a sequence of background
 * images as BufferedImgages. All scaling is then performed on these
 * BufferedImages. 
 */
class JbgsDefault extends Background {
	private LinkedList bgs = new LinkedList();
	public Image imgcache[] = null;
	public float cachescale[] = null;
	public int cachebq[] = null;
	public int cachefade[] = null;
	public Color cachecc[] = null;
	/* these arrays contains characteristics corresponding to the
	page images in imgcache. The cached image is used only if the
	characteristics fits the requested ones. */
	public boolean noReaderMsg = true;
	public boolean bgLoad = true;

	/**
	 * Create an empty background image sequence.
	 * It has size 0 and returns "" as its name.
	 */
	JbgsDefault() {
		this(new JbgsSource("",null));
	}

	/**
	 * Create a background image sequence.
	 * For a description of parameters see Jbgs(String,byte[]).
	 */
	JbgsDefault(JbgsSource s) {
		super(s);
	}

	public void unmake(){
		astate = 0;
		bgs = new LinkedList();
		imgcache = null;
		cachescale = null;
		cachecc = null;
	}

	public void clearCache(){
		int pc = size();
		if(pc == 0) return;
		imgcache = new Image[pc];
		cachescale = new float[pc];
		cachebq = new int[pc];
		cachefade = new int[pc];
		cachecc = new Color[pc];
		for (int i=0; i < pc; i++) cachescale[i]=0;
	}

	public void removeCachedImage(int i){
		if(imgcache == null) return;
		if(i >= imgcache.length) return;	
		imgcache[i] = null;
		cachescale[i] = 0;
		//System.out.println("removing bg " + i);
	}

	public void make(){
		if(astate == 1) return;
		astate = 1;
		JbgsSource s = src;
		if ("".equals(s.getName())) return;
		try{
			InputStream sin = s.getInputStream();
			if(sin == null){
				System.out.println("Background not found");
					if(noReaderMsg) Jarnal.getInstance().getJrnlTimerListener().setMessage("Background file not found\n or could not be opened: " + s.getName(), "Problem Loading Background");
					noReaderMsg = true;
				return;
			}
			ZipInputStream zip = new ZipInputStream(sin);
			BufferedImage bg;
			int iind = 0;
			ImageIO.scanForPlugins();
			boolean oof = false;
			for(ZipEntry ze = zip.getNextEntry(); ze != null; ze = zip.getNextEntry()){
				if(ze.getName().endsWith(".xml")) {
					oof = true;
					iind = 0;
				}
				if(!oof){
					if(Jarnal.getInstance().isApplet || Jarnal.getInstance().showGUI){
						ByteArrayOutputStream bbb = new ByteArrayOutputStream();	
						int nmax = 10000; 
						byte bb[] = new byte[nmax + 1];
						int nread = 0;
						while((nread = zip.read(bb, 0, nmax)) >= 0)bbb.write(bb,0, nread);
						InputStream ccc = new ByteArrayInputStream(bbb.toByteArray());
						bg = ImageIO.read(ccc);
					}
					else bg = ImageIO.read(zip);
					if(bg != null){
						bgs.add(bg);
						iind++;	
					}
				}
			}
			if(iind == 0){
				ImageInputStream iis = ImageIO.createImageInputStream (s.getInputStream());
				Iterator readers = ImageIO.getImageReaders(iis);
				if(readers.hasNext()) {
					ImageReader reader = (ImageReader)readers.next();
					reader.setInput (iis, false);
					ImageReadParam ip = reader.getDefaultReadParam();
					int np = reader.getNumImages(true);
 					for (int i = 0; i < np; i++){
						bg = reader.read (i, ip);
						if(bg != null){
							bgs.add(bg);
							iind++;
						} 
					}
				}
				else {
					System.out.println("no background reader found");
					if(noReaderMsg) Jarnal.getInstance().getJrnlTimerListener().setMessage("No background reader found for: " + s.getName(), "Problem Loading Background");
					noReaderMsg = false;
					bgLoad = false;
				}
			}	
		}
		catch(IOException ex){ex.printStackTrace();}
		clearCache();
		initRep();
	}

	int size() { return bgs.size(); }

	int getHeight(int i){
		if (i < 0 || i >= bgs.size()) return -1;
		return (int)(((BufferedImage)bgs.get(i)).getHeight()/internalScale);
	}
	int getWidth(int i){
		if (i < 0 || i >= bgs.size()) return -1;
		return (int)(((BufferedImage)bgs.get(i)).getWidth()/internalScale);
	}

	Image getScaledBg(int i, float s, int bq, int bgfade, Color cc) {
		i = getPage(i);
		if (i < 0 || i >= bgs.size()) return null;
		s = s/internalScale;
		if ((cachescale[i] == s) && (cachebq[i] == bq) && (cachefade[i] == bgfade) && (cachecc[i] != null) &&  (cachecc[i].equals(cc))) return imgcache[i];
		BufferedImage res = (BufferedImage) bgs.get(i);
		BufferedImage g = rewriteBI(res, s, bq, bgfade, cc);
		if(cacheOn){
			notifyAddImage(i);		
			cachescale[i] = s;
			imgcache[i] = g;
			cachebq[i] = bq;
			cachefade[i] = bgfade;
			cachecc[i] = cc;
		}
		return g;
	}
}

//This is a dummy JbgsPdf class so you can compile without Jpedal
//Comment out the usual JbgsPdf class
//and uncomment this one
/**
class JbgsPdf extends Jbgs {
	JbgsPdf(JbgsSource s) {
		super(s);
	}

	Image getScaledBg(int i, float s, int bq, Color cc) {
		return null;
	}
	static boolean jpedalAvailable() {
		return false;
	}
	public void make(){}
	public void unmake(){}
	public void clearCache(){}
}
*/

/**
 * This class implements Jbgs by the sequence of pages in a PDF file.
 * The Jpedal PDF decoder is used. Scaling is done by the PDF decoder 
 * while the page is converted to a bitmap.
 */
class JbgsPdf extends JbgsDefault {

	private boolean isWarned = false;

	private PdfDecoder pd = new PdfDecoder();
	private String pagefiles[] = null;
	private static boolean noRescale = true;
	private com.lowagie.text.pdf.PdfReader reader = null;

	JbgsPdf(JbgsSource s) {
		super(s);
		isPdf = true;
	}

	public com.lowagie.text.pdf.PdfReader getReader(){
		if(reader != null) return reader;
		String fname = getSource().getName();
		if(fname.equals("none")) return null;
		try{
			reader = new com.lowagie.text.pdf.PdfReader(fname);
		}
		catch(Exception ex){ex.printStackTrace(); return null;}
		return reader;
	}

	public void unmake(){
		astate = 0;
		pd = new PdfDecoder();
		imgcache = null;
		cachescale = null;
		pagefiles = null;
		cachecc = null;
	}
	
	public void make(){
		if(astate == 1) return;
		astate = 1;
		JbgsSource s = src;
		if ("".equals(s.getName())) return;
		byte [] b = s.getBuffer();
		try {
			if (b != null) pd.openPdfArray(b);
			else pd.openPdfFile(s.getName());
		}
		catch (PdfException ex) {ex.printStackTrace(); }
		clearCache();
		pagefiles = new String[size()];
		initRep();
	}

	int size() { return pd.getPageCount(); }
	
	int getHeight(int i){
		if (i < 0 || i >= size()) return -1;
		Image res = null;
		int h = -1;
		//java.awt.print.PageFormat pf = pd.getPageFormat(i+1);
		//h = (int) pf.getHeight();
		org.jpedal.objects.PdfPageData ppd = pd.getPdfPageData();
		h = ppd.getMediaBoxHeight(i + 1);
		int r = ppd.getRotation(i+1);
		if((r !=0) && (r != 180)) h = ppd.getMediaBoxWidth(i + 1);
		return h;
	}

	private String formatInfoLine(String n, String v){
		return "<tr><td>" + n + ":" + "</td><td>" + v + "</td></tr>";
	}

	public void setOutline(Out outline, String bghandle){
		String ans = getOutline();
		if(ans != null) outline.setOutline(ans, bghandle);
	}		

	public String getOutline(){
		String ans = null;
		try{
			org.w3c.dom.Document xdoc = null;
			try{
				xdoc = pd.getOutlineAsXML();
			}
			catch(Exception ex){return null;}
			if(xdoc == null) return null;
 			javax.xml.transform.TransformerFactory tFactory = javax.xml.transform.TransformerFactory.newInstance();
   			javax.xml.transform.Transformer transformer = tFactory.newTransformer();

   			javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(xdoc);
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
   			javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(bs);
   			transformer.transform(source, result);
			ans = new String(bs.toByteArray());
			
		}
		catch(Exception ex){ex.printStackTrace();}
		return ans;
	}		

	public String getInfo(){
		//System.out.print(getOutline());
		org.jpedal.objects.PdfFileInformation pfi = pd.getFileInformationData();
		String names[] = pfi.getFieldNames();
		String values[] = pfi.getFieldValues();
		String ans = "<html><table>" + formatInfoLine("File", getSource().getName());
		for(int i = 0; i < names.length; i++){
			ans = ans + formatInfoLine(names[i], values[i]);
		}
		return ans + "</table></html>";
	}

	int getRotation(int i){
		org.jpedal.objects.PdfPageData ppd = pd.getPdfPageData();
		return ppd.getRotation(i+1);
	}

	int getWidth(int i){
		if (i < 0 || i >= size()) return -1;
		Image res = null;
		int h = -1;
		java.awt.print.PageFormat pf = pd.getPageFormat(i+1);
		//h = (int) pf.getWidth();
		//if(pf.getOrientation() == java.awt.print.PageFormat.LANDSCAPE) h = (int) pf.getHeight();
		org.jpedal.objects.PdfPageData ppd = pd.getPdfPageData();
		h = ppd.getMediaBoxWidth(i + 1);
		int r = ppd.getRotation(i+1);
		if((r !=0) && (r != 180)) h = ppd.getMediaBoxHeight(i + 1);
		return h;
	}

	Image getScaledBg(int i, float s, int bq, int bgfade, Color cc) {
		i = getPage(i);
		if (i < 0 || i >= size()) return null;
		if ((cachescale[i] == s) && (cachefade[i] == bgfade) && (cachecc[i] != null) && (cachecc[i].equals(cc))) return imgcache[i];
		else {
			if(useGS){
				String est = Jarnal.getInstance().gs;
				String additionalMsg = "";
				try{
					String pfile = pagefiles[i];
					if(pfile == null){
						File tfile = File.createTempFile("pdfpage", ".png");
						pfile = tfile.getPath();
						pagefiles[i] = pfile;
					}
					//else (new File(pfile)).delete();
					Runtime rt = Runtime.getRuntime();
					int fact = 2; 
					if((Jarnal.getInstance().gs.indexOf("GraphicsAlphaBits") > -1) && (bq == 2)) noRescale = false;
					int bbq = 1;
					int cut = fact * 72;
					if((Jarnal.getInstance().gs.indexOf("pdftoppm") > -1) || noRescale) {
						fact = 1;
						bbq = 1;
						cut = 1;
					}
					int rs = (int) (fact * s * 72);
					float rescale = 1 / (float) fact;
					boolean rewrite = true;
					if(rs < cut) {
						rescale = rescale * (float) rs / (float) cut;
						rs = cut;
						if(cachescale[i] > 0){
							int ors = (int) (fact * cachescale[i] * 72);
							if(ors < cut) rewrite = false;
						}
					}
					int ev = 0;
					String rfile = pfile;
					if(rewrite){		
						int j = i + 1;
						Process ps;	
						est = est.replaceAll("%1", "" + j);
						est = est.replaceAll("%2", "" + j);
						est = Tools.replaceAll(est, "%3", Tools.cmdQuote(pfile));
						est = est.replaceAll("%4", "" + rs);
						est = Tools.replaceAll(est, "%5", Tools.cmdQuote(src.getName()));
						if(Tools.checkMSWindows()){
							ps = rt.exec(est);
						}
						else{
							String estev[] = new String[5];
							estev[0] = "" + j;
							estev[1] = "" + j;
							estev[2] = pfile;
							estev[3] = "" + rs;
							estev[4] = src.getName();
							estev = Tools.replaceAllUnixFoo(Jarnal.getInstance().gs, estev);
							ps = rt.exec(estev);
						}							
   						java.util.Timer jst = new java.util.Timer();
						jst.schedule(new waitTimer(ps), 5000);
						ps.waitFor();
						jst.cancel();
						ev = ps.exitValue();
						if(est.indexOf("pdftoppm") > -1) {
							String num = "" + j;
							if(num.length() == 1) num = "00000" + num;
							if(num.length() == 2) num = "0000" + num;
							if(num.length() == 3) num = "000" + num;
							String ppmext = ".ppm";
							if(Jarnal.getInstance().gs.indexOf("png") > -1) ppmext = ".png";
							if(Jarnal.getInstance().gs.indexOf("jpeg") > -1) ppmext = ".jpg";
							rfile = pfile + "-" + num + ppmext;
							File rrfile = new File(rfile);
							if(!rrfile.exists()){
								num = "" + j;
								for(int k = 0; k <= 5; k++){
									rfile = pfile + "-" + num + ppmext;
									rrfile = new File(rfile);
									if(rrfile.exists()) break;
									num = "0" + num;
								}
							}
						}
						//else if((new File(rfile)).exists()) ev = 0;
					}
					if(ev == 0){	
						File rrfile = new File(rfile);
						rrfile.deleteOnExit();	
						BufferedImage bg = ImageIO.read(rrfile);
						if(bg != null){
							System.out.println("using external renderer " + est);
							BufferedImage g = null;
							if((fact == 1) && (bgfade == 0)) g = bg;
							else g = rewriteBI(bg, rescale, bbq, bgfade, cc);
							if(cacheOn){
								notifyAddImage(i);	
								imgcache[i] = g;
								cachescale[i] = s;
								cachefade[i] = bgfade;
								cachecc[i] = cc;
							}
							return g;
						}
						additionalMsg = "\nDetails: could not read ppm file.\nDo you have the JAI installed correctly?"; 	
					}	
				}
				catch(Exception ex){ex.printStackTrace();}
				String gsmsg = "Cannot use external pdf renderer:\n" + est + "\nFalling back on internal renderer" + additionalMsg;
				if(!silentGS){
					useGS = false;
					Jarnal.getInstance().getJrnlTimerListener().setMessage(gsmsg, "PDF External Renderer Warning");
				}
				else {	
					System.out.println("In silent external renderer mode. Cannot translate current page. Will try again with other pages." + gsmsg);
				}
			}
			//gs failed, try using jpedal for rendering
			Image res = null;
			try {
				if(!Jarnal.getInstance().isApplet){
					Runtime rt = Runtime.getRuntime();
					float test = (float)rt.freeMemory() + (float)Tools.maxMemory() - (float)rt.totalMemory();
					test = 100.0f - (100.0f *test/(float) Tools.maxMemory());
					if(test > 80.0f) {
						System.out.println("memory low; cache cleared for Jpedal " + i);
						clearCache();
					}
	    			}
				// beware: PdfDecoder counts pages beginning with 1
				pd.setPageParameters(s,i+1);
				res = pd.getPageAsImage(i+1);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				res = null; 
				if(!isWarned)
					Jarnal.getInstance().getJrnlTimerListener().setMessage("An error occured during the loading of a pdf file.\nThe pdf background will not display.", "PDF Load Warning");
				isWarned = true;
			}
			BufferedImage g = rewriteBI(res, 1.0f, 1, bgfade, cc);
			if(cacheOn){
				notifyAddImage(i);
				cachescale[i] = s;
				imgcache[i] = g;
				cachefade[i] = bgfade;
				cachecc[i] = cc;
			}
			if(!isWarned){
				isWarned = true;
				//Runtime rt = Runtime.getRuntime();
				long test = Tools.maxMemory();
				if(test < 68000000)
					Jarnal.getInstance().getJrnlTimerListener().setMessage("Your virtual machine has only " + (test/1000000) + "M of memory.\nIf your PDF background does not display you will need to\nincrease the memory allocated to your java virtual machine: use\njava -Xmx256m -jar jarnal.jar\nto run a 256 meg virtual machine.\nYou can use a larger number if necessary.", "PDF Memory Warning");
			}
			return g;
		}
	}

	static boolean jpedalAvailable() {
		boolean res = false;
		try {
			Class.forName("org.jpedal.PdfDecoder");
			res = true;
		}
		catch (Exception ex) {ex.printStackTrace();}
		return res;
	}
}

class jrnlPDFWriter {
	
	private com.lowagie.text.Document document;
	private com.lowagie.text.pdf.PdfWriter writer;
	private com.lowagie.text.pdf.DefaultFontMapper mapper;
	private com.lowagie.text.pdf.PdfImportedPage pg;
	private com.lowagie.text.pdf.PdfContentByte cb;
	private com.lowagie.text.pdf.PdfTemplate tp;
	private com.lowagie.text.pdf.PdfReader reader = null;
	private OutputStream os;
	private boolean firstPageDone = false;
	private float margX;
	private float margY;
	private float pageW;
	private float pageH;

	public jrnlPDFWriter(OutputStream os){
		this.os = os;
	}

	public void setMargins(double X, double Y){
		margX = (float) X;
		margY = (float) Y;
	}

	public boolean checkPDF(Background bgs){
		if(!bgs.isPdf) return false;
		//if(reader == null) return false;
		return true;
	}

	public boolean writeBg(Background bgs, float fscale, int bgrotate, int pi){
		if(!bgs.isPdf) return false;
		//if(bgrotate != 0) return false;
		reader = ((JbgsPdf)bgs).getReader();
		//String fname = bgs.getSource().getName();
		//if(fname.equals("none")) return false;
		if(reader == null) return false;
		try{
			//if(reader == null) reader = new com.lowagie.text.pdf.PdfReader(fname);
			//com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(fname);
			pg = writer.getImportedPage(reader, pi + 1);
			float ww = pg.getWidth();
			float hh = pg.getHeight();
			JbgsPdf pbgs = (JbgsPdf) bgs;
			int pr = pbgs.getRotation(pi);
			if((pr != 0) || (bgrotate != 0)){
				int rot = bgrotate + (pr / 90);
				rot = rot % 4;
				if(rot < 0) rot = rot + 4;
				int drot = -90 * rot;
				double theta = Math.toRadians(drot);
				int shiftup = 0;
				int shiftright = 0;
				if((rot == 1) || (rot == 3)) {
					float temp = hh;
					hh = ww;
					ww = temp;
				}
				if((rot == 1) || (rot == 2)) shiftup = 1;
				if((rot == 2) || (rot == 3)) shiftright = 1;
				cb.addTemplate(pg, (float) (fscale*Math.cos(theta)), (float)(fscale*Math.sin(theta)), (float)(-fscale*Math.sin(theta)), (float)(fscale*Math.cos(theta)), margX + (shiftright * fscale * ww), -margY + pageH - (fscale * hh) + (shiftup * fscale * hh));
				return true;
			}				
                	cb.addTemplate(pg, fscale, 0f, 0f, fscale, margX, -margY + pageH - (fscale * hh));
			return true;
		}
		catch(Exception ex){ex.printStackTrace(); return false;}
		//return false;
	}

	private Graphics2D g2;

	public Graphics2D writePDF(Image page, String op, int w, int h){
		try{
			if(op.equals("newPage")) {
				pageW = (float) w;
				pageH = (float) h;
				com.lowagie.text.Rectangle r = new com.lowagie.text.Rectangle((float) w, (float) h);
				if(!firstPageDone){
					document = new com.lowagie.text.Document(r);
            				writer = com.lowagie.text.pdf.PdfWriter.getInstance(document, os);
            				document.open();
					mapper = new com.lowagie.text.pdf.DefaultFontMapper();
            				com.lowagie.text.FontFactory.registerDirectories();
					if(Tools.checkMSWindows()){
						mapper.insertDirectory("c:/windows/fonts");
						mapper.insertDirectory("c:/winnt/fonts");
					}
					else {
            					mapper.insertDirectory("/usr/share/fonts");
            					mapper.insertDirectory("/usr/share/fonts/local");
					}
					firstPageDone = true;
				}
				else{
					document.setPageSize(r);
					document.newPage();
				}
            			cb = writer.getDirectContent();
            			tp = cb.createTemplate(w, h);
				g2 = tp.createGraphics(w, h, mapper);
            			tp.setWidth(w);
            			tp.setHeight(h);
				return g2;
			}
			if(op.equals("close")){
				document.close();
				reader = null;
				return null;
			}
			cb.addTemplate(tp, 0f, 0f);
			g2.dispose();	
		}
		catch(Exception ex){ex.printStackTrace();}
		return null;		 
	}
}

class waitTimer extends TimerTask{
	private Process ps;
	waitTimer(Process ps){
		this.ps = ps;
	}
	public void run() {
		ps.destroy();
	}
}


