package common.java.session;

import common.java.JGrapeSystem.SystemDefined;
import common.java.authority.PermissionsPowerDef;
import common.java.httpServer.HttpContext;
import common.java.nlogger.nlogger;
import common.java.rpc.rMsg;
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

    public static final Session createUserSession(String uid, JSONObject userInfo) {
        for (String key : keyArray) {
            checkUserKey(userInfo, key);
        }
        Session se = Session.createSession(uid, userInfo, 86400 * 30);
        HttpContext.current().sid(se._getSID());
        return se;
    }

    public static final Session current() {
        Session se = new Session();
        if (!se.checkSession()) {
            HttpContext.current().throwOut(rMsg.netMSG(SystemDefined.interfaceSystemErrorCode.MissSession, "会话已失效，请重新登陆"));
        }
        return se;
    }

    private static final void checkUserKey(JSONObject userInfo, String key) {
        if (!userInfo.containsKey(key)) {
            nlogger.errorInfo("用户信息关键字段:\"" + key + "\" 不存在");
        }
    }
}
