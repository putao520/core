import common.java.Cache.CacheHelper;
import common.java.Thread.ThreadHelper;
import junit.framework.TestCase;


public class testDbAndCache extends TestCase {
    public void TestCacheLocal() {
        // 测试不带配置名本地缓存
        CacheHelper ca = CacheHelper.buildLocalCache();
        ca.set("TestA", "putao520");
        assertEquals(ca.get("TestA"), "putao520", "写入缓存失败");
        ca.setExpire("TestA", 1000);    // 设置缓存过期时间1秒
        ThreadHelper.sleep(3000);
        assertEquals(ca.get("TestA"), null, "过期设置无效");
    }
}
