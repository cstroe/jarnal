package jarnal;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;

import jarnal.Jarnal;


public class Jarnscr extends JPanel implements Runnable{

	JFrame frame = new JFrame();
	Jarnal.JrnlPane jp = null;
	BufferedImage scr = null;
	BufferedImage ans = null;
	boolean image = true;
	boolean entireScreen = false;
	int startX;
	int startY;
	int X;
	int Y;
	int x[] = new int[5];
	int y[] = new int[5];

	public Jarnscr(Jarnal.JrnlPane jp, boolean image, boolean entireScreen){
		frame.addMouseListener(new scrMouseListener(frame, this));
		frame.addMouseMotionListener(new scrMouseMotionListener(this));
		frame.setUndecorated(true);
		frame.getContentPane().add(this);
		this.jp =jp;
		this.image = image;
		this.entireScreen = entireScreen;
	}

	//putting this on a separate thread causes the menu to disappear before the screenshot

		
	public void run(){
		try{
			Thread.yield();
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.sync();
			Rectangle r = new Rectangle(tk.getScreenSize());
			Robot robot = new Robot();
			scr = robot.createScreenCapture(r);
		}
		catch(Exception ex){System.out.println(ex); scr = null;}
		if(entireScreen){
			jp.scr = scr;
			if(image) jp.doAction("xxImage for Insertion");
			else jp.doAction("xxBackground for Insertion");
			return;
		}
		GraphicsEnvironment.getLocalGraphicsEnvironment().
			getDefaultScreenDevice().setFullScreenWindow(frame);
		repaint(1l);
	}

	public void paintComponent(Graphics g){
		if(g == null) return;
	    	setBackground(Color.white);
	    	super.paintComponent(g);
		if(scr == null) return;	
            	Graphics2D g2 = (Graphics2D) g;
		if(ans == null){
			Rectangle r = g.getClipBounds();
			ans = (BufferedImage) createImage(r.width, r.height);
		}
		Graphics2D a2 = ans.createGraphics();
		a2.drawImage(scr, 0, 0, this);
		x[0] = startX;
		y[0] = startY;
		x[1] = startX;
		y[1] = Y;
		x[2] = X;
		y[2] = Y;
		x[3] = X;
		y[3] = startY;
		x[4] = x[0];
		y[4] = y[0];
		a2.setPaint(Color.red);
		a2.setStroke(new BasicStroke(0.5f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		a2.drawPolyline(x, y, 5);
		g2.drawImage(ans, 0, 0, this);
		g2.dispose();
		a2.dispose();
	}
}

class scrMouseListener extends MouseAdapter {

	JFrame f;
	Jarnscr j;

	public scrMouseListener(JFrame frame, Jarnscr js){
		f = frame;
		j = js;
	}

	private void doneclose(){
		GraphicsEnvironment.getLocalGraphicsEnvironment().
			getDefaultScreenDevice().setFullScreenWindow(null);
		f.setVisible(false);
	}
	
        public void mousePressed(MouseEvent e) {
            j.startX = e.getX();
	    j.startY = e.getY();
	    j.X = j.startX;
	    j.Y = j.startY;
	}

	public void mouseClicked(MouseEvent e){
		doneclose();    
	}

	public void mouseReleased(MouseEvent e){
		doneclose();
		int x = j.startX;
		int y = j.startY;
		int w = j.X - j.startX;
		int h = j.Y - j.startY;
		if(x > j.X){
			x = j.X;
			w = -w;
		}
		if(y > j.Y){
			y = j.Y;
			h = -h;
		}
		if((w <= 0) || (h <= 0)) return;
		j.scr = j.scr.getSubimage(x, y, w, h);
		if(j.scr != null){
			j.jp.scr = j.scr;	
			if(j.image)j.jp.doAction("xxImage for Insertion");
			else j.jp.doAction("xxBackground for Insertion");
		}		
	}
}
	
class scrMouseMotionListener extends MouseMotionAdapter {

	Jarnscr j;

	public scrMouseMotionListener(Jarnscr js){
		j = js;
	}

        public void mouseDragged(MouseEvent e) {
	    j.X = e.getX();
	    j.Y = e.getY();
	    j.repaint(1l);
	}
}
