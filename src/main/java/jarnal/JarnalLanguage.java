package jarnal;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Locale;

public class JarnalLanguage {
    private Hashtable hlang = null;
    private String language = Locale.getDefault().toString().substring(0, 2);

    public JarnalLanguage(String language){
        hlang = null;

        InputStream in = Jarnal.class.getResourceAsStream("languages/" + language + ".txt");
        if (in == null) return;

        String s;
        try {
            s = new String(Pages.streamToByteArray(in));
        } catch (Exception ex) {
            System.err.println(ex);
            s = "";
        }
        if (s == null) return;

        hlang = new Hashtable();
        s = Tools.replaceAll(s, "\r\n", "\n");
        s = Tools.replaceAll(s, "\r", "\n");

        boolean done = false;
        int pos = 0;
        while (!done) {
            pos = s.indexOf("\n");
            if (pos < 0) pos = s.length();
            String t = s.substring(0, pos);
            pos++;
            if (pos < s.length()) s = s.substring(pos);
            else                  done = true;
            pos = t.indexOf("===");
            if ((pos >= 0) && !(t.substring(0, 1).equals("#"))) {
                String key = t.substring(0, pos).trim();
                t = t.substring(pos + 3);
                pos = t.indexOf("===");
                String value = t.substring(0, pos).trim();
                if ((pos >= 0) && (!key.equals("")) && (!value.equals(""))) hlang.put(key, value);
            }
        }
    }

    public String translate(String label) {
        if (hlang == null) return label;
        String lbl = (String) hlang.get(label);
        if (lbl == null) return label;
        return lbl;
    }

    public Hashtable getLanguages() {
        return hlang;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }
}
