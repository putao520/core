package common.java.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RpcBefore {
    // 过滤链
    public static final HashMap<String, List<FilterCallback>> filterArray = new HashMap<>();
    // 通用过滤链
    public static FilterCallback global_fn = null;

    public static void $(String actionName, FilterCallback fn) {
        if (actionName.equals("*")) {
            global_fn = fn;
        } else {
            List<FilterCallback> fnArray = filterArray.get(actionName);
            if (fnArray == null) {
                fnArray = new ArrayList<>();
                filterArray.put(actionName, fnArray);
            }
            fnArray.add(fn);
        }

    }

    public static void $(String[] actionNameArray, FilterCallback fn) {
        for (String actionName : actionNameArray) {
            $(actionName, fn);
        }
    }

    public static FilterReturn global_filter(String actionName, Object[] input) {
        if (global_fn != null) {
            return global_fn.run(actionName, input);
        }
        return FilterReturn.buildTrue();
    }

    public static FilterReturn filter(String actionName, Object[] input) {
        FilterReturn r = global_filter(actionName, input);
        if (!r.state()) {
            return r;
        }
        List<FilterCallback> fnArray = filterArray.get(actionName);
        if (fnArray != null) {
            for (FilterCallback fn : fnArray) {
                r = fn.run(actionName, input);
                if (!r.state()) {
                    return r;
                }
            }
        }
        return FilterReturn.buildTrue();
    }
}
