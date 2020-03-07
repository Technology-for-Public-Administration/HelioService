package tech.feily.unistarts.heliostration.helioservice.p2p;

import java.util.List;
import java.util.Set;

import org.java_websocket.WebSocket;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import tech.feily.unistarts.heliostration.helioservice.model.MetaModel;
import tech.feily.unistarts.heliostration.helioservice.model.ServerNodeModel;

/**
 * Cache information for the current node.
 * 
 * @author Feily zhang
 * @version v.01
 */
public class SocketCache {
    
    /**
     * Cache all ws connected to this root node.
     */
    public static Set<WebSocket> wss = Sets.newConcurrentHashSet();

    /**
     * Cache network state, initialized according to root node response.
     */
    private static MetaModel metaModel = new MetaModel();
    
    private static ServerNodeModel myself = new ServerNodeModel();
    
    /**
     * The current session permission information of all nodes is initialized according to the root node response.
     */
    public static List<ServerNodeModel> listServer = Lists.newArrayList();
    
    /**
     * @return the myself
     */
    public static ServerNodeModel getMyself() {
        return myself;
    }

    /**
     * @param myself the myself to set
     */
    public static void setMyself(ServerNodeModel myself) {
        SocketCache.myself = myself;
    }

    /**
     * Iinitialized metaModel.
     * 
     * @param meta
     */
    public static void setMeta(MetaModel meta) {
        SocketCache.metaModel = meta;
    }
    
    /**
     * The following implements atomic operations for all MetaModels.
     */
    public static MetaModel getMeta() {
        synchronized (SocketCache.class) {
            return metaModel;
        }
    }
    
    public static MetaModel getAndIncre() {
        synchronized (SocketCache.class) {
            MetaModel meta = metaModel;
            metaModel.setIndex(metaModel.getIndex() + 1);
            metaModel.setSize(metaModel.getSize() + 1);
            metaModel.setMaxf((metaModel.getSize() - 1) / 3);
            return meta;
        }
    }
    
    public static MetaModel getAndMinus() {
        synchronized (SocketCache.class) {
            MetaModel meta = metaModel;
            metaModel.setIndex(metaModel.getIndex() - 1);
            metaModel.setSize(metaModel.getSize() - 1);
            metaModel.setMaxf((metaModel.getSize() - 1) / 3);
            return meta;
        }
    }

    
    public static MetaModel increAndGet() {
        synchronized (SocketCache.class) {
            metaModel.setIndex(metaModel.getIndex() + 1);
            metaModel.setSize(metaModel.getSize() + 1);
            metaModel.setMaxf((metaModel.getSize() - 1) / 3);
            return metaModel;
        }
    }
    
    public static MetaModel minusAndGet() {
        synchronized (SocketCache.class) {
            metaModel.setIndex(metaModel.getIndex() - 1);
            metaModel.setSize(metaModel.getSize() - 1);
            metaModel.setMaxf((metaModel.getSize() - 1) / 3);
            return metaModel;
        }
    }
    
}
