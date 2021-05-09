package common.java.OAuth;

import common.java.Cache.Cache;
import common.java.HttpServer.HttpContext;
import common.java.String.StringHelper;

public class oauthApi {
    public static oauthApi getInstance() {
        return new oauthApi();
    }

    private static String build_api_name(String serviceName, String className, String actionName) {
        return serviceName + "@" + className + "@" + actionName;
    }

    /**
     * @param api_name 接口方法名称
     * @apiNote 获得接口一次性授权码(集群内部使用)，1分钟有效
     */
    public String getApiToken(String api_name) {
        Cache c = Cache.getInstance();
        String token_key;
        do {
            token_key = "api_token_" + StringHelper.createRandomCode(64);
        } while (c.get(token_key) != null);
        c.set(token_key, 60, api_name);
        return token_key;
    }

    /**
     * @param serviceName 服务名称
     * @param className   类名称
     * @param actionName  方法名称
     * @apiNote 获得接口一次性授权码(公开使用)，10分钟有效
     */
    public void getApiTokenService(String serviceName, String className, String actionName) {
        Cache c = Cache.getInstance();
        String token_key;
        do {
            token_key = "api_token_" + StringHelper.createRandomCode(8);
        } while (c.get(token_key) != null);
        c.set(token_key, 600, build_api_name(serviceName, className, actionName));
    }

    /**
     * 验证接口和权限
     */
    public boolean checkApiToken() {
        // 通过上下文获得内容
        HttpContext header = HttpContext.current();
        String token_key = header.token();
        if (StringHelper.isInvalided(token_key)) {
            return false;
        }
        String api_name = build_api_name(header.serviceName(), header.className(), header.actionName());
        // 验证授权
        Cache c = Cache.getInstance();
        Object ca = c.get(token_key);
        if (ca == null) {
            return false;
        }
        return ca.equals(api_name);
    }
}
