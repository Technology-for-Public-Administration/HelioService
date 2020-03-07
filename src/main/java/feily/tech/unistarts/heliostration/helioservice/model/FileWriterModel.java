package feily.tech.unistarts.heliostration.helioservice.model;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class FileWriterModel {
    
    private FileWriter fw;
    private BufferedWriter bw;
    
    /**
     * @return the fw
     */
    public FileWriter getFw() {
        return fw;
    }
    /**
     * @param fw the fw to set
     */
    public void setFw(FileWriter fw) {
        this.fw = fw;
    }
    /**
     * @return the bw
     */
    public BufferedWriter getBw() {
        return bw;
    }
    /**
     * @param bw the bw to set
     */
    public void setBw(BufferedWriter bw) {
        this.bw = bw;
    }

}
