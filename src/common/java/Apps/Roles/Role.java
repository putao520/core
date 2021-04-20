package common.java.Apps.Roles;

public class Role {
    public final String name;
    public final int group_value;

    private Role(String name, int group_value) {
        this.name = name;
        this.group_value = group_value;
    }

    public static Role build(String name, int group_value) {
        return new Role(name, group_value);
    }

    public static Role build(String name) {
        return new Role(name, 0);
    }

    public int compareTo(Role r) {
        return this.group_value - r.group_value;
    }

    public String toString() {
        return this.name;
    }
}
