package common.java.Config;

import java.io.FileInputStream;
import java.util.Properties;

public class lConfig {
    private static Properties localProp = new Properties();

    static {
        try {
            localProp.load(new FileInputStream("localconfig.properties"));
        } catch (Exception e) {
            // nlogger.logInfo(e, "本地配置文件[localconfig.properties] ...读取异常!");
            localProp = null;
        }
    }

    public static String localConfig(String Session) {//存在本地对应配置
        return localProp != null ? localProp.getProperty(Session, "") : null;
    }
}
