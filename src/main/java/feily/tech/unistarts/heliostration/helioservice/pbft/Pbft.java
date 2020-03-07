package feily.tech.unistarts.heliostration.helioservice.pbft;

import java.util.Date;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import feily.tech.unistarts.heliostration.helioservice.model.AddrPortModel;
import feily.tech.unistarts.heliostration.helioservice.model.FileReaderModel;
import feily.tech.unistarts.heliostration.helioservice.model.MsgEnum;
import feily.tech.unistarts.heliostration.helioservice.model.PbftMsgModel;
import feily.tech.unistarts.heliostration.helioservice.p2p.P2pClientEnd;
import feily.tech.unistarts.heliostration.helioservice.p2p.P2pServerEnd;
import feily.tech.unistarts.heliostration.helioservice.p2p.SocketCache;
import feily.tech.unistarts.heliostration.helioservice.utils.FileUtil;

public class Pbft {

    private Logger log = Logger.getLogger(Pbft.class);
    private Gson gson = new Gson();
    private String file;
    private int port;
    /**
     * The Pbft of the service node does not need to be initialized,
     * because the necessary cache information needs to be.
     */
    public Pbft(String file, int port) {
        /**
         * Nothing to do.
         */
        this.file = file;
        this.port = port;
    }
    
    public void handle(WebSocket ws, String msg) {
        log.info("From " + ws.getRemoteSocketAddress().getAddress().toString() + ":"
                + ws.getRemoteSocketAddress().getPort() + ", message is " + msg);
        PbftMsgModel msgs = gson.fromJson(msg, PbftMsgModel.class);
        switch (msgs.getMsgType()) {
            case detective :
                onDetective(ws, msgs);
                break;
            case confirm :
                onConfirm(ws, msgs);
                break;
            case init :
                onInit(ws, msgs);
                break;
            case service :
                onService(ws, msgs);
                break;
            default :
                break;
        }
    }

    private void onService(WebSocket ws, PbftMsgModel msgs) {
        /**
         * We should first verify whether the detective message comes from the root node, and temporarily omit it.
         */
        /**
         * Get active node permission data and network metadata.
         */
        SocketCache.listServer = msgs.getListServer();
        SocketCache.setMeta(msgs.getMeta());
        System.out.println(new Date() + SocketCache.listServer.toString());
        System.out.println(new Date() + SocketCache.getMeta().toString());
    }

    private void onInit(WebSocket ws, PbftMsgModel msgs) {
        /**
         * We should first verify whether the detective message comes from the root node, and temporarily omit it.
         */
        /**
         * First obtain own accessKey.
         */
        SocketCache.setMyself(msgs.getServer());
        /**
         * Then assemble the service type message and request the P2P network information from the root node.
         */
        PbftMsgModel toRoot = new PbftMsgModel();
        toRoot.setMsgType(MsgEnum.service);
        toRoot.setServer(SocketCache.getMyself());
        P2pServerEnd.sendMsg(ws, gson.toJson(toRoot));
    }

    /**
     * Processing of client confirm message.
     * 
     * @param ws
     * @param msgs
     */
    private void onConfirm(WebSocket ws, PbftMsgModel msgs) {
        /**
         * Nothing to do, because we just want to acquire ws of client.
         * When the client requests this node through the p2pclientend class, we have obtained the WS of the client.
         */
    }

    /**
     * Probe client's ws for final receipt to it.
     * 
     * @param ws
     * @param msgs
     */
    private void onDetective(WebSocket ws, PbftMsgModel msgs) {
        /**
         * We should first verify whether the detective message comes from the root node, and temporarily omit it.
         */
        /**
         * Send probe information to the specified address (client).
         */
        System.out.println(msgs.getAp().toString());
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.detective);
        AddrPortModel ap = new AddrPortModel();
        ap.setAddr(ws.getLocalSocketAddress().getAddress().toString());
        ap.setPort(ws.getLocalSocketAddress().getPort());
        msg.setAp(ap);
        FileReaderModel fm = FileUtil.openForR(file);
        Integer pt = msgs.getAp().getPort();
        String pot = FileUtil.selectByPort(fm, pt.toString());
        Integer por = Integer.valueOf(pot);
        System.out.println(por == port);
        if (por == port) {
            return;
        }
        P2pClientEnd.connect(this, "ws:/" + msgs.getAp().getAddr() + ":" + por.toString(), gson.toJson(msg), file, port, false);
        FileUtil.closeForR(fm);
    }

}
