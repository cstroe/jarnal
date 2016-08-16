package jarnal;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;

public class Out {

	OutNode nodes = new OutNode(null, "root", null);
	int cursor[] = new int[1];
	int olevels;
	int oheight;
	Container cp;
	OutPane op;
	OutNode hitCursor, dragCursor;
	Jarnal jarn;
	JMenu spopm = new JMenu("Context");
	JMenu shortm = new JMenu("Context");
	JMenu popm;
	JMenu colorm = new JMenu("Background Color");
	int h = 1;
	int w = 1;
	int hilevel = 0;
	int ww = 1;
	int hh = 20;
	int oh = 5;
	int www = 20;
	public boolean dragop = false;
	public String bgcolor = "blue";
	public boolean synchPages = true;
    	JCheckBoxMenuItem spa = new JCheckBoxMenuItem("Synchronize Pages");

	public Out(Jarnal jarn){
		this.jarn = jarn;
		op = new OutPane();
		op.addMouseListener(new OutMouseListener());
        	op.addMouseMotionListener(new OutMouseMotionListener());
		op.addComponentListener(new OutSizeListener());
		spopm.add(bmi("Add Page"));
		spopm.add(bmi("Add Page As Subhead"));
		spopm.add(bmi("Add Page After"));
		spopm.add(bmi("Add Subhead"));
		spopm.add(bmi("Add After"));
		spopm.addSeparator();
		spopm.add(bmi("Edit"));
		spopm.add(bmi("Remove Page"));
		spopm.add(bmi("Delete"));
		spopm.addSeparator();
		spopm.add(spa);
		spa.addActionListener(new OutActionListener("Synchronize Pages"));
		spa.setState(true);
		spopm.addSeparator();
		spopm.add(colorm);
		colorm.add(bmi("blue"));
		colorm.add(bmi("red"));
		colorm.add(bmi("green"));
		popm = spopm;
		shortm.add(bmi("Add Page"));
		shortm.add(bmi("Remove Page"));
		shortm.add(bmi("Delete"));	
	}

	public void initOut(Container cp){
		cp.add(op);
		this.cp = cp;
		if(nodes.size() == 0){
			OutNode ond = new OutNode(nodes, "[No Title]", null);
			nodes.add(ond);
		}
		if(hitCursor == null) hitCursor = nodes.get(0);
	}

	public void setFullScreen(boolean tof){
		if(tof){
			popm = shortm;
			synchPages = true;
		}
		else popm = spopm;
	}	 

	public JMenuItem bmi(String action){
        	JMenuItem item;
        	item = new JMenuItem(action);
        	item.addActionListener(new OutActionListener(action));
        	return item;
	}

	public void setEntireOutline(String xml){
		nodes = new OutNode(null, "root", null);
		setOutline0(xml);
		if(nodes.size() == 0){
			OutNode ond = new OutNode(nodes, "[No Title]", null);
			nodes.add(ond);
		}
	}

	public String bghandle = null;

	public void setOutline(String xml, String bghandle){
		
		this.bghandle = bghandle;
		setOutline(xml);
		this.bghandle = null;
	}
		

	public void setOutline(String xml){
		
		jarn.dirty = true;
		String oldXML = getXML();
		jarn.jrnlPane.setStatus("");
		setOutline0(xml);
		jarn.jrnlPane.pages.setOutlineUndo(oldXML);
	}

	public void setOutline0(String xml){
		
		if(xml == null) return;
		int last = nodes.size();
		nodes.addXML(xml, last - 1);
		oset(true);
		op.repaint();
	}

	public String getXML(){
		
		if(nodes.size() == 0) return null;
		return nodes.getXML();
	}

	private void oset(boolean newCursor){
		
		olevels = 0;
		oheight = 0;
		oset(nodes, 0);
		if(olevels == 0) olevels = 1;
		if(oheight == 0) oheight = 1;
		int ocursor[] = new int[cursor.length];
		for(int ii = 0; ii < cursor.length; ii++) ocursor[ii]=cursor[ii];
		cursor = new int[olevels];
		for(int ii = 0; ii < olevels; ii++) cursor[ii] = 0;
		if(!newCursor)
			for(int ii = 0; (ii < cursor.length) && (ii < ocursor.length); ii++)
				cursor[ii] = ocursor[ii];
		if(cp != null){
			h = cp.getHeight();
			w = cp.getWidth();
			ww = w / olevels;
			hh = (int)((float) h / ((float) oheight + 0.5f));
			oh = hh/4;
		}
	}

	public void synchPage(int dir){
		
		if(dir == 0) return;
		if(!synchPages) return;
		if(nodes.size() == 0) return;
		if(hitCursor == null) hitCursor = nodes.get(0);
		Pages pages = jarn.jrnlPane.pages;
		int curp = pages.getPage() - 1;
		LinkedList pp = new LinkedList();
		LinkedList nn = new LinkedList();
		nodes.flatten(pp, nn);
		int imin = 100000;
		for(int ii = 0; ii < nn.size(); ii++){
			int jj = curp - ((Integer) pp.get(ii)).intValue();
			if((jj >= 0) && (jj < imin)) imin = jj;
		}	
		LinkedList nn2 = new LinkedList();
		int hc = 0;
		boolean found = false;
		boolean addedCurrent = false;
		for(int ii = 0; ii < nn.size(); ii++){
			int jj = curp - ((Integer) pp.get(ii)).intValue();
			if(jj == imin) {
				nn2.add(nn.get(ii));
				if(nn.get(ii) == hitCursor)found = true;
				if(!found) hc++;
			}
			else if(nn.get(ii) == hitCursor){
				nn2.add((OutNode)nn.get(ii));
				found = true;
				addedCurrent = true;
			}
		}
		if((nn2.size() == 1) && (nn.get(0) != hitCursor) && addedCurrent){
			nn2.add(0, nn.get(0));
			hc++;
		}
		else if((nn2.size() == 1) && addedCurrent) addedCurrent = false;
		if(dir >= 0){
			hc++;
			if(hc >= nn2.size()) {
				if(addedCurrent) hc = nn2.size() - 2;
				else hc = nn2.size() - 1;
			}
			hitCursor = (OutNode)nn2.get(hc);
		}
		if(dir < 0){
			hc--;
			if(hc < 0) {
				if(addedCurrent) hc = 1;
				else hc = 0;
			}
			hitCursor = (OutNode)nn2.get(hc);
		}
		if(hitCursor == null) return;
		hilevel = hitCursor.setcursor();
		op.repaint();				
	}

	private void oset(OutNode ond, int level){
		
		int nc = ond.size();
		if(nc > oheight) oheight = nc;
		if(nc > 0){
			level++;
			if(level > olevels) olevels = level;
			for (int ii = 0; ii < nc; ii++)
				oset(ond.get(ii), level);
		}
	}

	private OutNode hit(int X, int Y){
		
		oset(false);
		int xlevel = w / ww;
		xlevel = X / ww;
		int ylevel = (Y - oh) / hh;
		OutNode cur = nodes;
		for (int ii = 0; ii < xlevel; ii++){
			if(ii < cursor.length){
				cur = cur.get(cursor[ii]);
			}
		}
		hilevel = xlevel;
		if(ylevel >= cur.size()) return null;
		cur = cur.get(ylevel);
		return cur;
	}

	public void extendCur(){
		while(hitCursor.ref == null){
			if(hitCursor.size() == 0) return;
			hitCursor = hitCursor.get(0);
			hilevel++;
		}
	}

	class OutNode {
		public OutNode parent;
		LinkedList children = new LinkedList();
		public String desc;
		public String ref;
		public OutNode(OutNode parent, String desc, String ref){
			this.parent = parent;
			this.desc = desc;
			this.ref = ref;
		}
		public String getXML(){
			String ans = "<title title=\"" + desc + "\" page=\"" + ref + "\">";
			if(this == nodes) ans = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>";
			for (int ii = 0; ii < children.size(); ii++){
				OutNode child = (OutNode) children.get(ii);
				ans = ans + child.getXML();
			}
			if(this == nodes) ans = ans + "</root></xml>";
			else ans = ans + "</title>\n";
			return ans;
		} 
		public void add(OutNode child){
			children.add(child);
			child.parent = this;
		}
		public void add(int ii, OutNode child){
			children.add(ii, child);
			child.parent = this;
		}
		public void removeChild(OutNode ond){
			int ii = 0;
			for (ii = 0; ii < children.size(); ii++)
				if(ond == (OutNode) children.get(ii)) break;
			if(ii < children.size()) children.remove(ii);
		}
		public void flatten(LinkedList pp, LinkedList nn){
			boolean added = false;
			if(ref != null){
				int i = jarn.jrnlPane.pages.getPage(ref);
				if(i >= 0){
					pp.add(new Integer(i));
					nn.add(this);
					added = true;
				}
			}
			if(!added && (this == hitCursor)){
				pp.add(new Integer(100000));
				nn.add(this);
			}
			for(int ii = 0; ii < children.size(); ii++) get(ii).flatten(pp, nn);
		}
		public void delete(){
			parent.removeChild(this);
		}
		public void addSub(String desc, String pageref){
			OutNode newSub = new OutNode(this, desc, pageref);
			add(newSub);
		}
		public void addAfter(String desc, String pageref){
			OutNode newSub = new OutNode(parent, desc, pageref);
			parent.addAfter(this, newSub);
		}
		public boolean addBefore(OutNode target, OutNode source){
			OutNode par = target;
			while(par != null){
				if(source == par) return false;
				par = par.parent;
			}
			int ii = 0;
			for(ii = 0; ii  < children.size(); ii++)
				if(target == (OutNode) children.get(ii)) break;
			if(ii < children.size()) {
				children.add(ii, source);
				source.parent = this;
				return true;
			}
			return false;
		}
		public void remove(OutNode target){
			int ii = 0;
			for(ii = 0; ii  < children.size(); ii++)
				if(target == (OutNode) children.get(ii)) break;
			if(ii < children.size()) children.remove(ii);
		}
		public void addAfter(OutNode ond, OutNode newNode){
			int ii = 0;
			for (ii = 0; ii < children.size(); ii++)
				if(ond == (OutNode) children.get(ii)) break;
			if(ii < children.size()) {
				children.add(ii + 1, newNode);
				newNode.parent = this;
			}
		}	

		public int insertAfter = -1;
		public void addXML(String xml, int child){
			int ptr = 0;
			int state = 0;
			String title = "";
			String pageref ="";
			LinkedList stack = new LinkedList();
			stack.add(this);
			OutNode top = this;
			insertAfter = child;
			while(ptr >= 0){
				ptr = xml.indexOf("<", ptr);
				if(ptr < 0) return;
				int tagend = xml.indexOf(">", ptr);
				if(tagend < 0) return;
				String tag = xml.substring(ptr, tagend);
				ptr = tagend;
				if(tag.equals("</root")) return;
				if(tag.equals("</title")) {
					stack.removeLast();
					top = (OutNode) stack.getLast();
				}
				if(tag.startsWith("<title")){
					pageref = null;
					String pag = "page=\"";
					int t = tag.indexOf(pag);
					if(t >= 0){
						int tend = tag.indexOf("\"", t + pag.length());
						if(tend >= 0) {
							pageref = tag.substring(t + pag.length(), tend);
							if(!pageref.startsWith("pageref")){
								try{
									int iii = Integer.parseInt(pageref);
									pageref = jarn.jrnlPane.pages.getPageRef(iii - 1, bghandle);
								}
								catch(Exception ex){}
							}
							if(!pageref.startsWith("pageref")) pageref = null;
						}
					}
					title = "";
					String titl = "title=\"";
					t = tag.indexOf(titl);
					if(t >=0){
						int tend = tag.indexOf("\"", t + titl.length());
						if(tend >= 0) {
							title = tag.substring(t + titl.length(), tend);
							if(title.trim().equals("")) title = "[No Title]";
							OutNode nnode = new OutNode(top, title, pageref);
							top.insertAfter++;
							top.add(top.insertAfter, nnode);
							if(!tag.endsWith("/")) {
								stack.add(nnode);
								top = nnode;
							}
						}
					}
				}
			}
		}
				

		public int setCursor(OutNode ond, int[] backcur, int il){
			int ii = 0;
			for (ii = 0; ii < children.size(); ii++){
				if(ond == (OutNode) children.get(ii)) break;
			}
			if(ii < children.size()) {
				backcur[il] = ii;
				if(parent == null) return il;
				return parent.setCursor(this, backcur, il + 1);
			}
			return 0;	
		}			
			
		public int setcursor(){
			for(int ii = 0; ii < cursor.length; ii++) cursor[ii] = 0;
			int backcur[] = new int[cursor.length];
			if(parent == null) return 0;
			int il = parent.setCursor(this, backcur, 0);
			for(int ii = 0; ii <= il; ii++) 
				cursor[ii] = backcur[il - ii];
			return il;
		}
		public int size(){
			return children.size();
		}
		public void println(){System.out.println(desc);}
		public OutNode get(int i){
			if(i >= children.size()) return null;
			if(i < 0) return null;
			return (OutNode) children.get(i);
		}
		public void draw(Graphics2D g2, FontMetrics fm, int x, int y, int width, int level, boolean hi){
			if(level == 0){
				Rectangle2D r = fm.getStringBounds(desc, g2);
				int w = (int) r.getWidth();
				x = x + width - w;
			}
			g2.setColor(Color.lightGray);
			if(bgcolor.equals("green")) g2.setColor(Color.gray);
			if(hi) g2.setColor(Color.white);
			if(this == dragCursor) {
				g2.setColor(Color.red);
				if(bgcolor.equals("red")) g2.setColor(Color.blue);
			}
			if(dragop && (this == hitCursor)) g2.setColor(Color.black);
			g2.drawString(desc, x, y);
		}			
	}

 	private class OutMouseMotionListener extends MouseMotionAdapter {

        	public void mouseDragged(MouseEvent e) {
			dragop = true;
            		int X = e.getX();
	    		int Y = e.getY();
			dragCursor = hit(X,Y); 
			op.repaint();   
        	}
	}

	class OutMouseListener extends MouseAdapter {
	
        	public void mousePressed(MouseEvent e) {
			int X = e.getX();
			int Y = e.getY();
			hitCursor = hit(X, Y);
			if(hitCursor != null) hitCursor.println();
			if(e.isPopupTrigger()){
				hitCursor.setcursor();
				op.repaint();
				popm.getPopupMenu().show(op, X, Y);
				return;
			}
        	}


		public void mouseClicked(MouseEvent e){
			dragop = false;
			if((!e.isPopupTrigger()) && (hitCursor != null)) {
				extendCur();
				hitCursor.setcursor();
				String test = hitCursor.ref;
				if(test != null){
					if(test.startsWith("pageref") && synchPages) jarn.jrnlPane.doAction("Z" + test);
				}
			}			
			op.repaint();
		}

		public void mouseReleased(MouseEvent e){
			dragop = false;
			if((dragCursor != null) && (hitCursor != null)){
				jarn.dirty = true;
				String oldXML = getXML();
				jarn.jrnlPane.setStatus("");
				hitCursor.parent.remove(hitCursor);
				if(!dragCursor.parent.addBefore(dragCursor, hitCursor)){
					setEntireOutline(oldXML);
				}
				else jarn.jrnlPane.pages.setOutlineUndo(oldXML);
				synchPage(1);
			}
			dragCursor = null;
			op.repaint();
		}
    	}

	private class OutSizeListener implements ComponentListener{
		public void componentResized(ComponentEvent e){
			jarn.outHeight = jarn.jrnlPane.tpanel.getDividerLocation();
		}
		public void componentMoved(ComponentEvent e){
		}
		public void componentShown(ComponentEvent e){
		}
		public void componentHidden(ComponentEvent e){
		}
    	}

	private class OutActionListener implements ActionListener {
		private String action;
        	private JButton button;
		private String oldXML;
		private void setUndo(){
			jarn.dirty = true;
			jarn.jrnlPane.pages.setOutlineUndo(oldXML);
			jarn.jrnlPane.setStatus("");
		}

		private void setAction(String action){
			this.action = action;
			if(action.equals("Add Page As Subhead")) this.action = "Add Subhead With Page";
			if(action.equals("Add Page After")) this.action = "Add After With Page";
		}
        	public OutActionListener(String action, JButton button){
            		setAction(action);
            		this.button = button;
        	}
        
        	public OutActionListener(String action){
            		setAction(action);
            		button = null;
        	}
        
        	public void actionPerformed(ActionEvent e){
			if(action.equals("Synchronize Pages")){
				synchPages = !synchPages;
				spa.setState(synchPages);
				synchPage(1);
			}
			if(action.equals("blue") || action.equals("red") || action.equals(
"green")){
				bgcolor = action;
				op.repaint();
				return;
			}
			if(action.equals("Delete")){
				if(hitCursor == null) return;
				oldXML = getXML();
				hitCursor.delete();
				setUndo();
			}
			if(action.equals("Edit")){
				if(hitCursor == null) return;
				String ans = (new Jarnbox(jarn.gJrnlFrame, action, jarn, true)).getString(hitCursor.desc);
				if(ans != null){
					oldXML = getXML();
					hitCursor.desc = ans;
					setUndo();
				}
			}
			if(action.equals("Add Page")){
				if(hitCursor == null) return;
				oldXML = getXML();
				hitCursor.ref = jarn.jrnlPane.pages.getPageRef();
				setUndo();
				return;
			}
			if(action.equals("Remove Page")){
				if(hitCursor == null) return;
				oldXML = getXML();
				hitCursor.ref = null;
				setUndo();
				return;
			}			
			if(action.startsWith("Add")){
				String ans = (new Jarnbox(jarn.gJrnlFrame, action, jarn, true)).getString("");
				if(ans == null) return;
				if(action.startsWith("Add Subhead")) {
					if(hitCursor == null) return;
					oldXML = getXML();
					String pageref = null;
					if(action.equals("Add Subhead With Page"))
						pageref = jarn.jrnlPane.pages.getPageRef();
					hitCursor.addSub(ans, pageref);
					setUndo();
				}
				if(action.startsWith("Add After")) {
					if(hitCursor == null) return;
					oldXML = getXML();
					String pageref = null;
					if(action.equals("Add After With Page"))
						pageref = jarn.jrnlPane.pages.getPageRef();
					hitCursor.addAfter(ans, pageref);
					setUndo();
				}
			}
			op.repaint();
		}
	}	

	class OutPane extends JPanel{

		public void paintComponent(Graphics g){
			setBackground(Color.white);
			super.paintComponent(g);	
			Graphics2D g2 = (Graphics2D) g;
			oset(false);
			if(olevels == 0) return;
			int fs = hh;
			if(fs > 24) fs = 24;
			g2.setFont(new Font("Arial", Font.BOLD, fs));
			FontMetrics fm = g2.getFontMetrics();
			OutNode cur = nodes;
			int nlev = olevels;
			if(nlev == 1) {
				nlev = 2;
				ww = ww /2;
			}
			for(int ii = 0; ii < nlev; ii++){
				float test = 1.0f - ((float)ii * (0.5f/((float)(nlev - 1.0f))));
				g2.setColor(Color.blue);
				if(bgcolor.equals("red")) g2.setColor(Color.red);
				if(bgcolor.equals("green")) g2.setColor(Color.green);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, test));
				g2.fillRect(ii * ww, 0, ww, h);
				g2.setComposite(AlphaComposite.SrcOver);
				if((olevels == 1) && (ii == 1)) return;
				if(cur != null){
					int kk = cursor[ii];
					boolean hi = false;		
					for(int jj = 0; jj < cur.size(); jj++){
						if(jj == kk) hi = true;
						else hi = false;
						if(ii > hilevel) hi = false;
						cur.get(jj).draw(g2, fm, www + (ii * ww), ((jj + 1) * hh) + oh, ww - (2 * www), ii, hi);
					}
					cur = cur.get(kk);
				}
			}
			if(g2 != null) g2.dispose();						
		}

	}
}
