package common.java.Apps;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Apps.Roles.AppRoles;
import common.java.HttpServer.HttpContext;
import common.java.HttpServer.RequestSession;
import common.java.String.StringHelper;
import io.netty.channel.ChannelId;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppContext {
    public static final String SessionKey = "AppContext";
    private int appid;
    private String domain;
    private JSONObject appInfo;
    private ModelServiceConfig msc;
    private HashMap<String, MicroServiceContext> microServiceInfo;
    private AppRoles roles;

    private static final ExecutorService globalService = Executors.newCachedThreadPool();

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
     * 获得当前应用上下文
     */
    public static AppThreadContext virtualAppContext() {
        return AppThreadContext.build(HttpContext.current());
    }

    /**
     * 设置当前线程上下文
     */
    public static AppContext virtualAppContext(AppThreadContext atc) {
        return virtualAppContext(atc.AppID, atc.MicroServiceName);
    }

    /**
     * 根据指定的appid创建虚拟上下文
     */
    public static AppContext virtualAppContext(int appid, String serviceName) {
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
                return o.asLongText().equalsIgnoreCase(this.asLongText()) ? 0 : 1;
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
                MicroServiceContext msc = new MicroServiceContext(this.appid, s);
                if (msc.hasData()) {
                    this.microServiceInfo.put(s, msc);
                }
            }
            this.msc = new ModelServiceConfig(this.appInfo.getJson("configName"));
        }
        this.roles = AppRoles.build(this.appInfo.getJson("roles"));
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
    public int appId() {
        return this.appid;
    }

    /**
     * 获得当前应用角色定义
     */
    public AppRoles roles() {
        return roles;
    }

    /**
     * 获得应用包含的微服务的信息
     */
    public HashMap<String, MicroServiceContext> microServiceInfo() {
        return this.microServiceInfo;
    }

    /**
     * 当前上下文启动新线程
     */
    public AppContext thread(Runnable task) {
        return this.thread(task, null);
    }

    public AppContext thread(Runnable task, ExecutorService service) {
        ExecutorService serv = service == null ? globalService : service;
        AppThreadContext atc = AppContext.virtualAppContext();
        serv.submit(() -> {
            AppContext.virtualAppContext(atc);
            task.run();
        });
        return this;
    }
}
