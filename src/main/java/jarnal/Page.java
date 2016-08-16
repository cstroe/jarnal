package jarnal;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

class Jpaper{
	public static int WHITE = Color.white.getRGB();
	public static int dpi = 84;
	public static int adpi = 72;
	public static int standardHeight = 861;
	public int height = 861;
	public int width = 714;
	public int nlines = 25;
	public String paper = "Lined";
	public int showBg = 1;
	public int transparency = 255;
	public int bcolor = WHITE;
	public String bgid = "none";
	public float bgscale = 1.0f;
	public int bgrotate = 0;
	public int bgfade = 0;
	public boolean bgtext = false;
	public int bgindex = -1;
	public static int lighter(int n){
		float lmd = 0.3f;
		int ans = (int)((lmd * n) + ((1.0f - lmd) * WHITE));
		return ans;
	}
	public String getConf(){
		return "paper=" + paper + "\nlines=" + nlines + "\nheight=" + height + "\nwidth=" + width + "\nbg=" + showBg + "\ntransparency=" + transparency + "\nbcolor=" + bcolor + "\nbgtext=" + bgtext + "\nbgfade=" + bgfade + "\nbgrotate=" + bgrotate + "\nbgscale=" + bgscale + "\nbgid=" + bgid + "\nbgindex=" + bgindex + "\n";
	}
	private String formatDec(float x){
		String y = "" + x;
		int n = y.indexOf(".");
		if(n < 0) return y + ".00";
		n = n + 3;
		if(n > y.length()) {
			y = y + "0";
			n = y.length();
		}
		return y.substring(0, n);
	}	
	public String getDesc(boolean absoluteScale){
		int vdpi = dpi;
		if(absoluteScale) vdpi = adpi;
		return "" + formatDec((float)width/vdpi) + "\" X " + formatDec((float)height/vdpi) + "\"";
	}
	public void setConf(String s){
		String z = Tools.getLine(s, "paper");
		if(z != null) paper = z;
		z = Tools.getLine(s, "lines");
		if(z != null) nlines = Integer.parseInt(z);
		z = Tools.getLine(s, "height");
		if(z != null) height = Integer.parseInt(z);
		z = Tools .getLine(s, "width");
		if(z != null) width = Integer.parseInt(z);
		z = Tools.getLine(s, "bg");
		if(z != null) showBg = Integer.parseInt(z);
		z = Tools.getLine(s, "transparency");
		if(z != null) transparency = Integer.parseInt(z);
		z = Tools.getLine(s, "bcolor");
		if(z != null) bcolor = Integer.parseInt(z);
		z = Tools.getLine(s, "bgtext");
		if(z != null){
			if(z.equals("true")) bgtext = true;
		}
		z = Tools.getLine(s, "bgscale");
		if(z != null) bgscale = Float.parseFloat(z);
		z = Tools.getLine(s, "bgrotate");
		if(z != null) bgrotate = Integer.parseInt(z);
		z = Tools.getLine(s, "bgfade");
		if(z != null) bgfade = Integer.parseInt(z);
		z = Tools.getLine(s, "bgid");
		if(z != null) bgid = z;
		z = Tools.getLine(s, "bgindex");
		if(z != null) bgindex = Integer.parseInt(z);
	}
	public Jpaper copy(){
		Jpaper p = new Jpaper();
		p.height = height;
		p.width = width;
		p.nlines = nlines;
		p.paper = paper;
		p.showBg = showBg;
		p.transparency = transparency;
		p.bcolor = bcolor;
		p.bgtext = bgtext;
		p.bgscale = bgscale;
		p.bgrotate = bgrotate;
		p.bgfade = bgfade;
		p.bgid = bgid;
		p.bgindex = bgindex;
		return p;
	}
}

class Page{

	static String header = "<?xml version=\"1.0\" standalone=\"no\"?>\n<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 20010904//EN\" \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">\n";
	static String middle = "xmlns=\"http://www.w3.org/2000/svg\">\n<title>Jarnal document - see http://www.dklevine.com/general/software/tc1000/jarnal.htm for details</title>\n";
	static String footer = "</svg>";
	static long seed = (new Random()).nextLong();

	public Pages parent = null;
	public LinkedList strokes = new LinkedList();
	private LinkedList dragList;
	private boolean overlaySelected = false;
	int dragOp = 0;
	private Point2D.Double dragStart;
	private Point2D.Double drag;
	private Point2D.Double startL;
	private BrushStroke current;
	private int itext = -1;
	private Point2D.Double ptext;
	private Point2D.Double ptext2 = null;
	private Point2D.Double ptext3 = null;
	private float textPadding;
	private boolean dragCorner;
	private Jscrap dragImage = null;
	public jrnlPDFWriter pdfWriter = null;
	public String pageref = null;

	private Jpaper ppr = new Jpaper();

	public int bgindex(){
		return ppr.bgindex;
	}
	public String bgid(){
		return ppr.bgid;
	}
	public void bgindex(int bindex){
		ppr.bgindex = bindex;
		if(bindex == -2){
			ppr.bgindex = 0;
			ppr.showBg = 0;
		}
	}

	public void showBg(int i){
		ppr.showBg = i;
	}

	public int showBg(){
		return ppr.showBg;
	}		

	private float scale = 1;
	private Tools tools;
	public int print = 0;
	public boolean withBorders = true;
	Graphics2D gg;
	private Line2D.Double border;
	BasicStroke bs;

	private void setPtext(){
		float x = (ppr.standardHeight * scale) / ppr.nlines;
		ptext = new Point2D.Double(2.2 * x, 3 * x);
	}

	public int getX(){
		float x = (ppr.standardHeight * scale) / ppr.nlines;
		return (int)(2.2 * x);
	}

	public Page(Pages p){
		parent = p;
		strokes.add(new BrushStroke(this));
		setPtext();
		resetpageref();
	}

	public void resetpageref(){
		pageref = "pageref" + (new Random(seed)).nextInt();
		seed = (new Random(seed)).nextLong();
	}

	public void clearSel(){
		ctext().clearSel();
	}

	public Jtext ctext(){
		if((itext == -1) || (itext >= strokes.size())) return null;
		BrushStroke js = (BrushStroke) strokes.get(itext);
		if(!js.isText) {
			itext = -1;
			return null;
		}
		return (Jtext) js;
	}

	private boolean highlight;

  	public Page copy() {
		Page res = new Page(parent);
		res.strokes.remove(0);
		res.ppr = ppr.copy();
		res.pageref = pageref;
		for (Iterator i = strokes.iterator(); i.hasNext(); )
			res.strokes.add(((BrushStroke)i.next()).copy(res));
		return res;
	}

	public void setParent(Pages jps){
		parent = jps;
	}

	public int getHeight(){ 
		if(parent == null) return (int) (ppr.height * scale);
		if(parent.PO >= 1.0) return (int) (ppr.height * scale);
		return (int) (ppr.height * scale * parent.PO);
	}
	public int getWidth(){ return (int) (ppr.width * scale);}
	public int getBaseWidth(){ return ppr.width;}
	public int getBaseHeight(){
		if(parent == null) return ppr.height;
		if(parent.PO >= 1.0) return ppr.height;
		return (int) (ppr.height * parent.PO);
	}
	public float getScale(){return scale;}
	public JarnalSelection clipText(){ if(ctext() != null) return ctext().clip(); return null;}

	public boolean pageSelected(){
		if(lastDrag == 76) return false;
		if(dragList == null) return true;
		if(dragList.size() == 0) return true;
		return false;
	}

	public boolean overlaySelected(){
		return overlaySelected;
	}

	public boolean dragShape(){
		if(dragCorner) return true;
		if(dragImage != null) return true;
		return false;
	}

	public boolean dragImage(){
		if(dragImage != null) return true;
		return false;
	}
	
	private String getHeader(){
		return header + "<svg width=\"" + getBaseWidth() + "px\" height=\"" + getBaseHeight() + "px\" " + middle + "<desc>\n[Jarnal Page Parameters]\n" + ppr.getConf() + "pageref=" + pageref + "\n</desc>\n";
	}

	public void setScale(float scale){
		this.scale = scale;
	}

	public void setPaper(Jpaper p){
		ppr = p;
	}

	public Jpaper getPaper(){
		return ppr;
	}

	public int getTransparency(){
		return ppr.transparency;
	}

	public String getAllText(Hashtable ht, boolean html){
		String all = "";
		LinkedList ll = new LinkedList();
		for(int i = 0; i < strokes.size(); i++){
			BrushStroke js = (BrushStroke) strokes.get(i);
			if(js.isText) {
				Jtext jt = (Jtext) js;
				boolean now = false;
				int j = 0;
				for(j = 0; j < ll.size(); j++){
					Jtext jj = (Jtext) ll.get(j);
					if(jt.corner.getY() < jj.corner.getY()) now = true;
					if((jt.corner.getY() == jj.corner.getY()) && (jt.corner.getX() < jj.corner.getX())) now = true;
					if(now) break;
				}
				ll.add(j, jt);
			}
		}
		for(int j = 0; j < ll.size(); j++) {
			if(!html){
				String test = ((Jtext)ll.get(j)).getText(ht);
				if(!all.equals("") && !test.equals("")) all = all + "\n\n" + test;
				else all = all + test;
			}
			else {
				String test = ((Jtext)ll.get(j)).getHtml(ht);
				if(!all.equals("") && !test.equals("")) all = all + "<hr>" + test;
				else all = all + test;
			}
		}
		return all;
	}

	private boolean isText(int i){
		BrushStroke js = (BrushStroke) strokes.get(i);
		return js.isText;
	}

	public boolean find(String targ, boolean findFirst, boolean reverse, boolean entire, boolean matchCase, boolean wholeWord){
		int itarg = itext;
		Jtext jtx;
		boolean newFind = findFirst;
		if(!entire){
			if(itarg == -1){
				newFind = true;
				for(int i = 0; i < strokes.size(); i++){
					if(isText(i)) {
						itarg = i;
						break;
					}
				}
			}
			if(itarg == -1) return false;
			jtx = (Jtext) strokes.get(itarg);
			itext = itarg;
			return jtx.find(targ, newFind, reverse, matchCase, wholeWord);
		}
		int dir = 1;
		if(reverse) dir = -1;
		if((itarg == -1) || findFirst){
			itarg = 0;
			if(reverse) itarg = strokes.size() - 1;
			newFind = true;
		}
		while(true){
			if(isText(itarg)){
				jtx = (Jtext) strokes.get(itarg);
				boolean found = jtx.find(targ, newFind, reverse, matchCase, wholeWord);
				findFirst = false;
				if(found) {
					if((itext != -1) && (itext != itarg)) ctext().clearSel(true);
					itext = itarg;
					return true;
				}
			}
			itarg = itarg + dir;
			newFind = true;
			if((itarg < 0) || (itarg >= strokes.size())) return false;
		}
	}		

	public BrushStroke undo(boolean top){
		if(strokes.size() == 0) return null;
		BrushStroke swap;
		if(top) swap = (BrushStroke) strokes.removeLast();
		else swap = (BrushStroke) strokes.remove(0);
		return swap;
	}

	public String putdo(boolean top){
		if(strokes.size() == 0) return "";
		BrushStroke swap;
		if(top) swap = (BrushStroke) strokes.getLast();
		else swap = (BrushStroke) strokes.get(0);
		return swap.save(null);
	}

	public void redo(UndoPage undo){
		BrushStroke swap = (BrushStroke) undo.data;
		swap.page = this;
		if(undo.top) strokes.add(swap);
		else strokes.add(0, swap);
	}

	public int getText(){
		return itext;
	}

	public String getDesc(){
		Jtext jt = ctext();
		if(jt == null) return "";
		if(itext > -1) return jt.getDesc();
		return null;
	}

	public String[] getTextStyle(){
		Jtext jt = ctext();
		if(jt == null) return null;
		if(itext > -1) return jt.getTextStyle();
		return null;
	}		

	public String getHtmlDesc(){
		Jtext jt = ctext();
		if(jt == null) return "";
		if(itext > -1) return jt.getHtmlDesc();
		return null;
	}

	public Jchar getCurParms(){
		if(itext > -1) return ctext().getCurParms();
		return null;
	}

	public void setParms(Jchar jc){
		if(itext == -1) return;
		ctext().setParms(jc);
	}

	public Jchar getFinalParms(){
		if(ctext() == null) itext = -1;
		if(itext > -1) return ctext().getFinalParms();
		return null;
	}

	public void unselectText(){
		itext = -1;
	}

	public void endDragOp(){
		if((dragOp == 4) || (dragOp == 5)){
			for (Iterator i = strokes.iterator(); i.hasNext(); ){
				BrushStroke js = (BrushStroke) i.next();
				if(dragOp == 4){
					if(!js.below((int) dragStart.getY())) i.remove();
				}
				if(dragOp == 5){
					if(js.below((int) dragStart.getY())) i.remove();
				}
			}
		}
		dragOp = 0;
	}

	private void setPageSize(Point2D.Double p){
		ppr.width =  (int)(7 * Math.floor(p.getX()/(7 * scale)));
		ppr.height = (int)(7 * Math.floor(p.getY()/(7 * scale)));
	}

	Point2D.Double cornDrag;
	Point2D.Double dispDrag;
	Point2D.Double rsize;
	int xmax = 0;
	int xmin = 0;
	int ymax = 0;
	int ymin = 0;
	int lastDrag;

	public int[] getDragRectX(int xx[], int offX){
		if(lastDrag == 77){
			int x = (int) dispDrag.getX();
			xmin = xmin + x;
			xmax = xmax + x;
		}
		else if(lastDrag == 78) xmax = (int) dragStart.getX();
		xx[0] = xmin + offX;
		xx[1] = xmax + offX;
		return xx;
	}
	public int[] getDragRectY(int yy[], int offY){
		if(lastDrag == 77){
			int y = (int) dispDrag.getY();
			ymin = ymin + y;
			ymax = ymax + y;
			cornDrag = new Point2D.Double(-xmin, -ymin);
		}
		else if(lastDrag == 78) ymax = (int) dragStart.getY();
		yy[0] = ymin + offY;
		yy[1] = ymax + offY;
		return yy;
	}

	public void startDragRect(Point2D.Double x[]){
		dragList = new LinkedList();
		overlaySelected = false;
		//int xmax; int xmin; int ymax; int ymin;
		if(x[0].getX() > x[1].getX()){
			xmax = (int) x[0].getX();
			xmin = (int) x[1].getX();
		}
		else {
			xmax = (int) x[1].getX();
			xmin = (int) x[0].getX();
		}
		if(x[0].getY() > x[1].getY()){
			ymax = (int) x[0].getY();
			ymin = (int) x[1].getY();
		}
		else {
			ymax = (int) x[1].getY();
			ymin = (int) x[0].getY();
		}
		cornDrag = new Point2D.Double(-xmin, -ymin);
		dispDrag = new Point2D.Double(0,0);
		rsize = new Point2D.Double(xmax - xmin, ymax - ymin);
		for (Iterator i = strokes.iterator(); i.hasNext(); ){
			BrushStroke js = (BrushStroke) i.next();							
			if(js.below(ymin) && js.above(ymax) && js.left(xmax) && js.right(xmin)) dragList.add(js);
		}
		lastDrag = 76;
	}

	public void setDragOp(int op){
		dragOp = op;
	}

	public void platter(BrushStroke jsp){
		boolean addStroke = false;
		int xmax = (int)(jsp.xmax * scale);
		int xmin = (int)(jsp.xmin * scale);
		int ymax = (int)(jsp.ymax * scale);
		int ymin =  (int)(jsp.ymin * scale);
		for (Iterator i = strokes.iterator(); i.hasNext(); ){
			BrushStroke js = (BrushStroke) i.next();							
			if(addStroke && js.below(ymin) && js.above(ymax) && js.left(xmax) && js.right(xmin)) dragList.add(js);
			if(js ==jsp) addStroke = true;
		}
	}

	public void startDragOp(int op){
		dragOp = op;
		lastDrag = op;
		dispDrag = new Point2D.Double(0,0);
		dragStart = startL;
		//dragOp=77 is the rectangular select, which was already set
		//dragOp=78 is the rectangular select also - for distortion
		if((dragOp != 77) && (dragOp != 78)) {
			dragList = new LinkedList();
			overlaySelected = false;
		}
		//select tool, drag what we find
		if(dragOp == 2){
			for(int i = strokes.size() - 1; i >= 0; i--){
				BrushStroke js = (BrushStroke) strokes.get(i);
				if(js.hitZ(dragStart)) {
					dragList.add(js);
					i = -1;
					if(js.isOverlay) {
						overlaySelected = true;
						platter(js);
					}
				}
			}
		}
		//razor, drag everything below
		if((dragOp == 1) || (dragOp == 5)){
			cornDrag = new Point2D.Double(0, -startL.getY());
			if(dragOp == 5) cornDrag = new Point2D.Double(0,0);
			for(int i = 0; i < strokes.size(); i++){
				BrushStroke js = (BrushStroke) strokes.get(i);
				if(dragOp == 1)
					if(js.below((int) dragStart.getY())) dragList.add(js);
				if(dragOp == 5)
					if(js.above((int) dragStart.getY())) dragList.add(js);
			}
		}
		if(dragOp == 13) setPageSize(dragStart);
	}

	public LinkedList eraseDragList(){
		overlaySelected = false;
		LinkedList res = new LinkedList();
		int ns = dragList.size();
		for(int ii = 0; ii < ns; ii++){
			BrushStroke brushStroke = (BrushStroke) dragList.remove(0); 
			res.addFirst(brushStroke.getRectangle());
			strokes.remove(brushStroke);
		}
		return res;
	}

	public Rectangle smooth(){
		BrushStroke js = current;
		if(js == null) js = (BrushStroke) strokes.getLast();
		return js.smooth(false);
	}

	public void applyArrow(int wt){
		if(dragList == null) return;
		for(int ii = 0; ii < dragList.size(); ii++){
			BrushStroke js = (BrushStroke) dragList.get(ii);
			js.setMarker(wt);
		}		
	}
		

	public void applyPen(String color, String hicolor, float zscale, float hiscale, String trans, Tools jt){
		if(dragList == null) return;
		for(int ii = 0; ii < dragList.size(); ii++){
			BrushStroke js = (BrushStroke) dragList.get(ii);
			js.applyPen(color, hicolor, zscale, hiscale, trans, js.isHighlight, jt);
		}		
	}

	public Rectangle2D.Double getDragRect(){
		if(xmin < 0) return null;
		if(ymin < 0) return null;
		if(xmin >= xmax) return null;
		if(ymin >= ymax) return null;
		if(xmax > scale * ppr.width) return null;
		//if(ymax > scale * ppr.height) return null;
		if(ymax > scale * getBaseHeight()) return null;
		return new Rectangle2D.Double((int)(xmin/scale), (int)(ymin/scale), (int)((xmax-xmin)/scale), (int)((ymax-ymin)/scale));
	}		

	public String copyDragList(){
		if((cornDrag == null) && (lastDrag !=2)) return null;
		boolean pageMarker = false;
		LinkedList tempDrag = new LinkedList();
		Page jp = new Page(parent);
		jp.setScale(scale);
		if(lastDrag == 2){
			BrushStroke js = (BrushStroke) dragList.getLast();
			cornDrag = new Point2D.Double(-dragStart.getX(), -dragStart.getY());
		}
		else if((lastDrag == 77) || (lastDrag == 76) || (lastDrag == 78))
			cornDrag = new Point2D.Double(cornDrag.getX() - dispDrag.getX() - rsize.getX()/2, cornDrag.getY() - dispDrag.getY() - rsize.getY()/2);
		else cornDrag = new Point2D.Double(cornDrag.getX() - dispDrag.getX(), cornDrag.getY() - dispDrag.getY());
		for(int ii = 0; ii < dragList.size(); ii++){
			BrushStroke js = (BrushStroke) dragList.get(ii);
			BrushStroke jsc = js.copy(jp);
			jsc.offset(cornDrag);
			if(!js.isHighlight && !pageMarker){
				tempDrag.add(new BrushStroke(jp)); //the page marker
				pageMarker = true;
			}
			tempDrag.add(jsc);
		}
		if(!pageMarker) tempDrag.add(new BrushStroke(jp));
		jp.strokes = tempDrag;
		return jp.save(null);		
	}

	public void pasteList(Page jp, Point2D.Double p){
		boolean isHighlight = true;
		LinkedList bothi = new LinkedList();
		dragList = new LinkedList();
		overlaySelected = false;
		for(int ii = 0; ii < jp.strokes.size(); ii++){
			BrushStroke js = (BrushStroke) jp.strokes.get(ii);
			js.page = this;
			if(js.isPage) {
				//the bottom strokes have to go in the correct order
				isHighlight = false;
				for(int jj = 0; jj < bothi.size(); jj++) strokes.add(0, bothi.get(jj));
			}
			else{
				js.offset(p);
				if(isHighlight) bothi.add(0, js);
				else strokes.add(js);
				dragList.add(js);
			}
		}
	}

	public String getlink(){
		BrushStroke js = (BrushStroke) dragList.getLast();
		if(js == null) return "";
		return js.getlink();
	}
	public void setlink(String str){
		BrushStroke js = (BrushStroke) dragList.getLast();
		if(js == null) return;
		js.setlink(str);
	}				

	public LinkedList dragOp(Point2D.Double p){
		LinkedList res = new LinkedList();
		if(dragList == null) return res;
		Point2D.Double sve = p;
		//drag the selection list
		if((dragOp < 3) || (dragOp == 5) || (dragOp == 77) || (dragOp == 78)){
			if((dragOp == 1) || (dragOp == 5)) p = new Point2D.Double(0, p.getY() - dragStart.getY());
			else p = new Point2D.Double(p.getX() - dragStart.getX(), p.getY() - dragStart.getY()); 
			dispDrag = new Point2D.Double(dispDrag.getX() + p.getX(), dispDrag.getY() + p.getY());
			for(int i = 0; i < dragList.size(); i++){
				BrushStroke js = (BrushStroke) dragList.get(i);
				Rectangle h = js.getRectangle();
				if(dragOp == 78) {
					sve = js.distort(cornDrag, sve, dragStart);
					//sve = dragStart;
				}
				else js.offset(p);
				Rectangle hh = js.getRectangle();
				if(js.isText) {
					float z = 2 * textPadding;
					h = new Rectangle((int)(h.getX()-z), (int)(h.getY()-z), (int)(h.width+(2*z)), (int)(h.height+(2*z)));
					hh = new Rectangle((int)(hh.getX()-z), (int)(hh.getY()-z), (int)(hh.width+(2*z)), (int)(hh.height+(2*z)));	
					if(js == ctext()) ptext = ctext().corner;	
				}
				//h.add(hh);
				res.addFirst(hh);
				res.addFirst(h);
			} 
		}
		//the eraser
		if(dragOp == 3){
			itext = -1;
			for (Iterator i = strokes.iterator(); i.hasNext(); ){
				BrushStroke brushStroke = (BrushStroke) i.next(); 
				if(brushStroke.hit(dragStart)) {
					i.remove();
					res.addFirst(brushStroke.getRectangle());
				}
			}	
		}
		//precision eraser
		if(dragOp == 33){
			itext = -1;
			for(int i = strokes.size() - 1; i >= 0; i--){
				BrushStroke brushStroke = (BrushStroke) strokes.get(i); 
				if(brushStroke.hitZ(dragStart)) {
					strokes.remove(i);
					res.addFirst(brushStroke.getRectangle());
					i = -1;
				}
			}	
		}
		if(dragOp == 13) {
			if (p.getX() > dragStart.getX())
				res.addFirst(new Rectangle((int)Math.floor(getWidth())-1,0, (int)Math.floor(p.getX()-getWidth())+2, (int)Math.floor(p.getY())+2));
			if (p.getY() > dragStart.getY())
				res.addFirst(new Rectangle(0,(int)Math.floor(getHeight())-1, (int)Math.floor(p.getX())+2, (int)Math.floor(p.getY()-getHeight())+2));
			setPageSize(p);
			if (p.getX() <= dragStart.getX())
				res.addFirst(new Rectangle((int)Math.floor(getWidth())-1,0,2,(int)Math.floor(getHeight())+1));
			if (p.getY() <= dragStart.getY())
				res.addFirst(new Rectangle(0,(int)Math.floor(getHeight())-1, (int)Math.floor(getWidth())+1,2));
		}
		if ((dragOp == 1) || (dragOp == 4) || (dragOp == 5)) {
			res.addFirst(new Rectangle(0,(int)Math.floor(dragStart.getY()-bs.getLineWidth()/2.0), (int)Math.floor(getWidth())+2, (int)Math.floor(bs.getLineWidth())+2));
			res.addFirst(new Rectangle(0,(int)Math.floor(sve.getY()-bs.getLineWidth()/2.0), (int)Math.floor(getWidth())+2, (int)Math.floor(bs.getLineWidth())+2));
		}
		dragStart = sve;
		return res;
	}
	
	public void startStroke(Point2D.Double startL, Tools jt){
		this.startL = startL;
		current = null;
		tools = jt;	
	} 

	public void addScrap(Point2D.Double p, String scrapName){
		current = new Jscrap(this, scrapName, p);
		strokes.add(current);
		dragList = new LinkedList();
		overlaySelected = false;
		dragList.add(current);
	}

	public Rectangle dragText(int x, int y){
		if((ctext() == null) && (dragImage == null)) return null;
		if(!dragCorner && (dragImage == null)) return fixR(ctext().dragText(x, y, scale));
		if(dragCorner && dragWidth) return fixR(ctext().setWidth((int)((float) x / scale)));
		if(dragCorner && (!dragWidth)) return fixR(ctext().setHeight((int)((float) y / scale)));
		return dragImage.rescale((int)(x/scale), (int)(y/scale));
	}

	public void addOverlay(Point2D.Double x[], String ostyle){
		current = new Joverlay(this, x[0], x[1], ostyle);
		strokes.add(current);
		//if(dragList == null) 
		dragList = new LinkedList();
		//if(dragList.isEmpty()){
		dragList.add(current);
		overlaySelected = true;
		//}
	}

	public int getOverlayInt(String defaultStyle, int index){
		Joverlay jove = new Joverlay(this, defaultStyle);
		if(overlaySelected && (dragList.size() > 0)){
			BrushStroke js = (BrushStroke) dragList.get(0);
			if(js.isOverlay) jove = (Joverlay) js;
		}
		return jove.getInt(index);
	}

	public String getOverlayStyle(String defaultStyle){
		Joverlay jove = new Joverlay(this, defaultStyle);
		if(overlaySelected && (dragList.size() > 0)){
			BrushStroke js = (BrushStroke) dragList.get(0);
			if(js.isOverlay) jove = (Joverlay) js;
		}
		return jove.getStyle();
	}

	public void makeOverlaySquare(String defaultStyle){
		Joverlay jove = new Joverlay(this, defaultStyle);
		if(overlaySelected && (dragList.size() > 0)){
			BrushStroke js = (BrushStroke) dragList.get(0);
			if(js.isOverlay) jove = (Joverlay) js;
		}
		jove.makeSquare();
	}
				

	public String setOverlayStyle(String defaultStyle, int arcWidth, int arcHeight, String fillColor, String strokeColor, int strokeWidth, int fillFade, int strokeFade){
		Joverlay jove = new Joverlay(this, defaultStyle);
		if(overlaySelected && (dragList.size() > 0)){
			BrushStroke js = (BrushStroke) dragList.get(0);
			if(js.isOverlay) jove = (Joverlay) js;
		}
		jove.setStyle(arcWidth, arcHeight, fillColor, strokeColor, strokeWidth, fillFade, strokeFade);
		return jove.getStyle();
	}

	public void setTextHeight(int y){
		ctext().setHeight((int)((float) y / scale));
	}		

	public String hitImage(){
		for(int i = strokes.size() - 1; i >= 0; i--){
			BrushStroke brushStroke = (BrushStroke) strokes.get(i);
			if(brushStroke.isImage){
				Jscrap js = (Jscrap) brushStroke;
				if(js.hit(startL)){
					return js.getClip();
				}
			}
		}
		return null;
	}

	public void selectAllText(){
		if(ctext() == null) itext = -1;
		if(itext == -1) return;
		ctext().selectAll();
	}

	public boolean collapseSel(){
		if(ctext() == null) itext = -1;
		if(itext == -1) return false;
		return ctext().collapseSel();
	}

	public void reText(){
		if(ctext() == null) itext = -1;
		if(itext == -1) return;
		String test = ctext().getText();
		if(test.equals("")) return;
		itext = -1;
		setPtext();
	}

	private boolean isCorner(Point2D.Double ptext2){
		boolean isCorner = true;
		double x = scale * ptext2.getX();
		double y = scale * ptext2.getY();
		double fark = (double) scale;
		if(fark > 1.0) fark = 1.0;
		double z = (double) (textPadding/fark);
		if(startL.getX() < x - z) isCorner = false;
		if(startL.getX() > x + z) isCorner = false;
		if(startL.getY() < y - z) isCorner = false;
		if(startL.getY() > y + z) isCorner = false;
		if(isCorner){
			dragCorner = true;
			return true;
		}
		return false;
	}

	boolean dragWidth;
	Point2D.Double sptext = null;

	public Rectangle startText(Point2D.Double startL){
		dragCorner = false;
		dragImage = null;
		Point2D.Double zsptext = new Point2D.Double(startL.getX()/scale,startL.getY()/scale);
		sptext = null;
		if(ptext2 != null) if(isCorner(ptext2)) {
			dragWidth = true;
			return null;
		}
		if(ptext3 != null) if(isCorner(ptext3)) {
			dragWidth = false;
			return null;
		}
		for(int i = strokes.size() - 1; i >= 0; i--){
			BrushStroke brushStroke = (BrushStroke) strokes.get(i);
			if(brushStroke.isText){
				Jtext jt = (Jtext) brushStroke;
				if(jt.hit(startL)){
					if(itext == i){
						ptext = jt.hitSel(startL, scale);
						return jt.getTextRectangle();
					}
					if(ctext() != null) {
						ctext().clearSel();
					}
					itext = i;
					ptext = jt.hitSel(startL, scale);
					return null;	
				}
			}
			if(brushStroke.isImage){
				Jscrap js = (Jscrap) brushStroke;
				if(js.hit(startL)){
					dragImage = (Jscrap) brushStroke;
					sptext = zsptext;
					return null;
				}
			}
		}
		if(ctext() != null) {
			ctext().clearSel();
		}
		itext = -1;
		//ptext = startL;
		//ptext = new Point2D.Double(startL.getX()/scale,startL.getY()/scale);
		ptext = zsptext;
		return null;
	}

	public void clickText(){
		if(sptext == null) return;
		ptext = sptext;
		itext = -1;
	}

	public void setArrow(int wt){
		if(current != null) current.setMarker(wt);
	}

	public boolean stroke(Point2D.Double endL){
		if(startL != null){
			if(current == null){
				current = new BrushStroke(this, tools);
				if(!tools.highlighter) strokes.add(current);
				else {
					strokes.add(0,current);
					if(itext > -1) itext++;
					current.isHighlight = true;
				}
			}	
			current.add(startL, endL);
			startL = endL;
			return true;
		}
		return false;
	}

	public String analyzeAll(Tools jt, Jchar parms){
		String str = "";
		if(ctext() == null) selctext(jt, parms);
		boolean train = ctext().janal(true).train;
		ctext().janal(true).train = false;		
		for(int i = 0; i < strokes.size(); i++){
			BrushStroke js = (BrushStroke) strokes.get(i);
			String temp = js.analyze(ctext().janal(true));
			if(temp != null) {
				String test = temp.substring(0,1);
				while(test.equals("\n")){
					int nn = str.length() - 1;
					str = str.substring(0, nn);
					temp = temp.substring(1);
					test = temp.substring(0,1);
				}
				str = str + temp;
			}
		}
		ctext().janal(true).train = train;
		return str;
	}

	public int getLastns(){
		if(itext == -1) return 0;
		return ctext().getLastns();
	}

	private String doanalyze(BrushStroke js, Tools jt, Jchar parms, boolean defaultfilepairs){
		if(ctext() == null) selctext(jt, parms);
		return js.analyze(ctext().janal(defaultfilepairs));
	}

	public Analyze getanalyze(Tools jt, Jchar parms){
		if(ctext() == null) selctext(jt, parms);
		return ctext().janal(false);
	}

	public String analyze(Tools jt, Jchar parms, boolean defaultfilepairs){
		if(current == null) return null;
		return doanalyze(current, jt, parms, defaultfilepairs);
	}

	public String analyzeClick(Tools jt, Jchar parms, boolean defaultfilepairs){
		BrushStroke js = (BrushStroke) strokes.getLast();
		return doanalyze(js, jt, parms, defaultfilepairs);
	}

	public Rectangle adv(int adv, int extend){
		Jtext jt = ctext();
		if(jt == null) return null;
		return jt.adv(adv, extend);
	}

	public boolean setCurrent(Tools jt, Jchar parms){
		if(ctext() != null) return false;
		selctext(jt, parms);
		return true;
	}

	public void newText(Tools jt, Jchar parms, Point2D.Double p){
		ptext = new Point2D.Double((int)(p.getX()/scale), (int)(p.getY()/scale));
		selctext(jt, parms);
	}

	public float getY(){
		if(ctext() == null) return (int) ptext.getY();
		return ctext().getY();
	}

	private int cwidth(){
		Point2D.Double p = ptext;
		int marg = 2 * ppr.standardHeight/ppr.nlines;
		int cwidth = (int)(ppr.width - p.getX());
		if(cwidth > 4 * marg) cwidth = cwidth - marg;
		return cwidth;
	}

	private Jtext selctext(Tools jt, Jchar parms){
		Point2D.Double p = ptext;
		//int cwidth = (int) (ppr.width - p.getX() - (2 * ppr.standardHeight/ppr.nlines));
		strokes.add(new Jtext(this, p, jt, cwidth(), parms));
		itext = strokes.size() - 1;
		return ctext();
	}

	public boolean findFrame(String frameId){
		for(int i = 0; i < strokes.size(); i++){
			BrushStroke js = (BrushStroke) strokes.get(i);
			if(js.isText){
				if(((Jtext) js).matchFrame(frameId)){
					itext = i;
					return true;
				}
			}
		}
		return false;
	}

	public void textSplit(){
		if(ctext() != null) ctext().split();
	}

	public int textWidth(){return ctext().getWidth();}
	public int textHeight(){return ctext().getHeight();}

	public String checkFrame(int advance){
		if(ctext() == null) return null;
		return ctext().checkFrame(advance);
	}

	public void textJoin(Tools jt, Jchar parms, Jtext jtext, int w, int h, int direc){
		if(ctext() == null) selctext(jt, parms);
		ctext().join(jtext, w, h, direc);
	}

	public String typeKey(String str, Tools jt, Jchar parms){
		if(ctext() == null){
			selctext(jt, parms).typeKey(str);
			//Point2D.Double p = ptext;
			//int cwidth = (int) (ppr.width - p.getX() - (2 * ppr.standardHeight/ppr.nlines));
			//strokes.add(new Jtext(this, p, jt, cwidth, parms));
			//itext = strokes.size() - 1;
			//ctext().typeKey(str);
			return null;
		}
		return ctext().typeKey(str);
	}

	public String setSelStyle(boolean bold, boolean italic, boolean underline, Float size, String font, String color, Jchar parms){
		if(ctext() == null){
			Point2D.Double p = ptext;
			//int cwidth = (int) (ppr.width - p.getX() - (2 * ppr.standardHeight/ppr.nlines));
			strokes.add(new Jtext(this, p, parent.tools, cwidth(), parms));
			itext = strokes.size() - 1;
			ctext().setSelStyle(bold, italic, underline, size, font, color);
			return null;
		}
		return ctext().setSelStyle(bold, italic, underline, size, font, color); 
	}

	public String putText(){
		if(ctext() == null) return "";
		return ctext().save(null);
	}

	public void resetPage(){
		for(ListIterator i = strokes.listIterator(); i.hasNext();) ((BrushStroke) i.next()).reset();
	}

	public String undoText(int oindex, String str){
		itext = oindex;
		if(ctext() == null) return "";
		//String oldXML = ctext().save(null);
		String oldXML = ctext().makeX(str);
		return oldXML;
	}

	public void click(Point2D.Double endL, Tools jt){
		if(!jt.highlighter) strokes.add(new BrushStroke(this, jt, endL));
		else {
			BrushStroke jse = new BrushStroke(this, jt, endL);
			strokes.add(0, jse);
			itext++;
			jse.isHighlight = true;
		}
	}

	private Rectangle fixR(Rectangle r){
		int z = (int) textPadding;
		return new Rectangle((int) r.getX() - z, (int) r.getY() - z, r.width + (2*z), r.height + (4 * z));
	}

	private void drawPaper(Graphics2D g2){
		float swidth = 0.5f;
		bs = new BasicStroke(swidth);
		g2.setStroke(bs);

		float x = (ppr.standardHeight * scale) / ppr.nlines;
		float y = 2 * x;
		textPadding = x/5;

		if(ppr.paper.equals("Lined")){
			Point2D.Double lmu = new Point2D.Double((double) y, 0.0);
			border = new Line2D.Double((double) y, 0.0 , (double) y, (double)getHeight());
			g2.setPaint(Color.orange);
			g2.draw(border);
			border = new Line2D.Double((double) (y + x/4), 0.0 , (double) (y + x/4), (double)getHeight());
			g2.draw(border);
		}
		if(ppr.paper.equals("Ruled")){
			border = new Line2D.Double(0.0, 2 * x, (double) getWidth(), 2 * x);
			g2.setPaint(Color.orange);
			g2.draw(border);
		}
		if(ppr.paper.equals("Lined") || ppr.paper.equals("Graph") || ppr.paper.equals("Ruled")){
			int start = 3;
			if(ppr.paper.equals("Graph")) start = 1;
			g2.setPaint(Color.cyan);
			int i = start;
			while((i * x) < getHeight()){
				border = new Line2D.Double(0.0, i * x, (double) getWidth(), i * x);
				g2.draw(border);
				i++;
			}
		}
		if(ppr.paper.equals("Graph")){
			g2.setPaint(Color.cyan);
			g2.setStroke(new BasicStroke(2*swidth));
			int i = 1;
			while((i * x) < getWidth()){
				border = new Line2D.Double(i * x, 0.0, i * x,(double) getHeight());
				g2.draw(border);
				i++;
			}
			g2.setStroke(bs);
		}

		if((print == 0) || withBorders){
			g2.setPaint(Color.gray);
			Point2D.Double lu = new Point2D.Double(0.0, 0.0);
			Point2D.Double lb = new Point2D.Double(0.0, (double)(getHeight() - 1));
			Point2D.Double ru = new Point2D.Double((double)(getWidth() - 1), 0.0);
			Point2D.Double rb = new Point2D.Double((double)(getWidth() - 1), (double)(getHeight() - 1));
			border = new Line2D.Double(lu, ru);
			g2.draw(border);
			border = new Line2D.Double(lu, lb);
			g2.draw(border);
			border = new Line2D.Double(ru, rb);
			g2.draw(border);
			border = new Line2D.Double(lb, rb);
			g2.draw(border);
		}
		if((dragOp == 1) || (dragOp == 4) || (dragOp == 5)) border = new Line2D.Double(0.0, dragStart.getY(), (double)(getWidth() -1), dragStart.getY());
	}

	private void drawTextMark(Graphics2D g2, Point2D.Double pt){
		float swidth = 0.5f;
		bs = new BasicStroke(swidth);
		g2.setStroke(bs);
		g2.setPaint(Color.gray);
		float z = textPadding;
		Point2D.Double p = new Point2D.Double(pt.getX() * scale, pt.getY() * scale);
		Point2D.Double first = new Point2D.Double(p.getX() - z, p.getY());
		Point2D.Double second = new Point2D.Double(p.getX() + z, p.getY());
		Line2D.Double border = new Line2D.Double(first, second);
		g2.draw(border);	
		first = new Point2D.Double(p.getX(), p.getY() - z);
		second = new Point2D.Double(p.getX(), p.getY() + z);
		border = new Line2D.Double(first, second);
		g2.draw(border);
	}

	public Rectangle drawLast(){
		if(gg == null) return new Rectangle(0,0,1,1);
		BrushStroke brushStroke = (BrushStroke) strokes.getLast();
		brushStroke.draw(gg, print);
		return brushStroke.getRectangle();
	}

	public Rectangle forceTextRectangle(){
		if((itext == -1)) return null;
		Jtext ctext = ctext();
		if(ctext == null) return null;
		return fixR(ctext.getTextRectangle());
	}

	public Rectangle getOldTextRectangle(){
		if((itext == -1) || (gg == null)) return null;
		Jtext ctext = ctext();
		if(ctext == null) return null;
		return fixR(ctext.getTextRectangle());
	}

	public Rectangle getTextRectangle(){
		if(gg == null) return new Rectangle(0,0,1,1);
		return fixR(ctext().getTextRectangle());
	}

	private void drawTextMarks(Graphics2D g2){
		if(print == 0){
			drawTextMark(g2, ptext);
			if(ctext() != null){
				ptext2 = new Point2D.Double(ptext.getX() + ctext().getWidth(), ptext.getY());
				drawTextMark(g2, ptext2);
				if(ctext().isFrame()){
					ptext3 = new Point2D.Double(ptext.getX(), ptext.getY() + (2 * textPadding) + ctext().getHeight());
					drawTextMark(g2, ptext3);
				}
				else ptext3 = null;
			}
			else ptext2 = null;
		}
	}

	public boolean bgIsSet(){
		if(ppr.showBg == 0) return false;
		String tgid = ppr.bgid;
		if(tgid.equals("none")) return false;
		if(parent.bgs(tgid) == null) return false;
		return true;
	}

	public boolean bgVisible(){
		if(ppr.showBg == 0) return false;
		String tgid = ppr.bgid;
		if(tgid.equals("none")) return false;
		if(parent.bgs(tgid) == null) return false;
		if(!parent.bgs(tgid).haveBg(ppr.bgindex)) return false;
		return true;
	}

	int bq;

	public void drawBg(Graphics2D g2){
		drawBg(g2, bq);
	}
		
	public void drawBg(Graphics2D g2, int bq){
		if((pdfWriter != null) && (ppr.bgfade == 0)){
			if(pdfWriter.checkPDF(parent.bgs(ppr.bgid)) && !bgVisible()) return;
			if(pdfWriter.writeBg(parent.bgs(ppr.bgid), scale * ppr.bgscale, ppr.bgrotate, ppr.bgindex))
				return;
		}
		this.bq = bq;
		int pi = ppr.bgindex;
		String tgid = ppr.bgid;
		Color cc = new Color(ppr.bcolor);
		if(!cc.equals(Color.white)){
			int cr = cc.getRed();
			int cg = cc.getGreen();
			int cb = cc.getBlue();
			cc = Color.white;
			int wr = cc.getRed();
			int wg = cc.getGreen();
			int wb = cc.getBlue();
			float zeta = 0.5f;
			float zeta1 = 1.0f - zeta;
			zeta = zeta/255.0f;
			zeta1 = zeta1/255.0f;
			cc = new Color((zeta * cr) + (zeta1 * wr), (zeta * cg) + (zeta1 * wg), (zeta * cb) + (zeta1 * wb));
		}
		g2.setColor(cc);
		//g2.fillRect(0, 0, (int)(ppr.width * scale), (int)(ppr.height * scale));
		g2.fillRect(0, 0, (int)(ppr.width * scale), (int)(getBaseHeight() * scale));
		if(!bgVisible()) return;
		AffineTransform at = new AffineTransform();
		float fscale = scale * ppr.bgscale;
		if(ppr.bgrotate != 0){
			double theta = Math.PI * 0.5; // * ppr.bgrotate;
			double hh = parent.bgs(tgid).getHeight(pi) * fscale * 0.5;
			double ww = parent.bgs(tgid).getWidth(pi) * fscale * 0.5;
			int r = ppr.bgrotate;
			for(int i = 1; i <= r; i++){
				double x = hh;
				if(i == 2) x = ww;
				AffineTransform bt = AffineTransform.getRotateInstance(theta, x, x);
				at.preConcatenate(bt);
			}
		}
		parent.bgs(tgid).writeBg(g2, at, pi, print, bq, fscale, ppr.bgfade, cc);
	}
	
	public void setGraphics2D(Graphics2D g2){
		gg = g2;
	}

	public void draw(Graphics2D g2, int x, int y, int w, int h){
		gg = g2;
		g2.setClip(x,y,w,h);
		drawBg(g2);
		if(!Pages.highlightLines) drawPaper(g2);
		for (Iterator i = strokes.iterator(); i.hasNext(); ){
			BrushStroke brushStroke = (BrushStroke) i.next();
			brushStroke.pdfWriter = pdfWriter;
	    		if(brushStroke.overlaps(x, y, w, h)) brushStroke.draw(g2, print);
			if(brushStroke.isPage && Pages.highlightLines) drawPaper(g2);
		}
		drawTextMarks(g2);
		if((dragOp == 1) || (dragOp == 4) || (dragOp == 5))	{
			g2.setStroke(bs);
			g2.setPaint(Color.red);
			g2.draw(border);
		}
		g2.setClip(null);
	}	

	public void draw(Graphics2D g2, int bq){
		gg = g2;
		drawBg(g2, bq);
		if(!Pages.highlightLines) drawPaper(g2);
		for (Iterator i = strokes.iterator(); i.hasNext(); ){
			BrushStroke brushStroke = (BrushStroke) i.next();
			brushStroke.pdfWriter = pdfWriter;
	    		if(brushStroke.draw(g2, print) && Pages.highlightLines) drawPaper(g2);
		}
		drawTextMarks(g2);
		if((dragOp == 1) || (dragOp == 4) || (dragOp == 5))	{
			g2.setStroke(bs);
			g2.setPaint(Color.red);
			g2.draw(border);
		}		
	}

	public String save(Hashtable ht){
		if(ht == null) ht = new Hashtable();
		String str = getHeader();
		for (Iterator i = strokes.iterator(); i.hasNext(); ){
			BrushStroke brushStroke = (BrushStroke) i.next();
			str = str + brushStroke.save(ht);	
		}
		str = str + footer;
		return str;
	}

	public void open(String str){
		highlight = true;
		int pos = str.indexOf("<desc>");
		if(pos == -1) return;
		String strstr = str.substring(pos);
		if(pos >= 0){
			pos = strstr.indexOf("</desc>");
			if(pos >= 0) {
				strstr = strstr.substring(0,pos) + "\n";
				String z = Tools.getLine(strstr, "pageref");
				if(z != null) pageref = z;
				ppr.setConf(strstr);
			}
		}
		strokes = new LinkedList();
		pos = str.indexOf("\n\n\n");
		if(pos > 0) {
			strstr = str.substring(0, pos);
			str = str.substring(pos + 1);
			openStrokes(strstr);
		}
		strokes.add(new BrushStroke(this));
		highlight = false;
		openStrokes(str);
	}

	// parse the first XML stroke in up.data
	// construct a Jstroke based on it, and return it.
	// update up.data to point past the Jstroke we parsed.
	
	// used in two places - openStrokes() loads XML into a page
	// and getdo() converts XML on an undo list into a JStroke

	// Brent Baccala modified this so as to look for for str.indexOf("<")
	// then used str.startsWith("<path") etc. to find what the element is
	// this is probably more robust and easier to maintain

	public BrushStroke parseStroke(UndoPage up){
		String str = (String) up.data;
		BrushStroke current;
		boolean circle = false;
		boolean text = false;
		boolean image = false;
		boolean rect = false;

		int pos = str.indexOf("<path");
		int posc = str.indexOf("<circle");
		String strstr;
		if((pos == -1) || ((posc < pos) && (posc != -1))) {
			pos = posc;
			circle = true;
		}
		posc = str.indexOf("<image");
		if((pos == -1) || ((posc < pos) && (posc !=-1))){
			pos = posc;
			image = true;
		}
		posc = str.indexOf("<rect");
		if((pos == -1) || ((posc < pos) && (posc !=-1))){
			pos = posc;
			rect = true;
			image = false;
		}
		posc = str.indexOf("<text");
		if((pos == -1) || ((posc < pos) && (posc != -1))){
			pos = posc;
			text = true;
		}
		posc = str.indexOf("<marker");
		if((posc != -1) && (posc < pos)){
			posc = str.indexOf("</marker");
			if(posc < 0) return null;
			str = str.substring(posc);
			up.data = str;
			return parseStroke(up);
		}
		if(pos < 0) return null;
		String link = null;
		posc = str.indexOf("<a");
		if((posc < pos) && (posc != -1)){
			posc = str.indexOf("xlink:href");
			if((posc < pos) && (posc != -1)){
				String temp = str.substring(posc);
				posc = temp.indexOf("\"");
				if(posc != -1){
					temp = temp.substring(posc + 1);
					posc = temp.indexOf("\"");
					if(posc != -1){
						link = temp.substring(0, posc);
					}
				}
			}		
		}
		str = str.substring(pos);
		if(!text){
			int last = str.indexOf("/>");
			if(last < 0) return null;
			strstr = str.substring(0, last);
			str = str.substring(last + 2);
			float sscale = scale;
			scale = 1.0f;
			if(image) current = new Jscrap(this, strstr);
			else if(rect) current = new Joverlay(this, strstr);
			else current = new BrushStroke(this, strstr, circle);
			scale = sscale;
			current.isHighlight = highlight;
		}
		else {
			int last = str.indexOf("</text>");
			if(last < 0) return null;
			strstr = str.substring(0, last);
			str = str.substring(last + 6);				
			current = new Jtext(this, strstr);
		}
		up.data = str;
		up.cindex = pos;
		if(link != null) current.setlink(link);
		return current;
	}

	private void openStrokes(String str){
		scale = 1.0f;
		UndoPage up = new UndoPage();
		up.cindex = 0;
		up.data = str;
		while(up.cindex >= 0){
			BrushStroke test = parseStroke(up);
			if(test == null) return;
			current = test;
			strokes.add(current);
		}
	}
}

