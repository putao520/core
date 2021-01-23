package common.java.rpc;


import common.java.number.NumberHelper;
import common.java.string.StringHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class rMsg {
    public static String netMSG(Object state, Object data) {
        return netMSG(NumberHelper.number2int(state), "", data);
    }

    public static String netMSG(Object data) {
        if (data instanceof JSONObject) {
            JSONObject json = (JSONObject) data;
            if (JSONObject.isInvaild(json)) {
                return netState(false);
            }
        }
        if (data instanceof JSONArray) {
            JSONArray array = (JSONArray) data;
            if (JSONArray.isInvaild(array)) {
                return netState(false);
            }
        }
        if (data instanceof String) {
            String str = (String) data;
            if (StringHelper.invaildString(str)) {
                return netState(false);
            }
        }
        return netMSG(true, data);
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
