package common.java.cache;

import common.java.number.NumberHelper;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.time.Duration;
import java.util.Objects;

public class EhCache implements InterfaceCache {
    private static final org.ehcache.Cache<String, String> static_jdc;

    static {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("commonCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class, ResourcePoolsBuilder.heap(8000))
                                .withExpiry(
                                        ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(60))
                                )
                )
                .build();
        cacheManager.init();
        static_jdc = cacheManager.getCache("commonCache", String.class, String.class);
    }

    private org.ehcache.Cache<String, String> jdc = null;

    public EhCache() {
        initEhCache();
    }

    private org.ehcache.Cache getEhCahe() {
        jdc = static_jdc;
        return jdc;
    }

    private void initEhCache() {
        jdc = getEhCahe();
    }

    public String get(String objectName) {
        return jdc.get(objectName);
    }

    /**
     * @param objectName
     * @param expire     秒
     * @return
     */
    @Override
    public boolean setExpire(String objectName, int expire) {
        return true;
    }

    @Override
    public String set(String objectName, Object objectValue) {
        jdc.put(objectName, objectValue.toString());
        return "OK";
    }

    @Override
    public String set(String objectName, int expire, Object objectValue) {
        jdc.put(objectName, objectValue.toString());
        return "OK";
    }

    @Override
    public boolean setNX(String objectName, Object objectValue) {
        boolean rs = true;
        String v = jdc.get(objectName);
        if (v == null) {
            rs = false;
            set(objectName, objectValue);
        }
        return rs;
    }

    @Override
    public String getset(String objectName, Object objectValue) {
        Object rs = jdc.get(objectName);
        if (rs == null) {
            set(objectName, objectValue);
        }
        return Objects.requireNonNull(rs).toString();
    }

    @Override
    public String getset(String objectName, int expire, Object objectValue) {
        Object rs = jdc.get(objectName);
        if (rs == null) {
            set(objectName, expire, objectValue);
        }
        return Objects.requireNonNull(rs).toString();
    }

    @Override
    public long inc(String objectName) {
        long r = NumberHelper.number2long(jdc.get(objectName));
        long i = r + 1;
        set(objectName, String.valueOf(i));
        return i;
    }

    @Override
    public long incby(String objectName, long num) {
        long r = NumberHelper.number2long(jdc.get(objectName));
        long i = r + num;
        set(objectName, String.valueOf(i));
        return i;
    }

    @Override
    public long dec(String objectName) {
        long r = NumberHelper.number2long(jdc.get(objectName));
        long i = r - 1;
        set(objectName, String.valueOf(i));
        return i;
    }

    @Override
    public long decby(String objectName, long num) {
        long r = NumberHelper.number2long(jdc.get(objectName));
        long i = r - num;
        set(objectName, String.valueOf(i));
        return i;
    }

    @Override
    public long delete(String objectName) {
        jdc.remove(objectName);
        return 1;
    }
}
