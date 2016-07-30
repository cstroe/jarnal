package jarnal;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import java.util.zip.*;
import java.lang.Math.*;
import java.lang.Number.*;
import java.text.*;
import java.awt.datatransfer.*;
import java.io.*;

import jarnal.Jarnal;

public class Jarnbox {	

	public static float gr = 1.0f;
	public JDialog jw;
	public JFrame f;
	public Jarnal jarn = null;
	public Jpages jp = null;
	public Jarnal.JrnlPane jpn = null;
	public String title;
	public boolean done = false;
	public boolean docancel = false;
	public SpinnerNumberModel model = null;
	public int buttonInt = 0;
	public javax.swing.Timer timer = null;
	public JProgressBar pbar = null;
	public JLabel msg = new JLabel();
	public JTextField text1 = new JTextField();
	public JComboBox combo1 = new JComboBox();
	public JList list1 = null;
	public JCheckBox cb0;
	public JCheckBox cb1;
	public JCheckBox cb2;
	public JCheckBox cb3;
	public JCheckBox cb4;
	public boolean cbstate[] = {false, false, false, false, false};	

	public Jarnbox(JFrame jf, String title){
		f = jf;
		jw = new JDialog(jf, title, true);
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.title = title;
	}

	public Jarnbox(JFrame jf, String title, Jarnal jarn, boolean modal){
		f = jf;
		jw = new JDialog(jf, title, modal);
		this.jarn = jarn;
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.title = title;
	}

	private void setCenter(Component c){
		setCenter(f, c);
	}

	public static Dimension newDimension(int x, int y){
		return new Dimension((int)(gr*x), (int)(gr*y));
	}
		

	public static void setCenter(JFrame f, Component c){
		if(f == null) return;
		int w = f.getWidth();
		int h = f.getHeight();
		int x = f.getX();
		int y = f.getY();
		int ww = c.getWidth();
		int hh = c.getHeight();
		c.setLocation(x + ((w - ww) /2), y + ((h - hh) /2));
	}

	private JButton bb(String action){
		JButton item;
		item = new JButton(action);
		item.addActionListener(new dialogListener(action, this));
		return item;
	}

    	private JCheckBox bc(String label, String action){
        	JCheckBox item;
        	item = new JCheckBox(label);
        	item.addActionListener(new dialogListener(action, this));
        	return item;
    	}

	public void showCancel(){
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		cp.add(msg, c); 
		c.gridy = 1;
		cp.add(new JLabel(" "), c);
		c.gridy = 2;
		cp.add(bb("Cancel"), c);
            	jw.setSize(newDimension(200, 100));
		setCenter(jw);
		jw.addWindowListener(new dialogClosing(new dialogListener("Cancel", this)));
            	jw.setVisible(true);		 
	}

	public int showSaveExitCancel(){
		jw.getContentPane().setLayout(new FlowLayout());
		jw.getContentPane().add(new JLabel("Unsaved data will be lost. OK to close?"));
		JPanel bot = new JPanel();
		bot.add(bb("Save"));
		bot.add(bb("Cancel"));
		bot.add(bb("Close Without Saving"));
		jw.getContentPane().add(bot);
		jw.setSize(newDimension(450, 120));
		setCenter(jw);
		jw.addWindowListener(new dialogClosing(new dialogListener("Cancel", this)));
		jw.setVisible(true);
		while(!done){}
		System.out.println(buttonInt);
		return buttonInt;
	}

	public void showUnRe(){
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		String sels[] = {"Undo", "Redo"};
		combo1 = new JComboBox(sels);
	    	combo1.setSelectedIndex(0);
		combo1.addActionListener(new dialogListener("UnRe", this));
		cp.add(combo1, c);
		c.gridy = 1;
		list1 = new JList(); 
	    	//list1.addListSelectionListener(new list1SelectionListener());
	    	list1.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	    	list1.setVisibleRowCount(-1);
		list1.setListData(jp.getUndo(true));
	    	JScrollPane listScroller = new JScrollPane(list1);
	    	listScroller.setPreferredSize(newDimension(280, 200));
	    	cp.add(listScroller, c);
		c.gridwidth = 1;
		c.gridy = 2;
		cp.add(bb("Cancel"), c);
		c.gridx = 1;		
		cp.add(bb("Undo/Redo"), c);
            	jw.setSize(newDimension(300, 300));
		setCenter(jw);
		jw.addWindowListener(new dialogClosing(new dialogListener("Cancel", this)));
            	jw.setVisible(true);
	}

	public void showFind(){
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 4;
		combo1 = new JComboBox(jp.getFind());
		combo1.setEditable(true);
		text1 = (JTextField) combo1.getEditor().getEditorComponent();
		text1.setColumns(20);
		text1.setText(jp.findTarget());
		cp.add(combo1, c);
		c.anchor = GridBagConstraints.WEST;
		c.gridwidth = 1;
		c.gridx = 4;
		cp.add(bb("Paste"), c);
		c.gridy = 1;
		c.gridx = 2;
		cb3 = bc("Match Case", "cb3");
		cp.add(cb3, c);
		c.gridx = 3;
		cb4 = bc("Entire Word", "cb4");
		cp.add(cb4, c);
		c.gridy = 2;
		c.gridx = 1;
		cb0 = bc("First", "cb0");
		cp.add(cb0, c);
		c.gridx = 2;
		cb1 = bc("Entire Document", "cb1");
		cp.add(cb1, c);
		c.gridx = 3;
		cb2 = bc("Background", "cb2");
		if(!jarn.isApplet) cp.add(cb2, c);
		c.gridy = 3;
		c.gridx = 2;
		cp.add(bb("Cancel"), c);
		c.gridx = 3;
		cp.add(bb("Reverse"), c);
		c.gridx = 4;
		cp.add(bb("Find"), c);		
            	jw.setSize(newDimension(400, 150));
		setCenter(jw);
		jw.addWindowListener(new dialogClosing(new dialogListener("Cancel", this)));
            	jw.setVisible(true);
	}

	public void showLink(String link){
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		cp.add(bb("Rel"), c);
		c.gridx = 1;
		c.gridwidth = 4;
		text1.setColumns(20);
		text1.setText(link);
		cp.add(text1, c);
		c.gridwidth = 1;
		c.gridx = 5;
		cp.add(bb("Paste"), c);
		c.gridy = 1;
		c.gridx = 0;
		cp.add(bb("File"), c);
		c.gridx = 3;
		cp.add(bb("Cancel"), c);
		c.gridx = 4;
		cp.add(bb("Internal Link"), c);
		c.gridx = 5;
		cp.add(bb("Insert Link"), c);		
            	jw.setSize(newDimension(400, 100));
		setCenter(jw);
		jw.addWindowListener(new dialogClosing(new dialogListener("Cancel", this)));
            	jw.setVisible(true);
	}		

	public void showReplay(){
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		if(jp.active) jp.setMark("tempundoall");
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 5;
		pbar = new JProgressBar(0, 100);
		pbar.setValue(jp.undoRatio());
		cp.add(pbar, c);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		cp.add(bb("<<"), c);
		c.gridx = 1;
		cp.add(bb("<"), c);
		c.gridx = 2;
		cp.add(bb("||"), c);
		c.gridx = 3;
		cp.add(bb(">"), c);
		c.gridx = 4;
		cp.add(bb(">>"), c);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		cp.add(msg, c);
		c.gridwidth = 1;
		model = new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1);
		JSpinner spinner = new JSpinner(model);
		c.gridx = 2;
		cp.add(spinner, c);
		c.gridx = 4;
		cp.add(bb("Close"), c);
            	jw.setSize(newDimension(350, 100));
		setCenter(jw);
		jw.addWindowListener(new dialogClosing(new dialogListener("Close", this)));
            	jw.setVisible(true);
	}

	public int getTime(){
		return (int)(1000/model.getNumber().floatValue());
	}

	public void startPassiveTimer(){
		if(timer != null) timer.stop();
		timer = new javax.swing.Timer(getTime(), new timerListener(jp, jarn, this, "time"));
		timer.start();
	}

	public Number getPosDecNumber(double init, double max){
	    return getPosDecNumber(init, max, false);
	}

	public Number getPosDecNumber(double init, double max, boolean redrawButton){
	    jw.getContentPane().setLayout(new FlowLayout());
	    model = new SpinnerNumberModel(init, 0, max, 0.1);
	    JSpinner spinner = new JSpinner(model);
	    jw.getContentPane().add(spinner);
	    JPanel bot = new JPanel();
	    if(redrawButton) bot.add(bb("Preview"));
	    bot.add(bb("Cancel"));
	    bot.add(bb("OK"));
	    jw.getContentPane().add(bot);
	    int width = 200;
	    if(redrawButton) width = 250;
            jw.setSize(newDimension(width, 100));
	    setCenter(jw);
	    jw.addWindowListener(new dialogClosing(new dialogListener("Cancel", this)));
            jw.setVisible(true);
	    if(!redrawButton){
	    	while(!done){}
	    	if (docancel) return null;
	    	return model.getNumber();
	    }
	    return null;
	}

	public Number getInt(int init, int max){
		return getInt(init, max, false);
	}

	public Number getInt(int init, int max, boolean redrawButton){
		return getInt(init, 0, max, redrawButton);
	}

	public Number getInt(int init, int min, int max, boolean redrawButton){
		return getInt(init, min, max, redrawButton, false);
	}

	public void flipSaveBg(boolean flipon){
		jarn.saveBg = flipon;
		jarn.sbg.setState(flipon);
		jarn.sbg2.setState(flipon);
		jarn.jrnlPane.jpages.saveBg = flipon;
		if(!flipon){
			jarn.jrnlPane.jpages.portableBgs = false;
			jarn.pbgs.setState(jarn.jrnlPane.jpages.portableBgs);
		}
		cb0.setSelected(flipon);
	}

	public Number getInt(int init, int min, int max, boolean redrawButton, boolean saveBg){
	    jw.getContentPane().setLayout(new FlowLayout());
	    model = new SpinnerNumberModel(init, min, max, 1);
	    JSpinner spinner = new JSpinner(model);
	    jw.getContentPane().add(spinner);
	    JPanel bot = new JPanel();
	    if(redrawButton) bot.add(bb("Preview"));
	    if(saveBg) {
		cb0 = bc("Send Background", "saveBg");
		cb0.setSelected(true);
		flipSaveBg(true);
		jw.getContentPane().add(cb0);
		spinner.setEditor(new JSpinner.NumberEditor(spinner, "#####"));
	    }
	    bot.add(bb("Cancel"));
	    bot.add(bb("OK"));
	    jw.getContentPane().add(bot);	    
	    int width = 200;
	    if(redrawButton) width = 250;
	    int height = 100;
	    if(saveBg) height = 150;
            jw.setSize(newDimension(width, height));
	    setCenter(jw);
	    jw.addWindowListener(new dialogClosing(new dialogListener("Cancel", this)));
            jw.setVisible(true);
	    if(!redrawButton){
	    	while(!done){}
	   	 if (docancel) return null;
	    	return model.getNumber();
	    }
	    return null;
	}

	public String getString(String init){
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 4;
		text1.setColumns(20);
		text1.setText(init);
		cp.add(text1, c);
		c.anchor = GridBagConstraints.WEST;
		c.gridwidth = 1;
		c.gridx = 4;
		cp.add(bb("Paste"), c);
		c.gridx = 5;
		cp.add(bb("Cancel"),c);
		c.gridy = 1;
		c.gridx = 2;
		c.anchor = GridBagConstraints.CENTER;
		//cp.add(bb("Cancel"), c);
		c.gridx = 4;
		c.anchor = GridBagConstraints.WEST;
		cp.add(bb("Delete"), c);
		c.gridx = 5;
		c.anchor = GridBagConstraints.WEST;
		cp.add(bb("OK"), c);
		c.gridy = 2;
		c.gridx = 0;
		c.gridwidth = 6;
		if(jarn.pencentric) {
			JPanel jpan = new JPanel(new BorderLayout());
			Jarnal min = jarn.miniJarnal(false, null, jpan, jarn.toolkit);
			cp.add(jpan, c);
			jw.setSize(newDimension(600, 330));
		}					
            	else jw.setSize(newDimension(400, 100));
		setCenter(jw);
		jw.addWindowListener(new dialogClosing(new dialogListener("Cancel", this)));
            	jw.setVisible(true);
	    	while(!done){}
	    	if (docancel) return null;
	    	return text1.getText();
	}


	public JDialog getDialog(){
		return jw;
	}
}

class dialogListener implements ActionListener {
	private String action;
	private Jarnbox jb;

	public dialogListener(String action, Jarnbox jb){
		this.jb = jb;
		this.action = action;
	}

	public void actionPerformed(ActionEvent e){
		if(action.equals("saveBg")){
			jb.flipSaveBg(!jb.jarn.saveBg);
		}
		if(action.equals("UnRe")){
        		JComboBox cb = (JComboBox)e.getSource();
        		String selstr = (String)cb.getSelectedItem();
			if(selstr.equals("Undo")) jb.list1.setListData(jb.jp.getUndo(true));
			else jb.list1.setListData(jb.jp.getUndo(false));
		}
		if(action.equals("Undo/Redo")){
			String selstr = (String) jb.combo1.getSelectedItem();
			int n = jb.list1.getSelectedIndex();
			boolean isUndo = true;
			if(selstr.equals("Redo")) isUndo = false;
			int m = jb.jp.getUndo(isUndo).length;
			int mm = m;
			while((m - mm) <= n){
				if(isUndo) jb.jp.undo();
				else jb.jp.redo();
				mm = jb.jp.getUndo(isUndo).length;
			}
			if(selstr.equals("Undo")) jb.list1.setListData(jb.jp.getUndo(true));
			else jb.list1.setListData(jb.jp.getUndo(false));
			jb.jarn.jrnlPane.doAction("Redraw Page");
		}
		if(action.startsWith("cb")){
			String test = action.substring(2);
			int n = Integer.parseInt(test);
			jb.cbstate[n] = !jb.cbstate[n];
			return;
		}
		if(action.equals("Find") || action.equals("Reverse")){
			jb.jarn.jtm.setClockCursor(jb.jw);
			boolean reverse = false;
			if(action.equals("Reverse")) reverse = true;
			int oldPage = jb.jp.getPage();
			if(jb.cbstate[2]){
				jb.cbstate[1] = true;
				jb.cb1.setSelected(true);
			}
			jb.jp.find(jb.text1.getText(), jb.cbstate[0], reverse, jb.cbstate[1], jb.jarn.jt, jb.cbstate[2], jb.cbstate[3], jb.cbstate[4]);
			if(jb.cbstate[1]){
				jb.cbstate[0] = false;
				jb.cb0.setSelected(false);
			}
			if(jb.cbstate[1] && (jb.jp.getPage() != oldPage)){
				jb.jarn.jrnlPane.setTSize();
				jb.jarn.jrnlPane.setup();
			}
			jb.combo1.setModel(new DefaultComboBoxModel(jb.jp.getFind()));
			jb.jarn.jrnlPane.doAction("Redraw Page");
		}
		if(action.equals("Delete")){
			jb.text1.replaceSelection("");
		}
		if(action.equals("Paste")){
			try{
				String data = null;
				Transferable contents;	
				if(!jb.jarn.isApplet){
					Clipboard clip = jb.jarn.toolkit.getSystemClipboard();
					contents=clip.getContents(jb.f);
				}
				else contents = jb.jarn.internalClipboard;
				if(contents.isDataFlavorSupported(DataFlavor.stringFlavor)) data = (String) contents.getTransferData(DataFlavor.stringFlavor);
				if(contents.isDataFlavorSupported(new DataFlavor("jaj/pair; class=java.lang.String", "Jarnal Clipboard Data"))){
					String temp[] = (String[]) contents.getTransferData(new DataFlavor("jaj/pair; class=java.lang.String", "Jarnal Clipboard Data"));
					if(temp[1].equals("page")){
						data = (String) contents.getTransferData(new DataFlavor("text/html", "HTML (HyperText Markup Language)"));
						String stemp = jb.jarn.getFileName();
						if((stemp != null) && data.startsWith(stemp)){
							data = data.substring(jb.jarn.getFileName().length() + 1);
						}
					}
				}
				if(data != null) jb.text1.replaceSelection(data);
			}
			catch(Exception ex){ ex.printStackTrace();} 
		}
		if(action.equals("Internal Link")){
			String target = jb.text1.getText();
			target = Jarnal.getAbsoluteName(jb.jarn.cwd, target);
			String url[] = Jarnal.parseURL(target);
			String name = url[1];
			String fname = name;
			int n = name.lastIndexOf("/");
			if((n > -1) && (n < name.length() - 1)) name = name.substring(n + 1);
			jb.jp.addExtra(name, fname);
			jb.jp.setlink(name + url[2]);			
			jb.jarn.dirty = true;
			jb.getDialog().setVisible(false);
			jb.done = true;
		}
		if(action.equals("Insert Link")){
			jb.jp.setlink(jb.text1.getText());
			jb.jarn.dirty = true;
			jb.getDialog().setVisible(false);
			jb.done = true;
		}
		if(action.equals("Rel")){
			String test = jb.text1.getText();
			if(test.startsWith("..")){
				String url[] = Jarnal.parseURL(test);
				String path = Jarnal.getAbsoluteName(jb.jarn.cwd, url[1]);
				jb.text1.setText("file://" + path + url[2]);
			}
			if(test.startsWith("file://")){
				String url[] = Jarnal.parseURL(test);
				String path = "../" + Jbgs.relativePath(jb.jarn.cwd, url[1]);
				jb.text1.setText(path + url[2]);
			}				
		}
		if(action.equals("File")){
			String oldcwd = jb.jarn.cwd;
			String temp = jb.jarn.jrnlPane.getFile("Link File", false);
			String lfile = "";
			if(temp != null){
				lfile = jb.jarn.cwd + File.separator + temp;
				String url[] = Jarnal.parseURL(lfile);
				String path = "../" + Jbgs.relativePath(jb.jarn.cwd, url[1]);
				jb.text1.setText(path + url[2]);
			}
			jb.jarn.cwd = oldcwd;			
		}			
		if(action.equals("Close")){
			if(jb.timer != null) jb.timer.stop();
			jb.timer = null;
			jb.getDialog().setVisible(false);
			if(jb.jp.active) {
				jb.jarn.locked = false;
				jb.jarn.dragOp = 0;
			}
			jb.jarn.replayActive = false;
			jb.jp = null;
		}			
		if(action.equals("Cancel")) {
			jb.getDialog().setVisible(false);
			jb.docancel = true;
			jb.done = true;
			if(jb.jarn != null) jb.jarn.jrnlPane.cancelPrint = true;
	        }
		if(action.equals("Save")){
			jb.getDialog().setVisible(false);
			jb.buttonInt = 1;
			jb.done = true;
		}
		if(action.equals("Close Without Saving")){
			jb.getDialog().setVisible(false);
			jb.buttonInt = 2;
			jb.done = true;
		}
		if(action.equals("OK")){
			jb.getDialog().setVisible(false);
			jb.done = true;
			action = "Preview";
		}
		if(action.equals("Preview")){
			if(jb.title.equals("Zoom")) action = "Preview";
			else if(jb.title.startsWith("Shift")) action = jb.title;
			else if(jb.title.equals("GoToPage")) action = "GoToPage";
			else action = "";
		}
		if(action.equals("GoToPage")){
			if(jb.jarn != null){
				jb.jarn.jtm.setClockCursor(jb.jw);
				jb.jarn.gotopage = jb.model.getNumber();
				jb.jarn.jrnlPane.doAction("GoToPage");
			}
		}
		if(action.equals("Shift Right"))jb.jarn.jrnlPane.foffX = jb.model.getNumber().intValue();
		if(action.equals("Shift Down"))jb.jarn.jrnlPane.foffY = jb.model.getNumber().intValue();
		if(action.equals("Preview")){
			if(jb.jarn != null) {
				jb.jarn.jtm.setClockCursor(jb.jw);
				jb.jarn.previewZoom = jb.model.getNumber();
				jb.jarn.jrnlPane.doAction("Preview Zoom");
			}
		}
		if(jb.jp == null) return;
		if(jb.pbar != null){
			if(!jb.jp.active){
				jb.startPassiveTimer();
				return;
			}
		}			
		if(action.equals("<<")){
			jb.jp.untilMark("undo", "markthisdoesnotexist");
			jb.jpn.getdo(true);
			jb.pbar.setValue(jb.jp.undoRatio());	
		}
		if(action.equals(">>")){
			jb.jp.untilMark("redo", "marktempundoall");
			jb.jpn.getdo(true);
			jb.pbar.setValue(jb.jp.undoRatio());
		}
		if(action.equals(">")){
			if(jb.timer != null) jb.timer.stop();
			jb.timer = new javax.swing.Timer(jb.getTime(), new timerListener(jb.jp, jb.jarn, jb, "forward"));
			jb.timer.start();
		}
		if(action.equals("<")){
			if(jb.timer != null) jb.timer.stop();
			jb.timer = new javax.swing.Timer(jb.getTime(), new timerListener(jb.jp, jb.jarn, jb, "backward"));
			jb.timer.start();
		}
		if(action.equals("||")){
			if(jb.timer != null) jb.timer.stop();
			jb.timer = null;
		}
	}
}

class dialogClosing extends WindowAdapter {

	ActionListener al = null;
	String action = null;

	public dialogClosing(ActionListener al){
		//System.out.println("creating dialog closing");
		this.al = al;
	}

	public dialogClosing(ActionListener al, String action){
		this.al = al;
		this.action = action;
	}
		

	public void windowClosing(WindowEvent e){
		System.out.println("dialog closing with action " + action);
		if(action == null) al.actionPerformed(new ActionEvent(al, 0, ""));
		else al.actionPerformed(new ActionEvent(al, 0, action));
	}
}

class timerListener implements ActionListener{

	Jpages jpa;
	Jarnal jarn;
	Jarnal.JrnlPane jpn;
	String action;
	Jarnbox jb;

	public timerListener(Jpages jpa, Jarnal jarn, Jarnbox jb, String action){
		this.jpa = jpa;
		this.jarn = jarn;
		jpn = jarn.jrnlPane;
		this.jb = jb;
		this.action = action;
	}

	private void setTime(){
		if(jpa.utime != 0){
			String dtstr = DateFormat.getDateTimeInstance().format(new Date(jpa.utime));
			jb.msg.setText(dtstr);
		}
	}

	public void actionPerformed(ActionEvent e) {

		if(action.equals("time")){
			setTime();
			jb.pbar.setValue(jpa.undoRatio());
			return;
		}

		if(!jb.jp.active){
			jb.startPassiveTimer();
			return;
		}

		if(action.equals("forward")){
			jpa.redo();
			setTime();
			jpn.getdo(true);
		}
		if(action.equals("backward")){
			jpa.undo();
			setTime();
			jpn.getdo(true);
		}
		jb.pbar.setValue(jpa.undoRatio());
	}
}

class paperDialogListener implements ActionListener {

	JDialog jw;
	JFrame jf;
	JComboBox combo1;
	JComboBox combo2;
	JComboBox combo3;
	JComboBox combo4;
	JComboBox combo5;
	JComboBox combo6;
	SpinnerNumberModel model1;
	SpinnerNumberModel model2;
	SpinnerNumberModel model3;
	SpinnerNumberModel model4;
	SpinnerNumberModel model5;
	JCheckBox jcb1;
	JCheckBox jcb2;
	JCheckBox jcb3;
	JCheckBox jcb4;
	JCheckBox jcb5;
	Jarnal jarn;
	Jpages jp;
	Jarnal.JrnlPane jpn;

	private void setRuling(){
		jp = jpn.jpages;
		int transparency = jp.getPaper().transparency;
		combo5.setSelectedIndex(3);
		if(transparency == 100) combo5.setSelectedIndex(1);
		else if(transparency == 60) combo5.setSelectedIndex(2);
		else if(transparency == 255) combo5.setSelectedIndex(0);
		model2.setValue(new Double(transparency));
		model3.setValue(new Double(jp.bgFade()));
		model4.setValue(new Double(jp.bgindex()));
		model5.setValue(new Double(jp.bgScale()));
		String paper = jp.getPaper().paper;
		if(paper.equals("Lined")) combo2.setSelectedIndex(0);
		if(paper.equals("Plain")) combo2.setSelectedIndex(1);
		if(paper.equals("Graph")) combo2.setSelectedIndex(2);
		if(paper.equals("Ruled")) combo2.setSelectedIndex(3);
		int nlines = jp.getPaper().nlines;
		combo3.setSelectedIndex(3);
		if(nlines == 15) combo3.setSelectedIndex(0);
		if(nlines == 25) combo3.setSelectedIndex(1);
		if(nlines == 35) combo3.setSelectedIndex(2);
		model1.setValue(new Double(nlines));
		if(jp.getPaper().showBg == 1) jcb1.setSelected(true);
		else jcb1.setSelected(false);
		if(jp.getRepeating()) jcb2.setSelected(true);
		else jcb2.setSelected(false);
		if(jp.portableBgs) jcb3.setSelected(true);
		else jcb3.setSelected(false);
		if(jarn.saveBg) jcb4.setSelected(true);
		else jcb4.setSelected(false);
		if(jarn.absoluteScale) jcb5.setSelected(true);
		else jcb5.setSelected(false);
	}

	public void showDialog(JFrame jf, Jarnal jarn){
		this.jarn = jarn;
		this.jf = jf;
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		jw = new JDialog(jf, "Paper and Background", false);
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		JButton item = new JButton("Undo");
		item.addActionListener(this);
		cp.add(item, c); 
		c.gridwidth = 5;
		item = new JButton("Update");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridwidth = 1;
		c.gridx = 4;
		item = new JButton("Redo");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridx = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 1;
		c.anchor = GridBagConstraints.SOUTHWEST;
		JLabel label = new JLabel("Paper");
		cp.add(label, c);
		c.gridy++;
		c.anchor = GridBagConstraints.NORTHWEST;
		String sels1[] = {"white", "yellow", "pink", "orange", "blue", "green", "color"};
		JLabel jmi[] = new JLabel[7];
		for(int ii = 0; ii < 6; ii++) jmi[ii] = new JLabel(sels1[ii], new colorIcon(sels1[ii]), JLabel.CENTER);
		jmi[6] = new JLabel(sels1[6], new colorIcon(sels1[0]), JLabel.CENTER);
		combo1 = new JComboBox(jmi);
		combo1.setRenderer(new labelCellRenderer());
		combo1.setSelectedIndex(6);
		combo1.addActionListener(this);
		cp.add(combo1, c);
		c.gridy++;
		String sels2[] = {"Lined", "Plain", "Graph", "Ruled"};
		combo2 = new JComboBox(sels2);
		combo2.addActionListener(this);
	    	cp.add(combo2, c);
		c.gridy++;		
		String sels3[] = {"Thick Lines", "Medium Lines", "Thin Lines", "Other Lines"};	
		combo3 = new JComboBox(sels3); 
		combo3.addActionListener(this);
	    	cp.add(combo3, c);
		c.gridy++;
		c.gridwidth = 1;
		model1 = new SpinnerNumberModel(1, 0, 1000.0, 1.0);
		JSpinner spinner = new JSpinner(model1);
		cp.add(spinner, c);
		c.gridx = 1;
		item = new JButton("Set Lines");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		String sels4[] = {"Normal Size", "Index Card", "Size"};	
		combo4 = new JComboBox(sels4); 
		combo4.setSelectedIndex(2);
		combo4.addActionListener(this);
	    	cp.add(combo4, c);
		c.gridy++;
		item = new JButton("Fit to Background");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;
		item = new JButton("Toggle Landscape");
		item.addActionListener(this);
		cp.add(item, c);		
		c.gridy++;
		c.anchor = GridBagConstraints.SOUTHWEST;
		label = new JLabel("Bottom Highlighter");
		cp.add(label, c);
		c.gridy++;
		c.anchor = GridBagConstraints.NORTHWEST;
		String sels5[] = {"opaque", "translucent", "transparent", "other"};	
		combo5 = new JComboBox(sels5); 
		combo5.addActionListener(this);
	    	cp.add(combo5, c);
		c.gridy++;
		c.gridwidth = 1;
		model2 = new SpinnerNumberModel(255.0, 0, 255.0, 1.0);
		spinner = new JSpinner(model2);
		cp.add(spinner, c);
		c.gridx = 1;
		item = new JButton("Transparency");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		item = new JButton("Copy Paper");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		jcb5 = new JCheckBox("Absolute Scale");
		jcb5.addActionListener(this);
		cp.add(jcb5, c);			
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.gridy = 1;
		c.gridx = 3;
		label = new JLabel("Background");
		cp.add(label, c);
		c.gridy++;
		c.anchor = GridBagConstraints.NORTHWEST;
		jcb1 = new JCheckBox("Show");
		jcb1.addActionListener(this);
		cp.add(jcb1, c);
		c.gridy++;
		jcb2 = new JCheckBox("Repeating");
		jcb2.addActionListener(this);
		cp.add(jcb2, c);
		c.gridy++;
		c.gridwidth = 1;
		model3 = new SpinnerNumberModel((float) jp.bgFade(), 0, 100.0, 1.0);
		spinner = new JSpinner(model3);
		cp.add(spinner, c);
		c.gridx = 4;
		item = new JButton("Fade");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 3;
		model4 = new SpinnerNumberModel((float)jp.bgindex(), 0, 1000.0, 1.0);
		spinner = new JSpinner(model4);
		cp.add(spinner, c);
		c.gridx = 4;
		item = new JButton("Page");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		c.gridx = 3;		
		model5 = new SpinnerNumberModel((float) jp.bgScale(), 0, 100.0,0.01);
		spinner = new JSpinner(model5);
		cp.add(spinner, c);
		c.gridx = 4;
		item = new JButton("Scale");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridx = 3;
		c.gridwidth = 2;
		c.gridy++;
		item = new JButton("Fit Page Width");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Fit Page Height");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		String sels6[] = {"Clockwise", "Counterclockwise", "Upside Down", "Rotate"};	
		combo6 = new JComboBox(sels6); 
		combo6.setSelectedIndex(3);
		combo6.addActionListener(this);
	    	cp.add(combo6, c);
		c.gridy++;
		item = new JButton("Insert Text");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Information");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;				
		jcb3 = new JCheckBox("Portable");
		jcb3.addActionListener(this);
		cp.add(jcb3, c);
		c.gridy++;
		jcb4 = new JCheckBox("Save With File");
		jcb4.addActionListener(this);
		cp.add(jcb4, c);
		if(!jarn.isApplet){
			c.gridy++;
			item = new JButton("Open");
			item.addActionListener(this);
			cp.add(item, c);
		}
		c.gridy++;
		item = new JButton("Remove");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		c.gridwidth = 5;
		c.gridx = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		item = new JButton("Apply Paper to All Pages");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Apply to All with Background");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Done");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		setRuling();
            	jw.setSize(Jarnbox.newDimension(400, 570));
		Jarnbox.setCenter(jf, jw);
            	jw.setVisible(true);
		jw.addWindowListener(new dialogClosing(this, "Done"));
	}
		
	public void actionPerformed(ActionEvent e){
		String action = e.getActionCommand();
		//System.out.println(action);
		if(action.equals("comboBoxChanged")){
			JComboBox cb = (JComboBox)e.getSource();
			try{
        			action = (String)cb.getSelectedItem();
			}
			catch(Exception ex){
				JLabel jl = (JLabel) cb.getSelectedItem();
				action = jl.getText();
			}
			//System.out.println(action);
		}
		if(action.equals("white") || action.equals("yellow") || action.equals("pink") || action.equals("orange") || action.equals("blue") || action.equals("green")) jpn.doAction(action + " paper");
		else if(action.endsWith("Lines")){
			if(action.startsWith("Set")){
				jp.setLines(model1.getNumber().intValue());
				//combo3.setSelectedIndex(3);
				jpn.doAction("Redraw Page");
			}
			else if(!action.equals("Other Lines")){
				jpn.doAction(action);
				//model1.setValue(new Float(jp.getPaper().nlines));
			}
		}
		else if(action.equals("Transparency")){
			jp.setTransparency(model2.getNumber().intValue());
			//combo5.setSelectedIndex(3);
			jpn.doAction("Redraw Page");
		}
		else if(action.equals("Fade")){
			jp.bgFade(model3.getNumber().intValue());
			jpn.doAction("Redraw Page");
		}
		else if(action.equals("Page")){
			jp.bgindex(model4.getNumber().intValue());
			jpn.doAction("Redraw Page");
		}
		else if(action.equals("Scale")){
			jp.bgScale(model5.getNumber().floatValue());
			jpn.doAction("Redraw Page");
		}
		else if(action.equals("Show"))jpn.doAction("Show Background");
		else if(action.equals("Portable")) {
			jpn.doAction("Portable Backgrounds");
			//if(jarn.saveBg) jcb4.setSelected(true);
			//else jcb4.setSelected(false);
		}
		else if(action.equals("Clockwise")) jpn.doAction("Rotate Background");
		else if(action.equals("Counterclockwise")){
			jp.setStartMark();
			jpn.doAction("Rotate Background");
			jpn.doAction("Rotate Background");
			jpn.doAction("Rotate Background");
			jp.setEndMark();
		}
		else if(action.equals("Upside Down")){
			jp.setStartMark();
			jpn.doAction("Rotate Background");
			jpn.doAction("Rotate Background");
			jp.setEndMark();
		}
		else if(action.equals("Save With File")) jpn.doAction("Save Background With File");
		else if(action.equals("other"));
		else if(action.equals("Update")) jpn.doAction("Redraw Page");
		else if(action.equals("Insert Text")) jpn.doAction("Insert Background Text");
		else if(action.equals("Information")) jpn.doAction("Background Information");
		else if(action.equals("Open")) jpn.doAction("Open Background");
		else if(action.equals("Remove")) jpn.doAction("Remove Background");
		else if(action.equals("Absolute Scale")) jpn.doAction("absoluteScale");
		else if(action.equals("Done")) jw.setVisible(false);
		else jpn.doAction(action);
		//if(action.equals("Undo") || action.equals("Redo") || action.equals("Update")) {
		//	jw.setVisible(false);
		//	showDialog(jf, jarn);
		//}
		//if(action.equals("Index Card"))setRuling();
		setRuling();
			
	}
}

class checkForUpdatesListener implements ActionListener {

	JDialog jw;
	JFrame jf;
	Jarnal jarn;
	Jpages jp;
	Jarnal.JrnlPane jpn;
	String curver = null;
	String stableverstr = null;
	String libverstr = null;

	private String readVersion(InputStream in){
		int nmax = 10000;
		byte b[] = new byte[nmax];
		String s = "";
		int nread = 0;
		try { nread = in.read(b); }
		catch (Exception e) {System.err.println(e);}
		s = new String(b,0,nread);
		return s;
	}

	private String getVersion(){
		InputStream in = null;
		try{
			in = Jarnal.class.getResourceAsStream("ver.txt");
		}
		catch (Exception ex) {System.err.println(ex); return "current 0";}
		return readVersion(in);
	}

	private String getLibVersion(){
		String libf = Jtool.getBasepath() + "lib/lib-ver.txt";
		FileInputStream fis = null;
		try{
			fis = new FileInputStream(libf);
		}
		catch (Exception ex) {System.err.println(ex); return "0";}
		return readVersion(fis);
	}

	private void checkVersions(){
		HtmlPost hp = new HtmlPost(jarn.verserver + "jarnal-ver.txt", null, null, null, null, false);
		byte b[] = hp.pipeBytes();
		if(b != null) curver = new String(b, 0, b.length);
		hp = new HtmlPost(jarn.verserver + "stable-jarnal-ver.txt", null, null, null, null, false);
		b = hp.pipeBytes();
		if(b != null) stableverstr = new String(b, 0, b.length);
		hp = new HtmlPost(jarn.verserver + "/lib/lib-ver.txt", null, null, null, null, false);
		b = hp.pipeBytes();
		if(b != null) libverstr = new String(b, 0, b.length);
		
		jw.setVisible(false);
		showDialog(jf, jarn);
	}

	private void getReleaseNotes(){
		String est = jarn.firefox;
		est = Jtool.replaceAll(est, "%1", jarn.verserver + "../../../workshops/changelog.php?econsite=econsjarn");
		try{
			Runtime.getRuntime().exec(est);	
		}
		catch(Exception ex){System.out.println("Cannot exec " + est);}
	}

	public void showDialog(JFrame jf, Jarnal jarn){
		showDialog(jf, jarn, "helpmenu");
	}

	private void showDialog(JFrame jf, Jarnal jarn, String action){
		this.jarn = jarn;
		this.jf = jf;
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		String title = "Download File?";
		if(action.equals("helpmenu")) title = "Check For Updates";
		jw = new JDialog(jf, title, false);
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		String ver = getVersion().trim();
		String libver = getLibVersion().trim();
		boolean stablever = false;
		if(ver.startsWith("stable")) stablever = true;
		int nver = ver.indexOf(" ");
		ver = ver.substring(nver).trim();
		nver = Integer.parseInt(ver);
		String vermsg = "<html>You are running ";
		if(stablever) vermsg = vermsg + "stable ";
		else vermsg = vermsg + "current ";
		vermsg = vermsg + "version " + ver;
		vermsg = vermsg + "<br>with library version " + libver + "<br></html>";
		JLabel label = new JLabel(vermsg);
		cp.add(label, c);
		c.gridy++;
		JButton item = null;
		if(action.equals("stable")){
			item = new JButton("Download stable version " + stableverstr + "?");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
			label = new JLabel("<html><br>File will be downloaded to<br>" + Jtool.getBasepath() + "upgrade-jarnal.jar<br>");
			cp.add(label, c);
			c.gridy++;
		}
		if(action.equals("current")){
			item = new JButton("Download current version " + curver + "?");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
			label = new JLabel("<html><br>File will be downloaded to<br>" + Jtool.getBasepath() + "upgrade-jarnal.jar<br>");
			cp.add(label, c);
			c.gridy++;
		}
		if(action.equals("library")){
			item = new JButton("Download library version " + libverstr + "?");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
			label = new JLabel("<html><br>Files will be downloaded to<br>" + Jtool.getBasepath() + "upgrade-lib<br>");
			cp.add(label, c);
			c.gridy++;
		}			
		if(action.equals("helpmenu")){
			item = new JButton("Check Web For More Recent Versions");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;

			if(curver != null){
				item = new JButton("Update to Available Current Version " + curver);
				item.addActionListener(this);
				cp.add(item, c);
				c.gridy++;
			}

			if(stableverstr != null){
				item = new JButton("Update to Available Stable Version " + stableverstr);
				item.addActionListener(this);
				cp.add(item, c);
				c.gridy++;
			}

			if(libverstr != null){
				item = new JButton("Update to Available Library Version " + libverstr);
				item.addActionListener(this);
				cp.add(item, c);
				c.gridy++;
			}

			item = new JButton("View Release Notes");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
		}
		
		item = new JButton("Exit This Dialog");
		item.addActionListener(this);
		cp.add(item, c);
            	jw.setSize(Jarnbox.newDimension(380, 260));
		Jarnbox.setCenter(jf, jw);
            	jw.setVisible(true);
		jw.addWindowListener(new dialogClosing(this, "Exit This Dialog"));
	}

	private void downloadFile(String action){
		jw.setVisible(false);
		String source = "jarnal.jar";
		if(action.equals("stable")) source = "stable-jarnal.jar";
		String target = Jtool.getBasepath();
		if(action.equals("library")){
			source = "jpedallib.zip";
			target = target + "upgrade-lib.zip";
		}
		else target = target + "upgrade-jarnal.jar";
		HtmlPost hp = new HtmlPost(jarn.verserver + source, null, null, null, null, false);
		boolean success = hp.pipeFile(target);
		if(success && action.equals("library")){
			File dirName = new File(Jtool.getBasepath() + "upgrade-lib");
			dirName.mkdir();
			try{
				FileInputStream fis = new FileInputStream(target);
				if(fis != null){
					ZipInputStream zip = new ZipInputStream(fis);
					for(ZipEntry ze = zip.getNextEntry(); ze != null; ze = zip.getNextEntry()){
						String zname = ze.getName().substring(4);
						FileOutputStream fos = new FileOutputStream(Jtool.getBasepath() + "upgrade-lib" + File.separator + zname);
						int nmin = 1000000;
						int nborg = 40000;
						int nmax = nmin + ( 5 * nborg); 
						byte b[] = new byte[nmax];
						int nread = 0;
						int noff = 0;

						while((nread = zip.read(b, noff, nborg)) >= 0){
							noff = noff + nread;
							if(noff > nmax - (2 * nborg)){
								fos.write(b, 0, noff);
								noff = 0;
							}
						}
						fos.write(b, 0, noff);
						fos.close();
					}
					zip.close();
					fis.close();
					(new File(target)).delete();				
				}
			} catch(Exception ex){ex.printStackTrace();}
		}	
	}
		
	public void actionPerformed(ActionEvent e){
		String action = e.getActionCommand();
		System.out.println(action);
		if(action.equals("Exit This Dialog")) jw.setVisible(false);
		if(action.equals("View Release Notes")) getReleaseNotes();
		if(action.equals("Check Web For More Recent Versions")) checkVersions();
		if(action.startsWith("Update to Available Stable")){
			jw.setVisible(false);
			showDialog(jf, jarn, "stable");
		}
		if(action.startsWith("Update to Available Current")){
			jw.setVisible(false);
			showDialog(jf, jarn, "current");
		}
		if(action.startsWith("Update to Available Library")){
			jw.setVisible(false);
			showDialog(jf, jarn, "library");
		}
		if(action.startsWith("Download current")) downloadFile("current");
		if(action.startsWith("Download stable")) downloadFile("stable");
		if(action.startsWith("Download library")) downloadFile("library");
	}
}

class screenShotListener implements ActionListener {

	JDialog jw;
	JFrame jf;
	JComboBox combo1;
	JCheckBox jcb1;
	JCheckBox jcb2;
	Jarnal jarn;
	Jpages jp;
	Jarnal.JrnlPane jpn;

	public void showDialog(JFrame jf, Jarnal jarn, String title){
		this.jarn = jarn;
		this.jf = jf;
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		jw = new JDialog(jf, title, false);
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		String sels1[] = {"Image", "Background"};
		combo1 = new JComboBox(sels1);
		combo1.setSelectedIndex(0);
		if(title.equals("Background Screenshot")) combo1.setSelectedIndex(1);
		combo1.addActionListener(this);
		cp.add(combo1, c);
		c.gridy++;
		jcb1 = new JCheckBox("Entire Screen");
		jcb1.addActionListener(this);
		if(jarn.ascr) jcb1.setSelected(true);
		cp.add(jcb1, c);	
		c.gridy++;
		jcb2 = new JCheckBox("Minimize for Screenshot");
		jcb2.addActionListener(this);
		if(jarn.mscr) jcb2.setSelected(true);
		cp.add(jcb2, c);
		c.gridy++;
		JButton item = new JButton("Cancel");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Shoot");
		item.addActionListener(this);
		cp.add(item, c);
            	jw.setSize(Jarnbox.newDimension(240, 180));
		Jarnbox.setCenter(jf, jw);
            	jw.setVisible(true);
		jw.addWindowListener(new dialogClosing(this, "Cancel"));
	}
		
	public void actionPerformed(ActionEvent e){
		String action = e.getActionCommand();
		System.out.println(action);
		if(action.equals("comboBoxChanged")) return;
		if(action.equals("Cancel")) jw.setVisible(false);
		else if(action.equals("Shoot")){
			jw.setVisible(false);
			if(((String)combo1.getSelectedItem()).equals("Image")) jpn.doAction("Screenshot Image");
			else jpn.doAction("Screenshot Background");
		}
		else jpn.doAction(action);			
	}
}

class printOptionsListener implements ActionListener {

	JDialog jw;
	JFrame jf;
	JCheckBox jcb1;
	JCheckBox jcb2;
	JCheckBox jcb3;
	JCheckBox jcb4;
	JCheckBox jcb5;
	Jarnal jarn;
	Jpages jp;
	Jarnal.JrnlPane jpn;

	public void showDialog(JFrame jf, Jarnal jarn){
		this.jarn = jarn;
		this.jf = jf;
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		jw = new JDialog(jf, "Print Options", false);
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		jcb1 = new JCheckBox("Align to Margins");
		jcb1.addActionListener(this);
		if(jarn.alignToMargins) jcb1.setSelected(true);
		cp.add(jcb1, c);	
		c.gridy++;
		jcb2 = new JCheckBox("Fit to Page");
		jcb2.addActionListener(this);
		if(jarn.bestFit) jcb2.setSelected(true);
		cp.add(jcb2, c);
		c.gridy++;
		jcb3 = new JCheckBox("Absolute Scale");
		jcb3.addActionListener(this);
		if(jarn.absoluteScale) jcb3.setSelected(true);
		cp.add(jcb3, c);	
		c.gridy++;
		jcb4 = new JCheckBox("Show Page Numbers");
		jcb4.addActionListener(this);
		if(jarn.showPageNumbers) jcb4.setSelected(true);
		cp.add(jcb4, c);
		c.gridy++;
		jcb5 = new JCheckBox("Print Borders");
		jcb5.addActionListener(this);
		if(jarn.withBorders) jcb5.setSelected(true);
		cp.add(jcb5, c);
		c.gridy++;
		JButton item = new JButton("Exit This Dialog");
		item.addActionListener(this);
		cp.add(item, c);
		if(!jarn.isApplet){
			c.gridy++;
			item = new JButton("Print as PDF");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
			item = new JButton("Send PDF as Email");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
			item = new JButton("Print Background");
			item.addActionListener(this);
			//cp.add(item, c);
			//c.gridy++;
			item = new JButton("Print via PDF");
			item.addActionListener(this);
			//cp.add(item, c);
		}
		c.gridy++;
		item = new JButton("Print");
		item.addActionListener(this);
		cp.add(item, c);
            	//jw.setSize(newDimension(240, 300));
            	jw.setSize(Jarnbox.newDimension(240, 260));
		Jarnbox.setCenter(jf, jw);
            	jw.setVisible(true);
		jw.addWindowListener(new dialogClosing(this, "Exit This Dialog"));
	}
		
	public void actionPerformed(ActionEvent e){
		String action = e.getActionCommand();
		System.out.println(action);
		if(action.startsWith("Exit")) jw.setVisible(false);
		else if(action.equals("Align to Margins")) jpn.doAction("alignToMargins");
		else if(action.equals("Fit to Page")) jpn.doAction("bestFit");
		else if(action.equals("Absolute Scale")) jpn.doAction("absoluteScale");
		else if(action.equals("Show Page Numbers")) jpn.doAction("showPageNumbers");
		else if(action.equals("Print Borders")) jpn.doAction("withBorders");
		else if(action.startsWith("Print") || action.startsWith("Send")){
			jw.setVisible(false);
			jpn.doAction(action);
		}		
	}
}

class saveOptionsListener implements ActionListener {

	JDialog jw;
	JFrame jf;
	JCheckBox jcb1;
	JCheckBox jcb2;
	JCheckBox jcb3;
	JCheckBox jcb4;
	JCheckBox jcb5;
	JCheckBox jcb6;
	Jarnal jarn;
	Jpages jp;
	Jarnal.JrnlPane jpn;
	boolean thenExit = false;

	private void setDialog(){
		jcb1.setSelected(jarn.saveSelfexecuting);
		jcb2.setSelected(jarn.saveBg);
		jcb3.setSelected(jp.portableBgs);
		jcb6.setSelected(jp.recordingOn());
		jcb4.setSelected(jarn.saveOnExit);
		jcb5.setSelected(jarn.saveBookmarks);
	}

	public void setExit(){
		thenExit = true;
	}

	public void showDialog(JFrame jf, Jarnal jarn){
		this.jarn = jarn;
		this.jf = jf;
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		jw = new JDialog(jf, "Save Options", false);
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		jcb1 = new JCheckBox("Save Self Executing");
		jcb1.addActionListener(this);
		cp.add(jcb1, c);	
		c.gridy++;
		jcb2 = new JCheckBox("Save Background With File");
		jcb2.addActionListener(this);
		cp.add(jcb2, c);
		c.gridy++;
		jcb3 = new JCheckBox("Portable Backgrounds");
		jcb3.addActionListener(this);
		cp.add(jcb3, c);	
		c.gridy++;
		jcb6 = new JCheckBox("Record");
		jcb6.addActionListener(this);
		cp.add(jcb6, c);	
		c.gridy++;
		jcb4 = new JCheckBox("Save On Close");
		jcb4.addActionListener(this);
		cp.add(jcb4, c);
		c.gridy++;
		jcb5 = new JCheckBox("Save User Info");
		jcb5.addActionListener(this);
		cp.add(jcb5, c);
		c.gridy++;
		JButton item = new JButton("Exit This Dialog");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Send As Email");
		item.addActionListener(this);
		cp.add(item,c);
		if(!jarn.isApplet){
			c.gridy++;
			item = new JButton("Save");
			item.addActionListener(this);
			cp.add(item, c);
			c.gridy++;
			item = new JButton("Save As");
			item.addActionListener(this);
			cp.add(item, c);
		}
		setDialog();
            	jw.setSize(Jarnbox.newDimension(240, 280));
		Jarnbox.setCenter(jf, jw);
            	jw.setVisible(true);
		jw.addWindowListener(new dialogClosing(this, "Exit This Dialog"));
	}
		
	public void actionPerformed(ActionEvent e){
		String action = e.getActionCommand();
		System.out.println(action);
		boolean reset = true;
		if(action.startsWith("Exit")) {
			jw.setVisible(false);
			return;
		}
		if(action.equals("Save User Info")) action = "Save Bookmarks";
		if(action.equals("Save") || action.equals("Save As") || action.equals("Send As Email")) {
			jw.setVisible(false);
			reset = false;
			if(action.equals("Save")) action = "Savex";
		}
		jpn.doAction(action);
		if(reset) setDialog();
		else if(thenExit) jpn.doAction("Close");			
	}
}

class overlayDialogListener implements ActionListener {

	JDialog jw;
	JFrame jf;
	JComboBox combo1;
	JComboBox combo2;
	SpinnerNumberModel model1;
	SpinnerNumberModel model2;
	SpinnerNumberModel model3;
	SpinnerNumberModel model4;
	Jarnal jarn;
	Jpages jp;
	Jarnal.JrnlPane jpn;
	boolean lockgui = false;

	private void setDialog(){
		lockgui = true;
		String str = jp.getOverlayStyle(jpn.defaultOverlay);
		Joverlay jo = new Joverlay();
		String a = jo.getParm(str, "stroke-width=");
		if(a != null) model2.setValue(new Double(Integer.parseInt(a, 10))); 
		a = jo.getParm(str, "fill=");
		if(a.equals("white")) combo1.setSelectedIndex(0); 
		if(a.equals("yellow")) combo1.setSelectedIndex(1); 
		if(a.equals("pink")) combo1.setSelectedIndex(2); 
		if(a.equals("orange")) combo1.setSelectedIndex(3); 
		if(a.equals("blue")) combo1.setSelectedIndex(4); 
		if(a.equals("green")) combo1.setSelectedIndex(5); 	
		a = jo.getParm(str, "stroke=");
		if(a.equals("black")) combo2.setSelectedIndex(0);
		if(a.equals("blue")) combo2.setSelectedIndex(1);
		if(a.equals("green")) combo2.setSelectedIndex(2);
		if(a.equals("gray")) combo2.setSelectedIndex(3);
		if(a.equals("magenta")) combo2.setSelectedIndex(4);
		if(a.equals("pink")) combo2.setSelectedIndex(5);
		if(a.equals("orange")) combo2.setSelectedIndex(6);
		if(a.equals("red")) combo2.setSelectedIndex(7);
		if(a.equals("white")) combo2.setSelectedIndex(8);
		if(a.equals("yellow")) combo2.setSelectedIndex(9);
		a = jo.getParm(str, "fill-opacity=");
		if(a != null) {
			float fo = Float.parseFloat(a);
			model1.setValue(new Double((int)(100.0f * (1.0f - fo))));
		}
		a = jo.getParm(str, "stroke-opacity=");
		if(a != null) {
			float fo = Float.parseFloat(a);
			int strokeFade = (int)(100.0f * (1.0f - fo));
		}
		a = jo.getParm(str, "rx=");
		if(a != null){
			float z = Float.parseFloat(a);
			a = jo.getParm(str, "width=");
			if(a != null){
				int width = Integer.parseInt(a, 10);
				int arcWidth = (int)((z * 200.0f)/(float) width);
				model3.setValue(new Integer(arcWidth));
			}
		}
		a = jo.getParm(str, "ry=");
		if(a != null){
			float z = Float.parseFloat(a);
			a = jo.getParm(str, "height=");
			if(a != null){
				int height = Integer.parseInt(a, 10);
				int arcHeight = (int)((z * 200.0f)/(float) height);
				model4.setValue(new Integer(arcHeight));
			}
		}
		lockgui = false;
	}

	public void showDialog(JFrame jf, Jarnal jarn){
		this.jarn = jarn;
		this.jf = jf;
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		jw = new JDialog(jf, "Overlay", false);
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		JButton item = new JButton("Undo");
		item.addActionListener(this);
		cp.add(item, c); 
		c.gridx = 1;
		c.weightx = 0;
		item = new JButton("Redo");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		item = new JButton("Reset Dialog");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		JLabel label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		String sels1[] = {"white", "yellow", "pink", "orange", "blue", "green"};
		JLabel jmi[] = new JLabel[6];
			for(int ii = 0; ii < 6; ii++) jmi[ii] = new JLabel(sels1[ii] + " overlay", new colorIcon(sels1[ii]), JLabel.CENTER);	
		combo1 = new JComboBox(jmi); 
		combo1.setRenderer(new labelCellRenderer());
		combo1.addActionListener(this);
	    	cp.add(combo1, c);
		c.gridy++;
		c.gridwidth = 1;
		model1 = new SpinnerNumberModel(1, 0, 100.0, 1.0);
		JSpinner spinner = new JSpinner(model1);
		cp.add(spinner, c);
		c.gridx = 1;
		c.weightx = 0;
		item = new JButton("Fade Overlay");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;
		String sels2[] = {"black", "blue", "green", "gray", "magenta", "pink", "orange", "red", "white", "yellow"};
		jmi = new JLabel[10];
			for(int ii = 0; ii < 10; ii++) jmi[ii] = new JLabel(sels2[ii] + " outline", new colorIcon(sels2[ii]), JLabel.CENTER);
		combo2 = new JComboBox(jmi); 
		combo2.setRenderer(new labelCellRenderer());
		combo2.addActionListener(this);
	    	cp.add(combo2, c);
		c.gridy++;
		c.gridwidth = 1;
		model2 = new SpinnerNumberModel(1, 0, 20.0, 1.0);
		spinner = new JSpinner(model2);
		cp.add(spinner, c);
		c.gridx = 1;
		c.weightx = 0;
		item = new JButton("Thickness");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		c.gridx = 0;
		label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;
		model3=new SpinnerNumberModel(0, 0, 100, 1);
		spinner = new JSpinner(model3);
		cp.add(spinner, c);
		c.gridx = 1;
		c.weightx = 0;
		//c.gridheight = 2;
		item = new JButton("Equalize");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Roundness");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridheight = 1;
		c.gridx = 0;
		//c.gridy++;
		model4 = new SpinnerNumberModel(0, 0, 100, 1);
		spinner = new JSpinner(model4);
		cp.add(spinner, c);
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		item = new JButton("Make Square");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Insert Overlay");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		c.gridwidth = 1;
		item = new JButton("Circle");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridx++;
		item = new JButton("Square");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy++;
		item = new JButton("Done");
		item.addActionListener(this);
		cp.add(item, c);
		setDialog();		
            	jw.setSize(Jarnbox.newDimension(240, 440));
		Jarnbox.setCenter(jf, jw);
            	jw.setVisible(true);
		jw.addWindowListener(new dialogClosing(this, "Done"));
	}
		
	public void actionPerformed(ActionEvent e){
		if(lockgui) return;
		String action = e.getActionCommand();
		System.out.println(action);
		if(action.equals("comboBoxChanged")){
			JComboBox cb = (JComboBox)e.getSource();
			try{
        			action = (String)cb.getSelectedItem();
			}
			catch(Exception ex){
				JLabel jl = (JLabel)cb.getSelectedItem();
				action = jl.getText();
			}
			System.out.println(action);
		}
		if(action.equals("Done")) jw.setVisible(false);
		else if(action.equals("Reset Dialog")) setDialog();
		else if(action.equals("Fade Overlay")){
			jpn.defaultOverlay = jp.setOverlayStyle(jpn.defaultOverlay, -1, -1, null, null, -1, model1.getNumber().intValue(), -1);
			jarn.dirty = true;
			jp.invalidateGraphics();
			jpn.doAction("Redraw Page");
		}
		else if(action.equals("Thickness")){
			jpn.defaultOverlay = jp.setOverlayStyle(jpn.defaultOverlay, -1, -1, null, null, model2.getNumber().intValue(), -1, -1);
			jarn.dirty = true;
			jp.invalidateGraphics();
			jpn.doAction("Redraw Page");
		}
		else if(action.equals("Equalize")){
			model4.setValue(model3.getNumber());
		}
		else if(action.equals("Roundness")){
			jpn.defaultOverlay = jp.setOverlayStyle(jpn.defaultOverlay, model3.getNumber().intValue(), model4.getNumber().intValue(), null, null, -1, -1, -1);
			jarn.dirty = true;
			jp.invalidateGraphics();
			jpn.doAction("Redraw Page");
		}
		else if(action.equals("Make Square")){
			jp.makeOverlaySquare(jpn.defaultOverlay);
			jarn.dirty = true;
			jp.invalidateGraphics();
			jpn.doAction("Redraw Page");
		}			 
		else jpn.doAction(action);
	}
}

class textDialogListener implements ActionListener {

	JDialog jw;
	JFrame jf;
	JCheckBox jcb1;
	JCheckBox jcb2;
	JCheckBox jcb3;
	JComboBox combo1;
	JComboBox combo2;
	JComboBox combo3;
	String cols1[] = {"black", "blue", "green", "gray", "magenta", "orange", "pink", "red", "white", "yellow"};
	String sels1[] = {"Black Text", "Blue Text", "Green Text", "Gray Text", "Magenta Text", "Orange Text", "Pink Text", "Red Text", "White Text", "Yellow Text"};
	String sels2[] = {" 6pt", " 7pt", " 8pt", " 9pt", "10pt", "11pt", "12pt", "13pt", "14pt", "15pt", "16pt", "18pt", "20pt", "22pt", "24pt", "26pt", "28pt", "32pt", "36pt", "40pt", "48pt", "54pt", "60pt", "66pt", "72pt", "80pt", "88pt", "96pt"};
	String sels3[];
	Jarnal jarn;
	Jpages jp;
	Jarnal.JrnlPane jpn;
	boolean lockgui = false;

	private void setCombo(JComboBox cb, String[] ls, String targ){
		for(int ii = 0; ii < ls.length; ii++){
			if(targ.toLowerCase().equals(ls[ii].toLowerCase())) cb.setSelectedIndex(ii);
		}
	}

	private void setDialog(){
		lockgui = true;
		String[] sty = jp.getTextStyle();
		if(sty[3] != null) jcb1.setSelected(true);
		else jcb1.setSelected(false);
		if(sty[5] != null) jcb2.setSelected(true);
		else jcb2.setSelected(false);
		if(sty[4] != null) jcb3.setSelected(true);
		else jcb3.setSelected(false);
		setCombo(combo1, sels1, sty[2] + " Text");
		setCombo(combo2, sels2, "" + sty[1] + "pt");
		setCombo(combo3, sels3, "Font " + sty[0]);
		lockgui = false;
	}

	public void showDialog(JFrame jf, Jarnal jarn){
		this.jarn = jarn;
		this.jf = jf;
		jpn = jarn.jrnlPane;
		jp = jpn.jpages;
		jw = new JDialog(jf, "Text Style", false);
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		JButton item = new JButton("Undo");
		item.addActionListener(this);
		cp.add(item, c); 
		c.gridy++;
		item = new JButton("Redo");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Update");
		item.addActionListener(this);
		cp.add(item, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;
		JLabel label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;
		jcb1 = new JCheckBox("Bold Text");
		jcb1.addActionListener(this);
		cp.add(jcb1, c);
		c.gridy++;
		jcb2 = new JCheckBox("Underline Text");
		jcb2.addActionListener(this);
		cp.add(jcb2, c);
		c.gridy++;
		jcb3 = new JCheckBox("Italic Text");
		jcb3.addActionListener(this);
		cp.add(jcb3, c);
		c.gridy++;
		JLabel jmi[] = new JLabel[10];
			for(int ii = 0; ii < 10; ii++) jmi[ii] = new JLabel(sels1[ii], new colorIcon(cols1[ii]), JLabel.CENTER);
		combo1 = new JComboBox(jmi); 
		combo1.setRenderer(new labelCellRenderer());	
		//combo1 = new JComboBox(sels1); 
		combo1.addActionListener(this);
	    	cp.add(combo1, c);
		c.gridy++;
		combo2 = new JComboBox(sels2); 
		combo2.addActionListener(this);
	    	cp.add(combo2, c);
		c.gridy++;
		String fn[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		sels3 = new String[fn.length];
		for(int i = 0; i < fn.length; i++) sels3[i] = "Font " + fn[i];
		combo3 = new JComboBox(sels3); 
		combo3.addActionListener(this);
	    	cp.add(combo3, c);
		c.gridy++;
		label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;		
		c.fill = GridBagConstraints.NONE;
		item = new JButton("Set Text Default");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;		
		item = new JButton("Done");
		item.addActionListener(this);
		cp.add(item, c);
		setDialog();		
            	jw.setSize(Jarnbox.newDimension(240, 340));
		Jarnbox.setCenter(jf, jw);
            	jw.setVisible(true);
		jw.addWindowListener(new dialogClosing(this, "Done"));
	}
		
	public void actionPerformed(ActionEvent e){
		if(lockgui) return;
		String action = e.getActionCommand();
		System.out.println(action);
		if(action.equals("comboBoxChanged")){
			JComboBox cb = (JComboBox)e.getSource();
			try{
        			action = (String)cb.getSelectedItem();
			}
			catch(Exception ex){
				JLabel jl = (JLabel) cb.getSelectedItem();
				action = jl.getText();
			}
			System.out.println(action);
		}
		if(action.equals("Done")) jw.setVisible(false);
		else if(action.equals("Update")) setDialog();
		else jpn.doAction(action);
	}
}

class selectionDialogListener implements ActionListener {

	JDialog jw;
	JComboBox combo1;
	JComboBox combo2;
	JComboBox combo3;
	SpinnerNumberModel model1;
	SpinnerNumberModel model2;
	SpinnerNumberModel model3;
	Jarnal jarn;
	Jpages jp;
	Jtool curPen;
	boolean done = false;

	public void showDialog(JFrame jf, Jtool curPen, Jarnal jarn){
		this.curPen = curPen;
		this.jarn = jarn;
		jp = jarn.jrnlPane.jpages;
		jw = new JDialog(jf, "Modify Selection", false);
		jw.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		Container cp = jw.getContentPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0.5;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;
		JButton item = new JButton("Undo");
		item.addActionListener(this);
		cp.add(item, c); 
		c.gridx = 2;
		item = new JButton("Redo");
		item.addActionListener(this);
		cp.add(item, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 1;
		c.gridx = 0;
		JLabel label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;			
		c.gridwidth = 1;
		model1 = new SpinnerNumberModel(1.0, 0, 100.0, 0.1);
		JSpinner spinner = new JSpinner(model1);
		cp.add(spinner, c);
		c.gridx = 1;
		c.weightx = 0;
		item = new JButton("Pen Width        ");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		String sels1[] = {"no change", "black", "blue", "green", "gray", "magenta", "orange", "pink", "red", "white", "yellow"};
		JLabel jmi[] = new JLabel[11];
			for(int ii = 0; ii < 11; ii++) jmi[ii] = new JLabel(sels1[ii], new colorIcon(sels1[ii]), JLabel.CENTER);
		combo1 = new JComboBox(jmi); 
		combo1.setRenderer(new labelCellRenderer());
		combo1.addActionListener(this);
    		cp.add(combo1, c);
		c.gridy++;			
		c.gridwidth = 1;
		c.gridx = 0;
		model3 = new SpinnerNumberModel(0, -100, 100, 1);
		spinner = new JSpinner(model3);
		cp.add(spinner, c);
		c.gridx = 1;
		c.weightx = 0;
		item = new JButton("Arrow Weight");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridwidth = 2;
		c.gridy = 1;
		c.gridx = 2;
		label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;
		c.gridwidth = 1;
		model2 = new SpinnerNumberModel(1.0, 0, 100.0, 0.1);
		spinner = new JSpinner(model2);
		cp.add(spinner, c);
		c.gridx = 3;
		c.weightx = 0;
		item = new JButton("Highlighter Width");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridwidth = 2;
		c.gridx = 2;
		c.gridy++;
		//String sels2[] = {"no change", "black", "blue", "green", "gray", "magenta", "orange", "pink", "red", "white", "yellow"};
		jmi = new JLabel[11];
		for(int ii = 0; ii < 11; ii++) jmi[ii] = new JLabel(sels1[ii] + " highlighter", new colorIcon(sels1[ii]), JLabel.CENTER);
		//for(int ii = 1; ii < sels2.length; ii++) sels2[ii] = sels2[ii] + " highlighter";
		combo2 = new JComboBox(jmi); 
		combo2.setRenderer(new labelCellRenderer());
		combo2.addActionListener(this);
    		cp.add(combo2, c);
		c.gridy++;
		String sels3[] = {"No Change", "Translucent", "Transparent"};
		//for(int ii = 0; ii < sels2.length; ii++) sels2[ii] = sels2[ii] + " highlighter";
		combo3 = new JComboBox(sels3); 
		combo3.addActionListener(this);
    		cp.add(combo3, c);
		c.gridy++;
		label = new JLabel(" ");
		cp.add(label, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 4;
		c.fill = GridBagConstraints.NONE;
		item = new JButton("Smooth Strokes");
		item.addActionListener(this);
		cp.add(item, c);
		c.gridy++;
		item = new JButton("Apply Current Pen");
		item.addActionListener(this);
		cp.add(item, c); 
		c.gridy++;           
		item = new JButton("Done");
		item.addActionListener(this);
		cp.add(item, c);
		jw.setSize(Jarnbox.newDimension(420, 260));
		Jarnbox.setCenter(jf, jw);
           	jw.setVisible(true);
		jw.addWindowListener(new dialogClosing(this, "Done"));
	}
		
	public void actionPerformed(ActionEvent e){
		String action = e.getActionCommand();
		if(action.equals("comboBoxChanged")){
			JComboBox cb = (JComboBox)e.getSource();
			try{
        			action = (String)cb.getSelectedItem();
			}
			catch(Exception ex){
				JLabel jl = (JLabel) cb.getSelectedItem();
				action = jl.getText();
			}
			if(!action.equals("Translucent") && !action.equals("Transparent")){
				if(action.endsWith("highlighter")){
					int n = action.indexOf(" ");
					action = action.substring(0, n);
					jp.applyPen("no change", action, 1.0f , 1.0f, "No Change", null);
				}
				else
					jp.applyPen(action, "no change", 1.0f , 1.0f, "No Change", null);
				jarn.jrnlPane.doAction("Redraw Page");
				return;
			}
		}
		System.out.println(action);
		if(action.equals("Done")) jw.setVisible(false);
		if(action.equals("Undo") || action.equals("Redo")) jarn.jrnlPane.doAction(action);
		if(action.startsWith("Pen Width")){
			jp.applyPen("no change", "no change", model1.getNumber().floatValue(), 1.0f, "No Change", null);
			jarn.jrnlPane.doAction("Redraw Page");
		}
		if(action.startsWith("Arrow Weight")){
			jarn.markerweight = model3.getNumber().intValue();
			jarn.jrnlPane.doAction("Arrow Weight");
		}
		if(action.equals("Highlighter Width")){
			jp.applyPen("no change", "no change", 1.0f , model2.getNumber().floatValue(), "No Change", null);
			jarn.jrnlPane.doAction("Redraw Page");
		}
		if(action.equals("Apply Current Pen")){
			jp.applyPen("no change", "no change", 1.0f, 1.0f, "No Change", curPen);
			jarn.jrnlPane.doAction("Redraw Page");
		}
		if(action.equals("Translucent") || action.equals("Transparent")){
			jp.applyPen("no change", "no change", 1.0f, 1.0f, action, null);
			jarn.jrnlPane.doAction("Redraw Page");
		}
		if(action.equals("Smooth Strokes")){
			jp.applyPen("smooth strokes", "no change", 1.0f, 1.0f, "No Change", null);
			jarn.jrnlPane.doAction("Redraw Page");
		}	
	}
}



