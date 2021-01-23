package common.java.Reflect;

import common.java.Config.nConfig;
import common.java.JGrapeSystem.SystemDefined;
import common.java.file.UploadFile;
import common.java.interfaceType.ApiType;
import common.java.interfaceType.ApiTypes;
import common.java.nlogger.nlogger;
import common.java.oauth.oauthApi;
import common.java.rpc.*;
import common.java.session.UserSession;
import common.java.string.StringHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class _reflect {
    private static final HashMap<Class<?>, Class<?>> callType;

    static {
        callType = new HashMap<>();
        callType.put(String.class, null);
        callType.put(Integer.class, int.class);
        callType.put(Float.class, float.class);
        callType.put(Double.class, double.class);
        callType.put(Boolean.class, boolean.class);
        callType.put(Long.class, long.class);
        callType.put(char.class, null);
        callType.put(short.class, null);
        callType.put(JSONObject.class, null);
        callType.put(JSONArray.class, null);
        callType.put(Function.class, null);
        callType.put(ArrayList.class, List.class);
        callType.put(Object.class, null);
        callType.put(File.class, null);
        callType.put(UploadFile.class, null);
        callType.put(byte.class, null);
        callType.put(byte[].class, null);
        callType.put(InputStream.class, null);
        callType.put(FileInputStream.class, null);
        callType.put(ByteArrayInputStream.class, null);
        callType.put(OutputStream.class, null);
        callType.put(FileOutputStream.class, null);
        callType.put(ByteArrayOutputStream.class, null);
    }

    private final Class<?> _Class;        //类
    private final HashMap<String, FilterCallback> filterCallback; //调用前接口HOOK数组
    private final HashMap<String, ReturnCallback> returnCallback; //调用后接口HOOK数组
    private Object _oObject;        //对象
    private boolean _superMode;    //私有接口是否生效
    private boolean privateMode;    //注解解析是否生效

    public _reflect(Class<?> cls) {
        // 初始化属性
        filterCallback = new HashMap<>();
        returnCallback = new HashMap<>();
        _superMode = false;
        privateMode = false;
        _Class = cls;

    }
    // 获得class名称

    // api接口类型文本化
    private static String declApiType(ApiType _at) {
        String apiLevel = switch (_at.value()) {
            case CloseApi -> "closeApi，";
            case OauthApi -> "oauth2Api，";
            case PublicApi -> "PublicApi，";
            case PrivateApi -> "LocalApi，";
            case SessionApi -> "SessionApi，";
        };
        return apiLevel;
    }

    /**
     * 获得当前类全部公开方法和参数的api声明
     *
     * @return
     */
    public static String getServDecl(Class<?> cls) {
        return _reflect.ServDecl(cls);
    }

    private static String AnnotationMethod(Method method) {
        StringBuilder apiString = new StringBuilder();
        Annotation[] atApiTypes = method.getDeclaredAnnotations();
        for (Annotation an : atApiTypes) {
            if (an.annotationType() == ApiTypes.class) {
                ApiTypes at = method.getAnnotation(ApiTypes.class);
                for (ApiType _at : at.value()) {
                    apiString.append(declApiType(_at));
                }
            } else if (an.annotationType() == ApiType.class) {
                apiString.append(declApiType(method.getAnnotation(ApiType.class)));
            }
        }
        return apiString.toString();
    }

    private static String ParameterMethod(Method method) {
        Parameter[] parameters = method.getParameters();
        StringBuilder ParamString = new StringBuilder();
        for (final Parameter parameter : parameters) {
            //生成参数字符串
            ParamString.append(ExecRequest.class2string(parameter.getType())).append(":").append(parameter.getName()).append(",");
        }
        return ParamString.toString();
    }

    private static String ServDecl(Class<?> cls) {
        JSONArray funcs = new JSONArray();
        Method[] methods;
        do {
            methods = cls.getDeclaredMethods();
            for (Method method : methods) {
                /* mf = 1  public
                 * mf = 9  public static
                 * mf = 25 public static final
                 * */
                if (method.getModifiers() == Modifier.PUBLIC) {
                    String api = AnnotationMethod(method);
                    String param = ParameterMethod(method);
                    funcs.add(new JSONObject("name", method.getName()).puts("level", api).puts("param", StringHelper.build(param).trimFrom(',').toString()));
                }
            }
            cls = cls.getSuperclass();
        } while (cls != Object.class);
        return rMsg.netMSG(0, funcs.size() > 0 ? "ok" : "err", funcs);
    }

    public _reflect superMode() {
        _superMode = true;
        return this;
    }

    public _reflect newInstance(Object... _parameters) {
        List<Object> parameters = Arrays.asList(_parameters);
        Class<?>[] parametercls = object2class(parameters);
        // 初始化反射类
        try {
            Constructor<?> cObject = _Class.getDeclaredConstructor(parametercls.length == 0 ? null : parametercls);
            _oObject = _parameters.length > 0 ? cObject.newInstance(_parameters) : cObject.newInstance();
        } catch (InstantiationException e) {
            nlogger.logInfo(e, "初始化类:" + _Class.getName() + " 实例化失败");
            _oObject = null;
        } catch (IllegalAccessException e) {
            nlogger.logInfo(e, "初始化类:" + _Class.getName() + " 访问异常");
            _oObject = null;
        } catch (IllegalArgumentException e) {
            nlogger.logInfo(e, "初始化类:" + _Class.getName() + " 无效参数");
            _oObject = null;
        } catch (InvocationTargetException e) {
            nlogger.logInfo(e, "初始化类:" + _Class.getName() + " 无效调用");
            _oObject = null;
        } catch (NoSuchMethodException e) {
            nlogger.logInfo(e, "初始化类:" + _Class.getName() + " 方法不存在");
            _oObject = null;
        } catch (SecurityException e) {
            nlogger.logInfo(e, "初始化类:" + _Class.getName() + " acl异常");
            _oObject = null;
        }
        return this;
    }

    public _reflect filterInterface(List<String> actionNameArray, FilterCallback cb) {
        for (String actionName : actionNameArray) {
            filterInterface(actionName, cb);
        }
        return this;
    }

    public _reflect filterInterface(String actionName, FilterCallback cb) {
        filterCallback.put(actionName, cb);
        return this;
    }

    public _reflect returnInterface(List<String> actionNameArray, ReturnCallback cb) {
        for (String actionName : actionNameArray) {
            returnInterface(actionName, cb);
        }
        return this;
    }

    public _reflect returnInterface(String actionName, ReturnCallback cb) {
        returnCallback.put(actionName, cb);
        return this;
    }

    private Method _getMethod(String functionName, Class<?>[] parameterlist) {
        int i = parameterlist == null ? 0 : parameterlist.length;
        Method comMethod = null;
        while (true) {
            try {
                comMethod = _Class.getMethod(functionName, parameterlist);
            } catch (NoSuchMethodException e) {
            }
            if (comMethod == null && i > 0) {
                i--;
                parameterlist[i] = Object.class;
            } else {
                break;
            }
        }
        return comMethod;
    }

    private Class<?>[] object2class(List<Object> parameters) {
        List<Object> rList = new ArrayList<>();
        Class<?>[] rs;
        try {
            for (Object obj : parameters) {
                Class<?> _vClass;
                Class<?> _oClass = obj.getClass();
                if (callType.containsKey(_oClass)) {
                    _vClass = callType.get(_oClass);
                    if (_vClass != null) {
                        _oClass = _vClass;
                    }
                } else {
                    //增补特殊形参
                    if (obj instanceof Function<?, ?>) {
                        _oClass = Function.class;
                    } else if (obj instanceof Consumer<?>) {
                        _oClass = Consumer.class;
                    } else if (obj instanceof Predicate<?>) {
                        _oClass = Predicate.class;
                    } else if (obj instanceof Supplier<?>) {
                        _oClass = Supplier.class;
                    } else {
                        _oClass = Object.class;
                    }
                }
                rList.add(_oClass);
            }
            rs = rList.toArray(new Class<?>[rList.size()]);
        } catch (Exception e) {
            rs = null;
        }
        return rs;
    }

    public _reflect privateMode() {
        privateMode = true;
        return this;
    }

    /**
     * @apiNote 输入请求过滤
     */
    private Object _Hook(String functionName, Object... parameters) {
        Object rs = null;
        try {
            if ("@description".equals(functionName)) {
                rs = ServDecl(_Class);
            }
        } catch (Exception e) {
            rs = "调用参数类型异常";
        }
        return rs;
    }

    private RpcError chkApiType(ApiType _at) {
        RpcError rs = null;
        switch (_at.value()) {
            case SessionApi:
                if (!UserSession.current().checkSession()) {//会话不存在
                    // rs = rMsg.netMSG(SystemDefined.interfaceSystemErrorCode.SessionApi, "当前请求不在有效会话上下文内");
                    rs = RpcError.Instant(SystemDefined.interfaceSystemErrorCode.SessionApi, "当前请求不在有效会话上下文内");
                }
                break;
            case OauthApi:
                if (!oauthApi.getInstance().checkApiToken()) {
                    // rs = rMsg.netMSG(SystemDefined.interfaceSystemErrorCode.OauthApi, "当前token无效或已过期");
                    rs = RpcError.Instant(SystemDefined.interfaceSystemErrorCode.OauthApi, "当前token无效或已过期");
                }
                break;
            case CloseApi:
                // rs = rMsg.netMSG(SystemDefined.interfaceSystemErrorCode.CloseApi, "非法接口");
                rs = RpcError.Instant(SystemDefined.interfaceSystemErrorCode.CloseApi, "非法接口");
                break;
            case PrivateApi:
                // rs = _superMode ? null : rMsg.netMSG(SystemDefined.interfaceSystemErrorCode.PrivateApi, "内部接口");
                rs = _superMode ? null : RpcError.Instant(SystemDefined.interfaceSystemErrorCode.PrivateApi, "内部接口");
                break;
            default:
                break;
        }
        return rs;
    }

    // 调用主方法
    private Object callMainAction(Method comMethod, Class<?>[] cls, String functionName, Object... parameters) {
        Object rs = null;
        if (comMethod != null) {
            //------------------方法注解检查，多个注解权限时，OR逻辑连接多个注解条件
            if (!nConfig.debug && !privateMode) {
                Annotation[] ans = comMethod.getDeclaredAnnotations();
                // Annotation[] ans = comMethod.getAnnotations();
                for (Annotation an : ans) {//遍历全部注解
                    if (an.annotationType() == ApiType.class) {
                        rs = chkApiType(comMethod.getAnnotation(ApiType.class));
                    } else if (an.annotationType() == ApiTypes.class) {
                        ApiTypes atApiType = comMethod.getAnnotation(ApiTypes.class);
                        for (ApiType _at : atApiType.value()) {
                            rs = chkApiType(_at);
                            if (rs == null) {
                                break;
                            } else {
                                rs = "Interface Error:[" + rs.toString() + "]";
                            }
                        }
                    }
                }
            }
            //rs == null表示通过检查
            if (rs == null) {
                //------------------方法类型检查
                int mf = comMethod.getModifiers();
                /* mf = 1  public
                 * mf = 9  public static
                 * mf = 25 public static final
                 * */
                try {
                    // 如果包含调用hook
                    // 调用方法
                    rs = comMethod.invoke((mf == 9 || mf == 25) ? null : _oObject, parameters);
                    // 如果包含结果hook
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    nlogger.logInfo(_Class.getName() + "." + functionName + "无效访问");
                    //e.printStackTrace();
                    rs = null;
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    nlogger.logInfo(e, _Class.getName() + "." + functionName + "无效参数");
                    rs = null;
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    nlogger.logInfo(e, "函数:" + _Class.getName() + "." + functionName + "内部错误");
                    rs = null;
                } catch (Exception e) {
                    nlogger.logInfo(e, "函数:" + _Class.getName() + "." + functionName + "未知异常");
                    //e.printStackTrace();
                    rs = null;
                }
            }
        } else {
            StringBuilder clsString = new StringBuilder();
            if (parameters != null) {
                try {
                    for (int i = 0; i < parameters.length; i++) {
                        clsString.append(cls[i].getSimpleName()).append(":").append(parameters[i].toString()).append(",");
                    }
                } catch (Exception e) {
                    clsString = new StringBuilder();
                }
            }
            nlogger.logInfo("函数:" + _Class.getName() + "." + functionName + "(" + StringHelper.build(clsString.toString()).removeTrailingFrom().toString() + ") -不存在,或者形参与实参不匹配");
        }
        return rs;
    }

    /**
     * 反射调用类方法
     * 补充功能，此处获得方法apiType注解信息，判断其接口属性，决定是否有返回值
     *
     * @param functionName
     * @param parameters
     * @return
     */
    public Object _call(String functionName, Object... parameters) {
        Object rs = null;
        Method comMethod;
        Class<?>[] cls = object2class(Arrays.asList(parameters));
        comMethod = _getMethod(functionName, cls);
        // 主方法存在
        if (comMethod != null) {
            // 调用主要方法
            rs = callMainAction(comMethod, cls, functionName, parameters);
        }
        _superMode = false;
        return rs;
    }
}
