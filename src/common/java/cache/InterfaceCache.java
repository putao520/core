package common.java.cache;

interface InterfaceCache {
    String get(String objectName);

    boolean setExpire(String objectName, int expire);

    String set(String objectName, Object objectValue);

    String set(String objectName, int expire, Object objectValue);

    boolean setNX(String objectName, Object objectValue);

    String getset(String objectName, Object objectValue);

    String getset(String objectName, int expire, Object objectValue);

    long inc(String objectName);

    long incby(String objectName, long num);

    long dec(String objectName);

    long decby(String objectName, long num);

    long delete(String objectName);
}
