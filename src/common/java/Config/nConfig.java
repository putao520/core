/**
 * @author Administrator
 */
/**
 * @author Administrator
 *
 */
package common.java.Config;

import common.java.Coordination.PathMapped;
import common.java.JGrapeSystem.SystemDefined;
import common.java.master.MasterProxy;
import common.java.rpc.RpcResponse;
import common.java.serviceHelper.MasterServiceName;
import common.java.string.StringHelper;
import org.json.simple.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class nConfig {
    private static final String configNodePath = "/configs";
    public static String masterHost;
    public static int masterPort;
    public static String bindip;
    public static int port;
    public static boolean debug;
    public static boolean localDebug;
    public static String nodeID;
    private static PathMapped configs;

    static {
        updateConfig();
    }

    public static void updateConfig() {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("pt520config.properties"));
            masterHost = prop.getProperty("masterhost", "http://127.0.0.1");//read putao520system host url
            masterPort = Integer.valueOf(prop.getProperty("masterport", "80"));
            bindip = prop.getProperty("bindip", "0.0.0.0");//本地服务节点通信Ip
            // 下面是增强选项
            port = Integer.valueOf(prop.getProperty("port", "80"));
            debug = Boolean.valueOf(prop.getProperty("debug", "false")).booleanValue();
            localDebug = Boolean.valueOf(prop.getProperty("localdebug", "false")).booleanValue();
            nodeID = createNodeID(SystemDefined.ip(), port);
            // 添加path映射
        } catch (IOException e) {
            // nlogger.logInfo(e);
        }
    }

    public static final String createNodeID(String ip, int port) {
        return ip + "_" + port;
    }

    // 通过masterproxy从数据库获得配置信息
    public static String netConfig(String session) {
        String rs = lConfig.localConfig(session);
        if (rs == null) {
            rs = shareConfig(session);
        }
        if (StringHelper.invaildString(rs)) {
            JSONObject cfgInfo = RpcResponse.build(MasterProxy.serviceName(MasterServiceName.Setting).find("configname", session)).asJson();
            rs = cfgInfo.getString("configjson");
        }
        return rs;
    }

    // 通过zookeeper获得配置信息
    public static String shareConfig(String session) {
        if (configs == null) {
            // 下面是初始化配置映射
            configs = PathMapped.build().mapped(configNodePath);
        }
        String configString = configs.getData(session);
        if (configString == null) {
            configString = netConfig(session);
        }
        return configString;
    }
}