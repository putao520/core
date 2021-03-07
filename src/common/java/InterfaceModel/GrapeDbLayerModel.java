package common.java.InterfaceModel;

import common.java.Apps.MModelPerm;
import common.java.Apps.MicroModel;
import common.java.Apps.MicroServiceContext;
import common.java.Authority.Permissions;
import common.java.Authority.PermissionsPowerDef;
import common.java.Authority.PlvDef;
import common.java.Authority.PlvDef.Operater;
import common.java.Check.FormHelper;
import common.java.Database.DbLayer;
import common.java.Database.InterfaceDatabase;
import common.java.HttpServer.HttpContext;
import common.java.Session.UserSession;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
//app依赖

/**
 * @author yuyao
 */
public class GrapeDbLayerModel implements InterfaceDatabase<GrapeDbLayerModel> {
    private final String rField = PermissionsPowerDef.readMode;
    private final String uField = PermissionsPowerDef.updateMode;
    private final String dField = PermissionsPowerDef.deleteMode;
    private final String cField = PermissionsPowerDef.createMode;
    private final String sField = PermissionsPowerDef.statisticsMode;
    private final String rVField = PermissionsPowerDef.readValue;
    private final String uVField = PermissionsPowerDef.updateValue;
    private final String dVField = PermissionsPowerDef.deleteValue;
    private final String cVField = PermissionsPowerDef.createValue;
    private final String sVField = PermissionsPowerDef.statisticsValue;
    private final DbLayer db;
    private boolean isDirty = false;
    private String[] cacheField;
    private boolean authEnable = false;
    // private PermissionGroup permissionModel = null;
    private Permissions op;
    private String pkField = null;
    // private GrapeDBDescriptionModel gDb = null;
    private MicroModel mModel = null;
    private FormHelper checker = null;
    private JSONObject dataCache = null;
    private JSONObject fullDataCache = null;
    private boolean useField = false;
    private boolean tempAdmin = false;
    private int runtimeMode = 0;

    public GrapeDbLayerModel() {//自动绑定当前服务对应数据库配置
        db = new DbLayer();
    }

    public GrapeDbLayerModel(String dataModelName) {
        db = new DbLayer();
        this.descriptionModel(dataModelName);
    }

    //获得模型描述对象
    protected MicroModel descriptionModel() {
        return this.mModel;
    }

    protected Permissions getPlvCheckObject() {
        return op;
    }

    public DbLayer getPureDB() {
        return this.db;
    }

    //自动补齐字段初始值
    private GrapeDbLayerModel autoComplete() {
        if (checker != null && dataCache != null) {
            fullDataCache = checker.autoComplete(dataCache, this.mModel.perms().buildPermRuleNode());
        }
        return this;
    }

    public GrapeDbLayerModel addFieldOutPipe(String fieldName, Function<Object, Object> func) {
        this.db.addFieldOutPipe(fieldName, func);
        return this;
    }

    public GrapeDbLayerModel addFieldInPipe(String fieldName, Function<Object, Object> func) {
        this.db.addFieldInPipe(fieldName, func);
        return this;
    }

    public GrapeDbLayerModel dirty() {
        isDirty = true;
        this.db.dirty();
        return this;
    }

    public GrapeDbLayerModel outPiperEnable(boolean flag) {
        this.db.setPiperEnable(flag);
        return this;
    }

    public String getPk() {
        return pkField;
    }


    public GrapeDbLayerModel bind() {
        this.db.bind();
        return this;
    }

    public GrapeDbLayerModel checkMode() {
        runtimeMode = GrapeDBMode.checkMode;
        return this;
    }

    public GrapeDbLayerModel safeMode() {
        runtimeMode = GrapeDBMode.safeMode;
        return this;
    }

    public GrapeDbLayerModel dataEx(JSONObject json) {
        fullDataCache = new JSONObject();
        dataCache = json;
        this.db.data(json);
        return this;
    }

    public GrapeDbLayerModel dataEx(String jsonString) {
        data(JSONObject.toJSON(jsonString));
        return this;
    }

    //临时提高到管理员权限
    public GrapeDbLayerModel upToken() {
        tempAdmin = true;
        if (checker != null) {
            checker.stopCheck();
        }
        return this;
    }

    //设置模型描述结构->必须

    /**
     * 填充模型和权限模型
     */
    private GrapeDbLayerModel descriptionModel(String modelName) {
        this.mModel = MicroServiceContext.current().model(modelName);
        String tableName = this.mModel.tableName();
        if (!StringHelper.invaild(tableName)) {
            pkField = this.db.form(tableName).bind().getGeneratedKeys();
            checker = (new FormHelper()).importField(this.mModel.rules());
            enableCheck();
        }
        return this;
    }

    private fieldState saveField() {
        fieldState fs = new fieldState();
        fs.cacheField = cacheField;
        //fs.cacheFielduse = useField;
        return fs;
    }

    //开启检查
    private GrapeDbLayerModel enableCheck() {
        op = (new Permissions(this.db.getFormName()));
        authEnable = true;
        return this;
    }

    //关闭检查
    public GrapeDbLayerModel disableCheck() {
        op = null;
        authEnable = false;
        return this;
    }

    // 将补齐后结果集合与校验后结果合并
    private boolean checkAndPushData() {
        boolean auth = true;
        if (checker != null && !tempAdmin) {
            switch (runtimeMode) {
                case GrapeDBMode.safeMode:
                    auth = checker.filterAndCheckTable(dataCache, true);
                    break;
                case GrapeDBMode.checkMode:
                    auth = checker.checkTable(dataCache, true);
                    break;
            }
            fullDataCache.putAll(dataCache);
            this.db.data(fullDataCache);
        }
        return auth;
    }

    //添加数据
    public Object insertEx() {
        Object rID = null;
        boolean auth = this.autoComplete().checkAndPushData();    // 新增数据时,动态生成要检查的gsc-model
        if (auth && authStub(Operater.create)) {//授权通过时
            rID = this.db.insertOnce();
        } else {
            nLogger.debugInfo("新增失败 原因:[" + UserSession.current().getUID() + " 无权操作！]");
        }
        reInit();
        return rID;
    }

    //覆盖父类计数方法

    public long count() {
        long rl;
        if (!tempAdmin) {
            List<List<Object>> newCond = authFilter(Operater.statist);
            this.db.and().groupCondition(newCond);
        }

        rl = this.db.count();
        reInit();
        return rl;
    }

    //覆盖父类分组方法
    public JSONArray group(String groupName) {
        JSONArray rArray;
        if (!tempAdmin) {
            List<List<Object>> newCond = authFilter(Operater.statist);
            this.db.and().groupCondition(newCond);
        }

        rArray = this.db.group(groupName);
        reInit();
        return rArray;
    }

    //覆盖父类分组方法
    public JSONArray group() {
        JSONArray rArray;
        if (!tempAdmin) {
            List<List<Object>> newCond = authFilter(Operater.statist);
            this.db.and().groupCondition(newCond);
        }
        rArray = this.db.group();

        reInit();
        return rArray;
    }

    public GrapeDbLayerModel handle() {
        return this;
    }

    private void restoreField(fieldState fs) {
        field(fs.cacheField);
    }

    public GrapeDbLayerModel field(String[] fields) {
        if (fields != null) {
            if (checker != null && !tempAdmin) {//设置了检查模型，而且临时管理员模式关闭
                checker.filterMask(fields);
            }
            String _val = null;
            if (fields.length > 0) {
                useField = true;
                _val = StringHelper.join(fields);
            }
            if (_val == null) {
                this.db.field();
            } else {
                this.db.field(_val);
            }
        } else {
            useField = false;
            this.db.field();
        }
        cacheField = fields;

        return this;
    }

    //数据库里的权限对象转化成检查用的权限对象

    /**
     * 从{
     * rmode:
     * rvalue:
     * cmode:
     * cvalue:
     * umode:
     * uvalue:
     * dmode:
     * dvalue:
     * smode:
     * svalue:
     * }
     * 到
     * {
     * rmode:{
     * chktype:
     * chkvalue:
     * },
     * ...
     * }
     */
    private JSONObject DBPermissionsField2Permissions(JSONObject impJson) {
        JSONObject rJson = new JSONObject();
        if (impJson.containsKey(rField) && impJson.containsKey(rVField)) {
            rJson.put(rField, (new JSONObject(PlvDef.plvType.chkType, impJson.get(rField))).puts(PlvDef.plvType.chkVal, impJson.get(rVField)).toJSONString());
        }
        if (impJson.containsKey(uField) && impJson.containsKey(uVField)) {
            rJson.put(uField, (new JSONObject(PlvDef.plvType.chkType, impJson.get(uField))).puts(PlvDef.plvType.chkVal, impJson.get(uVField)).toJSONString());
        }
        if (impJson.containsKey(dField) && impJson.containsKey(dVField)) {
            rJson.put(dField, (new JSONObject(PlvDef.plvType.chkType, impJson.get(dField))).puts(PlvDef.plvType.chkVal, impJson.get(dVField)).toJSONString());
        }
        return rJson;
    }

    //获得行数据权限效验字段值
    private MModelPerm getPermissionsFromDB() {
        /*
        String[] fields = {rField, uField, dField, rVField, uVField, dVField};
        fieldState _fs = saveField();
        boolean dirtyMode = false;
        if (isDirty) {
            dirtyMode = true;
        }
        upToken().field(fields).dirty();    // 从数据库读数据的权限值
        JSONObject permissionsObjects = this.db.findbyCache(2);//获得read,update,delete权限数据,如果表中数据对应的权限字段不存在,直接从模型中获得
        // 根据脏操作状态，还原脏操作
        if (dirtyMode) {
            dirty();
        }
        // isDirty = false;
        restoreField(_fs);
        return permissionsObjects == null ? this.mModel.perms() : new MModelPerm(DBPermissionsField2Permissions(permissionsObjects));
         */
        return this.mModel.perms();
    }

    /**
     * 授权检查
     * 如果没有开启权限检查，默认都通过
     */
    private boolean authStub(int plvOperate) {
        boolean rs = true;
        if (authEnable && !tempAdmin) {
            rs = checkOperateItem(plvOperate, getPermissionsFromDB());
        }
        return rs;
    }

    public GrapeDbLayerModel mask(String[] maskfield) {
        if (maskfield != null && maskfield.length > 0) {
            this.db.mask(StringHelper.join(maskfield, ","));
        }
        return this;
    }

    private void initOp(MModelPerm pInfo) {
        op.putPermInfo(pInfo);
    }

    /**
     * 数据权限检查
     *
     * @param plvOperate 操作类型
     * @param pInfo      行级权限描述
     */
    //数据权限检查
    private boolean checkOperateItem(int plvOperate, MModelPerm pInfo) {
        initOp(pInfo);
        return op.checkOperate(plvOperate);
    }

    private List<List<Object>> authFilter(int plvOperate) {
        return _authFilter(plvOperate, getPermissionsFromDB());
    }

    /**
     * 根据权限过滤返回值数据
     *
     * @param plvOperate
     * @param pInfo
     * @return
     */
    private List<List<Object>> _authFilter(int plvOperate, MModelPerm pInfo) {
        // List<List<Object>> newCond = null;
        initOp(pInfo);
        return op.filterCond(plvOperate);
    }

    //覆盖父类查找一个
    public JSONObject find() {
        JSONObject robj = null;
        if (!useField && checker != null && !tempAdmin) {//没有使用字段,开启了模型检查,非临时管理模式
            mask(checker.getMaskFields());    //过滤掉mask字段
        }

        if (authStub(Operater.read)) {//授权通过时
            robj = _find();
        } else {
            nLogger.debugInfo("查找失败 原因:[" + UserSession.current().getUID() + " 无权操作！]");
        }

        reInit();
        return robj;
    }

    private boolean updateChecker() {
        boolean auth = true;
        if (checker != null && !tempAdmin) {//数据效验通过
            switch (runtimeMode) {
                case GrapeDBMode.safeMode:
                    checker.filterProtect(dataCache);//过滤临时字段
                    break;
                case GrapeDBMode.checkMode:
                    auth = checker.checkTable(dataCache, false);
                    break;
            }
        }
        return auth;
    }

    @Deprecated
    public JSONObject insertOnce() {
        nLogger.logInfo("insertOnce方法禁止使用");
        return null;
    }


    public GrapeDbLayerModel asc(String field) {
        this.db.asc(field);
        return this;
    }


    public GrapeDbLayerModel desc(String field) {
        this.db.desc(field);
        return this;
    }

    @Deprecated
    public JSONObject update() {
        nLogger.logInfo("update方法禁止使用");
        return null;
    }

    public boolean updateEx() {
        JSONObject rjson = null;
        if (updateChecker() && authStub(Operater.update)) {//授权通过时
            rjson = this.db.update();
        } else {
            nLogger.debugInfo("更新失败 原因:[" + UserSession.current().getUID() + " 无权操作！]");
        }
        reInit();
        return rjson != null;
    }

    //覆盖父类更新全部方法

    public long updateAll() {
        long rl;
        List<List<Object>> newCond = authFilter(Operater.update);
        //附加条件到update上
        this.db.and().groupCondition(newCond);
        rl = this.db.updateAll();
        reInit();
        return rl;
    }


    @Deprecated
    public JSONObject delete() {
        nLogger.logInfo("delete方法禁止使用");
        return null;
    }

    //覆盖父类删除方法
    public boolean deleteEx() {
        JSONObject robj = null;
        if (authStub(Operater.delete)) {//授权通过时
            robj = this.db.delete();
        } else {
            nLogger.debugInfo("删除失败 原因:[" + UserSession.current().getUID() + " 无权操作！]");
        }
        reInit();
        return robj != null;
    }

    //覆盖父类删除全部方法

    public long deleteAll() {
        long rl;
        List<List<Object>> newCond = authFilter(Operater.delete);
        //附加条件到update上
        this.db.and().groupCondition(newCond);
        rl = this.db.deleteAll();
        reInit();
        return rl;
    }

    private JSONObject _find() {
        return this.db.find();
    }

    private void reInit() {
        if (!isDirty) {
            useField = false;
            dataCache = null;
            tempAdmin = false;
            cacheField = null;
            if (checker != null) {
                checker.resumeCheck();
            }
        }
    }

    // 覆盖父类查询全部
    public JSONArray select() {
        JSONArray robj;
        if (!useField && checker != null && !tempAdmin) {//没有使用字段,开启了模型检查,非临时管理模式
            mask(checker.getMaskFields());
        }
        if (!tempAdmin) {
            List<List<Object>> newCond = authFilter(Operater.read);
            this.db.and().groupCondition(newCond);
        }

        robj = this.db.select();
        reInit();
        return robj;
    }

    //普通分页查询

    public JSONArray page(int pageidx, int pagemax) {
        return page(pageidx, pagemax, null);
    }

    //优化后分页查询
    /*
     * lastObj	上一次查询结果的最大主键值
     * */
    public JSONArray page(int pageIdx, int pageMax, Object lastObj) {
        JSONArray rObj;
        if (!useField && checker != null && !tempAdmin) {//没有使用字段,开启了模型检查,非临时管理模式
            mask(checker.getMaskFields());
        }

        if (pkField != null && lastObj != null) {
            this.db.gte(pkField, lastObj);
        }

        if (!tempAdmin) {
            List<List<Object>> newCond = authFilter(Operater.read);
            this.db.and().groupCondition(newCond);
        }

        rObj = this.db.page(pageIdx, pageMax);
        reInit();
        return rObj;
    }

    protected FormHelper getChecker() {
        return checker;
    }

    protected JSONObject getDataCache() {
        return dataCache;
    }

    //当前数据是否存在
    public boolean isExisting() {
        JSONObject rs = null;
        if (pkField != null) {
            rs = this.db.field(pkField).find();
        }
        reInit();
        return !JSONObject.isInvalided(rs);
    }

    //数据存储
    public GrapeDbLayerModel data(JSONObject obj) {
        dataCache = obj;
        this.db.data(obj);
        return this;
    }


    public GrapeDbLayerModel data(String str) {
        JSONObject json = JSONObject.toJSON(str);
        if (json == null) {
            nLogger.logInfo("参数:" + str + "->不是有效JSON格式字符串");
        }
        this.db.data(json);
        return this;
    }

    /**
     * 返回字段数据检查时最后一个出错的字段名称
     *
     * @return
     */
    public String getLastErrorField() {
        String rString = null;
        if (checker != null) {
            rString = checker.getlastErrorName();
        }
        return rString;
    }


    public int pageMax(int max) {
        return this.db.pageMax(max);
    }

    public List<String> getTables() {
        List<String> nTable = new ArrayList<>();
        int appid = HttpContext.current().appid();
        if (appid > 0) {
            String appidStr = String.valueOf(appid);
            List<String> tables = this.db.getAllTables();
            if (tables.size() > 0) {
                for (String tableName : tables) {
                    String[] nodeName = tableName.split("_");
                    if (nodeName[nodeName.length - 1].equals(appidStr)) {
                        nTable.add(tableName);
                    }
                }
            }
        }
        return nTable;
    }

    public JSONArray selectByCache(int second) {
        return this.db.selectByCache(second);
    }

    public void invaildCache() {
        this.db.invaildCache();
    }

    public void Close() {
        this.db.Close();
    }

    public void addConstantCond(String fieldName, Object CondValue) {
        this.db.addConstantCond(fieldName, CondValue);
    }

    public JSONObject findByCache(int second) {
        return this.db.findByCache(second);
    }

    public JSONObject findByCache() {
        return this.db.findByCache();
    }

    public GrapeDbLayerModel and() {
        this.db.and();
        return this;
    }

    public GrapeDbLayerModel or() {
        this.db.or();
        return this;
    }

    public boolean nullCondition() {
        return this.db.nullCondition();
    }

    public GrapeDbLayerModel where(JSONArray condArray) {
        this.db.where(condArray);
        return this;
    }

    public GrapeDbLayerModel groupCondition(List<List<Object>> conds) {
        this.db.groupCondition(conds);
        return this;
    }

    public GrapeDbLayerModel groupWhere(JSONArray conds) {
        this.db.groupWhere(conds);
        return this;
    }

    public GrapeDbLayerModel eq(String field, Object value) {
        this.db.eq(field, value);
        return this;
    }

    public GrapeDbLayerModel ne(String field, Object value) {
        this.db.ne(field, value);
        return this;
    }

    public GrapeDbLayerModel gt(String field, Object value) {
        this.db.gt(field, value);
        return this;
    }

    public GrapeDbLayerModel lt(String field, Object value) {
        this.db.lt(field, value);
        return this;
    }

    public GrapeDbLayerModel gte(String field, Object value) {
        this.db.gte(field, value);
        return this;
    }

    public GrapeDbLayerModel lte(String field, Object value) {
        this.db.lte(field, value);
        return this;
    }

    public GrapeDbLayerModel like(String field, Object value) {
        this.db.like(field, value);
        return this;
    }

    public GrapeDbLayerModel field() {
        this.db.field();
        return this;
    }

    public GrapeDbLayerModel field(String fieldString) {
        this.db.field(fieldString);
        return this;
    }

    public GrapeDbLayerModel mask(String fieldString) {
        this.db.mask(fieldString);
        return this;
    }

    public GrapeDbLayerModel form(String _formName) {
        this.db.form(_formName);
        return this;
    }

    public GrapeDbLayerModel skip(int no) {
        this.db.skip(no);
        return this;
    }

    public GrapeDbLayerModel limit(int no) {
        this.db.limit(no);
        return this;
    }

    public GrapeDbLayerModel findOne() {
        this.db.findOne();
        return this;
    }

    public List<Object> insert() {
        return this.db.insert();
    }

    public JSONObject inc(String fieldName) {
        return this.db.inc(fieldName);
    }

    public JSONObject dec(String fieldName) {
        return this.db.dec(fieldName);
    }

    public JSONObject add(String fieldName, long num) {
        return this.db.add(fieldName, num);
    }

    public JSONObject sub(String fieldName, long num) {
        return this.db.sub(fieldName, num);
    }

    public String getConditionString() {
        return this.db.getConditionString();
    }

    public JSONArray distinct(String fieldName) {
        return this.db.distinct(fieldName);
    }

    public JSONArray page(int pageidx, int pagemax, int lastid, String fastfield) {
        return this.db.page(pageidx, pagemax, lastid, fastfield);
    }

    public GrapeDbLayerModel count(String groupbyString) {
        this.db.count(groupbyString);
        return this;
    }

    public GrapeDbLayerModel max(String groupbyString) {
        this.db.max(groupbyString);
        return this;
    }

    public GrapeDbLayerModel min(String groupbyString) {
        this.db.min(groupbyString);
        return this;
    }

    public GrapeDbLayerModel avg(String groupbyString) {
        this.db.avg(groupbyString);
        return this;
    }

    public GrapeDbLayerModel sum(String groupbyString) {
        this.db.sum(groupbyString);
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

    public void asyncInsert() {
        reInit();
        this.db.asyncInsert();
    }

    public GrapeDbLayerModel bind(String ownerID) {
        this.db.bind(ownerID);
        return this;
    }

    public int limit() {
        return this.db.limit();
    }

    public String getGeneratedKeys() {
        return this.db.getGeneratedKeys();
    }

    public void clear() {
        this.db.clear();
    }


    public JSONArray scan(Function<JSONArray, JSONArray> func, int max) {
        return this.db.scan(func, max);
    }

    public JSONArray scan(Function<JSONArray, JSONArray> func, int max, int synNo) {
        return this.db.scan(func, max, synNo);
    }

    public JSONObject getCond() {
        return this.db.getCond();
    }

    public GrapeDbLayerModel setCond(JSONObject conJSON) {
        this.db.setCond(conJSON);
        return this;
    }

    public List<String> getAllTables() {
        return this.db.getAllTables();
    }

    public static class GrapeDBMode {
        public static final int safeMode = 0;//对数据做过滤，尽可能保证操作可以完成
        public static final int checkMode = 1;//对数据做检查，尽可能保证数据安全性，一致性
    }

    public static class fieldState {
        public String[] cacheField;
    }
}
