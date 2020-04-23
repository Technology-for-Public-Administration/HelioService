package tech.feily.unistarts.heliostration.helioservice.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.client.MongoDatabase;

public class PreCmd {

    private static Scanner scan = new Scanner(System.in);
    private static String[] fields = {"rootAddr", "rootPort", "curtAddr", "curtPort", "dbHost", "dbname", "docName", "serverId", "serverKey"};
    private static String[] display = {"Root addr: ", "Curt addr: ", "DB & doc: ", "Id & Key: "};
    private static Pattern pattern;
    private static Map<String, String> param = new HashMap<String, String>();
    
    public static boolean run() {
        String temp = "";
        int i = 0;
        while (i < display.length) {
            System.out.print(display[i]);
            temp = scan.nextLine();
            if (display[i].equals("Root addr: ")  && addrIsValid(temp)) {
                param.put(fields[0], temp.split(":")[0]);
                param.put(fields[1], temp.split(":")[1]);
                i++;
            } else if (display[i].equals("Curt addr: ")  && addrIsValid(temp)) {
                param.put(fields[2], temp.split(":")[0]);
                param.put(fields[3], temp.split(":")[1]);
                i++;
            } else if (display[i].equals("DB & doc: ")  && dbIsValid(temp)) {
                param.put(fields[4], temp.split("\\.")[0]);
                param.put(fields[5], temp.split("\\.")[1]);
                param.put(fields[6], temp.split("\\.")[2]);
                i++;
            } else if (display[i].equals("Id & Key: ")){
                param.put(fields[7], temp.split("&")[0]);
                param.put(fields[8], temp.split("&")[1]);
                i++;
            }
        }
        return true;
    }
    
    public static boolean addrIsValid(String addr) {
        String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{4,5}";
        pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(addr);
        if (!matcher.matches()) return false;
        int port = Integer.parseInt(addr.split(":")[1]);
        return matcher.matches() && port > 5000 && port < 65535;
    }

    
    public static boolean dbIsValid(String dbdoc) {
        try {
            MongoDatabase mgdb = MongoDB.getInstance(dbdoc.split("\\.")[0], dbdoc.split("\\.")[1]);
            mgdb.getCollection(dbdoc.split("\\.")[2]);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
    
    public static Map<String, String> getParam() {
        return param;
    }
    
}
