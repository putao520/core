package common.java.rpc;

import org.json.simple.JSONObject;

import java.util.function.Function;

public class RpcBefore {
    // 过滤链
    public static final JSONObject filterArray = new JSONObject();

    public static void $(String actionName, Function<Object[], Object[]> fn) {
        filterArray.puts(actionName, fn);
    }

    public static void $(String[] actionNameArray, Function<Object, Object> fn) {
        for (String actionName : actionNameArray) {
            filterArray.puts(actionName, fn);
        }
    }

    public static Object[] filter(String actionName, Object[] input) {
        Function<Object[], Object[]> fn = (Function<Object[], Object[]>) filterArray.get(actionName);
        if (fn != null) {
            return fn.apply(input);
        }
        return input;
    }
}
