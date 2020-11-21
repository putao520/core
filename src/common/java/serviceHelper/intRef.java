package common.java.serviceHelper;

/*
 * 模拟按址方式传递int类型参数
 * */
public class intRef {
    private int value = 0;

    public intRef(int initValue) {
        value = initValue;
    }

    public void setValue(int i) {
        value = i;
    }

    public int intValue() {
        return value;
    }
}
