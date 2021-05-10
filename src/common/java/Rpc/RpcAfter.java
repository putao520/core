package common.java.Rpc;

import java.util.HashMap;

public class RpcAfter {
    // 过滤链
    public static final HashMap<String, ReturnLink> filterArray = new HashMap<>();

    public static void filter(Class clsName, String actionName, ReturnCallback fn) {
        ReturnLink rl = filterArray.get(actionName);
        if (rl == null) {
            rl = ReturnLink.build();
        }
        // 锁定过滤器，不允许新增了
        if (rl.isLocked()) {
            return;
        }
        rl.put(actionName, fn);
        filterArray.put(clsName.getSimpleName(), rl);
    }

    public static void filter(Class clsName, String[] actionNameArray, ReturnCallback fn) {
        for (String actionName : actionNameArray) {
            filter(clsName, actionName, fn);
        }
    }

    public static Object filter(String className, String actionName, Object returnValue) {
        ReturnLink rl = filterArray.get(className);
        if (rl == null) {
            return returnValue;
        }
        return rl.runFor(actionName, returnValue);
    }

    public RpcAfter lock() {
        String clsName = this.getClass().getSimpleName();
        ReturnLink rl = filterArray.get(clsName);
        if (rl != null) {
            rl.lock();
        }
        return this;
    }

    public RpcAfter unlock() {
        String clsName = this.getClass().getSimpleName();
        ReturnLink rl = filterArray.get(clsName);
        if (rl != null) {
            rl.unlock();
        }
        return this;
    }
}
