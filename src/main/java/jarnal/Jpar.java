package jarnal;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;

class Jchar{
	private String aschar;
	public float size = 20.0f;
	private boolean bold = false;
	private boolean italic = false;
	public boolean underline = false;
	public String font = "Vera";
	public Color color = Color.black;
	private String cname = "black";
	private int hash;
	private boolean isBreak = false;
	private boolean isNull = false;

	public Jchar(){}
	public Jchar(String aschar, Jchar jc){
		if(aschar.equals("\n")) isBreak = true;
		this.aschar = aschar;
		size = jc.size;
		bold = jc.bold;
		italic = jc.italic;
		underline = jc.underline;
		font = jc.font;
		color = jc.color;
		cname = jc.cname;
		setHash();	
	}

	public Jchar copy() {
		Jchar res = new Jchar();
		res.aschar = aschar;
		res.size = size;
		res.bold = bold;
		res.italic = italic; 
		res.underline = underline;
		res.font = font;
		res.color = color;
		res.cname = cname;
		res.hash = hash;
		res.isBreak = isBreak;
		res.isNull = isNull;
		return res;
	}

	public void clearStyle(){
		bold = false;
		italic = false;
		underline = false;
		color = Color.black;
		setHash();
	}
	public void setHash(){
		String svg = getSVG();
		hash = svg.hashCode();
	}
	public void setNull(){ isNull = true; isBreak = true;}
	public boolean isBreak(){return isBreak;}
	public boolean isNull(){return isNull;}
	public void setColor(Color color, String cname){ this.color = color; this.cname = cname; setHash();}
	public void setItalic(){ italic = !italic; setHash();}
	public void setBold(){ bold = !bold; setHash();}
	public void setUnderline(){ underline = !underline; setHash();}
	public void setSize(float size){ this.size = size; setHash();}
	public void setFont(String font){ this.font = font; setHash();}

	public String toString() {
		return "Jchar " +
			((aschar==null)?"null":aschar) + " " +
			((isBreak)?"isBreak":"noBreak") + " " +
			((isNull)?"isNull":"noNull") + " " +
			size + " " + bold + " " + italic + " " + underline + " " +
			font + " " + cname;
	}
	public String getChar(){ return aschar;}
	public int getStyle(){
		if(bold & italic) return Font.BOLD + Font.ITALIC;
		if(bold) return Font.BOLD;
		if(italic) return Font.ITALIC;
		return Font.PLAIN;
	}
	public String getSVG(){
		String svg = "";
		svg = svg + "font-family=\"" + font + "\" ";
		svg = svg + "font-size=\"" + (int) size + "\" ";	
		svg = svg + "fill=\"" + cname + "\" ";
		if(bold) svg = svg + "font-weight=\"bold\" ";
		if(italic) svg = svg + "font-style=\"italic\" ";
		if(underline) svg = svg + "text-decoration=\"underline\" ";
		return svg;
	}
	public String[] getHTML(){
		String html = "<FONT ";
		html = html + "COLOR=\"" + cname + "\" ";
		html = html + "FACE=\"" + font + "\" ";
		html = html + "POINT-SIZE=\"" + (int) size + "\">";
		String html2 = "</FONT>";	
		if(bold) {
			html = html + "<STRONG>";
			html2 = "</STRONG>" + html2;
		}
		if(italic) {
			html = html + "<I>";
			html2 = "</I>" + html2;
		}
		if(underline) {
			html = html + "<U>";
			html2 = "</U>" + html;
		}
		String ht[] = new String[2];
		ht[0] = html;
		ht[1] = html2;			
		return ht;
	}
	public String getDesc(){
		String d = "";
		d = font + " " + size + "pt " + cname;
		if(bold) d = d + " bold";
		if(italic) d = d + " italic";
		if(underline) d = d + " underline";
		return d;
	}
	public String[] getTextStyle(){
		String ans[] = new String[6];
		ans[0] = font;
		ans[1] = "" + ((int) size);
		ans[2] = cname;
		if(bold) ans[3] = "bold";
		if(italic) ans[4] = "italic";
		if(underline) ans[5] = "underline";
		return ans;
	}
	public String getHtmlDesc(){
		String d = "";
		String f = font;
		if(bold) f = f + " bold";
		if(italic) f = "<I>" + f + "</I>";
		if(underline) f = "<U>" + f + "</U>";
		d = f + " " + size + "pt";
		d = d + " " + cname;
		return d;
	}
	
	public int getHash(){ return hash;}

	public int getTrapColor(){
		if(color == Color.black) return 0;
		if(color == Color.blue) return 1;
		if(color == Color.green) return 2;
		if(color == Color.gray) return 3;
		if(color == Color.magenta) return 4;
		if(color == Color.orange) return 5;
		if(color == Color.pink) return 6;
		if(color == Color.red) return 7;
		if(color == Color.white) return 8;
		if(color == Color.yellow) return 9;
		return -1;
	}
}

public class Jpar{
	public static Graphics2D gg2 = (new BufferedImage(1, 1, BufferedImage.TYPE_INT_BGR)).createGraphics();
	public static String sep = "\n\t\r\t";
	public LinkedList chars = new LinkedList();
	private LinkedList jspans = new LinkedList();
	private LinkedList jframes = null;
	private int width;
	private int height = -1;
	private int ptr;
	private int dx;
	private int dy;
	private Jchar parms = new Jchar();
	private int sel = 0;
	private int selwidth = 0;
	private Jspan selSpan = null;
	public Jtext parent = null;
	//private boolean isMade = false;
	public String id = null;

	public Jpar(){
		Jchar jc = new Jchar();
		jc.setNull();
		chars.add(jc);
		makeDirty(true, null);
	}
	public Jpar(String str){
		makeNew(str, true);
	}

	public String makeNew(String str, boolean forceRemake){
		String a = getParm(str, "id=");
		String ans = null;
		if(a != null){
			String x[] = a.split("\\$");
			id = x[0];
			ans = x[1];
			if(x[2].equals("no")){
				//isMade = false;
				return ans;
			}
		}
		//if(!forceRemake && isMade) return ans;
		//isMade = true;	
		chars = new LinkedList();
		jspans = new LinkedList();
		height = -1;
		parms = new Jchar();
		sel = 0;
		selwidth = 0;
		selSpan = null;
		parent = null;
		//width is set only for non-flowed text
		//for frames height and width of the frame are recovered from the spans
		a = getParm(str, "textLength=");	
		if(a == null) {
			//isMade = false;
			return ans;
		}
		width = Integer.parseInt(a, 10);
		int pos = 0;
		String strstr;
		while(pos >=0){
			pos = str.indexOf("<tspan", pos);
			int last = str.indexOf("</tspan>", pos);
			if(pos >= 0){
				if(last < 0) return ans;
				strstr = str.substring(pos, last);
				pos = last + 7;
				makeSpan(strstr);
			}

			//pos = str.indexOf("<tspan");				
			//if(pos >= 0){
			//	str = str.substring(pos);
			//	int last = str.indexOf("</tspan>");
			//	if(last < 0) return ans;
			//	strstr = str.substring(0, last);
			//	str = str.substring(last + 7);				
			//	makeSpan(strstr);
			//}
		}
		Jchar jc = new Jchar();
		jc.setNull();
		chars.add(jc);
		sel = -1;
		if(chars.size() > 1){
			jc = ((Jchar)chars.get(chars.size() - 2)).copy();
			parms = jc.copy();
			jc.setNull();
			chars.removeLast();
			chars.add(jc);
		}
		//if(gg2 == null) return ans;
		makeDirty(true, null);
		//findSel();
		return ans;
	}

	public Jpar(LinkedList list, int w){
		Jchar jc;
		if((list == null) || (list.size() == 0)) {
			jc = new Jchar();
			jc.setNull();
			chars.add(jc);
		}
		else {
			jc = (Jchar) list.getLast();
			if(!jc.isNull()) {
				jc = new Jchar();
				jc.setNull();
				list.add(jc);
			}
			chars = list;
		}
		//isMade = true;
		width = w;
		makeDirty(true, null);
	}

	public Jpar(Jpar jp) {
		//isMade = true;
		for (Iterator i = jp.chars.iterator(); i.hasNext(); )
			chars.add(((Jchar)i.next()).copy());
		width = jp.width;
		sel = -1;
		//if (gg2 == null) return;
		makeDirty(true, null);
	}

	private int getFrame(String frameId){
		int ans = -1;
		if(frameId == null) return ans;
		if(jframes == null) return ans;
		JrnlTextFrame jtf;
		for(ListIterator i = jframes.listIterator(); i.hasNext();){
			ans = i.nextIndex();
			jtf = (JrnlTextFrame) i.next();
			if(jtf.getId().equals(frameId)) break;
		}
		return ans;
	}

	private JrnlTextFrame findFrame(String frameId){
		if(frameId == null) return null;
		if(jframes == null) return null;
		JrnlTextFrame jtf = null;
		for(ListIterator i = jframes.listIterator(); i.hasNext();){
			jtf = (JrnlTextFrame) i.next();
			if(jtf.getId().equals(frameId)) return jtf;
		}
		return null;
	}			 

	public int height(String frameId){
		if(frameId == null) return height;
		JrnlTextFrame jtf = findFrame(frameId);
		if(jtf != null) {
			make(getFrame(jtf.getId()));
			return jtf.height;
		}
		return height;
	}

	public int width(String frameId){
		if(frameId == null) return width;
		JrnlTextFrame jtf = findFrame(frameId);
		if(jtf != null) {
			make(getFrame(jtf.getId()));			
			return jtf.width;
		}
		return width;
	}

	public int setHeight(){
		height = (int) parms.size;
		return height;
	}

	public void setHeight(String frameId, int height){
		this.height = height;
		if(frameId != null){
			JrnlTextFrame jtf = findFrame(frameId);
			if(jtf != null) jtf.height = height;
			makeDirty(false, frameId);
		}
	}

	public void setWidth(String frameId, int width){
		this.width = width;
		if(frameId != null){
			JrnlTextFrame jtf = findFrame(frameId);
			if(jtf != null) jtf.width = width;
			makeDirty(false, frameId);
		}
		else makeDirty(true, null);
	}	

	public String addFrame(int width, int height, String frameId, int direc){
		if(id == null){
			jframes = new LinkedList();
			id = "jpar" + (new Random()).nextInt();
		}
		JrnlTextFrame jtf = new JrnlTextFrame(null);
		jtf.width = width;
		jtf.height = height;
		jtf.count = 1;
		boolean found = false;
		if(frameId != null){
			for(int i = 0; i < jframes.size(); i++){
				JrnlTextFrame jtf2 = (JrnlTextFrame) jframes.get(i);
				if(jtf2.getId().equals(frameId)){
					if((i + direc < jframes.size()) && (i + direc >= 0)) return ((JrnlTextFrame)jframes.get(i + direc)).getId();
				}
			}
		}
		if(!found && (direc > 0)) jframes.add(jtf);
		makeDirty(false, jtf.getId());
		return jtf.getId();
	}

	public boolean trapColor(Jchar test){
		if(parent == null) return false;
		if(parent.jpage.parent.trapColors){
			int ii = test.getTrapColor();
			if(ii != -1){
				if(!parent.jpage.parent.trapc[ii]) return true;
			}
		}
		return false;
	}

	//parms control the style that is to be typed next
	//it is ordinarily the style of the previous character
	//if the previous character was a space or break it is the style of the next character
	//the final character of a jpar is a null
	//the style of the terminating null is set to the style of the last actual character
	//this can be overridden by set parms
	//in addition when setstyle is used, parms is set to the style that has just been set

	public void setParms(Jchar parms){
		this.parms = parms;
		chars.removeLast();
		Jchar jc = parms.copy();
		jc.setNull();
		chars.add(jc);
	}

	public String getDesc(){ return parms.getDesc();}
	public String getHtmlDesc(){ return parms.getHtmlDesc();}
	public String[] getTextStyle(){ return parms.getTextStyle();}
	public Jchar getCurParms(){ return parms;}
	public Jchar getFinalParms(){
		return ((Jchar)chars.getLast()).copy();
	}

	public static int findWhole(String test, String targ, int n, boolean reverse, boolean wholeWord){
		int nn = n;
		if(!wholeWord) return nn;
		boolean found = Jtool.foundWord(test, n, targ.length());
		while(!found){
			int start = n;
			if(!reverse) {
				start = n + 1;
				if(start >= test.length()) break;
				n = test.indexOf(targ, start);
				if(n == -1) break;
			}
			else {
				start = n - 1;
				if(start < 0) break;
				n = test.lastIndexOf(targ, start);
				if(n == -1) break;
			}
			found = Jtool.foundWord(test, n, targ.length());
			if(found) break;
		}
		if(!found) nn = -1;
		else nn = n;
		return nn;
	}

	public boolean find(String targ, boolean findFirst, boolean reverse, boolean matchCase, boolean wholeWord, String frameId){
		String test = getText();
		if(!matchCase) test = test.toLowerCase();
		int start = sel;
		if(!reverse && (selwidth > 0)) start = start + selwidth;
		if(reverse && (selwidth < 0)) start = start + selwidth;
		if(!reverse && findFirst) start = 0;
		if(reverse && findFirst) start = test.length();
		if(reverse && (!findFirst) && start > 0)
			if(test.substring(start).startsWith(targ)) start = start - 1;
		if(reverse && (start == 0)) return false;
		JrnlTextFrame jtf = null;
		if((frameId != null) && (jframes != null)){
			jtf = findFrame(frameId);
			make(getFrame(frameId));
			if(jtf != null){
				if(!reverse && (jtf.start > start)) start = jtf.start;
				if(reverse && (jtf.stop < start)) start = jtf.stop;
			}
		}
		int n = -1;
		if(!reverse) n = test.indexOf(targ, start);
		else n = test.lastIndexOf(targ, start);
		if(n > -1) {
			n = findWhole(test, targ, n, reverse, wholeWord);
			if((n >= 0) && (jtf != null)){
				if(!reverse && (n >= jtf.stop)) n = -1;
				if(reverse && (n + targ.length() - 1 < jtf.start)) n = -1;
				if(n >= 0) nocollapse = true;
			}
			if(n >= 0) {				
				sel = n;
				selwidth = targ.length();
				setParms();
				return true;
			}
		}
		return false;
	}

	public void selectAll(){
		sel = 0;
		selwidth = chars.size() - 1;
	}

	private boolean nocollapse = false;

	public boolean collapseSel(){
		if(nocollapse){
			nocollapse = false;
			return false;
		}		
		if(selwidth == 0) return false;
		sel = sel + selwidth;
		selwidth = 0;
		return true;
	}
			
	public void clearSel(){
		sel = -1;
		selSpan = null;
	}

	public void endSel(){
		sel = chars.size() - 1;
		selwidth = 0;
	}

	public void findSel(){
		if(sel == -1) {
			selSpan = null;
		}
		else{
			int p = sel;
			if(selwidth < 0) p = sel + selwidth;
			int q = sel;
			if(selwidth > 0) q = sel + selwidth;
			int iframe = 0;
			while(q > ptr) {
				if(make(iframe++)) break;	
			}			
			for(ListIterator i = jspans.listIterator(); i.hasNext();){
				Jspan js = (Jspan) i.next();
				js.initSel();
			}
			for(ListIterator i = jspans.listIterator(); i.hasNext();){
				Jspan js = (Jspan) i.next();
				p = js.findSel(p);
				selSpan = js;
				if(p == -1) break;
			}
			if(selwidth != 0){
				p = sel + selwidth;
				if(selwidth < 0) p = sel;
				for(ListIterator i = jspans.listIterator(); i.hasNext();){
					Jspan js = (Jspan) i.next();
					p = js.findSel2(p);
					if(p == -1) break;
				}
			}
		}	
	}

	public void hitSel(Point2D.Double p, float s, boolean findWidth, String frameId){
		int oldsel = sel;
		int dx = (int)(p.getX()/s);
		int dy = (int)(p.getY()/s);
		sel = 0;
		selSpan = null;
		for(ListIterator i = jspans.listIterator(); i.hasNext();){
			Jspan js = (Jspan) i.next();
			int a = js.hitSel(dx, dy, frameId);
			sel = sel + Math.abs(a);
			if(a >= 0) break;
		}
		if(sel >= chars.size()) sel = chars.size() - 1;
		if(findWidth){
			selwidth = sel - oldsel;
			sel = oldsel;
		}
		else selwidth = 0;
		setParms();
	}

	public static Jchar makeParms(String str){
		Jchar jc = new Jchar();
		String a = getParm(str, "font-size=");
		if(a != null) jc.setSize((float) Integer.parseInt(a, 10));
		a = getParm(str, "font-family=");
		if(a != null) jc.setFont(a.trim());
		a = getParm(str, "fill=");
		if(a != null) {
			a = a.trim();
			jc.setColor(getColor(a), a);
		}
		a = getParm(str, "font-weight=");
		if(a != null) if(a.equals("bold")) jc.setBold();
		a = getParm(str, "font-style=");
		if(a != null) if(a.equals("italic")) jc.setItalic();
		a = getParm(str, "text-decoration=");
		if(a != null) if(a.equals("underline")) jc.setUnderline();
		return jc;
	}					

	public void makeSpan(String str){
		Jchar parms = new Jchar();
		String a = getParm(str, "id=");
		if(a != null){
			String x[] = a.split("\\$");
			JrnlTextFrame jtf = findFrame(x[0]);
			if(jtf == null){
				jtf = new JrnlTextFrame(x[0]);
				jtf.width = Integer.parseInt(x[1]);
				jtf.height = Integer.parseInt(x[2]);
				if(jframes == null) jframes = new LinkedList();
				jframes.add(jtf);
			}
			jtf.count++;
		}
		//next for compatibility with old file format
		//new format for a hard break uses a <tspan with specified font
		//and has text equal to an \n
		//note that the old version of Jarnal will work with the new file format
		if(str.equals("<tspan>") || str.equals("<tspan></tspan>")){
			chars.add(new Jchar("\n", parms));	
			return;
		}
		parms = makeParms(str);
		int last = str.indexOf(">");	
		str = str.substring(last + 1);
		last = str.indexOf("</tspan>");
		if(last > 0) str = str.substring(0, last);
		str = str.replaceAll("&lt;", "<");
		str = str.replaceAll("&gt;", ">");
		for(int i = 0; i < str.length(); i++){
			chars.add(new Jchar(str.substring(i, i+1), parms));
		}
	}

	public static Color getColor(String color){
		Color c = Color.black;
		if(color.equals("blue")) c = Color.blue;
		if(color.equals("green")) c = Color.green;
		if(color.equals("dark gray")) c = Color.darkGray;
		if(color.equals("gray")) c = Color.gray;
		if(color.equals("light gray")) c = Color.lightGray;
		if(color.equals("magenta")) c = Color.magenta;
		if(color.equals("orange")) c = Color.orange;
		if(color.equals("pink")) c = Color.pink;
		if(color.equals("red")) c = Color.red;
		if(color.equals("white")) c = Color.white;
		if(color.equals("yellow")) c = Color.yellow;
		return c;
	}

	public static void setGraphics(Graphics2D gg2){
		if(Jpar.gg2 != null) Jpar.gg2.dispose();
		Jpar.gg2 = gg2;
	}

	public static String getParm(String str, String parm){
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
	public void setStyle(Jchar jc, boolean bold, boolean italic, boolean underline, Float size, String font, String color){
		if(bold)jc.setBold();
		if(italic)jc.setItalic();
		if(underline)jc.setUnderline();
		if(size != null) jc.setSize(size.floatValue());
		if(font != null) jc.setFont(font);
		if(color != null) jc.setColor(getColor(color), color);			
	}
	public String setSelStyleX(boolean bold, boolean italic, boolean underline, Float size, String font, String color){
		int start = sel;
		int stop = sel + selwidth;
		if(selwidth < 0){
			start = sel + selwidth;
			stop = sel;
		}
		String ans = oldXML(start, stop - start, null);
		Jchar jc;
		for(ListIterator i = chars.listIterator(start); i.nextIndex() < stop;){
			jc = (Jchar) i.next();
			setStyle(jc, bold, italic, underline, size, font, color);
		}
		setStyle(parms, bold, italic, underline, size, font, color);
		if(stop == (chars.size() - 1)){
			jc = (Jchar) chars.get(stop);
			setStyle(jc, bold, italic, underline, size, font, color);
		}
		makeDirty(false, null);
		//if you did set the parms, it would undo whatever style you just set
		//setParms();
		return ans;
	}

	public void setParms(){
		int p = sel;
		if(selwidth < 0) p = sel + selwidth;
		if(p > 0) {
			p = p - 1;
			Jchar jc = (Jchar) chars.get(p);
			if(jc == null) return;
			String s = jc.getChar();
			if(s != null){
				if(!s.equals(" ") && !jc.isBreak()){
					parms = new Jchar(" ", jc);
					return;
				}
			}
		}
		p = sel;
		if(selwidth > 0) p = sel + selwidth;
		if(p >= chars.size()) p = chars.size() - 1;
		Jchar jc = (Jchar) chars.get(p);
		parms = new Jchar(" ", jc);
	}
	public void advSel(int adv, int extend){
		if(extend == 2){
			if(selwidth == 0) extend = 1;
		}
		if(extend == 1){
			selwidth = selwidth + adv;
			if(sel + selwidth < 0) selwidth = -sel;
			if(sel + selwidth >= chars.size()) selwidth = chars.size() - sel - 1;
		}
		if(extend == 0) {
			//if we do nothing, we do not reset the parms
			if((adv == 0) && (selwidth == 0)) return;
			if(selwidth > 0) sel = sel + selwidth;
			selwidth = 0;
			sel = sel + adv;
			if(sel < 0) sel = 0;
			if(sel >= chars.size()) sel = chars.size() - 1;
		}
		setParms();
	}
	private void normalizeSel(){
		if(selwidth < 0){
			sel = sel + selwidth;
			selwidth = -selwidth;
		}
	}
	public String makeX(String s){
		String oldXML;
		if(s.startsWith(">" + sep)) oldXML = makeCharX(s);
		else {
			oldXML = getSVG(0, 0, null, "yes");
			makeNew(s, true);
		}
		return oldXML;
	}
	private static boolean newStyle = true;

	private String oldXML(int start, int len, LinkedList old){
		if(newStyle){
			//if old == null then called from setstyle and use start/len to find it
			if(old == null){
				old = new LinkedList();
				if(len > 0){
					for(ListIterator i = chars.listIterator(start); i.nextIndex() < start + len;) 
						old.add(i.next());
					Jchar jc = (Jchar) old.getLast();
					if(jc.isNull()) old.removeLast();
				}
			}
			if(old.size() == 0) return ">" + sep + start + sep + len + sep + "E";
			Jpar jp = new Jpar(old, width);
			String ans = ">" + sep + start + sep + len + sep + jp.getSVG(0, 0, null, "yes");
			return ans;
		}
		return getSVG(0, 0, null, "yes");
	}
	private LinkedList deleteSel(){
		LinkedList old = new LinkedList();
		if(selwidth == 0) return old;
		normalizeSel();
		for(int i = 0; i < selwidth; i++) old.add(chars.remove(sel));
		Jchar jc = (Jchar) old.getLast();
		if(jc.isNull()){
			chars.add(old.removeLast());
		}
		selwidth = 0;
		return old;
	}
	public String makeCharX(String s){
		String v[] = s.split(sep);
		int n = v.length / 4;
		String ans = null;
		for(int i = (n - 1); i >=0; i--){
			int i0 = 4 * i;
			sel = Integer.parseInt(v[i0 + 1]);
			selwidth = Integer.parseInt(v[i0 + 2]);
			String test = v[i0 + 3];
			String moreXML;
			if(test.equals("E")) moreXML = typeCharX("");
			else moreXML = typeCharX(test);
			if(ans == null) ans = moreXML;
			else ans = ans + sep + moreXML;		
		}
		return ans;			
	}
	public String typeCharX(String s){
		if(s.startsWith(">" + sep)) return makeCharX(s);
		int start = sel;
		if(selwidth < 0) start = start + selwidth;
		LinkedList old = deleteSel();
		ListIterator ctext = chars.listIterator(sel);
		if(s.startsWith("<text")){
			Jpar jp = new Jpar(s);
			parms = new Jchar(" ", (Jchar) jp.chars.getLast());
			for(ListIterator i = jp.chars.listIterator(); i.hasNext();){
				Jchar jc = (Jchar) i.next();
				if(!jc.isNull()) {
					ctext.add(jc);
					sel++;
				}
			}
			int savesel = sel;
			sel = start;
			makeDirty(false, null);
			sel = savesel;
			return oldXML(start, jp.chars.size() - 1, old);
		}
		if((s.length() > 0) && (sel >= 0)) {
			for(int i = 0; i < s.length(); i++){
				Jchar jc = new Jchar(s.substring(i, i+1), parms);
				ctext.add(jc);
				sel++;
			}
		}
		int savesel = sel;
		sel = start;
		makeDirty(false, null);
		sel = savesel;
		return oldXML(start, s.length(), old);
	}

	public String getText(){
		String txt = "";
		int n = chars.size() - 1;
		if(n <= 0) return "";
		char buf[] = new char[n];
		int j = 0;
		for(ListIterator i = chars.listIterator(); i.nextIndex() < n;){
			Jchar jc = (Jchar) i.next();
			buf[j++] = jc.getChar().charAt(0);
		}
		return new String(buf);
	}

	public String getHTML(){
		make(-1);
		nbuf = 0;
		buf = new char[(5 * chars.size()) + 1200];
		writeBuf("<html><head></head><body bgcolor=white>");
		String initiator = "<p>";
		for(ListIterator i = jspans.listIterator(); i.hasNext(); ){
			writeBuf(initiator);
			Jspan js = (Jspan) i.next();
			String test = js.getHTML();
			if(test.endsWith("</p>")) initiator = "<p>";
			else initiator = "";
			writeBuf(test);
		}
		writeBuf("</body></html>");	
		String html = new String(buf, 0, nbuf);
		buf = null;
		return html;
	}

	int nbuf;
	char[] buf;
	private void writeBuf(String s){
		for(int i = 0; i < s.length(); i++) buf[nbuf++] = s.charAt(i);
	}
	public String getSVG(int x, int y, String frameId, String withData){
		make(-1);
		nbuf = 0;
		int jsize = 0;
		if(jframes != null) jsize = jframes.size();
		buf = new char[(5 * chars.size()) + 1200 + (80 * jspans.size()) + (80 * jsize)];
		writeBuf("<text ");
		if((id != null) && (frameId != null)) 
			writeBuf("id=\"" + id + "$" + frameId + "$" + withData + "\" ");
		writeBuf("x=\"" + x + "\" ");
		writeBuf("y=\"" + y + "\" ");
		writeBuf("textLength=\"" + width + "\" ");
		writeBuf(">");
		if(withData.equals("yes")){
			if(jframes != null){
				for(ListIterator i = jframes.listIterator(); i.hasNext();){
					JrnlTextFrame jtf = (JrnlTextFrame) i.next();
					writeBuf("<tspan id=\"" + jtf.getSVG() + "\"></tspan>");
				}
			}
			for(ListIterator i = jspans.listIterator(); i.hasNext();){
				Jspan js = (Jspan) i.next();
				writeBuf(js.getSVG(x, y));
			}
		}
		writeBuf("</text> ");
		String svg = new String(buf, 0, nbuf);
		buf = null;
		return svg;
	}

	public String getLast2(){
		int start = sel;
		String ans = "  ";
		if(selwidth < 0) start = sel + selwidth;
		if(start < 1) return ans;
		start = start - 1;
		Jchar jc = (Jchar) chars.get(start);
		ans = jc.getChar();
		if(start < 1) return " " + ans;
		jc = (Jchar) chars.get(start - 1);
		return jc.getChar() + ans;
	}		
	public JarnalSelection clipSel(){
		LinkedList clip = new LinkedList();
		int start = sel;
		int end = sel + selwidth;
		if(selwidth < 0) {
			start = sel + selwidth;
			end = sel;
		}
		for(ListIterator i = chars.listIterator(start); i.nextIndex() < end;) clip.add(i.next());
		Jpar jp = new Jpar(clip, width);
		return jp.clip();
	}
	public JarnalSelection clip(){
		String copy = getSVG(0,0, "null", "yes");
		String plain = 	getText();
		String html = getHTML();
		return new JarnalSelection(plain, html, copy, "text");
	}
	public void draw(int x, int y, float s, Graphics2D g2, int print, boolean ispdf,  String frameId){
		make(getFrame(frameId));
		findSel();
		if(selwidth != 0){
			boolean state = false;
			for(ListIterator i = jspans.listIterator(); i.hasNext();){
				Jspan js = (Jspan) i.next();
				boolean found = js.shadeSel(x, y, s, g2, frameId);
				if(found) state = true;
				if(!found && state) break;
			}
		}
		boolean state = false;
		for(ListIterator i = jspans.listIterator(); i.hasNext();) {
			Jspan js = (Jspan) i.next();
			boolean found = js.display(x, y, s, g2, print, ispdf, frameId);
			if(found) state = true;
			if(!found && state) break;
		}
		if((selwidth == 0) && (selSpan != null) && (print == 0))selSpan.displaySel(x, y, s, g2, frameId);
	}

	public FontMetrics setFont(Jchar jc){
		Font f = new Font(jc.font, jc.getStyle(), (int) (jc.size));
		gg2.setFont(f);
		return gg2.getFontMetrics();
	}

	Jchar mparms;
	FontMetrics fm;
	int findex;
	JrnlTextFrame jtf;

	public void resetFrame(String frameId){
		if(frameId == null) return;
		makeDirty(false, frameId);
	}
	private void makeDirty(boolean completely, String frameId){
		if(chars.size() == 0) return;
		if((jframes == null) || (jframes.size() == 0)) completely = true;
		if(ptr >= chars.size()) completely = true;
		if(completely){
			ptr = 0;
			jtf = null;
			findex = 0;
		}
		else{
			int frameNo = -1;
			if(frameId != null) frameNo = getFrame(frameId);
			int start = 0;
			if(sel >= 0){
				start = sel;
				if(selwidth < 0) start = sel + selwidth;
			}
			int temp = ptr;
			for(ListIterator i = jframes.listIterator(); i.hasNext();){
				findex = i.nextIndex();
				jtf = (JrnlTextFrame) i.next();
				if(jtf.dirty) {
					ptr = temp; 
					return;
				}
				ptr = jtf.start;
				if(frameNo == findex) break;
				if(start <= jtf.stop) break;
				temp = jtf.stop + 1;
			}
		}
		dx = 0;
		mparms = (Jchar) chars.get(ptr);
		fm = setFont(mparms);
		dy = fm.getAscent();
		if((jframes != null) && (jframes.size() > 0)) {
			for(ListIterator i = jframes.listIterator(findex); i.hasNext();){
				jtf = (JrnlTextFrame) i.next();
				jtf.dirty = true;
				jtf.start = chars.size();
				jtf.stop = chars.size();
			}
			jtf = (JrnlTextFrame) jframes.get(findex);
			jtf.start = ptr;
			width = jtf.width;
		}
		if((jframes != null) && (jframes.size() > 0)){
			LinkedList oldspans = jspans;
			jspans = new LinkedList();
			for(ListIterator i = oldspans.listIterator(); i.hasNext();){
				Jspan js = (Jspan) i.next();
				JrnlTextFrame jtf0 = js.getFrame();
				if((jtf0 != null) && !jtf0.dirty) jspans.add(js);
				else break;
			}
		}
		else jspans = new LinkedList();
	}

	public boolean make(int tframe){
		if(ptr >= chars.size()) return true;
		if((jframes != null) && (jframes.size() > 0) && (tframe >= 0)){
			if(tframe < findex) return false;
			if(findex >= jframes.size()) return true;
		}
		ListIterator lchars = chars.listIterator(ptr);
		if(jtf != null) {
			jtf.start = ptr;
			width = jtf.width;
			jtf.dirty = false;
		}
		boolean ok = true;
		while(ok){
			if(jtf != null){
				if(dy > jtf.height){
					jtf.stop = ptr - 1;
					if(ptr < chars.size()){
						//mparms = (Jchar) chars.get(ptr);
						mparms = (Jchar) lchars.next();
						lchars.previous();
						fm = setFont(mparms);
						dy = fm.getAscent();
						findex++;
						if(findex >= jframes.size()) {
							jtf = null;
							return true;
						}
						jtf = (JrnlTextFrame) jframes.get(findex);
						if((tframe >= 0) && (tframe < findex)) return false;
						width = jtf.width;
						jtf.start = ptr;
						jtf.dirty = false;
					}
				}
			}
			Jspan js = findSpan(lchars);
			if(js != null) js.setFrame(jtf);
			if(js == null) ok = false;
			else if(js.txt.length() > 0) jspans.add(js);
		}
		height = dy;
		if(jtf != null) jtf.stop = ptr - 1;
		jtf = null;
		return true;
	}

	public String checkFrame(String frameId, int advance){
		if((selwidth != 0) && (advance == 0)) return null;
		if(jframes == null) return null;
		if(advance != 0){
			int m = getFrame(frameId);
			m= m + advance;
			if((m >= jframes.size()) || (m < 0)) return "new";
			return ((JrnlTextFrame) jframes.get(m)).getId();
		}
		JrnlTextFrame jtf = findFrame(frameId);
		make(getFrame(frameId));
		if((jtf != null) && !jtf.dirty){
			if((sel >= jtf.start) && (sel <= jtf.stop)) return null;
		}
		for(ListIterator i = jframes.listIterator(); i.hasNext();){
			int ii = i.nextIndex();
			jtf = (JrnlTextFrame) i.next();
			if(jtf.dirty) make(ii);
			if((sel >= jtf.start) && (sel <= jtf.stop + 1)) return jtf.getId();
		}
		return "new";
	} 
	

	char wrd[] = new char[200];
	char txt[] = new char[400];
	int adv[] = new int[400];
	//Jchar newparms = null;
	private Jspan findSpan(ListIterator lchars){
		int cs = chars.size();
		if(ptr >= cs) return null;
		//Jchar parms = (Jchar) chars.get(ptr);
		//Jchar oldparms = newparms;
		Jchar parms = (Jchar) lchars.next();
		//newparms = parms;
		lchars.previous();
		FontMetrics fm = setFont(parms);
		int tled = fm.getAscent() + fm.getLeading();
		if(parms.isBreak()) {
			Jspan js = new Jspan(this, parms, dx, dy);
			if(parms.isNull())js.setNull();
			else{
				dx = 0;
				//if(lchars.hasPrevious() && (oldparms != null)){
				//	newparms = oldparms;
				//	fm = setFont(oldparms);
				//	tled = fm.getAscent() + fm.getLeading();
				//}
				dy = dy + tled;
			}
			ptr++;
			lchars.next();
			return js;
		}
		int ntxt = 0;
		int nwrd = 0;
		int n = 0;
		int hash = 0;
		boolean ok = true;
		Jchar jc = null;
		hash = parms.getHash();
		int olddx = dx;
		int olddy = dy;
		int i = 0;
		int m = 0;
		char c = ' ';
		while(ok){
			if(ptr >= cs){
				ok = false;
				for(i = 0; i < nwrd; i++) txt[ntxt++] = wrd[i];
			}
			else{
				jc = (Jchar) lchars.next();
				m = jc.getHash();
				if((hash != m) || jc.isBreak()){
					ok = false;
					for(i = 0; i < nwrd; i++) txt[ntxt++] = wrd[i];
					lchars.previous();
				}
			}
			if(ok) {
				c = jc.getChar().charAt(0);
				m = fm.charWidth(c);
				if(c == ' '){
					for(i = 0; i < nwrd; i++) txt[ntxt++] = wrd[i];
					txt[ntxt++] = ' ';
					dx = dx + m;
					nwrd = 0;
					adv[n++] = dx;
					ptr++;
				}
				else {
					if(dx + m <= width) {
						wrd[nwrd++] = c;
						dx = dx + m;
						adv[n++] = dx;
						ptr++;
					}
					else{
						ok = false;
						dx = 0;
						dy = dy + tled;
						if((ntxt > 0) || (olddx > 0)) {
							ptr = ptr - nwrd;
							for(i = 0; i <= nwrd; i++) lchars.previous();
						}
						else {
							for(i = 0; i < nwrd; i++) txt[ntxt++] = wrd[i];
							lchars.previous();
						}
						if(ntxt == 0) return null;
					}
				}
			}
		}
		int adv2[] = new int[n];
		for(i = 0; i < ntxt; i++) adv2[i]=adv[i];
		String ans;
		if(ntxt > 0) ans = new String(txt, 0, ntxt);
		else ans = "";
		return new Jspan(this, parms, ans, adv2, olddx, olddy); 	
	}
				
}

class Jspan{
	private Jchar parms;
	public String txt;
	private int adv[];
	private int dx;
	private int dy;
	private int sel = -1;
	private int sel2 = -2;
	private boolean isBreak = false;
	private boolean isNull = false;
	//public boolean isEmpty = false;
	private Jpar parent = null;
	private JrnlTextFrame jtf = null;

	public Jspan(Jpar parent, Jchar parms, String txt, int adv[], int dx, int dy){
		this.parent = parent;
		this.parms = parms;
		this.txt = txt;
		this.adv = adv;
		this.dx = dx;
		this.dy = dy;
	}

	public Jspan(Jpar parent, Jchar parms, int dx, int dy){
		this.parent = parent;
		this.parms = parms;
		adv=new int[1];
		adv[0] = dx; 
		txt = " "; 
		isBreak = true;
		this.dx = dx;
		this.dy = dy;
	}

	public void setNull(){isNull = true;}
	public void setFrame(JrnlTextFrame jtf){this.jtf = jtf;}
	public JrnlTextFrame getFrame(){return this.jtf;}

	public String getSVG(int x, int y){
		if(isNull) return "";
		//if(isBreak) return "<tspan></tspan>";
		String rtxt;
		if(isBreak) rtxt = "\n";
		else{
			rtxt = txt.replaceAll("<", "&lt;");
			rtxt = rtxt.replaceAll(">", "&gt;");
		}
		String svg = "<tspan ";
		svg = svg + "x=\"" + (x + dx) + "\" ";
		svg = svg + "y=\"" + (y + dy) + "\" ";
		svg = svg + parms.getSVG();
		svg = svg + ">" + rtxt + "</tspan>";
		return svg;
	}

	public String getHTML(){
		if(isNull) return "";
		//if(isEmpty) return "";
		if(isBreak) return "</p>";
		String rtxt = txt.replaceAll("<", "&lt;");
		rtxt = rtxt.replaceAll(">", "&gt;");
		String htmlall[] = parms.getHTML();
		String html = htmlall[0] + rtxt + htmlall[1];
		return html;
	}

	private boolean checkFrame(String frameId){
		if(frameId == null) return false;
		if(jtf == null) return true;
		if(frameId.equals(jtf.getId())) return false;
		return true;
	}		

	public boolean displaySel(int x, int y, float s, Graphics2D g2, String frameId){
		//if(isEmpty) return true;
		if(checkFrame(frameId)) return false;
		if(sel > -1){
			int start = dx;
			if(sel > 0) start = adv[sel - 1];
			Point2D.Double first = new Point2D.Double(s * (x + start), s * (y + dy));
			Point2D.Double second = new Point2D.Double(s * (x + start), s * (y + dy - parms.size));
			Line2D.Double border = new Line2D.Double(first, second);
			float swidth = 0.5f;
			BasicStroke bs = new BasicStroke(swidth);
			g2.setStroke(bs);
			g2.setPaint(Color.gray);
			g2.draw(border);
		}
		return true;			
	}

	public void initSel(){
		sel = -1;
		sel2 = -2;
	}

	public int findSel(int sel){
		if(sel < txt.length()){
			sel2 = -4;
			this.sel = sel;
			return -1;
		}
		sel2 = -3;
		int left = sel - txt.length();
		this.sel = txt.length();
		return (left);
	}

	public int findSel2(int sel){
		if(sel < txt.length()){
			if(sel2 != -4) this.sel = 0;
			sel2 = sel;
			return -1;
		}
		int left = sel - txt.length();
		if(sel2 == -2) {
			this.sel = 0;
			sel2 = txt.length();
		}
		else sel2 = txt.length(); 
		return (left);
	}

	public int hitSel(int x, int y, String frameId){
		if(checkFrame(frameId)) return (-txt.length());
		if(y < dy - parms.size) return 0;
		if(y > dy) return (-txt.length());
		for(int i = 0; i < txt.length(); i++){
			int mid = 0;
			if(i == 0) mid = ((2 * dx) + adv[0])/3;
			else mid = (2 * adv[i-1]+ (1 *adv[i]))/3;
			if(x < mid) return (i);
		}
		return (-txt.length());
	}

	private void drawStringX(Graphics2D g2, String txt0, int x0, int x, int y, int adv[], int i, float s, Font ff){
		double x00 = (double) x;
		char cc[] = txt0.toCharArray();
		GlyphVector gv = ff.createGlyphVector(g2.getFontRenderContext(), cc);
		for(int j = 0; j < txt0.length(); j++){
			gv.setGlyphPosition(j, new Point2D.Double(s * x, s * y));
			if(ispdf) g2.drawString(txt0.substring(j, j+1), s * x, s * y);
			int a = x - x0;
			if((i + j) > 0) a = adv[i + j - 1];
			x = x + adv[i + j] - a;	
		}
		if(!ispdf) g2.drawGlyphVector(gv, 0.0f, 0.0f);
		if(parms.underline){
			double yy = s * ((double) y + ((0.1) * parms.size));
			Line2D.Double ul = new Line2D.Double(s * x00, yy,(double) (s * x), yy);
			BasicStroke bs = new BasicStroke(s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			g2.setStroke(bs);
			g2.draw(ul);
		}
	}

	public boolean shadeSel(int x, int y, float s, Graphics2D g2, String frameId){
		//if(isEmpty) return true;
		if(checkFrame(frameId)) return false;
		if(isBreak) return true;
		if(!((sel2 <= sel) || (sel < 0))){
			int a = 0;
			if(sel > 0) a = adv[sel - 1];
			else a = dx;
			float fx = s*(x + a);
			float fy = s*(y + dy);
			float fdy = s*parms.size;
			float fdx = s*(adv[sel2 - 1] - a);
			float swidth = 0.5f;
			BasicStroke bs = new BasicStroke(swidth);
			g2.setStroke(bs);
			g2.setPaint(Color.gray);
			g2.fill(new Rectangle2D.Double(fx, fy - fdy, fdx, fdy));				
		}
		return true;
	}

	boolean print;	
	boolean ispdf;
	public boolean display(int x, int y, float s, Graphics2D g2, int iprint, boolean iispdf, String frameId){
		//if(isEmpty) return true;
		if(checkFrame(frameId)) return false;
		if(isBreak) return true;
		if(parent.trapColor(parms)) return true;
		print = false;
		if(iprint == 1) print = true;
		ispdf = iispdf;
		Font f = new Font(parms.font, parms.getStyle(), (int) (s * parms.size));
		g2.setFont(f);
		//when sel2 <= sel the span does not have the selection in it
		if((sel2 <= sel) || (sel < 0) || print){
			g2.setPaint(parms.color);
			drawStringX(g2, txt, x, x + dx, y + dy, adv, 0, s, f);
		}
		else{
			String txt0 = "";
			if(sel > 0){
				txt0 = txt.substring(0, sel);
				g2.setPaint(parms.color);
				drawStringX(g2, txt0, x, x + dx, y + dy, adv, 0, s, f);	
			}	
			txt0 = txt.substring(sel, sel2); 
			int a = 0;
			if(sel > 0) a = adv[sel - 1];
			else a = dx;
			float fx = s*(x + a);
			float fy = s*(y + dy);
			g2.setPaint(Color.white);
			drawStringX(g2, txt0, x, x + a, y + dy, adv, sel, s, f);	
			if(txt.length() > sel2){
				txt0 = txt.substring(sel2, txt.length());
				g2.setPaint(parms.color);
				a = adv[sel2 - 1];
				drawStringX(g2, txt0, x, x + a, y + dy, adv, sel2, s, f);
			}
						
		}
		return true;
	}
}
class JrnlTextFrame{
	static private Random random = new Random();
	private String id;
	public int height;
	public int width;
	public int count = 0;
	public int start = -1;
	public int stop = -1;
	public boolean dirty = true;

	public JrnlTextFrame(String id){
		if(id == null) this.id = "jtf" + random.nextInt();
		else this.id = id;
	}
	public String getSVG(){
		return id + "$" + width + "$" + height;
	}
	public String getId(){return id;}		
}

