package tech.feily.unistarts.heliostration.helioservice.pbft;

import java.util.Queue;

import org.java_websocket.WebSocket;

import com.google.common.collect.Queues;
import com.google.gson.Gson;

import tech.feily.unistarts.heliostration.helioservice.model.AddrPortModel;
import tech.feily.unistarts.heliostration.helioservice.model.BlockModel;
import tech.feily.unistarts.heliostration.helioservice.model.MsgEnum;
import tech.feily.unistarts.heliostration.helioservice.model.PbftContentModel;
import tech.feily.unistarts.heliostration.helioservice.model.PbftMsgModel;
import tech.feily.unistarts.heliostration.helioservice.model.ServerNodeModel;
import tech.feily.unistarts.heliostration.helioservice.p2p.P2pClientEnd;
import tech.feily.unistarts.heliostration.helioservice.p2p.P2pServerEnd;
import tech.feily.unistarts.heliostration.helioservice.p2p.SocketCache;
import tech.feily.unistarts.heliostration.helioservice.utils.BlockChain;
import tech.feily.unistarts.heliostration.helioservice.utils.SHAUtil;
import tech.feily.unistarts.heliostration.helioservice.utils.SystemUtil;

/**
 * Construction of P2P network and implementation of pbft algorithm.
 * 
 * @author Feily Zhang
 * @version v0.1
 */
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
                onPrepare(ws, msgs, false);
                break;
            case commit :
                onCommit(ws, msgs, false);
                break;
            default :
                break;
        }
    }

    /**
     * This method processes commit messages.
     * 
     * @param ws WebSocket of Sender.
     * @param msgs Message Entity.
     * @param isCall Whether it belongs to method call.
     */
    private void onCommit(WebSocket ws, PbftMsgModel msgs, boolean isCall) {
        // If it is triggered by an event, enter the branch.
        if (!isCall) {
            // If the message is illegal, or the other node does not have permission, reject it directly.
            if (!comIsValid(msgs.getPcm()) || !containServer(msgs.getServer())) {
                return;
            }
            /*
             * If the request number of commit message has not appeared in SocketCache.preIsDone, 
             * then this node is very slow and has not executed the statement of setting socketcache.preisdone.
             * Then cache the message and set the request number of prepare message of SocketCache.preIsDone to false.
             */
            if (!SocketCache.preIsDone.containsKey(msgs.getPcm().getReqNum())) {
                Queue<PbftMsgModel> que = Queues.newConcurrentLinkedQueue();
                que.add(msgs);
                SocketCache.comQue.put(msgs.getPcm().getReqNum(), que);
                SocketCache.preIsDone.put(msgs.getPcm().getReqNum(), false);
                return;
            } else if (SocketCache.preIsDone.get(msgs.getPcm().getReqNum()) == false) {
                Queue<PbftMsgModel> que = SocketCache.comQue.get(msgs.getPcm().getReqNum());
                que.add(msgs);
                SocketCache.comQue.put(msgs.getPcm().getReqNum(), que);
                return;
            }
            /*
             * If it is executed here, the prepare stage of the request number of this node has been completely executed.
             * In other words, in the prepare stage, the SocketCache.preIsDone has been set to true.
             * Then the new prepare message will not enter the above branch.
             * The message is processed before the cache is processed.
             */
            SocketCache.preNum.put(msgs.getPcm().getReqNum(), SocketCache.preNum.get(msgs.getPcm().getReqNum()) + 1);
        } else {
            /*
             * If the queue is found to be empty after the method is called, 
             * then the current node is faster than any other node.
             * Since there is no actual message, it returns directly.
             */
            if (SocketCache.comQue.get(msgs.getPcm().getReqNum()) == null) {
                return;
            }
        }
        Queue<PbftMsgModel> que = SocketCache.comQue.get(msgs.getPcm().getReqNum());
        if (que != null) {
            SocketCache.preNum.put(msgs.getPcm().getReqNum(), SocketCache.preNum.get(msgs.getPcm().getReqNum()) + que.size());
            que.clear();
            SocketCache.comQue.put(msgs.getPcm().getReqNum(), que);
        }
        // Send messages to client if consensus conditions are met.
        if (SocketCache.preNum.get(msgs.getPcm().getReqNum()) >= (2 * SocketCache.getMeta().getMaxf() + 1)) {
            BlockModel block = Btc.beBlock(0, msgs.getPcm().getReqNum(), SocketCache.getPreviousHash(), msgs.getPcm().getTransaction());
            SocketCache.setPreviousHash(block.getBlockHash());
            BlockChain.insert("chain", gson.toJson(block));
            //System.out.println("\n" + gson.toJson(block) + "\n");
            PbftMsgModel ret = new PbftMsgModel();
            ret.setMsgType(MsgEnum.reply);
            ret.setAp(ap);  // Tell other nodes who the information comes from.
            msgs.setMsgType(MsgEnum.reply);
            msgs.setAp(msgs.getPcm().getAp());  // to who.
            P2pClientEnd.connect(this, "ws:/" + msgs.getPcm().getAp().getAddr() + ":" + msgs.getPcm().getAp().getPort(), gson.toJson(ret), msgs);
            SocketCache.ack.set(msgs.getPcm().getReqNum()); // Update the maximum confirmation number(ack) of this node.
            remove(msgs);   // Remove all queue elements for the current message request number.
        }

    }

    /**
     * Remove element of request number.
     * 
     * @param msgs Message entity.
     */
    private void remove(PbftMsgModel msgs) {
        SocketCache.ppreNum.remove(msgs.getPcm().getReqNum());
        SocketCache.preNum.remove(msgs.getPcm().getReqNum());
        SocketCache.ppreIsDone.remove(msgs.getPcm().getReqNum());
        SocketCache.preIsDone.remove(msgs.getPcm().getReqNum());
        SocketCache.preQue.remove(msgs.getPcm().getReqNum());
        SocketCache.comQue.remove(msgs.getPcm().getReqNum());
    }

    /**
     * Determine whether the commit message from the other service node is legal. Mainly include:
     *  1. Whether the data hash value is correct, because data may be corrupted during transmission;
     *  2. Whether the view is correct;
     *  3. Whether the request number of this pbft message is greater than the confirmation number(ack) of the current node.
     * 
     * @param pcm The core model of pbft message.
     * @return true or false.
     */
    private boolean comIsValid(PbftContentModel pcm) {
        return SHAUtil.sha256BasedHutool(pcm.getTransaction().toString()).equals(pcm.getDigest())
                && pcm.getViewNum() == SocketCache.getMeta().getView()
                && pcm.getReqNum() > SocketCache.ack.get();
    }

    /**
     * This method processes prepare messages.
     * 
     * @param ws WebSocket of Sender.
     * @param msgs Message Entity.
     * @param isCall Whether it belongs to method call.
     */
    private void onPrepare(WebSocket ws, PbftMsgModel msgs, boolean isCall) {
        // If it is triggered by an event, enter the branch.
        if (!isCall) {
            // If the message is illegal, or the other node does not have permission, reject it directly.
            if (!preIsValid(msgs.getPcm()) || !containServer(msgs.getServer())) {
                return;
            }
            /*
             * If the request number of prepare message has not appeared in SocketCache.ppreIsDone, 
             * then this node is very slow and has not executed the statement of setting socketcache.ppreisdone.
             * Then cache the message and set the request number of prepare message of SocketCache.ppreIsDone to false.
             */
            if (!SocketCache.ppreIsDone.containsKey(msgs.getPcm().getReqNum())) {
                Queue<PbftMsgModel> que = Queues.newConcurrentLinkedQueue();
                que.add(msgs);
                SocketCache.preQue.put(msgs.getPcm().getReqNum(), que);
                SocketCache.ppreIsDone.put(msgs.getPcm().getReqNum(), false);
                return;
            /*
             * If the request number of prepare message has appeared in SocketCache.ppreIsDone, but is false,
             * Then add it to the cache and return.
             */
            } else if (SocketCache.ppreIsDone.get(msgs.getPcm().getReqNum()) == false) {
                Queue<PbftMsgModel> que = SocketCache.preQue.get(msgs.getPcm().getReqNum());
                que.add(msgs);
                SocketCache.preQue.put(msgs.getPcm().getReqNum(), que);
                return;
            }           
            /*
             * If it is executed here, the prepre stage of the request number of this node has been completely executed.
             * In other words, in the prepre stage, the SocketCache.ppreIsDone has been set to true.
             * Then the new prepare message will not enter the above branch.
             * The message is processed before the cache is processed.
             */
            SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) + 1);
        } else {
            /*
             * If the queue is found to be empty after the method is called, 
             * then the current node is faster than any other node.
             * Since there is no actual message, it returns directly.
             */
            if (SocketCache.preQue.get(msgs.getPcm().getReqNum()) == null) {
                return;
            }
        }
        Queue<PbftMsgModel> que = SocketCache.preQue.get(msgs.getPcm().getReqNum());
        if (que != null) {
            SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) + que.size());
            que.clear();
            SocketCache.preQue.put(msgs.getPcm().getReqNum(), que);
        }
        // Broadcast messages if consensus conditions are met.
        if (SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) > 2 * SocketCache.getMeta().getMaxf()) {
            msgs.setMsgType(MsgEnum.commit);
            ServerNodeModel snm = new ServerNodeModel();
            snm.setAccessKey(SocketCache.getMyself().getAccessKey());
            snm.setServerId(SocketCache.getMyself().getServerId());
            // Set my permission info so that I can pass the permission verification of other service nodes.
            msgs.setServer(snm);
            msgs.setAp(ap); // Tell other nodes who the information comes from.
            P2pServerEnd.broadcasts(gson.toJson(msgs), msgs);
            // Because the message has been broadcast, the pre stage is finished.
            SocketCache.preNum.put(msgs.getPcm().getReqNum(), 1);
            SocketCache.preIsDone.put(msgs.getPcm().getReqNum(), true);
            // As in the prepre stage, make a method call.
            onCommit(ws, msgs, true);
        }
    }

    /**
     * This method determines whether a node has permission to participate in the pbft consensus process.
     * 
     * @param server Permission information of service node.
     * @return true or false.
     */
    private boolean containServer(ServerNodeModel server) {
        for (ServerNodeModel ser : SocketCache.listServer) {
            if (ser.getAccessKey().equals(server.getAccessKey())
                    && ser.getServerId().equals(server.getServerId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether the prepare message from the other service node is legal. Mainly include:
     *  1. Whether the data hash value is correct, because data may be corrupted during transmission;
     *  2. Whether the view is correct;
     *  3. Whether the request number of this pbft message is greater than the confirmation number(ack) of the current node.
     * 
     * @param pcm The core model of pbft message.
     * @return true or false.
     */
    private boolean preIsValid(PbftContentModel pcm) {
        return SHAUtil.sha256BasedHutool(pcm.getTransaction().toString()).equals(pcm.getDigest())
                && pcm.getViewNum() == SocketCache.getMeta().getView()
                && pcm.getReqNum() > SocketCache.ack.get();
    }

    /**
     * From here on, officially enter the pbft algorithm processing stage.
     * Here, if pbft message(pre-prepare) is detected to be legal, the prepare message is encapsulated and broadcast.
     * 
     * The most important parameter is SocketCache.ppreIsDone, why is it?
     * In my practice, I find that the order of messages is very important.
     * If a message has not been processed in this node, other nodes have already processed messages of the same type,
     * the method of the next message of this node will be triggered. Details are as follows:
     * 
     *  (1/7) The onPrePrepare method of this node is very slow to execute.
     *  (2/7) If the pre-prepare message has not been put into the socketcache.ppre container,
     *  (3/7) the message of the type of prepare broadcast by other nodes will not pass the data verification,
     *  (4/7) and then the pre-prepare message of this method can not enter the prepare stage of pbft algorithm,
     *  (5/7) because the prepare message of other nodes has already been sent, 
     *  (6/7) or after entering the Prepare phase, due to the insufficient number of prepare messages received,
     *  (7/7) the commit message cannot be broadcast, which results in the pbft consensus interruption.
     * 
     * @param ws WebSocket of sender(root node).
     * @param msgs Message entity.
     */
    private void onPrePrepare(WebSocket ws, PbftMsgModel msgs) {
        // If the pre-prepare message is invalid, reject it.
        if (!ppreIsValid(msgs.getPcm())) {
            return;
        }
        // Here, the pre-prepare message is right, and add it into SocketCache.ppreNum.
        SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), 0);
        // Next, assemble the message of prepare type.
        msgs.setMsgType(MsgEnum.prepare);
        ServerNodeModel snm = new ServerNodeModel();
        snm.setAccessKey(SocketCache.getMyself().getAccessKey());
        snm.setServerId(SocketCache.getMyself().getServerId());
        // Set my permission info so that I can pass the permission verification of other service nodes.
        msgs.setServer(snm);
        msgs.setAp(ap);    // Tell other nodes who the information comes from.
        P2pServerEnd.broadcasts(gson.toJson(msgs), msgs);   // Broadcast messages all over the network.
        // Set the SocketCache.ppreIsDone of the current request number to true, 
        // which means the ppre stage of the current request number has been completed.
        SocketCache.ppreIsDone.put(msgs.getPcm().getReqNum(), true);
        /*
         * If the prepare message of nodes in the whole network has not been broadcast, this method call is not necessary.
         * 
         * However, if the prepare message of this node is broadcast last in the whole network, 
         * it means that this node will not receive the prepare message from other nodes after that,
         * then the prepare event of this node cannot be triggered,
         * resulting in the interruption of the pbft algorithm process of the current pbft message request number of this node.
         * 
         * Therefore, in the prepre stage of this node, after setting SocketCache.ppreIsDone,
         * you must call the prepare method once to prevent extreme situations.
         * 
         * However, this method call does not directly deliver pbft messages, so it must be identified by a boolean variable.
         */
        onPrepare(ws, msgs, true);
    }

    /**
     * Determine whether the pre-prepare message from the root node is legal. Mainly include:
     *  1. Whether the data hash value is correct;
     *  2. Whether the view is correct;
     *  3. Whether the request number of this pbft message is greater than the confirmation number(ack) of the current node.
     * 
     * @param pcm The core model of pbft message.
     * @return true or false.
     */
    private boolean ppreIsValid(PbftContentModel pcm) {
        return SHAUtil.sha256BasedHutool(pcm.getTransaction().toString()).equals(pcm.getDigest())
                && pcm.getViewNum() == SocketCache.getMeta().getView()
                && pcm.getReqNum() > SocketCache.ack.get();
    }
    
    /**
     * If a node exits P2P network for any reason, I can receive the latest network metadata from the root node.
     * 
     * @param ws WebSocket of sender(root node).
     * @param msgs Message entity.
     */
    private void onUpdate(WebSocket ws, PbftMsgModel msgs) {
        // We should first verify whether the detective message comes from the root node, and temporarily omit it.
        SocketCache.setMeta(msgs.getMeta());    // Just save it.
    }

    /**
     * Receive detective requests from other nodes as the server of this node.
     * Here I can save the client connection of the other party, so that I can broadcast messages to the other party.
     * 
     * @param ws WebSocket of sender.
     * @param msgs Message entity.
     */
    private void onDetective(WebSocket ws, PbftMsgModel msgs) {
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.confirm);
        // Tell the other party that I sent this message.
        msg.setAp(ap);
        // This next line is just to tell myself that this is a message to the other party,
        // so that I can see it on the console.
        msgs.setMsgType(MsgEnum.confirm);
        // Response the other party by it's WebSocket connection.
        P2pServerEnd.sendMsg(ws, gson.toJson(msg), msgs);
    }

    /**
     * Processing of the service message from the root node.
     * Take out the permission infomation of all service nodes and set the matedata of P2P network.
     * 
     * @param ws WebSocket of sender(root node).
     * @param msgs Message entity.
     */
    private void onService(WebSocket ws, PbftMsgModel msgs) {
        // We should first verify whether the detective message comes from the root node, and temporarily omit it.
        SocketCache.listServer = msgs.getListServer();  // Get the permission info of all services node.
        SocketCache.setMeta(msgs.getMeta());    // Get the metadata of P2P network.
    }

    /**
     * Processing of the init message from the root node.
     * This is mainly to take out my accessKey and make a service request.
     * 
     * @param ws WebSocket of sender(root node).
     * @param msgs Message entity.
     */
    private void onInit(WebSocket ws, PbftMsgModel msgs) {
        // We should first verify whether the detective message comes from the root node, and temporarily omit it.
        // First obtain my accessKey.
        SocketCache.setMyself(msgs.getServer());
        // Then assemble the service type message and request the P2P network information from the root node.
        PbftMsgModel toRoot = new PbftMsgModel();
        toRoot.setMsgType(MsgEnum.service);
        toRoot.setServer(SocketCache.getMyself());
        toRoot.setAp(ap);   // Tell the root node to send it to me.
        P2pServerEnd.sendMsg(ws, gson.toJson(toRoot), toRoot);
    }

    /**
     * Processing of the confirm message.
     * 
     * @param ws WebSocket of sender.
     * @param msgs Message entity.
     */
    private void onConfirm(WebSocket ws, PbftMsgModel msgs) {
        /*
         * Nothing to do, because we just want to acquire WebSocket of the other node.
         */
    }

    /**
     * Get WebSocket of the newly added node.
     * 
     * @param ws WebSocket of sender and it can be used here to determine whether the message is from the root node.
     * @param msgs Message entity.
     */
    private void onNote(WebSocket ws, PbftMsgModel msgs) {
        // We should first verify whether the detective message comes from the root node, and temporarily omit it.
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.detective);
        msg.setAp(ap);  // This parameter is my WebSocket, so that receiver can response me.
        // If msgs.getApm() isn't null, it means that the newly added node is the current node.
        // Then the root node must send the server address list of the existing node and one by one request them.
        // So I can get a broadcast from them.
        if (msgs.getApm() != null) {
            for (AddrPortModel apm : msgs.getApm()) {
                if (apm.getAddr().equals(ap.getAddr()) && apm.getPort() == ap.getPort()) {
                    continue;
                } else {
                    msgs.setMsgType(MsgEnum.detective);
                    msgs.setAp(apm);    // to who.
                    P2pClientEnd.connect(this, "ws:/" + apm.getAddr() + ":" + apm.getPort(), gson.toJson(msg), msgs);
                }
            }
            // If msgs.getApm() is null, it means that the newly added node isn't the current node.
            // Just request it so that I can receive its broadcast.
        } else {
            msgs.setMsgType(MsgEnum.detective);
            msgs.setAp(msgs.getAp());   // to who.
            P2pClientEnd.connect(this, "ws:/" + msgs.getAp().getAddr() + ":" + msgs.getAp().getPort(), gson.toJson(msg), msgs);
        }
    }

}
