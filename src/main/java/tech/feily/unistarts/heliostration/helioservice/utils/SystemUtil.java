package tech.feily.unistarts.heliostration.helioservice.utils;

import java.util.Date;

import tech.feily.unistarts.heliostration.helioservice.model.PbftMsgModel;

public class SystemUtil {

    private static Date date = new Date();

    public static void println(PbftMsgModel msg) {
        println("    - []          | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                 + " this is my address, no need to probe.");
    }
    
    public static void printHead() {
        System.out.println("  Time   |      MsgType      | Bc  |                     Details                     ");
        System.out.println("-------------------------------------------------------------------------------------");
    }
    
    public static void printlnIn(PbftMsgModel msg) {
        switch (msg.getMsgType()) {
            case note :
                println("in  - [note]      | no  | @rootnode new node joining, websocket to detect new nodes.");
                break;
            case init :
                println("in  - [init]      | no  | @rootnode receive my accessKey.");
                break;
            case service :
                println("in  - [service]   | no  | @rootnode receive metadata & session credentials of all service nodes.");
                println("    -             |     | " + msg.getMeta());            
                break;
            case confirm :
                println("in  - [confirm]   | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                         + " WebSocket of new node has been saved.");
                break;
            case detective :
                println("in  - [detective] | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                        + " the other side probes my websocket, and I gets its WebSocket.");
                break;
            case update :
                println("in  - [update]    | no  | @rootnode node exit, root node update metadata of p2p network.");
                println("    -             |     | " + msg.getMeta());   
                break;
            default:
                break;
        }
    }

    public static void printlnOut(PbftMsgModel msg) {
        switch (msg.getMsgType()) {
            case init :
                println("out - [init]      | no  | @rootnode" + " request accessKey.");
                break;
            case service :
                println("out - [service]   | no  | @rootnode" + " request metadata & session credentials of all service nodes.");
                break;
            case detective :
                println("out - [detective] | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                        + " send detection packet.");
                break;
            case confirm :
                println("out - [confirm]   | no  | service node @" + msg.getAp().getAddr() + ":" + msg.getAp().getPort()
                        + " send my WebSocket.");
               break;
            default:
                break;
        }
    }
    
    public static void printlnClientCloseOrError(PbftMsgModel msg, String wsUrl) {
        switch (msg.getMsgType()) {
            case close :
                println("    - [close]     | no  | node @" + wsUrl.substring(4) + " closed.");
                break;
            case error :
                println("    - [error]     | no  | node @" + wsUrl.substring(4) + " occurs error.");
                break;
            case exception :
                println("    - [exception] | no  | node @" + wsUrl.substring(4) + " exception.");
                break;
            default:
                break;
        }
    }
    
    @SuppressWarnings("deprecation")
    public static void println(String line) {
        System.out.println(date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds() + " | " + line);
    }
    
}
