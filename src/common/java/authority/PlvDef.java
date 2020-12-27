package common.java.authority;


public class PlvDef {
    public static class Operater {
        public final static int read = 1;
        public final static int create = 2;
        public final static int update = 3;
        public final static int delete = 4;
        public final static int statist = 5;//统计权限
    }

    public static class UserMode {
        public final static int guess = 10;     //游客模式
        public final static int normal = 100;//普通模式
        public final static int admin = 1000;//管理模式
        public final static int root = 10000;//根模式
    }

    public static class plvType {
        public final static int powerVal = 0;        //权限值
        public final static int userOwn = 1;        //用户所有权
        public final static int groupOwn = 2;        //用户组所有权

        public final static String chkType = "chkType";//描述json类型的keyName
        public final static String chkVal = "chkCond";//描述json类型判断值

    }
}
