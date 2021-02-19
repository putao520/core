package common.java.ServiceTemplate;

import common.java.InterfaceModel.Type.ApiType;

import java.util.function.Consumer;

/**
 * 该模板默认修改数据必须在会话下
 */
public class MicroServiceManagerTemplate extends MicroServiceTemplate {
    public MicroServiceManagerTemplate(String ModelName) {
        super(ModelName);
    }

    public MicroServiceManagerTemplate(String ModelName, Consumer<MicroServiceTemplate> fn) {
        super(ModelName, fn);
    }

    @ApiType(ApiType.type.SessionApi)
    public Object insert(String json) {
        return super.insert(json);
    }

    @ApiType(ApiType.type.SessionApi)
    public int delete(String uids) {
        return super.delete(uids);
    }

    @ApiType(ApiType.type.SessionApi)
    public int deleteEx(String cond) {
        return super.deleteEx(cond);
    }

    @ApiType(ApiType.type.SessionApi)
    public int update(String uids, String json) {
        return super.update(uids, json);
    }

    @ApiType(ApiType.type.SessionApi)
    public int updateEx(String json, String cond) {
        return super.updateEx(json, cond);
    }
}
