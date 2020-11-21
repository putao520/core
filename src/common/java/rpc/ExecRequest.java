/**
 * 请求url说明
 * http://host/app/module/function/p1/p2/p3/.../pN
 *
 * @author Administrator
 */
/**
 * @author Administrator
 *
 */
package common.java.rpc;

import common.java.Config.nConfig;
import common.java.JGrapeSystem.GrapeJar;
import common.java.JGrapeSystem.SystemDefined;
import common.java.Reflect._reflect;
import common.java.apps.MicroServiceContext;
import common.java.httpServer.HttpContext;
import common.java.httpServer.RequestSession;
import common.java.nlogger.nlogger;
import common.java.string.StringHelper;
import org.json.simple.JSONObject;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecRequest {//框架内请求类

    private static final AtomicInteger randON;
    private static final HashMap<Class<?>, String> class2string;
    private static final String clsDesp = "@getClass";
    private static String appsURL;

    static {
        randON = new AtomicInteger(0);

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

    private static final String getAppsURL() {
        if (appsURL == null) {
            AppRepository();
        }
        return appsURL;
    }

    private static void AppRepository() {
        JSONObject obj;
        try {
            obj = JSONObject.toJSON(nConfig.netConfig(SystemDefined.commonConfigUnit.FileNode));
            appsURL = StringHelper.build(obj.getString("nodeAddresses")).trimFrom('/').toString();
        } catch (Exception e) {
            System.err.println("AppRepository Config Error!:" + e.getMessage());
            appsURL = "";
        }
    }

    /**
     * 执行当前上下文环境下的调用
     * */
    public static Object _run(HttpContext ctx) {
        // return autoExecute(ctx , ctx.appid() == 0 );
        return autoExecute(ctx, true);
    }

    /**
     * 执行接收到的请求
     * */
    public static Object autoExecute(HttpContext ctx, boolean localService) {
        Class<?> _cls = null;
        HttpContext hCtx = HttpContext.current();
        String className = hCtx.className();
        String path = ctx.path();
        JSONObject paramter = ctx.parameter();

        Object rs = null;
        try {
            // 获得class
            if (!localService) {// 需要加载目标jar包
                MicroServiceContext msc = MicroServiceContext.current();
                String jarName = msc.serviceFileName();
                String appsURL = getAppsURL();
                ClassLoader _loader = clsLoader("http://" + appsURL + "/" + jarName + ".jar");
                if (_loader != null) {// 远程jar包存在
                    if (className.equals(clsDesp)) {// 获得包内类方法
                        return getPackageClassInfo("main/java/interfaceApplication");
                    }
                    _cls = _loader.loadClass("interfaceApplication." + className);
                }
            } else {
                _cls = Class.forName("main.java.interfaceApplication" + "." + className);
            }

            // 执行call
            if (_cls != null) {
                try {
                    // 创建类反射
                    _reflect obj = new _reflect(_cls);
                    // 保存反射类
                    RequestSession.setCurrent(obj);
                    // 构造反射类实例
                    obj.newInstance();
                    rs = (obj._call(hCtx.actionName(), hCtx.invokeParamter()));
                } catch (Exception e) {
                    nlogger.logInfo(e, "实例化 " + _cls.toString() + " ...失败");
                }
            }

        } catch (Exception e) {
            nlogger.logInfo(e, "类:" + className + " : 不存在");
        }
        return rs;
    }


    /**
     * java类型转成字符串类型
     */
    public static String class2string(Class<?> cls) {
        return class2string.containsKey(cls) ? class2string.get(cls).split(",")[0] : cls.getName();
    }

    public static String objects2string(Object[] objs) {
        if (objs == null) {
            return "";
        }
        String value;
        String rString = "";
        for (int i = 0; i < objs.length; i++) {
            Object val = objs[i];
            value = class2string(val.getClass());
            rString += "/" + value + ":" + StringHelper.any2String(val);
        }
        return rString;
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
        String GetParams = "";
        for (String key : info.keySet()) {
            GetParams += info.getString(key) + ":;";
        }
        return "gsc-post:" + StringHelper.build(GetParams).removeTrailingFrom(2).toString();
    }

    private static ClassLoader clsLoader(String url) {
        ClassLoader clr = null;
        try {
            clr = new URLClassLoader(new URL[]{new URL(url)});
        } catch (Exception e) {
            clr = null;
        }
        return clr;
    }

    private static final String getPackageClassInfo(String packagePath) {
        List<Class<?>> clsList = GrapeJar.getClass(packagePath, true);
        List<String> rList = new ArrayList<>();
        for (Class<?> c : clsList) {
            String[] clss = c.getName().split("\\.");
            rList.add(clss[clss.length - 1]);
        }
        return StringHelper.join(rList, ",");
    }

}