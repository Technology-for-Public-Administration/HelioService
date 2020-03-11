package tech.feily.unistarts.heliostration.helioservice.utils;

import java.util.Date;

import tech.feily.unistarts.heliostration.helioservice.model.PbftMsgModel;

/**
 * This class for console output.
 * 
 * @author Feily zhang
 * @version v0.1
 */
public class SystemUtil {

    /**
     * If the target address of the broadcast received from the root node is itself, the method is output.
     * 
     * @param msg
     */
    public static void println(PbftMsgModel msg) {
        println("    - []          | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                 + " $ this is my address, no need to probe.");
    }
    
    /**
     * Table head.
     */
    public static void printHead() {
        System.out.println("  Time   |      MsgType      | Bc  |                     Details                     ");
        System.out.println("-------------------------------------------------------------------------------------");
    }
    
    /**
     * Execute this output when a message is received.
     * 
     * @param msg
     */
    public static void printlnIn(PbftMsgModel msg) {
        switch (msg.getMsgType()) {
            case note :
                println("in  - [note]      | no  | @rootnode $ new node joining, websocket to detect new nodes.");
                break;
            case init :
                println("in  - [init]      | no  | @rootnode $ receive my accessKey.");
                break;
            case service :
                println("in  - [service]   | no  | @rootnode $ receive metadata & session credentials of all service nodes.");
                println("    -             |     | " + msg.getMeta());            
                break;
            case confirm :
                println("in  - [confirm]   | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                         + " $ WebSocket of new node has been saved.");
                break;
            case detective :
                println("in  - [detective] | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                        + " $ the other side probes my websocket, and I gets its WebSocket.");
                break;
            case update :
                println("in  - [update]    | no  | @rootnode $ node exit, root node update metadata of p2p network.");
                println("    -             |     | " + msg.getMeta());   
                break;
            case prePrepare :
                println("in  - [prepre]    | yes | @rootnode $ receive pre-prepare msg.");
                break;
            case prepare :
                println("in  - [prepare]   | yes | service node@" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                         + " $ receive prepare msg.");
                break;
            case commit :
                println("in  - [commit]    | yes | service node@" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                         + " $ receive commit msg.");
                break;
            default:
                break;
        }
    }

    /**
     * Execute this output when a message is sent.
     * 
     * @param msg
     */
    public static void printlnOut(PbftMsgModel msg) {
        switch (msg.getMsgType()) {
            case init :
                println("out - [init]      | no  | @rootnode $ request accessKey.");
                break;
            case service :
                println("out - [service]   | no  | @rootnode $ request metadata & session credentials of all service nodes.");
                break;
            case detective :
                println("out - [detective] | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                        + " $ send detection packet.");
                break;
            case confirm :
                println("out - [confirm]   | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                        + " $ send my WebSocket.");
                break;
            case prepare :
                println("out - [prepare]   | yes | @all $ broadcast prepare msg.");
                break;
            case commit :
                println("out - [commit]    | yes | @all $ broadcast commit msg.");
                break;
            case reply :
                println("out - [reply]     | yes | client node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                        + " $ send reply msg.");
                break;
            default:
                break;
        }
    }
    
    /**
     * This output is performed when a client error occurs or the connection is closed.
     * 
     * @param msg
     * @param wsUrl
     */
    public static void printlnClientCloseOrError(PbftMsgModel msg, String wsUrl) {
        switch (msg.getMsgType()) {
            case close :
                println("    - [close]     | no  | node @" + wsUrl.substring(4) + " $ closed.");
                break;
            case error :
                println("    - [error]     | no  | node @" + wsUrl.substring(4) + " $ occurs error.");
                break;
            case exception :
                println("    - [exception] | no  | node @" + wsUrl.substring(4) + " $ exception.");
                break;
            default:
                break;
        }
    }
    
    /**
     * Assembly time.
     * 
     * @param line
     */
    @SuppressWarnings("deprecation")
    public static void println(String line) {
        Date date = new Date();
        System.out.println(date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds() + " | " + line);
    }
    
}
