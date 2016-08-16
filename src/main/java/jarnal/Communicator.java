package jarnal;

import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.*;

// each client is connected to the server by two pipes
// one pipe sends data and one recieves data
// acks are not sent when data is received
// the Jcom parent class has a thread for sending data
// the Jclient class has a thread for reading incoming data
// for the most part only one "active" client machine can actually send data
// the server rebroadcasts this data to all the inactive clients

public class Communicator extends Thread {

	// handles the sending queue
	// a client jc and a server js
	// sends are placed in the queue
	// and handed off to jc by the run() method for sending
	// or to the js for broadcasting
	LinkedList queue = new LinkedList();
	JarnalClient jc = null;
	public JarnalServer js = null;
	public boolean active = false;
	public boolean requestactive = false;
	public boolean listen = true;

	public void disconnect(){
		if(js != null) {
			js.listen = false;
			js.closeSock();
		}
		if(jc != null) {
			jc.eof = true;
			jc.closeSock();
		}
		listen = false;
	}

	public synchronized void requestactive(){
		requestactive = true;
		request("requestactive");
	}

	public synchronized void requestinactive(){
		requestactive = false;
		active = false;
		if(js != null) js.unfreeze();
		request("testactive");
	}

	// the request method puts a string in the sending queue
	public synchronized void request(String str){
		UndoPage up = new UndoPage();
		up.op = str;
		queue.add(up);
		notifyAll();
	}		

	// the send method puts an undo page in the sending queue
	public synchronized void send(UndoPage up){
		if(!active) {
			System.err.println("Error: Inactive client attempting to send undopage.");
			return;
		}
		queue.add(up);
		notifyAll();
	}

	// sending an undo page when not active
	public synchronized void forcesend(UndoPage up){
		queue.add(up);
		notifyAll();
	}

	// broadcasts only happen if we are the server
	public synchronized void broadcast(UndoPage up){
		if(js != null){
			queue.add(up);
			notifyAll();
		}
	}

	// this is the threading that keeps clearing the queue
	public synchronized UndoPage get(){
		while(queue.size() == 0) {
			try {wait();} 
			catch (InterruptedException ie) {}
		}
		return (UndoPage) queue.remove(0);
	}

	// the run() method is the thread loop
	public void run() {
		while(listen){
			UndoPage up = get();
			// you should be the server or the client, but not both
			if(jc != null) {
				if(up.op.equals("requestactive")) jc.send("requestactive");
				else { 
					if(up.op.equals("testactive")) jc.send("requestinactive");
					else jc.send(up);
				}
			}
			if(js != null) {
				if(up.op.equals("requestactive") || up.op.equals("testactive")) js.testActive();
				else {
					if(up.op.equals("putfile")) js.putfile((JarnalClient) up.data);
					else js.broadcast(up);
				}
			}
		}
	}
}

class JarnalClient extends Thread {
	InputStream is;
	public OutputStream os;
	Socket sock = null;
	boolean eof = false;
	static String terminator = "<<?rope termination 876&*()_+..>";
	static int nb = terminator.length();
	byte bb[] = (terminator + terminator).getBytes();
	int nn = 0;
	Jarnal jarn;
	Communicator communicator;
	String server = "localhost";
	public boolean active = false;
	public boolean requestactive = false;
	public boolean modify = false;
	public boolean ready = false;

	public void setJarnal(Jarnal jarn){
		this.jarn = jarn;
		jarn.jrnlPane.pages.communicator = communicator;
	}

	// creating the client also tries to connect to the server
	public JarnalClient(Socket sock, Jarnal jarn, String server, Communicator parent){
		this.jarn = jarn;
		this.server = server;
		if(parent == null){
			communicator = new Communicator();
			communicator.jc = this;
			jarn.jrnlPane.pages.communicator = communicator;
		}
		else communicator = parent;
		if(sock == null){
			int port = jarn.defaultServerPort;
			int n = server.indexOf(":");
			if(n >= 0){
				port = Integer.parseInt(server.substring(n + 1, server.length()));
				server = server.substring(0, n);
			}
			try{
				sock = new Socket(server, port);
			}
			catch(Exception ex){System.err.println("Error getting socket: " + ex); sock = null;}
		}
		this.sock = sock;
		if(sock != null){
			try{
				is = sock.getInputStream();
				os = sock.getOutputStream();
			}
			catch(IOException iox){System.err.println("Error getting stream: " + iox); sock = null; is = null; os = null;}
		}
	}

	// getFile() retrieves the entire file from the server when communications are initiated
	// first it write a getfile request on the output stream
	// then it reads the result from the input stream and saves it into a temporary file fname
	// this of course won't work with the applet
	// I think this gets called directly from outside of the jcom classes 
	public File getFile(){
		File fname = null;
		if(os == null) return fname;
		System.out.println("Getting file");
		try{
			os.write(("getfile" + terminator).getBytes());
			String ffname = readString();
			fname = File.createTempFile(ffname + "." + Jarnal.getInstance().jarnalTmp, ".jaj");
			FileOutputStream out = new FileOutputStream(fname);
			byte[] ba = read();
			out.write(ba, 0, ba.length - terminator.length());
			out.close();
		}
		catch(Exception ex){System.err.println("Problem getting file: " + ex);}
		return fname;
	}

	// starts the parent thread handling the sending queue
	public void startCom(){
		communicator.start();
	}

	// formats the undo as a long string suitable for sending and sends it
	public synchronized void send(UndoPage up){
		if(up.op.equals("undo")){
			send("undo");
			return;
		}
		if(up.op.equals("redo")){
			send("redo");
			return;
		}
		String str = "command" + terminator + up.op + terminator;
		str = str + up.cindex + terminator + up.oindex + terminator;
		if(up.top) str = str + "top=true" + terminator;
		else str = str + "top=false" + terminator;
		send(str + (String) up.data);
		yield();
	}

	// this is where the client actually sends data
	// data is a string by a "terminator"
	// note that sending is "blind" we don't wait for or expect an ack
	public synchronized void send(String str){
		if(os == null) return;
		try{
			os.write((str + terminator).getBytes());
		}
		catch(Exception ex){System.err.println("Send error: " + ex);}
		yield();
	}

	// logic for locating terminator strings
	private int find(byte[] bb, int nn, byte cc[]){
		int found = -1;
		int pos = 0;
		while((found == -1) && (pos <= (nn - cc.length))){
			if(bb[pos] == cc[0]){
				found = pos;
				for(int ii=1; ii < cc.length; ii++){
					if(bb[pos + ii] != cc[ii]) {
						found = -1;
						break;
					}
				}
			}
			else found = -1;
			if(found == -1) pos++;
		}
		return found;
	}
					
	//the read method finds strings in the input stream terminated by the "terminator"
	private byte[] read(){
		boolean eor = false;
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		while(!eor){
			try{
				int n = is.read(bb, nn, (2 * nb) - nn);
				if(n == -1) {System.err.println("Error: premature end of stream"); return null;}
				nn = nn + n;
				if(nn >= nb){
					n = find(bb, nn, terminator.getBytes());
					int nw = nb;
					if (n >= 0){
						eor = true;
						nw = nb + n;
					}	
					if (eor || (nn == (2 * nb))){
						ba.write(bb, 0, nw);
						nn = nn - nw;
						for (int i = 0; i < nn; i++) bb[i] = bb[nw + i];
					}
				}					
			}
			catch(IOException iox){System.err.println("read error " + iox); return null;}
		}
		return ba.toByteArray();
	}

	private String readString(){
		byte b[] = read();
		String ans = null;
		if(b != null) ans = new String(b, 0, b.length - terminator.length());
		return ans;
	}

	void endmodify(){
		modify = false;
		if(communicator.js != null){
			communicator.js.unfreeze();
		}
	}

	public synchronized void unfreeze(){
		notifyAll();
	}

	public void closeSock(){
		if(sock == null) return;
		try{
			sock.close();
		}
		catch(IOException iox){System.err.println(iox);}
		sock = null;
	}

	public synchronized void flipTo(String line){
		int zzz = Integer.parseInt(line.substring(6));
		//System.out.println(zzz);
		jarn.jrnlPane.absPage(zzz);
		return;
	}

	// docommand() processes the strings recieved by the read() method
	// the most important case where the command is "command"
	// in this case the read() method reads another string which contains an
	// undo structure
	// the undo is decoded by jpages with getdo() and passed to the redo stack 
	public synchronized void docommand(String line){
		System.out.println("docommand: " + line.substring(0, line.length() - terminator.length()));

		if(line.equals("requestactive" + terminator) && (communicator.js != null)){
			requestactive = true;
			communicator.request("testactive");
			return;
		}

		if(line.equals("youareactive" + terminator)){
			communicator.requestactive = false;
			communicator.active = true;
			jarn.jrnlPane.setStop();
			jarn.jrnlPane.pages.setSynch(readString());			
			return;
		}

		if(line.equals("youareactivewithwarning" + terminator)){
			communicator.requestactive = false;
			communicator.active = true;
			jarn.jrnlPane.setStop();
			jarn.jrnlPane.setWarning();
			jarn.jrnlPane.pages.setSynch(readString());
			return;
		}

		if(line.equals("pendingrequests" + terminator)){
			jarn.jrnlPane.setWarning();
			return;
		}

		if(line.equals("requestinactive" + terminator) && (communicator.js != null)){
			requestactive = false;
			active = false;
			communicator.request("testactive");
			return;
		}

		// only the server is supposed to be asked to provide the file
		if(line.equals("getfile" + terminator)){
			if(communicator.js != null) communicator.js.getfile(this);
			else System.err.println("Error: client received getfile request");
		}
		else if(!active && (communicator.js != null)) {
			System.err.println("Error: received command from inactive client.");
			return;
		}

		if(communicator.js != null){
			while(communicator.js.freeze) {
				try {wait();} 
				catch (InterruptedException ie) {}
			}
		}

		if(line.equals("getfile" + terminator)) return;
		modify = true;

		// this next handles almost all the real communication
		if(line.equals("command" + terminator)){	
			UndoPage up = new UndoPage();
			up.op = readString();
			System.out.println("operation received: " + up.op);
			up.cindex = Integer.parseInt(readString());
			up.oindex = Integer.parseInt(readString());
			String top = readString();
			up.top = false;
			if(top.equals("top=true")) up.top = true;
			up.data = readString();	
			if(up.op.startsWith("flipTo")) {
				flipTo(up.op);
				endmodify();
				return;
			}
			if(up.op.equals("serverfullscreen")){
				jarn.jrnlPane.doAction("xFull Screen");
				endmodify();
				return;
			}
			if(up.op.equals("serverlockpage")){
				jarn.jrnlPane.pages.serverlockpage();
				endmodify();
				return;
			}
	
			communicator.broadcast(up.copy());
			jarn.jrnlPane.getdo(jarn.jrnlPane.pages.getdo(up));
			endmodify();
			return;
		}

		// watch for underflowing the undo or redo stacks
		// if this happens we have to disconnect
		if(line.equals("undo" + terminator)) {
			if(!jarn.jrnlPane.pages.checkundo(true)){
				closeSock();
				System.err.println("Error: undo stack underflow");
				return;
			}
			// note that for an undo (or redo) no data was just sent, we just use the
			// existing stack in jpages
			jarn.jrnlPane.getdo(jarn.jrnlPane.pages.undo());
			UndoPage up = new UndoPage();
			up.op = "undo";
			communicator.broadcast(up);
			endmodify();
			return;
		}

		if(line.equals("redo" + terminator)) {
			if(!jarn.jrnlPane.pages.checkundo(false)){
				closeSock();
				System.err.println("Error: redo stack underflow");
				return;
			}
			jarn.jrnlPane.getdo(jarn.jrnlPane.pages.redo());
			UndoPage up = new UndoPage();
			up.op = "redo";
			communicator.broadcast(up);
			endmodify();
			return;
		}

		endmodify();
		System.err.println("Error: unknown docommand " + line.substring(0, line.length() - terminator.length()));
	}	
	
	// keep reading strings and passing them to docommand
	// the Jcom parent thread takes sends off the queue and passes
	// them to the client or server for sending
	public void run(){
		ready = true;
		eof = false;
		while(!eof){
			byte ba[] = read();
			if(ba == null) eof = true;
			if(!eof) docommand(new String(ba));
		}
		System.out.println("Client exiting");
		if(communicator.js != null) communicator.js.clientremove(this);
		if(communicator.jc != null) {
			communicator.listen = false;
			jarn.jrnlPane.doDisconnect();
		}
	}

}

// there is only one server which handles all the remote clients and connections
// it has a list jconns of all known connections
// each element of the list is a client that handles the server side communication
// with the client class running on the remote machine
// the server has to rebroadcast any data received from a client to the other clients
// it broadcasts by calling each client in jconns and having it send the message
// it handles requests to "become active" making sure that only one client is active at a time
// only the active client is allowed to send data
// except that any client can place a request to become active
class JarnalServer extends Thread {

	int port = 1189;
	ServerSocket sock = null;
	boolean listen = true;
	LinkedList jconns = new LinkedList();
	Jarnal jarn;
	Communicator jc;
	Random rr = new Random();
	public boolean freeze = false;

	public JarnalServer(Jarnal jarn, int port){
		jc = new Communicator();
		jc.js = this;
		this.jarn = jarn;
		this.port = port;
		jc.start();
		jarn.jrnlPane.pages.communicator = jc;
	}

	public void activeclientremove(){
		JarnalClient jcl = null;
		int m = -1;
		for (int ii = 0; ii < jconns.size(); ii++) {
			jcl = (JarnalClient) jconns.get(ii);
			if(jcl.active) m = ii;
		}
		if(m >= 0) {
			jcl.eof = true;
			jconns.remove(m);
			System.out.println("Removing active client");
		}
		System.out.println("Number of Clients: " + jconns.size());
		jc.request("testactive");
	}

	public void clientremove(JarnalClient jcl){
		jcl.eof = true;
		int m = -1;
		for (int ii = 0; ii < jconns.size(); ii++) if(jconns.get(ii) == jcl) m = ii;
		if(m >= 0) {
			jconns.remove(m);
			System.out.println("Removing client");
		}
		System.out.println("Number of Clients: " + jconns.size());
		jc.request("testactive");
	}	
	
	boolean anymodify(){
		boolean ans = jc.active;
		JarnalClient jcl;
		for (int ii = 0; ii < jconns.size(); ii++) {
			jcl = (JarnalClient) jconns.get(ii);
			if(jcl.ready) ans = ans || jcl.modify;
		}
		return ans;
	}

	public synchronized void unfreeze(){
		notifyAll();
	}

	public synchronized void getfile(JarnalClient jcl){
		freeze = true;
		if(jc.active) jarn.handButton.setIcon(Images.handyellow);
		if(anymodify()) {
			try {wait();} 
			catch (InterruptedException ie) {}
		}
		UndoPage up = new UndoPage();
		up.op = "putfile";
		up.data = jcl;
		jc.forcesend(up);
	}

	// this is the opposite of getfile
	// here the server sends the file
	// it has jpages save it to a byte array first
	// so that it gets a consistent copy of the file as it exists right now
	// all subsequent changes after the putfile will be broadcast to the new client
	public synchronized void putfile(JarnalClient jcl){
		System.out.println("Putting file");
		try{
			String fname = jarn.fname;
			if(fname.equals("")) fname = "new_file";
			jcl.os.write((fname + JarnalClient.terminator).getBytes());
			ByteArrayOutputStream bas = new ByteArrayOutputStream();
			jarn.pages().save(bas, jarn.getConf());
			jcl.os.write(bas.toByteArray());
			jcl.os.write(JarnalClient.terminator.getBytes());
		}
		catch(IOException iox){System.err.println("Error writing file to client: " + iox);}
		freeze = false;
		for (int ii = 0; ii < jconns.size(); ii++) {
			jcl = (JarnalClient) jconns.get(ii);
			jcl.unfreeze();
		}
	}

	private boolean activeRequests(){
		if(jc.requestactive) return true;
		JarnalClient jcl;
		for (int ii = 0; ii < jconns.size(); ii++) {
			jcl = (JarnalClient) jconns.get(ii);
			if(jcl.requestactive) return true;
		}
		return false;	
	}

	public void testActive(){
		if(jc.active) {
			jarn.jrnlPane.setWarning();
			return;
		}
		int ii;
		int n = jconns.size();
		JarnalClient jcl;
		for (ii = 0; ii < jconns.size(); ii++) {
			jcl = (JarnalClient) jconns.get(ii);
			if(jcl.active) {
				jcl.send("pendingrequests");
				return;
			}
		}
		for (ii = 0; ii < n; ii++) {
			jcl = (JarnalClient) jconns.get(ii);
			jcl.active = false;
		}
		if(freeze) return;
		if (jc.requestactive) {
			jc.active = true;
			jc.requestactive = false;
			jarn.jrnlPane.setStop();
			if(activeRequests()) jarn.jrnlPane.setWarning();
			return;
		}
		else jc.active = false;
		if(n == 0) return;
		int m = rr.nextInt(n);
		boolean found = false;
		ii = m;
		while (!found){
			jcl = (JarnalClient) jconns.get(ii);
			if (jcl.requestactive){
				jcl.active = true;
				jcl.requestactive = false;
				if(activeRequests())jcl.send("youareactivewithwarning");
				else jcl.send("youareactive");
				jcl.send("" + (new Date()).getTime());
				found = true;
			}
			else {
				ii++;
				if(ii >= n) ii = 0;
				if(ii == m) found = true;
			}
		}		
	}

	private void bindSock(){
		try{
			sock = new ServerSocket(port);
		}
		catch(IOException iox){
			System.err.println("Error cannot bind to port" + iox); 
			sock = null; 
			listen = false; 
			JOptionPane.showMessageDialog(null, "Cannot bind to port");
		}
		if (sock != null) jarn.serverMsg = "&nbsp;&nbsp;&nbsp;Server port: " + sock.getLocalPort();
	}

	private void readSock(Socket insock){
		if(insock == null) return;
		JarnalClient jcom = new JarnalClient(insock, jarn, null, jc);
		jconns.add(jcom);
		jcom.start();	
	}

	public void closeSock(){
		if(sock == null) return;
		try{
			sock.close();
		}
		catch(IOException iox){System.err.println(iox);}
		sock = null;
	}

	public void broadcast(UndoPage up){
		for(int i = 0; i < jconns.size(); i++){
			JarnalClient jb = (JarnalClient) jconns.get(i);
			if(!jb.active && jb.ready) {
System.out.println("Server sending to: " + i);
				jb.send(up);
			}
		}
	}		

	// the run method listens for incoming connections
	// when it gets a connection it calls readSock
	// to create a new client and add it to the jconns collection
	public void run(){
		System.out.println("Jarnal server is running");
		while(listen){
			if(sock != null){
				Socket insock = null;
				try{
					insock = sock.accept();
				}
				catch(Exception ex){
					System.err.println("Error: waiting for connection: " + ex); 
					closeSock();
				}
				readSock(insock);
			}
			else bindSock();
		}
		closeSock();
		JarnalClient jcl;
		int mm = jconns.size();
		for (int ii = 0; ii < mm; ii++) {
			jcl = (JarnalClient) jconns.get(0);
			jcl.eof = true;
			jconns.remove(0);
		}
		jc.listen = false;
		jarn.jrnlPane.doDisconnect();
	}

}
