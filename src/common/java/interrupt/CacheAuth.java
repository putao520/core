package common.java.interrupt;

import common.java.cache.CacheHelper;

public class CacheAuth {
    private final CacheHelper cache = new CacheHelper();//使用当前服务的缓存

    public boolean breakRun(String ssid, String code) {
        cache.getSet(ssid, code, 30);
        return true;
    }

    public boolean resumeRun(String ssid, String code) {
        String rString = cache.get(ssid);
        boolean rb = rString != null && rString.equals(code);
        if (rb) {
            cache.delete(ssid);
        }
        return rb;
    }
}
