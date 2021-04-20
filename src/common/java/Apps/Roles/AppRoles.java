package common.java.Apps.Roles;

import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AppRoles {
    private final JSONObject store;

    private AppRoles(JSONObject info) {
        // 如果应用未配置角色,使用缺省角色,否则合并角色
        JSONObject defaultRoleArr = buildDefaultRoles();
        this.store = JSONObject.isInvalided(info) ? defaultRoleArr : info.putAlls(defaultRoleArr);
        // this.updateMaxAndMin();
    }

    private JSONObject buildDefaultRoles() {
        return AppRolesDef.defaultRoles();
    }

    public static final AppRoles build(JSONObject info) {
        return new AppRoles(info);
    }

    /**
     * 获得角色权值
     */
    public int getPV(String roleName) {
        return store.containsKey(roleName) ? store.getInt(roleName) : -1;
    }

    /**
     * 获得权值大于 roleName 的全部角色名称
     */
    public List<String> gt(String roleName) {
        List<String> arr = new ArrayList<>();
        if (roleName != null) {
            int basePV = this.getPV(roleName);
            if (basePV >= 0) {
                for (String _roleName : store.keySet()) {
                    if (store.getInt(_roleName) >= basePV) {
                        arr.add(_roleName);
                    }
                }
            }
        }
        return arr;
    }

    /**
     * 获得权值小于于 roleName 的全部角色名称
     */
    public List<String> lt(String roleName) {
        List<String> arr = new ArrayList<>();
        if (roleName != null) {
            int basePV = this.getPV(roleName);
            if (basePV >= 0) {
                for (String _roleName : store.keySet()) {
                    if (store.getInt(_roleName) <= basePV) {
                        arr.add(_roleName);
                    }
                }
            }
        }
        return arr;
    }

    /*
    private void updateMaxAndMin(){
        int temp_max = Integer.MIN_VALUE;
        String temp_max_name = null;
        int temp_min = Integer.MAX_VALUE;
        String temp_min_name = null;
        int current_val = 0;
        for (String _roleName : store.keySet()) {
            current_val = store.getInt(_roleName);
            if( current_val > temp_max){
                temp_max = current_val;
                temp_max_name = _roleName;
            }
            if( current_val < temp_min){
                temp_min = current_val;
                temp_min_name = _roleName;
            }
        }
        this.maxRole = temp_max_name;
        this.minRole = temp_min_name;
    }
    */

    public String getMaxRole(List<String> values) {
        return values.size() > 0 ? values.get(values.size() - 1) : null;
    }

    public String getMinRole(List<String> values) {
        return values.size() > 0 ? values.get(0) : null;
    }
}
