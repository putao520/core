package common.java.Rpc;

import common.java.Apps.MicroServiceContext;
import common.java.EventWorker.EventBus;
import common.java.HttpServer.HttpContext;
import common.java.Node.NodeManage;
import common.java.OAuth.oauthApi;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.RequestBodyEntity;
import kong.unirest.Unirest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;

public class rpc {
    private final String servName;
    private final MicroServiceContext msc;
    private String servPath;
    private HttpContext ctx;
    private boolean needApiAuth;

    private rpc(String servName) {
        this.servName = servName;
        boolean nullContext = false;
        this.needApiAuth = false;
        msc = new MicroServiceContext(this.servName);
    }

    // 静态起步方法
    public static rpc service(String servName) {
        return new rpc(servName);
    }

    public static RpcResponse call(String path, HttpContext ctx, Object... args) {
        return call(path, ctx, false, args);
    }

    public static RpcResponse call(String path, HttpContext ctx, boolean api_auth, Object... args) {
        String url = path;
        // 构造http协议rpc完整地址
        if (!path.toLowerCase().startsWith("http://")) {
            String[] strArr = path.split("/");
            url = rpc.service(strArr[1]).setPath(strArr[2], strArr[3]).toString();
        } else {
            path = path.split("//")[1];
        }
        String[] rArr = path.split("/");
        // 设置请求参数[get]
        // url += (( args != null ) ? ExecRequest.objects2string(args) : "");
        // 创建http对象[get]
        // GetRequest r = Unirest.get( url );
        // 创建http对象[post]
        HttpRequestWithBody r = Unirest.post(url);

        // 构造httpContent
        if (ctx == null) {
            ctx = HttpContext.current();
            if (ctx == null) {
                ctx = HttpContext.newHttpContext();
            }
        }
        // 设置httpHeader环境
        JSONObject requestHeader = ctx.header();
        for (String key : requestHeader.keySet()) {
            r.header(key, requestHeader.getString(key));
        }
        // 设置授权
        if (api_auth) {
            r.header(HttpContext.GrapeHttpHeader.token, oauthApi.getInstance().getApiToken(rArr[1] + "@" + rArr[2] + "@" + rArr[3]));
        }
        // 设置请求参数[post]
        RequestBodyEntity rBody = r.body(args != null ? ExecRequest.objects2poststring(args) : "");
        String rs;
        try {
            rs = rBody.asString().getBody();
        } catch (Exception e) {
            nLogger.debugInfo(e, "服务:[" + path + "] ->连接失败！");
            rs = null;
        }
        return RpcResponse.build(rs);
    }

    /**
     * @apiNote 包含参数的URL的使用
     */
    public static RpcResponse call(String url, HttpContext ctx, boolean api_auth) {
        String[] strArr = url.split("/");
        Object[] args = Arrays.stream(strArr).skip(4).toArray();
        return call(StringHelper.join(strArr, "/", 0, 4), ctx, api_auth, args);
    }

    public static void broadCast(String servPath) {
        broadCast(servPath, HttpContext.current());
    }

    public static void broadCast(String servPath, HttpContext ctx, Object... args) {
        JSONArray nodes = NodeManage.getNodes();
        for (Object obj : nodes) {
            JSONObject info = (JSONObject) obj;
            String host = "http://" + info.getString("ip") + ":" + info.getInt("port");
            try {
                EventBus.event(() -> {
                    String path = host + "/" + StringHelper.build(servPath).trimFrom('/').toString();
                    call(path, ctx, args);
                });
            } catch (InterruptedException e) {
                nLogger.logInfo(e, "广播RPC[" + servPath + "] ...失败!");
            }
        }
    }

    /**
     * 设置自定义http上下文
     */
    public rpc setContext(HttpContext ai) {
        this.ctx = ai;
        return this;
    }

    /**
     * 设置请求path
     */
    public rpc setPath(String className, String actionName) {
        servPath = "/" + className + "/" + actionName;
        return this;
    }

    /**
     * 设置请求path
     */
    public rpc setPath(String rpcURL) {
        servPath = "/" + StringHelper.build(rpcURL).trimFrom('/').toString();
        return this;
    }

    /**
     * 设置授权
     */
    public rpc setApiAuth() {
        needApiAuth = true;
        return this;
    }

    /**
     * 调用RPC
     */
    public RpcResponse call(Object... args) {
        return call(this.toString(), this.ctx, this.needApiAuth, args);
    }

    /**
     * 获得RPC调用URL
     */
    @Override
    public String toString() {
        return "http://" + msc.bestServer() + "/" + this.servName + this.servPath;
    }

    public void broadCast(Object... args) {
        broadCast(this.servPath, this.ctx, args);
    }
}
