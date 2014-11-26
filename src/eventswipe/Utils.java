package eventswipe;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matt Wildman
 */
public class Utils {

    public static final String UTF8 = "UTF8";
    public static final String UNICODE = "UTF-16";
    public static final String ANSI = "Cp1252";

    public static boolean isInternetReachable() {
        try {
            InetAddress address = InetAddress.getByName("java.sun.com");
            if (address == null) {
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static void pressAlt() {
        /*try {
            Robot r = new Robot();
            r.keyPress(KeyEvent.VK_ALT);
        } catch (AWTException ex) {
            System.err.println(ex.getMessage());
        }*/
    }

    public static void releaseAlt() {
        /*try {
            Robot r = new Robot();
            r.keyRelease(KeyEvent.VK_ALT);
        } catch (AWTException ex) {
            System.err.println(ex.getMessage());
        }*/
    }

    public static String readLine(File file, String encoding) {
        String line = "";
        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fstream);
            InputStreamReader isr = new InputStreamReader(dis, encoding);
            BufferedReader br = new BufferedReader(isr);
            line = br.readLine();
            line = encoding.equals("UTF8") ? Utils.removeUTF8BOM(line) : line;
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return line;
    }

    public static List<String> readAllLines(File file, String encoding) {
        ArrayList<String> list = new ArrayList<String>();
        boolean firstLine = true;
        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fstream);
            InputStreamReader isr = new InputStreamReader(dis, encoding);
            BufferedReader br = new BufferedReader(isr);
            String strLine = null;
            while ((strLine = br.readLine()) != null) {
                strLine = firstLine && encoding.equals("UTF8")
                        ? Utils.removeUTF8BOM(strLine) : strLine;
                list.add(strLine);
                if (firstLine) {
                    firstLine = false;
                }
            }
            dis.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return list;
    }

    public static String getEncoding(File file) {
        String testString = Utils.readLine(file, ANSI).substring(0, 2);
        if (testString.equals(UTF8_TEST_STRING)) {
            return UTF8;
        } else if (testString.equals(UNICODE_LE_TEST_STRING)
                || testString.equals(UNICODE_BE_TEST_STRING)) {
            return UNICODE;
        } else {
            return ANSI;
        }
    }

    private static String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }

    public static String getDate(String format) {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        return dateFormat.format(date);
    }
    
    public static void failureNoise() {
        Runnable sound = (Runnable) Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.hand");
        if (sound != null) {
            sound.run();
        }
        /*try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    public static void successNoise() {
        Runnable sound = (Runnable) Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.asterisk");
        if (sound != null) {
            sound.run();
        }
        /*try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    private static final String UTF8_BOM = "\uFEFF";
    private static final String UTF8_TEST_STRING = "ï»";
    private static final String UNICODE_LE_TEST_STRING = "ÿþ";
    private static final String UNICODE_BE_TEST_STRING = "þÿ";
}
