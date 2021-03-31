package common.java.Apps.Roles;

import org.json.gsc.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class AppRoles {
    private final JSONObject store;

    private AppRoles(JSONObject info) {
        this.store = info;
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
    public Set<String> gt(String roleName) {
        int basePV = this.getPV(roleName);
        if (basePV < 0) {
            return null;
        }
        Set<String> arr = new HashSet<>();
        for (String _roleName : store.keySet()) {
            if (store.getInt(_roleName) >= basePV) {
                arr.add(_roleName);
            }
        }
        return arr;
    }

    /**
     * 获得权值小于于 roleName 的全部角色名称
     */
    public Set<String> lt(String roleName) {
        int basePV = this.getPV(roleName);
        if (basePV < 0) {
            return null;
        }
        Set<String> arr = new HashSet<>();
        for (String _roleName : store.keySet()) {
            if (store.getInt(_roleName) <= basePV) {
                arr.add(_roleName);
            }
        }
        return arr;
    }
}
