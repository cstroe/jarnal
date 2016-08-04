package jarnal;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

public class SendMail {

	String server;
	String email;
	LinkedList emaillist = new LinkedList();
	String from;
	String subject;
	String message;
	String boundary = "Boundary_(QTt7kzZ2i19qxab8KNJL2w";
	String filetitle[];
	String filetype[];
	BufferedReader in;
	BufferedWriter out;
	boolean errorstate;
	byte bav[][];

	private void parseaddr(String email){
		int n = email.indexOf("<");
		if(n >= 0) email = email.substring(n + 1);
		n = email.indexOf(">");
		if(n >= 0) email = email.substring(0, n);
		email = email.trim();
		n = email.indexOf("@");
		if(n >= 0) emaillist.add(email);
	}

	private void parsesend(String email){
		int n = email.indexOf(",");
		if(n < 0) parseaddr(email);
		else {
			parseaddr(email.substring(0, n));
			parsesend(email.substring(n + 1));
		}
	}

	public LinkedList mxlist(String servername){
		LinkedList ans = new LinkedList();
		LinkedList prios = new LinkedList();
		try{
			DirContext dc = new InitialDirContext();
			Attributes a = dc.getAttributes("dns:/" + servername, new String[] {"MX"});
			NamingEnumeration enumx = a.getAll();
			while(enumx.hasMore()) {
				String mxrec = enumx.next().toString();
				String recs[] = mxrec.split(",");
				for(int ii = 0; ii < recs.length; ii++) {
					mxrec = recs[ii].trim();
					int n = mxrec.lastIndexOf(" ");
					if(n >= 0){
						String prio = mxrec.substring(0, n).trim();
						int m = prio.lastIndexOf(" ");
						if(m >= 0) prio = prio.substring(m).trim();
						m = Integer.parseInt(prio);
						Integer mm = new Integer(m);
						mxrec = mxrec.substring(n + 1);
						int ml = mxrec.length();
						String q = mxrec.substring(ml - 1, ml);
						if(q.equals(".")) mxrec = mxrec.substring(0, ml - 1);
						else mxrec = servername + "." + mxrec;
						int iii = 0;
						for(iii = 0; iii < prios.size(); iii++){
							Integer jj = (Integer) prios.get(iii);
							int j = jj.intValue();
							if(m < j) break;
						}
						ans.add(iii, mxrec);
						prios.add(iii, mm);
					}
				}
			}
		}
		catch(Exception ex){ex.printStackTrace();}
		//for(int ii = 0; ii < ans.size(); ii++) System.out.println(ans.get(ii));
		return ans;
	}
		

	public String sendmail(String server, String from, String email, String subject, String message, String sendname[], String filetype[], byte[][] bav) {
		parsesend(email);
		if(emaillist.size() == 0) return("No Valid Recipients Found In List: " + email);
		this.subject = subject;
		this.message = message;
		this.filetitle = sendname;
		this.filetype = filetype;
		this.bav = bav;
		if(from != null){
			int n = from.indexOf("@");
			if(n < 0) from = null;
		}
		this.from = from;
		if(from == null){
			this.from = (String) emaillist.get(0);
		}
		for(int ii = 0; ii < emaillist.size(); ii++){
			this.email = (String) emaillist.get(ii);
			if(server == null){
				int n = this.email.indexOf("@");
				LinkedList mlist = mxlist(this.email.substring(n+1));
				for(int jj = 0; jj < mlist.size(); jj++){
					server = (String) mlist.get(jj);
					System.out.println("Trying " + server);
					sendmail();
					if(!errorstate) break;
					System.out.println("Failed");
				}
			}
			else sendmail();
		}
		if(errorstate) return "Failed to send mail";
		return null;
	}

	private void sendheaders(){
		send("Subject: " + subject);
		send("From: " + from);
		send("Errors-To: " + from);
		send("MIME-version: 1.0");
		send("X-Mailer: Jarnal");
		send("Content-type: MULTIPART/MIXED;");
		send(" BOUNDARY=\"" + boundary + "\"");
	}

	private void sendmessage(){
		send("--" + boundary);
		send("Content-type: text/plain; charset=\"charset=iso-8859-1\"");
		send("Content-transfer-encoding: 7bit");
		send("");
		send(message);
		send("\n");
	}

	private void sendattach(){
		for(int ii = 0; ii < bav.length; ii++){
			sendattach(ii);
			send("--" + boundary + "--");
		}
	}

	private void sendattach(int ii){
		send("--" + boundary);
		send("Content-type: application/" + filetype[ii] + "; name=\"" + filetitle[ii] + "\"");
		send("Content-disposition: attachment; filename=\""  + filetitle[ii] + "\"");
		send("Content-transfer-encoding: base64");
		send("");
		send(base64.encode(bav[ii]));
		send("\n");
	}

	private void sendmail(){
		errorstate = false;
		try {
			Socket s = new Socket(server, 25);
			in = new BufferedReader(new InputStreamReader(s.getInputStream(), "8859_1"));
 			out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "8859_1"));
			boundary = boundary + System.currentTimeMillis() + ")";
			sendline("HELO there");
			sendline("MAIL FROM: <" + from + ">");
			sendline("RCPT TO: " + email);
			sendline("DATA");
			sendheaders();
			send("\n");
			sendmessage();
			sendattach();
			send("\n.\n");
			sendline("QUIT");
			s.close();
		}
		catch (Exception e) {e.printStackTrace(); errorstate = true;}
	}

	public void sendline(String s) {
		try {
			out.write(s + "\n");
			out.flush();
			s = in.readLine();
		}
		catch (Exception e) {e.printStackTrace();}
	}

	public void send(String s) {
		try {
			out.write(s + "\n");
			out.flush();
		}
		catch (Exception e) {e.printStackTrace();}
	}
}

class base64 {
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
		nl = -1;
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
}

