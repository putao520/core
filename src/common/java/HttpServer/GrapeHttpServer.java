package common.java.HttpServer;

import com.alibaba.druid.util.StringUtils;
import common.java.Apps.AppContext;
import common.java.Http.Mime;
import common.java.Rpc.ExecRequest;
import common.java.Rpc.rMsg;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.stream.ChunkedStream;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class GrapeHttpServer {
    // private final static ThreadLocal<HttpContext> requestData;

    private final static int bufferLen = 8192;
    // private final static ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
    private final static ExecutorService es = Executors.newCachedThreadPool();

    private static void fixHttpContext(HttpContext ctx) {
        String path = ctx.path();
        String[] blocks = path.split("/");
        int appid = (blocks.length > 1 && StringUtils.isNumber(blocks[1])) ? Integer.parseInt(blocks[1]) : 0;
        if (appid > 0) {
            // 自动补充appid
            ctx.appid(appid);
            // 自动修正path
            ctx.path(StringHelper.join(blocks, "/", 2, -1));
        }
    }

    /**
     * 生成请求上下文，处理HTTP请求信息
     */
    public static void startService(Object _req, ChannelHandlerContext _ctx, JSONObject post) {
        HttpContext ctx = null;
        if (_req == null || _req instanceof JSONObject) {
            //websocket请求
            if (_req != null) {
                ctx = (new HttpContext((JSONObject) _req));
            }
        } else if (_req instanceof HttpRequest) {
            ctx = (new HttpContext((HttpRequest) _req));
        }
        if (ctx != null) {
            ctx.use(_ctx);
            ctx.parameter(post);
            // 是普通http请求
            if (!ctx.isGscRequest()) {
                // 自动修正appid和path
                fixHttpContext(ctx);
            }
            HttpContext ctxFinal = ctx;

            es.submit(() -> {
                RequestSession.setChannelID(_ctx.channel().id());
                stubLoop(ctxFinal);
            });
        }
    }

    public static void stubLoop(HttpContext ctx) {
        Object rlt = GrapeHttpServer.EventLoop(ctx);
        if (ctx.method() == HttpContext.Method.websocket) {
            rlt = new TextWebSocketFrame(rlt.toString());
        }

        GrapeHttpServer.writeHttpResponse(ctx.channelContext(), rlt);
    }

    private static Object EventLoop(HttpContext ctx) {
        RequestSession.setValue(HttpContext.SessionKey, ctx);
        return systemCall(ctx);
    }

    /**
     * 来自netty服务器的调用
     *
     * @param ctx
     * @return
     */
    public static Object systemCall(HttpContext ctx) {
        String path = ctx.path();
        String host = ctx.host();
        int appid = ctx.appid();
        AppContext appContext;
        Object rsValue = "";

        String[] GrapeRequest = StringHelper.build(path).trimFrom('/').toString().split("/");
        if (GrapeRequest.length >= 2) {
            // 当无有效appid时,根据域名重置Appid
            if (appid == 0 && !GrapeRequest[0].equalsIgnoreCase("grapefw")) {
                appContext = new AppContext(host);
                if (appContext.hasData()) {    // 域名有效,重置appid
                    appid = appContext.appid();
                    ctx.appid(appid);
                }
            }
            //如果包含有效Appid,调用应用服务包
            if (appid > 0) {
                appContext = new AppContext(appid);
                // 微服务名无效或者应用ID无效
                if (!appContext.hasData() || !appContext.microServiceInfo().containsKey(GrapeRequest[0])) {
                    nLogger.logInfo("[应用ID:" + appid + " 无效] 或者 [微服务名称:" + GrapeRequest[0] + " 无效]");
                    return "";
                }
            }
            // 正式执行请求
            if (!ctx.invaildGscResquest()) {
                rsValue = ExecRequest._run(ctx);
            } else {
                rsValue = rMsg.netMSG(false, "不是合法的GSC请求!");
            }
        }
        return rsValue;
    }

    public static void ZeroResponse(ChannelHandlerContext ctx) {
        writeHttpResponse(ctx, "");
    }

    public static void location(ChannelHandlerContext ctx, String newURL) {
        GrapeHttpServer.writeHttpResponse(ctx, "".getBytes(), new JSONObject("Location", newURL));
    }

    public static void location(ChannelHandlerContext ctx, String newURL, JSONObject exheader) {
        JSONObject header = new JSONObject("Location", newURL);
        if (exheader != null) {
            header.putAll(exheader);
        }
        GrapeHttpServer.writeHttpResponse(ctx, "".getBytes(), header);
    }

    private static void addHeader(HttpResponse response, boolean hasChunked) {
        response.headers().set("Access-Control-Allow-Origin", "*");
        response.headers().set("Access-Control-Allow-Headers",
                HttpContext.GrapeHttpHeader.sid + " ," +
                        HttpContext.GrapeHttpHeader.token + " ," +
                        HttpContext.GrapeHttpHeader.appid
        );
        if (hasChunked) {
            response.headers().set("Transfer-Encoding", "chunked");
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
    }

    public static void writeHttpResponse(ChannelHandlerContext ctx, InputStream responseData, JSONObject exHeader) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        addHeader(response, true);
        if (exHeader != null) {
            for (String key : exHeader.keySet()) {
                response.headers().set(key, exHeader.getString(key));
            }
        }

        if (responseData != null) {
            ChannelFuture sendByteFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedStream(responseData, bufferLen)), ctx.newProgressivePromise());
            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            sendByteFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) {
                    try {
                        responseData.close();
                    } catch (Exception e) {
                        nLogger.logInfo("steam is closed");
                    }
                }
            });
        } else {
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                    .addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static void writeHttpResponse(ChannelHandlerContext ctx, byte[] responseData, JSONObject exHeader) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        addHeader(response, false);
        if (exHeader != null) {
            for (String key : exHeader.keySet()) {
                if (key.equalsIgnoreCase("location")) {
                    response.setStatus(HttpResponseStatus.FOUND);
                }
                response.headers().set(key, exHeader.getString(key));
            }
        }

        response.content().writeBytes(responseData);
        if (response.headers().get(CONTENT_TYPE) == null) {
            if (responseData.length > 0) {
                response.headers().set(CONTENT_TYPE, (new Mime()).getMime(responseData) + "; charset=UTF-8");
            } else {
                response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
            }
        }
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void writeHttpResponse(ChannelHandlerContext ctx, TextWebSocketFrame responseData) {
        ctx.channel().writeAndFlush(responseData);
    }

    public static void writeHttpResponse(ChannelHandlerContext ctx, Object responseData) {
        JSONObject exHeader = null;
        //----------流输出
        if (responseData instanceof File) {
            try {
                exHeader = new JSONObject(CONTENT_TYPE.toString(), Mime.getMime((File) responseData));
                responseData = new FileInputStream((File) responseData);
            } catch (Exception e) {
                responseData = null;
            }
        }
        if (responseData instanceof InputStream) {//输入流，不管是字符集还是文件
            writeHttpResponse(ctx, (InputStream) responseData, exHeader);
            return;
        }
        //----------字符输出
        if (responseData == null) {//当返回值为null
            responseData = "";
        }
        if (responseData instanceof byte[]) {
            writeHttpResponse(ctx, (byte[]) responseData, null);
            return;
        }
        //----------wsText输出
        else if (responseData instanceof TextWebSocketFrame) {
            writeHttpResponse(ctx, (TextWebSocketFrame) responseData);
            return;
        }
        //----------其他类型,强制转化成字符串
        else if (!(responseData instanceof String)) {
            try {
                // 如果对象有toString方法
                responseData = responseData.toString();
            } catch (Exception e) {
                // 如果对象可以被valueOf使用
                try {
                    responseData = String.valueOf(responseData);
                } catch (Exception e1) {
                    nLogger.logInfo("输出类型不可预知!!!");
                }
            }
        }
        if (responseData instanceof String) {
            //<!DOCTYPE HTML>
            String valType = "text/plain; charset=UTF-8";
            if (((String) responseData).length() > 14) {
                if (((String) responseData).indexOf("<!DOCTYPE HTML>") == 0) {
                    valType = "text/html; charset=UTF-8";
                }
            }
            exHeader = new JSONObject(CONTENT_TYPE.toString(), valType);
            responseData = ((String) responseData).getBytes();
            writeHttpResponse(ctx, (byte[]) responseData, exHeader);
        }
    }
}
