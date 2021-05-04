package common.java.Database;

import com.mongodb.MongoClient;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import common.java.nLogger.nLogger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.json.StrictJsonWriter;
import org.bson.types.ObjectId;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * {
 * "keepalive": true,
 * "dbName": "Mongodb",
 * "user": "",
 * "password": "",
 * "database": "test",
 * "replicaSet": "repset",
 * "nodeAddresses": ["123.57.213.15:27017", "123.57.213.15:27018", "123.57.213.15:27019"]
 * }
 */
public class Mongodb {
    private static final HashMap<String, MongoClient> DataSource;
    private static final JsonWriterSettings build;

    static {
        build = JsonWriterSettings.builder()
                .outputMode(JsonMode.EXTENDED)
                .int64Converter((Long value, StrictJsonWriter writer) -> writer.writeNumber(Long.toString(value)))
                .int32Converter((Integer value, StrictJsonWriter writer) -> writer.writeNumber(Integer.toString(value)))
                .objectIdConverter((ObjectId value, StrictJsonWriter writer) -> writer.writeString(value.toString()))
                .build();
        DataSource = new HashMap<>();
        java.util.logging.Logger.getLogger("org.Mongodb.driver").setLevel(Level.SEVERE);
    }

    private final boolean _clearRes = false;
    private MongoDatabase mongoDatabase;
    private String _configString;
    //private static Mongodb Mongodb;
    private MongoClient mongoClient;
    /*
     * 查询相关代码
     * {
     * 	[
     * 		["and","key","eq","value"],
     * 		["and","key","eq","value"],
     * 		["and","key","eq","value"]
     * 		...
     *  ]
     *
     * }
     */
    private boolean conditiobLogicAnd;
    private List<List<Object>> conditionJSON;    //条件
    private String formName;
    private String groupbyfield;
    private int skipNo;
    private int limitNo;
    private MongoCollection<Document> collection;
    private BasicDBObject fieldBSON;
    private List<Bson> sortBSON;
    private List<Document> dataBSON;
    private List<Bson> updateBSON;
    private boolean _count;
    private boolean _max;
    private boolean _min;
    private boolean _sum;
    private boolean _avg;
    private boolean _distinct;
    private boolean _atom;
    private String ownid;
    private boolean isDirty;
    private HashMap<String, Object> constantConds;

    public Mongodb(String configString) {
        _configString = configString;
        constantConds = new HashMap<>();
        initMongodb();
        reinit();
    }

    Mongodb() {
        isDirty = false;
        formName = "";
        reinit();
    }

    public static JSONObject bson2json(Document _bson) {
        return _bson != null ? JSONObject.toJSON(_bson.toJson(build)) : new JSONObject();
    }

    private void initMongodb() {
        try {
            //DebugPerformance dp = new DebugPerformance();
            mongodbConfig mongodbConfig = new mongodbConfig();
            MongoClientURI mgoURI = mongodbConfig.json2mongouri(_configString);
            if (DataSource.containsKey(_configString)) {
                mongoClient = DataSource.get(_configString);
            } else {
                //MongoClientOptions.Builder build = new MongoClientOptions.Builder();
                synchronized (this) {
                    try {
                        mongoClient = new MongoClient(mgoURI);
                        ReadPreference.primaryPreferred();
                        DataSource.put(_configString, mongoClient);
                    } catch (Exception e) {
                        if (mongoClient != null) {
                            mongoClient.close();
                        }
                        nLogger.logInfo(e);
                    }
                }
            }
            mongoDatabase = mongoClient.getDatabase(mongodbConfig.database);
            //dp.end();
        } catch (Exception e) {
            if (mongoClient != null) {
                mongoClient.close();
            }
            nLogger.logInfo(e);
        }
    }

    public Mongodb data(JSONObject doc) {
        dataBSON.add(json2document(doc));
        return this;
    }

    public Mongodb data(String jsonString) {
        data(JSONObject.toJSON(jsonString));
        return this;
    }

    public List<JSONObject> clearData() {
        List<JSONObject> v = data();
        dataBSON.clear();
        return v;
    }

    public List<JSONObject> data() {
        List<JSONObject> arr = new ArrayList<>();
        for (var doc : dataBSON) {
            arr.add(bson2json(doc));
        }
        return arr;
    }

    private void reinit() {
        if (isDirty) {//脏执行下不重置
            isDirty = false;
            return;
        }
        //tempCondtion = new ArrayList<>();
        conditionJSON = new ArrayList<>();
        conditiobLogicAnd = true;
        fieldBSON = new BasicDBObject();

        sortBSON = new ArrayList<>();
        dataBSON = new ArrayList<>();
        updateBSON = new ArrayList<>();
        limitNo = 0;
        skipNo = 0;

        _count = false;
        _max = false;
        _min = false;
        _sum = false;
        _avg = false;

        _distinct = false;
        _atom = false;

        groupbyfield = "";

        ownid = null;


        and();

        for (String _key : constantConds.keySet()) {//补充条件
            eq(_key, constantConds.get(_key));
        }
    }

    public void addConstantCond(String fieldName, Object CondValue) {
        and();
        constantConds.put(fieldName, CondValue);
        eq(fieldName, CondValue);//载入的时候填入条件
    }

    public Mongodb and() {
        conditiobLogicAnd = true;
        return this;
    }

    public Mongodb or() {
        conditiobLogicAnd = false;
        return this;
    }

    private Object _getID(String field, Object value) {
        Object rvalue = value;
        if (field != null && field.equalsIgnoreCase("_id")) {
            if (value instanceof String) {
                rvalue = new ObjectId(value.toString());
            }
            if (value instanceof JSONObject) {
                rvalue = new ObjectId(((JSONObject) value).getString("$oid"));
            }
        }
        return rvalue;
    }

    /**
     * 判断条件是否为空
     *
     * @return
     */
    public boolean nullCondition() {
        return conditionJSON.size() == 0;
    }

    public Mongodb where(List<List<Object>> condArray) {
        conditionJSON.addAll(condArray);
        return this;
    }
    //逻辑变化的时候
    //@SuppressWarnings("unchecked")

    /**
     * 条件组 field,logic,value
     *
     * @param condArray
     * @return
     */
    public Mongodb where(JSONArray condArray) {
        JSONObject tmpJSON;
        String field, logic, link_login;
        Object value;
        if (condArray == null) {
            return null;
        }

        if (condArray.size() > 0) {
            for (Object jObject : condArray) {
                field = null;
                logic = null;
                value = null;
                link_login = null;
                tmpJSON = (JSONObject) jObject;
                if (tmpJSON.containsKey("logic")) {
                    logic = (String) tmpJSON.get("logic");
                }
                if (tmpJSON.containsKey("value")) {
                    value = tmpJSON.get("value");
                }
                if (tmpJSON.containsKey("field")) {
                    field = (String) tmpJSON.get("field");
                }
                if (tmpJSON.containsKey("link_logic")) {
                    link_login = (String) tmpJSON.get("link_logic");
                }
                if (logic != null && field != null) {
                    if (link_login == null) {
                        addCondition(field, value, logic);
                    } else {
                        addCondition(field, value, logic, link_login.equalsIgnoreCase("and"));
                    }
                } else {
                    nLogger.errorInfo(condArray + " ->输入的 条件对象无效");
                }
            }
            return this;
        }
        return null;

    }

    private String logic2mongodb(String logic) {
        switch (logic) {
            case "=":
            case "==":
                return "$eq";
            case "!=":
                return "$ne";
            case ">":
                return "$gt";
            case "<":
                return "$lt";
            case ">=":
                return "$gte";
            case "<=":
                return "$lte";
            case "like":
                return "$regex";
            default:
                return logic;
        }
    }

    private void addCondition(String field, Object value, String logic) {
        addCondition(field, value, logic, conditiobLogicAnd);
    }

    private void addCondition(String field, Object value, String logic, boolean link_logic) {
        //fixCondObject();
        BasicDBList logicBSON;
        //JSONObject tempObject;
        String logicStr;
        if (value != null) {
            value = _getID(field, value);
            logicStr = logic2mongodb(logic);
            if (logicStr.equalsIgnoreCase("$regex")) {
                value = "^.*" + value.toString() + ".*$";
            }
            List<Object> bit = new ArrayList<>();

            // bit.add(conditiobLogicAnd ? "and" : "or");
            bit.add(link_logic ? "and" : "or");
            bit.add(field);
            bit.add(logicStr);
            bit.add(value);

            conditionJSON.add(bit);
            //near,in,nin,nor,not,text
        }
    }

    public Mongodb eq(String field, Object value) {//One Condition
        addCondition(field, value, "=");
        return this;
    }

    public Mongodb ne(String field, Object value) {//One Condition

        addCondition(field, value, "!=");
        return this;
    }

    public Mongodb gt(String field, Object value) {//One Condition

        addCondition(field, value, ">");
        return this;
    }

    public Mongodb lt(String field, Object value) {//One Condition

        addCondition(field, value, "<");
        return this;
    }

    public Mongodb gte(String field, Object value) {//One Condition

        addCondition(field, value, ">=");
        return this;
    }

    public Mongodb lte(String field, Object value) {//One Condition

        addCondition(field, value, "<=");
        return this;
    }

    public Mongodb like(String field, Object value) {

        //Pattern _value = Pattern.compile("^.*" + value.toString()+ ".*$", Pattern.CASE_INSENSITIVE);
        //这里根据value转换一下标识

        addCondition(field, value, "like");
        //addCondition(field,"^.*" + value.toString()+ ".*$","like");
        return this;
    }

    public Mongodb field() {
        fieldBSON.clear();
        return this;
    }

    public Mongodb field(String fieldString) {
        String[] fieldList = fieldString.split(",");
        return fieldOperate(fieldList, 1);
    }

    public Mongodb mask(String fieldString) {
        String[] fieldList = fieldString != null ? fieldString.split(",") : null;
        return fieldOperate(Objects.requireNonNull(fieldList), 0);
    }

    public Mongodb field(String[] fieldList) {
        return fieldOperate(fieldList, 1);
    }

    public Mongodb mask(String[] fieldList) {
        return fieldOperate(fieldList, 0);
    }

    private Mongodb fieldOperate(String[] fieldList, int visable) {
        //fieldBSON.put("_id",0);
        checkFieldMix(visable);
        for (String s : fieldList) {
            fieldBSON.put(s, visable);
        }
        return this;
    }

    private void checkFieldMix(int visable) {
        Object temp;
        boolean needClear = false;
        for (Object obj : fieldBSON.keySet()) {
            temp = fieldBSON.get(obj);
            if (temp != null && !obj.toString().equalsIgnoreCase("_id")) {
                if ((int) temp != visable) {//发现 字段冲突
                    needClear = true;
                    break;
                }
            }
        }
        if (needClear) {
            fieldBSON.clear();
        }
    }

    public Mongodb form(String _formName) {
        formName = _formName;
        collection = mongoDatabase.getCollection(getfullform());
        return this;
    }

    public Mongodb skip(int no) {
        skipNo = no;
        return this;
    }

    public String getfullform() {
        return ownid == null || ownid.equals("") ? formName : formName + "_" + ownid;
    }

    public String getForm() {
        return formName;
    }

    public Mongodb limit(int no) {
        limitNo = no;
        return this;
    }

    public Mongodb asc(String field) {
        sortBSON.add(Sorts.ascending(field));
        return this;
    }

    public Mongodb desc(String field) {
        sortBSON.add(Sorts.descending(field));
        return this;
    }

    public Mongodb findOne() {
        _atom = true;
        return this;
    }

    public List<Document> clearDocument(List<Document> imp) {
        for (Document doc : imp) {
            doc.remove("_id");
        }
        return imp;
    }

    public List<Object> insert() {
        List<Object> rList = new ArrayList<>();
        dataBSON = clearDocument(dataBSON);
        if (dataBSON.size() > 1) {
            collection.insertMany(dataBSON);
        } else {
            _insertOnce(false);
        }
        reinit();
        // int l = dataBSON.size();
        for (Document document : dataBSON) {
            rList.add(document.get("_id"));
        }
        return rList;
    }

    public Object insertOnce() {
        return _insertOnce(true);
    }

    private Object _insertOnce(boolean rsState) {
        ObjectId oid;
        String rString = "";
        try {
            collection.insertOne(dataBSON.get(0));
            if (rsState) {
                oid = (ObjectId) (dataBSON.get(0).get("_id"));
                if (oid != null) {
                    rString = oid.toString();
                }
            }
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return rString;
    }

    public JSONObject update() {
        JSONObject rs = null;
        Bson updateData;
        Bson filterData = translate2bsonAndRun();
        updateData = document2updateBSON(false);
        try {
            if (filterData != null && updateData != null) {
                if (_atom) {
                    rs = bson2json(Objects.requireNonNull(collection.findOneAndUpdate(filterData, updateData)));
                } else {
                    //filterData = Filters.and( Filters.eq("_id", new ObjectId("58c11cb21a4769cbf5e7eda2") ) );
                    UpdateResult result = collection.updateOne(filterData, updateData);

                    rs = (result.getModifiedCount() > 0) ? new JSONObject() : null;
                }
            }
        } catch (Exception e) {
            //errout();
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return rs;
    }

    public long updateAll() {
        Bson updateDatas;
        UpdateResult result = null;
        Bson filterData = translate2bsonAndRun();
        try {
            if (filterData == null) {
                filterData = new BasicDBObject();
            }
            if (dataBSON.size() > 0) {
                updateDatas = document2updateBSON(true);
                result = collection.updateMany(filterData, Objects.requireNonNull(updateDatas));
            }
        } catch (Exception e) {
            //errout();
            nLogger.logInfo(e);
            result = null;
        } finally {
            reinit();
        }
        return result != null ? result.getModifiedCount() : 0;
    }

    public JSONObject delete() {
        JSONObject rs = null;
        try {
            Bson filterData = translate2bsonAndRun();
            if (filterData != null) {
                if (_atom) {
                    rs = bson2json(Objects.requireNonNull(collection.findOneAndDelete(filterData)));
                } else {
                    DeleteResult result = collection.deleteOne(filterData);
                    rs = (result.getDeletedCount() > 0) ? new JSONObject() : null;
                }
            }
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return rs;
    }

    public long deleteAll() {
        DeleteResult result = null;
        try {
            Bson filterData = translate2bsonAndRun();
            if (filterData == null) {
                filterData = new BasicDBObject();
            }
            result = collection.deleteMany(filterData);
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return result != null ? result.getDeletedCount() : 0;
    }

    public JSONObject inc(String fieldName) {
        return add(fieldName, 1);
    }

    public JSONObject dec(String fieldName) {
        return add(fieldName, -1);
    }

    public JSONObject add(String fieldName, long num) {
        updateBSON.add(Updates.inc(fieldName, num));
        findOne();//open atom mode
        return update();
    }

    public JSONObject find() {
        try {
            return bson2json(_find().first());
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public JSONArray select() {
        Document doc;
        JSONObject json;
        JSONArray rs = new JSONArray();
        //解析内容，执行之
        try {
            FindIterable<Document> fd = _find();
            for (Document document : fd) {
                doc = document;
                json = bson2json(doc);
                if (json != null) {
                    rs.add(json);
                }
            }
        } catch (Exception e) {
            //errout();
            nLogger.logInfo(e);
            rs = null;
        }
        return rs;
    }

    private FindIterable<Document> _find() {
        Bson bson;
        FindIterable<Document> fd = null;
        try {

            Bson filterData = translate2bsonAndRun();
            fd = filterData == null ? collection.find() : collection.find(filterData);

            if (fieldBSON.size() > 0)
                fd = fd.projection(fieldBSON);
            if (sortBSON.size() > 0) {
                bson = Sorts.orderBy(sortBSON);
                fd = fd.sort(bson);
            }
            if (skipNo > 0)
                fd = fd.skip(skipNo);
            if (limitNo > 0)
                fd = fd.limit(limitNo);
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return fd;
    }

    public Mongodb on(String baseField, String forgenField) {
        return this;
    }

    public Mongodb distinct() {
        _distinct = true;
        return this;
    }

    public JSONArray group() {
        return group(null);
    }

    /**
     * @param groupName //groupby fieldName
     * @return
     */
    @SuppressWarnings("unchecked")
    public JSONArray group(String groupName) {
        JSONArray rs = new JSONArray();
        List<Bson> ntemp = new ArrayList<>();
        List<BsonField> groupParamts = new ArrayList<>();
        Bson filterData = translate2bsonAndRun();
        String groupString = groupName == null ? null : "$" + groupName;
        String _valueName = groupbyfield == null || groupbyfield.equals("") ? groupName : groupbyfield;

        if (filterData != null)
            ntemp.add(Aggregates.match(filterData));
        if (fieldBSON.size() > 0)
            ntemp.add(Aggregates.project(fieldBSON));
        if (_count)
            groupParamts.add(Accumulators.sum("count", 1));
        if (_sum)
            groupParamts.add(Accumulators.sum("total", "$" + _valueName));
        if (_max)
            groupParamts.add(Accumulators.max("max", "$" + _valueName));
        if (_min)
            groupParamts.add(Accumulators.min("min", "$" + _valueName));
        if (_avg)
            groupParamts.add(Accumulators.avg("avg", "$" + _valueName));
        ntemp.add(Aggregates.group(groupString, groupParamts));
        if (sortBSON.size() > 0)
            ntemp.add(Aggregates.sort(Sorts.orderBy(sortBSON)));
        if (skipNo > 0)
            ntemp.add(Aggregates.skip(skipNo));
        if (limitNo > 0)
            ntemp.add(Aggregates.limit(limitNo));
        try {
            AggregateIterable<Document> fd = collection.aggregate(ntemp);
            for (Document item : fd) {
                rs.add(bson2json(item));
            }
        } catch (Exception e) {
            //errout();
            nLogger.logInfo(e);
            rs = null;
        } finally {
            reinit();
        }
        return rs;
    }

    /**
     * @param islist 是否是链式，链式不清除条件
     * @return
     */
    public long count(boolean islist) {
        long rl = 0;

        //System.out.println(condString());
        try {
            Bson filterData = translate2bsonAndRun();
            rl = filterData == null ? collection.estimatedDocumentCount() : collection.countDocuments(filterData);
            if (!islist) {
                reinit();
            }
        } catch (Exception e) {
            nLogger.logInfo(e, "Mongodb.count,返回集为空，对象表单未设置");
        }
        return rl;
    }

    public String getConditionString() {
        return conditionJSON.toString();
    }

    @SuppressWarnings("unchecked")
    public JSONArray distinct(String fieldName) {
        JSONArray rTs = new JSONArray();
        Bson filterData = translate2bsonAndRun();
        DistinctIterable<String> fd;
        try {
            fd = filterData != null ? collection.distinct(fieldName, filterData, String.class) : collection.distinct(fieldName, String.class);
            for (String item : fd) {
                rTs.add(item);
            }
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
            rTs = null;
        } finally {
            reinit();
        }
        return rTs;
    }
    //透明分表mongodb不需要

    //！！！权限分到一个新模块里面

    public JSONArray page(int pageidx, int pagemax) {//普通分页
        return skip((pageidx - 1) * pagemax).limit(pagemax).select();
    }

    public long count() {
        return count(false);
    }

    public Mongodb count(String groupbyString) {//某字段分组后数量
        groupbyfield = groupbyString;
        _count = true;
        return this;
    }

    public Mongodb max(String groupbyString) {
        groupbyfield = groupbyString;
        _max = true;
        return this;
    }

    public Mongodb min(String groupbyString) {
        groupbyfield = groupbyString;
        _min = true;
        return this;
    }

    public Mongodb avg(String groupbyString) {
        groupbyfield = groupbyString;
        _avg = true;
        return this;
    }

    public Mongodb sum(String groupbyString) {
        groupbyfield = groupbyString;
        _sum = true;
        return this;
    }

    /**
     * 多线程同步扫描
     *
     * @param func
     * @param max
     * @param synNo
     * @return
     */
    @SuppressWarnings("unchecked")
    public JSONArray scan(Function<JSONArray, JSONArray> func, int max, int synNo) {
        if (func == null) {
            nLogger.logInfo("scan 过滤函数不存在");
        }
        if (max <= 0) {
            nLogger.logInfo("scan 每页最大值不能小于等于0");
            max = 1;
        }
        if (synNo <= 0) {
            nLogger.logInfo("scan 同步执行不能小于等于0");
        }

        long rl = dirty().count();
        int maxCount = (int) rl;
        int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
        ConcurrentHashMap<Integer, JSONArray> tempResult;
        tempResult = new ConcurrentHashMap<>();
        // ExecutorService es = Executors.newVirtualThreadExecutor();
        ExecutorService es = Executors.newCachedThreadPool();
        List<List<Object>> condJSON = getCond();
        String _formName = getfullform();
        try {
            for (int index = 1; index <= pageNO; index++) {
                final int _index = index;
                final int _max = max;
                es.execute(() -> {
                    try {
                        JSONArray jsonArray;
                        Mongodb db = new Mongodb(_configString);
                        db.form(_formName);
                        db.setCond(condJSON);
                        jsonArray = db.page(_index, _max);
                        tempResult.put(_index, Objects.requireNonNull(func).apply(jsonArray));
                    } catch (Exception e) {
                    }
                });
            }
        } finally {
            es.shutdown();
            try {
                es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException e) {
            }
        }
        JSONArray rArray = new JSONArray();
        for (int key : tempResult.keySet()) {
            rArray.addAll(tempResult.get(key));
        }
        return rArray;
    }

    @SuppressWarnings("unchecked")
    public JSONArray scan(Function<JSONArray, JSONArray> func, int max) {
        if (func == null) {
            nLogger.logInfo("scan 过滤函数不存在");
        }
        if (max <= 0) {
            nLogger.logInfo("scan 每页最大值不能小于等于0");
        }
        long rl = dirty().count();
        int maxCount = (int) rl;
        int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
        JSONArray jsonArray, tempResult;
        tempResult = new JSONArray();
        for (int index = 1; index <= pageNO; index++) {
            jsonArray = dirty().page(index, max);
            tempResult.addAll(Objects.requireNonNull(func).apply(jsonArray));
        }
        return tempResult;
    }

    public List<List<Object>> getCond() {
        return conditionJSON;
    }

    public Mongodb setCond(List<List<Object>> conJSON) {
        conditionJSON = conJSON;
        return this;
    }

    public Mongodb groupWhere(JSONArray condArray) {
        return groupCondition(DbFilter.buildDbFilter(condArray).buildEx());
    }

    public Mongodb groupCondition(List<List<Object>> conds) {
        if (conds != null && conds.size() > 0) {
            List<Object> block = new ArrayList<>();
            block.add(conditiobLogicAnd ? "and" : "or");
            block.add(conds);
            conditionJSON.add(block);
        }
        return this;
    }

    //document 2 updateBson
    private Bson document2updateBSON(boolean isMany) {
        List<Document> _dataBSON;
        if (dataBSON.size() + updateBSON.size() == 0) {
            return null;
        }
        if (isMany) {
            _dataBSON = dataBSON;
        } else {
            _dataBSON = new ArrayList<>();
            if (dataBSON.size() > 0) {
                _dataBSON.add(dataBSON.get(0));
            }
        }
        for (Document doc : _dataBSON) {
            doc.remove("_id");
            for (Object item : doc.keySet()) {
                updateBSON.add(Updates.set(item.toString(), doc.get(item)));
            }
        }
        return Updates.combine(updateBSON);
    }

    private BasicDBObject killAnd(BasicDBObject rBSON) {
        BasicDBObject r = new BasicDBObject();
        if (rBSON.size() > 0) {
            for (Object key : rBSON.keySet()) {
                if (key.equals("$and")) {
                    BasicDBList cArray = (BasicDBList) rBSON.get("$and");
                    for (Object _o : cArray) {
                        BasicDBObject o = (BasicDBObject) _o;
                        for (Object _l : o.keySet()) {
                            Object _v = r.get(_l);
                            if (_v == null) {
                                _v = o.get(_l);
                                r.put(_l.toString(), _v);
                            } else {
                                BasicDBList _vl;
                                if (_v instanceof BasicDBObject) {
                                    _vl = new BasicDBList();
                                } else {
                                    _vl = (BasicDBList) _v;
                                }
                                _vl.add(_v);
                            }

                        }
                    }
                }
            }
        }
        return r;
    }

    private BasicDBObject translate2bsonAndRun() {//翻译到BSON并执行
        BasicDBObject rBSON = new BasicDBObject();
        int size = conditionJSON.size();
        if (size > 0) {
            List<Object> tempConds = new ArrayList<>(conditionJSON);
            rBSON = translate2bsonAndRun(tempConds);
            // rBSON = killAnd(rBSON);  // 最外层 $and 转成 obj
        }
        return rBSON;
    }

    // 通KEY合并 r->l
    private BasicDBObject BasicDBObjectAppend(BasicDBObject l, BasicDBObject r) {
        for (Object rk : r.keySet()) {
            BasicDBList lvl;
            if (l.containsKey(rk)) {
                Object lv = l.get(rk);
                if (lv instanceof BasicDBObject) {
                    lvl = new BasicDBList();
                    lvl.add(lv);
                } else {
                    lvl = (BasicDBList) lv;
                }
                Object rv = r.get(rk);
                if (rv instanceof BasicDBObject) {
                    lvl.add(rv);
                } else {
                    BasicDBList rvl = (BasicDBList) rv;
                    lvl.addAll(rvl);
                }
                l.put(rk.toString(), lvl);
            } else {
                l.put(rk.toString(), r.get(rk));
            }
        }
        return l;
    }

    //返回BasicDBList或者BasicDBObject
    private BasicDBObject translate2bsonAndRun(List<Object> conds) {
        BasicDBObject r = new BasicDBObject();
        for (Object item : conds) {
            // r = new BasicDBObject();
            Object idx0 = conds.get(0);
            if (item instanceof ArrayList) {//列表对象是list
                List<Object> info = (List<Object>) item;
                BasicDBObject cond = translate2bsonAndRun(info);
                if (cond.size() > 0) {
                    BasicDBList tempInfoList = (BasicDBList) r.get("$" + info.get(0));
                    if (tempInfoList == null) {
                        tempInfoList = new BasicDBList();
                    }
                    tempInfoList.add(cond);
                    r.put("$" + info.get(0), tempInfoList);
                }
            } else {
                if (conds.size() == 2) {//是条件组
                    BasicDBObject rInfo = new BasicDBObject();
                    if (idx0 instanceof String) {
                        rInfo = translate2bsonAndRun((List<Object>) conds.get(1));
                    }
                    return rInfo;
                }
            }
            if (conds.size() == 4) {//是条件
                if (idx0 instanceof String) {
                    BasicDBObject cond = new BasicDBObject();
                    String logicStr = logic2mongodb((String) conds.get(2));
                    String field = (String) conds.get(1);
                    Object value = conds.get(3);
                    if (logicStr.equalsIgnoreCase("$regex")) {
                        value = "^.*" + value.toString() + ".*$";
                    } else {
                        value = _getID(field, value);
                    }
                    BasicDBObject rInfo = new BasicDBObject();
                    /*  // 简化 eq
                    if( logicStr.equals("$eq") ){
                        rInfo.put(field, value);
                    }
                    else{
                        cond.put(logicStr, value);
                        rInfo.put(field, cond);
                    }
                     */
                    cond.put(logicStr, value);
                    rInfo.put(field, cond);
                    return rInfo;
                }
            }
        }
        return r;
    }
    //bson 2 jsonObject

    //jsonObject string 2 bson object
    private Document json2document(JSONObject _json) {
        return new Document(_json);
    }

    public Mongodb bind(String ownerID) {
        if (!ownerID.equals(ownid)) {
            ownid = ownerID;
            form(formName);
        }
        return this;
    }

    public String getGeneratedKeys() {
        return "_id";
    }

    public String getFormName() {
        return formName;
    }

    public Mongodb dirty() {
        isDirty = true;
        return this;
    }

    public int limit() {
        return limitNo;
    }

    public int pageMax(int max) {
        double c = count(true);
        double d = c / max;
        return (int) Math.ceil(d);
    }

    //创建新表，仅仅和满足接口而已
    public Mongodb newTable() {
        return this;
    }

    public void clear() {
        isDirty = false;
        reinit();
    }

    //创建新临时表，仅仅和满足接口而已
    public Mongodb newTempTable() {
        return this;
    }

    public List<String> getAllTables() {
        List<String> rArray = new ArrayList<>();
        MongoIterable<String> clist = mongoDatabase.listCollectionNames();
        for (String s : clist) {
            rArray.add(s);
        }
        return rArray;
    }

    /**
     *
     */
    public static class mongodbConfig {
        private String database;

        public MongoClientURI json2mongouri(String jsonConfig) {
            MongoClientURI rs;
            String user;
            String password;
            StringBuilder nodeString = new StringBuilder();
            String authString;
            String repsetName;
            String sslString = "";
            int maxPoolSize;

            JSONObject obj = JSONObject.toJSON(jsonConfig);
            user = obj.getString("user");
            password = obj.getString("password");
            database = obj.getString("database");
            repsetName = obj.getString("replicaSet");
            JSONArray nodes = obj.getJsonArray("nodeAddresses");
            maxPoolSize = obj.getInt("maxTotal");
            if (maxPoolSize <= 0) {
                maxPoolSize = 150;
            }
            for (Object node : nodes) {
                nodeString.append(node).append(",");
            }
            authString = "";
            if (!user.equals("") && !password.equals("")) {
                authString = user + ":" + password + "@";
            }
            nodeString = new StringBuilder(nodeString.substring(0, nodeString.length() - 1));
            String url = "mongodb://" + authString + nodeString + "/" + database;
            MongoClientOptions.Builder build = new MongoClientOptions.Builder();
            url += "?maxPoolSize=" + maxPoolSize + "&waitQueueMultiple=5000";
            if (repsetName != null && repsetName.length() > 2) {
                url += "&replicaSet=" + repsetName;
            }

			/*
			new ServerAddress("host1", 27017)
			MongoClientOptions.Builder build = new MongoClientOptions.Builder();
			build.
			build.socketKeepAlive(true);
			*/
            build.sslEnabled(obj.getBoolean("ssl"));

            rs = new MongoClientURI(url, build);

            return rs;
        }
    }

}
