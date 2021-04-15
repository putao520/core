package common.java.Config;

import common.java.JGrapeSystem.SystemDefined;
import common.java.MasterProxy.MasterActor;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class Config {
    private static final String configNodePath = "configs";
    public static String masterHost;
    public static int masterPort;
    public static String bindIP;
    public static int port;
    public static final String masterId_Key = "MasterId";
    public static String nodeID;

    public static String masterId;
    public static String masterPass;

    private static String configPath = "gfw.cfg";
    private static MasterActor configs;
    public static final String masterPass_Key = "MasterPass";
    // public static int centerPort;
    public static String serviceName;

    static {
        updateConfig();
        configs = null;
    }

    private static void setConfigPath(String newConfigPath) {
        configPath = newConfigPath;
    }

    private static MasterActor getNetConfigHandle() {
        if (configs == null) {
            configs = MasterActor.getInstance(configNodePath);
        }
        return configs;
    }

    private static Properties loadProps() {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(configPath));
        } catch (IOException e) {
            nLogger.logInfo(e, "配置文件[" + configPath + "]不存在");
        }
        return prop;
    }

    public static void updateConfig() {
        Properties prop = loadProps();
        // 必选项
        port = Integer.parseInt(prop.getProperty("port", "80"));
        // 附加选项
        // centerPort = Integer.parseInt(prop.getProperty("centerPort", "80"));
        serviceName = prop.getProperty("name", "default");
        // 可选项
        masterHost = prop.getProperty("MasterHost", "http://127.0.0.1");//read putao520system host url
        masterPort = Integer.parseInt(prop.getProperty("MasterPort", "80"));
        bindIP = prop.getProperty("BindIP", "0.0.0.0");//本地服务节点通信Ip

        masterId = prop.getProperty(masterId_Key);
        masterPass = prop.getProperty(masterPass_Key);
        // 自动生成
        nodeID = createNodeID(SystemDefined.ip(), port);
    }

    public static String createNodeID(String ip, int port) {
        return ip + "_" + port;
    }

    // 通过masterProxy从数据库获得配置信息
    public static String netConfig(String session) {
        JSONObject rs = getNetConfigHandle().getDataByIndex("name", session);
        if (JSONObject.isInvalided(rs)) {
            throw new RuntimeException("配置[" + session + "] ->不存在!");
        }
        return rs.getJson("config").toJSONString();
    }

    public static void set(String key, Object val) {
        Properties prop = loadProps();
        prop.setProperty(key, StringHelper.toString(val));
        updateConfig();
    }

    public static String config(String session) {
        return loadProps().getProperty(session);
    }
}