package feily.tech.unistarts.heliostration.helioservice.p2p;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import feily.tech.unistarts.heliostration.helioservice.model.FileWriterModel;
import feily.tech.unistarts.heliostration.helioservice.pbft.Pbft;
import feily.tech.unistarts.heliostration.helioservice.utils.FileUtil;

/**
 * The client program of P2P node.
 * 
 * @author Feily Zhang
 * @version v0.1
 */
public class P2pClientEnd {

    private static Logger log = Logger.getLogger(P2pClientEnd.class);
    
    /**
     * Client connects to a server.
     * 
     * @param wsUrl - server's url.
     */
    public static void connect(final Pbft pbft, String wsUrl, final String msg, final String mapper, final int port, final boolean flag) {
        try {
            final WebSocketClient socketClient = new WebSocketClient(new URI(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    /**
                     * Write port mapping information to a file.
                     */
                    if (flag == true) {
                        FileWriterModel fm = FileUtil.openForW(mapper);
                        FileUtil.write(port + "=" + this.getLocalSocketAddress().getPort() + "\n", fm);
                        FileUtil.closeForW(fm);
                    }
                    sendMsg(this, msg);
                }

                @Override
                public void onMessage(String msg) {
                    pbft.handle(this, msg);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("Client close.");
                }

                @Override
                public void onError(Exception ex) {
                    log.info("Client error.");
                }
            };
            socketClient.connect();
        } catch (URISyntaxException e) {
            log.info("URISyntaxException : " + e.getMessage());
        }
    }

    /**
     * The method of sending a message to a server.
     * 
     * @param ws - websocket
     * @param msg - Messages to send.
     */
    public static void sendMsg(WebSocket ws, String msg) {
        ws.send(msg);
    }


    /**
     * The method of broadcasting a massage to all server.
     * 
     * @param msg - Messages to send.
     */
    public void broadcast(String msg) {
        if (SocketCache.wss.size() == 0 || msg == null || msg.equals("")) {
            return;
        }
        for (WebSocket ws : SocketCache.wss) {
            sendMsg(ws, msg);
        }
    }
    
}
