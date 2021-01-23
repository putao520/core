package common.java.rpc;

import org.json.simple.JSONObject;

public class RpcAfter {
    // 过滤链
    public static final JSONObject filterArray = new JSONObject();
    public static ReturnCallback global_fn = null;

    public static void $(String actionName, ReturnCallback fn) {
        if (actionName.equals("*")) {
            global_fn = fn;
        } else {
            filterArray.puts(actionName, fn);
        }
    }

    public static void $(String[] actionNameArray, ReturnCallback fn) {
        for (String actionName : actionNameArray) {
            $(actionName, fn);
        }
    }

    public static Object global_filter(String actionName, Object returnValue) {
        if (global_fn != null) {
            return global_fn.run(actionName, returnValue);
        }
        return returnValue;
    }

    public static Object filter(String actionName, Object returnValue) {
        Object g_o = global_filter(actionName, returnValue);
        ReturnCallback fn = (ReturnCallback) filterArray.get(actionName);
        if (fn != null) {
            return fn.run(actionName, g_o);
        }
        return g_o;
    }
}
