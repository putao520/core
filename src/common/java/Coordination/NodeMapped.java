package common.java.Coordination;

import common.java.MasterService.MasterConnect;
import common.java.nLogger.nLogger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;

public class NodeMapped {
    private static final CuratorFramework client = MasterConnect.getZookeeperClient();
    private String store;
    private NodeCache nodeCache;

    public static NodeMapped build() {
        return new NodeMapped();
    }

    /**
     * 缓存节点
     */
    public NodeMapped mapped(String path, String nodeName) {
        nodeCache = new NodeCache(client, path + "/" + nodeName, false);
        try {
            nodeCache.start(true);//true代表缓存当前节点
        } catch (Exception e) {
            nLogger.debugInfo(e, "建立节点映射失败");
        }

        // 初始化当前节点数据
        if (nodeCache.getCurrentData() != null) {//只有start中的设置为true才能够直接得到
            store = ZookeeperUnit.byteArray2String(nodeCache.getCurrentData().getData());
        }

        //添加节点数据监听
        nodeCache.getListenable().addListener(() -> store = ZookeeperUnit.byteArray2String(nodeCache.getCurrentData().getData()));
        return this;
    }

    public void release() {
        try {
            nodeCache.close();
        } catch (Exception e) {
        }
    }

    public String getData() {
        return store;
    }

}
