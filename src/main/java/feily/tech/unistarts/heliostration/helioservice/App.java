package feily.tech.unistarts.heliostration.helioservice;

import com.google.gson.Gson;

import feily.tech.unistarts.heliostration.helioservice.model.MsgEnum;
import feily.tech.unistarts.heliostration.helioservice.model.PbftMsgModel;
import feily.tech.unistarts.heliostration.helioservice.model.ServerNodeModel;
import feily.tech.unistarts.heliostration.helioservice.p2p.P2pClientEnd;
import feily.tech.unistarts.heliostration.helioservice.p2p.P2pServerEnd;
import feily.tech.unistarts.heliostration.helioservice.pbft.Pbft;

/**
 * Hello world!
 *
 */
public class App {
    
    public static void main( String[] args ) {
        String file = "C:\\Users\\fei47\\Desktop\\mapper.txt";
        int port = 7002;
        Pbft pbft = new Pbft(file, port);
        ServerNodeModel ser = new ServerNodeModel();
        ser.setServerId("123456");
        ser.setServerKey("789123");
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.init);
        msg.setServer(ser);
        P2pClientEnd.connect(pbft, "ws://localhost:7001", new Gson().toJson(msg), file, port, true);
        P2pServerEnd.run(pbft, port);
    }
}
