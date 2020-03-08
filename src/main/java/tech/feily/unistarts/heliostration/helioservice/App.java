package tech.feily.unistarts.heliostration.helioservice;

import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import tech.feily.unistarts.heliostration.helioservice.model.AddrPortModel;
import tech.feily.unistarts.heliostration.helioservice.model.MsgEnum;
import tech.feily.unistarts.heliostration.helioservice.model.PbftMsgModel;
import tech.feily.unistarts.heliostration.helioservice.model.ServerNodeModel;
import tech.feily.unistarts.heliostration.helioservice.p2p.P2pClientEnd;
import tech.feily.unistarts.heliostration.helioservice.p2p.P2pServerEnd;
import tech.feily.unistarts.heliostration.helioservice.pbft.Pbft;

/**
 * Hello world!
 *
 */
public class App {
    
    public static void main( String[] args ) throws Exception {
        int port = 7002;
        ServerNodeModel ser = new ServerNodeModel();
        ser.setServerId("123456");
        ser.setServerKey("789123");
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.init);
        msg.setServer(ser);
        AddrPortModel ap = new AddrPortModel();
        ap.setAddr("/127.0.0.1");
        ap.setPort(port);
        msg.setAp(ap);
        Pbft pbft = new Pbft(ap);
        P2pServerEnd.run(pbft, port);
        /**
         * Let the server start before sleeping for 500ms.
         */
        TimeUnit.MILLISECONDS.sleep(500);
        P2pClientEnd.connect(pbft, "ws://localhost:7001", new Gson().toJson(msg), msg);
    }
}

