import common.java.common.JsonValueFilter;
import org.json.gsc.JSONObject;

import java.util.function.Function;

public class test2 {
    public String genericAction(Function<Integer,String> func, String funcName){

        JSONObject a = JSONObject.putx("asd","asd");
        JSONObject b = (JSONObject)JsonValueFilter.build(a).filter();
        System.out.println(b);
        return "";
    }
}
