package jarnal;

import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;

public class Selfexec {

	static String jhome;
	static String jexec;
	static String uhome;
	static String udir;
	static String osname;
	static String osversion;
	static String url;
	static String appname = "jarnal";

	public static void main(String[] args) {
		selfexec("");
	}

	public static void inputToOutput(InputStream is, OutputStream os){
		try{
			int nmin = 1000000;
			int nborg = 40000;
			int nmax = nmin + ( 5 * nborg); 
			byte b[] = new byte[nmax];
			int nread = 0;
			int noff = 0;
			while((nread = is.read(b, noff, nborg)) >= 0){
				noff = noff + nread;
				if(noff > nmax - (2 * nborg)){
					os.write(b, 0, noff);
					noff = 0;
				}
			}
			os.write(b, 0, noff);
		}
		catch(Exception ex){ex.printStackTrace();}
	}

	public static void initself(){
		jhome = System.getProperty("java.home");
		jexec = jhome + File.separator + "bin" + File.separator + "java -Xmx192m -jar ";
		uhome = System.getProperty("user.home");
		udir = System.getProperty("user.dir");
		osname = System.getProperty("os.name");
		osversion = System.getProperty("os.version");
      		url = (Selfexec.class.getClassLoader().getSystemResource(appname + "/Selfexec.class")).toString();
		int n = url.indexOf("file:");
		url = url.substring(n + 5);
		n = url.indexOf("!/");
		if(n > 0) url = url.substring(0, n);
	}

	public static JarEntry jarDir(String dirname){
		JarEntry subname = new JarEntry(dirname + "/");
		subname.setMethod(ZipEntry.STORED);
		subname.setSize(0l);
		subname.setCrc(0l);
		subname.setCompressedSize(0l);
		return subname;
	}

	public static void pack(String app, String cwd, String fname){
		initself();
		//String jarurl = "/docroot/docs/web/general/software/tc1000/jarnal.jar";
		try{
			FileOutputStream out = new FileOutputStream(cwd + File.separator + fname + ".jar");
			JarOutputStream zip = new JarOutputStream(out);
			JarEntry subname = new JarEntry("files.txt");
			zip.putNextEntry(subname);
			zip.write((app + "\n" + fname).getBytes());
			subname = jarDir(appname);
			zip.putNextEntry(subname);
			subname = new JarEntry(appname + "/Selfexec.class");
			zip.putNextEntry(subname);
			//InputStream is = new FileInputStream(url);
			InputStream is = (Selfexec.class.getClassLoader().getSystemResource(appname + "/Selfexec.class")).openStream();
			inputToOutput(is, zip);
			subname = new JarEntry(app);
			zip.putNextEntry(subname);
			is = new FileInputStream(url);
			inputToOutput(is, zip);
			subname = new JarEntry(fname);
			zip.putNextEntry(subname);
			is = new FileInputStream(cwd + File.separator + fname);
			inputToOutput(is, zip);
			subname = jarDir("META-INF");
			zip.putNextEntry(subname);
			subname = new JarEntry("META-INF/MANIFEST.MF");
			zip.putNextEntry(subname);	
			zip.write(("Main-Class: " + appname + "/Selfexec\n").getBytes());		
			zip.close();
		}
		catch(Exception ex){ex.printStackTrace();}
	}

	public static void selfexec(String est){
		try{
			initself();	
			FileInputStream fis = new FileInputStream(url);
			ZipInputStream zip = new ZipInputStream(fis);
			String app = null;
			String fname = null;
			for(ZipEntry ze = zip.getNextEntry(); ze != null; ze = zip.getNextEntry()){
				if(ze.getName().equals("files.txt")){
					ByteArrayOutputStream baost = new ByteArrayOutputStream();
					inputToOutput(zip, baost);
					byte c[] = baost.toByteArray();
					String instr = new String(c);
					int n = instr.indexOf("\n");
					app = instr.substring(0, n);
					app.trim();
					fname = instr.substring(n + 1);
					fname.trim();	
				}
				if(ze.getName().equals(app)){
					FileOutputStream fos = new FileOutputStream(uhome + File.separator + app);
					inputToOutput(zip, fos);
				}
				if(ze.getName().equals(fname)){
					FileOutputStream fos = new FileOutputStream(uhome + File.separator + fname);
					inputToOutput(zip, fos);
				}
			}		
			Runtime rt = Runtime.getRuntime();
			est = jexec + uhome + File.separator + app + " " + uhome + File.separator + fname;
			System.out.println(est);
			Process ps = rt.exec(est);
			//InputStream is = ps.getInputStream();
			//InputStreamReader isr = new InputStreamReader(is);
            		//BufferedReader br = new BufferedReader(isr);
			//String line;
			//while ((line = br.readLine()) != null) System.out.println(line);
			//ps.waitFor();
		}
		catch(Exception ex){ex.printStackTrace();}
	    }
}
