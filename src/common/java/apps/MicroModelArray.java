package common.java.apps;

import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.function.BiConsumer;

public class MicroModelArray {
    private final HashMap<String, MicroModel> mModels;

    public MicroModelArray(JSONObject mModel) {
        this.mModels = new HashMap<>();
        if (mModel != null) {
            for (String key : mModel.keySet()) {
                this.mModels.put(key, new MicroModel(mModel.getJson(key)));
            }
        }
    }

    /**
     * 获得模型对象
     */
    public MicroModel microModel(String modelName) {
        return this.mModels.get(modelName);
    }

    /**
     * 获得JSON结构数据模型对象
     */
    public JSONObject toJson() {
        JSONObject rJson = new JSONObject();
        for (String key : this.mModels.keySet()) {
            rJson.put(key, this.mModels.get(key).toJson());
        }
        return rJson;
    }

    /**
     * 遍历全部数据
     */
    public void forEach(BiConsumer<String, MicroModel> lambdal) {
        for (String key : this.mModels.keySet()) {
            lambdal.accept(key, this.mModels.get(key));
        }
    }

}
