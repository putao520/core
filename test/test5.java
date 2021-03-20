import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Object 参数字符串为 -
 * 泛型表达方式为 type<type,type>
 * */
public class test5 {
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Method>> _ClassMethodMapped = new ConcurrentHashMap<>();

    /**
     * 获得方法对象
     * */
    private Method findMethod(Class<?> _Class, String funcName, Object ..._parameters){
        Method method = null;
        ConcurrentHashMap<String, Method> methodMap = null;
        String classHashKey = _Class.getCanonicalName();
        if( !_ClassMethodMapped.containsKey(classHashKey) ){
            methodMap = buildAllMethodMapped(_Class);
            if( methodMap != null ){
                _ClassMethodMapped.put(classHashKey, methodMap);
            }
        }
        if( methodMap == null ){
            methodMap = _ClassMethodMapped.get(classHashKey);
        }
        if( methodMap != null ){
            method = methodMap.get( buildMethodMapped(funcName, ParameterValues2TypeArray(_parameters)).hashCode() );
        }
        return method;
    }

    /**
     * 将实参转成形参对应的type[]
     * */
    private Type[] ParameterValues2TypeArray(Object ..._parameters){
        ArrayList<Type> parameterArray = new ArrayList<>();
        for(Object obj : _parameters){
            // Class<?> s = obj.getClass().getDeclaringClass().;
            // System.out.println(s.getName());
            Object d = obj.getClass().getTypeParameters();
            Class<?> e = obj.getClass().getEnclosingClass();
            parameterArray.add(new Type() {
                @Override
                public String getTypeName() {
                    return obj.getClass().getTypeName();
                }
            });
        }
        return parameterArray.toArray(new Type[parameterArray.size()]);
    }

    /**
     * 构造类方法与参数的映射表
     * */
    private ConcurrentHashMap<String, Method> buildAllMethodMapped(Class<?> _Class){
        Method[] methods = _Class.getMethods();
        ConcurrentHashMap<String, Method> tempMethods = new ConcurrentHashMap<>();
        for ( Method method : methods ){
            tempMethods.put(buildMethodMapped(method.getName(), method.getGenericParameterTypes()), method);
        }
        return tempMethods.size() > 0 ? tempMethods : null;
    }

    /**
     * 支持泛型参数的方法HASHKEY生成
     * */
    private String buildMethodMapped(String funcName,Type[] methodParameter){
        String out = funcName + "=>";
        for( Type type : methodParameter ){
            out += (GenericParameter(type) + ",");
        }
        return out;
    }

    /**
     * 支持泛型的参数hashkey计算
     * */
    private String GenericParameter(Type parameterType){
        /*
        String out = parameterType.getTypeName();
        if( parameterType instanceof ParameterizedType ){
            out += "<";
            Type[] actualTypes =((ParameterizedType) parameterType).getActualTypeArguments();
            for(Type type : actualTypes){
                out += (type.getTypeName() + ",");
            }
            out += ">";
        }
        return out;
        */
        return parameterType.getTypeName();
    }

    public void test(){
        ArrayList parameterArray = new ArrayList();
        Function<Integer, String > func = i -> "putao" + i;
        parameterArray.add( func );
        parameterArray.add( "test5" );
        Object[] objArray = parameterArray.toArray( new Object[ parameterArray.size() ] );
        Method mh = findMethod( test2.class, "genericAction", objArray );
        System.out.println( mh.getName() );
    }

    public static void main( String[] args ) {
        test5 t = new test5();
        t.test();
    }
}
