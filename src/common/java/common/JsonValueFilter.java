package common.java.common;

import common.java.session.UserSession;
import common.java.string.StringHelper;
import org.json.simple.JSONObject;

public class JsonValueFilter {
    private final JSONObject in;
    private final JSONObject otherData;
    private JSONObject userSession;

    public JsonValueFilter(JSONObject in) {
        this.in = in;
        this.otherData = new JSONObject();
        this.userSession = UserSession.current().getDatas();
    }

    public static final JsonValueFilter build(JSONObject in) {
        return new JsonValueFilter(in);
    }

    public JsonValueFilter addOther(JSONObject in) {
        if (in != null) {
            otherData.putAll(in);
        }
        return this;
    }

    public JsonValueFilter setSessionInfo(JSONObject sessionInfo) {
        this.userSession = sessionInfo;
        return this;
    }

    public final JSONObject filter() {
        JSONObject rJson = new JSONObject();
        for (String key : in.keySet()) {
            rJson.put(key, this.conv(in.get(key)));
        }
        return rJson;
    }

    private final Object conv(Object val) {
        Object r = val;
        String[] cap = StringHelper.any2String(val).split("::");
        if (cap.length > 1) {
            String caption = cap[0].toLowerCase();
            switch (caption) {
                case "session":
                    r = JSONObject.isInvaild(this.userSession) ? null : this.userSession.get(cap[1]);
                    break;
                case "other":
                    r = this.otherData.getString(cap[1]);
                    break;
            }
        }
        return r;
    }
}
