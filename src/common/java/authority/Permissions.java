package common.java.authority;

import common.java.apps.MModelPerm;
import common.java.apps.MModelPermInfo;
import common.java.authority.PlvDef.UserMode;
import common.java.authority.PlvDef.plvType;
import common.java.database.DbFilter;
import common.java.number.NumberHelper;
import common.java.session.Session;
import org.json.simple.JSONObject;

import java.util.List;

/*
 * 权限模式使用，需要保证用户登录后，将用户个人信息，用户组信息都填充到位
 *
 * objectPower_appid
 * oid : int			对象唯一ID
 * objectName:string		对象名称
 * cMode:JSON			对象创建权限验证类型和条件
 * 	{"chkType":Long,"chkCond":Object }
 * 	chkType : 判断条件
 * 	chkCond : 在权值模式下代表所需权值，在用户，用户组模式下，为0表示继承，非0表示不继承
 * sMode:JSON			对象统计权限验证类型和条件
 * */
public class Permissions {
    private static final String powvalFieldName = PermissionsPowerDef.powerValField;
    private static final String fatherIDFieldName = PermissionsPowerDef.fatherIDField;
    private static final String adminFieldName = PermissionsPowerDef.adminField;
    private static final String userFieldName = PermissionsPowerDef.userField;
    private static String commonSid = null;
    private final String objName;
    private MModelPerm tempMode = null;

    public Permissions(String objectName) {
        objName = objectName;
    }

    public Permissions putPermInfo(MModelPerm pInfo) {
        this.tempMode = pInfo;
        return this;
    }

    private boolean _checkObject(String uid) {
        boolean rs = false;
        ObjectAdmin oadmin = new ObjectAdmin();
        if (oadmin.bindAdmin(uid)) {//有效管理员账号
            rs = oadmin.ishas(objName);
        }
        return rs;
    }

    private boolean _operateChk(MModelPermInfo pInfo) {
        boolean rs = false;
        int chkType = pInfo.type();
        Object val = pInfo.value();
        Session se = getSE();
        switch (chkType) {
            case plvType.powerVal -> {
                int nowPowerVal = se.getInt(powvalFieldName);
                rs = nowPowerVal >= NumberHelper.number2int(val);
            }
            case plvType.userOwn -> {
                String nowUID = se.getUID();
                rs = nowUID.equals(val);
            }
            case plvType.groupOwn -> {
                String nowgid = se.get(fatherIDFieldName).toString();
                rs = val.equals(nowgid);
            }
        }
        return rs;
    }

    /*
    根据权限对象，获得过滤条件
     */
    public List<List<Object>> getAuthCond(String valueCaption, MModelPermInfo pInfo) {
        DbFilter newCond = DbFilter.buildDbFilter();
        Session se = getSE();
        int chkType = pInfo.type();
        switch (chkType) {
            case plvType.userOwn -> {
                String nowUID = se.getUID();
                newCond.eq(valueCaption, nowUID);
            }
            case plvType.groupOwn -> {
                String nowgid = se.get(fatherIDFieldName).toString();
                newCond.eq(fatherIDFieldName, nowgid);
            }
            case plvType.powerVal -> {
                int nowPowerVal = se.getInt(powvalFieldName);
                newCond.lte(valueCaption, nowPowerVal);
            }
        }
        return newCond.buildEx();
    }

    private Session getSE() {
        Session se;
        if (Session.getSID() != null) {
            se = new Session();
        } else {
            if (Session.checkSession(commonSid)) {
                se = new Session(commonSid);
            } else {
                // 不存在有效会话时,创建最低权限会话用来填平运行逻辑

                JSONObject newPermJson = JSONObject.putx(powvalFieldName, 0)
                        .puts(fatherIDFieldName, "");
                se = Session.createSession("defaultSession", newPermJson);
                commonSid = se._getSID();
            }


            // nlogger.debugInfo("会话不存在->无法实现权限验证");
        }
        return se;
    }

    public List<List<Object>> filterCond(int plvOperate) {
        List<List<Object>> rs = switch (plvOperate) {
            case PlvDef.Operater.read, PlvDef.Operater.statist -> getAuthCond(PermissionsPowerDef.readValue, tempMode.readPerm());
            case PlvDef.Operater.update -> getAuthCond(PermissionsPowerDef.updateValue, tempMode.updatePerm());
            case PlvDef.Operater.delete -> getAuthCond(PermissionsPowerDef.deleteValue, tempMode.deletePerm());
            default -> null;
        };
        //普通用户判断
        return rs;
    }

    //判断当前操作是否有权
    //判断过程中生成条件对象
    public boolean checkOperate(int plvOperate) {
        boolean rs = false;
        Session se = getSE();
        //管理员判定
        if (se.getInt(adminFieldName) >= UserMode.admin) {//是管理员模式
            rs = _checkObject(se.getUID());
            return rs;
        }
        //普通用户判断
        rs = switch (plvOperate) {
            case PlvDef.Operater.create -> _operateChk(tempMode.createPerm());
            case PlvDef.Operater.statist -> _operateChk(tempMode.statisticsPerm());
            case PlvDef.Operater.read -> _operateChk(tempMode.readPerm());
            case PlvDef.Operater.update -> _operateChk(tempMode.updatePerm());
            case PlvDef.Operater.delete -> _operateChk(tempMode.deletePerm());
            default -> false;
        };
        return rs;
    }
}
