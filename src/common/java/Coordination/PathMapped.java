package common.java.Coordination;

import common.java.master.MasterConnect;
import common.java.nlogger.nlogger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;

import java.util.HashMap;
import java.util.List;

public class PathMapped {
    private static final CuratorFramework client = MasterConnect.getZookeeperClient();
    private final HashMap<String, String> store = new HashMap<>();
    private PathChildrenCache pathCache;
    private boolean loaded = false;

    public static PathMapped build() {
        return new PathMapped();
    }

    /**
     * 缓存节点
     */
    public PathMapped mapped(String path) {
        pathCache = new PathChildrenCache(client, path, true);
        try {
            pathCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);//启动模式
        } catch (Exception e) {
            nlogger.debugInfo(e, "建立目录映射失败");
        }

        //添加目录数据监听
        pathCache.getListenable().addListener((c, e) -> {
            ChildData child = e.getData();
            // 初始化缓存
            // 新增缓存
            // 删除缓存
            // 更新缓存
            switch (e.getType()) {
                case INITIALIZED -> {
                    List<ChildData> initDatas = e.getInitialData();
                    for (ChildData _child : initDatas) {
                        store.put(ZookeeperUnit.nodeName(_child.getPath()), ZookeeperUnit.byteArray2Strinf(_child.getData()));
                    }
                    loaded = true;
                }
                case CHILD_ADDED, CHILD_UPDATED -> store.put(ZookeeperUnit.nodeName(child.getPath()), ZookeeperUnit.byteArray2Strinf(child.getData()));
                case CHILD_REMOVED -> store.remove(ZookeeperUnit.nodeName(child.getPath()));
            }
        });
        return this;
    }

    public void release() {
        try {
            pathCache.close();
        } catch (Exception e) {
        }
    }

    public HashMap<String, String> getDatas() {
        return store;
    }

    public String getData(String nodeName) {
        while (!loaded) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
        return store.get(nodeName);
    }
}
