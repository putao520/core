package common.java.Cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class MemCache<K, V> {
    // 数据刷新线程池
    protected static final ListeningExecutorService refreshPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(20));
    /**
     * @description: 利用guava实现的内存缓存。缓存加载之后永不过期，后台线程定时刷新缓存值。刷新失败时将继续返回旧缓存。
     * 在调用getValue之前，需要设置 refreshDuration， refreshTimeunit， maxSize 三个参数
     * 后台刷新线程池为该系统中所有子类共享，大小为20.
     * @author: luozhuo
     * @date: 2017年6月21日 上午10:03:45
     * @version: V1.0.0
     * @param <K>
     * @param <V>
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    // 缓存自动刷新周期
    protected int refreshDuration = 10;
    // 缓存刷新周期时间格式
    protected TimeUnit refreshTimeunit = TimeUnit.MINUTES;
    // 缓存过期时间（可选择）
    protected int expireDuration = -1;
    // 缓存刷新周期时间格式
    protected TimeUnit expireTimeunit = TimeUnit.HOURS;
    // 缓存最大容量
    protected int maxSize = 4;
    protected Function<K, V> _getValueWhenExpired;

    private LoadingCache<K, V> cache = null;

    public static <K, V> MemCache<K, V> buildMemCache() {
        return new MemCache<>();
    }

    // public abstract void loadValueWhenStarted();

    // protected abstract V getValueWhenExpired(K key) throws Exception;


    /**
     * @param key
     * @throws Exception
     * @description: 从cache中拿出数据操作
     * @author: luozhuo
     * @date: 2017年6月13日 下午5:07:11
     */
    public V getValue(K key) {
        try {
            return getCache().get(key);
        } catch (Exception e) {
            logger.error("从内存缓存中获取内容时发生异常，key: " + key, e);
            // throw e;
        }
        return null;
    }

    public V getValueOrDefault(K key, V defaultValue) {
        try {
            return getCache().get(key);
        } catch (Exception e) {
            logger.error("从内存缓存中获取内容时发生异常，key: " + key, e);
            return defaultValue;
        }
    }

    /**
     * 设置基本属性
     */
    public MemCache<K, V> setRefreshDuration(int refreshDuration) {
        this.refreshDuration = refreshDuration;
        return this;
    }

    public MemCache<K, V> setRefreshTimeUnit(TimeUnit refreshTimeunit) {
        this.refreshTimeunit = refreshTimeunit;
        return this;
    }

    public MemCache<K, V> setExpireDuration(int expireDuration) {
        this.expireDuration = expireDuration;
        return this;
    }

    public MemCache<K, V> setExpireTimeUnit(TimeUnit expireTimeunit) {
        this.expireTimeunit = expireTimeunit;
        return this;
    }

    public MemCache<K, V> setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public MemCache<K, V> setGetValueWhenExpired(Function<K, V> func) {
        this._getValueWhenExpired = func;
        return this;
    }

    public void clearAll() {
        this.getCache().invalidateAll();
    }

    /**
     * @description: 获取cache实例
     * @author: luozhuo
     * @date: 2017年6月13日 下午2:50:11
     */
    private LoadingCache<K, V> getCache() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
                            .maximumSize(maxSize);

                    if (refreshDuration > 0) {
                        cacheBuilder.refreshAfterWrite(refreshDuration, refreshTimeunit);
                    }
                    if (expireDuration > 0) {
                        cacheBuilder.expireAfterWrite(expireDuration, expireTimeunit);
                    }

                    cache = cacheBuilder.build(new CacheLoader<>() {
                        @Override
                        public V load(K key) {
                            return _getValueWhenExpired.apply(key);
                        }

                        @Override
                        public ListenableFuture<V> reload(final K key,
                                                          V oldValue) {
                            return refreshPool.submit(() -> _getValueWhenExpired.apply(key));
                        }
                    });
                }
            }
        }
        return cache;
    }

    @Override
    public String toString() {
        return "GuavaCache";
    }
}
