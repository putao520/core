package common.java.cache;

import common.java.Config.nConfig;
import common.java.Reflect._reflect;
import common.java.apps.AppContext;
import common.java.apps.MicroServiceContext;
import common.java.nlogger.nlogger;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;


// public class Cache implements InterfaceCache{ // 仅校对接口时使用
public class Cache {
    // 子缓存会用到该缓存对象
    public static ConcurrentHashMap<String, Object> CacheClient;

    static {
        CacheClient = new ConcurrentHashMap<>();
    }

    private _reflect _cache;            //缓存抽象对象
    private int _cacheName;             //缓存名

    public Cache() {
        init(null);
    }

    public Cache(String configName) {
        init(configName);
    }

    public static final Cache getInstance() {
        return new Cache();
    }

    public static final Cache getInstance(String configName) {
        return new Cache();
    }

    private _reflect getCacheObject(String cN) {
        String cacheName;
        JSONObject obj;
        String _configString = nConfig.netConfig(cN);
        try {
            if (_configString != null) {
                obj = JSONObject.toJSON(_configString);
                if (obj != null) {
                    cacheName = obj.getString("cacheName");
                    switch (cacheName) {
                        case "redis":
                            _cache = (new _reflect(RedisSingle.class)).newInstance(_configString);
                            _cacheName = cacheType.redis;
                            break;
                        case "redis-cluster":
                            _cache = (new _reflect(RedisCluster.class)).newInstance(_configString);
                            _cacheName = cacheType.redis;
                            break;
                        case "redis-sentinel":
                            _cache = (new _reflect(RedisSentinel.class)).newInstance(_configString);
                            _cacheName = cacheType.redis;
                            break;
                        case "redis-masterslave":
                            _cache = (new _reflect(RedisMasterSlave.class)).newInstance(_configString);
                            _cacheName = cacheType.redis;
                            break;
                        default:
                            _cache = new _reflect(EhCache.class);
                            _cacheName = cacheType.ehcache;
                    }
                } else {
                    nlogger.logInfo("Cache配置信息格式错误 ：" + _configString);
                }
            } else {
                nlogger.logInfo("cache配置信息[" + cN + "]为空:=>" + _configString);
            }
            _cache.privateMode();//内部调用，启动私有模式
        } catch (Exception e) {
            nlogger.logInfo(e, "连接缓存系统失败! 配置名:[" + cN + "]");
            _cache = null;
        }
        return _cache;
    }

    private void init(String inputConfigName) {
        try {
            String configName = null;
            if (inputConfigName == null) {
                if (MicroServiceContext.current().hasData()) {
                    configName = MicroServiceContext.current().config().cache();
                } else if (AppContext.current().hasData()) {
                    configName = AppContext.current().config().cache();
                }
            } else {
                configName = inputConfigName;
            }
            // String configName = inputConfigName == null ? AppContext.current().Config().cache() : inputConfigName;
            if (configName == null || configName.equals("")) {
                nlogger.logInfo("缓存配置丢失");
            }
            _cache = getCacheObject(configName);
        } catch (Exception e) {
            // TODO: handle exception
            nlogger.logInfo(e, "缓存配置读取失败");
        }
    }

    public String get(String objectName) {
        return (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
    }

    public long inc(String objectName) {
        return (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
    }

    public long delete(String objectName) {
        return (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
    }

    public long dec(String objectName) {
        return (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
    }

    public long decby(String objectName, long num) {
        return (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, num);
    }

    public long incby(String objectName, long num) {
        return (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, num);
    }

    public boolean setExpire(String objectName, int expire) {
        return (boolean) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, expire);
    }

    public String set(String objectName, Object objectValue) {
        return (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, objectValue);
    }

    /**
     * @param objectName  缓存key
     * @param objectValue 缓存value
     * @param expire      缓存到期时间(秒)
     */
    public String set(String objectName, Object objectValue, int expire) {
        return (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, expire, objectValue);
    }

    public String getset(String objectName, Object objectValue, int expire) {
        return (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, expire, objectValue);
    }

    public boolean setNX(String objectName, Object objectValue) {
        return (boolean) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, objectValue);
    }

    public String getset(String objectName, Object objectValue) {
        return (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, objectValue);
    }

    public static class cacheType {
        public final static int redis = 1;
        public final static int ehcache = 2;
    }
}