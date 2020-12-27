package common.java.master;

import common.java.Config.lConfig;
import common.java.Coordination.Zookeeper;
import common.java.nlogger.nlogger;
import common.java.string.StringHelper;
import org.apache.curator.framework.CuratorFramework;

/**
 * 获得主zookeeper连接信息
 */
public class MasterConnect {
    private static final Zookeeper ins;

    static {
        String configString = lConfig.localConfig("zookeeper");
        if (StringHelper.invaildString(configString)) {
            configString = MasterProxy.serviceName("MasterClient").custom("getClient");
        }
        if (StringHelper.invaildString(configString)) {
            nlogger.debugInfo("主控连接信息获得失败!");
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
