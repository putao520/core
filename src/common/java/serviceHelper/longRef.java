package common.java.serviceHelper;

public class longRef {
    private long value = 0;

    public longRef(long initValue) {
        value = initValue;
    }

    public void setValue(long i) {
        value = i;
    }

    public long longValue() {
        return value;
    }
}
