package common.java.rpc;

@FunctionalInterface
public interface FilterCallback {
    Boolean run(String funcName, Object[] parameter);
}
