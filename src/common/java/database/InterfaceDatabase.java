package common.java.database;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.function.Function;

public interface InterfaceDatabase<T> {
    T addFieldOutPipe(String fieldName, Function<Object, Object> func);

    T addFieldInPipe(String fieldName, Function<Object, Object> func);

    JSONArray selectbyCache(int second);

    void InvaildCache();

    JSONObject findbyCache(int second);

    JSONObject findbyCache();

    void Close();

    void addConstantCond(String fieldName, Object CondValue);

    T and();

    T or();

    boolean nullCondition();

    T where(JSONArray condArray);

    T groupCondition(List<List<Object>> conds);

    T eq(String field, Object value);

    T ne(String field, Object value);

    T gt(String field, Object value);

    T lt(String field, Object value);

    T gte(String field, Object value);

    T lte(String field, Object value);

    T like(String field, Object value);

    T data(String jsonString);

    T data(JSONObject doc);

    T field();

    T field(String fieldString);

    T mask(String fieldString);

    T form(String _formName);

    T skip(int no);

    T limit(int no);

    T asc(String field);

    T desc(String field);

    T findOne();

    List<Object> insert();

    JSONObject update();

    long updateAll();

    JSONObject delete();

    long deleteAll();

    JSONObject inc(String fieldName);

    JSONObject dec(String fieldName);

    JSONObject add(String fieldName, long num);

    JSONObject find();

    JSONArray select();

    String condString();

    JSONArray group();

    JSONArray group(String groupName);

    JSONArray distinct(String fieldName);

    JSONArray page(int pageidx, int pagemax);

    JSONArray page(int pageidx, int pagemax, int lastid, String fastfield);

    long count();

    T count(String groupbyString);

    T max(String groupbyString);

    T min(String groupbyString);

    T avg(String groupbyString);

    T sum(String groupbyString);

    String getformName();

    String getform();

    String getfullform();

    void asyncInsert();

    Object insertOnce();

    T bind(String ownerID);

    T bind();

    T bindApp();

    int limit();

    int pageMax(int max);

    String getGeneratedKeys();

    T dirty();

    void clear();

    JSONArray scan(Function<JSONArray, JSONArray> func, int max);

    JSONArray scan(Function<JSONArray, JSONArray> func, int max, int synNo);

    JSONObject getCond();

    T setCond(JSONObject conJSON);

    List<String> getAllTables();


}
