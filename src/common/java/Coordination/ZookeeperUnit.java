package common.java.Coordination;

public class ZookeeperUnit {
    public static String byteArray2String(byte[] in) {
        return new String(in);
    }

    public static String nodeName(String fullPath) {
        String[] strArray = fullPath.split("/");
        return strArray[strArray.length - 1];
    }

    public static String notRootPath(String rootPath, String fullPath) {
        return fullPath.replace(rootPath, "");
    }
}
