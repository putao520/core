package common.java.InterfaceModel;

import common.java.Apps.AppContext;
import common.java.Authority.PermissionsPowerDef;
import common.java.Database.DbLayer;
import common.java.HttpServer.HttpContext;
import common.java.InterfaceModel.Type.Aggregation;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;


public class GrapeTreeDbLayerModel extends GrapeDbLayerModel {
    private final List<Function<JSONArray, JSONArray>> pipeJSONArray_Out;
    private final ForkJoinPool pStreamExec = new ForkJoinPool(10);
    private List<String> fields;
    private List<String> maskfields;
    private boolean realMode = false;
    private Aggregation aggregationJSONArray_Out;

    private GrapeTreeDbLayerModel() {
        super();
        pipeJSONArray_Out = new ArrayList<>();
        init();
    }

    public DbLayer getPureDB() {
        return super.getPureDB();
    }

    private GrapeTreeDbLayerModel(String modelName) {
        super(modelName);
        pipeJSONArray_Out = new ArrayList<>();
        init();
    }

    public static GrapeTreeDbLayerModel getInstance() {
        return new GrapeTreeDbLayerModel();
    }

    public static GrapeTreeDbLayerModel getInstance(String modelName) {
        return new GrapeTreeDbLayerModel(modelName);
    }

    public GrapeDbLayerModel fieldOutPipe(String fieldName, Function<Object, Object> func) {
        super.addFieldOutPipe(fieldName, func);
        return this;
    }

    public GrapeDbLayerModel fieldInPipe(String fieldName, Function<Object, Object> func) {
        super.addFieldInPipe(fieldName, func);
        return this;
    }

    // 数据管道
    public GrapeTreeDbLayerModel outPipe(Function<JSONArray, JSONArray> func) {
        pipeJSONArray_Out.add(func);
        return this;
    }

    // 数据聚合
    public void outAggregation(Aggregation func) {
        aggregationJSONArray_Out = func;
    }


    // map-reduce执行
    private JSONArray runOutPipe(JSONArray input) {
        JSONArray r = input;
        if (input != null) {
            if (aggregationJSONArray_Out != null) {
                Vector<JSONArray> resultArray = new Vector<>();
                // 并发执行piper
                JSONArray finalR = r;
                HttpContext hCtx = HttpContext.current();
                pipeJSONArray_Out.parallelStream().forEach(func -> {
                    AppContext.virtualAppContext(hCtx.appid(), hCtx.serviceName());
                    resultArray.add(func.apply(finalR));
                });
                // 线性聚合data
                for (JSONArray array : resultArray) {
                    r = aggregationJSONArray_Out.run(r, array);
                }
            } else {
                // 顺序执行
                for (Function<JSONArray, JSONArray> func : pipeJSONArray_Out) {
                    r = func.apply(r);
                }
            }
        }
        return r;
    }


    private void init() {
        aggregationJSONArray_Out = null;
        fields = null;
        maskfields = null;
    }

    /**
     * 真实模式，数据操作时不考虑特殊字段
     */
    public GrapeTreeDbLayerModel realMode() {
        realMode = true;
        return this;
    }

    /**
     *
     */
    public GrapeTreeDbLayerModel niceMode() {
        realMode = false;
        return this;
    }


    public GrapeTreeDbLayerModel bind() {
        super.bind();
        return this;
    }


    public GrapeTreeDbLayerModel dirty() {
        super.dirty();
        return this;
    }


    public int pageMax(int max) {
        return super.pageMax(max);
    }


    public GrapeTreeDbLayerModel handle() {
        return this;
    }


    public GrapeTreeDbLayerModel field(String fieldString) {
        if (fieldString != null) {
            String[] s = fieldString.split(",");
            field(s);
        }
        return this;
    }


    public GrapeTreeDbLayerModel field(String[] fields) {
        this.fields = null;
        if (fields != null && fields.length > 0) {
            this.fields = Arrays.asList(fields);
        }
        super.field(fields);
        maskfields = null;
        return this;
    }


    public GrapeTreeDbLayerModel mask(String fieldString) {
        if (fieldString != null) {
            String[] s = fieldString.split(",");
            mask(s);
        }
        fields = null;
        return this;
    }


    public GrapeTreeDbLayerModel mask(String[] fields) {
        if (fields != null && fields.length > 0) {
            maskfields = Arrays.asList(fields);
            super.mask(fields);
        }
        this.fields = null;
        return this;
    }


    public GrapeTreeDbLayerModel max(String groupbyString) {
        super.max(groupbyString);
        return this;
    }


    public GrapeTreeDbLayerModel min(String groupbyString) {
        super.min(groupbyString);
        return this;
    }


    public GrapeTreeDbLayerModel avg(String groupbyString) {
        super.avg(groupbyString);
        return this;
    }


    public GrapeTreeDbLayerModel data(JSONObject obj) {
        super.data(obj);
        return this;
    }


    public GrapeTreeDbLayerModel data(String str) {
        super.data(str);
        return this;
    }

    //删除数据（实际标注数据删除状态为1）
    public boolean deleteEx() {
        return data(new JSONObject(PermissionsPowerDef.deleteField, 1)).updateEx();
    }

    /**
     * 直接删除
     */
    public boolean delete_() {
        return super.deleteEx();
    }

    public long deleteAllEx() {
        return data(new JSONObject(PermissionsPowerDef.deleteField, 1)).updateAll();
    }

    /**
     * 直接删除全部
     */
    public long deleteAll_() {
        return super.deleteAll();
    }

    //显示数据
    public boolean show() {
        return data(new JSONObject(PermissionsPowerDef.visableField, 0)).updateEx();
    }

    //隐藏数据
    public boolean hide() {
        return data(new JSONObject(PermissionsPowerDef.visableField, 1)).updateEx();
    }

    //判断是否包含子节点，返回包含的子节点数量
    public long hasChildren() {
        long rl = 0;
        String pkField = getPk();
        JSONObject json = field(pkField)._find();
        if (json != null && json.containsKey(pkField)) {
            rl = eq(PermissionsPowerDef.fatherIDField, json.getPkValue(pkField)).count();
        }
        return rl;
    }

    //获得下1级子节点数据集合
    public JSONArray getChilren(List<Function<JSONArray, JSONArray>> outPipe) {
        JSONArray rArray = null;
        JSONObject json = find();//获得当前数据
        if (json != null) {
            getChildren(json, null, false);
            rArray = json.getJsonArray(PermissionsPowerDef.childrenData);
        }
        return rArray;
    }

    // 获得下级全部子节点数据集合
    public JSONObject getAllChildren() {
        /// 获得当前设定的根数据
        JSONObject json = find();
        if (json != null) {
            // 管线方式过滤数据
            JSONArray rArray = JSONArray.addx(json);
            if (pipeJSONArray_Out != null && !JSONArray.isInvalided(rArray)) {
                for (Function<JSONArray, JSONArray> func : pipeJSONArray_Out) {
                    rArray = func.apply(rArray);
                }
            }
            json = JSONArray.isInvalided(rArray) ? null : (JSONObject) rArray.get(0);
            if (json != null) {
                if ((count() > 10)) {
                    getAllChildren0(json);
                } else {
                    getAllChildren1(json);
                }
            }
        }
        fields = null;
        maskfields = null;
        return json;
    }

    // 获得下级全部子节点数据集合,一次访问数据库
    private JSONObject getAllChildren1(JSONObject json) {
        // 获得表全部数据库
        JSONArray array = select();
        if (JSONArray.isInvalided(array)) {
            System.out.println("返回集合为空");
        }
        // 根据根数据构造树
        getChildren(json, array, true);
        return json;
    }

    // 获得下级全部子节点数据集合,分多次访问数据库
    private JSONObject getAllChildren0(JSONObject json) {
        getChildren(json, null, true);
        return json;
    }

    //获得所有父ID是fid的数据
    private boolean getChildren(JSONObject fristJson, JSONArray array, boolean isAll) {
        Object fid = null;
        String pkField = getPk();
        if (pkField != null && fristJson.containsKey(pkField)) {
            fid = fristJson.getPkValue(pkField);
        }
        if (fid != null) {
            JSONArray rArray = new JSONArray();
            if (array == null) {
                if (fields != null) {
                    String[] s = new String[fields.size()];
                    fields.toArray(s);
                    super.field(s);
                }
                if (maskfields != null) {
                    String[] s = new String[maskfields.size()];
                    maskfields.toArray(s);
                    super.mask(s);
                }
                rArray = eq(PermissionsPowerDef.fatherIDField, fid).select();
            } else {
                // 找到所有fatherIDField == fid的数据
                for (JSONObject obj : (Iterable<JSONObject>) array) {
                    if (obj.get(PermissionsPowerDef.fatherIDField).equals(fid)) {
                        rArray.add(obj);
                    }
                }
            }

            // 管线方式过滤数据
            if (pipeJSONArray_Out != null && rArray != null && rArray.size() > 0) {
                for (Function<JSONArray, JSONArray> func : pipeJSONArray_Out) {
                    rArray = func.apply(rArray);
                }
            }

            // 下一层
            if (isAll) {
                JSONObject tempJson;
                int l = Objects.requireNonNull(rArray).size();
                for (int i = 0; i < l; i++) {
                    tempJson = (JSONObject) (rArray.get(i));
                    getChildren(tempJson, array, true);
                    rArray.set(i, tempJson);
                }
                fristJson.put(PermissionsPowerDef.childrenData, rArray);
            }
        }
        return fid != null;
    }

    //判断是否包含父节点，返回父节点值，为空表示不包含
    public boolean hasFather() {
        return getFather() != null;
    }

    //获得父节点值
    public Object getFather() {
        Object ro = null;
        JSONObject json = field(PermissionsPowerDef.fatherIDField).find();
        if (json != null && json.containsKey(PermissionsPowerDef.fatherIDField)) {
            ro = json.get(PermissionsPowerDef.fatherIDField);
        }
        return ro;
    }

    //获得父节点数据
    public JSONObject getFatherData() {
        JSONObject rjson = null;
        Object ro = getFather();
        if (ro != null) {
            rjson = eq(getPk(), ro).find();
        }
        return rjson;
    }

    private void niceCond() {
        and().eq(PermissionsPowerDef.deleteField, 0);
        //and().groupCondition(dbf.and().eq(PermissionsPowerDef.deleteField, 0).buildex());
    }

    private JSONObject _find() {
        if (!realMode) {
            niceCond();
        }
        return super.find();
    }


    public JSONObject find() {
        if (!realMode) {
            niceCond();
        }
        JSONObject rInfo = super.find();
        if (!JSONObject.isInvalided(rInfo)) {
            JSONArray rArray = runOutPipe(JSONArray.addx(rInfo));
            if (!JSONArray.isInvalided(rArray)) {
                rInfo = (JSONObject) rArray.get(0);
            }
        }
        return rInfo;
    }


    public JSONArray select() {
        if (!realMode) {
            niceCond();
        }
        return runOutPipe(super.select());
    }


    public JSONArray page(int pageidx, int pagemax) {
        if (!realMode) {
            niceCond();
        }
        return runOutPipe(super.page(pageidx, pagemax));
    }


    public JSONArray page(int pageidx, int pagemax, Object lastObj) {
        if (!realMode) {
            niceCond();
        }
        return runOutPipe(super.page(pageidx, pagemax, lastObj));
    }

    //设置数据权重级别
    public boolean setItemLevel(int newLevel) {
        return data(new JSONObject(PermissionsPowerDef.levelField, newLevel)).updateEx();
    }

    //设置数据排序权重级别
    public boolean setSort(int newSort) {
        return data(new JSONObject(PermissionsPowerDef.sortField, newSort)).updateEx();
    }

    //设置父节点
    public boolean setFather(Object newfid) {
        return data(new JSONObject(PermissionsPowerDef.fatherIDField, newfid)).updateEx();
    }

    private boolean setAuth(int op, int type, Object val) {
        boolean rb = false;
        String fieldName = null;
        String vFieldName = null;
        switch (op) {
            case 0: {
                fieldName = PermissionsPowerDef.readMode;
                vFieldName = PermissionsPowerDef.readValue;
                break;
            }
            case 1: {
                fieldName = PermissionsPowerDef.updateMode;
                vFieldName = PermissionsPowerDef.updateValue;
                break;
            }
            case 2: {
                fieldName = PermissionsPowerDef.deleteMode;
                vFieldName = PermissionsPowerDef.deleteValue;
                break;
            }
        }
        if (fieldName != null) {
            JSONObject json = (new JSONObject(fieldName, type)).puts(vFieldName, val);
            rb = data(json).updateEx();
        }
        return rb;
    }

    //设置读权限
    public boolean readAuth(int checkType, Object checkCond) {
        return setAuth(0, checkType, checkCond);
    }

    //设置改权限
    public boolean updateAuth(int checkType, Object checkCond) {
        return setAuth(1, checkType, checkCond);
    }

    //设置删权限
    public boolean deleteAuth(int checkType, Object checkCond) {
        return setAuth(2, checkType, checkCond);
    }

    private JSONObject getAuth(int op) {
        JSONObject rs = null;
        String fieldName;
        switch (op) {
            case 0:
                fieldName = PermissionsPowerDef.readMode;
                break;
            case 1:
                fieldName = PermissionsPowerDef.updateMode;
                break;
            case 2:
                fieldName = PermissionsPowerDef.deleteMode;
                break;
            default:
                fieldName = null;
        }
        if (fieldName != null) {
            JSONObject json = field(fieldName)._find();
            if (json != null && json.containsKey(fieldName)) {
                rs = json.getJson(fieldName);
            }
        }
        return rs;
    }

    //获得读权限
    public JSONObject readAuth() {
        return getAuth(0);
    }

    //获得改权限
    public JSONObject updateAuth() {
        return getAuth(1);
    }

    //获得删权限
    public JSONObject deleteAuth() {
        return getAuth(2);
    }


    public GrapeTreeDbLayerModel and() {
        super.and();
        return this;
    }


    public GrapeTreeDbLayerModel or() {
        super.or();
        return this;
    }


    public GrapeTreeDbLayerModel where(JSONArray condArray) {
        super.where(condArray);
        return this;
    }


    public GrapeTreeDbLayerModel eq(String field, Object value) {//One Condition
        super.eq(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel ne(String field, Object value) {//One Condition
        super.ne(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel gt(String field, Object value) {//One Condition
        super.gt(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel lt(String field, Object value) {//One Condition
        super.lt(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel gte(String field, Object value) {//One Condition
        super.gte(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel lte(String field, Object value) {//One Condition
        super.lte(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel like(String field, Object value) {
        super.like(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel asc(String field) {
        super.asc(field);
        return this;
    }


    public GrapeTreeDbLayerModel desc(String field) {
        super.desc(field);
        return this;
    }


    public JSONArray scan(Function<JSONArray, JSONArray> func, int max) {
        return super.scan(func, max);
    }
}
