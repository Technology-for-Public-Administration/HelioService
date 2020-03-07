package feily.tech.unistarts.heliostration.helioservice.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import feily.tech.unistarts.heliostration.helioservice.model.FileReaderModel;
import feily.tech.unistarts.heliostration.helioservice.model.FileWriterModel;

public class FileUtil {
    
    public static FileWriterModel openForW(String fileName) {
        File file = new File(fileName);
        FileWriterModel fm = new FileWriterModel();
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fm.setFw(new FileWriter(file, true));
            fm.setBw(new BufferedWriter(fm.getFw()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fm;
    }
    
    public static void write(String content, FileWriterModel fm) {
        try {
            fm.getBw().write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void closeForW(FileWriterModel fm) {
        try {
            fm.getBw().close();
            fm.getFw().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static FileReaderModel openForR(String fileName) {
        File file = new File(fileName);
        FileReaderModel fm = new FileReaderModel();
        try {
            fm.setFis(new FileInputStream(file));
            fm.setIsr(new InputStreamReader(fm.getFis(), "utf-8"));
            fm.setBr(new BufferedReader(fm.getIsr()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fm;
    }
    
    public static String selectByPort(FileReaderModel fm, String port) {
        String rt = null;
        try {
            String line = null;
            while ((line = fm.getBr().readLine()) != null) {
                if (line.split("=")[1].equals(port)) {
                    rt = line.split("=")[0];
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rt;
    }
    
    public static void closeForR(FileReaderModel fm) {
        try {
            fm.getBr().close();
            fm.getIsr().close();
            fm.getFis().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
