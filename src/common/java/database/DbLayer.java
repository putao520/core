package common.java.database;

import common.java.Config.nConfig;
import common.java.Reflect._reflect;
import common.java.apps.AppContext;
import common.java.apps.MicroServiceContext;
import common.java.cache.Cache;
import common.java.httpServer.HttpContext;
import common.java.nlogger.nlogger;
import common.java.string.StringHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;


public class DbLayer implements InterfaceDatabase<DbLayer> {
    private final HashMap<String, List<Function<Object, Object>>> outHookFunc = new HashMap<>();
    private final HashMap<String, List<Function<Object, Object>>> inHookFunc = new HashMap<>();
    public int _dbName;
    public String formName;
    private _reflect _db;            //数据库抽象对象
    private Cache cache;        //缓存抽象对象
    private String ownid;

    public DbLayer() {
        init(null);
    }

    public DbLayer(String configName) {
        init(configName);
    }

    public DbLayer addFieldOutPipe(String fieldName, Function<Object, Object> func) {
        if (func != null) {
            List<Function<Object, Object>> link = outHookFunc.get(fieldName);
            if (link == null) {
                link = new ArrayList<>();
            }
            link.add(func);
            outHookFunc.put(fieldName, link);
        }
        return this;
    }

    public DbLayer addFieldInPipe(String fieldName, Function<Object, Object> func) {
        if (func != null) {
            List<Function<Object, Object>> link = inHookFunc.get(fieldName);
            if (link == null) {
                link = new ArrayList<>();
            }
            link.add(func);
            inHookFunc.put(fieldName, link);
        }
        return this;
    }

    private void fieldPiper(JSONObject data, HashMap<String, List<Function<Object, Object>>> inList) {
        if (JSONObject.isInvaild(data)) {
            return;
        }
        for (String k : inList.keySet()) {
            if (data.containsKey(k)) {
                Object outVal = data.get(k);
                List<Function<Object, Object>> link = inList.get(k);
                for (Function<Object, Object> func : link) {
                    outVal = func.apply(outVal);
                }
                data.put(k, outVal);
            }
        }
    }

    private Object fieldOutPiper(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof JSONArray) {
            for (Object item : (JSONArray) data) {
                fieldPiper((JSONObject) item, outHookFunc);
            }
        } else if (data instanceof JSONObject) {
            fieldPiper((JSONObject) data, outHookFunc);
        }
        return data;
    }


    private _reflect getDBObject(String cN) {
        String dbName;
        JSONObject obj;
        String _configString = nConfig.netConfig(cN);
        try {
            if (_configString != null) {
                obj = JSONObject.toJSON(_configString);
                if (obj != null) {
                    dbName = obj.getString("dbName");
                    switch (dbName) {
                        case "mongodb":
                            _db = (new _reflect(Mongodb.class)).newInstance(_configString);
                            _dbName = dbType.mongodb;
                            break;
                        default://mysql
                            _db = (new _reflect(Sql.class)).newInstance(_configString);
                            _dbName = dbType.mysql;
                            break;
                    }
                } else {
                    nlogger.logInfo("DB配置信息格式错误 ：" + _configString);
                }
            } else {
                nlogger.logInfo("DB配置信息[" + cN + "]为空:=>" + _configString);
            }
            _db.privateMode();//内部调用，启动私有模式
        } catch (Exception e) {
            nlogger.logInfo(e, "连接关系型数据系统失败! 配置名:[" + cN + "]");
            _db = null;
        }
        return _db;
    }

    private Cache getCache() {
        if (cache == null) {
            try {
                String cacheConfigName = null;
                if (MicroServiceContext.current().hasData()) {
                    cacheConfigName = MicroServiceContext.current().config().cache();
                } else if (AppContext.current().hasData()) {
                    cacheConfigName = AppContext.current().config().cache();
                }
                cache = cacheConfigName != null ? new Cache(cacheConfigName) : null;
            } catch (Exception e) {
                cache = null;
                nlogger.logInfo(e, "数据系统绑定的缓存系统初始化失败");
            }
        }
        return cache;
    }

    private void init(String inputConfigName) {
        try {
            String configName = null;
            if (inputConfigName == null) {
                if (MicroServiceContext.current().hasData()) {
                    configName = MicroServiceContext.current().config().db();
                } else if (AppContext.current().hasData()) {
                    configName = AppContext.current().config().db();
                }
            } else {
                configName = inputConfigName;
            }
            if (configName == null || configName.equals("")) {
                nlogger.logInfo("数据库配置丢失");
            }
            _db = getDBObject(configName);

        } catch (Exception e) {
            // TODO: handle exception
            nlogger.logInfo(e, "DB配置读取失败");
        }
    }

    /**
     * 从缓存取数据，如果缓存不存在数据，那么从数据库取并填充
     *
     * @return
     */
    public JSONArray selectbyCache() {
        return selectbyCache(3);
    }

    /**
     * 从缓存取数据，如果缓存不存在数据，那么从数据库取并填充
     *
     * @return
     */
    public JSONArray selectbyCache(int second) {
        JSONArray rs = null;
        String key = getformName() + condString();
        Cache c = getCache();
        if (c != null) {
            rs = JSONArray.toJSONArray(c.get(key));
        }
        if (rs == null) {//不存在
            rs = select();
            if (rs != null && c != null) {
                if (rs.size() > 0 && c != null) {
                    c.set(key, rs.toJSONString(), second);
                }
            }
        }
        return rs;
    }

    public void InvaildCache() {
        String key = getformName() + condString();
        Cache c = getCache();
        if (c != null) {
            c.delete(key);
        }
    }

    public JSONObject findbyCache(int second) {
        JSONObject rs = null;
        String key = getformName() + condString();
        Cache c = getCache();
        if (c != null) {
            rs = JSONObject.toJSON(c.get(key));
        }
        if (rs == null) {//不存在
            rs = this.find();
            if (rs != null && c != null) {
                if (rs.size() > 0 && c != null) {
                    c.set(key, rs.toJSONString(), second);
                }
            }
        }
        return rs;
    }

    public JSONObject findbyCache() {
        return findbyCache(3);
    }

    private void updatefix() {
        form(formName);
        bind(ownid);
    }

    //---------------------------db接口引用
    public void Close() {
        //_db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public void addConstantCond(String fieldName, Object CondValue) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName, CondValue);
    }

    public DbLayer and() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    public DbLayer or() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    public boolean nullCondition() {
        return (boolean) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer where(JSONArray condArray) {
        if (condArray == null) {
            condArray = new JSONArray();
        }
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), condArray);
        return this;
    }

    public DbLayer groupCondition(List<List<Object>> conds) {
        if (conds == null) {
            conds = new ArrayList<>();
        }
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), conds);
        // System.out.println(getCond());
        return this;
    }
	/*
	public DbLayer where(List<List<Object>> condList){
		if( condList == null ){
			condList = new ArrayList<>();
		}
		_db._call(Thread.currentThread().getStackTrace()[1].getMethodName(),condList);
		return this;
	}
	*/

    public DbLayer eq(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer ne(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer gt(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer lt(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer gte(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer lte(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer like(String field, Object value) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer data(String jsonString) {//One Condition
        return data(JSONObject.toJSON(jsonString));
    }

    public DbLayer data(JSONObject doc) {//One Condition
        fieldPiper(doc, inHookFunc);
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), doc);
        return this;
    }

    public DbLayer field() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    public DbLayer field(String fieldString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldString);
        return this;
    }

    public DbLayer mask(String fieldString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldString);
        return this;
    }

    public DbLayer form(String _formName) {
        formName = _formName;
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), _formName);
        return this;
    }

    public DbLayer skip(int no) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), no);
        return this;
    }

    public DbLayer limit(int no) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), no);
        return this;
    }

    public DbLayer asc(String field) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field);
        return this;
    }

    public DbLayer desc(String field) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field);
        return this;
    }

    public DbLayer findOne() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    @SuppressWarnings("unchecked")
    public List<Object> insert() {
        updatefix();
        return (List<Object>) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONObject update() {
        updatefix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public long updateAll() {
        updatefix();
        return (long) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONObject delete() {
        updatefix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public long deleteAll() {
        updatefix();
        return (long) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONObject inc(String fieldName) {
        updatefix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName);
    }

    public JSONObject dec(String fieldName) {
        updatefix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName);
    }

    public JSONObject add(String fieldName, long num) {
        updatefix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName, num);
    }

    public JSONObject find() {
        updatefix();
        return (JSONObject) fieldOutPiper(_db._call(Thread.currentThread().getStackTrace()[1].getMethodName()));
    }

    public JSONArray select() {
        updatefix();
        return (JSONArray) fieldOutPiper(_db._call(Thread.currentThread().getStackTrace()[1].getMethodName()));
    }

    public String condString() {
        updatefix();
        return (String) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONArray group() {
        updatefix();
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONArray group(String groupName) {
        updatefix();
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupName);
    }

    public JSONArray distinct(String fieldName) {
        updatefix();
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName);
    }

    public JSONArray page(int pageidx, int pagemax) {
        updatefix();
        return (JSONArray) fieldOutPiper(_db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), pageidx, pagemax));
    }

    public final JSONArray page(int pageidx, int pagemax, int lastid, String fastfield) {
        updatefix();
        return (JSONArray) fieldOutPiper(_db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), pageidx, pagemax, lastid, fastfield));
    }

    public long count() {
        return (long) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer count(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    public DbLayer max(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    public DbLayer min(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    public DbLayer avg(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    public DbLayer sum(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    /*
    public String getformName() {
        return formName;
        //return (String)_db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }
    */
    public String getformName() {
        return formName;
        //return (String)_db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public String getform() {
        return (String) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public String getfullform() {
        return (String) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public void asyncInsert() {
        updatefix();
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public Object insertOnce() {
        updatefix();
        return _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer bind(String ownerID) {
        ownid = ownerID == null || ownerID.equals("0") ? "" : ownerID;
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), ownid);
        return this;
    }

    public DbLayer bind() {
        int appid = HttpContext.current().appid();
        if (appid == 0) {
            //throw new NullPointerException("appid or intid is zero");
        } else {
            try {
                ownid = StringHelper.any2String(appid);
                bind(ownid);
            } catch (Exception e) {
                nlogger.logInfo(e, "appid is not a integer");
            }
        }
        return this;
    }

    public DbLayer bindApp() {
        bind();
        return this;
    }

    public int limit() {
        return (int) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public int pageMax(int max) {
        return (int) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), max);
    }

    public String getGeneratedKeys() {
        return (String) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer dirty() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    public void clear() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONArray scan(Function<JSONArray, JSONArray> func, int max) {
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), func, max);
    }

    public JSONArray scan(Function<JSONArray, JSONArray> func, int max, int synNo) {
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), func, max, synNo);
    }

    public JSONObject getCond() {
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer setCond(JSONObject conJSON) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), conJSON);
        return this;
    }

    public List<String> getAllTables() {
        return (List<String>) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public static class dbType {
        public final static int mongodb = 1;
        public final static int mysql = 2;
    }
}