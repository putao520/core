package common.java.rpc;

import common.java.Reflect._reflect;
import common.java.httpServer.HttpContext;
import common.java.httpServer.RequestSession;
import common.java.nlogger.nlogger;
import common.java.string.StringHelper;
import org.json.simple.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;

public class ExecRequest {//框架内请求类

    private static final HashMap<Class<?>, String> class2string;
    private static final String clsDesp = "@getClass";
    private static final String appsURL;

    static {

        class2string = new HashMap<>();
        class2string.put(String.class, "s");
        class2string.put(int.class, "i,int");
        class2string.put(long.class, "l,long");
        class2string.put(char.class, "char");
        class2string.put(float.class, "f,float");
        class2string.put(boolean.class, "b,boolean");
        class2string.put(short.class, "short");
        class2string.put(double.class, "d,double");

        class2string.put(Integer.class, "i,int");
        class2string.put(Long.class, "l,long");
        class2string.put(Character.class, "char");
        class2string.put(Float.class, "f,float");
        class2string.put(Boolean.class, "b,boolean");
        class2string.put(Short.class, "short");
        class2string.put(Double.class, "d,double");
        appsURL = null;
    }

    /**
     * 执行当前上下文环境下的调用
     */
    public static Object _run(HttpContext ctx) {
        HttpContext hCtx = HttpContext.current();
        String className = hCtx.className();
        String actionName = hCtx.actionName();
        Object rs = null;
        try {
            // 执行前置类
            Object[] _objs = hCtx.invokeParamter();
            if (beforeExecute(className, actionName, _objs)) {
                // 载入主类
                Class<?> _cls = Class.forName("main.java._api" + "." + className);
                // 执行call
                try {
                    // 创建类反射
                    _reflect obj = new _reflect(_cls);
                    // 保存反射类
                    RequestSession.setCurrent(obj);
                    // 构造反射类实例
                    obj.newInstance();
                    // 调用主要类,后置类,固定返回结构
                    rs = obj._call(actionName, _objs);
                    rs = RpcResult(afterExecute(className, actionName, rs));
                } catch (Exception e) {
                    nlogger.logInfo(e, "实例化 " + _cls.toString() + " ...失败");
                }
            } else {
                rs = RpcError.Instant(false, "前置接口过滤错误!");
            }
        } catch (Exception e) {
            nlogger.logInfo(e, "类:" + className + " : 不存在");
        }
        return rs;
    }

    // 过滤函数改变输入参数
    private static Boolean beforeExecute(String className, String actionName, Object[] objs) {
        try {
            // 载入主类的前置类
            Class<?> _before_cls = Class.forName("main.java.before_api" + "." + className);
            Method fn = _before_cls.getMethod("filter", String.class, Object[].class);
            return (Boolean) fn.invoke(null, actionName, objs);
        } catch (ClassNotFoundException e1) {
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // System.out.println("类:" + className + "_前置类,异常！");
            return false;
        }
    }

    // 结果函数改变输入参数
    private static Object afterExecute(String className, String actionName, Object obj) {
        try {
            // 载入主类的前置类
            Class<?> _after_cls = Class.forName("main.java.after_api" + "." + className);
            Method fn = _after_cls.getMethod("filter", String.class, Object.class);
            return fn.invoke(null, actionName, obj);
        } catch (Exception e) {
            // System.out.println("类:" + className + "_后置类,异常！");
        }
        return obj;
    }

    private static Object RpcResult(Object o) {
        if (o == null) {
            return rMsg.netState(false);
        }
        if (o instanceof byte[]) {
            return o;
        } else if (o instanceof RpcPageInfo) {
            return o.toString();
        } else if (o instanceof Boolean) {
            return rMsg.netState(o);
        } else if (o instanceof RpcError) {
            return o.toString();
        } else {
            return rMsg.netMSG(o);
        }
    }

    /**
     * java类型转成字符串类型
     */
    public static String class2string(Class<?> cls) {
        return class2string.containsKey(cls) ? class2string.get(cls).split(",")[0] : cls.getName();
    }

    private static boolean is_grape_args(String arg) {
        return arg != null && arg.split(":").length > 1;
    }

    public static String objects2string(Object[] objs) {
        if (objs == null) {
            return "";
        }
        String value;
        StringBuilder rString = new StringBuilder();
        for (Object val : objs) {
            rString.append("/");
            if (!is_grape_args(val.toString())) {
                value = class2string(val.getClass());
                rString.append(value).append(":");
            }
            rString.append(StringHelper.any2String(val));
        }
        return rString.toString();
    }

    public static String objects2poststring(Object... args) {
        if (args == null || args.length == 0) {
            return "";
        }
        String[] GetParams = StringHelper.build(ExecRequest.objects2string(args)).trimFrom('/').toString().split("/");
        return "gsc-post:" + StringHelper.join(GetParams, ":,");
    }

    public static Object[] postJson2ObjectArray(JSONObject postParameter) {
        Object[] args = null;
        if (postParameter != null) {
            int i = 0;
            args = new Object[postParameter.size()];
            for (String key : postParameter.keySet()) {
                args[i] = postParameter.get(key);
            }
        }
        return args;
    }

    public static String objects2poststring(JSONObject info) {
        if (info == null || info.size() == 0) {
            return "";
        }
        // String[] GetParams = StringHelper.build(ExecRequest.objects2string(args)).trimFrom('/').toString().split("/");
        StringBuilder GetParams = new StringBuilder();
        for (String key : info.keySet()) {
            GetParams.append(info.getString(key)).append(":;");
        }
        return "gsc-post:" + StringHelper.build(GetParams.toString()).removeTrailingFrom(2).toString();
    }

}