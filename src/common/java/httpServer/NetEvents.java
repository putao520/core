package common.java.httpServer;

import common.java.encrypt.UrlCode;
import common.java.file.FileHelper;
import common.java.file.UploadFileInfo;
import common.java.nlogger.nlogger;
import common.java.string.StringHelper;
import common.java.xml.XmlHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class NetEvents extends ChannelInboundHandlerAdapter {
    private HttpRequest _req;
    private HttpPostRequestDecoder decoder;

    public static final String tryfixURL(String content) {
        String _url = StringHelper.join(StringHelper.path2list(content), "/");
        return _url;
    }

    public static final JSONObject postContent2JSON(String _httpContent) {
        JSONObject rString = null;
        try {
            String httpContent = _httpContent;
            if (httpContent != null) {
                httpContent = UrlCode.decode(httpContent);
                rString = StringHelper.path2rpc(httpContent);//get风格请求转化
                if (null == rString) {
                    rString = JSONObject.toJSON(httpContent);//json风格转换
                    if (null == rString) {
                        rString = XmlHelper.xml2json(httpContent);//xml 风格转换,同时附加原始数据
                        if (rString != null) {
                            rString.put("xmldata", httpContent);//保留原始数据
                        }
                    }
                }
            }
        } catch (Exception e) {
            rString = null;
        }
        return rString;
    }

    public Object _Hook(ChannelHandlerContext ctx, String url) {
        String[] gr = UrlCode.decode(StringHelper.build(url).trimFrom('/').toString().split("/"));
        Object rs = null;
        try {
            switch (gr[0]) {
                /*
                case "@wechatCode":
                    if (gr.length >= 3) {
                        String paramString = StringHelper.join(gr, "/", 2, -1);
                        String nurl = WechatHelper.getRedirectURL(NumberHelper.number2int(gr[1]), paramString);
                        if (nurl != null) {
                            GrapeHttpServer.location(ctx, nurl, new JSONObject("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1 wechatdevtools/0.7.0 MicroMessenger/6.3.9 Language/zh_CN webview/0"));
                        }
                    } else {
                        rs = "调用参数数量异常";
                    }
                    rs = "";
                    break;
                    */
                case "@redirect":
                    if (gr.length == 2) {
                        GrapeHttpServer.location(ctx, gr[1]);
                    } else {
                        rs = "调用参数数量异常";
                    }
                    rs = "";
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            rs = "调用参数类型异常";
        }
        return rs;
    }

    //传统get参数转换成内部参数
    private QueryStringDecoder isNotGscGet(String uri) {
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        /*
        List<String> paramList = new ArrayList<>();
        decoder.parameters().entrySet().forEach(entry -> {
            // entry.getValue()是一个List, 只取第一个元素
            //parmMap.put(entry.getKey(), entry.getValue().get(0));
            paramList.add(entry.getValue().get(0));
        });
        return decoder.path() + StringHelper.join(paramList, "/");
        */
        return decoder.parameters().size() > 0 ? decoder : null;
    }

    private void webSocket(ChannelHandlerContext _ctx, TextWebSocketFrame msg) {
        /*
         * get:{"path":"url","header":{header},"param":{postparam},}
         * */
        WebSocketFrame frame = msg;
        ByteBuf buf = frame.content();  //真正的数据是放在buf里面的
        String wsData = buf.toString(StandardCharsets.UTF_8);  //将数据按照utf-8的方式转化为字符串
        String[] wsCmd = wsData.split(":");
        JSONObject json = new JSONObject();
        if (wsCmd.length > 1) {//不仅仅包含方法
            wsData = StringHelper.join(wsCmd, ":", 1, -1);
            json = JSONObject.toJSON(wsData);
            GrapeHttpServer.startService(json, _ctx, null);
        }
    }

    private String filterURLencodeWord(String url) {
        /*
        String[] uris = UrlCode.decode( url.split("/") );
        return StringHelper.join(uris, "/");
        */
        //return url;
        return UrlCode.decode(url);
    }

    private boolean isGscPost(String str) {
        if (StringHelper.invaildString(str)) {
            return false;
        }
        return str.startsWith("gsc-post:");
    }

    private String convertByteBufToString(ByteBuf buffer) {
        String rString;
        byte[] req = new byte[buffer.readableBytes()];
        buffer.readBytes(req);
        rString = new String(req, StandardCharsets.UTF_8);
        return rString;
    }

    private void httpRequest(ChannelHandlerContext _ctx, HttpContent msg) {
        JSONObject postParam = null;
        String _url = filterURLencodeWord(_req.uri());
        boolean vaild = false;
        if (_req.method().equals(HttpMethod.POST)) {
            String appendURL = null;
            String tempBody = convertByteBufToString(msg.content().copy());
            // 是gsc-post
            if (isGscPost(tempBody)) {
                // 将请求格式是 gsc-rpc 的post转化成等同的get
                appendURL = tryfixURL(tempBody);
                if (!appendURL.equals("") && !StringHelper.invaildString(appendURL)) {
                    _url += "/" + appendURL;
                }
                nlogger.debugInfo("gsc-post:" + _url);
            } else {
                //分析正常post请求参数
                postParam = postParamter(msg);
                nlogger.debugInfo("post:" + _url);
            }
            vaild = true;
        }
        if (_req.method().equals(HttpMethod.GET)) {
            QueryStringDecoder decoder = isNotGscGet(_url);
            // 不是普通gsc-get请求
            if (decoder != null) {
                JSONObject _normalGetParameter = new JSONObject();
                decoder.parameters().forEach((key, values) -> _normalGetParameter.put(key, values.get(0)));
                postParam = _normalGetParameter;
                _url = decoder.path();
                nlogger.debugInfo("normal-get:" + _url);
            } else {
                nlogger.debugInfo("gsc-get:" + _url);
            }
            vaild = true;
        }
        if (vaild) {
            // 重设uri地址
            _req.setUri(_url);
            GrapeHttpServer.startService(_req, _ctx, postParam);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext _ctx, Object msg) {
        //Connection: Upgrade
        if (msg instanceof HttpRequest) {
            _req = (HttpRequest) msg;
        }
        if (_req != null) {
            if (_req.uri().equals("/favicon.ico")) {
                GrapeHttpServer.ZeroResponse(_ctx);
                return;
            }

            if (_req.uri().equals("/@heart")) {
                GrapeHttpServer.writeHttpResponse(_ctx, "1");
                return;
            }

            if (!_req.method().equals(HttpMethod.GET) && !_req.method().equals(HttpMethod.POST)) {
                GrapeHttpServer.ZeroResponse(_ctx);
                return;
            }

            if (_Hook(_ctx, _req.uri()) != null) {//过滤执行
                GrapeHttpServer.ZeroResponse(_ctx);
                return;
            }
        }
        if (msg instanceof HttpContent) {
            httpRequest(_ctx, (HttpContent) msg);
        }
        if (msg instanceof TextWebSocketFrame) {//文本ws
            webSocket(_ctx, (TextWebSocketFrame) msg);
        }
    }

    private JSONObject postParamter(Object req) {
        JSONObject parmMap = new JSONObject();
        decoder = new HttpPostRequestDecoder(_req);
        decoder.offer((HttpContent) req);
        try {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    try {
                        if (data.getHttpDataType() == HttpDataType.Attribute || data.getHttpDataType() == HttpDataType.InternalAttribute) {
                            Attribute _data = (Attribute) data;
                            try {
                                parmMap.put(_data.getName(), _data.getValue());
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                // nlogger.debugInfo(e);
                            }
                            continue;
                        }
                        if (data.getHttpDataType() == HttpDataType.FileUpload) {
                            //String uploadFileName = getUploadFileName(data);
                            FileUpload _data = (FileUpload) data;
                            if (_data.isCompleted()) {
                                //System.err.println(_data.getName());
                                UploadFileInfo fileInfo = new UploadFileInfo(_data.getFilename(), _data.getContentType(), _data.getMaxSize());
                                if (_data.isInMemory()) {
                                    try {
                                        fileInfo.append(_data.getByteBuf().copy());
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        // nlogger.debugInfo(e);
                                    }
                                } else {
                                    try {
                                        File newTempFile = new File(FileHelper.newTempFileName());
                                        _data.getFile().renameTo(newTempFile);
                                        fileInfo.append(newTempFile);
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        // nlogger.debugInfo(e);
                                    }
                                }
                                parmMap.put(_data.getName(), fileInfo);
                                continue;
                            }
                        }
                    } catch (Exception e) {
                        // nlogger.debugInfo(e);
                    } finally {
                        data.release();
                    }
                }
            }
        } catch (EndOfDataDecoderException e1) {
            // nlogger.debugInfo(e1);
        } finally {
            decoder.destroy();
        }
        return parmMap != null ? parmMap.isEmpty() ? null : parmMap : null;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        RequestSession.create(ctx.channel().id());
        try {
            super.channelActive(ctx);
        } catch (Exception e) {
            nlogger.debugInfo(e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        RequestSession.remove(ctx.channel().id());
        try {
            super.channelInactive(ctx);
        } catch (Exception e) {
            nlogger.debugInfo(e);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        RequestSession.remove(ctx.channel().id());
        ctx.close();
        try {
            super.exceptionCaught(ctx, cause);
        } catch (Exception e) {
            nlogger.debugInfo(e);
        }
    }

}
