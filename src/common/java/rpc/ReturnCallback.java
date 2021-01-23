package common.java.rpc;

@FunctionalInterface
public interface ReturnCallback {
    Object run(String funcName, Object returnValue);
}
