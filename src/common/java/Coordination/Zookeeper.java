package common.java.Coordination;

import common.java.nLogger.nLogger;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.json.simple.JSONObject;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

public class Zookeeper {
    private static final HashMap<String, CuratorFramework> clientCache = new HashMap();
    private String username;
    private String password;
    private List<ACL> authInfo;
    private CuratorFramework client;

    private Zookeeper(String configString) {
        client = getClient(configString);
    }

    public static Zookeeper build(String configString) {
        return new Zookeeper(configString);
    }

    public CuratorFramework instance() {
        return this.client;
    }

    private CuratorFramework getClient(String configString) {
        JSONObject configInfo = JSONObject.toJSON(configString);
        client = clientCache.get(configString);
        JSONObject authInfo = configInfo.getJson("auth");
        if (client == null) {
            String connectString = configInfo.getString("nodes");
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
            if (!JSONObject.isInvaild(authInfo)) {
                this.username = authInfo.getString("username");
                this.password = authInfo.getString("password");
                builder.authorization("digest", (this.username + ":" + this.password).getBytes());
            }
            if (configInfo.containsKey("sessionTimeout")) {
                builder.sessionTimeoutMs(configInfo.getInt("sessionTimeout"));
            }
            if (configInfo.containsKey("connectTimeout")) {
                builder.connectionTimeoutMs(configInfo.getInt("connectTimeout"));
            }
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
            client = builder.namespace("GrapeFW3").connectString(connectString).retryPolicy(retryPolicy).build();
            clientCache.put(configString, client);
        }
        if (client != null) {
            client.start();
        }
        return client;
    }

    public Zookeeper setACL() {
        return setACL(ZooDefs.Perms.ALL);
    }

    public Zookeeper setACL(int perms) {
        try {
            authInfo = ZooDefs.Ids.READ_ACL_UNSAFE;
            Id user = new Id("digest", AclUtils.getDigestUserPwd(username + ":" + password));
            authInfo.add(new ACL(perms, user));
        } catch (Exception e) {
            authInfo = null;
        }
        return this;
    }

    private CreateBuilder creater() {
        CreateBuilder creater = client.create();
        if (authInfo != null) {
            creater.withACL(authInfo);
        }
        return creater;
    }

    /**
     * 创建永久Zookeeper节点
     *
     * @param nodePath  节点路径（如果父节点不存在则会自动创建父节点），如：/curator
     * @param nodeValue 节点数据
     * @return java.lang.String 返回创建成功的节点路径
     */
    public String createPersistentNode(String nodePath, String nodeValue) {
        try {
            return creater().creatingParentsIfNeeded()
                    .forPath(nodePath, nodeValue.getBytes());
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("创建永久Zookeeper节点失败,nodePath:{0},nodeValue:{1}", nodePath, nodeValue));
        }
        return null;
    }

    /**
     * 创建永久有序Zookeeper节点
     *
     * @param nodePath  节点路径（如果父节点不存在则会自动创建父节点），如：/curator
     * @param nodeValue 节点数据
     * @return java.lang.String 返回创建成功的节点路径     * @date 2018/8/1 14:31
     */
    public String createSequentialPersistentNode(String nodePath, String nodeValue) {
        try {
            return creater().creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT_SEQUENTIAL)
                    .forPath(nodePath, nodeValue.getBytes());
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("创建永久有序Zookeeper节点失败,nodePath:{0},nodeValue:{1}", nodePath, nodeValue));
        }
        return null;
    }

    /**
     * 创建临时Zookeeper节点
     *
     * @param nodePath  节点路径（如果父节点不存在则会自动创建父节点），如：/curator
     * @param nodeValue 节点数据
     * @return java.lang.String 返回创建成功的节点路径     * @date 2018/8/1 14:31
     */
    public String createEphemeralNode(String nodePath, String nodeValue) {
        try {
            return creater().creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(nodePath, nodeValue.getBytes());
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("创建临时Zookeeper节点失败,nodePath:{0},nodeValue:{1}", nodePath, nodeValue));
        }
        return null;
    }

    /**
     * 创建临时有序Zookeeper节点
     *
     * @param nodePath  节点路径（如果父节点不存在则会自动创建父节点），如：/curator
     * @param nodeValue 节点数据
     * @return java.lang.String 返回创建成功的节点路径     * @date 2018/8/1 14:31
     */
    public String createSequentialEphemeralNode(String nodePath, String nodeValue) {
        try {
            return creater().creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(nodePath, nodeValue.getBytes());
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("创建临时有序Zookeeper节点失败,nodePath:{0},nodeValue:{1}", nodePath, nodeValue));
        }
        return null;
    }

    /**
     * 检查Zookeeper节点是否存在
     *
     * @param nodePath 节点路径
     * @return boolean 如果存在则返回true     * @date 2018/8/1 17:06
     */
    public boolean checkExists(String nodePath) {
        try {
            Stat stat = client.checkExists().forPath(nodePath);
            return stat != null;
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("检查Zookeeper节点是否存在出现异常,nodePath:{0}", nodePath));
        }
        return false;
    }

    /**
     * 获取某个Zookeeper节点的所有子节点
     *
     * @param nodePath 节点路径
     * @return java.util.List<java.lang.String> 返回所有子节点的节点名     * @date 2018/8/1 17:06
     */
    public List<String> getChildren(String nodePath) {
        try {
            return client.getChildren().forPath(nodePath);
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("获取某个Zookeeper节点的所有子节点出现异常,nodePath:{0}", nodePath));
        }
        return null;
    }

    /**
     * 获取某个Zookeeper节点的数据
     *
     * @param nodePath 节点路径
     * @return java.lang.String     * @date 2018/8/1 17:06
     */
    public String getData(String nodePath) {
        try {
            return new String(client.getData().forPath(nodePath));
        } catch (Exception e) {
            // nLogger.debugInfo(e, MessageFormat.format("获取某个Zookeeper节点的数据出现异常,nodePath:{0}",nodePath));
        }
        return null;
    }

    /**
     * 设置某个Zookeeper节点的数据
     *
     * @param nodePath 节点路径     * @date 2018/8/1 17:06
     */
    public void setData(String nodePath, String newNodeValue) {
        try {
            client.setData().forPath(nodePath, newNodeValue.getBytes());
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("设置某个Zookeeper节点的数据出现异常,nodePath:{0}", nodePath));
        }
    }

    /**
     * 删除某个Zookeeper节点
     *
     * @param nodePath 节点路径     * @date 2018/8/1 17:06
     */
    public void delete(String nodePath) {
        try {
            client.delete().guaranteed().forPath(nodePath);
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("删除某个Zookeeper节点出现异常,nodePath:{0}", nodePath));
        }
    }

    /**
     * 级联删除某个Zookeeper节点及其子节点
     *
     * @param nodePath 节点路径     * @date 2018/8/1 17:06
     */
    public void deleteChildrenIfNeeded(String nodePath) {
        try {
            client.delete().guaranteed().deletingChildrenIfNeeded().forPath(nodePath);
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("级联删除某个Zookeeper节点及其子节点出现异常,nodePath:{0}", nodePath));
        }
    }

    /**
     * <p><b>注册节点监听器</b></p>
     * NodeCache: 对一个节点进行监听，监听事件包括指定路径的增删改操作
     *
     * @param nodePath 节点路径
     * @return void
     */
    public NodeCache registerNodeCacheListener(String nodePath) {
        try {
            //1. 创建一个NodeCache
            NodeCache nodeCache = new NodeCache(client, nodePath);            //2. 添加节点监听器
            nodeCache.getListenable().addListener(() -> {
                ChildData childData = nodeCache.getCurrentData();
                if (childData != null) {
                    System.out.println("Path: " + childData.getPath());
                    System.out.println("Stat:" + childData.getStat());
                    System.out.println("Data: " + new String(childData.getData()));
                }
            });            //3. 启动监听器
            nodeCache.start();            //4. 返回NodeCache
            return nodeCache;
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("注册节点监听器出现异常,nodePath:{0}", nodePath));
        }
        return null;
    }

    /**
     * <p><b>注册子目录监听器</b></p>
     * PathChildrenCache：对指定路径节点的一级子目录监听，不对该节点的操作监听，对其子目录的增删改操作监听
     *
     * @param nodePath 节点路径
     * @param listener 监控事件的回调接口
     * @return org.apache.curator.framework.recipes.cache.PathChildrenCache
     */
    public PathChildrenCache registerPathChildListener(String nodePath, PathChildrenCacheListener listener) {
        try {
            //1. 创建一个PathChildrenCache
            PathChildrenCache pathChildrenCache = new PathChildrenCache(client, nodePath, true);            //2. 添加目录监听器
            pathChildrenCache.getListenable().addListener(listener);            //3. 启动监听器
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);            //4. 返回PathChildrenCache
            return pathChildrenCache;
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("注册子目录监听器出现异常,nodePath:{0}", nodePath));
        }
        return null;
    }

    /**
     * <p><b>注册目录监听器</b></p>
     * TreeCache：综合NodeCache和PathChildrenCahce的特性，可以对整个目录进行监听，同时还可以设置监听深度
     *
     * @param nodePath 节点路径
     * @param maxDepth 自定义监控深度
     * @param listener 监控事件的回调接口
     * @return org.apache.curator.framework.recipes.cache.TreeCache
     */
    public TreeCache registerTreeCacheListener(String nodePath, int maxDepth, TreeCacheListener listener) {
        try {
            //1. 创建一个TreeCache
            TreeCache treeCache = TreeCache.newBuilder(client, nodePath)
                    .setCacheData(true)
                    .setMaxDepth(maxDepth)
                    .build();            //2. 添加目录监听器
            treeCache.getListenable().addListener(listener);            //3. 启动监听器
            treeCache.start();            //4. 返回TreeCache
            return treeCache;
        } catch (Exception e) {
            nLogger.debugInfo(e, MessageFormat.format("注册目录监听器出现异常,nodePath:{0},maxDepth:{1}",nodePath));
        }
        return null;
    }
}
