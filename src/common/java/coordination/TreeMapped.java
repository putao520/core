package common.java.Coordination;

import common.java.master.MasterConnect;
import common.java.nlogger.nlogger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;

import java.util.HashMap;

public class TreeMapped {
    private static final CuratorFramework client = MasterConnect.getZookeeperClient();
    private final HashMap<String, String> store = new HashMap<>();
    private String rootPath;
    private TreeCache treeCache;

    public static final TreeMapped build() {
        return new TreeMapped();
    }

    /**
     * 缓存节点
     */
    public TreeMapped mapped(String path) {
        rootPath = path;
        treeCache = new TreeCache(client, path);
        try {
            treeCache.start();
        } catch (Exception e) {
            nlogger.debugInfo(e, "建立树映射失败");
        }
        //添加错误监听器
        treeCache.getUnhandledErrorListenable().addListener((msg, e) -> {
            nlogger.debugInfo(msg);
        });

        //添加树数据监听
        treeCache.getListenable().addListener((c, e) -> {
            ChildData child = e.getData();
            switch (e.getType()) {
                case NODE_ADDED:
                    // 新增缓存
                    store.put(child.getPath(), ZookeeperUnit.byteArray2Strinf(child.getData()));
                    break;
                case NODE_REMOVED:
                    // 删除缓存
                    store.remove(child.getPath());
                    break;
                case NODE_UPDATED:
                    // 更新缓存
                    store.put(child.getPath(), ZookeeperUnit.byteArray2Strinf(child.getData()));
                    break;
            }
        });
        return this;
    }

    public void release() {
        try {
            treeCache.close();
        } catch (Exception e) {
        }
    }

    // 获得全部的值
    public HashMap<String, String> getDatas() {
        return store;
    }

    // 获得NodePath的值
    public String getData(String nodePath) {
        return store.get(nodePath);
    }

    // 获得root节点的值
    public String getData() {
        return store.get(rootPath);
    }
}
