package common.java.database;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.mongodb.BasicDBObject;
import common.java.nlogger.nlogger;
import common.java.number.NumberHelper;
import common.java.string.StringHelper;
import common.java.time.TimeHelper;
import common.java.worker.Worker;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.Reader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Oracle {
    /**
     *
     */
    private static final HashMap<String, DruidDataSource> DataSource;


    static {
        DataSource = new HashMap<>();
    }

    private final String _configString;
    private final HashMap<String, Object> constantConds;
    private DruidDataSource dataSource;//  = new DruidDataSource();
    //声明线程共享变量
    private boolean conditiobLogicAnd;
    private int skipNo;
    private int limitNo;
    private BasicDBObject sortBSON;
    private boolean _count;
    private boolean _max;
    private boolean _min;
    private boolean _sum;
    private boolean _avg;
    private boolean _distinct;
    private boolean _atom;
    private List<List<Object>> conditionJSON;    //条件
    private List<List<Object>> conditionJSON_backup;    //条件
    private List<JSONObject> dataJSON;
    private List<String> fieldList;
    private String fastfieldString;
    private String groupbyfield = "";
    private String formName;
    private String ownid;
    private HashMap<String, String> baseTable;        //基本表信息,准备用
    private HashMap<String, Boolean> tableState;    //派生表状态，加速用
    private List<String> tableFields;                    //表字段结构
    private boolean isDirty;

    //配置说明，参考官方网址
    //http://blog.163.com/hongwei_benbear/blog/static/1183952912013518405588/
    public Oracle(String configString) {
        isDirty = false;
        formName = "";
        _configString = configString;
        constantConds = new HashMap<>();
        initsql();
        reinit();
    }

    public static String TransactSQLInjection(String str) {
        //System.out.println(str);
        return str;//.replaceAll(".*(';|).*", " ");}
    }

    /***获取当前线程上的连接开启事务*/
    public void startTransaction() {
        DruidPooledConnection conn = getNewConnection();
        if (conn == null) {//如果连接为空
            //container.set(conn);//将此连接放在当前线程上
            nlogger.logInfo(Thread.currentThread().getName() + "空连接从dataSource获取连接");
        } else {
            nlogger.logInfo(Thread.currentThread().getName() + "从缓存中获取连接");
        }
        try {
            Objects.requireNonNull(conn).setAutoCommit(false);//开启事务
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {
            _Close(conn);
        }
    }

    private void initsql() {
        JSONObject obj;
        String user;
        String password;
        String databaseName;
        boolean useunicode;
        boolean useSSL;
        String charName;
        String sid;
        int initSize;
        int minIdle;
        int maxWait;
        int maxActive;
        String validAtionQuery = "select 1";
        boolean testOnBorrow = true;
        boolean testWhileIdle = true;
        boolean poolpreparedStatements;
        try {
            obj = JSONObject.toJSON(_configString);
            user = obj.getString("user");
            password = obj.getString("password");
            sid = obj.getString("sid");
            databaseName = obj.getString("database");
            // charName = obj.getString("characterEncoding");
            // useunicode = obj.getBoolean("useUnicode");
            // useSSL = obj.getBoolean("useSSL");
            // jdbc:oracle:thin:59.203.206.51:1521
            String url = "oracle:thin:@" + obj.getString("host") + ":" + sid;
            initSize = obj.getInt("initsize");
            minIdle = obj.getInt("minidle");
            maxWait = obj.getInt("maxwait");
            maxActive = obj.getInt("maxactive");
            poolpreparedStatements = obj.getBoolean("poolpreparedstatements");

            DruidDataSource ds = DataSource.getOrDefault(_configString, null);
            if (ds == null || ds.isClosed() || !ds.isEnable()) {
                ds = new DruidDataSource();

                if (!user.equals("") && !password.equals("")) {
                    ds.setUsername(user);
                    ds.setPassword(password);
                    ds.setInitialSize(initSize);
                    ds.setMaxActive(maxActive);
                    ds.setMinIdle(minIdle);
                    ds.setMaxWait(maxWait);
                    // ds.setValidationQuery(validAtionQuery);
                    ds.setTestOnBorrow(false);
                    ds.setTestWhileIdle(true);
                    ds.setPoolPreparedStatements(poolpreparedStatements);
                    ds.setUrl("jdbc:" + url);
                    ds.setLoginTimeout(5);
                    ds.setQueryTimeout(7200);
                    ds.setKeepAlive(true);
                    ds.setAsyncInit(true);
                    ds.setKillWhenSocketReadTimeout(true);
                    ds.setRemoveAbandoned(true);
                    ds.setRemoveAbandonedTimeout(7200);
                    ds.setTimeBetweenEvictionRunsMillis(90000);
                    ds.setMinEvictableIdleTimeMillis(1800000);

                }
                DataSource.put(_configString, ds);
            }
            //nlogger.logInfo("[SQL]nowCnt:" + ds.getActiveCount() + " closedCnt:" + ds.getCloseCount() + " connectCnt:" + ds.getConnectCount() + " connectErrorCnt:" + ds.getConnectErrorCount());
            dataSource = ds;
        } catch (Exception e) {
            nlogger.logInfo(e, "Sql server Config node error!");
            nlogger.logInfo("Config:" + _configString);
        }
        tableFields = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public JSONArray scan(Function<JSONArray, JSONArray> func, int max) {
        if (func == null) {
            nlogger.logInfo("scan 过滤函数不存在");
        }
        if (max <= 0) {
            nlogger.logInfo("scan 每页最大值不能小于等于0");
        }

        try {
            int maxCount = (int) dirty().count();
            int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
            JSONArray jsonArray, tempResult;
            tempResult = new JSONArray();
            for (int index = 1; index <= pageNO; index++) {
                jsonArray = dirty().page(index, max);
                assert func != null;
                jsonArray = func.apply(jsonArray);
                if (jsonArray != null) {
                    tempResult.addAll(jsonArray);
                }
            }
            return tempResult;
        } finally {
            reinit();
        }
    }

    public String getConditionString() {
        return conditionJSON.toString();
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
            nlogger.logInfo("scan 过滤函数不存在");
        }
        if (max <= 0) {
            nlogger.logInfo("scan 每页最大值不能小于等于0");
            max = 1;
        }
        if (synNo <= 0) {
            nlogger.logInfo("scan 同步执行不能小于等于0");
        }

        int index;
        long rl = dirty().count();
        int maxCount = (int) rl;
        int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
        ConcurrentHashMap<Integer, JSONArray> tempResult;
        tempResult = new ConcurrentHashMap<>();
        // ExecutorService es = Executors.newVirtualThreadExecutor();
        ExecutorService es = Executors.newCachedThreadPool();
        List<List<Object>> condJSON = getCond();
        String _formName = getform();
        try {

            for (index = 1; index <= pageNO; index++) {
                final int _index = index;
                final int _max = max;
                es.execute(() -> {
                    try {
                        JSONArray jsonArray;
                        Oracle db = new Oracle(_configString);
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


    //public Connection getNewConnection(){
    public DruidPooledConnection getNewConnection() {
        //return pool;
        DruidPooledConnection con = null;
        try {
            con = dataSource.getConnection();
        } catch (SQLException e) {
            nlogger.logInfo(e);
        }
        return con;
    }

    private void _Close(DruidPooledConnection conn) {
        try {
            if (conn != null) {//如果连接为空
                //conn.close();
                conn.recycle();
            }
        } catch (SQLException e) {
            nlogger.logInfo(e);
        }
    }

    /**
     * 获取数据连接
     *
     * @return
     */
    //提交事务
    public void commit() {
        DruidPooledConnection conn = getNewConnection();
        try {
            if (null != conn) {
                conn.commit();//提交事务
                // nlogger.logInfo(Thread.currentThread().getName()+"事务已经提交......");
            }
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {
            _Close(conn);
        }
    }

    /***回滚事务*/
    public void rollback() {
        DruidPooledConnection conn = getNewConnection();
        try {
            if (conn != null) {
                conn.rollback();//回滚事务
            }
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {
            _Close(conn);
        }
    }

    /***关闭连接*/
    private void reinit() {
        if (isDirty) {//脏执行下不重置
            isDirty = false;
            return;
        }
        conditionJSON_backup = new ArrayList<>();
        conditionJSON = new ArrayList<>();
        conditiobLogicAnd = true;
        fieldList = new ArrayList<>();
        sortBSON = new BasicDBObject();//默认_id排序
        dataJSON = new ArrayList<>();
        BasicDBObject updateJSON = new BasicDBObject();
        limitNo = 0;
        skipNo = 0;
        fastfieldString = "*";
        _count = false;
        _max = false;
        _min = false;
        _sum = false;
        _avg = false;

        _distinct = false;
        _atom = false;

        ownid = null;

        and();

        tableState = new HashMap<>();
        baseTable = new HashMap<>();
        for (String _key : constantConds.keySet()) {//补充条件
            eq(_key, constantConds.get(_key));
        }

    }

    /**
     * 获得主键，并且构造表结构
     *
     * @return
     */
    public String getGeneratedKeys() {
        String pkName = "";
        if (tableFields.size() < 1) {
            DruidPooledConnection conn = getNewConnection();
            try {
                java.sql.DatabaseMetaData DBM = conn.getMetaData();
                //ResultSet tableRet = DBM.getTables(null, "%", getform(),new String[]{"TABLE"});
                ResultSet colRet = DBM.getColumns(null, "%", getfullform(), "%");
                int i = 0;
                while (colRet.next()) {
                    if (i == 0) {
                        pkName = colRet.getString("COLUMN_NAME");
                    }
                    i++;
                    tableFields.add(colRet.getString("COLUMN_NAME"));
                }

            } catch (SQLException e) {
                // TODO Auto-generated catch block
                nlogger.logInfo(e);
            } finally {
                _Close(conn);
            }
        } else {
            pkName = tableFields.get(0);
        }
        return pkName;
    }

    public void addConstantCond(String fieldName, Object CondValue) {
        and();
        constantConds.put(fieldName, CondValue);
        eq(fieldName, CondValue);//载入的时候填入条件
    }

    public Oracle and() {
        conditiobLogicAnd = true;
        return this;
    }

    public Oracle or() {
        conditiobLogicAnd = false;
        return this;
    }

    public Oracle eq(String field, Object value) {//One Condition
        addCondition(field, value, "=");
        return this;
    }

    public Oracle ne(String field, Object value) {//One Condition
        addCondition(field, value, "!=");
        return this;
    }

    public Oracle gt(String field, Object value) {//One Condition
        addCondition(field, value, ">");
        return this;
    }

    public Oracle lt(String field, Object value) {//One Condition
        addCondition(field, value, "<");
        return this;
    }

    public Oracle gte(String field, Object value) {//One Condition
        addCondition(field, value, ">=");
        return this;
    }

    public Oracle lte(String field, Object value) {//One Condition
        addCondition(field, value, "<=");
        return this;
    }

    public Oracle like(String field, Object value) {//One Condition
        addCondition(field, "%" + value.toString() + "%", "like");
        return this;
    }

    /**
     * 判断条件是否为空
     *
     * @return
     */
    public boolean nullCondition() {
        return conditionJSON.size() == 0;
    }

    public Oracle where(List<List<Object>> condArray) {
        conditionJSON.addAll(condArray);
        return this;
    }

    public Oracle where(JSONArray condArray) {
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
                    nlogger.errorInfo(condArray.toJSONString() + " ->输入的 条件对象无效");
                }
            }
            return this;
        }
        return null;
    }

    private <T> void addCondition(String field, T value, String logic) {
        addCondition(field, value, logic, conditiobLogicAnd);
    }

    private <T> void addCondition(String field, T value, String logic, boolean link_logic) {
        List<Object> nJSONArray;
        if (value != null && !value.toString().equals("")) {
            nJSONArray = new ArrayList<>();
            nJSONArray.add(link_logic ? "and" : "or");
            nJSONArray.add(field);
            if (logic.equals("==")) {
                logic = "=";
            }
            nJSONArray.add(logic);
            nJSONArray.add(value);
            conditionJSON.add(nJSONArray);
        }
    }

    public Oracle data(String jsonString) {
        data(JSONObject.toJSON(jsonString));

        return this;
    }

    public Oracle data(JSONObject doc) {
        dataJSON.add(doc);
        return this;
    }

    public Oracle field(String fieldString) {
        fastfieldString = fieldString;
        field(fieldString.split(","));
        return this;
    }

    public Oracle field() {
        fastfieldString = "*";
        fieldList = new ArrayList<>();
        return this;
    }

    private String stringList2string(String[] _fieldList) {
        StringBuilder rs = new StringBuilder();
        if (_fieldList.length > 0) {
            for (String s : _fieldList) {
                rs.append("`").append(s).append("`,");
            }
            rs = new StringBuilder(StringHelper.build(rs.toString()).removeTrailingFrom().toString());
        }
        return rs.toString();
    }

    private String stringList2string(List<String> _fieldList) {
        StringBuilder rs = new StringBuilder();
        if (_fieldList.size() > 0) {
            for (String val : _fieldList) {
                rs.append("`").append(val).append("`,");
            }
            rs = new StringBuilder(StringHelper.build(rs.toString()).removeTrailingFrom().toString());
        }
        return rs.toString();
    }

    public Oracle field(String[] _fieldList) {
        if (fastfieldString.equals("*")) {
            fastfieldString = stringList2string(_fieldList);
        }
        fieldList = Arrays.asList(_fieldList);
        return this;
    }

    public Oracle mask(String fieldString) {
        String[] maskField = fieldString.split(",");
        return mask(maskField);
    }

    public Oracle mask(String[] _FieldList) {
        //getGeneratedKeys();
        List<String> tempField = new ArrayList<>();
        if (tableFields.size() < 1) {
            getGeneratedKeys();
        }
        if (tableFields.size() > 0) {
            tempField = tableFields;
            int l = _FieldList.length;
            int idx;
            for (String s : _FieldList) {
                idx = tempField.indexOf(s);
                if (idx >= 0) {
                    tempField.remove(idx);
                }
            }
        }
        fieldList = tempField;
        fastfieldString = stringList2string(fieldList);
        return this;
    }

    private String result2create(ResultSet rst) {
        ResultSetMetaData m;
        int columns, n;
        String fieldName, fieldType;
        StringBuilder tmpStr = null;
        try {
            m = rst.getMetaData();//获取 列信息
            columns = m.getColumnCount();
            if (columns > 0) {
                tmpStr = new StringBuilder("(");
                for (int i = 1; i <= columns; i++) {
                    fieldName = m.getColumnName(i);
                    fieldType = m.getColumnTypeName(i);
                    n = m.getColumnDisplaySize(i);
                    //fieldName += " NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";
                    switch (fieldType) {
                        case "INT UNSIGNED" -> fieldType = "INT(" + n + ") UNSIGNED";
                        case "TIMESTAMP" -> fieldType += " NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";
                        default -> fieldType = fieldType + "(" + n + ")";
                    }
                    if (m.isAutoIncrement(i)) {
                        fieldType += " auto_increment primary key";
                    }
                    //create table if not exists people(name text,age int(2),gender char(1));
                    tmpStr.append(fieldName).append(" ").append(fieldType).append(",");
                }
                tmpStr = new StringBuilder(StringHelper.build(tmpStr.toString()).removeTrailingFrom().toString() + ")");
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            nlogger.logInfo(e);
        }
        assert tmpStr != null;
        return tmpStr.toString();
    }

    private String getCreateSQL(String tableName) {
        ResultSet rs;
        Statement smt;
        String sqlString = null;
        DruidPooledConnection conn = getNewConnection();
        try {
            smt = conn.createStatement();
            String sql = "select * from " + tableName + " where rownum = 1";
            rs = smt.executeQuery(sql);
            if (rs != null) {
                sqlString = result2create(rs);
                baseTable.put(tableName, sqlString);
            }
        } catch (SQLException e) {
            nlogger.logInfo(e);
        } finally {
            _Close(conn);
        }
        return sqlString;
    }

    private boolean createTable(String tableName, String colString) {
        boolean rs = false;
        String sql;
        if (colString != null) {
            DruidPooledConnection conn = getNewConnection();
            try {
                sql = "create table if not exists " + tableName + colString + ("");
                Statement smt = conn.createStatement();
                //nlogger.logInfo(Sql);
                rs = smt.execute(sql);
                tableState.put(tableName, true);
            } catch (SQLException e) {
                nlogger.logInfo(e);
                //创建表单异常
                rs = false;
            } finally {
                _Close(conn);
            }
        }
        return rs;
    }

    private String getCreateTableColSQL() {
        JSONObject json;
        String rString = "";
        if (dataJSON.size() > 0) {
            json = dataJSON.get(0);
            SqlFieldProp fp;
            Object temp;
            StringBuilder newsql = new StringBuilder();
            for (Object key : json.keySet()) {
                temp = json.get(key);
                if (temp instanceof SqlFieldProp) {
                    fp = (SqlFieldProp) temp;
                    newsql.append(fp.build()).append(",");
                } else {
                    nlogger.logInfo("错误表字段描述");
                }
            }
            if (!newsql.toString().equals("")) {
                rString = "(" + StringHelper.build(newsql.toString()).trimFrom(',').toString() + ")";
            }
        }
        return rString;
    }

    public boolean newTable() {
        boolean rb = false;
        String createSQL = getCreateTableColSQL();
        if (!createSQL.equals("")) {
            rb = createTable(getfullform(), createSQL);
        }
        reinit();
        return rb;
    }

    public boolean newTempTable() {
        boolean rb = false;
        String createSQL = getCreateTableColSQL();
        if (!createSQL.equals("")) {
            rb = createTable(getfullform(), createSQL);
        }
        reinit();
        return rb;
    }

    /**
     * 判断表是否存在，不存在就创建
     *
     * @param
     * @return
     */
    private boolean safeTable() {
        /*
         * 1:获得基础表的字段信息
         * 2:生成创建SQL
         * 3:填入缓存
         * */
        if (ownid == null || ownid.equals("")) {
            return true;
        }
        boolean rs = true;
        String baseName = formName;
        String ownID = ownid;
        String createTableSql;
        String newTable = baseName + "_" + ownID;
        if (!tableState.containsKey(newTable)) {
            if (baseTable.containsKey(baseName)) {
                createTableSql = baseTable.get(baseName);
            } else {
                createTableSql = getCreateSQL(baseName);
            }
            rs = createTable(newTable, createTableSql);
        }
        return rs;
    }

    public Oracle form(String _formName) {
        formName = _formName;
        safeTable();
        return this;
    }

    public String getfullform() {
        return ownid == null || ownid.equals("") ? formName : formName + "_" + ownid;
    }

    public String getform() {
        return formName;
    }

    public Oracle skip(int no) {
        skipNo = no;
        limitNo += skipNo;
        return this;
    }

    public Oracle limit(int no) {
        limitNo = skipNo + no;
        return this;
    }

    public Oracle asc(String field) {
        sortBSON.put(field, "asc");
        return this;
    }

    public Oracle desc(String field) {
        sortBSON.put(field, "desc");
        return this;
    }

    public Oracle findOne() {
        _atom = true;
        return this;
    }

    public List<Object> insert() {
        ResultSet rs;
        List<Object> rList = new ArrayList<>();
        DruidPooledConnection conn = getNewConnection();
        try {
            List<String> lStrings = insertSQL();
            Statement smt = conn.createStatement();
            for (String _sql : lStrings) {
                String nsql = TransactSQLInjection(_sql);
                // nlogger.logInfo(nsql);
                if (smt.executeUpdate(nsql, Statement.RETURN_GENERATED_KEYS) > 0) {
                    rs = smt.getGeneratedKeys();
                    if (rs != null && rs.next()) {
                        rList.add(rs.getInt(1));
                    }
                }
            }
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return rList;
    }

    public void asyncInsert() {
        List<String> lStrings = insertSQL();
        try {
            Worker.submit(() -> {
                Statement smt;
                DruidPooledConnection conn = getNewConnection();
                try {
                    smt = conn.createStatement();
                    int i, l = lStrings.size();
                    for (i = 0; i < l; i++) {
                        smt.executeUpdate(TransactSQLInjection(lStrings.get(i)));
                    }
                    smt.close();
                } catch (Exception e1) {
                    nlogger.logInfo(e1);
                } finally {
                    _Close(conn);
                }
            });
        } finally {
            reinit();
        }
    }

    public Oracle clearResult() {

        return this;
    }

    public Object insertOnce() {
        String sqlString = "";
        ResultSet rs;
        Object rObject = null;
        DruidPooledConnection conn = getNewConnection();
        try {
            List<String> lStrings = insertSQL();
            //System.out.println(lStrings.toString());
            Statement smt = conn.createStatement();
            sqlString = lStrings.get(0);
            if (sqlString != null && !sqlString.equals("")) {
                String nsql = TransactSQLInjection(sqlString);
                // nlogger.logInfo(nsql);
                if (smt.executeUpdate(nsql, Statement.RETURN_GENERATED_KEYS) > 0) {
                    rObject = new JSONObject();
                    rs = smt.getGeneratedKeys();
                    if (rs != null) {
                        if (rs.next()) {
                            rObject = rs.getInt(1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            nlogger.logInfo(e, sqlString);
        } finally {
            reinit();
            _Close(conn);
        }
        return rObject;
    }

    public JSONObject update() {//atom后返回当前值再修改
        JSONObject rs;
        if (_atom) {
            try {
                rs = (JSONObject) (((JSONArray) _findex(false)).get(0));
                _update(false);
            } catch (Exception e) {
                rs = null;
            } finally {
                reinit();
            }
        } else {
            rs = _update(false) > 0 ? new JSONObject() : null;
        }
        return rs;
    }

    public long updateAll() {
        return _update(true);
    }

    private long _update(boolean isall) {//缺少特殊update政策支持,原子模式下模拟未实现
        long rs = 0;
        List<String> lStrings = updateSQL();
        Statement smt;
        DruidPooledConnection conn = getNewConnection();
        try {
            smt = conn.createStatement();
            for (String _sql : lStrings) {
                String nsql = TransactSQLInjection(_sql + (isall ? "" : " rownum = 1"));
                // nlogger.logInfo(nsql);
                rs += smt.executeUpdate(nsql);
            }
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return rs;
    }

    private List<String> insertSQL() {
        List<String> sqlList = new ArrayList<>();
        String fieldString = "", valueString = "";
        for (JSONObject _t : dataJSON) {//dataJSON可以包含多个jsonString,每一个jsonString代表一个操作
            for (Object _j : _t.keySet()) {//为每一个jsonString构造k-v insertsql
                fieldString = fieldString + "`" + _j.toString() + "`,";
                valueString = valueString + sqlvalue(_t.get(_j)) + ",";
            }
            sqlList.add("insert into " + getfullform() + "(" + StringHelper.build(fieldString).removeTrailingFrom().toString() + ")" + " values(" + StringHelper.build(valueString).removeTrailingFrom().toString() + ")");
            //sqlList.add("insert into " + formName + " values(" + StringHelper.killlast(valueString) + ")");
        }
        return sqlList;
    }

    public List<List<Object>> getCond() {
        return conditionJSON;
    }

    @SuppressWarnings("unchecked")
    public Oracle setCond(List<List<Object>> conJSON) {
        conditionJSON = conJSON;
        return this;
    }

    public Oracle groupWhere(JSONArray condArray) {
        return groupCondition(DbFilter.buildDbFilter(condArray).buildEx());
    }

    public Oracle groupCondition(List<List<Object>> conds) {
        if (conds != null && conds.size() > 0) {
            List<Object> block = new ArrayList<>();
            block.add(conditiobLogicAnd ? "and" : "or");
            block.add(conds);
            conditionJSON.add(block);
        }
        return this;
    }

    private void appendOracleConds() {
        if (conditionJSON_backup.size() > 0) {
            conditionJSON.clear();
            conditionJSON.addAll(conditionJSON_backup);
        } else {
            conditionJSON_backup.addAll(conditionJSON);
        }
        // 发现 skipNo 补充限制条件
        if (skipNo > 0) {
            gt((limitNo > 0 ? "rn" : "rownum "), skipNo);
        } else if (limitNo > 0) {
            lte("rownum ", limitNo);
        }
    }

    private String whereSQL() {//不支持自由条件，必须严格区分and和or2个组
        StringBuilder rString = new StringBuilder();
        appendOracleConds();
        if (conditionJSON.size() > 0) {
            int cnt = 0;
            for (List<Object> item : conditionJSON) {
                rString.append(whereSQL(item, cnt == 0));
                cnt++;
            }

            rString.insert(0, " where ");
        }
        return rString.toString();
    }

    private String whereSQL(List<Object> conds, boolean isfirst) {
        StringBuilder r = new StringBuilder();
        int cnt = 0;
        // 生成条件
        for (Object item : conds) {
            Object idx0 = conds.get(0);
            if (item instanceof ArrayList) {//列表对象是list
                List<Object> info = (List<Object>) item;
                r.append(whereSQL(info, cnt == 0));
            } else {
                if (conds.size() == 2) {//是条件组
                    if (idx0 instanceof String) {
                        r = new StringBuilder((isfirst ? "" : " " + idx0) + " ( " + whereSQL((List<Object>) conds.get(1), cnt == 0) + " ) ");
                    }
                    return r.toString();
                }
            }
            if (conds.size() == 4) {//是条件
                if (idx0 instanceof String) {
                    return (isfirst ? "" : " " + idx0) + " " + conds.get(1) + " " + conds.get(2) + " " + sqlvalue(conds.get(3));
                }
            }
            cnt++;
        }
        return r.toString();
    }

    private List<String> updateSQL() {
        List<String> sqlList = new ArrayList<>();
        String updateString = "";
        for (JSONObject _t : dataJSON) {//dataJSON可以包含多个jsonString,每一个jsonString代表一个操作
            for (Object _j : _t.keySet()) {//为每一个jsonString构造k-v insertsql
                updateString = updateString + _j.toString() + "=" + sqlvalue(_t.get(_j)) + ",";
            }
            sqlList.add("update " + getfullform() + " set " + StringHelper.build(updateString).removeTrailingFrom().toString() + whereSQL());
        }
        return sqlList;
    }

    public JSONObject delete() {
        //ResultSet fd;
        JSONObject rs;
        if (_atom) {
            try {
                rs = (JSONObject) (((JSONArray) _findex(false)).get(0));
                _delete(false);
            } catch (Exception e) {
                rs = null;
            } finally {
                reinit();
            }
        } else {
            rs = _delete(false) > 0 ? new JSONObject() : null;
        }
        return rs;
    }

    public long deleteAll() {
        return _delete(true);
    }

    private long _delete(boolean isall) {
        long rs = 0;
        Statement smt;
        if (conditionJSON.size() > 0 || isall) {
            DruidPooledConnection conn = getNewConnection();
            try {
                smt = conn.createStatement();
                String sql = TransactSQLInjection("delete from " + getfullform() + whereSQL() + (isall ? "" : " rownum = 1"));
                TransactSQLInjection(sql);
                rs = smt.executeUpdate(sql);
            } catch (Exception e) {
                nlogger.logInfo(e);
            } finally {
                reinit();
                _Close(conn);
            }
        }
        return rs;
    }

    public JSONObject inc(String fieldName) {
        return add(fieldName, 1);
    }

    public JSONObject dec(String fieldName) {
        return add(fieldName, -1);
    }

    public JSONObject add(String fieldName, long num) {
        data("{\"" + fieldName + "\":\"" + fieldName + (num > 0 ? "+" : "-") + Math.abs(num) + "\"}");
        findOne();//open atom mode
        return update();
    }

    public JSONObject find() {
        limit(1);
        JSONArray fd = _find(false);
        return fd == null ? null : (fd.size() > 0 ? (JSONObject) fd.get(0) : null);
    }

    public JSONArray select() {
        boolean skipMode = (skipNo > 0 && limitNo < 1);
        if (skipMode) {
            long fromCount = _count();
            if (fromCount > 0) {
                limit(Integer.parseInt(String.valueOf(fromCount)));
            }
        }
        return _find(true);
    }

    private String sortSQL() {//只有第一个有效
        String rs = "";
        if (sortBSON.size() > 0) {
            for (Object item : sortBSON.keySet()) {
                rs = " order by " + item.toString() + " " + sortBSON.get(item).toString();
                break;
            }
        }
        return rs;
    }

    private String getfullformSQL() {
        // 包含需要跳过的数据行
        if (limitNo > 0 && skipNo > 0) {
            return "(select rownum rn,a.* from " + getfullform() + " a where rownum <= " + limitNo + ")";
        } else {
            return getfullform();
        }
    }

    private Object _findex(boolean isall) {
        //ResultSet rs;
        Object rs;
        DruidPooledConnection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            if (!isall) {
                if (limitNo == 0)
                    limitNo = 1;
            }
            String sql = TransactSQLInjection("select " + fastfieldString + " from " + getfullformSQL() + whereSQL() + sortSQL());
            TransactSQLInjection(sql);
            rs = col2jsonArray(smt.executeQuery(sql));
        } catch (Exception e) {
            rs = null;
            nlogger.logInfo(e);
        }
        //这里关闭连接，结果集就没有了
        finally {
            _Close(conn);
        }
        return rs;
    }

    private JSONArray _find(boolean isall) {//不支持groupby
        try {
            return (JSONArray) _findex(isall);
        } catch (Exception e) {
            nlogger.logInfo(e);
            // TODO Auto-generated catch block
            //nlogger.logInfo(e);
        } finally {
            reinit();
        }
        return null;
    }

    public JSONArray group() {
        return group(null);
    }

    /**
     * @param groupName //groupby fieldName
     * @return
     */
    public JSONArray group(String groupName) {
        String groupSQL;
        String sql;
        String _valueName = groupbyfield == null || groupbyfield.equals("") ? groupName : groupbyfield;
        String otherfield = "";
        if (_count)
            otherfield += ", count(" + _distinctfield(_valueName) + ") as count";
        if (_sum)
            otherfield += ", sum(" + _distinctfield(_valueName) + ") as total";
        if (_max)
            otherfield += ", max(" + _distinctfield(_valueName) + ") as max";
        if (_min)
            otherfield += ", min(" + _distinctfield(_valueName) + ") as min";
        if (_avg)
            otherfield += ", avg(" + _distinctfield(_valueName) + ") as avg";
        String condString = whereSQL();
        groupSQL = groupName == null || groupName.equals("") ? "" : (condString) + " group by " + groupName;
        sql = TransactSQLInjection("select " + fastfieldString + otherfield + " from " + getfullform() + groupSQL + sortSQL());
        TransactSQLInjection(sql);
        JSONArray fd;
        Statement smt;
        DruidPooledConnection conn = getNewConnection();
        try {
            smt = conn.createStatement();
            fd = col2jsonArray(smt.executeQuery(sql));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            fd = null;
            nlogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return fd;
    }

    private String _distinctfield(String str) {
        String rs = str;
        if (_distinct) {
            rs = "DISTINCT(" + str + ")";
        }
        return rs;
    }

    public Oracle distinct() {
        _distinct = true;
        return this;
    }

    public JSONArray distinct(String fieldName) {
        boolean havefield = false;
        StringBuilder fieldString = new StringBuilder();
        DruidPooledConnection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            if (fieldList.size() > 0) {
                for (String item : fieldList) {
                    if (item.equals(fieldName)) {
                        fieldString.insert(0, "DISTINCT(" + item + "),");
                        havefield = true;
                    }
                    fieldString.append(item).append(",");
                }
            }
            if (!havefield) {
                fieldString.insert(0, "DISTINCT(" + fieldName + "),");
            }
            fieldString = new StringBuilder(StringHelper.build(fieldString.toString()).trimFrom(',').toString());
            String sql = TransactSQLInjection("select " + fieldString + " from " + getfullform() + whereSQL() + sortSQL());
            TransactSQLInjection(sql);
            return col2jsonArray(smt.executeQuery(sql));
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return null;
    }

    public JSONArray page(int pageidx, int pagemax) {//普通分页
        return skip(((pageidx - 1) * pagemax)).limit(pagemax).select();
    }

    private long _count() {
        //ResultSet rd;
        DruidPooledConnection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            String sql = TransactSQLInjection("select count(*) from " + getfullform());
            TransactSQLInjection(sql);
            return NumberHelper.number2long(Result(smt.executeQuery(sql)));
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {
            _Close(conn);
        }
        return 0;
    }

    public long count() {
        return count(false);
    }

    public long count(boolean islist) {
        DruidPooledConnection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            String sql = TransactSQLInjection("select count(*) from " + getfullform() + whereSQL());
            //TransactSQLInjection(Sql);
            return NumberHelper.number2long(Result(smt.executeQuery(sql)));
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {
            if (!islist) {
                reinit();
            }
            _Close(conn);
        }
        return 0;
    }

    public Oracle count(String groupbyString) {//某字段分组后数量
        groupbyfield = groupbyString;
        _count = true;
        return this;
    }

    public Oracle max(String groupbyString) {
        groupbyfield = groupbyString;
        _max = true;
        return this;
    }

    public Oracle min(String groupbyString) {
        groupbyfield = groupbyString;
        _min = true;
        return this;
    }

    public Oracle avg(String groupbyString) {
        groupbyfield = groupbyString;
        _avg = true;
        return this;
    }

    public Oracle sum(String groupbyString) {
        groupbyfield = groupbyString;
        _sum = true;
        return this;
    }

    private List<String> getResultCol(ResultSet rst) throws SQLException {
        ResultSetMetaData m = rst.getMetaData();//获取 列信息;
        int columns = m.getColumnCount();
        List<String> array = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            array.add(m.getColumnName(i));
        }
        return array;
    }

    private List<String> col2list(ResultSet rst) {
        List<String> tableList = new ArrayList<>();
        ResultSetMetaData m;
        try {
            m = rst.getMetaData();//获取 列信息
            int columns = m.getColumnCount();
            while (rst.next()) {
                for (int i = 1; i <= columns; i++) {
                    tableList.add(rst.getObject(i).toString());
                }
            }
        } catch (Exception e) {
            tableList = null;
            nlogger.logInfo(e);
        }
        return tableList;
    }

    private String ClobToString(Clob clob) {
        try {
            Reader is = clob.getCharacterStream();
            BufferedReader br = new BufferedReader(is);
            String s = br.readLine();
            StringBuffer sb = new StringBuffer();
            while (s != null) {// 执行循环将字符串全部取出付值给StringBuffer由StringBuffer转成STRING
                sb.append(s);
                s = br.readLine();
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private Object ConvertObj(Object o) {
        if (o instanceof Clob) {
            return ClobToString((Clob) o);
        }
        return o;
    }

    @SuppressWarnings("unchecked")
    private JSONArray col2jsonArray(ResultSet rst) {
        JSONArray rsj = new JSONArray();
        ResultSetMetaData m;
        int columns;

        //String aString;
        try {
            m = rst.getMetaData();//获取 列信息
            columns = m.getColumnCount();
            while (rst.next()) {
                JSONObject obj = new JSONObject();
                for (int i = 1; i <= columns; i++) {
                    Object tobj = null;
                    try {
                        tobj = ConvertObj(rst.getObject(i));
                    } catch (SQLException sqle) {
                        if (sqle.getSQLState().equals("S1009")) {
                            tobj = 0;
                        }
                        //System.out.println("errorNO:" + sqle.getSQLState());
                    }
                    if (tobj instanceof Timestamp) {
                        tobj = TimeHelper.build().dateTimeToTimestamp(tobj.toString());
                    }
                    obj.put(m.getColumnName(i), tobj);
                }
                rsj.add(obj);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block

            rsj = null;
            nlogger.logInfo(e);
        }
        return rsj;
    }

    private Object Result(ResultSet rst) {
        Object rs = null;
        try {
            if (rst.next()) {
                rs = rst.getObject(1);
            }
        } catch (Exception e) {
        }
        return rs;
    }

    private Object Result(ResultSet rst, String fieldName) {
        Object rs = null;
        try {
            if (rst.next()) {
                rs = rst.getObject(fieldName);
            }
        } catch (Exception e) {
        }
        return rs;
    }

    public String getformName() {
        return formName;
    }

    private String sqlvalue(Object _value) {//未考虑json字符串
        String rValue;
        String value;
        try {
            value = _value.toString();
            String[] _values = value.split(":");
            if (_values[0].equalsIgnoreCase("func")) {
                rValue = StringHelper.join(_values, ":", 1, -1);
            } else {
                rValue = StringHelper.typeString(_value, "'");
            }
            JSONObject jsontest = JSONObject.toJSON(rValue);
            if (jsontest != null) {
                rValue = "'" + rValue + "'";
            }
        } catch (Exception e) {
            rValue = StringHelper.typeString(_value, "'");
        }
        return rValue;
    }

    public Oracle bind(String ownerID) {
        ownid = ownerID == null ? "" : ownerID;
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

    public Oracle dirty() {
        isDirty = true;
        return this;
    }

    public void clear() {
        isDirty = false;
        reinit();
    }

    //获得全部表
    public List<String> getAllTables() {
        List<String> rs = null;
        DruidPooledConnection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            String sql = "show tables";
            TransactSQLInjection(sql);
            rs = col2list(smt.executeQuery(sql));
        } catch (Exception e) {
            nlogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return rs;
    }

    public static class sqlmethod {
        public final static int insert = 1;
        public final static int update = 2;
        public final static int delete = 3;
    }
}
