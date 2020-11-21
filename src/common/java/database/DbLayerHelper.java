package common.java.database;

import common.java.check.FormHelper;
import common.java.string.StringHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DbLayerHelper extends DbLayer {

    //private DbLayer serviceServer;
    private String pkfield;
    private FormHelper checker;
    private boolean useChecker;

    public DbLayerHelper() {
        super();
    }

    public DbLayerHelper(String fromName) {
        super();
        _initload(fromName);
    }

    public DbLayerHelper(String configNodeName, String fromName) {
        super(configNodeName);
        _initload(fromName);
    }

    void reint() {

    }

    private void _initload(String fromName) {
        form(fromName);
        pkfield = getGeneratedKeys();
        checker = new FormHelper();
        useChecker = false;
    }

    /**
     * 获得校验器
     *
     * @return
     */
    public FormHelper getChecker() {
        return checker;
    }

    public void setChecker(FormHelper newChekcer) {
        checker = newChekcer;
        useChecker = true;
    }

    /**
     * 获取主键
     *
     * @return 主键字段
     */
    public String getpk() {
        return pkfield;
    }

    /**
     * 显示字段
     *
     * @param field
     * @return
     */
    public DbLayerHelper field(String[] field) {
        String newfield;
        if (useChecker) {
            newfield = checker.filterMask(field);
        }
        newfield = useChecker ? checker.filterMask(field) : StringHelper.join(field, ",");
        field(newfield);
        return this;
    }

    public DbLayerHelper mask(String[] maskfield) {
        mask(StringHelper.join(maskfield, ","));
        return this;
    }

    /**
     * 插入数据
     *
     * @param JSONString JSON字符串
     * @return 唯一标识符
     */
    public String insert(String JSONString) {
        JSONObject json = JSONObject.toJSON(JSONString);
        return insert(json);
    }

    /**
     * 插入数据
     *
     * @param JSONObject
     * @return 唯一标识符
     */
    public String insert(JSONObject JSONObject) {
        Object tObject = null;
        JSONObject _JSONObject = JSONObject;
        String rString = "";
        if (_JSONObject != null) {
            if (!useChecker || checker.checkTable(_JSONObject, true)) {
                tObject = data(_JSONObject).insertOnce();
                if (tObject != null) {
                    rString = tObject.toString();//自增值
                }
            }
        }
        reint();
        return rString;
    }

    /**
     * 修改数据
     *
     * @param id           唯一标识符   （mysql，需设置主键）
     * @param configString 存入需要修改的数据的json字符串
     * @return true or false
     */
    public boolean update(Object id, String configString) {
        return update(id, JSONObject.toJSON(configString));
    }

    public String update(String id, String configString) {
        return String.valueOf(update((Object) id, configString));
    }

    /**
     * 修改数据
     *
     * @param id         唯一标识符   （mysql，需设置主键）
     * @param JSONObject JSONObject类型数据
     * @return
     */
    public boolean update(Object id, JSONObject JSONObject) {
        boolean rb = false;
        boolean auth = true;
        if (pkfield != null && JSONObject != null) {
            if (useChecker) {
                if (checker.checkTable(JSONObject, false)) {
                    checker.filterProtect(JSONObject);
                } else {
                    auth = false;
                }
            }
            if (auth) {
                rb = (eq(pkfield, id).data(JSONObject).update() != null);
            }
        }
        reint();
        return rb;
    }

    /**
     * 删除数据
     *
     * @param id 唯一标识符   （mysql，需设置主键）
     * @return
     */
    public boolean delete(Object id) {
        boolean rb = false;
        JSONObject rObject;
        if (pkfield != null) {
            rObject = (id != null) ? eq(pkfield, id).delete() : delete();
            if (rObject != null) {
                rb = true;
            }
        }
        reint();
        return rb;
    }

    /**
     * 删除数据
     *
     * @param id 唯一标识符   （mysql，需设置主键）
     * @return
     */
    public String delete(String id) {
        return String.valueOf(delete((Object) id));
    }

    /**
     * 查询数据
     *
     * @param id 唯一标识符
     * @return
     */
    public String find(Object id) {
        String rString = "";
        JSONObject jObject;
        if (pkfield != null) {
            jObject = (id != null && id.toString() != "") ? eq(pkfield, id).find() : find();
            if (jObject != null) {
                rString = safeObject(jObject);
            }
        }
        reint();
        return rString;
    }

    public String find(String id) {
        String rString = "";
        JSONObject jObject;
        if (pkfield != null) {
            jObject = (id != null && id != "") ? eq(pkfield, id).find() : find();
            if (jObject != null) {
                rString = safeObject(jObject);
            }
        }
        reint();
        return rString;
    }

    public String select(String id) {
        String rString = "";
        JSONArray jArray;


        if (pkfield != null) {
            jArray = (id != null && id != "") ? eq(pkfield, id).select() : select();
            if (jArray != null) {
                rString = safeResultSet(jArray);
            }
        }
        reint();
        return rString;
    }

    /**
     * 统计数据
     *
     * @return
     */
    public String _count() {
        return String.valueOf(super.count());
    }

    /**
     * 普通分页
     *
     * @param idx 当前页码
     * @param max 每页最大数据数量
     * @return
     */
    public String _page(int idx, int max) {
        return safeResultSet(page(idx, max));
    }

    /**
     * 普通分页
     *
     * @param idx 当前页码
     * @return
     */
    public String _page(int idx) {
        return _page(idx, 20);
    }


    /**
     * 优化分页
     *
     * @param idx
     * @param max
     * @param lastIdx
     * @return
     */
    public String _page(int idx, int max, String lastIdx) {
        Object val = null;
        try {
            long testLong = Long.valueOf(lastIdx);
            val = testLong;
        } catch (Exception e) {
            val = lastIdx;
        }
        return safeResultSet(and().gte(pkfield, val).page(idx, max));
    }

    /**
     * 优化分页
     *
     * @param idx     当前页码
     * @param lastIdx
     * @return
     */
    public String _page(int idx, String lastIdx) {
        return _page(idx, 20, lastIdx);
    }

    public String _page_max(int max) {
        return String.valueOf(pageMax(max));
    }

    private String safeObject(JSONObject jObject) {
        String rs;
        if (jObject != null) {
            rs = jObject.toJSONString();
        } else {
            rs = null;
        }
        return rs;
    }

    private String safeResultSet(JSONArray jArray) {
        reint();
        return jArray == null ? null : jArray.toJSONString();
    }

    public List<String> getTables(int appid) {
        String appidStr = String.valueOf(appid);
        List<String> tables = getAllTables();
        List<String> nTable = new ArrayList<>();
        if (tables.size() > 0) {
            for (String tableName : tables) {
                String[] nodeName = tableName.split("_");
                if (nodeName[nodeName.length - 1].equals(appidStr)) {
                    nTable.add(tableName);
                }
            }
        }
        return nTable;
    }

}
//{}
