package common.java.cache;

import common.java.httpServer.HttpContext;

public class CacheHelper {
    private final Cache cache;
    private boolean globalMode = false;

    //
    public CacheHelper() {
        //Cache = new RedisSingle(nodeString);
        cache = new Cache();
        reinit();
    }

    public CacheHelper(String nodeString) {
        cache = new Cache(nodeString);
        reinit();
    }

    public CacheHelper Global() {
        globalMode = true;
        return this;
    }

    private void reinit() {
        globalMode = false;
    }

    private String bindApp() {
        int appid = HttpContext.current().appid();
        return appid > 0 ? "_" + appid : "";
    }

    private String perfixHook(String key) {
        if (!globalMode) {
            key = key + bindApp();
        }
        reinit();
        return key;
    }

    /**
     * 获取缓存数据
     *
     * @param key 缓存键值
     * @return 缓存数据的类型（需转换类型）
     */
    public String get(String key) {
        key = perfixHook(key);
        return cache.get(key);
    }

    /**
     * 获取缓存数据
     *
     * @param key 缓存键值
     * @return 缓存数据的类型（需转换类型）
     */
    public String set(String key, Object value, int expire) {
        key = perfixHook(key);
        return cache.set(key, value, expire);
    }

    /**
     * 存入缓存数据
     *
     * @param key   缓存键值
     * @param value 缓存数据
     * @return
     */
    public String getSet(String key, Object value) {
        key = perfixHook(key);
        return cache.getset(key, value);
    }

    /**
     * 存入缓存数据
     *
     * @param key   缓存键值
     * @param value 缓存数据
     * @return
     */
    public String getSet(String key, Object value, int expire) {
        key = perfixHook(key);
        return cache.getset(key, value, expire);
    }

    /**
     * 存入缓存数据，
     *
     * @param key   缓存键值
     * @param value 缓存数据
     * @return
     */
    public boolean setNX(String key, Object value) {
        key = perfixHook(key);
        return cache.setNX(key, value);
    }

    /**
     * 删除数据
     *
     * @param key 缓存键值
     */
    public void delete(String key) {
        key = perfixHook(key);
        cache.delete(key);

    }

}
