package common.java.rpc;


import common.java.number.NumberHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class rMsg {
    public static String netMSG(Object state, Object msg) {
        return netMSG(NumberHelper.number2int(state), "", msg);
    }

    public static String netMSG(int state, String message, Object data) {
        JSONObject newData = new JSONObject();
        newData.puts("errorcode", state).puts("record", data);
        if (state > 0) {
            newData.puts("message", message);
        }
        return newData.toJSONString();
    }

    public static String netPAGE(int idx, int max, long count, JSONArray record) {
        JSONObject rs = new JSONObject();
        if (record != null) {
            rs.put("data", record);
            if (count >= 0) {
                rs.put("totalSize", count);
            }
            if (idx >= 0) {
                rs.put("currentPage", String.valueOf(idx));
            }
            if (max >= 0) {
                rs.put("pageSize", String.valueOf(max));
            }
        }
        return netMSG(0, rs);
    }

    public static String netState(Object state) {
        return netMSG(state, "");
    }
}
