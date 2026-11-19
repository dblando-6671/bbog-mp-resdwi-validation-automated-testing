package co.com.bdb.automation.service;

/** Utilidades de texto que imitan el comportamiento del script Python original. */
public final class Texto {

    public static final char NUL = (char) 0;
    public static final String CRLF = "\r\n";
    public static final String LF = "\n";
    /** Como se muestra un byte 0x00 en el Excel (punto medio). */
    public static final char NUL_VISIBLE = (char) 183;

    private Texto() {
    }

    /** Equivalente a str.strip() de Python (no quita el 0x00). */
    public static String strip(String s) {
        int ini = 0;
        int fin = s.length();
        while (ini < fin && esEspacio(s.charAt(ini))) {
            ini++;
        }
        while (fin > ini && esEspacio(s.charAt(fin - 1))) {
            fin--;
        }
        return s.substring(ini, fin);
    }

    private static boolean esEspacio(char c) {
        return c == ' ' || (c >= 9 && c <= 13) || (c >= 28 && c <= 31) || c == 133 || c == 160;
    }

    /** Solo digitos ASCII 0-9 y al menos uno. */
    public static boolean esNumerico(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static int contar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    /** Hace visible el 0x00 y cambia por espacio los caracteres de control que Excel no acepta. */
    public static String paraExcel(Object val) {
        if (val == null) {
            return "";
        }
        String texto = String.valueOf(val);
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == NUL) {
                sb.append(NUL_VISIBLE);
            } else if ((c >= 1 && c <= 8) || c == 11 || c == 12 || (c >= 14 && c <= 31) || (c >= 127 && c <= 159)) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
