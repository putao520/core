package common.java.apps;

import common.java.database.DbFilter;
import common.java.encrypt.GscJson;
import common.java.httpServer.HttpContext;
import common.java.master.MasterProxy;
import common.java.rpc.RpcResponse;
import common.java.serviceHelper.MasterServiceName;
import org.json.simple.JSONObject;

public class MicroServiceContext {
    private static int currentno = 0;
    private String servName;
    private JSONObject servInfo;
    private ModelServiceConfig servConfig;
    private MicroModelArray servModelInfo;

    private MicroServiceContext() {
        // 获得当前微服务名
        String servName = HttpContext.current().serviceName();
        init(servName);
    }

    public MicroServiceContext(String servName) {
        init(servName);
    }

    public static final MicroServiceContext current() {
        return new MicroServiceContext();
    }

    private void init(String servName) {
        this.servName = servName;
        // 获得对应微服务信息
        this.servInfo = AppsProxy.getServiceInfo(this.servName);
        if (this.servInfo != null) {
            this.servModelInfo = new MicroModelArray(this.servInfo.getJson("tableConfig"));
            this.servConfig = new ModelServiceConfig(this.servInfo.getJson("configName"));
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
        String[] servers = servInfo.getString("url").split(",");
        currentno++;
        return servers[currentno % servers.length];
    }

    /**
     * 获得微服务对应的JAR名称
     */
    public String serviceFileName() {
        return servInfo.getString("serviceTarget");
    }

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
     * 是否处于调试状态的服务
     */
    public boolean isDebug() {
        return this.servInfo.getBoolean("debug");
    }

    /**
     * 更新数据模型
     */
    public boolean updateMicroModel() {
        boolean rb = false;
        // 更新模型定义
        JSONObject newMicroModel = this.servModelInfo.toJson();
        if (RpcResponse.build(MasterProxy.serviceName(MasterServiceName.MicroService)
                .updateEx(
                        GscJson.encode(newMicroModel),
                        DbFilter.buildDbFilter().eq("serviceName", this.servName).build().toJSONString()
                )
        ).status()) {
            // 更新全部因为权限模型修改,而需要修改的数据
            this.servModelInfo.forEach((key, mModel) -> mModel.updatePermInfo());
            rb = true;
        }
        return rb;
    }
}
