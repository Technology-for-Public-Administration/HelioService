package tech.feily.unistarts.heliostration.helioservice.pbft;

import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import tech.feily.unistarts.heliostration.helioservice.model.AddrPortModel;
import tech.feily.unistarts.heliostration.helioservice.model.MsgEnum;
import tech.feily.unistarts.heliostration.helioservice.model.PbftContentModel;
import tech.feily.unistarts.heliostration.helioservice.model.PbftMsgModel;
import tech.feily.unistarts.heliostration.helioservice.model.ServerNodeModel;
import tech.feily.unistarts.heliostration.helioservice.p2p.P2pClientEnd;
import tech.feily.unistarts.heliostration.helioservice.p2p.P2pServerEnd;
import tech.feily.unistarts.heliostration.helioservice.p2p.SocketCache;
import tech.feily.unistarts.heliostration.helioservice.utils.SHAUtil;
import tech.feily.unistarts.heliostration.helioservice.utils.SystemUtil;

public class Pbft {

    private Gson gson = new Gson();
    private AddrPortModel ap;
    /**
     * The Pbft of the service node does not need to be initialized,
     * because the necessary cache information needs to be.
     */
    public Pbft(AddrPortModel ap) {
        /**
         * Nothing to do.
         */
        this.ap = ap;
    }
    
    public void handle(WebSocket ws, String msg) {
        //log.info("From " + ws.getRemoteSocketAddress().getAddress().toString() + ":"
                //+ ws.getRemoteSocketAddress().getPort() + ", message is " + msg);
        PbftMsgModel msgs = gson.fromJson(msg, PbftMsgModel.class);
        SystemUtil.printlnIn(msgs);
        switch (msgs.getMsgType()) {
            case note :
                onNote(ws, msgs);
                break;
            case detective:
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
            case update :
                onUpdate(ws, msgs);
                break;
            case prePrepare :
                onPrePrepare(ws, msgs);
                break;
            case prepare :
                onPrepare(ws, msgs);
                break;
            case commit :
                onCommit(ws, msgs);
                break;
            default :
                break;
        }
    }

    private void onCommit(WebSocket ws, PbftMsgModel msgs) {
        if (!comIsValid(msgs.getPcm()) || !containServer(msgs.getServer())) {
            return;
        }
        if (isFirstCom(msgs.getPcm())) {
            SocketCache.com.put(msgs.getPcm().getReqNum(), msgs);
            SocketCache.preNum.put(msgs.getPcm().getReqNum(), 1);
        }
        SocketCache.preNum.put(msgs.getPcm().getReqNum(), SocketCache.preNum.get(msgs.getPcm().getReqNum()) + 1);
        if (SocketCache.preNum.get(msgs.getPcm().getReqNum()) >= (2 * SocketCache.getMeta().getMaxf() + 1)) {
            PbftMsgModel ret = new PbftMsgModel();
            ret.setMsgType(MsgEnum.reply);
            ret.setAp(ap);
            P2pClientEnd.connect(this, "ws:/" + msgs.getPcm().getAp().getAddr() + ":" + msgs.getPcm().getAp().getPort(), gson.toJson(ret), ret);
            SocketCache.ack.set(msgs.getPcm().getReqNum());
            return;
        }
    }

    private boolean isFirstCom(PbftContentModel pcm) {
        return !SocketCache.com.containsKey(pcm.getReqNum());
    }

    private boolean comIsValid(PbftContentModel pcm) {
        return SHAUtil.sha256BasedHutool(pcm.getTransaction().toString()).equals(pcm.getDigest())
                && pcm.getViewNum() == SocketCache.getMeta().getView()
                && SocketCache.pre.containsKey(pcm.getReqNum())
                && pcm.getReqNum() > SocketCache.ack.get();
    }

    private void onPrepare(WebSocket ws, PbftMsgModel msgs) {
        /**
         * If there is no processing in the previous stage or no or permission.
         */
        if (!preIsValid(msgs.getPcm()) || !containServer(msgs.getServer())) {
            return;
        }
        /**
         * If this stage is processed for the first time.
         */
        if (isFirstPre(msgs.getPcm())) {
            SocketCache.pre.put(msgs.getPcm().getReqNum(), msgs);
            SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), 0);
        }
        SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) + 1);
        /**
         * If the conditions are met, proceed to the next stage.
         */
        if (SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) > 2 * SocketCache.getMeta().getMaxf()) {
            msgs.setMsgType(MsgEnum.commit);
            ServerNodeModel snm = new ServerNodeModel();
            snm.setAccessKey(SocketCache.getMyself().getAccessKey());
            snm.setServerId(SocketCache.getMyself().getServerId());
            msgs.setServer(snm);
            msgs.setAp(ap);
            P2pServerEnd.broadcasts(gson.toJson(msgs), msgs);
            return;
        }
    }


    private boolean isFirstPre(PbftContentModel pcm) {
        return !SocketCache.pre.containsKey(pcm.getReqNum()) && !SocketCache.preNum.containsKey(pcm.getReqNum());
    }

    private boolean containServer(ServerNodeModel server) {
        for (ServerNodeModel ser : SocketCache.listServer) {
            if (ser.getAccessKey().equals(server.getAccessKey())
                    && ser.getServerId().equals(server.getServerId())) {
                return true;
            }
        }
        return false;
    }

    private boolean preIsValid(PbftContentModel pcm) {
        return SHAUtil.sha256BasedHutool(pcm.getTransaction().toString()).equals(pcm.getDigest())
                && pcm.getViewNum() == SocketCache.getMeta().getView()
                && SocketCache.ppre.containsKey(pcm.getReqNum())
                && pcm.getReqNum() > SocketCache.ack.get();
    }

    private void onPrePrepare(WebSocket ws, PbftMsgModel msgs) {
        if (!ppreIsValid(msgs.getPcm())) {
            return;
        }
        /**
         * Then add current request to this node's ppre.
         */
        SocketCache.ppre.put(msgs.getPcm().getReqNum(), msgs);
        SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), 0);
        msgs.setMsgType(MsgEnum.prepare);
        ServerNodeModel snm = new ServerNodeModel();
        snm.setAccessKey(SocketCache.getMyself().getAccessKey());
        snm.setServerId(SocketCache.getMyself().getServerId());
        msgs.setServer(snm);
        msgs.setAp(ap);
        P2pServerEnd.broadcasts(gson.toJson(msgs), msgs);
    }

    /**
     * 
     * @param pcm
     * @return
     */
    private boolean ppreIsValid(PbftContentModel pcm) {
        return SHAUtil.sha256BasedHutool(pcm.getTransaction().toString()).equals(pcm.getDigest())
                && pcm.getViewNum() == SocketCache.getMeta().getView()
                && !SocketCache.ppre.containsKey(pcm.getReqNum())
                && !SocketCache.ppreNum.containsKey(pcm.getReqNum())
                && pcm.getReqNum() > SocketCache.ack.get();
    }
    
    private void onUpdate(WebSocket ws, PbftMsgModel msgs) {
        /**
         * We should first verify whether the detective message comes from the root node, and temporarily omit it.
         */
        SocketCache.setMeta(msgs.getMeta());
        //System.out.println("meta缓存已更新" + SocketCache.getMeta().toString());
    }

    private void onDetective(WebSocket ws, PbftMsgModel msgs) {
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.confirm);
        msg.setAp(ap);
        msgs.setMsgType(MsgEnum.confirm);
        P2pClientEnd.connect(this, "ws:/" + msgs.getAp().getAddr() + ":" + msgs.getAp().getPort(), gson.toJson(msg), msgs);
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
        //System.out.println(new Date() + SocketCache.listServer.toString());
        //System.out.println(new Date() + SocketCache.getMeta().toString());
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
        toRoot.setAp(ap);
        P2pServerEnd.sendMsg(ws, gson.toJson(toRoot), toRoot);
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
        //System.out.println("Current P2P network metadata: " + SocketCache.getMeta().toString());
    }

    /**
     * Probe client's ws for final receipt to it.
     * 
     * @param ws
     * @param msgs
     */
    private void onNote(WebSocket ws, PbftMsgModel msgs) {
        /**
         * We should first verify whether the detective message comes from the root node, and temporarily omit it.
         */
        /**
         * Send probe information to the specified address (client).
         */
        if (msgs.getAp().getAddr().equals(ws.getLocalSocketAddress().getAddress().toString())
                && msgs.getAp().getPort() == ap.getPort()) {
            SystemUtil.println(msgs);
            return;
        }
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.detective);
        msg.setAp(ap);
        msgs.setMsgType(MsgEnum.detective);
        P2pClientEnd.connect(this, "ws:/" + msgs.getAp().getAddr() + ":" + msgs.getAp().getPort(), gson.toJson(msg), msgs);
    }

}
