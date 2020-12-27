package common.java.interfaceModel;

import org.json.simple.JSONArray;

@FunctionalInterface
public interface aggregation {
    /**
     * @param inputArray 需要修改的输入流
     * @param storeArray 需要合并的数据流
     * @return 聚合后的数据流
     */
    JSONArray run(JSONArray inputArray, JSONArray storeArray);
}
