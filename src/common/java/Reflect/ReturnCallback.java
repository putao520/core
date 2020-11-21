package common.java.Reflect;

@FunctionalInterface
public interface ReturnCallback {
    void run(String funcName, Object[] parameter, Object returnValue);
}
