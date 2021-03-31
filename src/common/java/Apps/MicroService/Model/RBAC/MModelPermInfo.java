package common.java.Apps.MicroService.Model.RBAC;

import common.java.Authority.MModelPermDef;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * 权限节点设计,包含权限类型和权限值
 */

public class MModelPermInfo {
    private final JSONObject info;

    private MModelPermInfo(JSONObject info) {
        this.info = info;
    }

    public static MModelPermInfo build(JSONObject info) {
        return new MModelPermInfo(info);
    }

    public static MModelPermInfo build() {
        return new MModelPermInfo(new JSONObject());
    }

    /**
     * 获得权限类型
     */
    public int type() {
        return this.info.getInt(MModelPermDef.perm_type_caption);
    }

    /**
     * 获得权限值(有可能为null)
     */
    public String logic() {
        return this.info.getString(MModelPermDef.perm_logic_caption);
    }

    /**
     * 获得记录值(管理员才有用)
     */
    public Set<String> value() {
        JSONArray<String> arr = this.info.getJsonArray(MModelPermDef.perm_value_caption);
        var r = new HashSet<String>();
        if (JSONArray.isInvalided(arr)) {
            return r;
        }
        for (String key : arr) {
            r.add(key);
        }
        return r;
    }

    /**
     * 设置类型
     */
    public MModelPermInfo type(int t) {
        this.info.put(MModelPermDef.perm_type_caption, t);
        return this;
    }

    /**
     * 设置值
     */
    public MModelPermInfo value(String v) {
        this.info.put(MModelPermDef.perm_type_caption, v);
        return this;
    }

    /**
     * 输出权限节点内容
     */
    public JSONObject toJson() {
        return this.info;
    }
}
