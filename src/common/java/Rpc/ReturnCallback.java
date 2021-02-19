package common.java.Rpc;

@FunctionalInterface
public interface ReturnCallback {
    Object run(String funcName, Object returnValue);
}
