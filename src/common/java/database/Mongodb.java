/**
 * @author Administrator
 */
/**
 * @author Administrator
 *
 */
package common.java.database;

import com.mongodb.MongoClient;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import common.java.nlogger.nlogger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	static {
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
	//private List<Bson> andBSON;
	//private List<Bson> orBSON;
	//private BasicDBList andBSON;
	//private BasicDBList orBSON;
	private List<Bson> sortBSON;
	private List<Document> dataBSON;
	private List<Bson> updateBSON;
	private boolean _count;
	private boolean _max;
	private boolean _min;
	private boolean _sum;
	//private List<JSONObject> tempCondtion;
	//private Bson filterBSON;
	private boolean _avg;
	private boolean _distinct;
	private boolean _atom;
	private String ownid;
	private JSONObject joinJson;
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

	@SuppressWarnings("unchecked")
	public static final JSONObject bson2json(Document _bson) {
		JSONObject obj = null;
		if (_bson != null && _bson.size() > 0) {
			Object objVal;
			obj = new JSONObject();
			for (String bsonobj : _bson.keySet()) {
				objVal = _bson.get(bsonobj);
				if (bsonobj.equals("_id") && objVal instanceof ObjectId) {
					obj.put(bsonobj, ((ObjectId) objVal).toHexString());
				} else {
					obj.put(bsonobj, objVal);
				}
			}
		}
		return obj;
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
						nlogger.logInfo(e);
					}
				}
			}
			mongoDatabase = mongoClient.getDatabase(mongodbConfig.database);
			//dp.end();
		} catch (Exception e) {
			if (mongoClient != null) {
				mongoClient.close();
			}
			nlogger.logInfo(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			Close();
		} catch (Throwable t) {
			throw t;
		} finally {
			super.finalize();
		}
	}

	public void Close() {
		//mongoClient.close();
	}

	public Mongodb data(String jsonString) {
		data(JSONObject.toJSON(jsonString));
		return this;
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

		//andBSON = new BasicDBList();
		//orBSON = new BasicDBList();

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
		joinJson = new JSONObject();


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
		if (field != null && field.equals("_id")) {
			/*
			if( ( value instanceof ObjectId ) ){
				rvalue = value;
			}
			*/
			if (value instanceof String) {
				rvalue = new ObjectId(value.toString());
			}
			if (value instanceof JSONObject) {
				rvalue = new ObjectId(((JSONObject) value).getString("$oid"));
			}
		}
		return rvalue;
	}

	/**判断条件是否为空
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
	/*
	private void fixCondObject(){//当逻辑变化的时候把上一个逻辑最后添加的数据加到当前新逻辑上来
		//boolean fx = !conditiobLogicAnd;//上一个逻辑
		JSONObject lastData;
		if( tempCondtion.size() > 0 ){
			lastData = tempCondtion.get( tempCondtion.size() - 1 );
			lastData.remove("fix", conditiobLogicAnd);
		}
	}
	*/
	//@SuppressWarnings("unchecked")
	/*
	private void addCondition(String field,Object value,String logic){
		//fixCondObject();
		List<Bson> logicBSON;
		//JSONObject tempObject;
		if( value != null && value.toString() != ""){
			value =_getID(field,value);
			
			logicBSON = conditiobLogicAnd ? andBSON : orBSON;

			switch (logic) {
			case "=":
			case "==":
				logicBSON.add(Filters.eq(field, value));
				break;
			case "!=":
				logicBSON.add(Filters.ne(field, value));
				break;
			case ">":
				logicBSON.add(Filters.gt(field, value));
				break;
			case "<":
				logicBSON.add(Filters.lt(field, value));
				break;
			case ">=":
				logicBSON.add(Filters.gte(field, value));
				break;
			case "<=":
				logicBSON.add(Filters.lte(field, value));
				break;
			case "like":
				logicBSON.add(Filters.regex(field, (Pattern)value));
				break;
			default:
				logicBSON.add(Filters.eq(field, value));
				break;
			}
			//near,in,nin,nor,not,text
		}
	}
	*/

	/**条件组 field,logic,value
	 * @param condArray
	 * @return
	 */
	public Mongodb where(JSONArray condArray) {
		JSONObject tmpJSON;
		String field, logic;
		Object value;
		if (condArray == null) {
			return null;
		}

		if (condArray instanceof JSONArray && condArray.size() > 0) {
			for (Object jObject : condArray) {
				field = null;
				logic = null;
				value = null;
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
				if (logic != null && field != null) {
					addCondition(field, value, logic);
				} else {
					nlogger.errorInfo(condArray.toJSONString() + " ->输入的 条件对象无效");
				}
			}
			return this;
		}
		return null;

	}

	private String logic2mongodb(String logic) {
		String logicStr;
		switch (logic) {
			case "=":
			case "==":
				logicStr = "$eq";
				break;
			case "!=":
				logicStr = "$ne";
				break;
			case ">":
				logicStr = "$gt";
				break;
			case "<":
				logicStr = "$lt";
				break;
			case ">=":
				logicStr = "$gte";
				break;
			case "<=":
				logicStr = "$lte";
				break;
			case "like":
				logicStr = "$regex";
				break;
			default:
				logicStr = logic;
				break;
		}
		return logicStr;
	}

	private void addCondition(String field, Object value, String logic) {
		//fixCondObject();
		BasicDBList logicBSON;
		//JSONObject tempObject;
		String logicStr;
		if (value != null && value.toString() != "") {
			value = _getID(field, value);
			logicStr = logic2mongodb(logic);
			if (logicStr.equals("$regex")) {
				value = "^.*" + value.toString() + ".*$";
			}
			List<Object> bit = new ArrayList<>();

			bit.add(conditiobLogicAnd ? "and" : "or");
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

	public Mongodb data(JSONObject doc) {
		dataBSON.clear();
		dataBSON.add(json2document(doc));
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
		return fieldOperate(fieldList, 0);
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
		for (int i = 0; i < fieldList.length; i++) {
			fieldBSON.put(fieldList[i], visable);
		}
		return this;
	}

	private void checkFieldMix(int visable) {
		Object temp;
		boolean needClear = false;
		for (Object obj : fieldBSON.keySet()) {
			temp = fieldBSON.get(obj);
			if (temp != null && obj.toString() != "_id") {
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
		return (ownid == null || (ownid != null && ownid.equals(""))) ? formName : formName + "_" + ownid;
	}

	public String getform() {
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

	@SuppressWarnings("unchecked")
	public Mongodb join(String forgenForm) {
		joinJson.put("form", forgenForm);
		return this;
	}

	public List<Document> clearDocument(List<Document> imp) {
		for (Document doc : imp) {
			doc.containsKey("_id");
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
		int l = dataBSON.size();
		for (int i = 0; i < l; i++) {
			rList.add(dataBSON.get(i).get("_id"));
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
			nlogger.logInfo(e);
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
					rs = bson2json(collection.findOneAndUpdate(filterData, updateData));
				} else {
					//filterData = Filters.and( Filters.eq("_id", new ObjectId("58c11cb21a4769cbf5e7eda2") ) );
					UpdateResult result = collection.updateOne(filterData, updateData);

					rs = (result.getModifiedCount() > 0) ? new JSONObject() : null;
				}
			}
		} catch (Exception e) {
			//errout();
			nlogger.logInfo(e);
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
				result = collection.updateMany(filterData, updateDatas);
			}
		} catch (Exception e) {
			//errout();
			nlogger.logInfo(e);
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
					rs = bson2json(collection.findOneAndDelete(filterData));
				} else {
					DeleteResult result = collection.deleteOne(filterData);
					rs = (result.getDeletedCount() > 0) ? new JSONObject() : null;
				}
			}
		} catch (Exception e) {
			// errout();
			nlogger.logInfo(e);
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
			nlogger.logInfo(e);
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
		JSONObject rs = null;
		try {
			rs = bson2json(_find().first());
		} catch (Exception e) {
			// errout();
			nlogger.logInfo(e);
			rs = null;
		}
		return rs;
	}

	@SuppressWarnings("unchecked")
	public JSONArray select() {
		Document doc = null;
		JSONObject json = null;
		JSONArray rs = new JSONArray();
		//解析内容，执行之
		try {
			FindIterable<Document> fd = _find();
			MongoCursor<Document> it = fd.iterator();
			while (it.hasNext()) {
				doc = it.next();
				json = bson2json(doc);
				if (json != null) {
					rs.add(json);
				}
			}
		} catch (Exception e) {
			//errout();
			nlogger.logInfo(e);
			rs = null;
		}
		return rs;
	}

	public String condString() {
		return conditionJSON.toString();
	}

	private FindIterable<Document> _find() {
		Bson bson = null;
		FindIterable<Document> fd = null;
		try {

			Bson filterData = translate2bsonAndRun();
			fd = filterData == null ? collection.find() : collection.find(filterData);

			if (fieldBSON.size() > 0)
				fd = fd.projection(fieldBSON);
			if (sortBSON.size() > 0) {
				bson = Sorts.orderBy(sortBSON);
				if (bson != null) {
					fd = fd.sort(bson);
				} else {
					System.err.println("排序字段异常:" + sortBSON.toString());
				}
			}
			if (skipNo > 0)
				fd = fd.skip(skipNo);
			if (limitNo > 0)
				fd = fd.limit(limitNo);
		} catch (Exception e) {
			// errout();
			nlogger.logInfo(e);
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
		String _valueName = groupbyfield == null || groupbyfield == "" ? groupName : groupbyfield;

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
			nlogger.logInfo(e);
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
			rl = filterData == null ? collection.countDocuments() : collection.countDocuments(filterData);
			if (!islist) {
				reinit();
			}
		} catch (Exception e) {
			nlogger.logInfo(e, "Mongodb.count,返回集为空，对象表单未设置");
		}
		return rl;
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
			nlogger.logInfo(e);
			rTs = null;
		} finally {
			reinit();
		}
		return rTs;
	}
	/*
	public JSONArray page(int pageidx,int pagemax,int lastid,String fastfield){//高速分页
		if( fastfield == null || fastfield == "")
			fastfield = "_id";
		return ( pageidx == 1 ) ? page(pageidx, pagemax) : gt(fastfield, lastid).limit(pagemax).select();
	}
	*/
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

	/**多线程同步扫描
	 * @param func
	 * @param max
	 * @param synNo
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONArray scan(Function<JSONArray, JSONArray> func, int max, int synNo) {
		if (func == null) {
			nlogger.logInfo("scan 过滤函数不存在");
		}
		if (max <= 0) {
			nlogger.logInfo("scan 每页最大值不能小于等于0");
			max = 1;
		}
		if (synNo <= 0) {
			nlogger.logInfo("scan 同步执行不能小于等于0");
			synNo = 1;
		}

		long rl = dirty().count();
		int maxCount = (int) rl;
		int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
		ConcurrentHashMap<Integer, JSONArray> tempResult;
		tempResult = new ConcurrentHashMap<>();
		ExecutorService es = Executors.newVirtualThreadExecutor();
		JSONObject condJSON = getCond();
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
						tempResult.put(_index, func.apply(jsonArray));
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
			nlogger.logInfo("scan 过滤函数不存在");
		}
		if (max <= 0) {
			nlogger.logInfo("scan 每页最大值不能小于等于0");
		}
		long rl = dirty().count();
		int maxCount = (int) rl;
		int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
		JSONArray jsonArray, tempResult;
		tempResult = new JSONArray();
		for (int index = 1; index <= pageNO; index++) {
			jsonArray = dirty().page(index, max);
			tempResult.addAll(func.apply(jsonArray));
		}
		return tempResult;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getCond() {
		JSONObject conJSON = new JSONObject();
		conJSON.put("cond", conditionJSON);
		return conJSON;
	}

	public Mongodb setCond(JSONObject conJSON) {
		conditionJSON = (List<List<Object>>) conJSON.get("cond");
		return this;
	}

	public Mongodb groupCondition(List<List<Object>> conds) {
		if (conds != null && conds.size() > 0) {
			List<Object> block = new ArrayList<>();
			block.add(conditiobLogicAnd ? "and" : "or");
            /*
            for(List<Object> item : conds){
            	item.set(2,logic2mongodb((String)item.get(2)));
			}
			*/
			block.add(conds);
			conditionJSON.add(block);
		}
		return this;
	}

	/*
    public Mongodb clearResult(){
        _clearRes = true;
        return this;
    }
    //老的
    private JSONObject bson2json(Document _bson){
        JSONObject obj = null;
        if( _bson  != null){
            obj = _clearRes ? bson2json_helper(_bson) : JSONObject.toJSON(_bson.toJson());
            //默认非干净返回值
            _clearRes = false;
        }
        return obj;
    }
    //新的不包含多余对象的结构
    @SuppressWarnings("unchecked")
    public static final JSONObject bson2json_helper(Document _bson){
        JSONObject obj = new JSONObject();
        for( Object bsonobj : _bson.keySet() ){
            if( bsonobj.toString().equals("_id") ){
                obj.put(bsonobj, _bson.getObjectId(bsonobj).toHexString());
            }
            else{
                obj.put(bsonobj, _bson.get(bsonobj));
            }
        }
        return obj;
    }
    */
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
			for (Object item : doc.keySet()) {
				if (!item.toString().equals("_id")) {
					updateBSON.add(Updates.set(item.toString(), doc.get(item)));
				}
			}
		}
		return Updates.combine(updateBSON);
	}

	private BasicDBObject translate2bsonAndRun() {//翻译到BSON并执行
		BasicDBObject rBSON = new BasicDBObject();
		int size = conditionJSON.size();
		if (size > 0) {
			List<Object> tempConds = new ArrayList<>();
			tempConds.addAll(conditionJSON);
			rBSON = translate2bsonAndRun(tempConds);
		}
		// System.out.println(rBSON.toJson());
		return rBSON;
	}

	//返回BasicDBList或者BasicDBObject
	private BasicDBObject translate2bsonAndRun(List<Object> conds) {
		BasicDBObject r = new BasicDBObject();
		BasicDBList infoList = new BasicDBList();
		for (Object item : conds) {
			r = new BasicDBObject();
			Object idx0 = conds.get(0);
			if (item instanceof ArrayList) {//列表对象是list
				List<Object> info = (List<Object>) item;
				BasicDBObject cond = translate2bsonAndRun(info);
				infoList.add(cond);
				if (infoList.size() > 0) {
					r.put("$" + info.get(0), infoList);
					infoList = new BasicDBList();
					infoList.add(r);
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
					if (logicStr.equals("$regex")) {
						value = "^.*" + value.toString() + ".*$";
					} else {
						value = _getID(field, value);
					}
					cond.put(logicStr, value);
					BasicDBObject rInfo = new BasicDBObject();
					rInfo.put(field, cond);
					return rInfo;
				}
			}
		}
		return r;
	}
	//bson 2 jsonObject

	//jsonObject string 2 bson object
	/*
	private BasicDBObject json2bson(String _json) throws ParseException{
		JSONParser jParser = new JSONParser();
		return json2bson((JSONObject)jParser.parse(_json));
	}
	private BasicDBObject json2bson(JSONObject _json){
		BasicDBObject tempBSON = new BasicDBObject();
		for(Object item : _json.keySet() ){
			tempBSON.put(item.toString(),  _json.get(item) );
		}
		return tempBSON;
	}
	private Document json2document(String _json) throws ParseException{
		JSONParser jParser = new JSONParser();
		return json2document((JSONObject)jParser.parse(_json));
	}
	*/
	private Document json2document(JSONObject _json) {
		Document tempDoc = new Document();
		for (Object item : _json.keySet()) {
			tempDoc.append(item.toString(), _json.get(item));
		}
		return tempDoc;
	}

	public Mongodb bind(String ownerID) {
		if (!ownerID.equals(ownid)) {
			ownid = ownerID == null ? "" : ownerID;
			form(formName);
		}
		return this;
	}

	public String getGeneratedKeys() {
		return "_id";
	}

	public String getformName() {
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
		private JSONObject obj;
		private String database;

		public MongoClientURI json2mongouri(String jsonConfig) {
			MongoClientURI rs;
			String user = "";
			String password = "";
			String nodeString = "";
			String authString = "";
			String repsetName = "";
			String sslString = "";
			int maxPoolSize;

			obj = JSONObject.toJSON(jsonConfig);
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
				nodeString += node + ",";
			}
			authString = "";
			if (user != "" && password != "") {
				authString = user + ":" + password + "@";
			}
			nodeString = nodeString.substring(0, nodeString.length() - 1);
			//System.out.print("Mongodb://" + authString + nodeString + "/" + database + "?replicaSet=" + database);
			//rs = new MongoClientURI("Mongodb://" + authString + nodeString + "/" + database + "?replicaSet=" + database);
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
			//rs = new MongoClientURI(url);

			//System.out.println( rs.getOptions().isSocketKeepAlive()  );
			//System.out.println( rs.getOptions().getRequiredReplicaSetName() );
			return rs;
		}
	}
	/*
	private void errout(){
		System.err.println("跳过:" + String.valueOf(skipNo) );
		System.err.println("最大:" + String.valueOf(limitNo) );
		System.err.println("字段:" + fieldBSON != null ? fieldBSON.toJson() :"" );
		System.err.println("排序:" + sortBSON != null ? basicDBObjectHelper.basicDB2String(sortBSON) : "" );
		System.err.println("表单:" + formName);
		System.err.println("条件：" + conditionJSON.toString());
		System.err.println("=================================" );
	}
	*/
}