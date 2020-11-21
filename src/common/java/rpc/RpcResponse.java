package common.java.rpc;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class RpcResponse {
    private final JSONObject r;

    public RpcResponse(String rString) {
        this.r = rString == null ? new JSONObject() : JSONObject.toJSON(rString);
    }

    public RpcResponse(JSONObject rJson) {
        this.r = rJson;
    }

    public static final RpcResponse build(String rString) {
        return new RpcResponse(rString);
    }

    public static final RpcResponse build(JSONObject rJson) {
        return new RpcResponse(rJson);
    }

    @Override
    public String toString() {
        return this.r.toJSONString();
    }

    public int errCode() {
        return r != null && r.containsKey("errorcode") ? r.getInt("errorcode") : -1;
    }

    public boolean status() {
        return errCode() == 0;
    }

    public boolean hasMessage() {
        return r != null && r.containsKey("message");
    }

    public String message() {
        return r == null ? "" : r.getString("message");
    }

    public String asString() {
        return r == null ? "" : r.getString("record");
    }

    public RpcResponsePage asPage() {
        return new RpcResponsePage(r == null ? null : r.getJson("record"));
    }

    public JSONObject asJson() {
        return r == null ? null : r.getJson("record");
    }

    public boolean asBoolean() {
        return r != null && r.getBoolean("record");
    }

    public JSONArray asJsonArray() {
        if (r == null) {
            return null;
        }
        JSONArray t0 = r.getJsonArray("record");
        if (t0 == null) {
            JSONObject t1 = r.getJson("record");
            if (!JSONObject.isInvaild(t1)) {
                t0 = JSONArray.addx(t1);
            }
        }
        return t0;
    }

    public class RpcResponsePage {
        private final JSONObject r;

        public RpcResponsePage(JSONObject data) {
            this.r = data;
        }

        public int total() {
            return r == null ? 0 : r.getInt("totalSize");
        }

        public int idx() {
            return r == null ? 0 : r.getInt("idx");
        }

        public int max() {
            return r == null ? 0 : r.getInt("max");
        }

        public JSONArray record() {
            return r == null ? null : r.getJsonArray("data");
        }
    }
}
