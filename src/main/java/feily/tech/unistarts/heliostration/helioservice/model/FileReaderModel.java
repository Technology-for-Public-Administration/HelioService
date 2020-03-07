package feily.tech.unistarts.heliostration.helioservice.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class FileReaderModel {
    
    private FileInputStream fis;
    private InputStreamReader isr;
    private BufferedReader br;
    
    /**
     * @return the fis
     */
    public FileInputStream getFis() {
        return fis;
    }
    /**
     * @param fis the fis to set
     */
    public void setFis(FileInputStream fis) {
        this.fis = fis;
    }
    /**
     * @return the isr
     */
    public InputStreamReader getIsr() {
        return isr;
    }
    /**
     * @param isr the isr to set
     */
    public void setIsr(InputStreamReader isr) {
        this.isr = isr;
    }
    /**
     * @return the br
     */
    public BufferedReader getBr() {
        return br;
    }
    /**
     * @param br the br to set
     */
    public void setBr(BufferedReader br) {
        this.br = br;
    }
    
}
