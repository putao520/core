package common.java.httpServer;

import common.java.Reflect._reflect;
import io.netty.channel.ChannelId;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class RequestSession {
    private final static HashMap<ChannelId, ConcurrentHashMap<String, Object>> requestCache = new HashMap<>();
    private final static ThreadLocal<ChannelId> channelID = new ThreadLocal<>();
    private final static ThreadLocal<_reflect> currentClass = new ThreadLocal<>();

    public static final void setCurrent(_reflect _currentClass) {
        currentClass.set(_currentClass);
    }

    public static final _reflect getCurrentClass() {
        return currentClass.get();
    }

    public final static void create(ChannelId cid) {
        requestCache.put(cid, new ConcurrentHashMap<>());
    }

    public final static <T> T getValue(String key) {
        T r = null;
        try {
            r = (T) requestCache.get(channelID.get()).get(key);
        } catch (Exception e) {
            r = null;
        }
        return r;
    }

    public final static <T> void setValue(String key, T val) {
        ChannelId cid = channelID.get();
        if (cid == null) {
            setChannelID(cid);
        }
        requestCache.get(cid).put(key, val);
    }

    public final static void remove(ChannelId cid) {
        requestCache.remove(cid);
    }

    public final static ChannelId getChannelID() {
        return channelID.get();
    }

    public static final void setChannelID(ChannelId cid) {
        channelID.set(cid);
    }
}
