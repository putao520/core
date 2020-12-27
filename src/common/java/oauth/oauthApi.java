package common.java.oauth;

import common.java.cache.Cache;
import common.java.httpServer.HttpContext;
import common.java.string.StringHelper;

public class oauthApi {
    public static oauthApi getInstance() {
        return new oauthApi();
    }

    /**
     * @param api_name 接口方法名称
     * @apiNote 获得接口一次性授权码
     */
    public String getApiToken(String api_name) {
        Cache c = Cache.getInstance();
        String token_key;
        do {
            token_key = "api_token_" + StringHelper.createRandomCode(64);
        } while (c.get(token_key) != null);
        c.set(token_key, api_name, 60);
        return token_key;
    }

    /**
     * 验证接口和权限
     */
    public boolean checkApiToken() {
        // 通过上下文获得内容
        HttpContext header = HttpContext.current();
        String token_key = header.token();
        if (StringHelper.invaildString(token_key)) {
            return false;
        }
        String api_name = header.actionName();
        // 验证授权
        Cache c = Cache.getInstance();
        String ca = c.get(token_key);
        if (ca == null) {
            return false;
        }
        return ca.equals(api_name);
    }
}
