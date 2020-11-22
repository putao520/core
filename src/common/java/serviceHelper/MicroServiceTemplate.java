package common.java.serviceHelper;

import common.java.apps.MModelRuleNode;
import common.java.apps.MicroServiceContext;
import common.java.database.DbFilter;
import common.java.encrypt.GscJson;
import common.java.interfaceModel.GrapeTreeDbLayerModel;
import common.java.rpc.rMsg;
import common.java.string.StringHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class MicroServiceTemplate implements MicroServiceTemplateInterface {
    private final GrapeTreeDbLayerModel db;
    private final String modelName;

    public MicroServiceTemplate(String ModelName) {
        this.modelName = ModelName;
        db = GrapeTreeDbLayerModel.getInstance(ModelName);
    }

    protected GrapeTreeDbLayerModel getDB() {
        db.clear();
        return db;
    }

    public MicroServiceTemplate addInputFilter(String fieldName, Function<Object, Object> func) {
        this.db.addFieldInPipe(fieldName, func);
        return this;
    }

    public MicroServiceTemplate addOutputFilter(String fieldName, Function<Object, Object> func) {
        this.db.addFieldOutPipe(fieldName, func);
        return this;
    }


    protected JSONArray joinOn(String localKey, JSONArray localArray, String foreignKey, Function<String, JSONArray> func) {
        return joinOn(localKey, localArray, foreignKey, func, null);
    }

    protected JSONArray joinOn(String localKey, JSONArray localArray, String foreignKey, Function<String, JSONArray> func, String prefix) {
        return joinOn(localKey, localArray, foreignKey, func, prefix, false);
    }

    protected JSONArray joinOn(String localKey, JSONArray localArray, String foreignKey, Function<String, JSONArray> func, String prefix, boolean save_null_item) {
        if (JSONArray.isInvaild(localArray)) {
            return localArray;
        }
        List<String> ids = new ArrayList<>();
        // 构造模板ID条件组
        for (Object obj : localArray) {
            JSONObject _obj = (JSONObject) obj;
            if (JSONObject.isInvaild(_obj)) {
                continue;
            }
            String _id = _obj.getString(localKey);
            if (StringHelper.invaildString(_id)) {
                continue;
            }
            ids.add(_id);
        }
        if (ids.size() == 0) {
            return localArray;
        }

        String _ids = StringHelper.join(ids);
        JSONArray foreignArray = func.apply(_ids);
        if (prefix != null) {
            prefix = prefix + "#";
        }
        localArray.joinOn(localKey, foreignKey, foreignArray, prefix, save_null_item);
        // 设置返回数据
        return localArray;
    }

    protected JSONObject joinOn(String localKey, JSONObject localObject, String foreignKey, Function<String, JSONArray> func) {
        return joinOn(localKey, localObject, foreignKey, func, null);
    }

    protected JSONObject joinOn(String localKey, JSONObject localObject, String foreignKey, Function<String, JSONArray> func, String prefix) {
        JSONArray newArray = joinOn(localKey, JSONArray.addx(localObject), foreignKey, func, prefix);
        return JSONArray.isInvaild(newArray) ? localObject : (JSONObject) newArray.get(0);
    }


    @Override
    public String insert(String fastJson) {
        Object obj = null;
        JSONObject newData = GscJson.decode(fastJson);
        if (!JSONObject.isInvaild(newData)) {
            obj = db.data(newData).insertEx();
        }
        return rMsg.netMSG(obj != null, obj);
    }

    @Override
    public String delete(String uids) {
        return _delete(uids, null);
    }

    @Override
    public String deleteEx(String cond) {
        return _delete(null, JSONArray.toJSONArray(cond));
    }

    private String _delete(String ids, JSONArray cond) {
        int r = 0;
        _ids(db.getPk(), ids);
        _condition(cond);
        if (!db.nullCondition()) {
            r = (int) db.deleteAll();
        }
        /*
        else{
            r = db.delete_() ? 1 : 0;
        }
         */
        return rMsg.netMSG(true, r);
    }

    @Override
    public String update(String uids, String base64Json) {
        return _update(uids, GscJson.decode(base64Json), null);
    }

    @Override
    public String updateEx(String base64Json, String cond) {
        return _update(null, GscJson.decode(base64Json), JSONArray.toJSONArray(cond));
    }

    private String _update(String ids, JSONObject info, JSONArray cond) {
        int r = 0;
        if (!JSONObject.isInvaild(info)) {
            _ids(db.getPk(), ids);
            _condition(cond);
            if (!db.nullCondition()) {
                r = (int) db.data(info).updateAll();
            }
        }
        return rMsg.netMSG(true, r);
    }

    @Override
    public String page(int idx, int max) {
        return pageEx(idx, max, null);
    }

    @Override
    public String pageEx(int idx, int max, String cond) {
        _condition(JSONArray.toJSONArray(cond));
        return rMsg.netPAGE(idx, max, db.dirty().count(), db.page(idx, max));
    }

    @Override
    public String select() {
        return selectEx(null);
    }

    @Override
    public String selectEx(String cond) {
        _condition(JSONArray.toJSONArray(cond));
        return rMsg.netMSG(true, db.select());
    }

    @Override
    public String find(String field, String val) {
        int idNo = _ids(field, val);
        Object r = idNo <= 1 ? db.find() : db.select();
        return rMsg.netMSG(r != null, r);
    }

    @Override
    public String findEx(String cond) {
        _condition(JSONArray.toJSONArray(cond));
        return rMsg.netMSG(true, db.find());
    }

    /**
     * @apiNote 获得tree-json结构的全表数据,获得行政机构json树
     */
    @Override
    public String tree(String cond) {
        String rs;
        JSONArray condArray = JSONArray.toJSONArray(cond);
        GrapeTreeDbLayerModel db = getDB();
        _condition(condArray);
        long n = db.dirty().count();
        if (n != 1) {
            rs = rMsg.netMSG(false, "错误条件,必须仅有一条数据满足条件方可生成树JSON");
        } else {
            rs = rMsg.netMSG(true, db.getAllChildren());
        }
        return rs;
    }

    /**
     * @apiNote 微服务标准模板类独有API, 提供当前模型规则描述JSON给前端
     */
    public String getSafeDataModel() {
        HashMap<String, MModelRuleNode> mms = MicroServiceContext.current().model(this.modelName).rules();
        JSONObject desc = new JSONObject();
        for (String key : mms.keySet()) {
            MModelRuleNode mmrn = mms.get(key);
            if (mmrn.type() != MModelRuleNode.FieldType.maskField) {
                desc.put(key, mmrn.node());
            }
        }
        return rMsg.netMSG(true, desc);
    }


    private int _ids(String fieldName, String ids) {
        DbFilter dbf = DbFilter.buildDbFilter();
        String[] _ids = StringHelper.invaildString(ids) ? null : ids.split(",");
        if (_ids != null) {
            if (_ids.length > 1) {
                for (int i = 0; i < _ids.length; i++) {
                    dbf.or().eq(fieldName, _ids[i]);
                }
                if (!dbf.nullCondition()) {
                    db.and().groupCondition(dbf.buildex());
                }
            } else if (_ids.length == 1) {
                db.and().eq(fieldName, _ids[0]);
            }
        }
        return _ids.length;
    }

    private void _condition(JSONArray cond) {
        if (!JSONArray.isInvaild(cond)) {
            db.and().where(cond);
        }
    }

    public MicroServiceTemplate outPipe(Function<JSONArray, JSONArray> func) {
        db.outPipe(func);
        return this;
    }
}
