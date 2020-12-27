package common.java.apps;

import common.java.encrypt.GscJson;
import common.java.httpServer.HttpContext;
import common.java.httpServer.RequestSession;
import common.java.master.MasterProxy;
import common.java.rpc.RpcResponse;
import common.java.serviceHelper.MasterServiceName;
import common.java.string.StringHelper;
import io.netty.channel.ChannelId;
import org.json.simple.JSONObject;

import java.util.HashMap;

public class AppContext {
    public static final String SessionKey = "AppContext";
    private int appid;
    private String domain;
    private JSONObject appInfo;
    private ModelServiceConfig msc;
    private HashMap<String, MicroServiceContext> microServiceInfo;

    private AppContext() {
        // 默认使用当前上下文 或者 0
        init(HttpContext.current().appid());
    }

    public AppContext(int appid) {
        init(appid);
    }

    public AppContext(String domain) {
        init(domain);
    }

    public static AppContext current() {
        AppContext r = RequestSession.getValue(AppContext.SessionKey);
        if (r == null) {
            r = new AppContext();
            RequestSession.setValue(AppContext.SessionKey, r);
        }
        return r;
    }

    /**
     * 根据指定的appid创建虚拟上下文
     */
    public static AppContext virualAppContext(int appid, String serviceName) {
        ChannelId cid = new ChannelId() {
            private final String shortText = StringHelper.createRandomCode(6);
            private final String longText = shortText + "_" + StringHelper.createRandomCode(6);

            @Override
            public String asShortText() {
                return "v_" + shortText;
            }

            @Override
            public String asLongText() {
                return "vl_" + longText;
            }

            @Override
            public int compareTo(ChannelId o) {
                return o.asLongText().equals(this.asLongText()) ? 0 : 1;
            }
        };
        RequestSession.setChannelID(cid);
        RequestSession.create(cid);
        AppContext r = new AppContext(appid);
        RequestSession.setValue(AppContext.SessionKey, r);
        HttpContext.setNewHttpContext()
                .serviceName(serviceName)
                .appid(appid);
        return r;
    }

    private void init(int appid) {
        init(AppsProxy.getAppInfo(appid));
    }

    private void init(String domain) {
        init(AppsProxy.getAppInfo(domain));
    }

    private void init(JSONObject appInfo) {
        this.appInfo = appInfo;
        if (this.appInfo != null) {
            this.appid = this.appInfo.getInt("id");
            this.domain = this.appInfo.getString("domain");
            this.microServiceInfo = new HashMap<>();
            String[] meta = this.appInfo.getString("meta").split(",");
            for (String s : meta) {
                MicroServiceContext msc = new MicroServiceContext(s);
                if (msc.hasData()) {
                    this.microServiceInfo.put(s, msc);
                }
            }
            this.msc = new ModelServiceConfig(this.appInfo.getJson("configName"));
        }
    }

    public boolean hasData() {
        return this.appInfo != null;
    }

    /**
     * 获得应用名称
     */
    public String name() {
        return this.appInfo.getString("name");
    }

    /**
     * 获得应用的域名
     */
    public String domain() {
        return this.domain;
    }

    /**
     * 获得应用的配置
     */
    public ModelServiceConfig config() {
        return this.msc;
    }

    /**
     * 获得当前应用id
     */
    public int appid() {
        return this.appid;
    }

    /**
     * 获得应用包含的微服务的信息
     */
    public HashMap<String, MicroServiceContext> microServiceInfo() {
        return this.microServiceInfo;
    }

    /**
     * 更新应用程序的配置数据
     */
    public boolean updateAppConfig() {
        return RpcResponse.build(
                MasterProxy.serviceName(MasterServiceName.Appliction)
                        .update(
                                String.valueOf(this.appid),
                                GscJson.encode(JSONObject.putx("configName", this.msc.toJson()))
                        )
        ).status();
    }
}
