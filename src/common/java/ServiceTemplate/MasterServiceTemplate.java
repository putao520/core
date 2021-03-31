package common.java.ServiceTemplate;

import common.java.Cache.Mem.MemCache;
import common.java.Database.DbLayer;
import common.java.Encrypt.GscJson;
import common.java.HttpServer.HttpContext;
import common.java.InterfaceModel.Type.ApiType;
import common.java.Rpc.RpcPageInfo;
import common.java.String.StringHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class MasterServiceTemplate implements MicroServiceTemplateInterface {
    private static final HashMap<String, MemCache<String, String>> caches;

    static {
        caches = new HashMap<>();
    }

    // private DbLayerHelper db;
    private DbLayer fdb;
    private MemCache<String, String> _cache;
    private String commomKey;

    public MasterServiceTemplate() {

    }

    public MasterServiceTemplate(String tableName) {
        init(tableName);
    }


    /**
     * 获得fastDB 设置各类操作回调
     */
    public DbLayer getFastDB() {
        fdb.clear();
        return fdb;
    }

    @ApiType(ApiType.type.CloseApi)
    public void init(String tableName) {
        fdb = new DbLayer();
        fdb.form(tableName);
        if (caches.containsKey(tableName)) {
            _cache = caches.get(tableName);
        }
        if (_cache == null) {
            _cache = MemCache.<String, String>buildMemCache().setRefreshDuration(120)
                    .setRefreshTimeUnit(TimeUnit.SECONDS)
                    .setMaxSize(4096)
                    .setGetValueWhenExpired(key -> fdb.eq(commomKey, key).find().toString());
            caches.put(tableName, _cache);
        }

    }

    @ApiType(ApiType.type.SessionApi)
    @ApiType(ApiType.type.OauthApi)
    @Override
    public JSONArray select() {
        return fdb.select();
    }

    @ApiType(ApiType.type.SessionApi)
    @ApiType(ApiType.type.OauthApi)
    public JSONArray select(String appID) {
        return fdb.eq("appid", appID).select();
    }

    @ApiType(ApiType.type.SessionApi)
    @ApiType(ApiType.type.OauthApi)
    @Override
    public JSONArray selectEx(String cond) {
        if (fdb.where(JSONArray.toJSONArray(cond)).nullCondition()) {
            return null;
        }
        return select();
    }

    /**
     * 分页方式
     *
     * @param idx 当前页码
     * @param max 每页最大数量
     */
    @ApiType(ApiType.type.SessionApi)
    @Override
    public RpcPageInfo page(int idx, int max) {
        return RpcPageInfo.Instant(idx, max, fdb.dirty().count(), fdb.page(idx, max));
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public RpcPageInfo pageEx(int idx, int max, String cond) {
        if (fdb.where(JSONArray.toJSONArray(cond)).nullCondition()) {
            return null;
        }
        return page(idx, max);
    }

    /**
     * 更新计划任务信息
     *
     * @param uids       用,分开的id组
     * @param base64Json GSC-FastJSON:更新的内容
     */
    @ApiType(ApiType.type.SessionApi)
    @Override
    public int update(String uids, String base64Json) {
        return _update(uids, base64Json, null);
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public int updateEx(String base64Json, String cond) {
        return _update(null, base64Json, cond);
    }

    private int _update(String uids, String base64Json, String cond) {
        JSONObject info = GscJson.decode(base64Json);
        if (JSONObject.isInvalided(info)) {
            return 0;
        }
        if (HttpContext.current().appid() > 0) {//非管理员情况下
            info.remove("appid");
        }
        if (fdb.where(JSONArray.toJSONArray(cond)).nullCondition()) {
            return 0;
        }
        fdb.data(info);
        return (int) (uids != null ? fdb.putAllOr(uids).updateAll() : fdb.updateAll());
    }


    /**
     * 删除计划任务信息
     */
    @ApiType(ApiType.type.SessionApi)
    @Override
    public int delete(String uids) {
        return (int) (fdb.putAllOr(uids).nullCondition() ? 0 : fdb.deleteAll());
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public int deleteEx(String cond) {
        return (int) (fdb.where(JSONArray.toJSONArray(cond)).nullCondition() ? 0 : fdb.deleteAll());
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public String insert(String base64Json) {
        String rString = null;
        JSONObject nObj = GscJson.decode(base64Json);
        if (nObj != null) {
            nObj.put("appid", HttpContext.current().appid());
            rString = StringHelper.toString(fdb.data(nObj).insertOnce());
        }
        return rString;
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public String find(String key, String val) {
        return StringHelper.toString(fdb.eq(key, val).find());
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public JSONObject findEx(String cond) {
        return fdb.where(JSONArray.toJSONArray(cond)).nullCondition() ? null : fdb.find();
    }

    @Override
    public String tree(String cond) {
        return "";
    }
}
