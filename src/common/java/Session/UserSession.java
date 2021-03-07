package common.java.Session;

import common.java.Authority.PermissionsPowerDef;
import common.java.HttpServer.HttpContext;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserSession {
    private static final List<String> keyArray;

    static {
        keyArray = new ArrayList<>();
        keyArray.add(PermissionsPowerDef.powerValField);
        keyArray.add(PermissionsPowerDef.fatherIDField);
        keyArray.add(PermissionsPowerDef.adminField);
        keyArray.add(PermissionsPowerDef.userField);
    }

    public static Session createUserSession(String uid, JSONObject userInfo) {
        for (String key : keyArray) {
            checkUserKey(userInfo, key);
        }
        Session se = Session.createSession(uid, userInfo, 86400 * 30);
        HttpContext.current().sid(se.getSID());
        return se;
    }

    public static Session current() {
        return Session.build();
    }

    private static void checkUserKey(JSONObject userInfo, String key) {
        if (!userInfo.containsKey(key)) {
            nLogger.errorInfo("用户信息关键字段:\"" + key + "\" 不存在");
        }
    }
}
