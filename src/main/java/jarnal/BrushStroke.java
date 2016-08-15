package jarnal;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.awt.image.BufferedImage;

import jarnal.Tools;
import jarnal.Parameter;
import jarnal.Analyze;
import jarnal.Pages;

class BrushStroke{

	static private float cwidth = 1.8f;

	private LinkedList lines = new LinkedList();
	public String link = "";
	private Point2D.Double click = null;
	public Page page;
	public Tools tools = new Tools();
	public int xmin;
	public int xmax;
	public int ymin;
	public int ymax;
	public boolean isPage = false;
	public boolean isText = false;
	public boolean isHighlight = false;
	public boolean isImage = false;
	public boolean isOverlay = false;
	public boolean isSmoothed = false;
	public int marker = 0;
	public jrnlPDFWriter pdfWriter = null;

	public BrushStroke(){}

	public BrushStroke(Page parent){
		isPage = true;
	}

	public BrushStroke(Page parent, Tools jt){
		page = parent;
		tools.copy(jt);
	}

	public BrushStroke(Page parent, Tools jt, Point2D.Double click){
		page = parent;
		setClick(parent, jt, click);
	}

	public BrushStroke(Page parent, String str, boolean circle){
		page = parent;
		isSmoothed = true;
		String aa = getParm(str, "stroke-opacity=");
		if(aa != null){
			float oxx = Float.parseFloat(aa);
			tools.transparency = (int)((oxx * 255.0f) + 0.1f);
		}
		if(circle){
			String a = getParm(str, "cx=");
			if (a == null) return;
			int x = Integer.parseInt(a, 10);
			a = getParm(str, "cy=");
			if (a == null) return;
			int y = Integer.parseInt(a, 10);
			a = getParm(str, "r=");
			if(a == null) return;
			float r = Float.parseFloat(a);
			a = getParm(str, "fill=");
			if(a == null) return;
			tools.width = r;
			tools.color = a.trim();
			click = new Point2D.Double((double) x, (double) y);
			updateRectangle();
			return;
		}
		String stroke = getParm(str, "stroke=");
		if(stroke == null) return;
		String b = getParm(str, "stroke-width=");
		if(b == null) return;
		float s = Float.parseFloat(b);
		tools.width = s;
		tools.color = stroke;
		String arrow = getParm(str, "marker-start=");
		if(arrow != null) marker = -1;
		else{
			arrow = getParm(str, "marker-end=");
			if(arrow != null) marker = 1;
		}
		if(arrow != null){
			int pos = arrow.indexOf("tria");
			int posc = arrow.indexOf("x");
			if((pos >= 0) && (posc >= 0)){
				arrow = arrow.substring(pos + 4, posc);
				pos = Integer.parseInt(arrow);
				marker = marker * pos;
			}
			else marker = 0;			
		}		
		int pos = str.indexOf("\"");
		if (pos < 0) return;
		str = str.substring(pos + 1);
		pos = str.indexOf("\"");
		if(pos <= 0) return;
		str = str.substring(0, pos);
		String del = "M";
		pos = 0;
		Point2D.Double startL = null;
		while(pos >= 0){
			pos = str.indexOf(del);
			if(pos < 0) return;
			str = str.substring(pos + 1);
			pos = str.indexOf(" ");
			if(pos < 0) return;
			String a = str.substring(0, pos);
			str = str.substring(pos + 1);
			//int x = Integer.parseInt(a,10);
			double x = Double.parseDouble(a);
			pos = str.indexOf(" ");
			if(pos < 0) {
				a = str;
				str = "";
			}
			else {
				a = str.substring(0, pos);
				str = str.substring(pos);
			}
			//int y = Integer.parseInt(a, 10);
			double y = Double.parseDouble(a);
			//Point2D.Double p = new Point2D.Double((double) x, (double) y);
			Point2D.Double p = new Point2D.Double(x, y);
			if(del.equals("M")) del = "L";
			else add(startL, p);
			startL = p;
		}
	}

  	public BrushStroke copy(Page parent) {
		if (isText) return ((Jtext)this).copyJtext(parent);
		if (isPage) return new BrushStroke(parent);
		BrushStroke res = new BrushStroke(parent,tools);
		if (click != null) res.click = (Point2D.Double)click.clone();
		for (Iterator i = lines.iterator(); i.hasNext(); )
			res.lines.add(((Line2D.Double) i.next()).clone());
		res.xmin = xmin;
		res.xmax = xmax;
		res.ymin = ymin;
		res.ymax = ymax;
		res.link = link;
		res.isSmoothed = true;
		res.marker = marker;
		return res;
	}

	private double flatten(double x0, double x, double y, double z, double z1){
		double aa = .16;
		double bb = .08;
		double cc = 1.0 - aa - aa - bb - bb;
		double zz = (bb * ((2 * x) - x0)) + (bb * ((2 * z) - z1)) + (aa * x) + (aa *z) + (cc *y);
		return zz;
	}


	public Rectangle smooth(boolean forceSmooth){
		if(isPage) return null;
		if(isText || isImage) return getRectangle();
		if(isSmoothed && !forceSmooth) return getRectangle();
		isSmoothed = true;
		int n = lines.size();
		if(n == 0) return getRectangle();
		double ntimes = 3;
		double xx[] = new double[n + 1];
		double yy[] = new double[n + 1];
		for (int i = 0; i < n; i++){
			Line2D.Double dline = (Line2D.Double) lines.remove(0);
			xx[i] = dline.getX1();
			xx[i + 1] = dline.getX2();
			yy[i] = dline.getY1();
			yy[i + 1] = dline.getY2();	
		}
		for(int j = 0; j < ntimes; j++){
			for (int i = 2; i < n - 1; i++){
				xx[i] = flatten(xx[i - 2], xx[i - 1], xx[i], xx[i + 1], xx[i + 2]);
				yy[i] = flatten(yy[i - 2], yy[i - 1], yy[i], yy[i + 1], yy[i + 2]);		
			}
		}
		for (int i = 0; i < n; i++){
			Line2D.Double dline = new Line2D.Double(xx[i], yy[i], xx[i + 1], yy[i + 1]);
			lines.add(dline);
		}
		return getRectangle();
	}

	public void setMarker(int marker){
		if(isPage || isText || isImage || (click != null)) return;
		if(isHighlight || (tools.transparency != 255)) return;
		this.marker = marker;
		updateRectangle();
	}
	
	public int getMarker(){return marker;}		

	public void applyPen(String color, String hicolor, float zscale, float hiscale, String trans, boolean bothi, Tools jt){
		if(isPage || isText || isImage) return;
		if(color.equals("smooth strokes")){
			smooth(true);
			return;
		}
		if(jt != null){
			if((tools.transparency != 255) || bothi){
				if(jt.highlighter || (jt.transparency != 255)) tools.copy(jt);
			}
			else if(!jt.highlighter && (jt.transparency == 255)) tools.copy(jt);
			return;
		}
		if((tools.transparency != 255) || bothi){
			if(!hicolor.equals("no change"))tools.color = hicolor;
			tools.width = hiscale * tools.width;
			if(tools.transparency != 255){
				if(trans.equals("Translucent")) tools.setTranslucent();
				if(trans.equals("Transparent")) tools.setTransparent();
			}
		}
		else{
			if(!color.equals("no change"))tools.color = color;
			tools.width = zscale * tools.width;
		}	
	}

	public void reset(){};

	public String getlink(){
		return link;
	}
	public void setlink(String str){
		link = str;
	}

	public String analyze(Analyze janal){
		if(isText || isHighlight || isPage) return null;
		if(click != null) {
			LinkedList ll = new LinkedList();
			ll.add(new Line2D.Double(click, click));
			return janal.analyze(ll);
		}
		return janal.analyze(lines);
	}

	public Point2D.Double upscale(Point2D.Double p){
		float s = page.getScale();
		double x = p.getX() * s;
		double y = p.getY() * s;
		return new Point2D.Double(x, y);
	}

	public Point2D.Double dnscale(Point2D.Double p){
		float s = page.getScale();
		//double x = (double) Math.round(p.getX() / s);
		//double y = (double) Math.round(p.getY() / s);
		double x = p.getX() / s;
		double y = p.getY() / s;
		return new Point2D.Double(x, y);
	}

	public String getParm(String str, String parm){
		int pos = str.indexOf(parm);
		if(pos < 0) return null;
		str = str.substring(pos);
		pos = str.indexOf("\"");
		if (pos < 0) return null;
		str = str.substring(pos + 1);
		pos = str.indexOf("\"");
		if (pos < 0) return null;
		return str.substring(0, pos);
	}

	private void setClick(Page parent, Tools jt, Point2D.Double click){
		page = parent;
		this.click = dnscale(click);
		float swidth = jt.getWidth() * cwidth/2;
		this.click = new Point2D.Double(this.click.getX() - swidth, this.click.getY() - swidth);
		tools.copy(jt);	
		updateRectangle();
	}

	public Line2D.Double add(Point2D.Double startL, Point2D.Double endL){
		Line2D.Double dline = new Line2D.Double(dnscale(startL), dnscale(endL));
		lines.add(dline);
		updateRectangle();
		return dline;
	}

	public boolean below(int y){
		if(isPage) return false;
		y = (int)(y / page.getScale());
		if(y < ymin) return true;
		return false;
	}

	public boolean above(int y){
		if(isPage) return false;
		y = (int)(y / page.getScale());
		if(y > ymax) return true;
		return false;
	}

	public boolean left(int y){
		if(isPage) return false;
		y = (int)(y / page.getScale());
		if(y > xmax) return true;
		return false;
	}

	public boolean right(int y){
		if(isPage) return false;
		y = (int)(y / page.getScale());
		if(y < xmin) return true;
		return false;
	}

	public boolean hitZ(Point2D.Double p){
		if(isPage) return false;
		if(isImage || isText || (click != null)) return hit(p);
		p = dnscale(p);
		int x = (int) p.getX();
		int y = (int) p.getY();
		double rad = tools.getWidth();
		if(click != null) rad = rad * cwidth;
		double s = (float) page.getScale();
		rad = (float) (rad + (8/s));
		int r = (int) rad;
		boolean test = false;
		Rectangle rr = new Rectangle(x - r, y - r, 2*r, 2*r);
		for(int ii = 0; ii < lines.size(); ii++){
			Line2D.Double ll = (Line2D.Double) lines.get(ii);
			if(ll.intersects(rr)){
				test = true;
				break;
			}
		}
		return test;
	}

	public boolean hit(Point2D.Double p){
		if(isPage) return false;
		p = dnscale(p);
		int x = (int) p.getX();
		int y = (int) p.getY();
		double rad = tools.getWidth();
		if(click != null) rad = rad * cwidth;
		double s = (float) page.getScale();
		rad = (float) (rad + (8/s));
		int r = (int) rad;
		boolean test = true;
		if(x < xmin - r) test = false;
		if(x > xmax + r) test = false;
		if(y < ymin - r) test = false;
		if(y > ymax + r) test = false;
		return test;
	}

	public boolean overlaps(int a, int b, int w, int h){
		if(isPage) return false;
		double rad = tools.getWidth();
		if(click != null) rad = rad * cwidth;
		double s = (float) page.getScale();
		rad = (float) (rad + (8/s));
		int r = (int) rad;
		if((a + w)/s < xmin - r) return false;
		if(a/s > xmax + r) return false;
		if((b + h)/s < ymin - r) return false;
		if(b/s > ymax + r) return false;
		return true;
	}

	public Rectangle getRectangle(){
		double s = (double) page.getScale();
		double rad = (s * tools.getWidth())/ 2.0;
		if(marker != 0) rad = 3.0 * rad * Math.abs(marker) / 10.0;
		if(click != null) rad = 1.5 * cwidth * rad;
		int x = (int) Math.floor((s * xmin) - rad);
		int y = (int) Math.floor((s * ymin) - rad);
		int w = (int) Math.ceil((s*(xmax - xmin)) + (2.0 * rad));
		int h = (int) Math.ceil((s*(ymax - ymin)) + (2.0 * rad));
		return (new Rectangle(x - 1,y - 1,w + 3,h + 3));
	}

	public void updateRectangle(){
		if(click == null){
			Line2D.Double dline = (Line2D.Double) lines.getLast();
			if(lines.size() == 1){
				xmin = (int) dline.getX1();
				xmax = xmin;
				ymin = (int) dline.getY1();
				ymax = ymin;
			}
			int x = (int) dline.getX2();
			int y = (int) dline.getY2();
			if(x < xmin) xmin = x;
			if(x > xmax) xmax = x;
			if(y < ymin) ymin = y;
			if(y > ymax) ymax = y;
		}
		else {
			float swidth = (tools.getWidth() * cwidth)/2.0f;
			xmin = (int) (click.getX() + swidth);
			xmax = xmin;
			ymin = (int) (click.getY() + swidth);
			ymax = ymin;
		}
	}

	class Distortion{
		double x;
		double y;
		//double bx;
		//double by;
		//double ox;
		//double oy;
		Point2D.Double br;
		double r;
		double s;
		public boolean successful;

		public Distortion (Point2D.Double ul, Point2D.Double br, Point2D.Double oldbr){
			ul = dnscale(ul);
			br = dnscale(br);
			oldbr = dnscale(oldbr);
			x = -ul.getX();
			y = -ul.getY();
			double bx = br.getX();
			double by = br.getY();
			double ox = oldbr.getX();
			double oy = oldbr.getY();
			double dx = bx - x;
			double sgnx = 1;
			if(dx < 0) sgnx = -1;
			if(Math.abs(dx) < 2.0) {
				dx = 2.0 * sgnx;
				bx = x + dx;
			}
			double dy = by - y;
			double sgny = 1;
			if(dy < 0) sgny = -1;
			if(Math.abs(dy) < 2.0) {
				dy = 2.0 * sgny;
				by = y + dy;
			}
			this.br = upscale(new Point2D.Double(bx, by));
			successful = false;		
			try{
				r = dx / (ox - x);
				s = dy / (oy - y);
				successful = true;
			}
			catch(Exception ex){ex.printStackTrace();}
		}

		public Line2D.Double distort(Line2D.Double dline){
			if(!successful) return dline;
			double x1 = x + (r *(dline.getX1() - x));
			double x2 = x + (r * (dline.getX2() - x));
			double y1 = y + (s * (dline.getY1() - y));
			double y2 = y + (s * (dline.getY2() - y));
			dline.setLine(x1, y1, x2, y2);
			return dline;
		}

		public Point2D.Double distort(Point2D.Double cl){
			if(!successful) return cl;
			return new Point2D.Double(x + (r *(cl.getX() - x)), y + (s * (cl.getY() - y)));
		}

		public float distortX(float x){
			if(!successful) return x;
			float z = (float)(r * x);
			return z;
		}
		
		public float distortY(float y){
			if(!successful) return y;
			float z = (float)(s * y);
			return (float)(s * y);
		}
	}			
				

	public Point2D.Double distort(Point2D.Double ul, Point2D.Double br, Point2D.Double oldbr){

		Distortion dis = new Distortion(ul, br, oldbr);
		if(!dis.successful) return br;
		Rectangle nbb = null;
		for (int i = 0; i < lines.size(); i++){
			Line2D.Double dline = (Line2D.Double) lines.remove(i);	
			dline = dis.distort(dline);
			lines.add(i,dline);
			if (nbb == null) nbb = dline.getBounds();
			else nbb.add(dline.getBounds());
		}
		if(click != null) {
			click = dis.distort(click);
			updateRectangle();
		}
		else {
			xmin = (int)Math.floor(nbb.getX());
			xmax = (int)Math.floor(nbb.getX()+nbb.getWidth()) +1;
			ymin = (int)Math.floor(nbb.getY());
			ymax = (int)Math.floor(nbb.getY()+nbb.getHeight()) +1;
		}
		return dis.br;
	}				
	
	public void offset(Point2D.Double p){
		p = dnscale(p);
		Rectangle nbb = null;
		for (int i = 0; i < lines.size(); i++){
			Line2D.Double dline = (Line2D.Double) lines.remove(i);	
			dline.setLine(dline.getX1() + p.getX(), dline.getY1() + p.getY(), dline.getX2() + p.getX(), dline.getY2() + p.getY());
			lines.add(i,dline);
			if (nbb == null) nbb = dline.getBounds();
			else nbb.add(dline.getBounds());
		}
		if(click != null) {
			click = new Point2D.Double(click.getX() + p.getX(), click.getY() + p.getY());
			updateRectangle();
		}
		else {
			xmin = (int)Math.floor(nbb.getX());
			xmax = (int)Math.floor(nbb.getX()+nbb.getWidth()) +1;
			ymin = (int)Math.floor(nbb.getY());
			ymax = (int)Math.floor(nbb.getY()+nbb.getHeight()) +1;
		}
	}

	public boolean trapColor(){
		if(page.parent.trapColors){
			int ii = tools.getTrapColor();
			if(ii != -1){
				if(!page.parent.trapc[ii]) return true;
			}
		}
		return false;
	}

	public boolean draw(Graphics2D g2, int print){
		if(isPage) return true;
		if(trapColor()) return false;
		g2.setPaint(tools.getColor());
		if(isHighlight || (tools.transparency != 255)){			
			Color co = tools.getColor();
			int trans = tools.transparency;
			if(isHighlight) trans = page.getTransparency();
			g2.setPaint(new Color(co.getRed(), co.getGreen(), co.getBlue(), trans));
		}
		float swidth = tools.getWidth() * page.getScale();
		if(swidth < 0){
			System.out.println("Error: negative stroke width jtool.GetWidth=" + tools.getWidth() + " scale=" + page.getScale());
			return false;
		}
		BasicStroke bs = new BasicStroke(swidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		g2.setStroke(bs);
		if(click != null){
			Point2D.Double click = upscale(this.click);
			g2.fill(new Ellipse2D.Double(click.getX(), click.getY(), cwidth * swidth, cwidth * swidth));
			return false;
		}
		boolean fade = false;
		if(isHighlight && (page.getTransparency() != 255)) fade = true;
		if(tools.transparency != 255) fade = true;
		GeneralPath gp = new GeneralPath();
		boolean firstOne = true;
		Line2D.Double dline = null;
		for (Iterator i = lines.iterator(); i.hasNext(); ){
			dline = (Line2D.Double) i.next();
			dline = new Line2D.Double(upscale((Point2D.Double)dline.getP1()), upscale((Point2D.Double)dline.getP2()));
			if(fade){
				float X;
				float Y;
				if(firstOne){
					firstOne = false;
					X = (float)((Point2D.Double)dline.getP1()).getX();
					Y = (float)((Point2D.Double)dline.getP1()).getY();
					gp.moveTo(X,Y);
				}
				X = (float)((Point2D.Double)dline.getP2()).getX();
				Y = (float)((Point2D.Double)dline.getP2()).getY();
				gp.lineTo(X,Y);	
			}
			else g2.draw(dline);
		}
		if(fade) g2.draw(gp);
		else if(marker != 0){
			if(marker < 0) {
				dline = (Line2D.Double) lines.iterator().next();
				dline = new Line2D.Double(upscale((Point2D.Double)dline.getP2()), upscale((Point2D.Double)dline.getP1()));
			}
			Point2D.Double p2 = (Point2D.Double)dline.getP2();
			Point2D.Double p1 = (Point2D.Double)dline.getP1();
			double DX = (-p2.getY() + p1.getY());
			double DY = (p2.getX() - p1.getX());
			gp = new GeneralPath();
			gp.moveTo((float)p2.getX(), (float)p2.getY());
			double gamma = Math.sqrt(((p2.getX() - p1.getX())*(p2.getX() - p1.getX())) + ((p2.getY() - p1.getY())*(p2.getY() - p1.getY())));
			if(gamma >= 1.0){
				double fact = Math.abs(marker) / 10.0;
				double alpha = -3.5 * fact * swidth/gamma;
				double beta = 1.25 * fact * swidth/gamma;
				gp.lineTo((float)(p2.getX() + (alpha * (p2.getX() - p1.getX())) + (beta * DX)), (float)(p2.getY() + (alpha * (p2.getY() - p1.getY())) + (beta * DY)));
				gp.lineTo((float)(p2.getX() + (alpha * (p2.getX() - p1.getX())) - (beta * DX)), (float)(p2.getY() + (alpha * (p2.getY() - p1.getY())) - (beta * DY)));
				gp.closePath();
				g2.fill(gp);
				g2.draw(gp);
			}
		}
		return false;
	}

	public String saveLink(){
		String str = "";
		if(link.equals("")) return str;
		str = "<a xlink:href=\"" + link + "\">\n";
		return str;

	}
	public String endLink(String str){
		if(link.equals("")) return str;
		return str + "</a>\n";
	}

	public String lon(double x){
		java.text.DecimalFormat df = new java.text.DecimalFormat("###.#", new java.text.DecimalFormatSymbols(new Locale("en")));
		return df.format(x);
	}

	private String getArrowName(){
		String ans = "tria";
		int n = (int)Math.abs(marker);
		ans = ans + n + "x" + new Random().nextInt();
		return ans;
	}	
	
	public String save(Hashtable ht){
		if(isPage) return "\n\n\n\n";
		String trans = "";
		if(tools.transparency != 255){
			float fx = ((float)tools.transparency)/255.0f;
			trans = " stroke-opacity=\"" + fx + "\" ";
		}
		String str = saveLink();
		float swidth = tools.getWidth();
		if(click != null){
			str = str + "<circle cx=\"" + (long)click.getX() + "\" cy=\"" + (long)click.getY() + "\" r=\"" + swidth + "\" fill=\"" + tools.color + "\"" + trans + "/>\n";
			return endLink(str);
		}
		str = str + "<path d=\"";
		String sep = new String("M");
		for (Iterator i = lines.iterator(); i.hasNext(); ){
			Line2D.Double dline = (Line2D.Double) i.next();
			if(sep.equals("M")){
				str = str + sep + lon(dline.getX1()) + " " + lon(dline.getY1());
				sep = "L";
			}
			str = str + " " + sep + lon(dline.getX2()) + " " + lon(dline.getY2());
		
		}
		String m0 = "";
		String m1 = "";
		if(marker != 0){
			String mid = getArrowName();
			String m2 = "marker-end";
			if(marker < 0) m2 = "marker-start";
			m1 = " " + m2 + "=\"url(#" + mid +")\" ";
			double fact = Math.abs(marker) / 10.0;
			int ten = (int) (10 * fact);
			int five = (int) (5 * fact);
			int three = (int) (3 * fact);
			int four = (int) (4 * fact);
    			m0 = "<marker id=\"" + mid + "\" viewBox=\"0 0 " + ten + " " + ten + "\" refX=\"" + ten + "\" refY=\"" + five + "\" markerUnits=\"strokeWidth\" markerWidth=\"" + four + "\" markerHeight=\"" + three + "\" orient=\"auto\"><path d=\"M 0 0 L " + ten + " " + five + " L 0 " + ten + " z\" /></marker>";


		}
		str = str + "\" stroke=\"" + tools.color + "\" stroke-width=\"" + swidth + "\" fill=\"none\"" + trans + m1 + "/>\n";
		return m0 + endLink(str);
	}
}

class Jscrap extends BrushStroke {

	public int height;
	public int width;
	public String scrapName;
	public Point2D.Double corner;

	public Jscrap(){}

	public Jscrap(Page parent, String scrapName, Point2D.Double p){
		page = parent;
		isImage = true;
		if(scrapName.substring(0, 1).equals("<")){
			interpretSVG(scrapName);
			corner = dnscale(p);
			updateRectangle();
			return;	
		}	
		this.scrapName = scrapName;
		corner = dnscale(p);
		BufferedImage gg = (BufferedImage) page.parent.getScrap(scrapName);
		width = gg.getWidth();
		height = gg.getHeight();
		updateRectangle();
		Jarnal.getInstance().nextScrap = save(null);
	}

	public Jscrap(Page parent, String str){
		page = parent;
		isImage = true;
		interpretSVG(str);
		updateRectangle();
	}

	private void interpretSVG(String str){
		String a = getParm(str, "x=");
		if(a != null) xmin = Integer.parseInt(a, 10);
		a = getParm(str, "y=");
		if(a != null) ymin = Integer.parseInt(a, 10);
		a = getParm(str, "width=");
		if(a != null) width = Integer.parseInt(a, 10); 
		a = getParm(str, "height=");
		if(a != null) height = Integer.parseInt(a, 10);
		scrapName = getParm(str, "xlink:href=");
		corner = new Point2D.Double(xmin, ymin);
	}

	public void setCorner(Point2D.Double p){
		corner = p;
	}

	public BrushStroke copy(Page parent) {
		Jscrap jsc = new Jscrap(parent, scrapName, upscale(corner));
		jsc.setCorner(corner);
		jsc.setScale(width, height);
		jsc.link = link;
		return jsc;
	}

	public String getClip(){
		return save(null);
	}

	public void setScale(int width, int height){
		this.width = width;
		this.height = height;
		updateRectangle();
	}

	public Rectangle rescale(int x, int y){
		Rectangle r = getRectangle();
		width = x - xmin;
		if(width < 0) width = 0;
		height = y - ymin;
		if(height < 0) height = 0;
		updateRectangle();
		return Tools.maxR(r, getRectangle(), (int) (page.getScale() * 10));
	}

	public void updateRectangle(){
		if(width == 0) width = 1;
		if(height == 0) height = 1;
		if(width < 0){
			corner = new Point2D.Double(corner.getX() + width, corner.getY());
			width = -width;
		}
		if(height < 0){
			corner = new Point2D.Double(corner.getX(), corner.getY() + height);
			height = -height;
		}
		xmin = (int) Math.floor(corner.getX());
		ymin = (int) Math.floor(corner.getY());
		xmax = xmin + width;
		ymax = ymin + height;
		return;
	}

	public Point2D.Double distort(Point2D.Double ul, Point2D.Double br, Point2D.Double oldbr){
		Distortion dis = new Distortion(ul, br, oldbr);
		if(!dis.successful) return br;
		corner = dis.distort(corner);
		height = (int)dis.distortY((float)height);
		width = (int)dis.distortX((float)width);
		updateRectangle();
		return dis.br;
	}		
	
	public void offset(Point2D.Double p){
		p = dnscale(p);
		corner = new Point2D.Double(corner.getX() + p.getX(), corner.getY() + p.getY());
		updateRectangle();
	}

	public boolean draw(Graphics2D g2, int print){
		double s = (double) page.getScale();
		Point2D.Double p = upscale(corner);
		BufferedImage gg = (BufferedImage) page.parent.getScrap(scrapName);
		if(gg == null) return false;
		float hscale = (float) width / gg.getWidth();
		float vscale = (float) height / gg.getHeight();
		AffineTransform at = new AffineTransform();
		at.translate(p.getX(), p.getY());
		at.scale(s * hscale, s * vscale);
		g2.drawImage(gg, at, null);
		return false;
	}
	
	public String save(Hashtable ht){
		String ans = saveLink();
		ans = ans + "<image x=\"" + xmin + "\" y=\"" + ymin + "\" width=\"" + (xmax - xmin) + "\" height=\"" + (ymax - ymin) + "\" xlink:href=\"" + scrapName + "\"/>\n";
		return endLink(ans);
	}
}

class Joverlay extends Jscrap {

	private int arcWidth = 0;
	private int arcHeight = 0;
	private String fillColor = "white";
	private String strokeColor= "gray";
	private int strokeWidth = 0;
	private int fillFade = 10;
	private int strokeFade = 0;

	public int getInt(int index){
		if(index == 0) return arcWidth;
		if(index == 1) return arcHeight;
		if(index == 2) return strokeWidth;
		if(index == 3) return fillFade;
		if(index == 4) return strokeFade;
		return -1;
	}

	public Joverlay(){}	

	public Joverlay(Page parent, String svg, Point2D.Double p){
		page = parent;
		isImage = true;
		isOverlay = true;
		interpretSVG(svg);
		corner = dnscale(p);
		updateRectangle();
	}

	public Joverlay(Page parent, String str){
		page = parent;
		isImage = true;
		isOverlay = true;
		interpretSVG(str);
		updateRectangle();
	}

	public Joverlay(Page parent, Point2D.Double p0, Point2D.Double p1, String str){
		page = parent;
		isImage = true;
		isOverlay = true;
		xmin = 0;
		xmax = 0;
		interpretSVG(str);
		corner = dnscale(p0);
		p1 = dnscale(p1);
		width = (int)(p1.getX() - corner.getX());
		height = (int)(p1.getY() - corner.getY());
		updateRectangle();
	}

	public void makeSquare(){
		if(width < height) height = width;
		else width = height;
		updateRectangle();
	}

	public void setStyle(int arcWidth, int arcHeight, String fillColor, String strokeColor, int strokeWidth, int fillFade, int strokeFade){
		if(arcWidth >= 0) this.arcWidth = arcWidth;
		if(arcHeight >= 0) this.arcHeight = arcHeight;
		if(strokeWidth >= 0) this.strokeWidth = strokeWidth;
		if(fillFade >= 0) this.fillFade = fillFade;
		if(strokeFade >= 0) this.strokeFade = strokeFade;
		if(fillColor != null) this.fillColor = fillColor;
		if(strokeColor != null) this.strokeColor = strokeColor;
	}
			
	private void interpretSVG(String str){
		String a = getParm(str, "x=");
		if(a != null) xmin = (int) Float.parseFloat(a);
		a = getParm(str, "y=");
		if(a != null) ymin = (int) Float.parseFloat(a);		
		a = getParm(str, "width=");
		if(a != null) width = Integer.parseInt(a, 10); 
		a = getParm(str, "height=");
		if(a != null) height = Integer.parseInt(a, 10);
		float z;
		a = getParm(str, "rx=");
		if(a != null) {
			z = Float.parseFloat(a);
			arcWidth = (int)((z * 200.0f)/(float) width);
			if(arcWidth > 100) arcWidth = 100;
		}
		a = getParm(str, "ry=");
		if(a != null) {
			z = Float.parseFloat(a);
			arcHeight = (int)((z * 200.0f)/(float) height);
			if(arcHeight > 100) arcHeight = 100;
		}
		a = getParm(str, "stroke-width=");
		if(a != null) strokeWidth = Integer.parseInt(a, 10); 
		a = getParm(str, "fill=");
		if(a != null) fillColor=a; 
		a = getParm(str, "stroke=");
		if(a != null) strokeColor=a; 
		a = getParm(str, "fill-opacity=");
		if(a != null) {
			float fo = Float.parseFloat(a);
			fillFade = (int)(100.0f * (1.0f - fo));
		}
		a = getParm(str, "stroke-opacity=");
		if(a != null) {
			float fo = Float.parseFloat(a);
			strokeFade = (int)(100.0f * (1.0f - fo));
		}
		corner = new Point2D.Double(xmin, ymin);
	}

	public void updateRectangle(){
		super.updateRectangle();
		xmin = xmin - strokeWidth;
		ymin = ymin - strokeWidth;
		xmax = xmax + (2 * strokeWidth);
		ymax = ymax + (2 * strokeWidth);
		return;
	}

	public BrushStroke copy(Page parent) {
		Joverlay jsc = new Joverlay();
		jsc.page = parent;
		jsc.isImage = true;
		jsc.isOverlay = true;
		jsc.setCorner(corner);
		jsc.setScale(width, height);
		jsc.link = link;
		jsc.setStyle(arcWidth, arcHeight, fillColor, strokeColor, strokeWidth, fillFade, strokeFade);
		return jsc;
	}

	private int getTrans(int x){
		return (int)(2.55f * (float)(100 - x));
	}

	public boolean draw(Graphics2D g2, int print){
		double s = (double) page.getScale();
		Point2D.Double p = upscale(corner);
		Color xfill = Tools.getColor(fillColor);
		g2.setPaint(new Color(xfill.getRed(), xfill.getGreen(), xfill.getBlue(), getTrans(fillFade)));
		int aw = (int)(s * (float) width * (float) arcWidth / 100.0f);
		int ah = (int)(s * (float) height * (float) arcHeight / 100.0f);
		g2.fillRoundRect((int) p.getX(), (int) p.getY(), (int) (s * width), (int) (s * height), aw, ah);
		if(strokeWidth > 0){
			Color xstroke = Tools.getColor(strokeColor);
			g2.setPaint(new Color(xstroke.getRed(), xstroke.getGreen(), xstroke.getBlue(), getTrans(strokeFade)));
			float swidth = strokeWidth * page.getScale();
			BasicStroke bs = new BasicStroke(swidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			g2.setStroke(bs);
			g2.drawRoundRect((int) p.getX(), (int) p.getY(), (int) (s * width), (int) (s * height), aw, ah);
		}
		return false;
	}

	private String opac(int x){
		float fo = ((float)(100 - x))/100.0f;
		java.text.DecimalFormat df = new java.text.DecimalFormat("#.##", new java.text.DecimalFormatSymbols(new Locale("en")));
		return df.format(fo);
	}

	public String getStyle(){
		return "width=\"" + width + "\" height=\"" + height + "\" rx=\"" + lon((double) arcWidth * (double) width /200.0) + "\" ry=\"" + lon((double) arcHeight * (double) height /200.0) + "\" fill=\"" + fillColor + "\" stroke=\"" + strokeColor + "\" stroke-width=\"" + strokeWidth + "\" fill-opacity=\"" + opac(fillFade) + "\" stroke-opacity=\"" + opac(strokeFade) + "\"";
	}

	public String save(Hashtable ht){
		String ans = saveLink();
		ans = ans + "<rect x=\"" + (int)corner.getX() + "\" y=\"" + (int)corner.getY() + "\" " + getStyle() + " />\n";
		return endLink(ans);
	}
}


class Jtext extends BrushStroke{

	private Parameter parameter;

	public Point2D.Double corner = null;
	private int width;
	private int height = -1;
	private Analyze janal = null;
	private String frameId = null;

	public Jtext(Page parent, Point2D.Double corner, Tools jt, int width, Jchar parms){
		page = parent;
		this.corner = corner;
		tools = jt;
		this.width = width;
		height = 10;
		isPage = false;
		updateRectangle();
		parameter=new Parameter();
		parameter.setParms(parms);
		parameter.setWidth(null, width);
		isText = true;
	}

	public Jtext(Page parent, String str){
		page = parent;
		isPage = false;
		isText = true;
		make(str);
	}

	public Jtext copyJtext(Page parent) {
		Jtext res = new Jtext(parent,"");
		if (corner != null) res.corner = (Point2D.Double)corner.clone();
		res.width = width;
		res.height = height;
		if(frameId == null) res.parameter = new Parameter(parameter);
		else res.parameter = parameter;
		res.frameId = frameId;
		res.xmin = xmin;
		res.xmax = xmax;
		res.ymin = ymin;
		res.ymax = ymax;
		res.link = link;
		return res;
	}

	public boolean matchFrame(String frameId){
		if(this.frameId == null) return false;
		if(frameId == null) return false;
		if(this.frameId.equals(frameId)) return true;
		return false;
	}

	public float getY(){
		return (float) corner.getY();
	}

	public Jtext split(){
		//jpar.make();
		if(frameId != null) return this;
		frameId = parameter.addFrame(width, parameter.height(frameId), null, 1);
		return this;
	}

	public boolean isFrame(){
		if(frameId != null) return true;
		return false;
	}

	public int getHeight(){return height;}
	public int getWidth(){ return width;}

	public String checkFrame(int advance){
		if(frameId == null) return null;
		return parameter.checkFrame(frameId, advance);
	}

	public void join(Jtext jtext, int w, int h, int direc){
		this.parameter = jtext.parameter;
		frameId = parameter.addFrame(w, h, jtext.frameId, direc);
		width = w;
		height = h;
		//jpar.make();
	}

	public Analyze janal(boolean defaultfilepairs){
		if(janal == null) janal = new Analyze(defaultfilepairs);		
		return janal;
	}

	private void updateJanal(){
		if(janal == null) return;
		String restr = parameter.getLast2();
		janal.restr(restr);
	}

	public int getLastns(){
		if(janal == null) return 0;
		return janal.getLastns();
	}

	public void setParms(Jchar jc){
		parameter.setParms(jc);
	}
		
	public String typeKey(String str){
		//String oldXML = save(null);
		String oldXML = parameter.typeCharX(str);
		height = parameter.height(frameId);
		updateRectangle();
		return oldXML;
	}

	public String setSelStyle(boolean bold, boolean italic, boolean underline, Float size, String font, String color){
		//String oldXML = save(null);
		String oldXML = parameter.setSelStyleX(bold, italic, underline, size, font, color);
		height = parameter.height(frameId);
		updateRectangle();
		return oldXML;
	}

	private void initRectangle(){
		if(height == -1){
			height = parameter.height(frameId);
			width = parameter.width(frameId);
		}
		updateRectangle();
	}

	public boolean hit(Point2D.Double p){
		p = dnscale(p);
		int x = (int) p.getX();
		int y = (int) p.getY();
		boolean test = true;
		if(x < xmin) test = false;
		if(x > xmax) test = false;
		if(y < ymin) test = false;
		if(y > ymax) test = false;
		return test;
	}

	public Rectangle getTextRectangle(){
		if(height == -1){
			height = parameter.setHeight();
			updateRectangle();
		}
		return getRectangle();
	}

	public Rectangle getRectangle(){
		double s = (double) page.getScale();
		int x = (int) Math.floor((s * xmin));
		int y = (int) Math.floor((s * ymin));
		int w = (int) Math.ceil((s*(xmax - xmin)));
		int h = (int) Math.ceil((s*(ymax - ymin)));
		return (new Rectangle(x - 1,y - 1,w + 3,h + 3));
	}

	public void updateRectangle(){
		xmin = (int) corner.getX();
		ymin = (int) corner.getY();
		xmax = xmin + width;
		ymax = ymin + height;
	}

	public Point2D.Double distort(Point2D.Double ul, Point2D.Double br, Point2D.Double oldbr){
		Distortion dis = new Distortion(ul, br, oldbr);
		if(!dis.successful) return br;
		corner = dis.distort(corner);
		width = (int)dis.distortX((float)width);
		parameter.setWidth(frameId, width);
		height = (int)dis.distortY((float)parameter.height(frameId));
		parameter.setHeight(frameId, height);
		height = parameter.height(frameId);
		updateRectangle();
		return dis.br;
	}	
	
	public void offset(Point2D.Double p){
		p = dnscale(p);
		corner = new Point2D.Double(corner.getX() + p.getX(), corner.getY() + p.getY());
		updateRectangle();
	}

	public boolean draw(Graphics2D g2, int print){
		if(page.parent.trapColors) parameter.parent = this;
		else parameter.parent = null;
		initRectangle();
		boolean ispdf = false;
		if(pdfWriter != null) ispdf = true;
		parameter.draw((int) corner.getX(), (int) corner.getY(), page.getScale(), g2, print, ispdf, frameId);
		initRectangle();
		return false;
	}

	public void selectAll(){
		parameter.selectAll();
	}

	public boolean collapseSel(){
		boolean ans = parameter.collapseSel();
		height = parameter.height(frameId);
		updateRectangle();
		updateJanal();
		return ans;
	}

	public void clearSel(){
		clearSel(false);
	}

	public void clearSel(boolean collapse){
		if(collapse && (frameId != null)) parameter.collapseSel();
		else parameter.clearSel();
		height = parameter.height(frameId);
		updateRectangle();
		updateJanal();
	}
	
	public String save(Hashtable ht){
		String withData = "yes";
		if((ht != null) && (parameter.id != null)){
			Object test = ht.get(parameter.id);
			if(test != null) withData = "no";
			else ht.put(parameter.id, "yes");
		}
		String ans = saveLink() + parameter.getSVG((int)corner.getX(), (int) corner.getY(), frameId, withData) +
"\n";
		return endLink(ans);
	}

	public String makeX(String str){
		String oldXML = parameter.makeX(str);
		updateRectangle();
		updateJanal();
		return oldXML;
	}

	public void reset(){
		parameter.resetFrame(frameId);
	}

	public void make(String str){
		String a = super.getParm(str, "x=");
		if (a == null) return;
		int x = Integer.parseInt(a, 10);
		a = super.getParm(str, "y=");
		if (a == null) return;
		int y = Integer.parseInt(a, 10);
		corner = new Point2D.Double(x, y);
		a = super.getParm(str, "id=");
		if(a == null) parameter = new Parameter(str);
		else{
			Hashtable textFrames = page.parent.textFrames;
			String yy[] = a.split("\\$");
			if(parameter == null) {
				parameter = (Parameter) textFrames.get(yy[0]);
				if(parameter == null) parameter = new Parameter();
			}
			frameId = parameter.makeNew(str, false);
			textFrames.put(yy[0], parameter);
		}
		width = parameter.width(frameId);
		updateRectangle();
		//jpar.endSel();
		parameter.clearSel();
		updateJanal();
	}
	
	public Point2D.Double hitSel(Point2D.Double p, float s){
		Point2D.Double pp = new Point2D.Double(p.getX()-(s*corner.getX()), p.getY()-(s*corner.getY()));
		parameter.hitSel(pp, s, false, frameId);
		parameter.findSel();
		updateJanal();
		return corner;
	}

	public Rectangle dragText(int x, int y, float s){
		Point2D.Double pp = new Point2D.Double(x-(s*corner.getX()), y-(s*corner.getY()));
		parameter.hitSel(pp, s, true, frameId);
		parameter.findSel();
		return getRectangle();
	}

	public boolean find(String targ, boolean findFirst, boolean reverse, boolean matchCase, boolean wholeWord){
		boolean ans = parameter.find(targ, findFirst, reverse, matchCase, wholeWord, frameId);
		if(ans) parameter.findSel();
		return ans;
	}

	public Rectangle adv(int adv, int extend){
		parameter.advSel(adv, extend);
		parameter.findSel();
		return getRectangle();
	}
	public JarnalSelection clip(){ 
		JarnalSelection jsel = parameter.clipSel();
		updateJanal();
		return jsel;
	}
	public Rectangle setWidth(double x){
		Rectangle r = getRectangle();
		width = (int) (x - corner.getX());
		if(width <= 10) width = 10;
		parameter.setWidth(frameId, width);
		height = parameter.height(frameId);
		updateRectangle();
		return Tools.maxR(r, getRectangle(), (int) (page.getScale() * 10));
	}

	public Rectangle setHeight(double y){
		Rectangle r = getRectangle();
		height = (int) (y - corner.getY());
		if(height <= 10) height = 10;
		parameter.setHeight(frameId, height);
		height = parameter.height(frameId);
		updateRectangle();
		return Tools.maxR(r, getRectangle(), (int) (page.getScale() * 10));
	}

	String getText(Hashtable ht){
		String test = parameter.id;
		if(test == null) return parameter.getText();
		Object trial = ht.get(test);
		if(trial != null) return "";
		ht.put(test, test);
		return parameter.getText();
	}

	String getHtml(Hashtable ht){
		String test = parameter.id;
		if(test == null) return parameter.getHTML();
		Object trial = ht.get(test);
		if(trial != null) return "";
		ht.put(test, test);
		return parameter.getHTML();
	}

	String getDesc(){ return parameter.getDesc();}
	String getHtmlDesc(){ return parameter.getHtmlDesc();}
	String[] getTextStyle(){ return parameter.getTextStyle();}
	Jchar getCurParms(){ return parameter.getCurParms();}
	Jchar getFinalParms(){ return parameter.getFinalParms();}
	String getText(){ return parameter.getText();}
	String getHTML(){ return parameter.getHTML();}		
}
