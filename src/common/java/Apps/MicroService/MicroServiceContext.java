package common.java.Apps.MicroService;

import common.java.Apps.AppsProxy;
import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.Apps.MicroService.Model.MicroModel;
import common.java.Apps.MicroService.Model.MicroModelArray;
import common.java.HttpServer.HttpContext;
import org.json.gsc.JSONObject;

import java.util.HashMap;

public class MicroServiceContext {
    private static int currentno = 0;
    private String servName;
    private int appId;
    private JSONObject servInfo;
    private ModelServiceConfig servConfig;
    private MicroModelArray servModelInfo;

    private MicroServiceContext() {
        // 获得当前微服务名
        HttpContext ctx = HttpContext.current();
        init(ctx.appid(), ctx.serviceName());
    }

    public MicroServiceContext(int appId, String servName) {
        init(appId, servName);
    }

    public MicroServiceContext(String servName) {
        init(HttpContext.current().appid(), servName);
    }

    public static MicroServiceContext current() {
        return new MicroServiceContext();
    }

    private void init(int appId, String servName) {
        this.appId = appId;
        this.servName = servName;
        // 获得对应微服务信息
        this.servInfo = AppsProxy.getServiceInfo(this.appId, this.servName);
        if (this.servInfo != null) {
            this.servModelInfo = new MicroModelArray(this.servInfo.getJson("dataModel"));
            this.servConfig = new ModelServiceConfig(this.servInfo.getJson("config"));
        }
    }

    /**
     * 判断是否是有效对象
     */
    public boolean hasData() {
        return this.servModelInfo != null;
    }

    /**
     * 获得最佳服务节点
     */
    public String bestServer() {
        String[] servers = servInfo.getString("peerAddr").split(",");
        currentno++;
        return servers[currentno % servers.length];
    }

    /**
     * 获得微服务对应的JAR名称
     */
    /*
    public String serviceFileName() {
        return servInfo.getString("serviceTarget");
    }
    */

    /**
     * 获得微服务的配置
     */
    public ModelServiceConfig config() {
        return this.servConfig;
    }

    /**
     * 获得微服务的业务模型
     */
    public MicroModel model(String modelName) {
        return this.servModelInfo.microModel(modelName);
    }
    /**
     * 获得全部微服务的业务模型
     */
    public HashMap<String, MicroModel> model() {
        return this.servModelInfo.microModel();
    }

    /**
     * 是否处于调试状态的服务
     */
    public boolean isDebug() {
        return this.servInfo.getBoolean("debug");
    }

    /**
     * 更新数据模型
     */
    /*
    public boolean updateMicroModel() {
        boolean rb = false;
        // 更新模型定义
        JSONObject newMicroModel = this.servModelInfo.toJson();
        if (RpcResponse.build(MasterProxy.serviceName(MasterServiceName.MicroService)
                .updateEx(
                        GscJson.encode(newMicroModel),
                        DbFilter.buildDbFilter().eq("serviceName", this.servName).eq("appid", this.appId).build().toJSONString()
                )
        ).status()) {
            // 更新全部因为权限模型修改,而需要修改的数据
            this.servModelInfo.forEach((key, mModel) -> mModel.updatePermInfo());
            rb = true;
        }
        return rb;
    }
    */
}
