package common.java.httpServer;

import common.java.string.StringHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class HttpContextDb {
    public final static String fields = "GrapeDbFields";
    public final static String sorts = "GrapeDbSorts";
    public final static String options = "GrapeDbOptions";
    private final JSONObject sort_object;
    private final JSONObject options_object;
    private JSONArray field_array;
    private boolean field_logic_not = false;

    public HttpContextDb(JSONObject nHeader) {
        // 初始化字段组
        field_array = nHeader.getJsonArray(fields);
        if (JSONArray.isInvaild(field_array)) {
            field_array = new JSONArray();
            JSONObject not_field_json = nHeader.getJson(fields);
            if (!JSONObject.isInvaild(not_field_json)) {
                for (String key : not_field_json.keySet()) {
                    if (key.equalsIgnoreCase("not")) {
                        field_logic_not = true;
                    }
                    field_array = not_field_json.getJsonArray(key);
                }
            }
        }
        // 初始化排序组
        sort_object = nHeader.getJson(sorts);
        // 初始化开关
        options_object = nHeader.getJson(options);
        // 删除数据
        // nHeader.remove(fields);
        // nHeader.remove(sorts);
        // nHeader.remove(options);
    }

    /**
     * 获得header里的DB参数
     */
    public JSONObject header(JSONObject nHeader) {
        JSONObject r = JSONObject.putx(fields, nHeader.getString(fields))
                .puts(sorts, nHeader.getString(sorts))
                .puts(options, nHeader.getString(options));
        nHeader.remove(fields);
        nHeader.remove(sorts);
        nHeader.remove(options);
        return r;
    }

    public boolean notIn() {
        return field_logic_not;
    }

    public boolean hasFields() {
        return field_array.size() > 0;
    }

    public String fields() {
        String r = "";
        for (Object _o : field_array) {
            r += (_o + ",");
        }
        return StringHelper.build(r).trimFrom(',').toString();
    }

    public JSONObject sort() {
        return sort_object;
    }

    public JSONObject option() {
        return options_object;
    }
}
