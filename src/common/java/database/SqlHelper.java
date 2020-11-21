package common.java.database;

public class SqlHelper {
    private static String func(String str) {
        return "func:" + str + " ";
    }

    /**
     * 10位unixtime
     */
    public static final String nowTimestamp() {
        return SqlHelper.func("now()");
    }

    /**
     * 10位unixtime
     */
    public static final String nowTimeFormat(long unixTime) {
        return func("from_unixtime(" + unixTime + ")");
    }
}
