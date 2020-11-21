package common.java.serviceHelper;

/**
 * 只能使用 LocalDB
 */

import common.java.JGrapeSystem.SystemDefined;
import common.java.database.DbFilter;
import common.java.database.DbLayerHelper;
import common.java.httpServer.HttpContext;
import common.java.interfaceType.ApiType;
import common.java.rpc.rMsg;
import common.java.string.StringHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.function.Function;

public class fastDBService {
	private DbLayerHelper db;
	private String mainKey;
	private String pk;
	private Function<JSONArray, JSONArray> _onSelected = null;
	private Function<String, String> _onFindFunc = null;
	private Function<JSONObject, JSONObject> _onFindedFunc = null;
	private Function<onFind, onFind> _onFindsFunc = null;
	private Function<JSONArray, JSONArray> _onFindedsFunc = null;
	private Function<onPage, onPage> _onPageFunc = null;
	private Function<JSONArray, JSONArray> _onPagedFunc = null;
	private Function<JSONObject, JSONObject> _onInsertFunc = null;
	private Function<onUpdate, onUpdate> _onUpdateFunc = null;
	/**
	 * 删除前做参数过滤
	 */
	private Function<String[], String[]> _onDeleteFunc = null;

	public fastDBService() {
		init();
	}

	public fastDBService(String tableName) {
		init(tableName);
	}

	private void init(String tableName) {
		mainKey = null;
		db = new DbLayerHelper(SystemDefined.commonConfigUnit.LocalDB, tableName);
		pk = db.getpk();
	}

	private void init() {
		mainKey = null;
		db = new DbLayerHelper(SystemDefined.commonConfigUnit.LocalDB);
		pk = db.getpk();
	}

	public fastDBService form(String tableName) {
		db.form(tableName);
		return this;
	}

	public void setMainKey(String key) {
		mainKey = key;
	}

	private String getKey() {
		return mainKey == null ? pk : mainKey;
	}

	public String testCond() {
		return db.getCond().toJSONString();
	}

	public DbLayerHelper _getDB() {
		return db;
	}

	public String getPk() {
		return pk;
	}

	public boolean where(String cond) {
		boolean rs = false;
		if (cond != null && cond.length() > 2) {
			JSONArray condArray = JSONArray.toJSONArray(cond);
			if (condArray != null && condArray.size() > 0) {
				db.where(JSONArray.toJSONArray(cond));
				rs = true;
			}
		}
		return rs;
	}

	public String errorMsg() {
		return errorMsg("非法参数!");
	}

	public String errorMsg(String msg) {
		return rMsg.netMSG(false, msg);
	}

	//类提供的服务接口不允许相互之间调用
	public fastDBService fliterPlv() {
		int appid = HttpContext.current().appid();
		if (appid > 0) {//非系统级别会话
			DbFilter _f = DbFilter.buildDbFilter();
			db.and().groupCondition(_f.eq("appid", appid).buildex());
		}
		return this;
	}

	public fastDBService onSelected(Function<JSONArray, JSONArray> func) {
		_onSelected = func;
		return this;
	}

	public String select() {
		JSONArray result = db.select();
		if (_onSelected != null) {
			result = _onSelected.apply(result);
		}
		return rMsg.netMSG(true, result);
	}

	public fastDBService onFind(Function<String, String> func) {
		_onFindFunc = func;
		return this;
	}

	public String page(int idx, int max) {
		return pageby(idx, max, null);
	}

	public fastDBService onFinded(Function<JSONObject, JSONObject> func) {
		_onFindedFunc = func;
		return this;
	}


	public String find() {
		return rMsg.netMSG(0, db.find());
	}

	public String find(String val) {//find
		return find(getKey(), val);
	}

	public String find(String key, String val) {//find
		return rMsg.netMSG(0, _find(key, val));
	}

	@ApiType(ApiType.type.CloseApi)
	public JSONObject _find(String id) {//find
		return _find(getKey(), id);
	}

	@ApiType(ApiType.type.CloseApi)
	public JSONObject _find(String field, String id) {//find
		JSONObject rj = null;
		if (db != null) {
			if (_onFindFunc != null) {
				id = _onFindFunc.apply(id);
			}
			rj = db.eq(field, id).find();
			if (rj != null && _onFindedFunc != null) {
				rj = _onFindedFunc.apply(rj);
			}
		}
		return rj;
	}

	public fastDBService onFinds(Function<onFind, onFind> func) {
		_onFindsFunc = func;
		return this;
	}

	public fastDBService onFindeds(Function<JSONArray, JSONArray> func) {
		_onFindedsFunc = func;
		return this;
	}

	@ApiType(ApiType.type.CloseApi)
	public JSONArray finds(String ids, String[] fields) {
		JSONArray rJson = null;
		if (db != null) {
			String[] idList = ids.split(",");
			DbFilter dbf = DbFilter.buildDbFilter();
			if (idList.length > 0) {
				if (_onFindsFunc != null) {
					onFind of = new onFind(idList, fields);
					of = _onFindsFunc.apply(of);
					idList = of.getIds();
					fields = of.getField();
				}
				for (int i = 0; i < idList.length; i++) {
					dbf.or().eq(getKey(), idList[i]);
				}
				if (fields != null && fields.length > 0) {
					db.field(fields);
				}
				rJson = db.groupCondition(dbf.buildex()).desc(db.getGeneratedKeys()).select();
				if (_onFindedsFunc != null && rJson != null) {
					rJson = _onFindedsFunc.apply(rJson);
				}
			}
		}
		return rJson;
	}

	public String insert(String data) {
		JSONObject json = JSONObject.toJSON(data);
		return insert(json);
	}

	@ApiType(ApiType.type.PrivateApi)
	public String insert(JSONObject json) {
		JSONArray array = null;
		if (json != null) {
			array = new JSONArray();
			array.add(json);
		}
		return _insert(array);
	}

	public fastDBService onPage(Function<onPage, onPage> func) {
		_onPageFunc = func;
		return this;
	}

	public fastDBService onPaged(Function<JSONArray, JSONArray> func) {
		_onPagedFunc = func;
		return this;
	}

	public String pageby(int idx, int max, String conds) {
		JSONArray rj = null;
		long count = 0;
		if (db != null) {
			JSONArray array = JSONArray.toJSONArray(conds);
			if (_onPageFunc != null) {
				onPage op = new onPage(idx, max, array);
				op = _onPageFunc.apply(op);
				idx = op.idx();
				max = op.max();
				array = op.conds();
			}
			if (array != null) {
				db.where(array);
			}
			rj = db.dirty().desc(db.getGeneratedKeys()).page(idx, max);
			if (_onPagedFunc != null && rj != null) {
				rj = _onPagedFunc.apply(rj);
			}
			count = db.pageMax(max);
		}
		return rMsg.netPAGE(idx, max, count, rj);
	}

	public String insertAll(String data) {
		JSONArray array = JSONArray.toJSONArray(data);
		return _insert(array);
	}

	public fastDBService onInsert(Function<JSONObject, JSONObject> func) {
		_onInsertFunc = func;
		return this;
	}

	private String _insert(JSONArray array) {
		List<Object> rj = null;
		if (array != null && array.size() > 0) {
			if (db != null) {
				int l = array.size();
				for (int i = 0; i < l; i++) {
					JSONObject json = (JSONObject) array.get(i);
					if (_onInsertFunc != null) {
						json = _onInsertFunc.apply(json);
					}
					db.data(json);
				}
				rj = db.insert();
			}
		}
		return rMsg.netState(rj != null && rj.size() > 0);
	}

	public fastDBService onUpdate(Function<onUpdate, onUpdate> func) {
		_onUpdateFunc = func;
		return this;
	}

	public String update(JSONObject info) {
		return update(null, info);
	}

	public String update(String ids, String _data) {
		return update(ids, JSONObject.toJSON(_data));
	}

	public String update(String ids, JSONObject data) {
		long rj = 0;
		//JSONObject data = JSONObject.toJSON(_data);
		if (data != null) {
			if (db != null) {
				DbFilter dbf = DbFilter.buildDbFilter();
				if (!StringHelper.invaildString(ids)) {
					String[] idList = ids.split(",");
					if (_onUpdateFunc != null) {
						onUpdate ou = new onUpdate(idList, data);
						ou = _onUpdateFunc.apply(ou);
						idList = ou.ids();
						data = ou.info();
					}
					if (idList.length > 0) {
						for (int i = 0; i < idList.length; i++) {
							dbf.or().eq(getKey(), idList[i]);
						}
						db.groupCondition(dbf.buildex());
					}
				}
				// 数据库对象条件不为空才会更新
				if (db.getCond().size() > 0) {
					rj = db.data(data).updateAll();
				}
			}
		}
		return rMsg.netState(rj > 0);
	}

	public fastDBService onDelete(Function<String[], String[]> func) {
		_onDeleteFunc = func;
		return this;
	}

	public String delete() {
		return delete(null);
	}

	public String delete(String ids) {
		long rj = 0;
		DbFilter dbf = DbFilter.buildDbFilter();
		if (!StringHelper.invaildString(ids)) {
			String[] idList = ids.split(",");
			if (idList.length > 0) {
				if (_onDeleteFunc != null) {
					idList = _onDeleteFunc.apply(idList);
				}
				for (int i = 0; i < idList.length; i++) {
					dbf.or().eq(getKey(), idList[i]);
				}
				db.groupCondition(dbf.buildex());
			}
		}
		// 条件不能为空
		if (db.getCond().size() > 0) {
			rj = db.deleteAll();
		}
		return rMsg.netState(rj > 0);
	}
}
