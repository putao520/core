package common.java.Cache;

import common.java.HttpServer.HttpContext;

public class CacheHelper implements InterfaceCache {
    private final Cache cache;
    private String appPrefix;

    private CacheHelper(String configName, boolean globalShare) {
        cache = Cache.getInstance(configName, globalShare);
        Global(false);
    }

    public static final CacheHelper buildCache() {
        return new CacheHelper(null, true);
    }

    public static final CacheHelper buildCache(String configName, boolean globalShare) {
        return new CacheHelper(configName, globalShare);
    }

    public static final CacheHelper buildCache(boolean globalShare) {
        return new CacheHelper(null, globalShare);
    }

    public static final CacheHelper buildCache(String configName) {
        return new CacheHelper(configName, true);
    }

    public CacheHelper Global(boolean flag) {
        appPrefix = "";
        if (!flag) {
            HttpContext hCtx = HttpContext.current();
            if (hCtx != null) {
                int appID = hCtx.appid();
                appPrefix = appID > 0 ? "_" + appID : "";
            }
        }
        return this;
    }

    private String prefixHook(String key) {
        return key + appPrefix;
    }

    /**
     * 获取缓存数据
     *
     * @param key 缓存键值
     * @return 缓存数据的类型（需转换类型）
     */
    public String get(String key) {
        return cache.get(prefixHook(key));
    }

    /**
     * 获取缓存数据
     *
     * @param key 缓存键值
     * @return 缓存数据的类型（需转换类型）
     */
    @Override
    public String set(String key, int expire, Object value) {
        return cache.set(prefixHook(key), expire, value);
    }

    @Override
    public String set(String key, Object value) {
        return cache.set(prefixHook(key), value);
    }

    @Override
    public boolean setExpire(String objectName, int expire) {
        return cache.setExpire(prefixHook(objectName), expire);
    }

    @Override
    public long dec(String objectName) {
        return cache.dec(prefixHook(objectName));
    }

    @Override
    public long decBy(String objectName, long num) {
        return cache.decBy(prefixHook(objectName), num);
    }

    @Override
    public long inc(String objectName) {
        return cache.inc(prefixHook(objectName));
    }

    @Override
    public long incBy(String objectName, long num) {
        return cache.incBy(prefixHook(objectName), num);
    }


    /**
     * 存入缓存数据
     *
     * @param key   缓存键值
     * @param value 缓存数据
     * @return
     */
    public String getSet(String key, Object value) {
        return cache.getSet(prefixHook(key), value);
    }

    /**
     * 存入缓存数据
     *
     * @param key   缓存键值
     * @param value 缓存数据
     * @return
     */
    public String getSet(String key, int expire, Object value) {
        return cache.getSet(prefixHook(key), expire, value);
    }

    /**
     * 存入缓存数据，
     *
     * @param key   缓存键值
     * @param value 缓存数据
     * @return
     */
    public boolean setNX(String key, Object value) {
        return cache.setNX(prefixHook(key), value);
    }

    /**
     * 删除数据
     *
     * @param key 缓存键值
     */
    public long delete(String key) {
        return cache.delete(prefixHook(key));
    }

}
