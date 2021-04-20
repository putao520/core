package common.java.Apps.Roles;

import org.json.gsc.JSONObject;

public class AppRolesDef {
    public final static Role root = Role.build("root", 10000000);
    public final static Role admin = Role.build("admin", 1000000);
    public final static Role user = Role.build("user", 100000);
    public final static Role everyone = Role.build("everyone", 0);

    public static JSONObject defaultRoles() {
        return JSONObject.build().puts(root.name, root.group_value)
                .puts(admin.name, admin.group_value)
                .puts(user.name, user.group_value)
                .puts(everyone.name, everyone.group_value);
    }
}
