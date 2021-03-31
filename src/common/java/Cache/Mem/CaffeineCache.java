package common.java.Cache.Mem;

import com.github.benmanes.caffeine.cache.Caffeine;
import common.java.Cache.Common.InterfaceCache;
import common.java.Number.NumberHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CaffeineCache implements InterfaceCache {
    private static final com.github.benmanes.caffeine.cache.Cache<String, String> static_jdc;

    static {
        static_jdc = Caffeine.newBuilder()
                .maximumSize(1000000)
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();
    }

    private com.github.benmanes.caffeine.cache.Cache<String, String> jdc;

    public CaffeineCache() {
        jdc = getCache();
    }

    private com.github.benmanes.caffeine.cache.Cache getCache() {
        jdc = static_jdc;
        return jdc;
    }

    public String get(String objectName) {
        return jdc.getIfPresent(objectName);
    }

    public JSONObject getJson(String objectName) {
        return JSONObject.toJSON(jdc.getIfPresent(objectName));
    }

    public JSONArray getJsonArray(String objectName) {
        return JSONArray.toJSONArray(jdc.getIfPresent(objectName));
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
        String v = jdc.getIfPresent(objectName);
        if (v == null) {
            rs = false;
            set(objectName, objectValue);
        }
        return rs;
    }

    @Override
    public String getSet(String objectName, Object objectValue) {
        Object rs = jdc.getIfPresent(objectName);
        if (rs == null) {
            set(objectName, objectValue);
        }
        return Objects.requireNonNull(rs).toString();
    }

    @Override
    public String getSet(String objectName, int expire, Object objectValue) {
        Object rs = jdc.getIfPresent(objectName);
        if (rs == null) {
            set(objectName, expire, objectValue);
        }
        return Objects.requireNonNull(rs).toString();
    }

    @Override
    public long inc(String objectName) {
        long r = NumberHelper.number2long(jdc.getIfPresent(objectName));
        long i = r + 1;
        set(objectName, String.valueOf(i));
        return i;
    }

    @Override
    public long incBy(String objectName, long num) {
        long r = NumberHelper.number2long(jdc.getIfPresent(objectName));
        long i = r + num;
        set(objectName, String.valueOf(i));
        return i;
    }

    @Override
    public long dec(String objectName) {
        long r = NumberHelper.number2long(jdc.getIfPresent(objectName));
        long i = r - 1;
        set(objectName, String.valueOf(i));
        return i;
    }

    @Override
    public long decBy(String objectName, long num) {
        long r = NumberHelper.number2long(jdc.getIfPresent(objectName));
        long i = r - num;
        set(objectName, String.valueOf(i));
        return i;
    }

    @Override
    public long delete(String objectName) {
        jdc.invalidate(objectName);
        return 1;
    }
}
