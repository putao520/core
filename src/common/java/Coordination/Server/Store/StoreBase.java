package common.java.Coordination.Server.Store;

import common.java.String.StringHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class StoreBase {
    private final AtomicInteger index;
    private JSONArray<JSONObject> dataArr;
    private JSONObject dataMap;

    public StoreBase(JSONObject block) {
        this.index = new AtomicInteger(block.getInt("idx"));
        this.dataArr = block.getJsonArray("store");
    }

    private void updateIndex() {
        this.dataMap = dataArr.mapsByKey("id");
    }

    // 查找数据
    public JSONObject find(int idx) {
        return dataMap.getJson(String.valueOf(idx));
    }

    // 更新数据
    public StoreBase update(JSONObject json) {
        String idx = json.getString("id");
        if (StringHelper.isInvalided(idx)) {
            throw new RuntimeException("非法数据");
        }
        dataMap.getJson(idx).put(json);
        return this;
    }

    // 获得数据
    public JSONArray<JSONObject> getDataArr() {
        return this.dataArr;
    }

    // 删除数据
    public StoreBase remove(int idx) {
        dataArr = dataArr.<String>filter("id", v -> Integer.valueOf(v) != idx);
        return this;
    }

    // 插入数据
    public int insert(JSONObject data) {
        int v = index.incrementAndGet();
        this.dataArr.add(data.put("id", v));
        this.updateIndex();
        return v;
    }

}
