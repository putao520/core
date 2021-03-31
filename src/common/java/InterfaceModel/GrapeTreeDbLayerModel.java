package common.java.InterfaceModel;

import common.java.Apps.AppContext;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Apps.MicroService.Model.MicroModel;
import common.java.Authority.Permissions;
import common.java.Check.FormHelper;
import common.java.Database.DbFilter;
import common.java.Database.DbLayer;
import common.java.Database.InterfaceDatabase;
import common.java.HttpServer.HttpContext;
import common.java.InterfaceModel.Type.Aggregation;
import common.java.ServiceTemplate.SuperItemField;
import common.java.Session.UserSession;
import common.java.String.StringHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Function;


public class GrapeTreeDbLayerModel implements InterfaceDatabase<GrapeTreeDbLayerModel> {
    private List<Function<JSONArray, JSONArray>> pipeJSONArray_Out;
    private boolean hardMode = false;
    private boolean SuperMode = false;
    private DbLayer db;
    private MicroModel mModel = null;
    private String pkField = null;
    private FormHelper checker = null;
    private Permissions permissions = null;

    private Aggregation aggregationJSONArray_Out;

    private GrapeTreeDbLayerModel() {
        init();
    }

    private GrapeTreeDbLayerModel(String modelName) {
        init();
        this.descriptionModel(modelName);
    }

    public DbLayer getPureDB() {
        return this.db;
    }

    public GrapeTreeDbLayerModel outPiperEnable(boolean flag) {
        this.db.setPiperEnable(flag);
        return this;
    }

    public void Close() {
        this.db.Close();
    }

    public void addConstantCond(String fieldName, Object CondValue) {
        this.db.addConstantCond(fieldName, CondValue);
    }

    public String getConditionString() {
        return this.db.getConditionString();
    }

    public boolean nullCondition() {
        return this.db.nullCondition();
    }

    public GrapeTreeDbLayerModel groupCondition(List<List<Object>> conds) {
        this.db.groupCondition(conds);
        return this;
    }

    public GrapeTreeDbLayerModel groupWhere(JSONArray conds) {
        this.db.groupWhere(conds);
        return this;
    }

    public GrapeTreeDbLayerModel form(String _formName) {
        this.db.form(_formName);
        return this;
    }

    public GrapeTreeDbLayerModel skip(int no) {
        this.db.skip(no);
        return this;
    }

    public GrapeTreeDbLayerModel limit(int no) {
        this.db.limit(no);
        return this;
    }

    public GrapeTreeDbLayerModel findOne() {
        this.db.findOne();
        return this;
    }

    /**
     * 填充模型和权限模型
     */
    private GrapeTreeDbLayerModel descriptionModel(String modelName) {
        this.mModel = MicroServiceContext.current().model(modelName);
        String tableName = this.mModel.tableName();
        if (!StringHelper.isInvalided(tableName)) {
            pkField = this.db.form(tableName).bind().getGeneratedKeys();
            checker = FormHelper.build().importField(this.mModel.rules());
            permissions = new Permissions(this.mModel.tableName());
        }
        return this;
    }

    public static GrapeTreeDbLayerModel getInstance() {
        return new GrapeTreeDbLayerModel();
    }

    public static GrapeTreeDbLayerModel getInstance(String modelName) {
        return new GrapeTreeDbLayerModel(modelName);
    }

    public GrapeTreeDbLayerModel addFieldOutPipe(String fieldName, Function<Object, Object> func) {
        this.db.addFieldOutPipe(fieldName, func);
        return this;
    }

    public GrapeTreeDbLayerModel addFieldInPipe(String fieldName, Function<Object, Object> func) {
        this.db.addFieldInPipe(fieldName, func);
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
    private JSONArray<JSONObject> runOutPipe(JSONArray<JSONObject> input) {
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
        pipeJSONArray_Out = new ArrayList<>();
        aggregationJSONArray_Out = null;
        db = new DbLayer();
    }

    /**
     * 真实模式，数据操作时不考虑特殊字段
     */
    public GrapeTreeDbLayerModel hardMode() {
        hardMode = true;
        return this;
    }

    public GrapeTreeDbLayerModel softMode() {
        hardMode = false;
        return this;
    }

    /**
     * 超级模式,数据更新时,无视protected字段
     */
    public GrapeTreeDbLayerModel superMode() {
        SuperMode = true;
        return this;
    }

    public GrapeTreeDbLayerModel normalMode() {
        SuperMode = false;
        return this;
    }

    public GrapeTreeDbLayerModel bind() {
        this.db.bind();
        return this;
    }

    public GrapeTreeDbLayerModel bind(String ownerID) {
        this.db.bind(ownerID);
        return this;
    }

    public GrapeTreeDbLayerModel dirty() {
        this.db.dirty();
        return this;
    }


    public int pageMax(int max) {
        return this.db.pageMax(max);
    }

    public GrapeTreeDbLayerModel field(String fieldString) {
        if (fieldString != null) {
            String[] s = fieldString.split(",");
            field(s);
        }
        return this;
    }

    public GrapeTreeDbLayerModel field() {
        this.db.field();
        return this;
    }

    public GrapeTreeDbLayerModel field(String[] fields) {
        this.db.field(fields);
        return this;
    }


    public GrapeTreeDbLayerModel mask(String fieldString) {
        this.db.mask(fieldString.split(","));
        return this;
    }


    public GrapeTreeDbLayerModel mask(String[] fields) {
        mask(StringHelper.join(fields));
        return this;
    }


    public GrapeTreeDbLayerModel max(String groupByString) {
        this.db.max(groupByString);
        return this;
    }


    public GrapeTreeDbLayerModel min(String groupByString) {
        this.db.min(groupByString);
        return this;
    }


    public GrapeTreeDbLayerModel avg(String groupByString) {
        this.db.avg(groupByString);
        return this;
    }


    public GrapeTreeDbLayerModel data(JSONObject obj) {
        this.db.data(obj);
        return this;
    }


    public GrapeTreeDbLayerModel data(String str) {
        this.db.data(str);
        return this;
    }

    public List<JSONObject> ClearData() {
        return this.db.clearData();
    }

    public List<JSONObject> data() {
        return this.db.data();
    }

    //显示数据
    public boolean show() {
        return data(new JSONObject(SuperItemField.visibleField, 0)).update() != null;
    }

    //隐藏数据
    public boolean hide() {
        return data(new JSONObject(SuperItemField.visibleField, 1)).update() != null;
    }

    //判断是否包含子节点，返回包含的子节点数量
    public long hasChildren() {
        long rl = 0;
        String pkField = getGeneratedKeys();
        JSONObject json = field(pkField)._find();
        if (json != null && json.containsKey(pkField)) {
            rl = eq(SuperItemField.fatherField, json.getPkValue(pkField)).count();
        }
        return rl;
    }

    //获得下1级子节点数据集合
    public JSONArray getChildren(List<Function<JSONArray, JSONArray>> outPipe) {
        JSONArray rArray = null;
        JSONObject json = find();//获得当前数据
        if (json != null) {
            getChildren(json, null, false);
            rArray = json.getJsonArray(SuperItemField.childrenData);
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
    private boolean getChildren(JSONObject firstJson, JSONArray array, boolean isAll) {
        Object fid = null;
        String pkField = getGeneratedKeys();
        if (pkField != null && firstJson.containsKey(pkField)) {
            fid = firstJson.getPkValue(pkField);
        }
        if (fid != null) {
            JSONArray rArray = new JSONArray();
            if (array == null) {
                rArray = eq(SuperItemField.fatherField, fid).select();
            } else {
                // 找到所有fatherIDField == fid的数据
                for (JSONObject obj : (Iterable<JSONObject>) array) {
                    if (obj.get(SuperItemField.fatherField).equals(fid)) {
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
                firstJson.put(SuperItemField.childrenData, rArray);
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
        JSONObject json = field(SuperItemField.fatherField).find();
        if (json != null && json.containsKey(SuperItemField.fatherField)) {
            ro = json.get(SuperItemField.fatherField);
        }
        return ro;
    }

    //获得父节点数据
    public JSONObject getFatherData() {
        JSONObject rjson = null;
        Object ro = getFather();
        if (ro != null) {
            rjson = eq(getGeneratedKeys(), ro).find();
        }
        return rjson;
    }

    private void niceCond() {
        if (!hardMode) {
            and().eq(SuperItemField.deleteField, 0);
        }
    }

    public void invalidCache() {
        this.db.invalidCache();
    }

    // 写方法群
    private JSONObject autoCompletePerms(JSONObject data) {
        if (UserSession.hasSession()) {
            UserSession us = UserSession.current();
            if (us != null) {
                data.puts(SuperItemField.userIdField, us.getUID())
                        .put(SuperItemField.groupIdField, us.getGID());
            }
        }
        return data;
    }

    /**
     * @apiNote 按照字段定义过滤, 替换
     */
    private void _insertFilter() {
        List<JSONObject> dataArr = db.clearData();
        for (JSONObject data : dataArr) {
            // 验证数据是否正确 和 补充额外数据字段 和 补充权限管理字段
            db.data(autoCompletePerms(checker.autoComplete(data)));
        }
    }

    public Object insertOnce() {
        if (!permissions.writeFilter(db.data())) {
            HttpContext.current().throwDebugOut("当前用户无权新增数据!");
            return null;
        }
        _insertFilter();
        return db.insertOnce();
    }

    public List<Object> insert() {
        if (!permissions.writeFilter(db.data())) {
            HttpContext.current().throwDebugOut("当前用户无权新增数据!");
            return null;
        }
        _insertFilter();
        return this.db.insert();
    }

    public void asyncInsert() {
        if (!permissions.writeFilter(db.data())) {
            HttpContext.current().throwDebugOut("当前用户无权新增数据!");
            return;
        }
        _insertFilter();
        this.db.asyncInsert();
    }

    // 更新操作群

    /**
     * @apiNote 按照字段定义过滤, 替换
     * protectField:    不允许直接修改,需要显示提权
     * lockerField:     不允许修改
     */
    private void _updateFilter(JSONObject v) {
        // 去掉locker字段
        checker.store(v).filterLocked();
        // 普通模式时,过滤掉 protected 字段
        if (!SuperMode) {
            checker.filterProtect();
        }
        JSONObject info = checker.toJson();
        if (JSONObject.isInvalided(info)) {
            return;
        }
        db.data(info);
    }

    private boolean _updateImpl(JSONObject v) {
        DbFilter q = DbFilter.buildDbFilter();
        if (!permissions.updateFilter(q, v)) {
            HttpContext.current().throwDebugOut("当前用户无权更新数据!");
            return false;
        }
        _updateFilter(v);
        if (!q.nullCondition()) {
            this.db.and().groupCondition(q.buildEx());
        }
        return true;
    }

    private boolean _update() {
        List<JSONObject> d = db.data();
        if (d.size() == 0) {
            return false;
        }
        JSONObject info = d.get(0);
        if (JSONObject.isInvalided(info)) {
            HttpContext.current().throwDebugOut("当前用户更新数据为空!");
            return false;
        }
        if (!checker.checkTable(info, true)) {
            HttpContext.current().throwDebugOut("当前用户更新数据[" + checker.getlastErrorName() + "] ->不合法!");
            return false;
        }
        return _updateImpl(info);
    }

    public JSONObject update() {
        return _update() ? this.db.update() : null;
    }

    public long updateAll() {
        return _update() ? this.db.updateAll() : 0;
    }

    private boolean _computerOperator(String fieldName) {
        if (!_updateImpl(JSONObject.build(fieldName, ""))) {
            return false;
        }
        List<JSONObject> vArr = db.data();
        return vArr.size() != 0 && !JSONObject.isInvalided(vArr.get(0));
    }

    public JSONObject inc(String fieldName) {
        return _computerOperator(fieldName) ? this.db.inc(fieldName) : null;
    }

    public JSONObject dec(String fieldName) {
        return _computerOperator(fieldName) ? this.db.dec(fieldName) : null;
    }

    public JSONObject add(String fieldName, long num) {
        return _computerOperator(fieldName) ? this.db.add(fieldName, num) : null;
    }

    public JSONObject sub(String fieldName, long num) {
        return _computerOperator(fieldName) ? this.db.sub(fieldName, num) : null;
    }

    // 删操作集群
    private boolean _deleteFilter() {
        DbFilter q = DbFilter.buildDbFilter();
        if (!permissions.deleteFilter(q)) {
            HttpContext.current().throwDebugOut("当前用户无权删除数据!");
            return false;
        }
        if (!q.nullCondition()) {
            db.and().groupCondition(q.buildEx());
        }
        return true;
    }

    /**
     * 直接删除
     */
    public JSONObject delete() {
        if (!_deleteFilter()) {
            return null;
        }
        if (hardMode) {
            return this.db.delete();
        } else {
            return data(new JSONObject(SuperItemField.deleteField, 1)).update();
        }
    }

    /**
     * 直接删除全部
     */
    public long deleteAll() {
        if (!_deleteFilter()) {
            return -1;
        }
        if (hardMode) {
            return this.db.deleteAll();
        } else {
            return data(new JSONObject(SuperItemField.deleteField, 1)).updateAll();
        }
    }

    // 读取方法群
    private JSONObject findOutFilter(JSONObject rInfo) {
        if (JSONObject.isInvalided(rInfo)) {
            return rInfo;
        }
        JSONArray<JSONObject> rArray = runOutPipe(JSONArray.build(rInfo));
        if (JSONArray.isInvalided(rArray)) {
            return rInfo;
        }
        return rArray.get(0);
    }

    private boolean _readFilter() {
        // 处理额外条件
        DbFilter q = DbFilter.buildDbFilter();
        if (!permissions.readFilter(q)) {
            HttpContext.current().throwDebugOut("当前用户无权访问数据!");
            return false;
        }
        if (!q.nullCondition()) {
            db.and().groupCondition(q.buildEx());
        }
        // 处理mask字段
        String[] mask_fields = checker.getMaskFields();
        if (mask_fields.length > 0) {
            db.mask(mask_fields);
        }
        return true;
    }

    private JSONObject _find() {
        return _readFilter() ? this.db.find() : null;
    }

    public JSONObject find() {
        return findOutFilter(_find());
    }

    public JSONObject findByCache(int second) {
        return findOutFilter(
                _readFilter() ?
                        this.db.findByCache(second) :
                        null
        );
    }

    public JSONObject findByCache() {
        return findOutFilter(
                _readFilter() ?
                        this.db.findByCache() :
                        null
        );
    }

    public JSONArray selectByCache(int second) {
        return runOutPipe(_readFilter() ?
                this.db.selectByCache(second) :
                null
        );
    }

    public JSONArray select() {
        return runOutPipe(_readFilter() ?
                this.db.select() :
                null
        );
    }

    public JSONArray page(int pageIdx, int pageMax) {
        return runOutPipe(_readFilter() ?
                this.db.page(pageIdx, pageMax) :
                null
        );
    }

    public JSONArray page(int pageIdx, int pageMax, int lastId, String fastField) {
        return runOutPipe(_readFilter() ?
                this.db.page(pageIdx, pageMax, lastId, fastField) :
                null
        );
    }

    public JSONArray page(int pageIdx, int pageMax, Object lastObj) {
        return runOutPipe(_readFilter() ?
                this.db.and().eq(getGeneratedKeys(), lastObj).page(pageIdx, pageMax) :
                null
        );
    }

    public JSONArray group(String groupName) {
        return (_readFilter() ?
                this.db.group(groupName) :
                null
        );
    }

    //覆盖父类分组方法
    public JSONArray group() {
        return (_readFilter() ?
                this.db.group() :
                null
        );
    }

    public long count() {
        return (_readFilter() ?
                this.db.count() :
                -1
        );
    }

    //设置数据权重级别
    public boolean setItemLevel(int newLevel) {
        return data(new JSONObject(SuperItemField.levelField, newLevel)).update() != null;
    }

    //设置数据排序权重级别
    public boolean setSort(int newSort) {
        return data(new JSONObject(SuperItemField.sortField, newSort)).update() != null;
    }

    //设置父节点
    public boolean setFather(Object newFatherId) {
        return data(new JSONObject(SuperItemField.fatherField, newFatherId)).update() != null;
    }

    public GrapeTreeDbLayerModel and() {
        this.db.and();
        return this;
    }


    public GrapeTreeDbLayerModel or() {
        this.db.or();
        return this;
    }


    public GrapeTreeDbLayerModel where(JSONArray condArray) {
        this.db.where(condArray);
        return this;
    }


    public GrapeTreeDbLayerModel eq(String field, Object value) {//One Condition
        this.db.eq(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel ne(String field, Object value) {//One Condition
        this.db.ne(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel gt(String field, Object value) {//One Condition
        this.db.gt(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel lt(String field, Object value) {//One Condition
        this.db.lt(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel gte(String field, Object value) {//One Condition
        this.db.gte(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel lte(String field, Object value) {//One Condition
        this.db.lte(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel like(String field, Object value) {
        this.db.like(field, value);
        return this;
    }


    public GrapeTreeDbLayerModel asc(String field) {
        this.db.asc(field);
        return this;
    }


    public GrapeTreeDbLayerModel desc(String field) {
        this.db.desc(field);
        return this;
    }

    public JSONArray scan(Function<JSONArray, JSONArray> func, int max) {
        return this.db.scan(func, max);
    }

    public JSONArray scan(Function<JSONArray, JSONArray> func, int max, int synNo) {
        return this.db.scan(func, max, synNo);
    }

    public JSONArray distinct(String fieldName) {
        return this.db.distinct(fieldName);
    }

    public GrapeTreeDbLayerModel count(String groupByString) {
        this.db.count(groupByString);
        return this;
    }

    public GrapeTreeDbLayerModel sum(String groupByString) {
        this.db.sum(groupByString);
        return this;
    }

    public String getFullForm() {
        return this.db.getFullForm();
    }

    public String getFormName() {
        return this.db.getFormName();
    }

    public String getForm() {
        return this.db.getForm();
    }

    public int limit() {
        return this.db.limit();
    }

    public String getGeneratedKeys() {
        return pkField == null ? this.db.getGeneratedKeys() : pkField;
    }


    public void clear() {
        this.db.clear();
    }

    public JSONObject getCond() {
        return this.db.getCond();
    }

    public GrapeTreeDbLayerModel setCond(JSONObject conJSON) {
        this.db.setCond(conJSON);
        return this;
    }

    public List<String> getAllTables() {
        return this.db.getAllTables();
    }
}
