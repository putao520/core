package common.java.serviceHelper;


import org.json.simple.JSONObject;

public class onUpdate {
    private String[] ids;
    private JSONObject info;

    public onUpdate(String[] ids, JSONObject info) {
        this.ids = ids;
        this.info = info;
    }

    public String[] ids() {
        return ids;
    }

    public void ids(String[] ids) {
        this.ids = ids;
    }

    public JSONObject info() {
        return info;
    }

    public void info(JSONObject info) {
        this.info = info;
    }
}
