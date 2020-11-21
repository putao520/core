package common.java.Reflect;

@FunctionalInterface
public interface FilterCallback {
    void run(String funcName, Object[] parameter);
}
