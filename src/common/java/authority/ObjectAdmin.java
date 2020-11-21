package common.java.authority;

import common.java.apps.AppContext;
import common.java.database.DbLayer;
import common.java.session.Session;
import org.json.simple.JSONObject;

/* objectAdmin_appid
 * oid : int			管理员用户唯一ID
 * uid : String			管理员用户名
 * objects:ARRAY		对象名称组
 * */
//对象权限管理类 依赖应用级别配置的DB配置
public class ObjectAdmin extends DbLayer {
    private String uid = null;
    private String[] adminInfo;

    public ObjectAdmin() {
        super(AppContext.current().config().db());
        adminInfo = null;
        Session se = new Session();
        form(ObjectAdminDef.tableName).bindApp();
        String tempUID = se.getUID();
        if (tempUID != null) {
            uid = tempUID;
            updateAdminInfo();
        }
    }

    private void updateAdminInfo() {
        JSONObject rjson = uid == null ? null : eq(ObjectAdminDef.userName, uid).find();
        if (!(rjson == null || rjson.isEmpty()) && rjson.containsKey(ObjectAdminDef.objectList)) {
            String rString = rjson.getString(ObjectAdminDef.objectList);
            if (rString.equals("*")) {
                adminInfo[0] = rString;
            } else {
                adminInfo = !rString.equals("") ? rString.split(",") : null;
            }

        }

    }

    //根据用户名绑定管理员
    public boolean bindAdmin(String uid) {
        this.uid = uid;
        updateAdminInfo();
        return adminInfo != null;
    }

    //判断对象是否归本管理员管辖
    public boolean ishas(String objName) {
        boolean rs = false;
        if (adminInfo != null) {
            if (adminInfo.length == 1 && adminInfo[0].equals("*")) {
                rs = true;
            } else {
                int len = adminInfo.length;
                for (int i = 0; i < len; i++) {
                    if (adminInfo[i].equals(objName)) {
                        rs = true;
                        break;
                    }
                }
            }
        }
        return rs;
    }

}
