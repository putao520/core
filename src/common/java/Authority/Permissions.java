package common.java.Authority;

import common.java.Apps.MModelPerm;
import common.java.Apps.MModelPermInfo;
import common.java.Authority.PlvDef.UserMode;
import common.java.Authority.PlvDef.plvType;
import common.java.Database.DbFilter;
import common.java.Number.NumberHelper;
import common.java.Session.Session;
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
    private static final Session guesserSession;
    private static final String powvalFieldName = PermissionsPowerDef.powerValField;
    private static final String fatherIDFieldName = PermissionsPowerDef.fatherIDField;
    private static final String adminFieldName = PermissionsPowerDef.adminField;
    // private static final String userFieldName = PermissionsPowerDef.userField;
    // private static final String saltFieldName = PermissionsPowerDef.saltField;
    private static String commonSid = null;
    private final String objName;
    private MModelPerm tempMode = null;

    static {
        guesserSession = Session.build().memSession("defaultSession", JSONObject.putx(powvalFieldName, 0).puts(fatherIDFieldName, ""));
    }

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
        // 如果没有权限会话，直接返回不通过
        int chkType = pInfo.type();
        switch (chkType) {
            case plvType.userOwn -> {
                String nowUID = se.getUID();
                newCond.eq(valueCaption, nowUID);
            }
            case plvType.groupOwn -> {
                String nowGid = se.get(fatherIDFieldName).toString();
                newCond.eq(fatherIDFieldName, nowGid);
            }
            case plvType.powerVal -> {
                int nowPowerVal = se.getInt(powvalFieldName);
                newCond.lte(valueCaption, nowPowerVal);
            }
        }
        return newCond.buildEx();
    }

    // 获得对象权限设置
    private Session getSE() {
        if (commonSid.equalsIgnoreCase("guesser")) {
            return guesserSession;
        }
        if (Session.checkSession(commonSid)) {
            return Session.build(commonSid);
        }
        // 不存在有效会话时,返回默认会话
        commonSid = "guesser";
        return guesserSession;
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
        Session se = getSE();
        //管理员判定
        if (se.getInt(adminFieldName) >= UserMode.admin) {//是管理员模式
            return _checkObject(se.getUID());
        }
        //普通用户判断
        return switch (plvOperate) {
            case PlvDef.Operater.create -> _operateChk(tempMode.createPerm());
            case PlvDef.Operater.statist -> _operateChk(tempMode.statisticsPerm());
            case PlvDef.Operater.read -> _operateChk(tempMode.readPerm());
            case PlvDef.Operater.update -> _operateChk(tempMode.updatePerm());
            case PlvDef.Operater.delete -> _operateChk(tempMode.deletePerm());
            default -> false;
        };
    }
}
