package common.java.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RpcAfter {
    // 过滤链
    public static final HashMap<String, List<ReturnCallback>> filterArray = new HashMap<>();
    public static ReturnCallback global_fn = null;

    public static void $(String actionName, ReturnCallback fn) {
        if (actionName.equals("*")) {
            global_fn = fn;
        } else {
            List<ReturnCallback> fnArray = filterArray.get(actionName);
            if (fnArray == null) {
                fnArray = new ArrayList<>();
                filterArray.put(actionName, fnArray);
            }
            fnArray.add(fn);
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
        List<ReturnCallback> fnArray = filterArray.get(actionName);
        if (fnArray != null) {
            for (ReturnCallback fn : fnArray) {
                g_o = fn.run(actionName, g_o);
            }
        }
        return g_o;
    }
}
