package tech.feily.unistarts.heliostration.helioservice.p2p;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import tech.feily.unistarts.heliostration.helioservice.model.MsgEnum;
import tech.feily.unistarts.heliostration.helioservice.model.PbftMsgModel;
import tech.feily.unistarts.heliostration.helioservice.pbft.Pbft;
import tech.feily.unistarts.heliostration.helioservice.utils.SystemUtil;

/**
 * The client program of P2P node.
 * 
 * @author Feily Zhang
 * @version v0.1
 */
public class P2pClientEnd {

    //private static Logger log = Logger.getLogger(P2pClientEnd.class);
    
    /**
     * Client connects to a server.
     * 
     * @param wsUrl - server's url.
     */
    public static void connect(final Pbft pbft, final String wsUrl, final String msg, final PbftMsgModel pm) {
        try {
            final WebSocketClient socketClient = new WebSocketClient(new URI(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    sendMsg(this, msg, pm);
                }

                @Override
                public void onMessage(String msg) {
                    pbft.handle(this, msg);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    PbftMsgModel psm = new PbftMsgModel();
                    psm.setMsgType(MsgEnum.close);
                    SystemUtil.printlnClientCloseOrError(psm, wsUrl);
                }

                @Override
                public void onError(Exception ex) {
                    PbftMsgModel psm = new PbftMsgModel();
                    psm.setMsgType(MsgEnum.error);
                    SystemUtil.printlnClientCloseOrError(psm, wsUrl);
                }
            };
            socketClient.connect();
        } catch (URISyntaxException e) {
            PbftMsgModel psm = new PbftMsgModel();
            psm.setMsgType(MsgEnum.exception);
            SystemUtil.printlnClientCloseOrError(psm, wsUrl);
        }
    }

    /**
     * The method of sending a message to a server.
     * 
     * @param ws - websocket
     * @param msg - Messages to send.
     */
    public static void sendMsg(WebSocket ws, String msg, PbftMsgModel pm) {
        ws.send(msg);
        SystemUtil.printlnOut(pm);
    }


    /**
     * The method of broadcasting a massage to all server.
     * 
     * @param msg - Messages to send.
     */
    public static void broadcast(String msg, PbftMsgModel pm) {
        if (SocketCache.wss.size() == 0 || msg == null || msg.equals("")) {
            return;
        }
        for (WebSocket ws : SocketCache.wss) {
            sendMsg(ws, msg, pm);
        }
    }
    
}
