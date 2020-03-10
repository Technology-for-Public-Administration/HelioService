package tech.feily.unistarts.heliostration.helioservice.pbft;

import java.util.Queue;

import org.java_websocket.WebSocket;

import com.google.common.collect.Queues;
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
                onPrepare(ws, msgs, false);
                break;
            case commit :
                onCommit(ws, msgs, false);
                break;
            default :
                break;
        }
    }

    private void onCommit(WebSocket ws, PbftMsgModel msgs, boolean isThis) {
        if (!isThis) {
            System.out.println("commit here, false");
            /**
             * 如果数据和权限层面有问题，直接拒绝
             */
            if (!comIsValid(msgs.getPcm()) || !containServer(msgs.getServer())) {
                return;
            }
            /**
             * 如果该请求号在本节点尚未出现，那么先将该请求号的第一条commit消息缓存，然后设置该请求号的pre阶段尚未处理完毕
             */
            if (!SocketCache.preIsDone.containsKey(msgs.getPcm().getReqNum())) {
                Queue<PbftMsgModel> que = Queues.newConcurrentLinkedQueue();
                que.add(msgs);
                SocketCache.comQue.put(msgs.getPcm().getReqNum(), que);
                SocketCache.preIsDone.put(msgs.getPcm().getReqNum(), false);
                return;
            /**
             * 这个分支说明该请求号在该节点尚未处理完毕时，其余节点发来了非第一条消息，继续缓存
             */
            } else if (SocketCache.preIsDone.get(msgs.getPcm().getReqNum()) == false) {
                Queue<PbftMsgModel> que = SocketCache.comQue.get(msgs.getPcm().getReqNum());
                que.add(msgs);
                SocketCache.comQue.put(msgs.getPcm().getReqNum(), que);
                return;
            }
            System.out.println("commit here, false, lalala");
            /**
             * 到了这里，那么说明该请求号已经在该节点处理完毕了，那么在缓存队列不为空的情况下先处理队列，然后处理新发来的消息
             */
            Queue<PbftMsgModel> que = SocketCache.comQue.get(msgs.getPcm().getReqNum());
            if (que != null) {
                while (!que.isEmpty()) {
                    /**
                     * 如果是ppre中的第一个。那么先放进入，再将ppreNum置0，否则，直接加即可
                     */
                    if (isFirstCom(msgs.getPcm())) {
                        SocketCache.com.put(msgs.getPcm().getReqNum(), msgs);
                        SocketCache.preNum.put(msgs.getPcm().getReqNum(), 1);
                    } else {
                        SocketCache.preNum.put(msgs.getPcm().getReqNum(), SocketCache.preNum.get(msgs.getPcm().getReqNum()) + 1);
                    }
                    /**
                     * 这里只是为了计数，因为直接释放即可
                     */
                    que.poll();
                }
            }
            /**
             * 执行到这里存在两种情况
             * 一种是que队列有元素，但是已经处理完了
             * 另一种情况是que队列没元素（为null），也就是说这个请求号本节点处理的很快，其余节点的消息只来了这一个，也就是第一个
             * 一样需要先判断
             */
            if (isFirstCom(msgs.getPcm())) {
                SocketCache.com.put(msgs.getPcm().getReqNum(), msgs);
                SocketCache.preNum.put(msgs.getPcm().getReqNum(), 1);
            } else {
                SocketCache.preNum.put(msgs.getPcm().getReqNum(), SocketCache.preNum.get(msgs.getPcm().getReqNum()) + 1);
            }
            /**
             * 如果满足进入下一阶段的条件，就直接单播reply消息
             */
            if (SocketCache.preNum.get(msgs.getPcm().getReqNum()) >= (2 * SocketCache.getMeta().getMaxf() + 1)) {
                PbftMsgModel ret = new PbftMsgModel();
                ret.setMsgType(MsgEnum.reply);
                ret.setAp(ap);
                msgs.setMsgType(MsgEnum.reply);
                msgs.setAp(msgs.getPcm().getAp());
                P2pClientEnd.connect(this, "ws:/" + msgs.getPcm().getAp().getAddr() + ":" + msgs.getPcm().getAp().getPort(), gson.toJson(ret), msgs);
                SocketCache.ack.set(msgs.getPcm().getReqNum());
                remove(msgs);
            }
        } else {
            System.out.println("commit here, true");
            /**
             * 如果是这种情况，那么说明该节点是第一个处理完该请求号的pre阶段的，但是由于是方法调用而非事件触发，所以不能继续执行，直接退出
             */
            if (isFirstCom(msgs.getPcm())) {
                return;
            }
            /**
             * 到了这里，那么说明该请求号已经在该节点处理完毕了，那么在缓存队列不为空的情况下先处理队列，然后处理新发来的消息
             */
            Queue<PbftMsgModel> que = SocketCache.comQue.get(msgs.getPcm().getReqNum());
            if (que != null) {
                while (!que.isEmpty()) {
                    /**
                     * 如果是ppre中的第一个。那么先放进入，再将ppreNum置0，否则，直接加即可
                     */
                    if (isFirstCom(msgs.getPcm())) {
                        SocketCache.com.put(msgs.getPcm().getReqNum(), msgs);
                        SocketCache.preNum.put(msgs.getPcm().getReqNum(), 1);
                    } else {
                        SocketCache.preNum.put(msgs.getPcm().getReqNum(), SocketCache.preNum.get(msgs.getPcm().getReqNum()) + 1);
                    }
                    /**
                     * 这里只是为了计数，因为直接释放即可
                     */
                    que.poll();
                }
            }
            /**
             * 如果满足进入下一阶段的条件，就直接单播reply消息
             */
            if (SocketCache.preNum.get(msgs.getPcm().getReqNum()) >= (2 * SocketCache.getMeta().getMaxf() + 1)) {
                PbftMsgModel ret = new PbftMsgModel();
                ret.setMsgType(MsgEnum.reply);
                ret.setAp(ap);
                msgs.setMsgType(MsgEnum.reply);
                msgs.setAp(msgs.getPcm().getAp());
                P2pClientEnd.connect(this, "ws:/" + msgs.getPcm().getAp().getAddr() + ":" + msgs.getPcm().getAp().getPort(), gson.toJson(ret), msgs);
                SocketCache.ack.set(msgs.getPcm().getReqNum());
                remove(msgs);
            }
        }

    }

    private void remove(PbftMsgModel msgs) {
        SocketCache.ppre.remove(msgs.getPcm().getReqNum());
        SocketCache.ppreNum.remove(msgs.getPcm().getReqNum());
        SocketCache.pre.remove(msgs.getPcm().getReqNum());
        SocketCache.preNum.remove(msgs.getPcm().getReqNum());
        SocketCache.com.remove(msgs.getPcm().getReqNum());
        SocketCache.ppreIsDone.remove(msgs.getPcm().getReqNum());
        SocketCache.preIsDone.remove(msgs.getPcm().getReqNum());
        SocketCache.preQue.remove(msgs.getPcm().getReqNum());
        SocketCache.comQue.remove(msgs.getPcm().getReqNum());
    }
    
    private boolean isFirstCom(PbftContentModel pcm) {
        return !SocketCache.com.containsKey(pcm.getReqNum());
    }

    private boolean comIsValid(PbftContentModel pcm) {
        return SHAUtil.sha256BasedHutool(pcm.getTransaction().toString()).equals(pcm.getDigest())
                && pcm.getViewNum() == SocketCache.getMeta().getView()
                //&& SocketCache.pre.containsKey(pcm.getReqNum())
                && pcm.getReqNum() > SocketCache.ack.get();
    }

    /**
     * 一种极端情况是本请求号所有的消息均早于当前处理到达，所以必须自身触发
     * 
     * @param ws
     * @param msgs
     * @param isThis
     */
    private void onPrepare(WebSocket ws, PbftMsgModel msgs, boolean isThis) {
        if (!isThis) {
            /**
             * 如果数据和权限层面有问题，直接拒绝
             */
            if (!preIsValid(msgs.getPcm()) || !containServer(msgs.getServer())) {
                return;
            }
            /**
             * 如果该请求号在本节点尚未出现，那么先将该请求号的第一条prepare消息缓存，然后设置该请求号的ppre阶段尚未处理完毕
             */
            if (!SocketCache.ppreIsDone.containsKey(msgs.getPcm().getReqNum())) {
                Queue<PbftMsgModel> que = Queues.newConcurrentLinkedQueue();
                que.add(msgs);
                SocketCache.preQue.put(msgs.getPcm().getReqNum(), que);
                SocketCache.ppreIsDone.put(msgs.getPcm().getReqNum(), false);
                return;
            /**
             * 这个分支说明该请求号在该节点尚未处理完毕时，其余节点发来了非第一条消息，继续缓存
             */
            } else if (SocketCache.ppreIsDone.get(msgs.getPcm().getReqNum()) == false) {
                Queue<PbftMsgModel> que = SocketCache.preQue.get(msgs.getPcm().getReqNum());
                que.add(msgs);
                SocketCache.preQue.put(msgs.getPcm().getReqNum(), que);
                return;
            }
            /**
             * 到了这里，那么说明该请求号已经在该节点处理完毕了，那么在缓存队列不为空的情况下先处理队列，然后处理新发来的消息
             */
            Queue<PbftMsgModel> que = SocketCache.preQue.get(msgs.getPcm().getReqNum());
            if (que != null) {
                while (!que.isEmpty()) {
                    /**
                     * 如果是ppre中的第一个。那么先放进入，再将ppreNum置0，否则，直接加即可
                     */
                    if (isFirstPpre(msgs.getPcm())) {
                        SocketCache.pre.put(msgs.getPcm().getReqNum(), msgs);
                        SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), 0);
                    } else {
                        SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) + 1);
                    }
                    /**
                     * 这里只是为了计数，因为直接释放即可
                     */
                    que.poll();
                }
            }
            /**
             * 执行到这里存在两种情况
             * 一种是que队列有元素，但是已经处理完了
             * 另一种情况是que队列没元素（为null），也就是说这个请求号本节点处理的很快，其余节点的消息只来了这一个，也就是第一个
             * 一样需要先判断
             */
            if (isFirstPpre(msgs.getPcm())) {
                SocketCache.pre.put(msgs.getPcm().getReqNum(), msgs);
                SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), 0);
            } else {
                SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) + 1);
            }
            /**
             * 如果满足进入下一阶段的条件，就直接广播commit消息
             */
            if (SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) > 2 * SocketCache.getMeta().getMaxf()) {
                msgs.setMsgType(MsgEnum.commit);
                ServerNodeModel snm = new ServerNodeModel();
                snm.setAccessKey(SocketCache.getMyself().getAccessKey());
                snm.setServerId(SocketCache.getMyself().getServerId());
                msgs.setServer(snm);
                msgs.setAp(ap);
                P2pServerEnd.broadcasts(gson.toJson(msgs), msgs);
                /**
                 * 由于已经广播，那么直接设置该请求号的pre阶段本阶段已经处理完毕
                 */
                SocketCache.preIsDone.put(msgs.getPcm().getReqNum(), true);
                onCommit(ws, msgs, true);
            }
        } else {
            if (isFirstPpre(msgs.getPcm())) {
                return;
            }
            Queue<PbftMsgModel> que = SocketCache.preQue.get(msgs.getPcm().getReqNum());
            if (que != null) {
                while (!que.isEmpty()) {
                    /**
                     * 如果是ppre中的第一个。那么先放进入，再将ppreNum置0，否则，直接加即可
                     */
                    if (isFirstPpre(msgs.getPcm())) {
                        SocketCache.pre.put(msgs.getPcm().getReqNum(), msgs);
                        SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), 0);
                    } else {
                        SocketCache.ppreNum.put(msgs.getPcm().getReqNum(), SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) + 1);
                    }
                    /**
                     * 这里只是为了计数，因为直接释放即可
                     */
                    que.poll();
                }
            }
            if (SocketCache.ppreNum.get(msgs.getPcm().getReqNum()) > 2 * SocketCache.getMeta().getMaxf()) {
                msgs.setMsgType(MsgEnum.commit);
                ServerNodeModel snm = new ServerNodeModel();
                snm.setAccessKey(SocketCache.getMyself().getAccessKey());
                snm.setServerId(SocketCache.getMyself().getServerId());
                msgs.setServer(snm);
                msgs.setAp(ap);
                P2pServerEnd.broadcasts(gson.toJson(msgs), msgs);
                /**
                 * 由于已经广播，那么直接设置该请求号的pre阶段本阶段已经处理完毕
                 */
                SocketCache.preIsDone.put(msgs.getPcm().getReqNum(), true);
                onCommit(ws, msgs, true);
            }
        }
    }


    private boolean isFirstPpre(PbftContentModel pcm) {
        return !SocketCache.ppre.containsKey(pcm.getReqNum()) && !SocketCache.ppreNum.containsKey(pcm.getReqNum());
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
                //&& SocketCache.ppre.containsKey(pcm.getReqNum())
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
        SocketCache.ppreIsDone.put(msgs.getPcm().getReqNum(), true);
        onPrepare(ws, msgs, true);
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
        P2pServerEnd.sendMsg(ws, gson.toJson(msg), msgs);
        //P2pClientEnd.connect(this, "ws:/" + msgs.getAp().getAddr() + ":" + msgs.getAp().getPort(), gson.toJson(msg), msgs);
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
        PbftMsgModel msg = new PbftMsgModel();
        msg.setMsgType(MsgEnum.detective);
        msg.setAp(ap);
        if (msgs.getApm() != null) {
            for (AddrPortModel apm : msgs.getApm()) {
                if (apm.getAddr().equals(ap.getAddr()) && apm.getPort() == ap.getPort()) {
                    continue;
                } else {
                    msgs.setMsgType(MsgEnum.detective);
                    msgs.setAp(apm);
                    P2pClientEnd.connect(this, "ws:/" + apm.getAddr() + ":" + apm.getPort(), gson.toJson(msg), msgs);
                }
            }
        } else {
            msgs.setMsgType(MsgEnum.detective);
            msgs.setAp(msgs.getAp());
            P2pClientEnd.connect(this, "ws:/" + msgs.getAp().getAddr() + ":" + msgs.getAp().getPort(), gson.toJson(msg), msgs);
        }
        /**
         * 
        if (msgs.getAp().getAddr().equals(ws.getLocalSocketAddress().getAddress().toString())
                && msgs.getAp().getPort() == ap.getPort()) {
            SystemUtil.println(msgs);
            return;
        }
         */
    }

}
