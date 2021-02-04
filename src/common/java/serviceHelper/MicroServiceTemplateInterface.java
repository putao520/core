package common.java.serviceHelper;

import common.java.interfaceType.ApiType;
import common.java.rpc.RpcPageInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public interface MicroServiceTemplateInterface {
    /**
     * @param base64Json gsc-json
     * @apiNote 新增数据
     */
    @ApiType(ApiType.type.SessionApi)
    Object insert(String base64Json);

    /**
     * @param uids 主键组（不同主键值用“,”隔开）
     * @apiNote 删除数据
     */
    @ApiType(ApiType.type.SessionApi)
    int delete(String uids);

    /**
     * @param cond gsc-GraphQL
     * @apiNote 删除数据, 通过指定条件
     */
    @ApiType(ApiType.type.SessionApi)
    int deleteEx(String cond);

    /**
     * @param uids       主键组（不同主键值用“,”隔开）
     * @param base64Json gsc-json
     * @apiNote 更新数据
     */
    @ApiType(ApiType.type.SessionApi)
    int update(String uids, String base64Json);

    /**
     * @param base64Json gsc-json
     * @param cond       gsc-GraphQL
     * @apiNote 更新数据, 通过指定条件
     */
    @ApiType(ApiType.type.SessionApi)
    int updateEx(String base64Json, String cond);

    /**
     * @param idx 当前页码
     * @param max 每页最大显示数量
     * @apiNote 分页方式展示数据
     */
    @ApiType(ApiType.type.SessionApi)
    RpcPageInfo page(int idx, int max);

    /**
     * @param idx  当前页码
     * @param max  每页最大显示数量
     * @param cond gsc-GraphQL
     * @apiNote 页方式展示数据, 通过指定条件
     */
    @ApiType(ApiType.type.SessionApi)
    RpcPageInfo pageEx(int idx, int max, String cond);

    /**
     * @apiNote 获得全部数据
     */
    @ApiType(ApiType.type.SessionApi)
    @ApiType(ApiType.type.OauthApi)
    JSONArray select();

    /**
     * @param cond gsc-GraphQL
     * @apiNote 获得全部数据, 通过指定条件
     */
    @ApiType(ApiType.type.SessionApi)
    @ApiType(ApiType.type.OauthApi)
    JSONArray selectEx(String cond);

    /***
     * @apiNote 查找指定数据
     * @param key 查找的字段名
     * @param val 查找的字段值
     */
    @ApiType(ApiType.type.SessionApi)
    Object find(String key, String val);

    /***
     * @apiNote 查找指定数据, 通过指定条件
     * @param cond gsc-GraphQL
     */
    @ApiType(ApiType.type.SessionApi)
    JSONObject findEx(String cond);

    /**
     * @apiNote 根据条件获得以符合条件的数据为ROOT的构造JSON-TREE
     */
    @ApiType(ApiType.type.SessionApi)
    Object tree(String cond);

    /**
     * @apiNote 为特定的方法申请一次性授权
     * */
    // Object getApiAccessOnce(String className, String action);
}
