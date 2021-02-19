package common.java.Cache;

import common.java.Apps.AppContext;
import common.java.Apps.MicroServiceContext;
import common.java.Config.nConfig;
import common.java.Reflect._reflect;
import common.java.nLogger.nLogger;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;


public class Cache implements InterfaceCache {
    // 子缓存会用到该缓存对象
    public static final ConcurrentHashMap<String, Object> CacheClient;

    static {
        CacheClient = new ConcurrentHashMap<>();
    }

    private _reflect _cache;            // 共享缓存抽象对象
    private CaffeineCache _mem_cache;         // 内存缓存对象

    private Cache(boolean global_flag, String inputConfigName) {
        try {
            if (global_flag) {
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
                // 获得共享缓存配置信息
                if (configName == null || configName.equals("")) {
                    nLogger.logInfo("共享缓存配置名称为空或者未配置");
                }
                _cache = getCacheObject(configName);
            } else {
                _cache = null;
            }
            // 获得内存缓存配置此案系
            _mem_cache = getMemCacheObject();
        } catch (Exception e) {
            // TODO: handle exception
            nLogger.logInfo(e, "缓存配置数据获取失败");
        }
    }

    public static Cache getInstance() {
        return new Cache(true, null);
    }

    public static Cache getInstance(String configName) {
        return new Cache(true, configName);
    }

    public static Cache getInstance(boolean global_share) {
        return new Cache(global_share, null);
    }

    public static Cache getInstance(String configName, boolean global_share) {
        return new Cache(global_share, configName);
    }

    private CaffeineCache getMemCacheObject() {
        return new CaffeineCache();
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
                    //缓存名
                    switch (cacheName) {
                        case "redis" -> {
                            _cache = (new _reflect(RedisSingle.class)).newInstance(_configString);
                        }
                        case "redis-cluster" -> {
                            _cache = (new _reflect(RedisCluster.class)).newInstance(_configString);
                        }
                        case "redis-sentinel" -> {
                            _cache = (new _reflect(RedisSentinel.class)).newInstance(_configString);
                        }
                        case "redis-masterslave" -> {
                            _cache = (new _reflect(RedisMasterSlave.class)).newInstance(_configString);
                        }
                        default -> {
                            _cache = new _reflect(CaffeineCache.class);
                        }
                    }
                } else {
                    nLogger.logInfo("Cache配置信息格式错误:" + _configString);
                }
            } else {
                nLogger.logInfo("cache配置信息[" + cN + "]为空:=>" + null);
            }
            _cache.privateMode();//内部调用，启动私有模式
        } catch (Exception e) {
            nLogger.logInfo(e, "连接缓存系统失败! 配置名:[" + cN + "]");
            _cache = null;
        }
        return _cache;
    }

    public String get(String objectName) {
        String r = _mem_cache.get(objectName);
        if (r == null && _cache != null) {
            r = (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
        }
        return r;
    }

    public long inc(String objectName) {
        long r;
        if (_cache != null) {
            r = (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
            _mem_cache.set(objectName, r);
        } else {
            r = _mem_cache.inc(objectName);
        }
        return r;
    }

    public long delete(String objectName) {
        long r = _mem_cache.delete(objectName);
        if (_cache != null) {
            r = (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
        }
        return r;
    }

    public long dec(String objectName) {
        long r;
        if (_cache != null) {
            r = (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
            _mem_cache.set(objectName, r);
        } else {
            r = _mem_cache.dec(objectName);
        }
        return r;
    }

    public long decBy(String objectName, long num) {
        long r;
        if (_cache != null) {
            r = (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
            _mem_cache.set(objectName, r);
        } else {
            r = _mem_cache.decBy(objectName, num);
        }
        return r;
    }

    public long incBy(String objectName, long num) {
        long r;
        if (_cache != null) {
            r = (long) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName);
            _mem_cache.set(objectName, r);
        } else {
            r = _mem_cache.incBy(objectName, num);
        }
        return r;
    }

    public boolean setExpire(String objectName, int expire) {
        boolean r = _mem_cache.setExpire(objectName, expire);
        if (_cache != null) {
            r = (boolean) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, expire);
        }
        return r;
    }

    public String set(String objectName, Object objectValue) {
        String r = _mem_cache.set(objectName, objectValue);
        if (_cache != null) {
            r = (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, objectValue);
        }
        return r;
    }

    /**
     * @param objectName  缓存key
     * @param objectValue 缓存value
     * @param expire      缓存到期时间(秒)
     */
    public String set(String objectName, int expire, Object objectValue) {
        String r = _mem_cache.set(objectName, expire, objectValue);
        if (_cache != null) {
            r = (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, expire, objectValue);
        }
        return r;
    }

    public String getSet(String objectName, int expire, Object objectValue) {
        String r = _mem_cache.getSet(objectName, expire, objectValue);
        if (_cache != null) {
            r = (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, expire, objectValue);
        }
        return r;
    }

    public boolean setNX(String objectName, Object objectValue) {
        boolean r = _mem_cache.setNX(objectName, objectValue);
        if (_cache != null) {
            r = (boolean) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, objectValue);
        }
        return r;
    }

    public String getSet(String objectName, Object objectValue) {
        String r = _mem_cache.getSet(objectName, objectValue);
        if (_cache != null) {
            r = (String) _cache._call(Thread.currentThread().getStackTrace()[1].getMethodName(), objectName, objectValue);
        }
        return r;
    }
}