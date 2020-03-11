package tech.feily.unistarts.heliostration.helioservice.model;

/**
 * P2P network and pbft algorithm state enumeration.
 * 
 * @author Feily Zhang
 * @version v0.1
 */
public enum MsgEnum {

    hello, detective, confirm,
    init, service, note,
    update, close, error,
    exception, prePrepare,
    prepare, commit, reply;
    
}
