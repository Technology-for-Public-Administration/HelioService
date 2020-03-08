package tech.feily.unistarts.heliostration.helioservice;

import com.google.gson.Gson;

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
    
    public static void main( String[] args ) {
        int port = 7002;
        Pbft pbft = new Pbft(port);
        ServerNodeModel ser = new ServerNodeModel();
        ser.setServerId("123456");
        ser.setServerKey("789123");
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.init);
        msg.setServer(ser);
        P2pClientEnd.connect(pbft, "ws://localhost:7001", new Gson().toJson(msg), port);
        P2pServerEnd.run(pbft, port);
    }
}

