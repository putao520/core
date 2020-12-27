package common.java.session;

import common.java.authority.PermissionsPowerDef;
import common.java.httpServer.HttpContext;
import common.java.nlogger.nlogger;
import org.json.simple.JSONObject;

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
        HttpContext.current().sid(se._getSID());
        return se;
    }

    public static Session current() {
        return new Session();
    }

    private static void checkUserKey(JSONObject userInfo, String key) {
        if (!userInfo.containsKey(key)) {
            nlogger.errorInfo("用户信息关键字段:\"" + key + "\" 不存在");
        }
    }
}
