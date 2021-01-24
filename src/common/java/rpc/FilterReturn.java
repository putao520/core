package common.java.rpc;

public class FilterReturn {
    private final boolean state;
    private final String msg;

    private FilterReturn(boolean state, String msg) {
        this.state = state;
        this.msg = msg;
    }

    public static FilterReturn buildTrue() {
        return new FilterReturn(true, "");
    }

    public static FilterReturn build(boolean state, String msg) {
        return new FilterReturn(state, msg);
    }

    public boolean state() {
        return state;
    }

    public String message() {
        return msg;
    }
}
