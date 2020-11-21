package common.java.authority;

public class PermissionsPowerDef {
    public final static String tableName = "Permissions";//表名称

    public final static String createMode = "cMode";//创建权限描述字段
    public final static String statisticsMode = "sMode";//统计分析权限描述字段
    public final static String readMode = "rMode";//查询权限描述字段
    public final static String updateMode = "uMode";//更新权限描述字段
    public final static String deleteMode = "dMode";//删除权限描述字段
    public final static String createValue = "cValue";//创建权限值字段
    public final static String statisticsValue = "sValue";//统计分析权限值字段
    public final static String readValue = "rValue";//查询权限值字段
    public final static String updateValue = "uValue";//更新权限值字段
    public final static String deleteValue = "dValue";//删除权限值字段


	/*
	public final static String permissionsType = "chkType";//权限类型
	public final static String permissionsValue = "chkCond";//权限值
	*/

    /**
     * gsc-tree 不可少字段
     */
    public final static String powerValField = "_weight";
    public final static String fatherIDField = "_father";        //父ID字段名
    public final static String visableField = "_visible";
    public final static String deleteField = "_delete";
    public final static String levelField = "_level";
    public final static String sortField = "_sort";

    /**
     * 内存生成树不可少字段
     */
    public final static String childrenData = "_child";

    /**
     * 用户数据表不可少字段
     */
    public final static String adminField = "_type";                //用户类型字段名
    public final static String userField = "_name";                //用户名字段名

}

