import common.java.Cache.CacheHelper;
import common.java.Thread.ThreadHelper;
import junit.framework.TestCase;
import org.junit.Test;


public class testDbAndCache extends TestCase {
    // 测试不带配置名本地缓存
    @Test
    public void testCacheLocal() {
        CacheHelper ca = CacheHelper.buildLocalCache();
        ca.set("TestA", "putao520");
        assertTrue(ca.get("TestA").equals("putao520"));
        ca.setExpire("TestA", 1000);    // 设置缓存过期时间1秒
        ThreadHelper.sleep(3000);
        assertTrue(ca.get("TestA") == null);
    }
}
