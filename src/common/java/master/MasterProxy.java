package common.java.master;

import common.java.Config.nConfig;
import common.java.cache.MemCache;
import common.java.nlogger.nlogger;
import common.java.rpc.ExecRequest;
import common.java.serviceHelper.MicroServiceTemplateInterface;
import common.java.string.StringHelper;
import kong.unirest.Unirest;

import java.util.concurrent.TimeUnit;

/**
 * 与master服务通讯类
 * 与标准master服务接口保持一致
 * PS:需要对结果作缓存支持
 */
public class MasterProxy implements MicroServiceTemplateInterface {
    private static final String hostAndPost;
    private static final MemCache<String, String> caches;

    static {
        hostAndPost = nConfig.masterHost + (nConfig.masterPort == 80 ? "" : ":" + nConfig.masterPort);
        caches = MemCache.<String, String>buildMemCache().setRefreshDuration(120)
                .setRefreshTimeUnit(TimeUnit.SECONDS)
                .setMaxSize(4096)
                .setGetValueWhenExpired(key -> {
                    String[] temp = key.split("#");
                    String url = temp[0];
                    String param = StringHelper.join(temp, "#", 1, -1);
                    return MasterProxy.postRpc(url, param);
                });

    }

    private final String serviceName;

    private MasterProxy(String serviceName) {
        this.serviceName = serviceName;
    }

    public static final MasterProxy serviceName(String serviceName) {
        return new MasterProxy(serviceName);
    }

    private static final String postRpc(String url, String param) {
        String rs;
        try {
            rs = Unirest.post(url).body(param).asString().getBody();
        } catch (Exception e) {
            nlogger.logInfo(e);
            rs = "";
        }
        return rs;
    }

    public String custom(String funcName, Object... args) {
        return callMasterService(funcName, param2postpatam(args));
    }

    private String callMasterService(String aName, String param) {
        String url = hostAndPost + "/GrapeFW/" + serviceName + "/" + aName;
        String rs = "";
        // 如果设置了master port 说明是子服务,需要调用rpc,否则不执行rpc,这里可以尝试本地直接调用rpc
        if (nConfig.masterPort != 0) {
            rs = MasterProxy.caches.getValue(url + "#" + param);
            if (rs == null) {
                rs = MasterProxy.postRpc(url, param);
            }
        }
        return rs;
    }

    private String param2postpatam(Object... args) {
        return ExecRequest.objects2poststring(args);
    }

    @Override
    public String insert(String base64Json) {
        return callMasterService("insert", param2postpatam(base64Json));
    }

    @Override
    public String delete(String uids) {
        return callMasterService("delete", param2postpatam(uids));
    }

    @Override
    public String deleteEx(String cond) {
        return callMasterService("delete", param2postpatam(cond));
    }

    @Override
    public String update(String uids, String base64Json) {
        return callMasterService("delete", param2postpatam(uids, base64Json));
    }

    @Override
    public String updateEx(String base64Json, String cond) {
        return callMasterService("delete", param2postpatam(base64Json, cond));
    }

    @Override
    public String page(int idx, int max) {
        return callMasterService("page", param2postpatam(idx, max));
    }

    @Override
    public String pageEx(int idx, int max, String cond) {
        return callMasterService("page", param2postpatam(idx, max, cond));
    }

    @Override
    public String select() {
        return callMasterService("select", "");
    }

    @Override
    public String selectEx(String cond) {
        return callMasterService("select", param2postpatam(cond));
    }

    @Override
    public String find(String key, String val) {
        return callMasterService("find", param2postpatam(key, val));
    }

    @Override
    public String findEx(String cond) {
        return callMasterService("find", param2postpatam(cond));
    }

    @Override
    public String tree(String cond) {
        return callMasterService("tree", param2postpatam(cond));
    }
}
