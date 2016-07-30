package jarnal;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.lang.Math.*;
import java.lang.Number.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.net.*;
import jarnal.Jpages;

public class Jtool{

	//public static float bWidth = 2.2f;

	public String type = "Medium";
	public float width = 2.2f;
	public String color = "black";
	public boolean highlighter = false;
	public int transparency = 255;
	public float bWidth = 2.2f;
	public float hTrans = 1.0f;
	public float fatWidth = 11.0f;
	public static String defaultOverlay="width=\"1\" height=\"1\" rx=\"0\" ry=\"0\" fill=\"white\" stroke=\"gray\" stroke-width=\"0\" fill-opacity=\"0.9\" stroke-opacity=\"1.0\"";
	public static String circleOverlay="width=\"186\" height=\"186\" rx=\"93\" ry=\"93\" fill=\"white\" stroke=\"black\" stroke-width=\"2\" fill-opacity=\"0\" stroke-opacity=\"1\"";
	public static String squareOverlay="width=\"186\" height=\"186\" rx=\"0\" ry=\"0\" fill=\"white\" stroke=\"black\" stroke-width=\"2\" fill-opacity=\"0\" stroke-opacity=\"1\"";


	public static boolean foundWord(String str, int start, int length){
		if(start < 0) return false;
		if(start >= str.length()) return false;
		if(start > 0){
			char c = str.charAt(start - 1);
			if(Character.isLetter(c)) return false;
		}
		int end = start + length - 1;
		if(end >= str.length()) return false;
		if(end < str.length() - 1){
			char c = str.charAt(end + 1);
			if(Character.isLetter(c)) return false;
		}
		return true;
	}

	public static long maxMemory(){
		Runtime rt = Runtime.getRuntime();
		long mm = rt.maxMemory();
		if(checkMacOSX()){
			mm = mm - 101000000l;
		}
		return mm;
	}

	public static boolean checkMacOSX(){
		String osn = System.getProperty("os.name").toLowerCase();
		if (osn.toLowerCase().startsWith("mac os x")) return true;
		return false;
	}

	public static boolean checkMSWindows(){
		String osn = System.getProperty("os.name").toLowerCase();
		if (osn.indexOf("windows") > -1) return true;
		return false;
	}

	public static String getBasepath(){
		String myster = Jtool.class.getClassLoader().getSystemResource("jarnal/Jarnal.class").toString();
		myster = Jtool.replaceAll(myster, "jarnal/Jarnal.class", "");
		myster = Jtool.replaceAll(myster, "file:", "");
		myster = Jtool.replaceAll(myster, "jar:/", "");
		myster = Jtool.replaceAll(myster, "jarnal.jar!/", "");
		myster = Jtool.replaceAll(myster, "%20", " ");
		if(!checkMSWindows()) myster = "/" + myster;
		return myster;
	}

	public static String cmdQuote(String fname){
		if(checkMSWindows()) return "\"" + fname + "\"";
		return fname;
	}

	public static String inputStreamToString(InputStream isoo){
		int nmin = 1000000;
		int nborg = 40000;
		int nmax = nmin + ( 5 * nborg); 
		byte b[] = new byte[nmax];
		int nread = 0;
		int noff = 0;
		try{
			ByteArrayOutputStream baost = new ByteArrayOutputStream();
			while((nread = isoo.read(b, noff, nborg)) >= 0){
				noff = noff + nread;
				if(noff > nmax - (2 * nborg)){
					baost.write(b, 0, noff);
					noff = 0;
				}
			}
			baost.write(b, 0, noff);
			byte c[] = baost.toByteArray();
			return new String(c);
		}
		catch(Exception ex){ex.printStackTrace();}
		return null;	
	}

	public static String replaceAll(String str, String regexp, String repl){
		String orig = str;
		str = str + "\n";
		String x[] = str.split(regexp);
		if(x.length == 0) return orig;
		String ans = x[0];
		for(int i = 1; i < x.length; i++) ans = ans + repl + x[i];
		ans = ans.trim();
		return ans;
	}

	public static String[] replaceAllUnixFoo(String str, String fargs[]){
		String orig = str;
		LinkedList fina = new LinkedList();
		str = str + "\n";
		String x[] = str.split(" ");
		//int j = 0;
		if(x.length == 0)fina.add(orig);
		else{
			for(int i = 0; i < x.length; i++){
				if(x[i].startsWith("%")){
					String snum = x[i].substring(1).trim();
					int jj = Integer.parseInt(snum);
					fina.add(fargs[jj - 1]);
					//j++;
				}
				else fina.add(x[i].trim());
			}
		}
		String fstr[] = new String[fina.size()];
		for(int i = 0; i < fina.size();i++) {
			fstr[i] = (String) fina.get(i);
			//System.out.println(fstr[i]);
		}
		return fstr;
	}			
			

	public static Rectangle maxR(Rectangle r1, Rectangle r2, int pad){
		int x = (int) r1.getX();
		if((int) r2.getX() < x) x = (int) r2.getX();
		int y = (int) r1.getY();
		if((int) r2.getY() < y) y = (int) r2.getY();
		int xx = (int) r1.getX() + r1.width;
		int xxx = (int) r2.getX() + r2.width;
		if(xxx > xx) xx = xxx;
		int yy = (int) r1.getY() + r1.height;
		int yyy = (int) r2.getY() + r2.height;
		if(yyy > yy) yy = yyy;
		return new Rectangle(x - pad, y - pad, xx - x + (2*pad), yy - y + 2*pad);
	}

	public static String getLine(String s, String n){
		if (s == null) return null;
		int pos = s.indexOf(n);
		if (pos < 0) return null;
		s = s.substring(pos);
		pos = s.indexOf("=");
		if (pos < 0) return null;
		s = s.substring(pos + 1);
		pos = s.indexOf("\n");
		if(pos < 0) return null;
		s = s.substring(0, pos);
		s = s.trim();
		return s;
	}

	public static Hashtable readConf(String s){
		Hashtable ht = new Hashtable();
		boolean done = false;
		int pos = 0;
		while(!done){
			pos = s.indexOf("=");
			if (pos < 0) done = true;
			else{
				String key = s.substring(0, pos).trim();
				s = s.substring(pos + 1);
				pos = s.indexOf("\n");
				if(pos < 0) pos = s.length() - 1;
				ht.put(key, s.substring(0, pos).trim());
				s = s.substring(pos + 1);
			}
		}
		return ht;
	}

	public static String writeConf(Hashtable ht){
		String ans = "";
		for (Enumeration e = ht.keys() ; e.hasMoreElements() ;) {
         		String key = (String)(e.nextElement());
			String value = (String) ht.get(key);
			ans = ans + key + "=" + value + "\n";
		}
		return ans;	
	}

	public static Jtool getTool(String y){
		if (y == null) return null;
		Jtool jt = new Jtool();
		String z = getLine(y, "type");
		if(z == null) return null;
		jt.setWidth(z);
		z = getLine(y, "color");
		if(z == null) return null;
		jt.color = z;
		z = getLine(y, "highlighter"); 
		if(z == null) return null;
		if(z.equals("true")) jt.highlighter = true;
		else jt.highlighter = false;
		z = getLine(y, "transparency");
		if(z != null) jt.transparency = Integer.parseInt(z);
		z = getLine(y, "fatWidth");
		if(z != null) jt.fatWidth = Float.parseFloat(z);
		return jt;
	}

	public static String getOnlyEntry(String s, String n){
		s = getEntry(s, n);
		if(s == null) return null;
		int pos = s.indexOf("\n");
		if(pos < 0) return null;
		return s.substring(pos + 1, s.length());
	}

	public static String getEntry(String s, String n){
		int pos = s.indexOf(n);
		if(pos < 0) return null;
		s = s.substring(pos + n.length());
		pos = s.indexOf("\n\n");
		if(pos < 0) return null;
		s = s.substring(0, pos);
		return s + "\n";	
	}

	public String getConf(){
		String s = "type=" + type + "\n";
		s = s + "color=" + color + "\n";
		s = s + "highlighter=" + highlighter + "\n";
		s = s + "transparency=" + transparency + "\n";
		s = s + "fatWidth=" + fatWidth;
		s = s + "\n";
		return s;
	}

	public float getWidth(){
		return width;
	}

	public float getHeavy(){
		return 2.0f * getBaseWidth();
	}

	public void setWidth(String type){
		this.type = type;
		if(type.equals("Fine")) width = 0.60f * getBaseWidth();
		if(type.equals("Medium")) width = getBaseWidth();
		if(type.equals("Heavy")) width = 2.0f * getBaseWidth();
		if(type.equals("Fat")) width = fatWidth * getBaseWidth();
	}

	public void setTranslucent(){
		transparency = (int) (hTrans * 100);
	}
	public void setTransparent(){
		transparency = (int) (hTrans * 60);
	}
	public void setOpaque(){
		transparency = 255;
	}
	public void setTransparency(String str){
		setOpaque();
		if(str.equals("transparent"))setTransparent();
		if(str.equals("translucent"))setTranslucent();
	}

	public float getBaseWidth(){ 
		return bWidth;
	}

	public float getHTrans(){
		return hTrans;
	}

	public int getTransparency(){
		return transparency;
	}

	public Color getPaint(){
		if(transparency != 255){
			Color co = getColor();
			int trans = transparency;
			return new Color(co.getRed(), co.getGreen(), co.getBlue(), trans);
		}
		return getColor();
	}

	public Color getColor(){return getColor(color);}

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

	public int getTrapColor(){
		if(color.equals("black")) return 0;
		if(color.equals("blue")) return 1;
		if(color.equals("green")) return 2;
		if(color.equals("gray")) return 3;
		if(color.equals("magenta")) return 4;
		if(color.equals("orange")) return 5;
		if(color.equals("pink")) return 6;
		if(color.equals("red")) return 7;
		if(color.equals("white")) return 8;
		if(color.equals("yellow")) return 9;
		return -1;
	}

	public void copy(Jtool jt){
		this.width = jt.width;
		this.color = jt.color;
		this.transparency = jt.transparency;
		this.fatWidth = jt.fatWidth;
		this.bWidth = jt.bWidth;
		this.hTrans = jt.hTrans;
	}

	public void fullCopy(Jtool jt){
		copy(jt);
		this.type = jt.type;
		this.highlighter = jt.highlighter;
		this.transparency = jt.transparency;
	}

	public String desc(){
		String pen = "pen";
		if(highlighter) pen = "highlighter";
		if(transparency == 100) pen = "translucent";
		if(transparency == 60) pen = "transparent";
		return type + " " + color + " " + pen;
	}

	static public String getRGB(Color c){
		String R = Integer.toHexString(c.getRed());
		if(R.length() == 1) R = "0" + R;
		String G = Integer.toHexString(c.getGreen());
		if(G.length() == 1) G = "0" + G;
		String B = Integer.toHexString(c.getBlue());
		if(B.length() == 1) B = "0" + B;
		return R + G + B;
	}

	static public String lastToHtml(String s){
		s = s.trim();
		int n = s.lastIndexOf(" ");
		if(n < 0) return s;
		String c = s.substring(n + 1);
		s = s.substring(0, n) + " <font color=#" + getRGB(getColor(c.trim())) + ">" + c.trim() + "</font>";
		return s;		
	}

	public String htmlDesc(){
		String pen = "pen";
		if(highlighter) pen = "highlighter";
		if(transparency == 100) pen = "translucent";
		if(transparency == 60) pen = "transparent";		
		return type + " <font color=#" + getRGB(getColor()) + ">" + color + "</font> " + pen;
	}

	public penDialogListener showDialog(JFrame jf, Jtool curPen, Jtool defPen, Jtool defHigh, Jtool defBut, Jarnal jarn){
		penDialogListener pdl = new penDialogListener();
		return pdl.showDialog(jf, curPen, defPen, defHigh, defBut, this, jarn);
	}

	class penDialogListener implements ActionListener {

		JDialog jw;
		JComboBox combo1;
		JComboBox combo2;
		JComboBox combo3;
		SpinnerNumberModel model1;
		SpinnerNumberModel model2;
		SpinnerNumberModel model3;
		SpinnerNumberModel model4;
		Jtool curPen;
		Jtool defPen;
		Jtool defHigh;
		Jtool defBut;
		Jtool parent;
		Jtool oldparent;
		Jarnal jarn;
		String highlighterStyle;
		boolean done = false;
		boolean dirty = false;

		public void setDialog(){
	    		if(type.equals("Fine")) combo1.setSelectedIndex(0);
			if(type.equals("Medium")) combo1.setSelectedIndex(1);
			if(type.equals("Heavy")) combo1.setSelectedIndex(2);
			if(type.equals("Fat")) combo1.setSelectedIndex(3);
			combo2.setSelectedIndex(getTrapColor());
			if(highlighter) combo3.setSelectedIndex(3);
			else if(transparency == 100) combo3.setSelectedIndex(1);
			else if(transparency == 60) combo3.setSelectedIndex(2);
			else combo3.setSelectedIndex(0);
			model1.setValue(new Double(bWidth));
			model2.setValue(new Double(fatWidth));
			model3.setValue(new Integer(jarn.markerweight));
			model4.setValue(new Double(hTrans));
		}

		public penDialogListener showDialog(JFrame jf, Jtool curPen, Jtool defPen, Jtool defHigh, Jtool defBut, Jtool parent, Jarnal jarn){
			this.curPen = curPen;
			this.defPen = defPen;
			this.defHigh = defHigh;
			this.defBut = defBut;
			this.parent = parent;
			this.jarn = jarn;
			oldparent = new Jtool();
			oldparent.fullCopy(parent);
			jw = new JDialog(jf, "Choose Pen", true);
			jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			Container cp = jw.getContentPane();
			cp.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.weightx = 0.5;
			c.gridwidth = 1;
			c.gridx = 0;
			c.gridy = 5;
			c.anchor = GridBagConstraints.SOUTHWEST;
			JLabel label = new JLabel("Base Pen Thickness");
			cp.add(label, c);
			c.gridy = 6;
			c.anchor = GridBagConstraints.NORTHWEST;
			model1 = new SpinnerNumberModel(1.0, 0, 100.0, 0.1);
			JSpinner spinner = new JSpinner(model1);
			cp.add(spinner, c);
			c.gridy = 7;
			c.anchor = GridBagConstraints.SOUTHWEST;
			label = new JLabel("Base Highlighter Transparency");
			cp.add(label, c);
			c.gridy = 8;
			c.anchor = GridBagConstraints.NORTHWEST;
			model4 = new SpinnerNumberModel(1.0, 0, 100.0, 0.1);
			spinner = new JSpinner(model4);
			cp.add(spinner, c);
			c.gridy = 9;
			c.anchor = GridBagConstraints.SOUTHWEST;
			label = new JLabel("Fat Width");
			cp.add(label, c);
			c.gridy = 10;
			c.anchor = GridBagConstraints.NORTHWEST;
			model2 = new SpinnerNumberModel(1.0, 0, 100.0, 0.1);
			spinner = new JSpinner(model2);
			cp.add(spinner, c);
			c.gridy = 11;
			c.anchor = GridBagConstraints.SOUTHWEST;
			label = new JLabel("Arrow Weight");
			cp.add(label, c);
			c.gridy = 12;
			c.anchor = GridBagConstraints.NORTHWEST;
			model3 = new SpinnerNumberModel(10, -100, 100, 1);
			spinner = new JSpinner(model3);
			cp.add(spinner, c);
			c.gridy = 1;
			cp.add(new JLabel(" "), c);
			c.gridy = 2;
			String sels1[] = {"Fine", "Medium", "Heavy", "Fat"};
			combo1 = new JComboBox(sels1);
			combo1.addActionListener(this);
			cp.add(combo1, c);
			c.gridy = 3;
			String sels2[] = {"black", "blue", "green", "gray", "magenta", "orange", "pink", "red", "white", "yellow"};
			JLabel jmi[] = new JLabel[10];
			for(int ii = 0; ii < 10; ii++) jmi[ii] = new JLabel(sels2[ii], new colorIcon(sels2[ii]), JLabel.CENTER);
			combo2 = new JComboBox(jmi); 
			combo2.setRenderer(new labelCellRenderer());
			combo2.addActionListener(this);
	    		cp.add(combo2, c);
			c.gridy = 4;
			//c.gridx = 0;
			String sels3[] = {"Pen", "Translucent Highlighter", "Transparent Highlighter", "Bottom Highlighter"};
			combo3 = new JComboBox(sels3);
			combo3.addActionListener(this);
	    		cp.add(combo3, c);

			c.anchor = GridBagConstraints.NORTHEAST;
			c.gridy = 0;
			c.gridx = 1;
			JButton item = new JButton("Cancel");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy = 2;		
			item = new JButton("Get Default Pen");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy = 3;		
			item = new JButton("Get Default Highlighter");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy = 4;		
			item = new JButton("Get Button Pen");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy = 5;		
			item = new JButton("Get Current Pen");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
			c.gridy++;
			item = new JButton("Set Globals");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;		
			item = new JButton("Set Default");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
			item = new JButton("Set Button Pen");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
			item = new JButton("Set Current Pen");
			item.addActionListener(this);
			cp.add(item, c);
			setDialog();
            		jw.setSize(Jarnbox.newDimension(500, 320));
			Jarnbox.setCenter(jf, jw);
			jw.addWindowListener(new dialogClosing(this, "Cancel"));
            		jw.setVisible(true);
			while(!done){}
			return this;
		}
		
		public void actionPerformed(ActionEvent e){

			String action = e.getActionCommand();
			if(action.equals("comboBoxChanged")){
				JComboBox cb = (JComboBox)e.getSource();
				String test = null;
				try{
        				test = (String)cb.getSelectedItem();
				}
				catch(Exception ex){
					JLabel jl = (JLabel)cb.getSelectedItem();
					test = jl.getText();
				}
			}
			if(action.startsWith("Get")){
				if(action.equals("Get Default Highlighter")) parent.fullCopy(defHigh);
				if(action.equals("Get Default Pen")) parent.fullCopy(defPen);
				if(action.equals("Get Current Pen")) parent.fullCopy(oldparent);
				if(action.equals("Get Button Pen")) parent.fullCopy(defBut);
				setDialog();
			}
			if(action.startsWith("Set")){
				JLabel jl = (JLabel)combo2.getSelectedItem();
				color = (String)jl.getText();
				String test = (String)combo3.getSelectedItem();
				highlighter = false;
				transparency = 255;
				if(test.equals("Translucent Highlighter")) {
					setTranslucent();
					if(action.equals("Set Default")) highlighterStyle = "translucent";
				}
				if(test.equals("Transparent Highlighter")) {
					setTransparent();
					if(action.equals("Set Default")) highlighterStyle = "transparent";
				}
				if(test.equals("Bottom Highlighter")) {
					highlighter = true;
					if(action.equals("Set Default")) highlighterStyle = "bottom";
				}
				float x = model2.getNumber().floatValue();
				defHigh.fatWidth = x;
				defPen.fatWidth = x;
				curPen.fatWidth = x;
				defBut.fatWidth = x;
				parent.fatWidth = x;
				x = model1.getNumber().floatValue();
				defHigh.bWidth = x;
				defPen.bWidth = x;
				curPen.bWidth = x;
				defBut.bWidth = x;
				parent.bWidth = x;
				x = model4.getNumber().floatValue();
				defHigh.hTrans = x;
				defPen.hTrans = x;
				curPen.hTrans = x;
				defBut.hTrans = x;
				parent.hTrans = x;				
				jarn.markerweight = model3.getNumber().intValue();
				setWidth((String)combo1.getSelectedItem());
				boolean setHigh = false;
				if(highlighter || (transparency != 255)) setHigh = true;
				if(action.equals("Set Default")){
					if(setHigh) defHigh.fullCopy(parent);
					else defPen.fullCopy(parent);
				}
				if(action.equals("Set Button Pen")) defBut.fullCopy(parent);
				if(action.equals("Set Current Pen")) curPen.fullCopy(parent);
				defHigh.setWidth(defHigh.type);
				defPen.setWidth(defPen.type);
				defBut.setWidth(defBut.type);
				curPen.setWidth(curPen.type);
				jw.setVisible(false);
				dirty = true;
				done = true;
			}				
			if(action.equals("Cancel")){
				jw.setVisible(false);
				dirty = false;
				done = true;
			}
		}
	}

}

class labelCellRenderer extends JLabel implements ListCellRenderer {

	private static final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);

	public labelCellRenderer() {
		setOpaque(true);
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		JLabel jl = (JLabel) value;
		setText(jl.getText());
		setIcon(jl.getIcon());
		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		} 
		else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		return this;
	}
}

class colorIcon implements Icon {
	String col = "gray";
	int height = 6;
	int width = 6;
	
	public colorIcon(String col){
		this.col = col;
	}

	public int getIconHeight(){return height;}
	public int getIconWidth(){return width;}
	
   	public void paintIcon(Component c, Graphics g, int x, int y) {
		Color cc = g.getColor();
		Color cn = Jtool.getColor(col);
		if(col == "no change") cn = Color.lightGray;
            	g.setColor(cn);
        	g.fillRect(x, y, width, height);
		g.setColor(cc);
	}
}

class JarnalSelection implements Transferable, ClipboardOwner {

    DataFlavor flavors[] = {DataFlavor.stringFlavor, new DataFlavor("text/html; class=java.lang.String", "HTML (HyperText Markup Language)"), new DataFlavor("jaj/pair; class=java.lang.String", "Jarnal Clipboard Data")};

    private String data_plain, data_html;
    private String data_jaj[] = new String[2];
    public JarnalSelection(String data_plain, String data_html, String data_jaj, String jaj_type) {
        this.data_plain = data_plain;
	this.data_html = data_html;
	this.data_jaj[0] = data_jaj;
	this.data_jaj[1] = jaj_type;
    }

    public synchronized DataFlavor[] getTransferDataFlavors() {
	return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
	return (flavor.equals(flavors[0]) || flavor.equals(flavors[1]) || flavor.equals(flavors[2]));
    }

    public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
	if (flavor.equals(flavors[0])) return (Object) data_plain;
	else if (flavor.equals(flavors[1])) return (Object) data_html;
	else if (flavor.equals(flavors[2])) return (Object) data_jaj;
	else throw new UnsupportedFlavorException(flavor);
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}

class b64 {
	static int len;
	static int ng;
	static int ni;
	static int nout;
	static byte b[] = new byte[3];
	static byte ba[];
	static char c[] = new char[4];
	static char ca[];

	static int ml = 75;
	static int nl;
	static char[] ct = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();

	static byte getB(){
		if(ni >= len) return (byte) 0;
		byte b = ba[ni];
		ni++;
		return b;	
	}

	static void putC(char c){
		if(nl >= ml){
			nl = 0;
			ca[nout] = '\n';
			nout++;
		}
		else nl++;
		ca[nout] = c;
		nout++;
	}
	
	public static String encode(byte bax[]){
		ba = bax;
		ni = 0;
		nout = 0;
		nl = 0; //is this a mistake - it means the first line is shorter than the others; set nl = -1?
		len = ba.length;
		if(len == 0) return null;
		ng = (len + 2)/3;
		ca = new char[(4 * ng) + ((4 * ng)/ml)];
		for(int ii = 0; ii < ng; ii++){
			for(int jj = 0; jj < 3; jj++) b[jj] = getB();	
            		c[0] = ct[(b[0] >>> 2) & 0x3f];
            		c[1] = ct[((b[0] << 4) & 0x30) + ((b[1] >>> 4) & 0xf)];
            		c[2] = ct[((b[1]) << 2 & 0x3c) + ((b[2] >>> 6) & 0x3)];
            		c[3] = ct[b[2] & 0x3f];
			if(ni >= len){
				int jj = (3 * ng) - len;
				for(int kk = 0; kk < jj; kk++) c[3 - kk] = ct[64];
			}
			for(int jj = 0; jj < 4; jj++) putC(c[jj]);
		}
		return new String(ca);
	}

//Robert W. Harder's public domain base64 decoding

    	private final static byte[] DECODABET = {   
        	-9,-9,-9,-9,-9,-9,-9,-9,-9,                 // Decimal  0 -  8
        	-5,-5,                                      // Whitespace: Tab and Linefeed
        	-9,-9,                                      // Decimal 11 - 12
        	-5,                                         // Whitespace: Carriage Return
        	-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 14 - 26
        	-9,-9,-9,-9,-9,                             // Decimal 27 - 31
        	-5,                                         // Whitespace: Space
        	-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,              // Decimal 33 - 42
        	62,                                         // Plus sign at decimal 43
        	-9,-9,-9,                                   // Decimal 44 - 46
        	63,                                         // Slash at decimal 47
        	52,53,54,55,56,57,58,59,60,61,              // Numbers zero through nine
        	-9,-9,-9,                                   // Decimal 58 - 60
        	-1,                                         // Equals sign at decimal 61
        	-9,-9,-9,                                      // Decimal 62 - 64
        	0,1,2,3,4,5,6,7,8,9,10,11,12,13,            // Letters 'A' through 'N'
        	14,15,16,17,18,19,20,21,22,23,24,25,        // Letters 'O' through 'Z'
        	-9,-9,-9,-9,-9,-9,                          // Decimal 91 - 96
        	26,27,28,29,30,31,32,33,34,35,36,37,38,     // Letters 'a' through 'm'
        	39,40,41,42,43,44,45,46,47,48,49,50,51,     // Letters 'n' through 'z'
        	-9,-9,-9,-9                                 // Decimal 123 - 126
    	};
    	private final static byte WHITE_SPACE_ENC = -5; // Indicates white space in encoding
    	private final static byte EQUALS_SIGN_ENC = -1; // Indicates equals sign in encoding
	private final static byte EQUALS_SIGN = (byte)'=';

 	private static int decode4to3( byte[] source, int srcOffset, byte[] destination, int destOffset ) {
        	// Example: Dk==
        	if( source[ srcOffset + 2] == EQUALS_SIGN ) {

            		int outBuff =   ( ( DECODABET[ source[ srcOffset    ] ] & 0xFF ) << 18 )
                          	| ( ( DECODABET[ source[ srcOffset + 1] ] & 0xFF ) << 12 );
            
            		destination[ destOffset ] = (byte)( outBuff >>> 16 );
            		return 1;
        	}
        
        	// Example: DkL=
        	else if( source[ srcOffset + 3 ] == EQUALS_SIGN ) {
            		int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] & 0xFF ) << 18 )
                          	| ( ( DECODABET[ source[ srcOffset + 1 ] ] & 0xFF ) << 12 )
                          	| ( ( DECODABET[ source[ srcOffset + 2 ] ] & 0xFF ) <<  6 );
            
            		destination[ destOffset     ] = (byte)( outBuff >>> 16 );
            		destination[ destOffset + 1 ] = (byte)( outBuff >>>  8 );
            		return 2;
        	}
        
        	// Example: DkLE
        	else {
            		try{
            			int outBuff =   ( ( DECODABET[ source[ srcOffset     ] ] & 0xFF ) << 18 )
                          		| ( ( DECODABET[ source[ srcOffset + 1 ] ] & 0xFF ) << 12 )
                          		| ( ( DECODABET[ source[ srcOffset + 2 ] ] & 0xFF ) <<  6)
                          		| ( ( DECODABET[ source[ srcOffset + 3 ] ] & 0xFF )      );
            
            			destination[ destOffset     ] = (byte)( outBuff >> 16 );
            			destination[ destOffset + 1 ] = (byte)( outBuff >>  8 );
            			destination[ destOffset + 2 ] = (byte)( outBuff       );
            			return 3;
            		}
			catch( Exception e){
                		System.out.println(""+source[srcOffset]+ ": " + ( DECODABET[ source[ srcOffset     ] ]  ) );
                		System.out.println(""+source[srcOffset+1]+  ": " + ( DECODABET[ source[ srcOffset + 1 ] ]  ) );
                		System.out.println(""+source[srcOffset+2]+  ": " + ( DECODABET[ source[ srcOffset + 2 ] ]  ) );
                		System.out.println(""+source[srcOffset+3]+  ": " + ( DECODABET[ source[ srcOffset + 3 ] ]  ) );
                		return -1;
            		}
        	}
    	}

 	private static byte[] decode( byte[] source, int off, int len ) {
        	int    len34   = len * 3 / 4;
        	byte[] outBuff = new byte[ len34 ]; // Upper limit on size of output
        	int    outBuffPosn = 0;
        
        	byte[] b4        = new byte[4];
        	int    b4Posn    = 0;
        	int    i         = 0;
        	byte   sbiCrop   = 0;
        	byte   sbiDecode = 0;
        	for( i = off; i < off+len; i++ ) {
            		sbiCrop = (byte)(source[i] & 0x7f); // Only the low seven bits
            		sbiDecode = DECODABET[ sbiCrop ];
            
            		if( sbiDecode >= WHITE_SPACE_ENC ) {// White space, Equals sign or better
                		if( sbiDecode >= EQUALS_SIGN_ENC ) {
                    			b4[ b4Posn++ ] = sbiCrop;
                    			if( b4Posn > 3 ) {
                        			outBuffPosn += decode4to3( b4, 0, outBuff, outBuffPosn );
                        			b4Posn = 0;
                        
                        			// If that was the equals sign, break out of 'for' loop
                        			if( sbiCrop == EQUALS_SIGN )
                            				break;
                    				}   // end if: quartet built
                    
                			}   // end if: equals sign or better
                
           			}   // end if: white space, equals sign or better
            			else {
                		System.err.println( "Bad Base64 input character at " + i + ": " + source[i] + "(decimal)" );
                		return null;
            		} 
        	}   // each input character
                                   
        	byte[] out = new byte[ outBuffPosn ];
        	System.arraycopy( outBuff, 0, out, 0, outBuffPosn ); 
        	return out;
    	}
    

	public static byte[] decode(String str){
		byte bin[] = str.getBytes();
		byte bax[] = decode(bin, 0, bin.length);
		return bax;
	}
}

class HtmlPost {

	public HtmlPost(String xserver, String xmessage, Jpages xjpages, Hashtable xht, String xconf, boolean xurlencoded){
		urlencoded = xurlencoded;
		server = xserver;
		message = xmessage + "\n";
		jpages = xjpages;
		if(xht != null){
			ht = xht;
			fname = (String) ht.get("$f");
			if(fname == null) {
				fname="noname";
				ht.put("$f", fname);
			}
		}
		conf = xconf;
		boundary = "---------------------------";
		for(int ii = 0; ii < 3; ii++) boundary = boundary + Long.toString((new Random()).nextLong(), 36);
	}
	public boolean withBorders = false;
	private String server;
	private String message;
	private Jpages jpages;
	private Jarnal jarn;
	private String boundary;
	private HttpURLConnection conn;
	private OutputStream out;
	private Hashtable ht;
	private String conf;
	private String crlf = "\r\n";
	private String fname = "noname";
	private boolean urlencoded = false;
	public String serverMsg;
	public boolean netError = false;

	public static boolean checkURL(String s){
		if(s.length() <= 7) return false;
		if(s.substring(0,7).equals("http://")) return true;
		if(s.length() == 8) return false;
		if(s.substring(0,8).equals("https://")) return true;
		return false;
	}

	public void setJarnal(Jarnal xjarn){
		jarn = xjarn;
	}

	private String urlencode(String s){
		String ans = s;
		try{
			ans = URLEncoder.encode(s, "UTF-8");
		}
		catch(java.io.UnsupportedEncodingException uee){System.err.println(uee);}
		return ans;
	}
			

	private void writeVar(String key, String val){
		String prn = "";
		if(!urlencoded){
			prn = "--" + boundary + crlf + "Content-Disposition: form-data; name=\"";
			prn = prn + key + "\"";
			prn = prn + crlf + crlf;
			prn = prn + val + crlf;
		}
		else prn = key + "=" + urlencode(val) + "&";
		try {
			out.write(prn.getBytes());
		}
		catch(IOException ex){}
	}

	private void writeFile(String key, String fname, String op){
		String prn = "";
		if(!urlencoded){
			prn = "--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"";
			prn = prn + key + "\"; filename=\"";
			String xfname = fname;
			if(op.equals("$$snapshot")) xfname = "attach.jpg";
			if(op.equals("$$tiff")) xfname = "attach.tif";
			if(op.equals("$$pdf")) xfname = fname + ".pdf";
			prn = prn + xfname + "\"";
			prn = prn + crlf;
			prn = prn + "Content-Type: \"application/octet-stream\"";
			prn = prn + crlf + crlf;
			try {
				out.write(prn.getBytes());
				if(op.equals("$$pdf")){
					jarn.netsaveos = out;
					jarn.jrnlPane.doAction("zNetSave as PDF");
				}
				else jpages.netWrite(out, op, conf, withBorders);
				out.write(crlf.getBytes());
			}
			catch(IOException ex){};
		}
		else {
			prn = key + "=";
			ByteArrayOutputStream baost = new ByteArrayOutputStream();
			jpages.netWrite(baost, op, conf, withBorders);
			prn = prn + urlencode(b64.encode(baost.toByteArray())) + "&";
			try {
				out.write(prn.getBytes());
			}
			catch(IOException ex){}
		}
	}

	private String parseline(){
		if(message.equals("")) return null;
		int pos = message.indexOf("\n");
		if (pos < 0) return null;
		String ans = message.substring(0, pos);
		ans = ans.trim();
		message = message.substring(pos + 1);
		return ans;
	}

	private void writeLine(String line){
		if(line == null){
			System.err.println("null line");
			return;
		}
		int pos = line.indexOf("=");
		if(pos < 0) return;
		String key = line.substring(0, pos);
		String val = line.substring(pos + 1);
		if(val.equals("")) val = "none";
		if(val.substring(0,1).equals("$")){
			if(val.equals("$$jarnal") || val.equals("$$snapshot") || val.equals("$$pdf") || val.equals("$$tiff")) writeFile(key, fname, val);
			else {
				String xval = (String) ht.get(val);
				if(xval != null){
					writeVar(key, xval);
				}
			}
		}
		else writeVar(key, val);
	}

	public byte[] pipeBytes(){
		byte ba[] = null;
		try{
			URL url = new URL(server);
      			conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();	
			ByteArrayOutputStream tout = new ByteArrayOutputStream();
			int nmax = 10000; 
			byte b[] = new byte[nmax + 1];
			int nread = 0;
			while((nread = is.read(b, 0, nmax)) >= 0)tout.write(b,0, nread);
			ba = tout.toByteArray();
		}
		catch(Exception ex){System.err.println(ex);}
		return ba;
	}

	public boolean pipeFile(String fname){
		try{
			URL url = new URL(server);
      			conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();	
			FileOutputStream tout = new FileOutputStream(fname);
			int nmax = 10000; 
			byte b[] = new byte[nmax + 1];
			int nread = 0;
			while((nread = is.read(b, 0, nmax)) >= 0)tout.write(b,0, nread);
			return true;
		}
		catch(Exception ex){System.err.println(ex);}
		return false;
	}

	public String pipe(String ext){
		String nfile = null;
		try{
			URL url = new URL(server);
      			conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();	
			File tfile = File.createTempFile(Jarnal.jarnalTmp, ext);
			nfile = tfile.getPath();
			FileOutputStream tout = new FileOutputStream(tfile);
			int nmax = 10000; 
			byte b[] = new byte[nmax + 1];
			int nread = 0;
			while((nread = is.read(b, 0, nmax)) >= 0)tout.write(b,0, nread);
		}
		catch(Exception ex){System.err.println(ex);}
		return nfile;
	}

	public void post(){
		netError = false;
		try{
			URL url = new URL(server);
      			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
      			conn.setDoOutput(true);
			if(!urlencoded) conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			out = new ByteArrayOutputStream();
			String line;
			while((line = parseline()) != null) writeLine(line);
			if(!urlencoded) {
				String term = "--" + boundary + "--";
				out.write(term.getBytes());
				conn.setRequestProperty("Content-Length", "" + ((ByteArrayOutputStream) out).size());
			}
			OutputStream hout = conn.getOutputStream();
			((ByteArrayOutputStream) out).writeTo(hout);
			out.close();
			hout.close();
			InputStream is = conn.getInputStream();
			int nmax = 100000;
			int nread;
			byte b[] = new byte[nmax];
			serverMsg = "";
			while((nread = is.read(b)) >= 0) serverMsg = serverMsg + new String(b, 0, nread);
			System.out.print(serverMsg);
		}
		catch(Exception ex){System.err.println(ex); netError = true;}
	}

}

