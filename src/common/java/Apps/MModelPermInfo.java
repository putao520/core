package common.java.Apps;

import common.java.Authority.PlvDef;
import org.json.simple.JSONObject;

/**
 * 权限节点设计,包含权限类型和权限值
 */

public class MModelPermInfo {
    private final JSONObject info;

    public MModelPermInfo(JSONObject info) {
        this.info = info;
    }

    public static MModelPermInfo buildPermInfo() {
        return new MModelPermInfo(new JSONObject());
    }

    /**
     * 获得权限类型
     */
    public int type() {
        return this.info.getInt(PlvDef.plvType.chkType);
    }

    /**
     * 获得权限值
     */
    public Object value() {
        return this.info.get(PlvDef.plvType.chkVal);
    }

    /**
     * 设置类型
     */
    public MModelPermInfo type(int t) {
        this.info.put(PlvDef.plvType.chkType, t);
        return this;
    }

    /**
     * 设置值
     */
    public MModelPermInfo value(Object v) {
        this.info.put(PlvDef.plvType.chkVal, v);
        return this;
    }

    /**
     * 输出权限节点内容
     */
    public JSONObject toJson() {
        return this.info;
    }
}
