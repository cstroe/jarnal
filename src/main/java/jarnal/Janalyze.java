package jarnal;

import javax.swing.*;
import java.util.*;
import java.lang.Math.*;
import java.lang.Number.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.image.*;
import java.awt.font.*;

class JDictionaryEditor extends JPanel {

	public JPanel jpan = new JPanel(new BorderLayout()); 
	public Janalyze janal;
	public String dname;
	public JList list1;
	public JList list2;
	private File dics[];
	public JFrame jf;
	public JLabel currentStr = new JLabel(" ");
	public LinkedList stroke;
	public LinkedList breaks;
	public JComboBox jc;

	public JDictionaryEditor() {
		jf = new JFrame("Edit Recognition Dictionaries");
		Container cp = jf.getContentPane();
		LinkedList sels0 = new LinkedList();
		for (Enumeration e = Janalyze.hashdictin.keys(); e.hasMoreElements() ;) {
			String test = (String) e.nextElement();
			if(!test.startsWith("xx")){
				sels0.add(test);
			}
		}
		int n = sels0.size();
		String sels[] = new String[n];
		for(int i = 0; i < n; i++) sels[i] = (String) sels0.get(n - i - 1);	
		dname = sels[0];
		jc = new JComboBox(sels);
	    	jc.addActionListener(new dictActionListener("combo", this));
		cp.add(jc, BorderLayout.NORTH);
		JPanel ccp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		cp.add(ccp, BorderLayout.CENTER);
		setPreferredSize(new Dimension(235, 168));
		JPanel cpp = new JPanel(new BorderLayout());
		cpp.add(this, BorderLayout.CENTER);
		JToolBar jtb = new JToolBar(javax.swing.SwingConstants.VERTICAL);
		cpp.add(jtb, BorderLayout.WEST);
		jtb.add(bjb("Ed", "Edit Item"));
		jtb.add(bjb("Del", "Delete Item"));
		jtb.addSeparator();
		jtb.add(bjb("Clr", "Clear Dictionary"));
		cpp.add(currentStr, BorderLayout.NORTH);
		ccp.add(cpp);
		ccp.add(jpan);
		list1 = new JList(); 
	    	list1.addListSelectionListener(new dictListSelectionListener("list1", this));
	    	list1.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	    	list1.setVisibleRowCount(-1);
	    	JScrollPane listScroller = new JScrollPane(list1);
	    	listScroller.setPreferredSize(new Dimension(40, 200));
	    	cp.add(listScroller, BorderLayout.WEST);
		list2 = new JList(); 
	    	list2.addListSelectionListener(new dictListSelectionListener("list2", this));
	    	list2.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	    	list2.setVisibleRowCount(-1);
		updateList2();
	    	JScrollPane listScroller2 = new JScrollPane(list2);
	    	listScroller2.setPreferredSize(new Dimension(40, 200));
	    	cp.add(listScroller2, BorderLayout.EAST);
		jf.setSize(new Dimension(400, 440));
		jf.setVisible(true);
	}

	public JButton bjb(String action, String tooltip){
        	JButton item;
        	item = new JButton(action);
		item.setToolTipText(tooltip);
        	item.addActionListener(new dictActionListener(action, this));
        	return item;
	}

	private void updateList2(){
		char v[] = new char[1];
		char exc = '!';
		String listdata[] = new String [127 - exc];
		for(char c = exc; c < 127; c++){
			v[0] = c;
			listdata[c - exc] = new String(v);
		}
		list2.setListData(listdata);
	}
			

	public void updateList1(){
		String selstr = (String)jc.getSelectedItem();
		janal.setDictionary(selstr);
		dname = selstr;
		String listdata[] = new String[janal.dictout.size()];
		for(int ii = 0; ii < janal.dictout.size(); ii++)
			listdata[ii] =(String) janal.dictout.get(ii);
		list1.setListData(listdata);
	}
	
	private Point2D.Double rescale(Point2D.Double p){
		double scale = 100.0;
		double offsetX = 20.0;
		double offsetY = 20.0;
		return new Point2D.Double(offsetX + (p.getX() * scale), offsetY + (p.getY() * scale));
	}

	private float sw = 3.0f;
	
	private void drawCircle(Point2D.Double click, Graphics2D g2, int num){
		num++;
		g2.fill(new Ellipse2D.Double(click.getX() - (2 * sw), click.getY() - (2 * sw), sw * 4, sw * 4));
		if(num > 1){
			g2.drawString("" + num, (int) (click.getX() - (2 * sw)), (int)(click.getY() - (2 * sw)));
		}
	}

	private int getBr(int bnum){
		if(breaks == null) return 0;
		if(bnum >= breaks.size()) return 0;
		return ((Integer) breaks.get(bnum)).intValue();
	}		

    	public void paintComponent(Graphics g){
		if(g == null) return;
		setBackground(Color.white);
		super.paintComponent(g);	
		Graphics2D g2 = (Graphics2D) g;
		int w = getWidth();
		int h = getHeight();
		g2.clearRect(0, 0, w, h);
		g2.setPaint(Color.BLACK);
		BasicStroke bs = new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		g2.setStroke(bs);
		if((stroke != null) && (stroke.size() > 1)){
			LinkedList xn = janal.xn(stroke);
			int bnum = 0;
			int br = getBr(bnum);
			Point2D.Double p0 = rescale((Point2D.Double) xn.get(0));
			drawCircle(p0, g2, -1);
			for(int i = 1; i < stroke.size(); i++) {
				Point2D.Double p1 = rescale((Point2D.Double) xn.get(i));
				Line2D.Double ll = new Line2D.Double(p0, p1);
				if((i == br + 1) && (br != 0)){
					bnum++;
					br = getBr(bnum);
					drawCircle(p1, g2, bnum);
				}
				else g2.draw(ll);					
				p0 = p1;
			}
		}
		if((stroke != null) && (stroke.size() == 1)) drawCircle(rescale((Point2D.Double) stroke.get(0)), g2, -1);
		g2.dispose();
	}

}

class dictActionListener implements ActionListener {
	private String action;
	private JDictionaryEditor jd;

	public dictActionListener(String action, JDictionaryEditor jd){
		this.jd = jd;
		this.action = action;
	}

	public void actionPerformed(ActionEvent e) {
		if(action.equals("combo")){
			jd.updateList1();
		}
		if(action.equals("Clr")){
 			int n = 0;
			n = JOptionPane.showConfirmDialog(jd.jf, "OK to Clear Entire Dictionary?",    "Confirm Delete", JOptionPane.YES_NO_OPTION);
			if(n == 0) {
				String selstr = (String)jd.jc.getSelectedItem();
				jd.janal.setDictionary(selstr);
				jd.dname = selstr;
				jd.janal.clearDictionary();
				jd.updateList1();
			}
		}
		if(action.equals("Del")){
			int m = jd.list1.getSelectedIndex();
			if(m >= jd.janal.dictin.size()) return;
			if(m < 0) return;
			String selstr = (String)jd.jc.getSelectedItem();
			jd.janal.setDictionary(selstr);
			jd.dname = selstr;
 			int n = 0;
			n = JOptionPane.showConfirmDialog(jd.jf, "OK to delete " + jd.list1.getSelectedValue() + " from " + jd.dname,    "Confirm Delete", JOptionPane.YES_NO_OPTION);
			if(n == 0) {
				jd.janal.clearItem(m);
				jd.updateList1();
			}
		}
		if(action.equals("Ed")){
			int m = jd.list1.getSelectedIndex();
			if(m >= jd.janal.dictin.size()) return;
			if(m < 0) return;
			String selstr = (String)jd.jc.getSelectedItem();
			jd.janal.setDictionary(selstr);
			jd.dname = selstr;
			String ans = JOptionPane.showInputDialog(jd.jf, "Edit " + jd.list1.getSelectedValue(), jd.list1.getSelectedValue());
			if(ans != null) {
				jd.janal.dictout.set(m, ans);
				jd.updateList1();
			}
		}					
	}
}


class dictListSelectionListener implements ListSelectionListener {
	private JDictionaryEditor jd;
	private String action;

	public dictListSelectionListener(String action, JDictionaryEditor jd){
		this.jd = jd;
		this.action = action;
	}

    	public void valueChanged(ListSelectionEvent e) {
		if(action.equals("list2")) jd.janal.sug0 = (String) jd.list2.getSelectedValue();
		if(action.equals("list1")){
			String selstr = (String)jd.jc.getSelectedItem();
			jd.janal.setDictionary(selstr);
			jd.dname = selstr;
			jd.currentStr.setText("             " + (String) jd.list1.getSelectedValue());
			int n = jd.list1.getSelectedIndex();
			if(n >= jd.janal.dictin.size()) return;
			if(n < 0) return;
			jd.stroke = (LinkedList) jd.janal.dictin.get(n);
			jd.breaks = (LinkedList) jd.janal.dictbr.get(n);
			jd.repaint();
		}
	}
}


public class Janalyze {

	static private LinkedList basedictin;
	static private LinkedList basedictrin;
	static private LinkedList basedictout;
	static private LinkedList basedictn;
	static private LinkedList basedictbr;
	static public Hashtable hashdictin = new Hashtable();
	static private Hashtable hashdictrin = new Hashtable();
	static private Hashtable hashdictout = new Hashtable();
	static private Hashtable hashdictn = new Hashtable();
	static private Hashtable hashdictbr = new Hashtable();
	static private Hashtable hashdiction = new Hashtable();
	static private String diction;
	public LinkedList dictin;
	private LinkedList dictrin;
	public LinkedList dictout;
	private LinkedList dictn;
	public LinkedList dictbr;
	static private InputStream fis = null;
	static private InputStream in = null;
	private double pairs[][];
	static private double uniformpairs[][];
	static private double filepairs[][];
	//static public boolean defaultfilepairs = true;
	static private boolean loaded = false;
	static final double ratconst = 1.0/128.0;

	static private String instr = "";

	public JDictionaryEditor jdic;

	public boolean train = false;
	static public boolean dirty = false;
	public boolean defaultfilepairs;

	public String sug0 = null;

	public Janalyze(boolean defaultfilepairs){
		this.defaultfilepairs = defaultfilepairs;
		if(defaultfilepairs) {
			pairs = filepairs;
			setDictionary("xxall");
		}
		else {
			pairs = uniformpairs;
			setDictionary("base");
		}
	}

	static public void initStream(InputStream in, InputStream fis, InputStream din){
		if(loaded) return;
		loaded = true;
		filepairs = new double[128][128];
		uniformpairs = new double[128][128];
		//double ratconst = 1.0/128.0;
		for(int ii = 0; ii < 128; ii++){
			for(int jj = 0; jj < 128; jj++) filepairs[jj][ii] = 64.0 * ratconst;
			for(int jj = 0; jj < 128; jj++) uniformpairs[jj][ii] = ratconst;
		}
		byte b[] = new byte[1];
		String s = "";
		int ii = 0;
		int jj = 0;
		if(in != null) {try {
			b[0] = (byte) in.read(); 
			String test = new String(b,0,1);
			if(test.equals("$")){
				for(ii = 0; ii < 128; ii++){
					for(jj = 0; jj < 128; jj++) filepairs[ii][jj] = 0.05;
				}
				int ii0 = 0;
				int jj0 = 0;
				b[0] = (byte) in.read();
				while(b[0] != -1){
					String t = new String(b,0,1);
					if(t.equals("\t")){
						ii = ii0;
						jj = jj0;
						s = "";
					}
					else if(t.equals("\n")){
						filepairs[ii][jj] = filepairs[ii][jj] + Double.parseDouble(s);
						s = "";
					}
					else {
						ii0 = jj0;
						jj0 = (int) b[0];
						s = s + t;
					}
					b[0] = (byte) in.read();
				}
				for(ii = 0; ii < 128; ii++){
					for(jj = 0; jj < 128; jj++) filepairs[ii][jj] = filepairs[ii][jj]/100.0;
				}	
			}		
			else {
				while(b[0] != -1){
					String t = new String(b,0,1);
					if(t.equals("\n")){
						ii = 0;
						jj++;
					}
					else{
						if(t.equals(" ")){
							filepairs[jj][ii] = Double.parseDouble(s);
							s="";
							ii++;
						}
						else s = s + t;
					}
					b[0] = (byte) in.read();
				}
				String st = "$";
				for(ii = 0; ii < 128; ii++){
					for(jj = 0; jj < 128; jj++){
						int kk = (int)(100.0 * filepairs[ii][jj]);
						if(kk > 0) {
							b[0] = (byte) ii;
							s = new String(b,0,1);
							if((ii>31) && (ii<128)){
								b[0] = (byte) jj;
								String s2 = new String(b,0,1);
								if((jj>31) && (jj<128)) {
									st = st + s + s2;
									st = st + "\t" + kk + "\n";
								}
							}
						}
					}
				}
				FileOutputStream fos = new FileOutputStream(System.getProperty("user.home") + File.separator + "pairs.txt");
				fos.write(st.getBytes());
				fos.close();
			}			
		}
		catch (Exception ex) {ex.printStackTrace();}}
		if(din != null){try{
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			b[0] = (byte) din.read();
			String ts = "";
			while(b[0] != -1){
				String t = new String(b,0,1);
				if(t.equals("\n")) {
					Double dd = new Double(Double.parseDouble(s));
					hashdiction.put(ts, dd);
					s = "";
				}
				else{
					if(t.equals(" ")){
						bao.write(("$" + s).getBytes());
						ts = s;
						s="";
					}
					else s = s + t;
				}
			b[0] = (byte) din.read();
			}
			diction = new String(bao.toByteArray());
		}
		catch(Exception ex) {ex.printStackTrace();}}
		double df = 0;
		for(Enumeration e = hashdiction.elements(); e.hasMoreElements();) df = df + ((Double)e.nextElement()).doubleValue();
		Hashtable ht = hashdiction;
		hashdiction = new Hashtable();
		for(Enumeration e = ht.keys(); e.hasMoreElements();) {
			String str = (String) e.nextElement();
			hashdiction.put(str, new Double(((Double) ht.get(str)).doubleValue()/df));
		}
		initDictionary(fis);
	}

	static public LinkedList adic(LinkedList in, LinkedList toadd){
		if(in == null) in = new LinkedList();
		for(int i = 0; i < toadd.size(); i++) in.add(toadd.get(i));
		return in;
	}

	static public LinkedList getList(Hashtable ht, String str){
		LinkedList ll = (LinkedList) ht.get(str);
		if(ll == null) {
			ll = new LinkedList();
			ht.put(str, ll);
		}
		return ll;		
	}
	
	static public void addNew(){
		adic(getList(hashdictin, "xxall"), basedictin);
		adic(getList(hashdictrin, "xxall"), basedictrin);
		adic(getList(hashdictout, "xxall"), basedictout);
		adic(getList(hashdictn, "xxall"), basedictn);
		adic(getList(hashdictbr, "xxall"), basedictbr);
	}				

	static public void initDictionary(InputStream fis){
		initDictionary(fis, "base");
	}

	static public void initDictionary(InputStream fis, String dname){
		if(fis == null) return;	
		basedictin = new LinkedList(); //stroke
		basedictrin = new LinkedList(); //renormalized stroke
		basedictout = new LinkedList(); //character
		basedictn = new LinkedList(); //number of strokes
		basedictbr = new LinkedList();	//breakpositions
		ByteArrayOutputStream baost = new ByteArrayOutputStream();
		int nmin = 1000000;
		int nborg = 40000;
		int nmax = nmin + ( 5 * nborg); 
		byte b[] = new byte[nmax];
		int nread = 0;
		int noff = 0;
		try{
			while((nread = fis.read(b, noff, nborg)) >= 0){
				noff = noff + nread;
				if(noff > nmax - (2 * nborg)){
					baost.write(b, 0, noff);
					noff = 0;
				}
			}
				baost.write(b, 0, noff);
			byte c[] = baost.toByteArray();
			instr = new String(c);
		}
		catch(Exception ex){System.err.println(ex); return;}
		String line = getLine();
		int state = 0;
		LinkedList jstroke = null;
		try{
		while(line != null){
			if(state == 2){
				int pos = line.indexOf(" ");
				String sx = line.substring(0,pos);
				String sy = line.substring(pos + 1);
				double x = Double.parseDouble(sx);
				double y = Double.parseDouble(sy);
				jstroke.add(new Point2D.Double(x, y));
			}
			if(state == 1){
				state = 2;
				int pos = line.indexOf(" ");
				String s0 = null;
				boolean test = true;
				boolean start = true;
				int res = 1;
				LinkedList ls = new LinkedList();
				while(test){
					if(pos == -1){
						test = false;
						s0 = line;
					}
					else {
						s0 = line.substring(0, pos);
						line = line.substring(pos + 1);
					}
					res = Integer.parseInt(s0);
					if(start) {
						start = false;
						basedictn.add(new Integer(res));
					}
					else ls.add(new Integer(res));
					pos = line.indexOf(" ");
				}
				basedictbr.add(ls);
			}
				
			if(state == 0){
				if(!line.equals("")){
					state = 1;
					basedictout.add(line);
					jstroke = new LinkedList();
				}
			}
			if(state == 3) state = 0;
			line = getLine();
			if(line != null){
				if(line.equals("")) {
					state = 3;
					if(jstroke != null){
						basedictin.add(jstroke);
						Jrenorm rn = new Jrenorm(jstroke);
						basedictrin.add(rn.renormalize());
					}
					jstroke = null;
				}
			}
		}
		}
		catch(Exception ex){
			System.err.println(ex);
			//clearDictionary();
		}
		hashdictin.put(dname, basedictin);
		hashdictrin.put(dname, basedictrin);
		hashdictout.put(dname, basedictout);
		hashdictn.put(dname, basedictn);
		hashdictbr.put(dname, basedictbr);
		addNew();
		basedictin = (LinkedList) hashdictin.get("base");
		basedictrin = (LinkedList) hashdictrin.get("base");
		basedictout = (LinkedList) hashdictout.get("base");
		basedictn = (LinkedList) hashdictn.get("base");
		basedictbr = (LinkedList) hashdictbr.get("base");		
	}

	public void clearItem(int i){
		dictin.remove(i);
		dictrin.remove(i);
		dictout.remove(i);
		dictn.remove(i);
		dictbr.remove(i);
	}

	public void clearList(LinkedList ll){
		int n = ll.size();
		for(int i = 0; i < n; i++) ll.remove(0);
	}

	public void clearDictionary(){
		clearList(dictin);
		clearList(dictrin);
		clearList(dictout);
		clearList(dictn);
		clearList(dictbr);
	}

	public void setDictionary(String dname){
		dictin = (LinkedList) hashdictin.get(dname);
		dictrin = (LinkedList) hashdictrin.get(dname);
		dictout = (LinkedList) hashdictout.get(dname);
		dictn = (LinkedList) hashdictn.get(dname);
		dictbr = (LinkedList) hashdictbr.get(dname);
	}			

	static private String getLine(){
		if(instr.length() == 0) return null;
		int pos = instr.indexOf("\n");
		if(pos < 0) return null;
		String s = instr.substring(0, pos);
		instr = instr.substring(pos + 1);
		return s.trim();
	}

	static public void saveStream(OutputStream fos, String dname){
		//if(!dirty) return;
		LinkedList xbasedictin = (LinkedList) hashdictin.get(dname);
		if(xbasedictin == null) return;
		LinkedList xbasedictrin = (LinkedList) hashdictrin.get(dname);
		LinkedList xbasedictout = (LinkedList) hashdictout.get(dname);
		LinkedList xbasedictn = (LinkedList) hashdictn.get(dname);
		LinkedList xbasedictbr = (LinkedList) hashdictbr.get(dname);
		if(xbasedictin.size() == 0) return;
		if(fos == null) return;
		byte[] crlf = ("\n").getBytes();
		byte[] space = (" ").getBytes();
		try{
			for(int ii = 0; ii < xbasedictin.size(); ii++){
				LinkedList jstroke = (LinkedList) xbasedictin.get(ii);
				String jstr = (String) xbasedictout.get(ii);
				fos.write(jstr.getBytes());
				fos.write(crlf);
				Integer jii = (Integer) xbasedictn.get(ii);
				jstr = "" + jii;
				LinkedList ls = (LinkedList) xbasedictbr.get(ii);
				for(int ils = 0; ils < ls.size(); ils++){
					jii = (Integer) ls.get(ils);
					jstr = jstr + " " + jii;
				}
				fos.write(jstr.getBytes());
				fos.write(crlf);
				for(int jj = 0; jj < jstroke.size(); jj++){
					Point2D.Double p = (Point2D.Double) jstroke.get(jj);
					fos.write(("" + p.getX()).getBytes());
					fos.write(space);
					fos.write(("" + p.getY()).getBytes());
					fos.write(crlf);
				}
				fos.write(crlf);
			}
			fos.write(crlf);
		}
		catch(Exception ex){ex.printStackTrace();}
	}

	public void restr(String str){
		lastns = 1;
		lastnls = new LinkedList();
		lasts = null;
		lastr = null;
		rect = null;
		prevchar = " ";
		initWords();
		lastchar = str.substring(1);
		lastchar2 = str.substring(0,1);
	}

	public int getLastns(){
		return lastns;
	}

	static private double thresh = 0.02;

	private int lastns = 1;
	private LinkedList lastnls = new LinkedList();
	private LinkedList lasts = null;
	private Rectangle2D.Double lastr = null;
	private Rectangle2D.Double rect = null;
	private String lastchar = " ";
	private String lastchar2 = " ";
	private String prevchar = " ";
	private String thisword = "";
	private int ntwords = 5;
	//ntwords is supposed to be no larger than wnum
	private String thesewords[] = new String[ntwords];
	private double thesescores[] = new double[ntwords];
	private String lastwords[] = new String[ntwords];
	private double lastscores[] = new double[ntwords];

	private double sshape(double x){
		double y = 0.0;
		if(x >= 0.0) y = 1.0 - Math.exp(-x);
		if(x < 0.0)  y = Math.exp(2.0 * x) - 1.0;
		return (1.0 + y) / 2.0;
	}

	int which = -1;
	int wnum = 9;
	//wnum should be at least equal to ntwords
	int whiches[] = new int[wnum];
	double wscore[] = new double[wnum];
	double bscore = 100.0;
	double dscore = 100.0;
	LinkedList rnstroke;
	private void getscore(LinkedList nstroke, int nn){
		which = -1;
		for(int ii = 0; ii < wnum; ii++) {
			whiches[ii] = -1;
			wscore[ii] = 100.0;
		}
		double sscore = 100.0;
		bscore = 100.0;
		dscore = 100.0;
		Jrenorm rn = new Jrenorm(nstroke);
		rnstroke = rn.renormalize();
		if(dictin.size() != 0){
			for(int ii = 0; ii < dictin.size(); ii++){
				Integer ix = (Integer) dictn.get(ii);
				if(nn == ix.intValue()){
					sscore = score((LinkedList) dictin.get(ii), nstroke, ii, true);
					for(int jj = 0; jj < wnum; jj++){
						if(sscore < wscore[jj]){
							for(int jk = wnum - 1; jk > jj; jk--){
								whiches[jk] = whiches[jk - 1];
								wscore[jk] = wscore[jk - 1];
							}
							whiches[jj] = ii;
							wscore[jj] = sscore;
							break;
						}
					}
					if(sscore < bscore){
						bscore = sscore;
						which = ii;
					}
				}
				else if(nn < ix.intValue()){
					sscore = score((LinkedList) dictin.get(ii), nstroke, ii, false);
					if(sscore < dscore) dscore = sscore;
				}
			}
		}
		dscore = Math.min(bscore, dscore);
	}

	public String analyze(LinkedList lines){
		lastr = rect;
		LinkedList nstroke = normalize(lines);
		Rectangle2D.Double rect1 = rect;
		boolean addtodict = true;
		String sp = "";

		double psp = 0.0;
		double pcn = 0.0;
		if(lastr != null){
			double charwidth = (0.24 * (lastr.width + rect.width + (2 * Math.max(lastr.width, rect.width)))) + (0.12 * (lastr.height + rect.height));
			double strokewidth = 0.1 * charwidth;
			if(charwidth == 0){
				double dx = Math.abs(lastr.getX() - rect.getX());
				double dy = Math.abs(lastr.getY() - rect.getY());
				if(dy == 0.0) pcn = 0.0;
				else pcn = 1.0 - (2.0 * Math.atan(dx/dy)/Math.PI);
			}
			else{
				double left = lastr.getX() - strokewidth - rect.getX() - rect.width;
				double right = rect.getX() - strokewidth - lastr.getX() -lastr.width;
				double top = lastr.getY() - strokewidth - rect.getY() - rect.height;
				double bot = rect.getY() - strokewidth - lastr.getY() - lastr.height;
				double hdist = 0.0;
				double vdist = 0.0;
				boolean flag = true;
				if(top > 0.0){
					flag = false;
					hdist = (left - (5.0 * charwidth))/charwidth;
					hdist = Math.max(hdist, (right - 3.0 * charwidth)/charwidth);
					vdist = top/charwidth;
				}
				if(bot > 0.0){
					flag = false;
					hdist = ((Math.max(left, right) - charwidth)/charwidth);
					vdist = bot/charwidth;
				}
				if(flag && (left > 0.0)){
					flag = false;
					vdist = 0.0;
					hdist = (left - charwidth)/charwidth;
				}
				if(!flag){
					if(vdist > 0.0) vdist = Math.pow(1.0 + vdist, 0.2) - 1.0;
					psp = sshape(2.0 * (hdist + vdist));
					pcn = 1.0 - psp;
				}
				else{
					hdist = right/charwidth;
					if(right <= 0.0) hdist = hdist - 0.5;
					vdist = (right - (0.75) * charwidth)/charwidth;
					psp = sshape(3.0 * vdist);
					pcn = 1.0 - sshape(2.0 * hdist);
				}
				if((left > 0.0) && (top <= 0.0) && (bot <= 0.0)) psp = 0.0;
			}
		}

		boolean addStroke = true;
		prevchar = lastchar;
		if(psp > 0.6) {
			prevchar = " ";
			initWords();
		}
		getscore(nstroke, 1);
		double score1 = bscore;
		int which1 = which;

		double score2 = 100.0;
		int which2 = -1;
		if(pcn > 0.6){
			int curstrokes = lasts.size();
			lasts.addAll(lines);
			addStroke = false;
			nstroke = normalize(lasts);
			prevchar = lastchar2;
			int owhiches[] = new int[wnum];
			double owscore[] = new double[wnum];
			for(int kl = 0; kl < wnum; kl++){
				owhiches[kl] = whiches[kl];
				owscore[kl] = wscore[kl];
			}
			getscore(nstroke, lastns + 1);
			score2 = bscore;
			which2 = which;
			if(!train){
				double d2 = 8.0;
				if(defaultfilepairs) d2 = 4.0;
				if(dscore > d2 * thresh * pcn) {
					addStroke = true;
					rect = rect1;
					for(int kl = 0; kl < wnum; kl++){
						whiches[kl] = owhiches[kl];
						wscore[kl] = owscore[kl];
					}
				}
			}
			if(!addStroke) {
				score1 = score2;
				which1 = which;
				lastns++;
				lastnls.add(new Integer(curstrokes));
				sp = "\n";
			}
		}
		if(addStroke){
			lasts = new LinkedList();
			for(int ii = 0; ii < lines.size(); ii++) lasts.add(lines.get(ii));
			lastns = 1;
			lastnls = new LinkedList();
		}

		if(sp != "\n"){
			double test = getpair(lastchar, " ");
			if(defaultfilepairs) test = Math.max(0.25, test);
			test = (0.75) * test;
			test = Math.pow(test, 0.2) * psp;
			if(test > 0.65) sp = " ";
		}
			
		if(score1 < thresh) addtodict = false;
		if(!train) addtodict = false;
		String sugg = "";
		if(which1 >=0) sugg = sp + (String) dictout.get(which1);
		if(!addtodict) return dosugg(sugg);
		if(sug0 != null) sugg = sug0;
		String ans = JOptionPane.showInputDialog(null, "Add to Dictionary", sugg);
		if(ans == null) return dosugg(sugg);		
		if(ans.equals("")) return dosugg(sugg); 

		dirty = true;
		dictin.add(nstroke);
		Jrenorm rn = new Jrenorm(nstroke);
		dictrin.add(rn.renormalize());
		dictout.add(ans);
		dictn.add(new Integer(lastns));
		dictbr.add(lastnls);
		if(train) return "<recognized character>";
		return dosugg(sp + ans);		
	}

	private void initWords(){
		thisword = "";
		for(int ii = 0; ii < ntwords; ii++) {
			thesewords[ii] = "";
			lastwords[ii] = "";
		}
	}

	static String punct=".,:;?'\")!";
	private int checkpunct(String nword){
		int nn = nword.length();
		for(int ii = 0; ii < punct.length(); ii++){
			int mm = nword.indexOf(punct.substring(ii, ii + 1));
			if(mm >= 0){
				if(mm < nn) nn = mm;
			}
		}
		String test = nword.substring(0, nn);
		if(hashdiction.get(test) != null) return 1;
		return -1;
	}

	static BufferedImage bim = new BufferedImage(20, 20, BufferedImage.TYPE_USHORT_GRAY);

	private Rectangle2D charRect(String s, Font f,FontRenderContext frc){
		char[] tt = new char[1];
		tt[0] = s.charAt(0);
		GlyphVector v = f.createGlyphVector(frc, tt);
		GlyphMetrics gm = v.getGlyphMetrics(0);
		return gm.getBounds2D();
	}	

	private double padget(String nword){
		if(nword.length() < 2) return 1.0;
		if((lastr == null) || (rect == null)) return 1.0;
		Graphics2D g = (Graphics2D) bim.getGraphics();
		FontRenderContext frc = g.getFontRenderContext();
		Font f = g.getFont();
		g.dispose();
		Rectangle2D r0 = charRect(nword.substring(nword.length() - 2, nword.length() - 1), f, frc);
		Rectangle2D r1 = charRect(nword.substring(nword.length() - 1, nword.length()), f, frc);
		double rat1 = (0.1 + r0.getHeight())/(0.1 + r1.getHeight());
		double rat2 = (0.1 + (double) (lastr.height))/(0.1 + (double) (rect.height));
		double ans = 1.0;
		if(rat1 > rat2) ans = rat1/rat2;
		else ans = rat2/rat1;
		//ans = Math.pow(ans, 0.75);
		return ans;		
	}		

	private String dictionfix(String str){
		if(!defaultfilepairs) return str;
		if(str.length() > 1) return str;
		if(thisword.length() == 0){
			thisword = str.toLowerCase();
			for(int ii = 0; ii < ntwords; ii++){
				if(whiches[ii] >= 0){
					thesewords[ii] = (String) dictout.get(whiches[ii]);
					thesescores[ii] = wscore[ii];
				}
				else{
					thesewords[ii] = str.toLowerCase();
					thesescores[ii] = 100.0;
				}
			}
			return str;
		}
		for(int ii = 0; ii < ntwords; ii++){
			lastwords[ii] = thesewords[ii];
			lastscores[ii] = thesescores[ii];
		}
			
		//System.out.println(thisword + "$" + str);
		String nword;
		String st;
		int nn;
		String newwords[] = new String[ntwords * wnum];
		double newscores[] = new double[ntwords * wnum];
		int jk = 0;
		for(int jj = 0; jj < wnum; jj++){
			st = "#";
			if(whiches[jj] >= 0) st = ((String) dictout.get(whiches[jj])).toLowerCase();
			for (int ii = 0; ii < ntwords; ii++){
				nword = thesewords[ii] + st;
				nn = diction.indexOf("$" + nword);
				if(nn < 0) nn = checkpunct(nword);
				if(nn >= 0){
					newwords[jk] = nword;
					newscores[jk] = thesescores[ii] * wscore[jj] * padget(nword);
				}
				else{
					newwords[jk] = thesewords[ii] + "#";
					newscores[jk] = thesescores[ii] * 100.0;
				}
				jk++;
			}
		}
		for(int ii = 0; ii < ntwords; ii++) {
			thesescores[ii] = 100.0 * ntwords;
			thesewords[ii] = thesewords[ii] + "#";
		}
		double dff[] = new double[ntwords];
		for (int ii = 0; ii < ntwords; ii++) dff[ii] = 1.0;
		for(jk = 0; jk < ntwords * wnum; jk++){
			for (int ii = 0; ii < ntwords; ii++){
				if(thesewords[ii].equals(newwords[jk])) break;
				double df = 1.0;
				Double dd = (Double) hashdiction.get(newwords[jk]);
				if(dd !=null) df = df + (Math.pow(hashdiction.size() * dd.doubleValue(), 0.5)/1.0);
				//System.out.println(newwords[jk] + " " + df);
				if((newscores[jk]/df) < (thesescores[ii]/dff[ii])){
					for(int jj = ntwords - 1; jj > ii; jj--){
						thesescores[jj] = thesescores[jj - 1];
						thesewords[jj] = thesewords[jj - 1];
						dff[jj] = dff[jj - 1];
					}
					thesescores[ii] = newscores[jk];
					//System.out.println(thesewords[ii] + " replaced with " + newwords[jk]);
					thesewords[ii] = newwords[jk];
					dff[ii] = df;
					break;
				}
			}
		}
		//for(int ii = 0; ii < ntwords; ii++) System.out.println(thesewords[ii]);
		st = str.toLowerCase();
		nword = thisword + st;
		boolean fooster = true;
		for (int ii = 0; ii < ntwords; ii++){
			if(!thesewords[ii].endsWith("#")){
				if(nword.equals(thesewords[ii])){
					//System.out.println("nochange=" + nword);
					thisword = nword;
					return st;
				}
				nn = thisword.length();
				st = "";
				for(int jj = 0; jj < nn; jj++) st = st + "\n";
				thisword = thesewords[ii];
				st = st + thisword;
				//System.out.println("thisword=" + thisword);
				return st;
			}
			if(!thesewords[ii].endsWith("##")) fooster = false;
		}
		if(fooster){
			initWords();
			return dictionfix(str);
		}			
		thisword = nword;
		//System.out.println("default=" + st);
		return st;
	}

	private String dosugg(String sugg){
		if(sugg == null) return sugg;
		if(sugg.equals("")) return sugg;
		if(sugg.substring(0,1).equals("\n")) {
			lastchar = sugg.substring(1);
			if(thisword.length() > 0) {
				thisword = thisword.substring(0, thisword.length() - 1);
				for(int ii = 0; ii < ntwords; ii++) {
					thesewords[ii] = lastwords[ii];
					thesescores[ii] = lastscores[ii];
				}
			}
			sugg = "\n" + dictionfix(lastchar);
			lastchar = sugg.substring(sugg.length() - 1, sugg.length());
			return sugg;
		}
		if(sugg.substring(0,1).equals(" ")) {
			lastchar2 = " ";
			lastchar = sugg.substring(1);
			initWords();
			sugg = " " + dictionfix(lastchar);
			lastchar = sugg.substring(sugg.length() - 1, sugg.length());
			return sugg;
		}
		lastchar2 = lastchar;
		lastchar = sugg;
		sugg = dictionfix(lastchar);
		lastchar = sugg.substring(sugg.length() - 1, sugg.length());
		return sugg;
	}
		

	private double mse(Point2D.Double p1, Point2D.Double p2){
		return ((p1.getX() - p2.getX())*(p1.getX() - p2.getX())) + ((p1.getY() - p2.getY())*(p1.getY() - p2.getY()));
	}

	private double dot(Point2D.Double p0, Point2D.Double p1, Point2D.Double q0, Point2D.Double q1){
		double px = p1.getX() - p0.getX();
		double py = p1.getY() - p0.getY();
		double qx = q1.getX() - q0.getX();
		double qy = q1.getY() - q0.getY();
		double ans = ((px*px) + (py*py)) * ((qx*qx) + (qy*qy));
		if(ans == 0.0) return 0.0;
		ans = ((px*qx) + (py*qy))/Math.sqrt(ans);
		return ans;
	}

	private double getpair(String s1, String s2){
		s2 = s2.substring(0, 1);
		char c2[] = new char[1];
		s2.getChars(0, 1, c2, 0);
		s1 = s1.substring(s1.length() - 1);
		char c1[] = new char[1];
		s1.getChars(0, 1, c1, 0);
		double pair = pairs[(int) c1[0]][(int) c2[0]];
		return pair * 16.0;
	}

	private double score(LinkedList l1, LinkedList l2, int itest, boolean fullscore){
		//l1 = xn(l1);
		//l2 = xn(l2);
		double s1 = scoreTrace ((LinkedList) dictrin.get(itest), rnstroke, fullscore)/10.0;
		double s2 = scoreComp(l1, l2, fullscore);
		double s3 = scoreEnds(l1, l2, fullscore);
		double s4 = scoreQuad(l1, l2, fullscore);
		double fscore = 100.0;
		if(fullscore) fscore = (0.5 * s1) + (0.3 * s2) + (0.185 * s3) + (0.015 * s4);
		else fscore = (0.5 * s1) + (0.42 * s2) + (0.08 * s3);
		if(((l1.size() == 1) && (l2.size() !=1)) || ((l2.size() == 1) && (l1.size() !=1))) fscore = fscore + 10.0;
		double pair = getpair(prevchar, (String) dictout.get(itest));
		double pw = 0.1;
		if(prevchar.equals(" ")) pw = 0.07;		
		pw = pw * Math.pow(fscore/thresh, 0.3);
		double ff = Math.pow(pair, pw);
		fscore = fscore / ff;
		return fscore;
	}

	private double scoreTrace(LinkedList ll1, LinkedList ll2, boolean fullscore){
		double score = 0.0;
		for(int ii = 0; ii < Math.max(ll1.size(), ll2.size()); ii++){
			if(ii >= Math.min(ll1.size(), ll2.size())){
				//if(fullscore){
					if(ii >= ll1.size()) score = score + mse((Point2D.Double) ll1.getLast(), (Point2D.Double) ll2.get(ii));
					else if(fullscore) score = score + mse((Point2D.Double) ll1.get(ii), (Point2D.Double) ll2.getLast());
				//}
				//else {
					else ii = Math.max(ll1.size(), ll2.size());
				//}
			}
			else score = score + mse((Point2D.Double) ll1.get(ii), (Point2D.Double) ll2.get(ii));
		}
		return score;
	}		
			
	private double scoreComp(LinkedList l1, LinkedList l2, boolean fullscore){
		double[] score1 = new double[l1.size()];
		double[] score2 = new double[l2.size()];
		double score = 0.0;
		for(int ii = 0; ii < l1.size(); ii++) score1[ii] = -1.0;
		for(int jj = 0; jj < l2.size(); jj++) score2[jj] = -1.0;
		int ii = 0;
		for (Iterator i = l1.iterator(); i.hasNext(); ii++){
			Point2D.Double p1 = (Point2D.Double) i.next();
			int jj = 0;
			for (Iterator j = l2.iterator(); j.hasNext(); jj++){
				Point2D.Double p2 = (Point2D.Double) j.next();
				score = mse(p1, p2);
				if((score1[ii] > score) || (score1[ii] < 0)) score1[ii] = score;
				if((score2[jj] > score) || (score2[jj] < 0)) score2[jj] = score;
			}
		}
		score = 0.0;
		if(fullscore) for(ii = 0; ii < l1.size(); ii++) score = score + (score1[ii]/l1.size());
		for(int jj = 0; jj < l2.size(); jj++) score = score + (score2[jj]/l2.size());
		double mscore1 = 0.0;
		for(ii = 0; ii < l1.size(); ii++) mscore1 = Math.max(mscore1, score1[ii]);
		double mscore2 = 0.0;
		for(int jj = 0; jj < l2.size(); jj++) mscore2 = Math.max(mscore2, score2[jj]);
		if(fullscore) score = (0.7 * score) + (0.15 * mscore1) + (0.15 * mscore2);
		else score = (0.7 * score) + (0.3 * mscore2);
		return score;
	}

	private double scoreEnds(LinkedList l1, LinkedList l2, boolean fullscore){		

		double score0 = mse((Point2D.Double) l1.get(0), (Point2D.Double) l2.get(0));
		double scorelast = mse((Point2D.Double) l1.getLast(), (Point2D.Double) l2.getLast());
		
		double cl1 = clength(l1);
		double cl2 = clength(l2);

		Point2D.Double p1 = getmid(l1, cl1);
		Point2D.Double p2 = getmid(l2, cl2);
		double scoremid = mse(p1, p2);

		double score = (0.34 * score0) + (0.33 * scoremid) + (0.33 * scorelast);	

		double scorelength = 0.0;
		if(Math.max(cl1, cl2) != 0.0) scorelength = (cl1 - cl2)/Math.max(cl1, cl2);
		scorelength = 3.0 * scorelength * scorelength;
		
		score = (0.89 * score) + (0.11 * scorelength);
		if(!fullscore) score = score0;
		return score;
	}

	private double scoreQuad(LinkedList l1, LinkedList l2, boolean fullscore){
		double qscore = 0.0;
		if(!fullscore) return qscore;
		int q1[] = quads(l1);
		int q2[] = quads(l2);
		int jj = 0;
		for(int ii = 0; ii < 9; ii++) jj = jj + Math.abs(q1[ii] - q2[ii]);
		qscore = jj * thresh;
		return qscore;
	}

	private int[] quads(LinkedList ll){
		Rectangle2D.Double r = getNRectangle(ll);
		int q[] = new int[9];
		if((r.width == 0.0) || (r.height == 0.)){
			for (int jj = 0; jj < 9; jj++) q[jj]=1;
			return q;
		}
		for (int jj = 0; jj < 9; jj++) q[jj]=0;
		for(int ii = 0; ii < ll.size(); ii++){
			Point2D.Double p = (Point2D.Double) ll.get(ii);
			double x = (p.getX()-r.getX())/r.width;
			double y = (p.getY()-r.getY())/r.height;
			int xx = (int) x;
			int yy = (int) y;
			q[xx + (3 * yy)] = 1;
		}
		return q;
	}

	private double clength(LinkedList ll){
		double cl = 0.0;
		Point2D.Double p0 = (Point2D.Double) ll.get(0);
		Point2D.Double p1 = null;
		for(int ii = 1; ii < ll.size(); ii++){
			p1 = (Point2D.Double) ll.get(ii);
			cl = cl + Math.sqrt(mse(p0, p1));
			p0 = p1;
		}
		return cl;
	}
	
	private Point2D.Double getmid(LinkedList ll, double cl){
		double len = 0.0;
		Point2D.Double lastp = (Point2D.Double) ll.get(0);
		Point2D.Double curp  = lastp;
		int ii = 1;
		while((len < (cl/2.0)) && (ii < ll.size())){
			curp = (Point2D.Double) ll.get(ii);	
			len = len + Math.sqrt(mse(curp, lastp));
			lastp = curp;
			ii++;
		}	
		return curp;
	}
			
	private Point2D.Double normalizeP(double x, double y, double w, double h, Rectangle2D.Double r){
		return new Point2D.Double((x - r.getX())/w, (y - r.getY())/h);
	}				

	private LinkedList normalize(LinkedList lines){
		Rectangle2D.Double r = getRectangle(lines);
		rect = r;
		LinkedList points = new LinkedList();
		double w = r.width;
		double h = r.height;
		double f = Math.max(w, h);
		//double z = 0.2;
		double z = 0.4;
		if(f == 0){
			points.add(new Point2D.Double(0.0, 0.0));
			return points;			
		}
		//w = w + (z * f);
		//h = h + (z * f);
		w = ((1-z) * w) + (z * f);
		h = ((1-z) * h) + (z * f);
		Line2D.Double dline = (Line2D.Double) lines.get(0);
		points.add(normalizeP(dline.getX1(), dline.getY1(), w, h, r));
		for (Iterator i = lines.iterator(); i.hasNext(); ){
			dline = (Line2D.Double) i.next();
 			points.add(normalizeP(dline.getX2(), dline.getY2(), w, h, r));
		}
		return points;
	}

	public LinkedList xn(LinkedList lines){
		Rectangle2D.Double r = getNRectangle(lines);
		LinkedList points = new LinkedList();
		double w = r.width;
		double h = r.height;
		double z = 0.2;
		if(w >= h){
			w = w/(1+z);
			h = h - (z * w);
		}
		else{
			h = h/(1+z);
			w = w - (z * h);
		}
		if(w <= 0) w = 0;
		if(h <= 0) h = 0;
		double f = Math.max(w, h);
		z = 0.4;
		if(f == 0){
			return lines;			
		}
		w = ((1-z) * w) + (z * f);
		h = ((1-z) * h) + (z * f);
		for (Iterator i = lines.iterator(); i.hasNext(); ){
			Point2D.Double p0 = (Point2D.Double) i.next();
 			points.add(new Point2D.Double(p0.getX() * r.width/w, p0.getY()* r.height/h));
		}
		return points;
	}

	private Rectangle2D.Double getRectangle(LinkedList lines){
		double xmin = 0.0;
		double xmax = 0.0;
		double ymin = 0.0;
		double ymax = 0.0;
		boolean flag = true;
		for (Iterator i = lines.iterator(); i.hasNext(); ){
			Line2D.Double dline = (Line2D.Double) i.next();
			if(flag){
				flag = false;
				xmin = dline.getX1();
				xmax = xmin;
				ymin = dline.getY1();
				ymax = ymin;
			}
			double x = dline.getX2();
			double y = dline.getY2();
			if(x < xmin) xmin = x;
			if(x > xmax) xmax = x;
			if(y < ymin) ymin = y;
			if(y > ymax) ymax = y;
		}
		double w = xmax - xmin;
		double h = ymax - ymin;
		Rectangle2D.Double r = new Rectangle2D.Double(xmin, ymin, w, h);
		return r;
	}
	private Rectangle2D.Double getNRectangle(LinkedList lines){
		double xmin = 0.0;
		double xmax = 0.0;
		double ymin = 0.0;
		double ymax = 0.0;
		boolean flag = true;
		for (Iterator i = lines.iterator(); i.hasNext(); ){
			Point2D.Double p = (Point2D.Double) i.next();
			if(flag){
				flag = false;
				xmin = p.getX();
				xmax = xmin;
				ymin = p.getY();
				ymax = ymin;
			}
			double x = p.getX();
			double y = p.getY();
			if(x < xmin) xmin = x;
			if(x > xmax) xmax = x;
			if(y < ymin) ymin = y;
			if(y > ymax) ymax = y;
		}
		double w = xmax - xmin;
		double h = ymax - ymin;
		Rectangle2D.Double r = new Rectangle2D.Double(xmin, ymin, w, h);
		return r;
	}
}

class Jrenorm {
	
private Point2D.Double p2d;
	private int n2d;
	private double alpha;
	private LinkedList ll;

	public Jrenorm(LinkedList ll){
		this.ll = ll;
	}

	private double arclen(Point2D.Double p0, Point2D.Double p1){
		double dx = p1.getX() - p0.getX();
		double dy = p1.getY() - p0.getY();
		return Math.sqrt((dx*dx) + (dy*dy));
	}

	private boolean getnext(LinkedList ll, double delta){
		if(n2d + 1 >= ll.size()) return false;
		Point2D.Double p0 = (Point2D.Double) ll.get(n2d);
		Point2D.Double p1 = (Point2D.Double) ll.get(n2d + 1);
		double ddd = arclen(p0, p1);
		double dd = (1.0 - alpha) * ddd;
		if(dd < delta){
			alpha = 0;
			n2d++;
			return getnext(ll, delta - dd);
		}
		alpha = alpha + (delta/ddd);
		p2d = new Point2D.Double(p0.getX() + (alpha * (p1.getX() - p0.getX())), p0.getY() + (alpha * (p1.getY() - p0.getY())));
		return true;
	}

	public LinkedList renormalize(){
		double delta = 0.3;
		LinkedList ln = new LinkedList();
		ln.add(ll.get(0));
		n2d = 0;
		alpha = 0.0;
		while (getnext(ll, delta)) ln.add(p2d);
		return ln;
	}
}
