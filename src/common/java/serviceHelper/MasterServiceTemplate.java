package common.java.serviceHelper;

import common.java.cache.MemCache;
import common.java.encrypt.GscJson;
import common.java.httpServer.HttpContext;
import common.java.interfaceType.ApiType;
import common.java.string.StringHelper;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class MasterServiceTemplate implements MicroServiceTemplateInterface {
    private static final HashMap<String, MemCache<String, String>> caches;

    static {
        caches = new HashMap<>();
        /*
        caches = MemCache.<String,String>buildMemCache().setRefreshDuration(120)
                .setRefreshTimeUnit(TimeUnit.SECONDS)
                .setMaxSize(4096)
                .setGetValueWhenExpired( key ->{
                    String[] temp = key.split("#");
                    String url = temp[0];
                    String param = StringHelper.join(temp,"#", 1, -1);
                    return MasterProxy.postRpc(url,param);
                } );
                */
    }

    // private DbLayerHelper db;
    private fastDBService fdb;
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
    public fastDBService getFastDB() {
        fdb._getDB().clear();
        return fdb;
    }

    @ApiType(ApiType.type.CloseApi)
    public void init(String tableName) {
        fdb = new fastDBService(tableName);
        fdb.fliterPlv();
        if (caches.containsKey(tableName)) {
            _cache = caches.get(tableName);
        }
        if (_cache == null) {
            _cache = MemCache.<String, String>buildMemCache().setRefreshDuration(120)
                    .setRefreshTimeUnit(TimeUnit.SECONDS)
                    .setMaxSize(4096)
                    .setGetValueWhenExpired(key -> fdb.find(commomKey, key));
            caches.put(tableName, _cache);
        }

    }

    /**
     * 获得计划任务信息
     */
    @ApiType(ApiType.type.SessionApi)
    @ApiType(ApiType.type.OauthApi)
    @Override
    public String select() {
        return fdb.select();
    }

    @ApiType(ApiType.type.SessionApi)
    @ApiType(ApiType.type.OauthApi)
    /**
     * @param cond GSC-SQL-JSON:查询条件
     * */
    @Override
    public String selectEx(String cond) {
        return fdb.where(cond) ? fdb.select() : fdb.errorMsg();
    }

    /**
     * 分页方式获得计划任务信息
     *
     * @param idx 当前页码
     * @param max 每页最大数量
     */
    @ApiType(ApiType.type.SessionApi)
    @Override
    public String page(int idx, int max) {
        return fdb.page(idx, max);
    }

    @ApiType(ApiType.type.SessionApi)
    /**
     * @param cond GSC-SQL-JSON:查询条件
     * */
    @Override
    public String pageEx(int idx, int max, String cond) {
        return fdb.where(cond) ? fdb.page(idx, max) : fdb.errorMsg();
    }

    /**
     * 更新计划任务信息
     *
     * @param uids       用,分开的id组
     * @param base64Json GSC-FastJSON:更新的内容
     */
    @ApiType(ApiType.type.SessionApi)
    @Override
    public String update(String uids, String base64Json) {
        return _update(uids, base64Json, null);
    }

    @ApiType(ApiType.type.SessionApi)
    /**
     * @param cond GSC-SQL-JSON:查询条件
     * */
    @Override
    public String updateEx(String base64Json, String cond) {
        return _update(null, base64Json, cond);
    }

    private String _update(String uids, String base64Json, String cond) {
        String rString = null;
        JSONObject info = GscJson.decode(base64Json);
        if (info != null) {
            if (HttpContext.current().appid() > 0) {//非管理员情况下
                info.remove("appid");
            }
            if (!StringHelper.invaildString(cond) && !fdb.where(cond)) {
                return fdb.errorMsg();
            }
            rString = uids != null ? fdb.update(uids, info) : fdb.update(info);
        }
        return rString;
    }


    /**
     * 删除计划任务信息
     */
    @ApiType(ApiType.type.SessionApi)
    @Override
    public String delete(String uids) {
        return fdb.delete(uids);
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public String deleteEx(String cond) {
        return fdb.where(cond) ? fdb.delete() : fdb.errorMsg();
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public String insert(String base64Json) {
        String rString = null;
        JSONObject nObj = GscJson.decode(base64Json);
        if (nObj != null) {
            nObj.put("appid", HttpContext.current().appid());
            rString = fdb.insert(nObj);
        }
        return rString;
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public String find(String key, String val) {
        /*
        commomKey = key;
        return _cache.getValue(val);
        */
        return fdb.find(key, val);
    }

    @ApiType(ApiType.type.SessionApi)
    @Override
    public String findEx(String cond) {
        return fdb.where(cond) ? fdb.find() : fdb.errorMsg();
    }

    @Override
    public String tree(String cond) {
        return "";
    }
}
