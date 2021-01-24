package common.java.rpc;

@FunctionalInterface
public interface FilterCallback {
    FilterReturn run(String funcName, Object[] parameter);
}
