package common.java.Coordination;

public class ZookeeperUnit {
    public static final String byteArray2Strinf(byte[] in) {
        return new String(in);
    }

    public static final String nodeName(String fullPath) {
        String[] strArray = fullPath.split("/");
        return strArray[strArray.length - 1];
    }

    public static final String notRootPath(String rootPath, String fullPath) {
        return fullPath.replace(rootPath, "");
    }
}
