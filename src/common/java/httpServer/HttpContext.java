package common.java.httpServer;

import common.java.number.NumberHelper;
import common.java.rpc.rMsg;
import common.java.string.StringHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.AsciiString;
import org.json.simple.JSONObject;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class HttpContext {
    public static final JSONObject methodStore;
    public static final String SessionKey = "HttpContext";

    static {
        methodStore = new JSONObject();
        methodStore.put("get", Method.get);
        methodStore.put("post", Method.post);
        methodStore.put("websocket", Method.websocket);
    }

    private Method method;
    private HttpRequest request;
    private String absPath;
    private String svrName;
    private JSONObject parameter;
    private AsciiString mime;
    private ChannelHandlerContext ctx;
    private JSONObject values = new JSONObject();

    private HttpContext() {
    }

    public HttpContext(ChannelHandlerContext _ctx) {
        ctx = _ctx;
    }

    public HttpContext(HttpRequest _header) {
        initHttpRequest(_header);
    }

    public HttpContext(JSONObject _header) {
        JSONObject headerJson = _header.getJson(GrapeHttpHeader.WebSocket.header);
        if (headerJson != null) {
            for (String key : GrapeHttpHeader.keys) {
                updateValue(headerJson, key);
            }
        }
        parameter(_header.getJson(GrapeHttpHeader.WebSocket.param));
        absPath = _header.getString(GrapeHttpHeader.WebSocket.url);
        method = Method.websocket;
    }

    public static HttpContext current() {
        HttpContext r = RequestSession.getValue(HttpContext.SessionKey);
        return r;
    }

    public static HttpContext newHttpContext() {
        return new HttpContext();
    }

    public static HttpContext setNewHttpContext() {
        HttpContext httpCtx = new HttpContext();
        RequestSession.setValue(HttpContext.SessionKey, httpCtx);
        return httpCtx;
    }

    public static void showMessage(ChannelHandlerContext ctx, String msg) {
        if (ctx != null) {
            GrapeHttpServer.writeHttpResponse(ctx, rMsg.netMSG(false, msg));
            ctx.close();
            ctx.deregister();
        }
    }

    public void initHttpRequest(HttpRequest _header) {
        request = _header;
        for (String key : GrapeHttpHeader.keys) {
            updateValue(_header, key);
        }
        absPath = _header.uri().trim();
        method = (Method) methodStore.get(_header.method().name().toLowerCase());
    }

    public final HttpContext cloneTo() {
        return this.cloneTo(new HttpContext());
    }

    public final HttpContext cloneTo(HttpContext ctx) {
        if (this.request != null) {
            ctx.initHttpRequest(this.request);
        }
        ctx.serviceName(this.svrName);
        ctx.setMime(this.mime);
        ctx.use(this.ctx);
        ctx.method(this.method);
        ctx.path(this.absPath);
        ctx.parameter(this.parameter);
        ctx.headerValues(this.headerValues());
        return ctx;
    }

    public final HttpContext headerValues(JSONObject nheader) {
        this.values = nheader;
        return this;
    }

    public final JSONObject headerValues() {
        return values;
    }

    public final HttpContext method(Method nmh) {
        this.method = nmh;
        return this;
    }

    private HttpContext updateValue(HttpRequest header, String key) {
        HttpHeaders headers = request.headers();
        if (headers.contains(key)) {
            values.put(key, headers.get(key));
        }
        return this;
    }

    public final AsciiString getMime() {
        return this.mime;
    }

    public final HttpContext setMime(String value) {
        setMime(new AsciiString(value.getBytes()));
        return this;
    }

    public final HttpContext setMime(AsciiString value) {
        this.mime = value;
        return this;
    }

    public final HttpContext serviceName(String svrName) {
        this.svrName = svrName;
        return this;
    }

    public final String serviceName() {
        return this.svrName != null ? this.svrName : path().split("/")[1];
    }

    public final JSONObject getValues() {
        return values;
    }

    private HttpContext updateValue(JSONObject headers, String key) {
        if (headers.containsKey(key)) {
            values.put(key, headers.get(key));
        }
        return this;
    }

    public JSONObject parameter() {
        return this.parameter;
    }

    public HttpContext parameter(JSONObject p) {
        if (p != null) {
            parameter = p;
        }
        return this;
    }

    public HttpContext path(String path) {
        if (path != null) {
            absPath = path;
        }
        return this;
    }

    public String path() {
        return '/' + StringHelper.build(absPath).trimFrom('/').toString();
    }

    public boolean invaildGscResquest() {
        return path().split("/").length < 4;
    }

    /**
     * 获得类名称
     */
    public String className() {
        return path().split("/")[2];
    }

    /**
     * 获得方法名称
     */
    public String actionName() {
        return path().split("/")[3];
    }

    public HttpContext use(ChannelHandlerContext _ctx) {
        ctx = _ctx;
        return this;
    }

    /**
     * 获得请求方法
     */
    public Method method() {
        return method;
    }

    /**
     * 是否是长连接
     */
    public boolean keepAlive() {
        return HttpUtil.isKeepAlive(request);
    }

    /**
     * 获得host
     */
    public String host() {
        return StringHelper.build(values.getString(GrapeHttpHeader.host)).trimFrom('/').toString();
    }

    /**
     * 设置host
     */
    public HttpContext host(String newHost) {
        values.put(GrapeHttpHeader.host, newHost);
        return this;
    }

    /**
     * 获得IP
     */
    public String ip() {
        return ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
    }

    /**
     * 获得sid
     */
    public String sid() {
        return values.getString(GrapeHttpHeader.sid);
    }

    public HttpContext sid(String nSID) {
        values.put(GrapeHttpHeader.sid, nSID);
        return this;
    }

    /**
     * 获得Oauth2授权信息
     */
    public String token() {
        return values.getString(GrapeHttpHeader.token);
    }

    /**
     * 获得浏览器代理头
     */
    public String agent() {
        return values.getString(GrapeHttpHeader.agent);
    }

    /**
     * 获得表单数据
     */
    public String form() {
        return values.getString(GrapeHttpHeader.formdata);
    }

    /**
     * 获得请求所在APPID
     */

    public int appid() {
        try {
            return NumberHelper.number2int(values.get(GrapeHttpHeader.appid, 0));
        } catch (Exception e) {
        }
        return 0;
    }

    public HttpContext appid(int appid) {
        if (appid > 0) {
            values.put(GrapeHttpHeader.appid, appid);
        }
        return this;
    }

    /**
     * 是不是gsc-core该响应的请求
     */
    public boolean isGscRequest() {
        return values.containsKey(GrapeHttpHeader.appid);
    }

    /**
     * 获得参数实例组
     */
    public Object[] invokeParamter() {
        String[] urls = this.path().split("/");
        int offset = 4;
        if (urls.length < offset) {
            return null;
        }
        // 构造参数组
        Object[] arglist = new Object[urls.length - offset];
        String[] stype;
        String svalue;
        for (int i = offset; i < urls.length; i++) {
            svalue = urls[i];
            stype = svalue.split(":");
            int idx = i - offset;
            if (stype.length > 0) {//包含类型信息
                switch (stype[0]) {
//string
                    case "s" -> arglist[idx] = svalue.substring(2);
//int
                    case "int" -> arglist[idx] = Integer.parseInt(svalue.substring(4));
//long
                    case "long" -> arglist[idx] = Long.parseLong(svalue.substring(5));
//char
                    case "char" -> arglist[idx] = svalue.charAt(5);
//float
                    case "float" -> arglist[idx] = Float.parseFloat(svalue.substring(6));
//double
                    case "double" -> arglist[idx] = Double.parseDouble(svalue.substring(7));
//short
                    case "short" -> arglist[idx] = Short.parseShort(svalue.substring(6));
//Integer
                    case "i" -> arglist[idx] = Integer.parseInt(svalue.substring(2));
//boolean
                    case "b", "bool" -> arglist[idx] = Boolean.parseBoolean(svalue.substring(2));
//float
                    case "f" -> arglist[idx] = Float.parseFloat(svalue.substring(2));
//long
                    case "l" -> arglist[idx] = Long.parseLong(svalue.substring(2));
//double
                    case "d" -> arglist[idx] = Double.parseDouble(svalue.substring(2));
//boolean
                    default -> arglist[idx] = svalue;
                }
            } else {
                arglist[i] = svalue;
            }
        }
        return arglist;
    }

    public final String url() {
        return host() + path();
    }

    /**
     * 获得上下文频道
     */
    public Channel channel() {
        return ctx.channel();
    }

    /**
     * 获得上下文
     */
    public ChannelHandlerContext channelContext() {
        return ctx;
    }

    /**
     * 获得jsonobject header对象
     */
    public JSONObject header() {
        JSONObject nHeader = new JSONObject();
        getValueSafe(GrapeHttpHeader.sid, nHeader);
        getValueSafe(GrapeHttpHeader.token, nHeader);
        getValueSafe(GrapeHttpHeader.appid, nHeader);
        return nHeader;
    }

    /**
     * 设置jsonobject header对象
     */
    public HttpContext header(JSONObject nHeader) {
        setValueSafe(GrapeHttpHeader.sid, nHeader);
        setValueSafe(GrapeHttpHeader.token, nHeader);
        setValueSafe(GrapeHttpHeader.appid, nHeader);
        return this;
    }

    /**
     * 任意地点抛出返回值
     */
    public HttpContext throwOut(String msg) {
        HttpContext.showMessage(this.channelContext(), msg);
        throw new RuntimeException(msg);
    }

    private HttpContext setValueSafe(String key, JSONObject nHeader) {
        if (nHeader.containsKey(key)) {
            values.put(key, nHeader.get(key));
        }
        return this;
    }

    private HttpContext getValueSafe(String key, JSONObject c) {
        if (values.containsKey(key)) {
            c.put(key, values.get(key));
        }
        return this;
    }

    public enum Method {
        get, post, websocket
    }

    public static class GrapeHttpHeader {

        public final static String ip = "ip";
        public final static String sid = "GrapeSID";
        public final static String token = "GrapeOauth2";
        public final static String host = "host";
        public final static String agent = "agent";
        public final static String formdata = "exData";
        public final static String appid = "appID";

        public final static String ChannelContext = "socket";
        public final static List<String> keys = new ArrayList<>();
        public final static List<String> apps = new ArrayList<>();
        public final static List<String> xmls = new ArrayList<>();
        public final static List<String> websocket = new ArrayList<>();

        static {
            keys.add(GrapeHttpHeader.ip);
            keys.add(GrapeHttpHeader.sid);
            keys.add(GrapeHttpHeader.token);
            keys.add(GrapeHttpHeader.host);
            keys.add(GrapeHttpHeader.agent);
            keys.add(GrapeHttpHeader.formdata);
            keys.add(GrapeHttpHeader.appid);
            keys.add(GrapeHttpHeader.ChannelContext);

            //app
            apps.add(App.fullurl);

            //WebSocket
            websocket.add(WebSocket.url);
            websocket.add(WebSocket.header);
            websocket.add(WebSocket.param);
            websocket.add(WebSocket.wsid);

            //Xml
            xmls.add(xml.xmldata);
        }

        public static class WebSocket {
            public final static String url = "path";
            public final static String header = "header";
            public final static String param = "param";
            public final static String wsid = "wsID";
        }

        public static class xml {
            public final static String xmldata = "xmldata";
        }

        public static class App {
            public final static String fullurl = "requrl";
        }
    }

}
