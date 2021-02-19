package common.java.Cache;

interface InterfaceCache {
    String get(String objectName);

    boolean setExpire(String objectName, int expire);

    String set(String objectName, Object objectValue);

    String set(String objectName, int expire, Object objectValue);

    boolean setNX(String objectName, Object objectValue);

    String getSet(String objectName, Object objectValue);

    String getSet(String objectName, int expire, Object objectValue);

    long inc(String objectName);

    long incBy(String objectName, long num);

    long dec(String objectName);

    long decBy(String objectName, long num);

    long delete(String objectName);
}
