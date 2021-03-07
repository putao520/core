package common.java.MasterService;

import common.java.Config.lConfig;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.apache.curator.framework.CuratorFramework;

/**
 * 获得主zookeeper连接信息
 */
public class MasterConnect {
    private static final Zookeeper ins;

    static {
        String configString = lConfig.localConfig("zookeeper");
        if (StringHelper.invaild(configString)) {
            configString = MasterProxy.serviceName("MasterClient").custom("getClient");
        }
        if (StringHelper.invaild(configString)) {
            nLogger.debugInfo("主控连接信息获得失败!");
        }
        ins = Zookeeper.build(configString);
    }

    public static Zookeeper getMasterClient() {
        return ins;
    }

    public static CuratorFramework getZookeeperClient() {
        return ins.instance();
    }
}
