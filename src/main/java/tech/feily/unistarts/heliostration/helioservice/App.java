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
import tech.feily.unistarts.heliostration.helioservice.utils.PreCmd;

/**
 * Hello world!
 *
 */
public class App {
    
    public static void main( String[] args ) throws Exception {
        System.out.println("Welcome to the HelioChain platform(Service Node).");
        System.out.println("Current application version : Alpha 0.0.1.0423");
        System.out.println("This application is licensed through GNU General Public License version 3 (GPLv3).");
        System.out.println("Copyright \u00A92020 tpastd.com. All rights reserved.\n");
        System.out.println("First, you need to add some configuration information to use.");
        System.out.println("------------------------------------------------------------------");
        PreCmd.run();
        ServerNodeModel ser = new ServerNodeModel();
        ser.setServerId(PreCmd.getParam().get("serverId"));
        ser.setServerKey(PreCmd.getParam().get("serverKey"));
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.init);
        msg.setServer(ser);
        AddrPortModel ap = new AddrPortModel();
        ap.setAddr("/" + PreCmd.getParam().get("curtAddr"));
        ap.setPort(Integer.parseInt(PreCmd.getParam().get("curtPort")));
        msg.setAp(ap);
        Pbft pbft = new Pbft(ap);
        P2pServerEnd.run(pbft, Integer.parseInt(PreCmd.getParam().get("curtPort")));
        /**
         * Let the server start before sleeping for 500ms.
         */
        TimeUnit.MILLISECONDS.sleep(500);
        P2pClientEnd.connect(pbft, "ws://" + PreCmd.getParam().get("rootAddr") + ":" + PreCmd.getParam().get("rootPort"), new Gson().toJson(msg), msg);
    }
}

