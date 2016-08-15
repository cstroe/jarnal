package jarnal;

import javax.swing.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.image.*;
import java.util.*;
import java.net.*;
import java.lang.Math.*;
import java.lang.Number.*;
import java.io.*;
import java.util.zip.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.BufferedImage;

import jarnal.Tools;
import jarnal.Parameter;
import jarnal.Analyze;
import jarnal.BrushStroke;
import jarnal.Page;
import jarnal.Background;
import jarnal.Out;

public class Pages{

	public static boolean highlightLines = false;

	//public static File userDic = null;
	public static LinkedList globalscraps = new LinkedList();
	public static LinkedList globalbackgrounds = new LinkedList();

	public static LinkedList globalGraphics = new LinkedList();
	public static int globalGraphicsLimit = -1;

	public boolean saveBg = false;

	public static String pdftotext;

	public Communicator communicator = null;
	public boolean jsent = true;
	public boolean active = true;
	public boolean wantscontrol = false;
	private long synch = 0l;
	//bgsList is a list with the backgrounds
	public Hashtable bgsList = new Hashtable();
	public boolean portableBgs = false;

	public Out outline;

	private Page current = new Page(this);
	//jpages contains Jpages, one for each page in the document
	private LinkedList jpages = new LinkedList();
	private LinkedList redoList = new LinkedList();
	private LinkedList undoList = new LinkedList();
	private Hashtable extras = new Hashtable();
	private LinkedList graphicsList = new LinkedList();
	private Hashtable scraps = new Hashtable();
	private Hashtable scrapsbi = new Hashtable();
	private LinkedList findlist = new LinkedList();
	public Hashtable textFrames = new Hashtable();
	private UndoPage undo;
	//index of the current page in the jpages collection
	private int cindex = 0;
	private boolean savedUndo;
	private boolean serverlockpage = false;
	private float scale = 1;
	private Jpaper defaultPaper = new Jpaper();
	private Jchar parms = new Jchar();
	public boolean trapColors = false;
	private boolean recordingOn = false;
	public boolean timerFlag = false;
	private javax.swing.Timer timer = null;
	public boolean[] trapc = {true, true, true, true, true, true, true, true, true, true};
	public long utime = 0;
	public Tools tools = new Tools();
	public double PO = 2.0;

	public static boolean jpedalAvailable(){
		try{
			return JbgsPdf.jpedalAvailable();
		}
		catch(Error ex){}
		return false;
	}
	
	public Pages(){
		jpages.add(current);
		current.bgindex(0);
		if(jpedalAvailable()) System.out.println("PDF Backgrounds Available");
		else System.out.println("PDF Background Not Available");
		globalscraps.add(scraps);
		globalbackgrounds.add(bgsList);
	}

	public void serverlockpage(){
		serverlockpage = !serverlockpage;
	}

	public void exitPage(){
		if(current.collapseSel()) invalidate();
	}

	public void selectAllText(){current.selectAllText();}

	public boolean recordingOn(){
		return recordingOn;
	}
	
	public void recordingOn(boolean isOn){
		recordingOn = isOn;
		if(isOn && (timer == null)){
			timer = new javax.swing.Timer(60000, new recorderTimerListener(this));
			timer.start();
		}
		if((!isOn) && (timer != null)){
			timer.stop();
			timer = null;
		}
	}

	public void unmakeAll(){
		for(Enumeration e = bgsList.elements(); e.hasMoreElements();){
			Background bgs = (Background) e.nextElement();
			bgs.unmake();
		}
	}

	public Background bgs(){
		return bgs(cindex);
	}
	public Background bgs(int pi){
		Page jp = (Page) jpages.get(pi);
		return bgs(jp.bgid());
	}
	public Background bgs(String bgid){
		Background bgs = (Background) bgsList.get(bgid);
		if(bgs == null) {
			if(!findBackground(bgid)) return new JbgsDefault();
			else bgs = (Background) bgsList.get(bgid);
		}
		return bgs;
	}	

	public boolean[] getStatus(){
		boolean jpStatus[] = new boolean[4];
		jpStatus[0] = true;
		jpStatus[1] = true;
		jpStatus[2] = true;
		jpStatus[3] = true;
		if(undoList.size() == 0) jpStatus[0] = false;
		if(redoList.size() == 0) jpStatus[1] = false;
		if(cindex == 0) jpStatus[2] = false;
		if(cindex == (jpages.size() - 1)) jpStatus[3] = false;
		return jpStatus;
	}

	public int getHeightInt(int pi){
		if(pi < jpages.size()){
			Page jp = (Page)jpages.get(pi);
			return jp.getHeight();
		}
		return -1;
	}
	public int getWidthInt(int pi){
		if(pi < jpages.size()){
			Page jp = (Page)jpages.get(pi);
			return jp.getWidth();
		}
		return -1;
	}

	public double getHeight(int pi){
		if(pi < jpages.size()){
			Page jp = (Page)jpages.get(pi);
			return ((float) jp.getHeight())/jp.getScale();
		}
		return -1;
	}
	public double getWidth(int pi){
		if(pi < jpages.size()){
			Page jp = (Page)jpages.get(pi);
			return ((float) jp.getWidth())/jp.getScale();
		}
		return -1;
	}

	public int[] getMaxSize(int activePage, int npages){
		int ans[] = new int[2];
		ans[0] = current.getWidth();
		ans[1] = current.getHeight();
		if(npages == 1) return ans;
		int startP = cindex - activePage;
		for(int ii = startP; ii < startP + npages; ii++){
			if((ii != cindex) && (ii >=0) && (ii < jpages.size())){
				Page jp = (Page) jpages.get(ii);
				jp.setScale(scale);
				int ww = jp.getWidth();
				int hh = jp.getHeight();
				if(ww > ans[0]) ans[0] = ww;
				if(hh > ans[1]) ans[1] = hh;
			}
		}
		return ans;
	}
	public int bgindex(){return current.bgindex();}
	public boolean getRepeating(){return bgs().isRepeating;}
	public int getHeight(){return current.getHeight();}
	public int getWidth(){return current.getWidth();}
	public int getBaseWidth(){return current.getBaseWidth();}
	public int getBaseHeight(){return current.getBaseHeight();}
	public boolean pageSelected(){return current.pageSelected();}
	public Rectangle drawLast(){return current.drawLast();}
	public Rectangle dragText(int x, int y){return current.dragText(x, y);}
	public void reText(){current.reText();}
	public String hitImage(){return current.hitImage();}
	public Jpaper getPaper(){return current.getPaper();}
	public String getPaperDesc(){return getPaperDesc(false);}
	public String getPaperDesc(boolean absoluteScale){return current.getPaper().getDesc(absoluteScale);}
	public String getPaperConf(){return current.getPaper().getConf();}
	public String getPaperCopyConf(){
			Jpaper jpap = current.getPaper().copy();
			jpap.bgtext = false;
			return jpap.getConf();
	}
	public String getDefaultPaper(){return defaultPaper.getConf();}
	public JarnalSelection clipText(){return current.clipText();}
	public boolean dragShape(){return current.dragShape();}
	public String copyDragList(){return current.copyDragList();}
	public void setDragOp(int dragOp){current.setDragOp(dragOp);}
	public String analyze(Tools jt, boolean defaultfilepairs){return current.analyze(jt, parms, defaultfilepairs);}
	public Analyze getanalyze(Tools jt){return current.getanalyze(jt, parms);}
	public String analyzeClick(Tools jt, boolean defaultfilepairs){return current.analyzeClick(jt, parms, defaultfilepairs);}
	public String analyzeAll(Tools jt){return current.analyzeAll(jt, parms);}
	public String getDesc(){
		String desc = current.getDesc();
		if((desc == null) || (desc == "")) desc = parms.getDesc();
		return desc;
	}
	public String getHtmlDesc(){
		String desc = current.getHtmlDesc();
		if((desc == null) || (desc == "")) desc = parms.getHtmlDesc();
		return desc;
	}
	public String[] getTextStyle(){
		String[] desc = current.getTextStyle();
		if(desc == null) desc = parms.getTextStyle();
		return desc;
	}

	public Jtext checkFrame(int advance){
		String test = current.checkFrame(advance);
		if(test == null) return null;
		for(int i = cindex; i < cindex + jpages.size(); i++){
			int j = i % jpages.size();
			Page jp = (Page) jpages.get(j);
			if(jp.findFrame(test)) {
				cindex = j;
				current = jp;
				currentScale();
				return null;
			}
		}
		return current.ctext();
	}

	public float getY(){
		return scale * current.getY();
	}

	public void addFrame(Tools jt, Jtext jtext, Point2D.Double p, int direc){
		pushUndo(undoEntirePage());
		int h = current.textHeight();
		int w = current.textWidth();
		if(p != null){
			current.newText(jt, parms, p);
		}
		else{
			putdo(true);
			pageAfter("");
			newBg();
			setSizeToBg();
			pushUndo(undoEntirePage());
		}
		current.textJoin(jt, parms, jtext, w, h, direc);
		putdo(true);
	}

	public void textSplit(){
		current.textSplit();
	}
	public void setTextHeight(int y){
		current.setTextHeight(y);
	}

	public void nextFrame(){
		setStartMark();
		pushUndo(undoEntirePage());
		current.textSplit();
		putdo(true);
		setEndMark();
	}

	public int getX(){
		return current.getX();
	}

	public void applyArrow(int wt){
		pushUndo(undoEntirePage());
		current.applyArrow(wt);
		putdo(true);
	}

	public void applyPen(String color, String hicolor, float zscale, float hiscale, String trans, Tools jt){
		pushUndo(undoEntirePage());
		current.applyPen(color, hicolor, zscale, hiscale, trans, jt);
		putdo(true);
	}

	public Rectangle smooth(){
		return current.smooth();
	}

	public void pasteList(String page, Point2D.Double p){
		pushUndo(undoEntirePage());
		Page jp = new Page(this);
		jp.open(page);
		current.pasteList(jp, p);
		putdo(true);
	}

	public LinkedList eraseDragList(){
		pushUndo(undoEntirePage());
		LinkedList temp = current.eraseDragList();
		putdo(true);
		return temp;
	}

	public String getBgText(int p, String src){
		try{
			File tfile = File.createTempFile("pdftext", ".txt");
			tfile.deleteOnExit();
			String pfile = tfile.getPath();
			String est = pdftotext;
			est = Tools.replaceAll(est, "%1", "" + p);
			est = Tools.replaceAll(est, "%2", "" + p);
			est = Tools.replaceAll(est, "%3", Tools.cmdQuote(src));
			est = Tools.replaceAll(est, "%4", Tools.cmdQuote(pfile));
			Runtime rt = Runtime.getRuntime();
			Process ps = null;
			try{
				rt.exec(est);	
				ps = rt.exec(est);
				ps.waitFor();
			}
			catch (Exception ex){
				ex.printStackTrace();
				JOptionPane.showConfirmDialog(null, est, "Error Running External Program", JOptionPane.DEFAULT_OPTION);			
			}
			byte test[] = streamToByteArray(new FileInputStream(tfile));
			String data = new String(test);
			if(test.equals("")) return null;
			data = Tools.replaceAll(data, "\n\n", "<br>");
			data = Tools.replaceAll(data, "\n", " ");
			data = Tools.replaceAll(data, "<br>", "\n\n");
			return data;
		}
		catch(Exception ex){ex.printStackTrace(); return null;}
	}

	private static final int nfind = 5;

	public String findTarget(){
		if(findlist.size() == 0) return "";
		return (String) findlist.getLast();
	}

	public String[] getFind(){
		String x[] = new String[findlist.size()];
		for(int i =0; i < findlist.size(); i++) x[i] = (String) findlist.get(findlist.size() - i - 1);
		return x;
	}

	public void setFindConf(String s){
		for(int i = 0; i < nfind; i++){
			String str = Tools.getLine(s, "find" + i);
			if(str != null) findlist.add(str);
		}
	}

	public String getFindConf(){
		if(findlist.size() == 0) return "";
		String s = "[Find Strings]\n";
		for(int i = 0; i < findlist.size(); i++) s = s + "find" + i + "=" + (String) findlist.get(i) + "\n";
		s = s + "\n";
		return s;		
	}

	private void addFind(String str){
		for(int i = 0; i < findlist.size(); i++){
			String test = (String) findlist.get(i);
			if(str.equals(test)){
				findlist.remove(i);
				findlist.add(str);
				return;
			}
		}
		if(findlist.size() >= nfind) findlist.remove(0);
		findlist.add(str);
	}	

	public boolean find(String targ, boolean findFirst, boolean reverse, boolean entire, Tools jt, boolean includeBg, boolean matchCase, boolean wholeWord){
		addFind(targ);
		if(!matchCase) targ = targ.toLowerCase();
		if(includeBg) entire = true;
		if(!entire) return current.find(targ, findFirst, reverse, entire, matchCase, wholeWord);
		int itarg = cindex;
		if(findFirst){
			itarg = 0;
			if(reverse) itarg = jpages.size() - 1;
		}
		int dir = 1;
		if(reverse) dir = -1;
		while(true){
			Page jp = (Page) jpages.get(itarg);			
			if(includeBg){
				Background background = bgs(itarg);
				if(background.isPdf && !jp.getPaper().bgtext){
					String src = background.getSource().getName();
					if((src != null) && !src.equals("")){
						int p = jp.bgindex() + 1;
						String data = getBgText(p, src);
						if(data != null){
							int n = -1;
							String test = data;
							if(!matchCase) test = data.toLowerCase();
							n = test.indexOf(targ);
							if(n > -1){
								if(Parameter.findWhole(test, targ, n, reverse, wholeWord) > 0){
									setStartMark();
									if(itarg != cindex){
										exitPage();
										cindex = itarg;
										current = jp;
										currentScale();
									}
									bgFade(75);
									bgText(true);
									reText();
									typeKey(data, jt);
									setEndMark();
									current.clearSel();
								}
							}
						}
					}
				}
			}
			boolean found = jp.find(targ, findFirst, reverse, entire, matchCase, wholeWord);
			findFirst = true;
			if(found) {
				if(itarg != cindex){
					exitPage();
					cindex = itarg;
					current = jp;
					currentScale();
				}
				return true;
			}
			itarg = itarg + dir;
			if((itarg < 0) || (itarg >= jpages.size())) return false;
		}
	}

	public String[] getBgs(boolean areActive){
		LinkedList gbgs = new LinkedList();
		for(Enumeration e = bgsList.keys(); e.hasMoreElements();){
			String bghandle = (String) e.nextElement();
			Background bgs = (Background) bgsList.get(bghandle);
			if((bgs.astate == 1) && (areActive)) gbgs.add(bghandle);
			else if((bgs.astate != 1) && (!areActive)) gbgs.add(bghandle);
		}
		String test[] = new String[gbgs.size()];
		for(int i = 0; i < gbgs.size(); i++) test[i] = (String) gbgs.get(i);
		if(test.length == 0){
			test = new String[1];
			test[0] = "No Images Available";
		}
		return test;
	}

	public String[] getImages(boolean areActive){
		Hashtable inactive = new Hashtable();
		LinkedList active = new LinkedList();
		for(Enumeration e = scraps.keys(); e.hasMoreElements();) {
			Object ene = e.nextElement();
			inactive.put(ene, ene);
		}
		for(int i = 0; i < jpages.size(); i++){
			Page jp = (Page) jpages.get(i);
			for(int j = 0; j < jp.strokes.size(); j++){
				BrushStroke js = (BrushStroke) jp.strokes.get(j);
				if(js.isImage){
					Jscrap jss = (Jscrap) js;
					String str = (String) inactive.remove(jss.scrapName);
					if(str != null) active.add(str);
				}
			}
		}
		String test[] = new String[0];
		if(areActive){
			test = new String[active.size()];
			for(int i = 0; i < active.size(); i++) test[i] = (String) active.get(i);
		}
		if(!areActive){
			test = new String[inactive.size()];
			int i = 0;
			for(Enumeration e = inactive.keys(); e.hasMoreElements();) {
				test[i] = (String)e.nextElement();
				i++;
			}
		}					
		if(test.length == 0){
			test = new String[1];
			test[0] = "No Images Available";
		}
		return test;
	}

	public String[] getExtras(){
		String test[];
		if(extras.size() == 0){
			test = new String[1];
			test[0] = "No Extra Files Available";
		}
		else{ 
			int nex = extras.size();
			test = new String[nex];
			int i = 0;
			for(Enumeration e = extras.keys(); e.hasMoreElements();) {
				test[i] = (String) e.nextElement();
				i++;
			}
		}
		return test;			
	}

	public void addExtra(String name, String fname){
		extras.put(name, fname);
	}

	public void addExtra(String name, byte[] data){
		if(Jarnal.getInstance().isApplet) extras.put(name,data);
		else {
			int n = name.lastIndexOf('.');
			String suffix = ".tmp";
			String prefix = name;
			if(n > -1) {
				suffix = name.substring(n);
				prefix = name.substring(0, n);
			}
			try{
				File tfile = File.createTempFile(prefix, suffix);
				String efname = tfile.getPath();
				extras.put(name, efname);
				//System.out.println(efname);
				//tfile.deleteOnExit();
				FileOutputStream tout = new FileOutputStream(tfile);
				tout.write(data);
			}
			catch(Exception ex){System.err.println("addExtra: " + ex);}
		}
	}

	public String getExtraFile(String s){
		return (String) extras.get(s);
	}

	public byte[] getExtra(String s){
		Object data = extras.get(s);
		if(data == null) return null;
		String test = data.getClass().getName();
		if(!test.equals("java.lang.String")) return (byte[]) data;
		byte[] ans = null;
		try{
			FileInputStream fis = new FileInputStream((String) data);
			ans = streamToByteArray(fis);
		}
		catch(Exception ex){System.err.println("getExtra: " + ex);}
		return ans;
	}

	public byte[] getImage(String s){
		return (byte[]) scraps.get(s);
	}

	public void deleteImage(String s){
		scraps.remove(s);
		scrapsbi.remove(s);
	}

	public void deleteExtra(String s){
		extras.remove(s);
	}

	public boolean findBackground(String str){
		Background bgs = null;
		for(int ii = 0; ii < globalbackgrounds.size(); ii++){
			Hashtable ht = (Hashtable) globalbackgrounds.get(ii);
			bgs = (Background) ht.get(str);
			if(bgs != null) ii = globalbackgrounds.size();
		}
		if(bgs != null) {
			Background xbgs = Background.create(new JbgsSource(bgs.getSource().getName(), null));
			xbgs.make();
			bgsList.put(str, xbgs);
			return true;
		}
		return false;
	}

	public void deleteBackground(String bghandle){
		bgsList.remove(bghandle);
	}		

	public BufferedImage getScrap(String str){
		BufferedImage test = (BufferedImage) scrapsbi.get(str);
		if(test == null){
			//the image is not available in this file
			//check the global catalog, and import it if it is there
			byte[] findscr = null;
			for(int ii = 0; ii < globalscraps.size(); ii++){
				Hashtable ht = (Hashtable) globalscraps.get(ii);
				findscr = (byte[]) ht.get(str);
				if(findscr != null) ii = globalscraps.size();
			}
			if(findscr != null) addScrapImage(str, findscr);
			test = (BufferedImage) scrapsbi.get(str);			
		}
		if(test == null) System.out.println("Error can't find scrap: " + str);
		return test;
	}

	public boolean writeClippedGraphicFile(File f,OutputStream os, String type, boolean withBorders){
	    boolean ok = true;
	    Rectangle2D.Double rr = current.getDragRect();
	    if (rr == null) return false;
	    float scalex = 1.0f;
	    int w = (int)rr.getWidth();
	    int h = (int)rr.getHeight();
	    if(w <= 0) return false;
	    if(h <= 0) return false;
	    BufferedImage g = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
	    Graphics2D g2 = g.createGraphics();
	    setAllHints(g2);
	    g2.clearRect(0, 0, w, h);
	    g2.clip(new Rectangle2D.Double(0, 0, rr.getWidth(), rr.getHeight()));
	    g2.translate(-(int)rr.getX(), -(int)rr.getY());
	    try{
	    	if(print(g2, getPage() - 1, scalex, withBorders, -1)) {
			if(f == null) ImageIO.write(g, type, os);
	    		else ImageIO.write(g, type, f);
	    	}
	    	else {
			System.out.println("Error can't draw");
			ok = false;
	    	}
	    }
	    catch(Exception ex){System.out.println("Invalid selection"); ex.printStackTrace(); ok = false;}
	    g2.dispose();
	    return ok;
	}

	public void writeTIFFGraphicFile(File f, OutputStream os, boolean withBorders){
		int savepage = cindex;
		Iterator writers = ImageIO.getImageWritersByFormatName("tif");
		ImageWriter writer = (ImageWriter)writers.next();
		try{
			if(f != null) os = new FileOutputStream(f);
			ImageOutputStream ios = ImageIO.createImageOutputStream(os);
			writer.setOutput(ios);
			for(int i = 0; i < jpages.size(); i++){
				Page jp = (Page) jpages.get(i);
				cindex = i;
				current = jp;
				currentScale();
	    			int w = (int)(getWidth()/scale);
            			int h = (int)(getHeight()/scale);
	    			float scalex = 1.0f;
	    			BufferedImage g = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
	    			Graphics2D g2 = g.createGraphics();
	    			setAllHints(g2);
	    			g2.clearRect(0, 0, w, h);
	    			if(print(g2, getPage() - 1, scalex, withBorders, -1)) {
	    				try{
						if(cindex == 0) writer.write(g);
						else writer.writeInsert(-1, new IIOImage(g, null, null), new ImageWriteParam(Locale.US));
	    				} catch(java.io.IOException test){System.out.println(test);}
	    			}
	    			else System.out.println("Error can't draw");
	    			g2.dispose();
			}
			ios.close();
			Page jp = (Page) jpages.get(savepage);
			cindex = savepage;
			current = jp;
			currentScale();
		}
		catch (Exception ex){System.out.println(ex); return;}
	}

	public void writeGraphicFile(File f, OutputStream os, String type, boolean withBorders){
	    int w = (int)(getWidth()/scale);
            int h = (int)(getHeight()/scale);
	    float scalex = 1.0f;
	    BufferedImage g = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
	    Graphics2D g2 = g.createGraphics();
	    setAllHints(g2);
	    g2.clearRect(0, 0, w, h);
	    if(print(g2, getPage() - 1, scalex, withBorders, -1)) {
	    	try{
			if(f == null) ImageIO.write(g, type, os);
	    		else ImageIO.write(g, type, f);
	    	} catch(java.io.IOException test){System.out.println(test);}
	    }
	    else System.out.println("Error can't draw");
	    g2.dispose();
	} 

	public void setParms(String str){
		parms = Parameter.makeParms(str);
	}

	public String getParms(){
		return parms.getSVG();
	}

	public void setDefaultParms(){
		Jchar oparms = parms;
		parms = current.getCurParms();
		if(parms == null) parms = oparms;
		current.setParms(parms);
	}

	private void setFinalParms(){
		Jchar oparms = parms;
		parms = current.getFinalParms();
		if(parms == null) parms = oparms;
	}

	public Jpaper setDefaultPaper(){
		defaultPaper = current.getPaper().copy();
		defaultPaper.bgindex = -1;
		defaultPaper.bgtext = false;
		return defaultPaper.copy();
	}		

	public void setDefaultPaper(String s){
		defaultPaper.setConf(s);
		defaultPaper.bgindex = -1;
		defaultPaper.bgtext = false;
	}

	public void setWidth(float f){
		Jpaper pp = current.getPaper().copy();	
		pp.width = (int) (f * Jpaper.dpi);
		setPaper(pp);
	}	

	public void setHeight(float f){
		Jpaper pp = current.getPaper().copy();	
		pp.height = (int) (f * Jpaper.dpi);
		setPaper(pp);
	}

	public void toggleLandscape(){
		Jpaper pp = current.getPaper().copy();
		pp.height = current.getPaper().width;
		pp.width = current.getPaper().height;
		setPaper(pp);
	}

	public void newBg(){
		if(bgs().isRepeating) return;
		if(cindex < bgs().size()) return;
		if(bgs().size() == 0) return;
		Jpaper pp = current.getPaper().copy();
		pp.showBg = 0;
		setPaper(pp);
	}

	public boolean toggleBackground(){
		Jpaper pp = current.getPaper().copy();
		if(current.getPaper().showBg == 1) pp.showBg = 0;
		else pp.showBg = 1;
		setPaper(pp);
		if(pp.showBg == 1) return true;
		return false;
	}

	public boolean showBackground(){
		Jpaper pp = current.getPaper();
		if(pp.showBg == 1) return true;
		return false;
	}

	public void setTransparency(int trans){
		Jpaper pp = current.getPaper().copy();
		pp.transparency = trans;
		setPaper(pp);
	}		

	public void setLines(int nlines){
		Jpaper pp = current.getPaper().copy();	
		pp.nlines = nlines;
		setPaper(pp);
	}

	public void setPaper(String p){
		Jpaper pp = current.getPaper().copy();	
		pp.paper = p;
		setPaper(pp);
	}

	public void setPaper(int bcolor){
		Jpaper pp = current.getPaper().copy();
		pp.bcolor = bcolor;
		setPaper(pp);
	}

	public void setPaperConf(String p){
		Jpaper pp = current.getPaper();
		pp.setConf(p);
		if(cindex == 0) current.bgindex(0);
	}

	private void setPaperUndo(){
		undo = new UndoPage();
		undo.cindex = cindex;
		undo.op = "paper";
		undo.data = current.getPaper().copy();
		pushUndo(undo);
	}

	public void setOutlineUndo(String oldXML){
		undo = new UndoPage();
		undo.cindex = cindex;
		undo.op = "outline";
		undo.data = oldXML;
		pushUndo(undo);
		putdo(true);
	}

	public void setRepeating(boolean rep){
		undo = new UndoPage();
		undo.cindex = cindex;
		undo.op = "bgconf";
		undo.data = current.getPaper().bgid + " " + bgs().getConf(false);
		pushUndo(undo);
		bgs().isRepeating = rep;
		invalidateGraphics();
		putdo(true);
	}
	public void bgindex(int pi){
		Jpaper pp = current.getPaper().copy();
		pp.bgindex = pi;
		setPaper(pp);
	}
	public float bgScale(){
		Jpaper pp = current.getPaper();
		return pp.bgscale;
	}
	public void bgScale(float scale){
		Jpaper pp = current.getPaper().copy();
		pp.bgscale = scale;
		setPaper(pp);
		invalidate();
	}
	public void bgRotate(int rotate){
		Jpaper pp = current.getPaper().copy();
		int r = (pp.bgrotate + rotate) % 4;
		pp.bgrotate = r;
		setPaper(pp);
		invalidate();
	}
	public void bgFade(int fade){
		Jpaper pp = current.getPaper().copy();
		pp.bgfade = fade;
		setPaper(pp);
		invalidate();
	}
	public int bgFade(){
		Jpaper pp = current.getPaper().copy();
		return pp.bgfade;
	}
	public void bgText(boolean bgtext){
		Jpaper pp = current.getPaper().copy();
		pp.bgtext = bgtext;
		setPaper(pp);
		invalidate();
	}
	public boolean bgText(){
		Jpaper pp = current.getPaper().copy();
		return pp.bgtext;
	}				

	public void setPaper(Jpaper p){
	 	setPaper(p, true);
	}

	public void setPaper(Jpaper p, boolean rebg){
		int oldbg = current.bgindex();
		setPaperUndo();
		current.setPaper(p.copy());
		defaultPaper = p.copy();
		defaultPaper.bgindex = -1;
		if(!rebg) current.bgindex(oldbg);
		putdo(true);
	}

	public float getScale(){
		return scale;
	}
	
	public void setScale(float scale){
		this.scale = scale;
		currentScale();
	}

	private void removeAllCache(boolean clearAll){
 		ListIterator iter = globalGraphics.listIterator();
      		while (iter.hasNext()){
			memoryMan mm = (memoryMan) iter.next();
			if(mm.jp == this) iter.remove();
		}
		for(Enumeration e = bgsList.elements(); e.hasMoreElements();){
			Background bgs = (Background) e.nextElement();
			bgs.removeAllCache();
		}
		if(!clearAll) return;
		if(globalGraphics.size() > 0){
			memoryMan mm = (memoryMan) globalGraphics.remove(0);
			if(mm.jp != null) mm.jp.invalidateGraphics();
		}
		else{
			Background.globalbgCacheLimit = -1;
			Pages.globalGraphicsLimit = -1;
		}
	}	

	public void doExit(){
		graphicsList = null;;
		unmakeAll();
		removeAllCache(false);
	}

	private synchronized void graphicsListAddNull(int cindex){
		if(cindex < graphicsList.size()) graphicsList.add(cindex, null);
	}

	private synchronized void graphicsListRemove(int cindex){
		if(cindex < graphicsList.size()) graphicsList.remove(cindex);
		int jj = -1;
		for(int ii = 0; ii < globalGraphics.size(); ii++){
			memoryMan mm = (memoryMan) globalGraphics.get(ii);
			if(mm.jp == this){
				if(mm.pg == cindex) jj = ii;
				if(mm.pg > cindex) mm.pg = mm.pg -1;
			}
		globalGraphics.remove(ii);					
		}
	}

	public void invalidate(){
		if(cindex < graphicsList.size()) graphicsList.set(cindex, null);
		currentScale();
	}

	private synchronized boolean checkConstrained(){
		memoryMan.testMem();
		boolean constrained = true;
		if(globalGraphicsLimit == -1) constrained = false;
		if(constrained && globalGraphics.size() < globalGraphicsLimit) constrained = false;
		return constrained;
	}		

	public synchronized BufferedImage getImage(int w, int h){
		boolean constrained = checkConstrained();
		if(!constrained) return null;
		for(int ii = 0; (ii < ((globalGraphics.size()/3) + 1)) && (ii < globalGraphics.size()); ii++){
			memoryMan mm = (memoryMan) globalGraphics.get(ii);
			BufferedImage bi = (BufferedImage) mm.jp.graphicsList.get(mm.pg);
			if(bi != null){
				if((w == bi.getWidth()) && (h == bi.getHeight())){
					mm.jp.graphicsList.set(mm.pg, null);
					globalGraphics.remove(ii);
					Graphics2D gg2 = bi.createGraphics();
					gg2.setColor(Color.white);
					gg2.fillRect(0, 0, bi.getWidth(), bi.getHeight());
					return bi;
				}
			}
		}
		return null;
	}

	private synchronized void setImage(int i){
		if(globalGraphics.size() > 0){
			int jj = -1;
			for(int ii = 0; ii < globalGraphics.size(); ii++){
				memoryMan mm = (memoryMan) globalGraphics.get(ii);
				if((mm.jp == this) && (mm.pg == i)) jj = ii;
			}
			if(jj > -1) {
				globalGraphics.remove(jj);
				globalGraphics.add(new memoryMan(this, null, i));
				return;
			}
		}
		boolean constrained = checkConstrained();
		if(constrained & (globalGraphics.size() > 0)){
			memoryMan mm = (memoryMan) globalGraphics.remove(0);
			if(mm.jp.graphicsList != null){
				if(mm.jp.graphicsList.size() > mm.pg) mm.jp.graphicsList.set(mm.pg, null);
			}			
		}
		globalGraphics.add(new memoryMan(this, null, i));
	}

	public synchronized BufferedImage getGraphics(int i){
		if(i >= graphicsList.size()) return null;
		return (BufferedImage) graphicsList.get(i);
	}
	public synchronized void setGraphics2D(Graphics2D gg2){current.setGraphics2D(gg2);}
	public synchronized BufferedImage getGraphics(){return getGraphics(cindex);}
	public synchronized void setGraphics(BufferedImage bi){
		if(graphicsList == null) return;
		if(bi != null) setImage(cindex);
		for(int ii = graphicsList.size(); ii <= cindex; ii++) graphicsList.add(null);
		graphicsList.set(cindex, bi);
	}
	public synchronized void setGraphics(BufferedImage bi, int i){
		if(graphicsList == null) return;
		if(bi != null) setImage(i);
		for(int ii = graphicsList.size(); ii <= i; ii++) graphicsList.add(null);
		graphicsList.set(i, bi);
	}
	public synchronized void invalidateGraphics(){
		graphicsList = new LinkedList();
		invalidateBgs();
		removeAllCache(true);
	}
	private synchronized void invalidateBgs(){
		for(Enumeration e = bgsList.elements(); e.hasMoreElements();){
			Background bgs = (Background) e.nextElement();
			bgs.clearCache();
		}
	}
	public synchronized void draw(Graphics2D g2, int bq){current.draw(g2, bq);}
	public synchronized void draw(Graphics2D g2, int pi, int bq){((Page) jpages.get(pi)).draw(g2, bq);}
	public synchronized void draw(Graphics2D g2, int x, int y, int w, int h){current.draw(g2, x, y, w, h);}

	public synchronized void updatePage(Graphics2D gg2, BufferedImage gg, int firstPage, int p, int bq){
		if(getGraphics(firstPage + p) != null) return;
		Page jp = (Page) jpages.get(p);
	    	int w = jp.getWidth();
            	int h = jp.getHeight();
		jp.draw(gg2, bq);
		setGraphics(gg, firstPage + p);
	}

	private void setAllHints(Graphics2D g2){
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);	
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
	    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    g2.setBackground(Color.white);
	}

	public void setPrint(){
		current.print = -1;
	}

	public void showTextMarks(boolean show){
		int prn = 0;
		if(!show) prn = 2;
		for(int i = 0; i < jpages.size(); i++){
			Page jp = (Page) jpages.get(i);
			jp.print = prn;
		}
	}

	public synchronized boolean print(Graphics2D g2, int pi, float scale, boolean withBorders){
		return print(g2, pi, scale, withBorders, +1, null);
	}
	public synchronized boolean print(Graphics2D g2, int pi, float scale, boolean withBorders, jrnlPDFWriter pdfWriter){
		return print(g2, pi, scale, withBorders, +1, pdfWriter);
	}
	public synchronized boolean print(Graphics2D g2, int pi, float scale, boolean withBorders, int prn){
		return print(g2, pi, scale, withBorders, prn, null);
	}

	public synchronized boolean print(Graphics2D g2, int pi, float scale, boolean withBorders, int prn, jrnlPDFWriter pdfWriter){
		if(pi >= jpages.size()) return false;
		Page jp = new Page(this);
		jp = (Page) jpages.get(pi);
		String txt = jp.save(null);
		jp = new Page(this);
		jp.open(txt);
		//note that we are working form a copy of the jpage, so we don't have to
		//worry, for example, about setting jp.print back to false
		jp.print = prn;
		jp.pdfWriter = pdfWriter;
		jp.withBorders = withBorders;
		jp.setScale(scale);
		//turn the background cache off before printing
		//we won't need this particular scaled background again, but we might want the old one
		Background.cacheOn = false;
		jp.draw(g2, 2);
		Background.cacheOn = true;
		return true;	
	}

	public void setSizeToBg(){
		if(bgs().size() == 0) return;
		if(current.getPaper().showBg == 1) {
			Jpaper pp = current.getPaper().copy();
			int hh = (int)(current.getPaper().bgscale * bgs().getHeight(current.bgindex()));
			int ww = (int)(current.getPaper().bgscale * bgs().getWidth(current.bgindex()));
			int r = current.getPaper().bgrotate;
			if((r%2)!=0) {r = hh; hh = ww; ww = r;}
			if((hh <= 0) || (ww <= 0)) return;
			pp.height = hh;
			pp.width =  ww;
			setPaper(pp);
		}
	}

	public void setBgToPaper(boolean setWidth){
		if(bgs().size() == 0) return;
		if(current.getPaper().showBg == 1) {
			Jpaper pp = current.getPaper().copy();
			int hh = (int)(bgs().getHeight(current.bgindex()));
			int ww = (int)(bgs().getWidth(current.bgindex()));
			int r = current.getPaper().bgrotate;
			if((r%2)!=0) {r = hh; hh = ww; ww = r;}
			if((hh <= 0) || (ww <= 0)) return;
			if(setWidth) bgScale((float) pp.width/ww);
			else bgScale((float) pp.height/hh);
		}
	}			

	public float setScale(int i){
		((Page)jpages.get(i)).setScale(scale);
		return scale;
	}

	private void currentScale(){
		current.setScale(scale);
	}

	public void upScale(int adj){
		if(adj == 1) scale = scale * 1.2f;
		if(adj == -1) scale = scale / 1.2f;
		if(adj == 0) scale = 1.0f;
		currentScale();
	}

	//give up and put the entire page on the undo stack - this is safe but not efficient
	private UndoPage undoEntirePage(){
		undo = new UndoPage();
		undo.cindex = cindex;
		undo.op = "page";
		Page jp = current.copy();
		undo.data = jp;
		return undo;
	}

	public void clearPage(){
		pushUndo(undoEntirePage());
		Jpaper p = current.getPaper();
		current = new Page(this);
		jpages.set(cindex, current);
		current.setPaper(p);
		putdo(true);
	}

	public void replacePage(String str){
		pushUndo(undoEntirePage());
		Jpaper p = current.getPaper();
		current = new Page(this);
		jpages.set(cindex, current);
		if(!str.equals("")) current.open(str);
		else current.setPaper(p);
		currentScale();
		putdo(true);
	}

	int dragOp;

	public String getlink(){
		return current.getlink();
	}
	public void setlink(String str){
		pushUndo(undoEntirePage());
		current.setlink(str);
		putdo(true);
	}

	public boolean selectSingle(Point2D.Double startL, Tools jt){
		current.startStroke(startL, jt);
		current.startDragOp(2);
		return !pageSelected();
	}

	public void startDragOp(int op){
		dragOp = op;
		if(dragOp == 13) setPaperUndo();
		else {
			pushUndo(undoEntirePage());
		}
		current.startDragOp(op);
	}

	public int[] getDragRectX(int xx[], int offX){return current.getDragRectX(xx, offX);}
	public int[] getDragRectY(int yy[], int offY){return current.getDragRectY(yy, offY);}

	public void startDragRect(Point2D.Double x[]){
		current.startDragRect(x);
	}

	public void endDragOp(){
		if(dragOp == 13) {
			defaultPaper = current.getPaper().copy();
			defaultPaper.bgindex = -1;
		}
		current.endDragOp(); 
		putdo(true);
	}
	public LinkedList dragOp(Point2D.Double p){return current.dragOp(p);}

	public void startStroke(Point2D.Double startL, Tools jt){
		putdo(true);
		current.startStroke(startL, jt);
		undo = new UndoPage();
		if(jt.highlighter) undo.top = false;
		savedUndo = false;
	}

	public void addScrap(Point2D.Double p, String scrapName){
		current.addScrap(p, scrapName);
		undo = new UndoPage();
		undo.op = "addScrap";
		undo.cindex = cindex;
		pushUndo(undo);
		putdo(true);
	}

	public void addOverlay(Point2D.Double x[], String ostyle){
		putdo(true);
		undo = new UndoPage();
		undo.op = "stroke";
		undo.cindex = cindex;
		current.addOverlay(x, ostyle);
		pushUndo(undo);
		putdo(true);
	}

	public boolean overlaySelected(){
		return current.overlaySelected();
	}

	public String getOverlayStyle(String defaultStyle){
		return current.getOverlayStyle(defaultStyle);
	}

	public String setOverlayColor(String defaultStyle, String color){
		return setOverlayStyle(defaultStyle, -1, -1, color, null, -1, -1, -1);
	}

	public String setOverlayOutline(String defaultStyle, String color){
		return setOverlayStyle(defaultStyle, -1, -1, null, color, -1, -1, -1);
	}

	public int getOverlayFade(String defaultStyle){
		return getOverlayInt(defaultStyle, 3);
	}

	public int getOutlineThickeness(String defaultStyle){
		return getOverlayInt(defaultStyle, 2);
	}

	public int getOverlayInt(String defaultStyle, int index){
		return current.getOverlayInt(defaultStyle, index);
	}

	public String setOverlayStyle(String defaultStyle, int arcWidth, int arcHeight, String fillColor, String strokeColor, int strokeWidth, int fillFade, int strokeFade){
		pushUndo(undoEntirePage());
		String ans = current.setOverlayStyle(defaultStyle, arcWidth, arcHeight, fillColor, strokeColor, strokeWidth, fillFade, strokeFade);
		putdo(true);
		return ans;
	}

	public void makeOverlaySquare(String defaultStyle){
		pushUndo(undoEntirePage());
		current.makeOverlaySquare(defaultStyle);
		putdo(true);
	}

	public void clickText(){
		current.clickText();
	}

	public Rectangle startText(Point2D.Double startL){
		setFinalParms();
		if(undoList.size() > 0){
			UndoPage undo = (UndoPage) undoList.getLast();
			undo.skiptext = false;
		}
		Rectangle ans = current.startText(startL);
		if(current.dragShape()){
			pushUndo(undoEntirePage());
		}
		return ans;
	}

	//the undo was created with startStroke 
	//we now put it on the stack if we didn't already
	public void stroke(Point2D.Double endL){
		if(current.stroke(endL) && !savedUndo){
			undo.op = "stroke";
			undo.cindex = cindex;
			pushUndo(undo);
			savedUndo = true;
		}
	}

	public void setArrow(int wt){
		current.setArrow(wt);
	}

	//advance the text selection by adv characters
	public Rectangle adv(int adv, int extend){
		return current.adv(adv, extend);
	}

	public void setCurrent(Tools jt){
		if(current.setCurrent(jt, parms)){
			undo = new UndoPage();
			undo.oindex = current.getText();
			undo.op = "newText";
			undo.skiptext = true;
			undo.cindex = cindex;
			pushUndo(undo);
		}
	}

	public Rectangle forceTextRectangle(){
		return current.forceTextRectangle();
	}

	public Rectangle getOldTextRectangle(){
		return current.getOldTextRectangle();
	}

	public Rectangle typeKey(String str, Tools jt){
		undo = new UndoPage();
		String oldXML = current.typeKey(str, jt, parms);
		undo.oindex = current.getText();
		if(oldXML == null) undo.op = "newText";
		else {
			undo.op = "typeKey";
			undo.data = oldXML;
		}
		undo.skiptext = true;
		undo.cindex = cindex;
		if((str.length() == 1) && (!(str.equals(" ") || str.equals("\n")))){
			if(undoList.size() > 0){
				UndoPage topUndo = (UndoPage) undoList.getLast();
				if((!topUndo.skiptext) || (topUndo.cindex != undo.cindex) ||(topUndo.oindex != undo.oindex)) {pushUndo(undo);}
				else if(oldXML != null){
					String test = (String) topUndo.data;
					if((test != null) && test.startsWith(">" + Parameter.sep)) topUndo.data = test + Parameter.sep + oldXML;
				}
			}
			else {pushUndo(undo);}
		}
		else {pushUndo(undo);}
		return current.getTextRectangle();
	}

	public Rectangle setSelStyle(boolean bold, boolean italic, boolean underline, Float size, String font, String color){
		undo = new UndoPage();
		String oldXML = current.setSelStyle(bold, italic, underline, size, font, color, parms);
		undo.oindex = current.getText();
		if(oldXML == null) undo.op = "newText";
		else {
			undo.op = "typeKey";
			undo.data = oldXML;
		}
		undo.cindex = cindex;
		pushUndo(undo);
		putdo(true);
		return current.getTextRectangle();
	}


	public void click(Point2D.Double endL, Tools jt){
		current.click(endL, jt);
		undo = new UndoPage();
		if(jt.highlighter) undo.top = false;
		undo.op = "stroke";
		undo.cindex = cindex;
		pushUndo(undo);
		putdo(true);
	}

	public String[] getUndo(boolean isUndo){
		LinkedList ll = undoList;
		if(!isUndo) ll = redoList;
		String ans[] = new String[ll.size()];
		for(int i = ll.size() - 1; i >= 0; i--){
			UndoPage up = (UndoPage) ll.get(i);
			String str = up.op;
			String test = "";
			if(up.data != null) test = up.data.getClass().getName();
			if(test.equals("java.lang.String")) {
				String data = (String) up.data;
				int n = data.length();
				int m = 36 - str.length();
				if(m >= 2){
					if(n > m) n = m;
					str = str + " " + data.substring(data.length() - n);
				}
			}
			ans[ll.size() - i - 1] = str;
		}
		return ans;
	}

	public boolean checkundo(boolean isUndo){
		if( isUndo && (undoList.size() == 0)) return false;
		if(!isUndo && (redoList.size() == 0)) return false;
		return true;
	}

	public synchronized void undoRecog(){
		int ns = current.getLastns();
		for (int ii = 0; ii < (2 * ns); ii++) undo();
	}

	public int undoRatio(){
		if(redoList.size() == 0) return 100;
		return (int)(100*(float) undoList.size())/(undoList.size() + redoList.size());
	}

	public void setMark(String opstr){
		UndoPage up = new UndoPage();
		up.op = "mark" + opstr;
		//setMark shouldn't use pushUndo because it clears the redo stack
		//and it doesn't need to set the timer anyway
		putdo(false);
		undoList.add(up);
		putdo(true);
	}		

	public void setStartMark(){setMark("start");}

	public void setEndMark(){setMark("end");}

	public synchronized void untilMark(String stack, String test){
		LinkedList ll = undoList;
		if(stack.equals("redo")) ll = redoList;
		if(ll.size() == 0) return;
		UndoPage up = new UndoPage();
		do {
			up = (UndoPage) ll.getLast();
			if(stack.equals("redo")) redo();
			else undo();
		} while(!up.op.equals(test) && (ll.size() != 0));			
	}		

	public synchronized boolean undoMark(UndoPage up){
		if(up.op.startsWith("marktempredo")){
			if((communicator != null) && active){
				UndoPage sundo = new UndoPage();
				sundo.op = "undo";
				communicator.send(sundo);
			}
			return true;
		}
		if(up.op.startsWith("marktime")) utime = ((Long)up.data).longValue();		
		redoList.add(up);
		if((communicator != null) && active){
			UndoPage sundo = new UndoPage();
			sundo.op = "undotoredo";
			communicator.send(sundo);
		}
		if(up.op.equals("markend")) untilMark("undo", "markstart");
		return true;
	}

	public synchronized boolean redoMark(UndoPage up){
		if(up.op.startsWith("marktempundo")){
			if((communicator != null) && active){
				UndoPage sundo = new UndoPage();
				sundo.op = "redo";
				communicator.send(sundo);
			}
			return true;
		}
		if(up.op.startsWith("marktime")) utime = ((Long)up.data).longValue();
		undoList.add(up);
		if((communicator != null) && active){
			UndoPage sundo = new UndoPage();
			sundo.op = "redotoundo";
			communicator.send(sundo);
		}
		if(up.op.equals("markstart")) untilMark("redo", "markend");
		return true;
	}

	public synchronized void clearRedoList(){
		redoList = new LinkedList();
	}

	public synchronized void pushUndo(UndoPage up){
		putdo(false);
		if(recordingOn && timerFlag){
			UndoPage upmt = new UndoPage();
			upmt.op = "marktimer";
			upmt.data = new Long(getTimestamp());
			undoList.add(upmt);
			putdo(true);
			putdo(false);
			timerFlag = false;
		}
		undoList.add(up);
		//whenever we put something on the undo stack we should clear the redo stack
		clearRedoList();
	}

	public synchronized void doBgconf(UndoPage undo){
		String bgdata = (String) undo.data;
		int n = bgdata.indexOf(" ");
		String bghandle = bgdata.substring(0,n);
		String bgconf = bgdata.substring(n);
		Background bgs = (Background) bgsList.get(bghandle);
		undo.data = bghandle + " " + bgs.getConf(false);
		bgs.setConf(bgconf);
		invalidateGraphics();
	}		

	public synchronized boolean undo(){
		boolean repage = true;
		if(undoList.size() == 0) return false;
		//make sure the undo stack is sent before undoing something
		putdo(true);
		undo = (UndoPage) undoList.removeLast();
		if(undo.op.startsWith("mark")) return undoMark(undo);
		if(cindex == undo.cindex) repage = false;
		if(repage) exitPage();
		cindex = undo.cindex;
		//if(cindex >= jpages.size()) {
		//	System.out.println("Undo page index is out of bounds. " + undo.op);
		//	return false; 
		//}
		if(cindex < jpages.size()) current = (Page) jpages.get(cindex);
		if (undo.op.equals("paper")){
			Jpaper p = current.getPaper();
			current.setPaper((Jpaper) undo.data);
			defaultPaper = current.getPaper().copy();
			defaultPaper.bgindex = -1;
			undo.data = p;
			redoList.add(undo);
			repage = true;
		}
		//the actual stroke
		//it is at the top of the page stroke list unless
		//it is a bottom highlighter in which case it is at the bottom
		else if (undo.op.equals("stroke")){
			invalidate();
			undo.data = current.undo(undo.top);
			redoList.add(undo);
		}
		else if (undo.op.equals("addScrap")){
			invalidate();
			undo.data = current.undo(undo.top);
			redoList.add(undo);
		}
		else if (undo.op.equals("newText")){
			invalidate();
			current.unselectText();
			undo.data = current.undo(undo.top);
			redoList.add(undo);
		}
		else if (undo.op.equals("typeKey")){
			String oldXML = (String) undo.data;
			undo.data = current.undoText(undo.oindex, oldXML);
			redoList.add(undo);
		}
		else if(undo.op.equals("pagebefore")){
			exitPage();
			undo.op = "pagedelete";
			undo.data = jpages.remove(cindex);
			redoList.add(undo);
			if(cindex >= jpages.size()) cindex = jpages.size() - 1;
			current=(Page) jpages.get(cindex);
			currentScale();
			repage = true;
			invalidateGraphics();
		}
		else if(undo.op.equals("pagedelete")){
			exitPage();
			current = (Page) undo.data;
			currentScale();
			undo.op = "pagebefore";
			undo.data = null;
			redoList.add(undo);
			current.resetPage();
			jpages.add(cindex, current);
			repage = true;
			invalidateGraphics();
		}
		else if(undo.op.equals("outline")){
			String oldXML = outline.getXML();
			outline.setEntireOutline((String) undo.data);
			undo.data = oldXML;
			redoList.add(undo);
		}
		else if(undo.op.equals("page")){
			exitPage();
			current = (Page) jpages.remove(cindex);
			jpages.add(cindex, (Page) undo.data);
			undo.data = current;
			redoList.add(undo);
			current = (Page) jpages.get(cindex);
			current.resetPage();
			currentScale();
			repage = true;
			invalidateGraphics();
		}
		else if(undo.op.equals("bgopen")){
			String bghandle = (String) undo.data;
			redoList.add(undo);
			Background bgs = (Background) bgsList.get(bghandle);
			bgs.unmake();
			//repage = true;
			//invalidateGraphics();
		}
		else if(undo.op.equals("bgremove")){
			String bghandle = (String) undo.data;
			redoList.add(undo);
			((Background) bgsList.get(bghandle)).make();
			invalidateGraphics();
		}
		else if(undo.op.equals("bgconf")){
			doBgconf(undo);
			redoList.add(undo);
		}
		else if(undo.op.equals("addScrapImage")){
			String sctemp = (String) undo.data;
			LinkedList ll = new LinkedList();
			ll.add(sctemp);
			ll.add(scraps.remove(sctemp));
			scrapsbi.remove(sctemp);
			undo.data = ll;
			redoList.add(undo);
		}
		invalidate();
		if((communicator != null) && active){
			UndoPage sundo = new UndoPage();
			sundo.op = "undo";
			communicator.send(sundo);
		}
		return repage;	
	}

	public void setSynch(String s){
		long stime = Long.parseLong(s);
		synch = stime - (new Date()).getTime();
	}

	public long getTimestamp(){
		return (new Date()).getTime() + synch;
	}

	// used for remote communications
	// unless we are connected, jcom is null
	//
	// otherwise we should make sure that we create a new modified undo containing any data we need to transmit
	// after we are done, we send the modified undo
	//
	// putdo(true) means to ignore the next putdo command
	//
	// the sequencing is putdo(false) to send anything on the stack that is unsent, put the undo on the stack
	// then putdo(true) to send the undo and block the send stack
	//
	// repeated putdo(true) is safe and has no effect
	//
	// often we just use putdo(false); undoList.add(undo); putdo(true);
	// the sequence putdo(false); undoList.add(undo); is replaced with a call to pushUndo(undo)
	// this is so that timers can be placed on the stack
	//
	// but with text and strokes, the undo is added to the stack before the undo can be sent
	// in these cases the sequence is putdo(false); undoList.add(undo)
	// keep recording strokes/text until
	// the current object is completed
	// typical completion is when something else precedes putting a new undo on the stack with a putdo(false)
	// or in the case of a stroke, the mouse is released causing a putdo(true)
	//
	// text undoes continue to be built until a word is completed with a space or CR
	// this means full words are undone; it is irritating to have to undo character at a time
	// in text recognition mode switching between strokes and text means each undo 
	// refers to a single character, not an entire word
	// one result is that a word isn't sent until a space, CR or other operation takes place
	// strokes are similar - they get taken off the stack only when another operation does a putdo(false)
	// prior to a new undo
	//

	public void putdo(boolean newjsent){
		if(communicator == null) return;
		if(jsent) {
			jsent = newjsent;
			return;
		}
		jsent = newjsent;
		if(undoList.size() == 0) return;
		UndoPage sundo = ((UndoPage) undoList.getLast()).copy();
		Page scurrent = (Page) jpages.get(cindex);
		if (sundo.op.equals("paper")) sundo.data = scurrent.getPaper().getConf(); //Jpaper setConf()
		else if (sundo.op.equals("stroke")) sundo.data = scurrent.putdo(undo.top); //JStroke
		else if (sundo.op.equals("addScrap")) sundo.data = scurrent.putdo(undo.top); //Jscrap
		else if (sundo.op.equals("newText")) sundo.data = scurrent.putdo(undo.top); //JStroke
		else if (sundo.op.equals("typeKey")) sundo.data = scurrent.putText();
		else if (sundo.op.startsWith("marktimer")) sundo.data = "" + ((Long)(sundo.data)).longValue();
		else if (sundo.op.equals("outline")) sundo.data = outline.getXML();
		else if(sundo.op.equals("pagebefore")){
			sundo.op = "pagedelete";
			sundo.data = ((Page) jpages.get(cindex)).save(null); //Jpage
		}
		else if(sundo.op.equals("pagedelete")){
			sundo.op = "pagebefore";
			sundo.data = null;
		}
		else if(sundo.op.equals("page")) sundo.data = ((Page) jpages.get(cindex)).save(null); //Jpage
		else if(sundo.op.equals("bgopen")) {
			String bghandle = (String) sundo.data;
			Background bgs = (Background) bgsList.get(bghandle);
			sundo.data = bghandle + " " + bgs.getSource().getName();
		}
		else if(sundo.op.equals("addScrapImage")){
			String scrapName = (String) sundo.data;
			byte bb[] = (byte[]) scraps.get(scrapName);
			sundo.data = scrapName + "\n" + b64.encode(bb);
		}
		try{
			String test = (String) sundo.data;
		}
		catch(Exception ex){
			//once threw a class cast error; wasn't able to replicate
			//if it happens again the next code should provide diagnostics
			//undo.data is supposed to be either string or null at this point
			System.err.println("sundo.data is not a string in " + sundo.op + " undo data is: " + sundo.data);
		}
		communicator.send(sundo);
	}

	//getdo is the complement of putdo
	//it converts the modified undo back into a normal undo operation
	//then shoves it on the redo stack and redoes it
	public boolean getdo(UndoPage up){
		if(up.op.equals("bgopen")){
			String temp = (String) up.data;
			int n = temp.indexOf(" ");
			String bghandle = temp.substring(0,n);
			up.data = bghandle;
			String source = temp.substring(n+1);
			Background xbgs = Background.create(new JbgsSource(source, null));
			xbgs.make();
			bgsList.put(bghandle, xbgs);		
		}
		if(up.op.equals("paper")) {
			Jpaper jp = new Jpaper();
			jp.setConf((String) up.data);
			up.data = jp;
		}
		if(up.op.equals("stroke") || up.op.equals("newText") || up.op.equals("addScrap")){
			UndoPage utemp = new UndoPage();
			utemp.data = up.data;
			utemp.cindex = 0;
			up.data = current.parseStroke(utemp);
		}
		if(up.op.equals("pagedelete") || up.op.equals("page")) {
			Page jp = new Page(this);
			jp.open((String) up.data);
			up.data = jp;
		}
		if(up.op.equals("addScrapImage")){
			String test = (String) up.data;
			int pos = test.indexOf("\n");
			String scrapName = test.substring(0,pos);
			String bs64 = test.substring(pos + 1);
			LinkedList ll = new LinkedList();
			ll.add(scrapName);
			ll.add(b64.decode(bs64));
			up.data = ll;			
		}
		if(up.op.startsWith("marktimer")){
			up.data = new Long(Long.parseLong((String)up.data));
			utime = ((Long)up.data).longValue();
		}
		if(up.op.startsWith("mark")){
			undoList.add(up);
			return false;		
		}
		if(up.op.equals("undotoredo")){
			if(undoList.size() == 0) {
				System.err.println("Error: undo stack underflow");
				communicator.jc.closeSock();
				return false;
			}
			UndoPage upp = (UndoPage) undoList.removeLast();
			if(upp.op.startsWith("marktime")) utime = ((Long)upp.data).longValue();
			redoList.add(upp);
			return false;
		}
		if(up.op.equals("redotoundo")){
			if(redoList.size() == 0) {
				System.err.println("Error: redo stack underflow");
				communicator.jc.closeSock();
				return false;
			}
			UndoPage upp = (UndoPage) redoList.removeLast();
			if(upp.op.startsWith("marktime")) utime = ((Long)upp.data).longValue();
			undoList.add(upp);
			return false;
		}
		clearRedoList();
		redoList.add(up);
		return redo();
	}

	public synchronized boolean redo(){
		boolean repage = true;
		if(redoList.size() == 0) return false;
		undo = (UndoPage) redoList.removeLast();
		if(undo.op.startsWith("mark")) return redoMark(undo);
		if(cindex == undo.cindex) repage = false;
		if(repage) exitPage();
		cindex = undo.cindex;
		if(cindex < jpages.size()) current = (Page) jpages.get(cindex);
		if (undo.op.equals("paper")){
			Jpaper p = current.getPaper();
			current.setPaper((Jpaper) undo.data);
			defaultPaper = current.getPaper().copy();
			defaultPaper.bgindex = -1;
			undo.data = p;
			undoList.add(undo);
			repage = true;
		}
		else if (undo.op.equals("stroke")){
			invalidate();
			current.redo(undo);
			undo.data = null;
			undoList.add(undo);
		}
		else if (undo.op.equals("addScrap")){
			invalidate();
			current.redo(undo);
			undoList.add(undo);
		}
		else if (undo.op.equals("newText")){
			invalidate();
			current.unselectText();
			current.redo(undo);
			undoList.add(undo);
		}
		else if (undo.op.equals("typeKey")){
			String oldXML = (String) undo.data;
			undo.data = current.undoText(undo.oindex, oldXML);
			undoList.add(undo);
		}
		else if(undo.op.equals("pagebefore")){
			exitPage();
			undo.op = "pagedelete";
			undo.data = jpages.remove(cindex);
			undoList.add(undo);
			if(cindex >= jpages.size()) cindex = jpages.size() - 1;
			current=(Page) jpages.get(cindex);
			currentScale();
			repage = true;
			invalidateGraphics();
		}
		else if(undo.op.equals("pagedelete")){
			exitPage();
			current = (Page) undo.data;
			currentScale();
			undo.op = "pagebefore";
			undo.data = null;
			undoList.add(undo);
			current.resetPage();
			jpages.add(cindex, current);
			currentScale();
			repage = true;
			invalidateGraphics();
		}
		else if(undo.op.equals("outline")){
			String oldXML = outline.getXML();
			outline.setEntireOutline((String) undo.data);
			undo.data = oldXML;
			undoList.add(undo);
		}
		else if(undo.op.equals("page")){
			exitPage();
			current = (Page) jpages.remove(cindex);
			jpages.add(cindex, (Page) undo.data);
			undo.data = current;
			undoList.add(undo);
			current = (Page) jpages.get(cindex);
			current.resetPage();
			currentScale();
			repage = true;
			invalidateGraphics();
		}
		else if(undo.op.equals("bgopen")){
			String bghandle = (String) undo.data;
			undoList.add(undo);
			Background bgs = (Background) bgsList.get(bghandle);
			bgs.make();
			//repage = true;
			//invalidateGraphics();
		}
		else if(undo.op.equals("bgremove")){
			String bghandle = (String) undo.data;
			undoList.add(undo);
			((Background) bgsList.get(bghandle)).unmake();
			invalidateGraphics();
		}
		else if(undo.op.equals("bgconf")){
			doBgconf(undo);
			undoList.add(undo);
		}
		else if(undo.op.equals("addScrapImage")){
			LinkedList ll = (LinkedList) undo.data;
			addScrapImage((String) ll.get(0), (byte[]) ll.get(1));
			undo.data = (String) ll.get(0);
			undoList.add(undo);
		}
		invalidate();
		if(communicator != null && active){
			UndoPage sundo = new UndoPage();
			sundo.op = "redo";
			communicator.send(sundo);
		}
		return repage;
	}

	public String getPageRef(){
		return current.pageref;
	}

	public String getPageRef(int ii, String bghandle){
		for(int jj = 0; jj < jpages.size(); jj++){
			Page jp = (Page) jpages.get(jj);
			if(jp.bgid().equals(bghandle) && (jp.bgindex() == ii)) return jp.pageref;
		}
		return "";
		//if((ii < 0) || (ii >= jpages.size())) return "";
		//Jpage jp = (Jpage) jpages.get(ii);
		//return jp.pageref;
	}

	public int getPage(String pageref){
		for(int i = 0; i < jpages.size(); i++){
			Page jp = (Page) jpages.get(i);
			if(pageref.equals(jp.pageref)) return i;
		}
		return -1;
	}

	public synchronized boolean gotoPage(String pageref){
		for(int i = 0; i < jpages.size(); i++){
			Page jp = (Page) jpages.get(i);
			if(pageref.equals(jp.pageref)){
				if(cindex != i) exitPage();
				cindex = i;
				current = jp;
				currentScale();
				return true;
			}
		}
		return false;
	}
	
	public synchronized void absPage(int nindex){
		if(!serverlockpage) return;
		cindex = nindex;
		current = (Page) jpages.get(cindex);
		currentScale();
	}

	public synchronized boolean nextPage(int delta){
		boolean answer = false;
		if(delta != 0) exitPage();
		cindex = cindex + delta;
		if(cindex < 0) cindex = 0;
		if(cindex >= jpages.size()){
			answer = true;
			cindex = jpages.size() - 1;
		}
		current = (Page) jpages.get(cindex);
		currentScale();
		if(communicator != null) communicator.request("flipTo" + cindex);
		return answer;
	}

	public void pageDup(){
		pageAfter(current.save(null));
	}

	public void pageAfter(String str){
		setDefaultPaper();
		cindex = cindex + 1;
		pageBefore(str, false);
	}

	public void pageBefore(String str){
		setDefaultPaper();
		pageBefore(str, true);
	}

	public void pageBefore(String str, boolean isBefore){
		exitPage();
		current = new Page(this);
		current.setPaper(defaultPaper.copy());
		invalidateGraphics();
		undo = new UndoPage();
		undo.cindex = cindex;
		undo.op = "pagebefore";
		pushUndo(undo);
		jpages.add(cindex, current);
		graphicsListAddNull(cindex);
		if(!str.equals("")) current.open(str);
		if(current.bgindex() == -1){
			int altpi = 0;
			int pi = cindex;
			Page alt = null;
			if(isBefore){
				altpi = cindex + 1;
				if(altpi >= jpages.size()) isBefore = false;
				else{
					alt = (Page) jpages.get(altpi);
					if(alt.bgindex() != -1) pi = alt.bgindex() - 1;
					if(pi < 0) pi = 0;
					current.bgindex(pi);
				}
			}
			if(!isBefore){
				altpi = cindex - 1;
				if(altpi < 0) pi = 0;
				else{
					alt = (Page) jpages.get(altpi);
					if(alt.bgindex() != -1) pi = alt.bgindex() + 1;
				}
				current.bgindex(pi);
			}
			if((current.showBg() == 1) && !bgs().isRepeating){
				if((bgs().size() > 0) && (current.bgindex() < bgs().size())){
					if((cindex < jpages.size() - 1) && (cindex > 0)){
						Page prevP = (Page) jpages.get(cindex - 1);
						Page nextP = (Page) jpages.get(cindex + 1);
						if(nextP.bgindex() == (prevP.bgindex() + 1)) current.showBg(0);
					}
					if((cindex == 0) && !bgs().isRepeating) current.showBg(0);	
				}
			}		
		}
		currentScale();
		putdo(true);
	}

	public void pageDelete(){
		if(jpages.size() == 1) return;
		exitPage();
		undo = new UndoPage();
		undo.cindex = cindex;
		undo.op = "pagedelete";
		undo.data = jpages.remove(cindex);
		graphicsListRemove(cindex);
		pushUndo(undo);
		if(cindex >= jpages.size()) cindex = jpages.size() - 1;
		current=(Page) jpages.get(cindex);
		currentScale();	
		putdo(true);	
	}

	public int getPage(){
		return cindex + 1;
	}

	public int getPages(){
		return jpages.size();
	}

	public JarnalSelection copyPages(String action, TreeSet pageList){
		String allText = "";
		String mimeList = "";
		Iterator ts = pageList.iterator();
		while(ts.hasNext()){
			int ii = -((Integer)ts.next()).intValue();
			int pp = getPage() - 1;
			int delta = ii - pp;
			if(pp != 0) nextPage(delta);
			Page jp = current;
			if(action.startsWith("Copy")) {
				jp = current.copy();
				jp.resetpageref();
			}
			Hashtable ht = new Hashtable();
			allText = current.getAllText(ht, false) + "\n\n" + allText; 
			mimeList = jp.save(null) + UndoPage.terminator + mimeList;
		}
		return new JarnalSelection(allText, "", mimeList, "pages");		
	}

	public JarnalSelection copyPage(String action, String gname){
		Page jp = current;
		if(action.startsWith("Copy")) {
			jp = current.copy();
			jp.resetpageref();
		}
		if(gname != null) gname = gname + "?" + current.pageref;
		else gname = "";
		Hashtable ht = new Hashtable();
		return new JarnalSelection(current.getAllText(ht, false), gname, jp.save(null), "page");
	}

	public String copyAllHtml(){
		String ans = "";
		Hashtable ht = new Hashtable();
		for(int i = 0; i < jpages.size(); i++){
			Page jp = (Page) jpages.get(i);
			String test = jp.getAllText(ht, true);
			if(!ans.equals("") && !test.equals("")) ans = ans + "<hr><hr>" + test;
			else ans = ans + test;
		}
		return ans;
	}

	public String copyAllText(){
		String ans = "";
		Hashtable ht = new Hashtable();
		for(int i = 0; i < jpages.size(); i++){
			Page jp = (Page) jpages.get(i);
			String test = jp.getAllText(ht, false);
			if(!ans.equals("") && !test.equals("")) ans = ans + "\n\n" + test;
			else ans = ans + test;
		}
		return ans;
	}		

	public void netWrite(OutputStream out, String op, String conf, boolean withBorders){
		if(op.equals("$$jarnal")){
			save(out, conf);
		}
		if(op.equals("$$snapshot")){
			writeGraphicFile(null, out, "jpg", withBorders);
		}
		if(op.equals("$$tiff")){
			writeTIFFGraphicFile(null, out, withBorders);
		}
	}

	private void saveReplay(OutputStream out, LinkedList ll){
		for(int i = 0; i < ll.size(); i++){
			UndoPage up = (UndoPage) ll.get(i);
			String str = up.translate();
			try{
				out.write(str.getBytes());
			}
			catch(Exception ex){System.err.println("Cannot write replay " + ex);}
		}
	}

	public synchronized byte[] saveRedo(String conf){
		LinkedList tempUndo = undoList;
		undoList = new LinkedList();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean oldRec = recordingOn;
		recordingOn = true;
		save(baos, conf);
		recordingOn = oldRec;
		undoList = tempUndo;
		return baos.toByteArray();	  
	}

	public synchronized byte[] saveUndo(String conf){
		LinkedList tempRedo = redoList;
		redoList = new LinkedList();
		untilMark("undo", "markthisdoesnotexist");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean oldRec = recordingOn;
		recordingOn = true;
		save(baos, conf);
		recordingOn = oldRec;
		untilMark("redo", "markthisdoesnotexist");
		redoList = tempRedo;
		return baos.toByteArray();	  
	}

	private String saveReplay(LinkedList ll){
		String s = "";
		for(int i = 0; i < ll.size(); i++){
			UndoPage up = (UndoPage) ll.get(i);
			s = s + up.translate();
		}
		return s;
	}

	public String savePage(int i){
		Page outp = (Page) jpages.get(i);
		return outp.save(null);
	}

	public void readBgsList(String s, String fname){
		String y = Tools.getEntry(s, "[Background List]");
		if(y == null) return;
		Hashtable ht = Tools.readConf(y);
		for(Enumeration e = ht.keys(); e.hasMoreElements();){
			String bghandle = (String) e.nextElement();
			y = Tools.getEntry(s, "[Background " + bghandle + "]");
			if(y != null){
				Background bgs = (Background) bgsList.get(bghandle);
				if(bgs == null){
					String ss = Tools.getLine(y, "source");
					if(ss != null){
						if(!(new File(ss)).exists()){
							String rs = Tools.getLine(y, "rsource");
							ss = (new File(fname)).getParent() + File.separator + rs;
						}
						bgs = Background.create(new JbgsSource(ss,null));
						bgsList.put(bghandle, bgs);
					}
				}
				if(bgs != null) {
					boolean doMake = bgs.setConf(y);
					if(doMake) bgs.make();
				}
			}
		}
	}

	public String saveBgsList(String cwd){
		String s = "[Background List]\n";
		for(Enumeration e = bgsList.keys(); e.hasMoreElements();){
			String bghandle = (String) e.nextElement();
			Background bgs = (Background) bgsList.get(bghandle);
			if((bgs.astate != 0) || recordingOn) s = s + bghandle + "=" + bghandle + "\n";
		}
		s = s + "\n";
		for(Enumeration e = bgsList.keys(); e.hasMoreElements();){
			String bghandle = (String) e.nextElement();
			Background bgs = (Background) bgsList.get(bghandle);
			if((bgs.astate != 0) || recordingOn){
				s = s + "[Background " + bghandle + "]\n";
				s = s + bgs.getConf(!saveBg, cwd, portableBgs) + "\n";
			}
		}
		return s + "\n";
	}

	private void saveError(String str){
		JOptionPane.showConfirmDialog(null, "File could not be saved.\n" + str, "Error", JOptionPane.DEFAULT_OPTION);
	}		

	public synchronized boolean save(String fname, String conf){
		boolean isSaved = true;

		try{
			FileOutputStream out = new FileOutputStream(fname);
			isSaved = save(out, conf);
		}
		catch(IOException ex){ex.printStackTrace(); saveError("" + ex); isSaved = false;}
		return isSaved;
	}

	public void saveDic(String userDic, String dicName){
		FileOutputStream fos = null;
		try{
			fos = new FileOutputStream(userDic);
		}
		catch(Exception ex){fos = null; saveError("Problem saving dictionary:" + dicName + "\nas: " + userDic + "\n" + ex);}
		Analyze.saveStream(fos, dicName);
	}

	public String getBgsName(){
		if(!bgs().isPdf) return null;
		//if(current.getPaper().bgtext) return null;
		return bgs().getSource().getName();
	}

	public byte[] getBackground(String bghandle){
		Background bgs = (Background) bgsList.get(bghandle);
		if(bgs == null) return null;
		if(bgs.getSource().getName().equals("")) return null;
		InputStream is = bgs.getSource().getInputStream();
		return streamToByteArray(is);
	}		

	public synchronized boolean save(OutputStream out, String conf){
		boolean isSaved = true;
		String str;
		try{
			
			ZipOutputStream zip = new ZipOutputStream(out);
			Page outp;
			ZipEntry subname = new ZipEntry("jarnal.conf");
			zip.putNextEntry(subname);
			zip.write(conf.getBytes());

			String alltext = copyAllText();
			subname = new ZipEntry("alltext.txt");
			zip.putNextEntry(subname);
			zip.write(alltext.getBytes());		

			String outXML = outline.getXML();
			if(outXML != null){
				//System.out.print(outXML);
				subname = new ZipEntry("outline.xml");
				zip.putNextEntry(subname);
				zip.write(outXML.getBytes());
			}
			if(saveBg){
				for(Enumeration e = bgsList.keys(); e.hasMoreElements();){
					String bghandle = (String) e.nextElement();
					Background bgs = (Background) bgsList.get(bghandle);
					boolean saveBg = true;
					if(bgs.getSource().getName() == "") saveBg = false;
					if(!recordingOn){
						if(bgs.astate == 0) saveBg = false;
					}
 					if(saveBg){
						subname = new ZipEntry(bghandle);
						zip.putNextEntry(subname);
						if(!portableBgs){
							InputStream in3 = bgs.getSource().getInputStream();
							if(in3 == null){
								System.err.println("This isn't supposed to happen: null background stream");
							}
							else{
								int nmax = 1000000; 
								byte b[] = new byte[nmax];
								int nread = 0;
								while((nread = in3.read(b)) >= 0){
									zip.write(b, 0, nread);
								}
							}
						}
						else{
							ZipOutputStream pzip = new ZipOutputStream(zip);
							for(int ipz = 0; ipz < bgs.size(); ipz++){
								ZipEntry ze = new ZipEntry("pbg" + ipz + ".png");
								pzip.putNextEntry(ze);
								float sc = 1.0f;
								if(bgs.isPdf) {
									sc = 2.0f;
								}
								Image im = bgs.getScaledBg(ipz, sc, 0, 0, Color.white);
								BufferedImage bi = null;
								try{
									bi = (BufferedImage) im;
								}
								catch(Exception ex){
									bi = bgs.rewriteBI(im, 1.0f, 1, 0, Color.white);
								}
								ImageIO.write(bi, "png", (OutputStream) pzip);
							}
							pzip.finish();						
						}
					}
				}
			}
			Hashtable ht = new Hashtable();
			for(int i = 0; i < jpages.size(); i++){
				subname = new ZipEntry("p" + i + ".svg");
				zip.putNextEntry(subname);
				outp = (Page) jpages.get(i);
				str = outp.save(ht);
				zip.write(str.getBytes());
			}
			for(Enumeration e = scraps.keys(); e.hasMoreElements();){
				String scrapName = (String) e.nextElement();
				subname = new ZipEntry(scrapName);
				zip.putNextEntry(subname);
				zip.write((byte[]) scraps.get(scrapName));
			}
			for(Enumeration e = extras.keys(); e.hasMoreElements();){
				String extraName = (String) e.nextElement();
				subname = new ZipEntry(extraName);
				zip.putNextEntry(subname);
				try{
					zip.write(getExtra(extraName));
				}
				catch(Exception ex){System.out.println("error writing extra file: " + extraName);}
			}
			if(recordingOn){
				subname = new ZipEntry("undostack.replay");
				zip.putNextEntry(subname);
				saveReplay(zip, undoList);
				subname = new ZipEntry("redostack.replay");
				zip.putNextEntry (subname);
				saveReplay(zip, redoList);
			}
			zip.close();
		}
		catch(IOException ex){ex.printStackTrace(); saveError("" + ex); isSaved = false;}
		return isSaved;
	}

	public void removeBg(){
		String bgid = current.getPaper().bgid;
		if(bgsList.get(bgid) == null) return;
		int oldcindex = cindex;
		setStartMark();
		for(int cindex = 0; cindex < jpages.size(); cindex++){
			current = (Page) jpages.get(cindex);
			if(current.getPaper().bgid.equals(bgid)){
				Jpaper pp = current.getPaper().copy();
				pp.bgid = "none";
				setPaper(pp);
			}
		}
		cindex = oldcindex;
		current = (Page) jpages.get(cindex);
					
		undo = new UndoPage();
		undo.cindex = cindex;
		undo.op = "bgremove";
		undo.data = bgid;
		pushUndo(undo);
		((Background) bgsList.get(bgid)).unmake();
		invalidateGraphics();
		putdo(true);
		setEndMark();
	}

	public void insertBg(JbgsSource s, String action){
		setStartMark();
		if(action.equals("Insert Background Before")) pageBefore("");
		else pageAfter("");
		undo = new UndoPage();
		undo.cindex = cindex;
		undo.op = "bgopen";
		String bghandle = s.createHandle();
		undo.data = bghandle;
		pushUndo(undo);
		Background xbgs = Background.create(s);
		xbgs.make();
		bgsList.put(bghandle, xbgs);
		putdo(true);
		Jpaper pp = current.getPaper().copy();
		pp.bgid = bghandle;
		pp.showBg = 1;
		setPaper(pp);
		bgindex(0);
		setSizeToBg();
		padPages(true);
		xbgs.setOutline(outline, bghandle);
		invalidateGraphics();
		setEndMark();
	}		

	public void openBg(JbgsSource s){
		setStartMark();
		if(current.bgIsSet()){
			undo = new UndoPage();
			undo.cindex = cindex;
			undo.op = "bgremove";
			undo.data = current.getPaper().bgid;;
			pushUndo(undo);
		}
		undo = new UndoPage();
		undo.cindex = cindex;
		undo.op = "bgopen";
		String bghandle = s.createHandle();
		undo.data = bghandle;
		pushUndo(undo);
		Background xbgs = Background.create(s);
		xbgs.make();
		bgsList.put(bghandle, xbgs);
		putdo(true);
		if(current.bgIsSet()){
			int oldcindex = cindex;
			String oldhandle = current.getPaper().bgid;
			for(cindex = 0; cindex < jpages.size(); cindex++){
				current = (Page) jpages.get(cindex);
				Jpaper pp = current.getPaper().copy();
				if(pp.bgid.equals(oldhandle)){
					pp.bgid = bghandle;
					setPaper(pp);
				}
			}
			cindex = oldcindex;
			current = (Page) jpages.get(cindex);
			((Background)bgsList.get(oldhandle)).unmake();					
		}
		else{
			Jpaper pp = current.getPaper().copy();
			pp.bgid = bghandle;
			pp.showBg = 1;
			setPaper(pp);
			//setScale(getScale());
			bgindex(0);
			setSizeToBg();
			padPages(true);
		}
		xbgs.setOutline(outline, bghandle);
		invalidateGraphics();
		setEndMark();
	}

	private void padPages(){
		if(jpages.size() == 0){
			Page jp = new Page(this);
			jp.setPaper(defaultPaper.copy());
			jpages.add(jp);	
		}
		//the next code makes sure that older files are updated to have bgindexes pointing
		//to the right place
		int pi = 0;
		for(int ii = 0; ii < jpages.size(); ii++){
			Page jp = (Page) jpages.get(ii);
			if(jp.bgindex() == -1) jp.bgindex(pi);
			else pi = jp.bgindex();
			pi++;
		}	
	}

	private void padPages(boolean setUndo){
		if(bgs().isRepeating) return;
		int pad = bgs().size() - 1;
		int oldcindex = cindex;
		for(int ii = 0; ii < pad; ii++) {
			pageAfter("");
			setSizeToBg();
			current.getPaper().showBg = 1;
		}
		cindex = oldcindex;
		current = (Page) jpages.get(cindex);
		currentScale();		
	}

	public void initOpenBg(JbgsSource s){
		//old style background
		String bghandle = s.createHandle();
		Background xbgs = Background.create(s);
		xbgs.make();
		bgsList.put(bghandle, xbgs);
		for(int ii = 0; ii < jpages.size(); ii++){
			Page jp = (Page) jpages.get(ii);
			Jpaper pp = jp.getPaper();
			if(pp.bgid.equals("none")) pp.bgid = bghandle;
		}
		xbgs.setOutline(outline, bghandle);
	}

	public static BufferedImage toBufferedImage(Image image){
        	if (image instanceof BufferedImage) return (BufferedImage)image;
        	image = new ImageIcon(image).getImage();
        	BufferedImage bimage = null;
		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        	try {
			pg.grabPixels();
        	} catch (InterruptedException ex) {System.err.println(ex); bimage = null; return bimage;}
		ColorModel cm = pg.getColorModel();
		boolean hasAlpha = cm.hasAlpha();
        	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        	try {
			int transparency = Transparency.OPAQUE;
            		if (hasAlpha) transparency = Transparency.BITMASK;
			GraphicsDevice gs = ge.getDefaultScreenDevice();
            		GraphicsConfiguration gc = gs.getDefaultConfiguration();
            		bimage = gc.createCompatibleImage(
                	image.getWidth(null), image.getHeight(null), transparency);
        	} catch (HeadlessException ex) {System.err.println(ex); bimage = null; return bimage;}
    	        if (bimage == null) {
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) type = BufferedImage.TYPE_INT_ARGB;
            		bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        	}
        	Graphics g = bimage.createGraphics();
	        g.drawImage(image, 0, 0, null);
	        g.dispose();
	        return bimage;
	}

	public String addScrapImage(Image im){
		String scrapName = "image" + (new Random()).nextInt();
		BufferedImage sc = toBufferedImage(im);
		if(sc == null) return null;
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		try{
			ImageIO.write(sc, "png", bao);
		}
		catch(Exception ex){System.err.println(ex); return null;}
		byte[] bb = bao.toByteArray();
		scraps.put(scrapName, bb);
		scrapsbi.put(scrapName, sc);
		undo = new UndoPage();
		undo.op = "addScrapImage";
		undo.cindex = cindex;
		undo.data = scrapName;
		pushUndo(undo);
		putdo(true);
		return scrapName;
	}


	public boolean addScrapImage(String scrapName, byte[] bb){
		ImageIO.scanForPlugins();
		BufferedImage sc = null;
		try{
			sc = ImageIO.read(new ByteArrayInputStream(bb));
		}
		catch(Exception ex){System.err.println(ex); sc = null;}
		if(sc == null) return false;
		scraps.put(scrapName, bb);
		scrapsbi.put(scrapName, sc);
		return true;
	}		

	public String addScrapImage(InputStream is, String name){
		String scrapName = "image" + (new Random()).nextInt() + "." + name;
		byte[] bb= streamToByteArray(is);
		if(!addScrapImage(scrapName, bb)) return null;
		undo = new UndoPage();
		undo.op = "addScrapImage";
		undo.cindex = cindex;
		undo.data = scrapName;
		pushUndo(undo);
		putdo(true);
		return scrapName;
	}

	//this logic is used over and over, really it would be better to replace them all
	//with a call to this function
	public static byte[] streamToByteArray(InputStream is){
		try{
			int nmin = 1000000;
			int nborg = 40000;
			int nmax = nmin + ( 5 * nborg); 
			byte b[] = new byte[nmax];
			int nread = 0;
			int noff = 0;
			ByteArrayOutputStream baost = new ByteArrayOutputStream();
			while((nread = is.read(b, noff, nborg)) >= 0){
				noff = noff + nread;
				if(noff > nmax - (2 * nborg)){
					baost.write(b, 0, noff);
					noff = 0;
				}
			}
			baost.write(b, 0, noff);
			return baost.toByteArray();
		}
		catch(Exception ex){ex.printStackTrace();}
		return null;
	}

	public InputStream pipeStream(String fname){
		InputStream in = null;
		if(HtmlPost.checkURL(fname)){
			try{
				URL url = new URL(fname);
      				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				in = conn.getInputStream();	
			}
			catch(Exception ex){ex.printStackTrace();}
		}
		if((in == null) && !Jarnal.getInstance().isApplet) {
			try{
				in = new FileInputStream(fname);
			}
			catch(Exception ex){ex.printStackTrace();}
		}
		return in;
	}

	private LinkedList openReplay(String str){
		LinkedList ll = new LinkedList();
		String instr = str;
		while(instr != null){
			UndoPage up = new UndoPage();
			instr = up.translate(instr, this);
			if(up.op != null) ll.add(up);
		}		
		return ll;
	}
	
	public String open(String fname){
		boolean updateBackground = false;
		String oldbghandle = "";
		String conf = "";
		jpages = new LinkedList();
		String outXML = null;
		try{
			InputStream in = pipeStream(fname);
			if(in == null) throw new IOException();
			ZipInputStream zip = new ZipInputStream(in);
			for(ZipEntry ze = zip.getNextEntry(); ze != null; ze = zip.getNextEntry()){
				int nmin = 1000000;
				int nborg = 40000;
				int nmax = nmin + ( 5 * nborg); 
				byte b[] = new byte[nmax];
				int nread = 0;
				String instr = "";
				int noff = 0;

				ByteArrayOutputStream baost = new ByteArrayOutputStream();

				while((nread = zip.read(b, noff, nborg)) >= 0){
					noff = noff + nread;
					if(noff > nmax - (2 * nborg)){
						baost.write(b, 0, noff);
						noff = 0;
					}
				}
				baost.write(b, 0, noff);
				byte c[] = baost.toByteArray();
				instr = new String(c);
				if(ze.getName().equals("jarnal.conf")) conf = instr;
				else if(ze.getName().equals("outline.xml")) outXML = instr;
				else {
					String bghandle = ze.getName();
					if(bghandle.startsWith("background")){
						if(c.length > 0){
							String bgfname = "none";
							if(!Jarnal.getInstance().isApplet){
								File tfile = File.createTempFile("jarnalBg", ".tmp");
								bgfname = tfile.getPath();
								System.out.println(bgfname);
								//tfile.deleteOnExit();
								FileOutputStream tout = new FileOutputStream(tfile);
								tout.write(c);
								c = null;
							}
							JbgsSource jbgsS = new JbgsSource(bgfname,c);
							if(bghandle.equals("background")){
								//old style background
								bghandle = jbgsS.createHandle();
								oldbghandle = bghandle;
								updateBackground = true;
							}
							Background xbgs = Background.create(jbgsS);
							//we only need to do this for compability with old style background files - new style will get made when we read the background list later on - but it harmless to do a make twice in a row
							xbgs.make();
							bgsList.put(bghandle, xbgs);
						}	
					}
					else {
						String test = ze.getName();
						boolean isPage = (test.length() >= 6);
						if(isPage){
							isPage = isPage && (test.substring(0,1).equals("p"));
							int nsvg = test.indexOf(".svg");
							if(nsvg >= 2){
								test = test.substring(0, nsvg);
								try{ Integer.parseInt(test.substring(1)); }
								catch(NumberFormatException nfe){isPage = false;}
							}
							else isPage = false;
						}
						if(isPage){
							current = new Page(this);
							jpages.add(current);
							current.open(instr);
						}
						else{
							test = ze.getName();
							if(test.substring(0,5).equals("image")){
								addScrapImage(test, c);
							}
							else if(test.equals("undostack.replay")){
								undoList = openReplay(instr);
								recordingOn(true);
							}
							else if(test.equals("redostack.replay")){
								redoList = openReplay(instr);
								recordingOn(true);
							}
							else if(test.equals("alltext.txt")){}
							else{
								addExtra(ze.getName(), c);
							}
						}
					}
				}
			}		
		}
		catch(Exception ex){ex.printStackTrace();}
		padPages();
		current = (Page) jpages.get(0);
		if(updateBackground){
			for(int ii = 0; ii < jpages.size(); ii++){
				Page jp = (Page) jpages.get(ii);
				Jpaper pp = jp.getPaper();
				if(pp.bgid.equals("none")) pp.bgid = oldbghandle;
			}
		}
		readBgsList(conf, fname);
		outline.setOutline(outXML);
		return conf;
	}

	public void insert(String fname, String action){
		if(Jarnal.getInstance().isApplet) return;
		exitPage();
		clearRedoList();
		setStartMark();
		//if(pageSelected() && !textMode) pageDelete();
		//else cindex = cindex + 1;
		if(action.equals("Insert Jarnal After")) cindex = cindex + 1;
		int firstPage = cindex;
		boolean updateBackground = false;
		String oldbghandle = "";
		String conf = "";
		int npages = 0;
		String outXML = null;
		try{
			InputStream in = pipeStream(fname);
			if(in == null) throw new IOException();
			ZipInputStream zip = new ZipInputStream(in);
			for(ZipEntry ze = zip.getNextEntry(); ze != null; ze = zip.getNextEntry()){
				int nmin = 1000000;
				int nborg = 40000;
				int nmax = nmin + ( 5 * nborg); 
				byte b[] = new byte[nmax];
				int nread = 0;
				String instr = "";
				int noff = 0;

				ByteArrayOutputStream baost = new ByteArrayOutputStream();

				while((nread = zip.read(b, noff, nborg)) >= 0){
					noff = noff + nread;
					if(noff > nmax - (2 * nborg)){
						baost.write(b, 0, noff);
						noff = 0;
					}
				}
				baost.write(b, 0, noff);
				byte c[] = baost.toByteArray();
				instr = new String(c);
				String bghandle = ze.getName();
				if(ze.getName().equals("jarnal.conf")) conf = instr;
				else if(ze.getName().equals("outline.xml")) outXML = instr;
				else if(bghandle.startsWith("background")){
					if((c.length > 0) && (bgsList.get(bghandle) == null)){
						String bgfname = "none";
						File tfile = File.createTempFile("jarnalBg", ".tmp");
						bgfname = tfile.getPath();
						System.out.println(bgfname);
						//tfile.deleteOnExit();
						FileOutputStream tout = new FileOutputStream(tfile);
						tout.write(c);
						c = null;
						JbgsSource jbgsS = new JbgsSource(bgfname,c);
						if(bghandle.equals("background")){
							//old style background
							bghandle = jbgsS.createHandle();
							oldbghandle = bghandle;
							updateBackground = true;
						}
						Background xbgs = Background.create(jbgsS);
						//only needed for compability with old style background files
						xbgs.make();
						bgsList.put(bghandle, xbgs);
					}
				}	
				else {
					String test = ze.getName();
					boolean isPage = (test.length() >= 6);
					if(isPage){
						isPage = isPage && (test.substring(0,1).equals("p"));
						int nsvg = test.indexOf(".svg");
						if(nsvg >= 2){
							test = test.substring(0, nsvg);
							try{ Integer.parseInt(test.substring(1)); }
							catch(NumberFormatException nfe){isPage = false;}
						}
						else isPage = false;
						if(isPage){
							pageBefore(instr);
							cindex++;
							npages++;
						}
						else{
							test = ze.getName();
							if(test.substring(0,5).equals("image")){
								addScrapImage(test, c);
							}
							else if(test.equals("redostack.replay")){
								redoList = openReplay(instr);
								for(int ii = 0; ii < redoList.size(); ii++){
									UndoPage up = (UndoPage) redoList.get(ii);
									up.cindex = up.cindex + firstPage;
								}
								recordingOn(true);
							}
						}
					}
				}
			}		
		}
		catch(Exception ex){ex.printStackTrace();}
		if(updateBackground){
			for(int ii = firstPage; ii < firstPage + npages; ii++){
				Page jp = (Page) jpages.get(ii);
				Jpaper pp = jp.getPaper();
				if(pp.bgid.equals("none")) pp.bgid = oldbghandle;
			}
		}
		readBgsList(conf, fname);
		outline.setOutline(outXML);
		setEndMark();
		cindex--;
		current = (Page) jpages.get(cindex);
	}

	public void openLoad(LinkedList loadFiles){
		for(int ii = 0; ii < loadFiles.size(); ii++){
			String uname = (String) loadFiles.get(ii);
			InputStream in = pipeStream(uname);
			if(in != null) {
				ByteArrayOutputStream baost = new ByteArrayOutputStream();
				int nmin = 1000000;
				int nborg = 40000;
				int nmax = nmin + ( 5 * nborg); 
				byte b[] = new byte[nmax];
				int nread = 0;
				int noff = 0;
				try{
					while((nread = in.read(b, noff, nborg)) >= 0){
						noff = noff + nread;
						if(noff > nmax - (2 * nborg)){
							baost.write(b, 0, noff);
							noff = 0;
						}
					}
					baost.write(b, 0, noff);
				}
				catch(Exception ex){ex.printStackTrace(); baost = null;}
				if(baost != null){
					int nn = uname.lastIndexOf(File.separator);
					if(nn >= 0) uname = uname.substring(nn+1);
					addExtra(uname, baost.toByteArray());				
				}				
			}
		}
		
	}
}

class memoryMan{
	public Pages jp;
	public Background bg;
	public int pg;

	memoryMan(Pages jp, Background bg, int pg){
		this.jp = jp;
		this.bg = bg;
		this.pg = pg;
	}

	static void testMem(){
		if(Jarnal.getInstance().isApplet) return;
		//System.out.println("bgCache=" + Jbgs.globalbgCache.size() + " pgCache=" + Jpages.globalGraphics.size());
		Runtime rt = Runtime.getRuntime();
		float test = (float)rt.freeMemory() + (float)Tools.maxMemory() - (float)rt.totalMemory();
		test = 100.0f - (100.0f *test/(float) Tools.maxMemory());
		if(test < 40.0f) {
			Background.globalbgCacheLimit = -1;
			Pages.globalGraphicsLimit = -1;
			return;	
		}
		if(test > 80.0f) {
			Background.globalbgCacheLimit = Background.globalbgCache.size();
			Pages.globalGraphicsLimit = Pages.globalGraphics.size();
		}
		else{
			int n = Background.globalbgCache.size();
			if(n < Pages.globalGraphics.size()) n = Pages.globalGraphics.size();
			Background.globalbgCacheLimit = n;
			Pages.globalGraphicsLimit = n;		
		}
		if(Background.globalbgCacheLimit <= 1) Background.globalbgCacheLimit = 2;
		if(Pages.globalGraphicsLimit <= 1) Pages.globalGraphicsLimit = 2;
	}
}

class UndoPage{
	public int cindex;
	public boolean top = true;
	public String op;
	public Object data;
	public int oindex;
	public boolean skiptext = false;
	public static final String terminator = "<<)rp?\n\r\n";

	public UndoPage copy(){
		UndoPage up = new UndoPage();
		up.cindex = cindex;
		up.top = top;
		up.op = op;
		up.data = data;
		up.oindex = oindex;
		up.skiptext = skiptext;
		return up;
	}
	//undo data appears to belong to one of the following classes
	//String Jpage Jstroke Jpaper
	public String translate(){
		String strt = op + "\n" + top + "\n" + skiptext + "\n" + cindex + "\n" + oindex + "\n";
		String ans = "nu\n";
		if(data != null){
			String test = data.getClass().getName();
			if(test.equals("jarnal.Jpage"))
				ans = "pg\n" + ((Page) data).save(null);
			if(test.equals("jarnal.Jstroke") || test.equals("jarnal.Jtext") || test.equals("jarnal.Jscrap"))
				ans = "sr\n" + ((BrushStroke) data).save(null);
			if(test.equals("jarnal.Jpaper"))
				ans = "pp\n" + ((Jpaper) data).getConf();
			if(test.equals("java.lang.String"))
				ans = "st\n" + (String) data;
			if(test.equals("java.lang.Long"))
				ans = "ln\n" + ((Long) data).longValue();
			if(test.equals("java.util.LinkedList")){
				ans = "ll\n";
				LinkedList ll = (LinkedList) data;
				ans = ans + (String) ll.get(0) + "\n";
				byte bb[] = (byte[]) ll.get(1);
				ans = ans + b64.encode(bb);
			}
		}
		return strt + ans + terminator;
	}
	String ww;
	private String nxt(){
		int pos = ww.indexOf("\n");
		if(pos < 0) return null;
		String ans = ww.substring(0, pos).trim();
		ww = ww.substring(pos + 1);
		return ans;
	}
	public String translate(String str, Pages parent){
		int pos = str.indexOf(terminator);
		if(pos < 0) return null;
		ww = str.substring(0, pos);
		str = str.substring(pos + terminator.length());
		op = nxt();
		top = true;
		if(nxt().equals("false")) top = false;
		skiptext = true;
		if(nxt().equals("false")) skiptext = false;
		String test = nxt();
		if(!test.equals("")) cindex = Integer.parseInt(test);
		test = nxt();
		if(!test.equals("")) oindex = Integer.parseInt(test);
		test = nxt();
		if(test.equals("pg")){
			Page jp = new Page(parent);
			jp.open(ww);
			data = jp;
		}
		//the parent jpage of the stroke will get set by redo
		if(test.equals("sr")) {
			data = ww;
			int oldcindex = cindex;
			data = (new Page(parent)).parseStroke(this);
			//parseStroke for some reason resets the cindex
			cindex = oldcindex;
		}
		if(test.equals("pp")) {
			Jpaper jpp = new Jpaper();
			jpp.setConf(ww);
			data = jpp;
		}
		if(test.equals("st")) data = ww;
		if(test.equals("ln")) data = new Long(Long.parseLong(ww));
		if(test.equals("ll")){
			LinkedList ll = new LinkedList();
			ll.add(nxt());
			ll.add(b64.decode(ww));
			data = ll;
		}			
		return str;
	}
}

class recorderTimerListener implements ActionListener{

	Pages jpa;

	public recorderTimerListener(Pages jpa){
		this.jpa = jpa;
	}

	public void actionPerformed(ActionEvent e) {
		jpa.timerFlag = true;
	}
}




