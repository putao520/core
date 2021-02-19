package common.java.Session;

import common.java.Apps.AppContext;
import common.java.Apps.ModelServiceConfig;
import common.java.Authority.PermissionsPowerDef;
import common.java.Cache.CacheHelper;
import common.java.HttpServer.HttpContext;
import common.java.Number.NumberHelper;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;
import org.json.simple.JSONObject;

import java.util.Objects;
import java.util.UUID;

public class Session {
    private static final int sessiontime = 86400;
    private CacheHelper cacher;
    private JSONObject sessionInfo;    //会话id控制
    private String uid;                //当前操作的用户名
    private String sid;                //当前操作的会话ID
    private String gid;                //当前操作的用户组ID
    private int appid;                //当前会话所属APPID
    private int expireTime;

    private Session() {
        cacher = getCacher();
        String sid = getRequestSID();
        this.expireTime = 1800;
        updateUserInfo(sid);
    }

    //绑定会话
    private Session(String sid) {
        init(sid, -1);
    }

    private Session(String sid, int expireTime) {
        init(sid, expireTime);
    }

    public static Session build() {
        return new Session();
    }

    public static Session build(String sid) {
        return new Session(sid);
    }

    public static Session build(String sid, int expireTime) {
        return new Session(sid, expireTime);
    }

    private static CacheHelper getCacher() {
        ModelServiceConfig info = AppContext.current().config();
        if (info == null) {
            return null;
        }
        String appCache = info.cache();
        /*
        if (appCache == null) {
            nLogger.logInfo("应用[" + AppContext.current().appid() + "] 未设置缓存配置,无法使用会话系统!");
        }
        */
        return CacheHelper.buildCache(appCache);
    }

    /**
     * 获得当前会话id，如果不存在返回空
     *
     * @return
     */
    public static String getRequestSID() {
        Object temp;
        try {
            temp = HttpContext.current().sid();
        } catch (Exception e) {
            temp = null;
        }
        return temp == null || temp.equals("") ? null : temp.toString();
    }

    public static Session createSession(String uid, String jsonString, int expire) {
        CacheHelper cacher = getCacher();
        String sid = uniqueUUID(uid);//申请会话id
        JSONObject exJson = JSONObject.toJSON(jsonString);
        if (exJson == null) {
            exJson = new JSONObject();
        }
        jsonString = exJson
                .puts("_GrapeFW_SID", sid)
                .puts("_GrapeFW_Expire", expire)
                .puts("_GrapeFW_NeedRefresh", (expire + TimeHelper.build().nowSecond()) / 2)
                .puts(uid + "_GrapeFW_AppInfo_", HttpContext.current().appid()).toJSONString();//补充appid参数
        // 先获得上次的会话实体ID并删除
        JSONObject lastInfo = JSONObject.toJSON(Objects.requireNonNull(cacher).get(uid));
        if (lastInfo != null) {
            String lastSID = lastInfo.getString("_GrapeFW_SID");
            if (lastSID != null) {
                cacher.delete(lastSID);
            }
        }
        // 更新本次会话
        cacher.set(uid, expire, jsonString);//更新用户数据集
        cacher.set(sid, expire, uid);
        return new Session(sid, expire);
    }

    public static boolean hasSession() {
        return Session.getRequestSID() != null;
    }

    /**
     * 创建会话
     *
     * @param uid        用户名
     * @param jsonString 需要传入的数据集
     * @return
     */
    public static Session createSession(String uid, String jsonString) {
        return createSession(uid, jsonString, sessiontime);
    }

    /**
     * 创建会话
     *
     * @param uid
     * @param json
     * @return
     */
    public static Session createSession(String uid, JSONObject json) {
        return createSession(uid, json.toJSONString(), sessiontime);
    }

    private static String uuidkey(String uid, String fixed) {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        // 去掉"-"符号
        String temp = str.substring(0, 8) + str.substring(9, 13) + str.substring(14, 18) + str.substring(19, 23) + str.substring(24);
        temp = fixed + temp + uid;
        return temp;
    }

    public static boolean checkSession(String sid) {
        CacheHelper ch = getCacher();
        return ch != null && !StringHelper.invaild(ch.get(sid));
    }

    /**
     * 创建临时会话
     *
     * @param code   临时会话id
     * @param expire 有效期(秒)
     * @return
     */
    public static String createGuessSession(String code, String data, int expire) {
        String sid = uuidkey(code, "guesser");
        Objects.requireNonNull(getCacher()).getSet(sid, expire, data);
        return sid;
    }

    /**
     * 创建临时会话
     *
     * @param code   临时会话id
     * @param expire 有效期(秒)
     * @return
     */
    public static String createGuessSession(String code, int expire) {
        return createGuessSession(code, "", expire);
    }

    /**
     * 创建临时会话
     *
     * @param code   临时会话id
     * @param expire 有效期(秒)
     * @return
     */
    public static String createGuessSession(String code, JSONObject data, int expire) {
        return createGuessSession(code, data.toJSONString(), expire);
    }

    public static Session createSession(String uid, JSONObject json, int expireTime) {
        return createSession(uid, json.toJSONString(), expireTime);
    }

    public Session memSession(String uid, JSONObject infos) {
        this.sessionInfo = infos;
        this.uid = uid;
        return this;
    }

    private static String uniqueUUID(String uid) {
        String tempUUID;
        CacheHelper cacher = getCacher();
        do {
            tempUUID = uuidkey(uid, "gsc_");
        }
        while (Objects.requireNonNull(cacher).get(tempUUID) != null);
        return tempUUID;
    }

    /**
     * 获得当前用户权限值
     */
    public static int getPermissionsValue() {
        return 0;
    }

    /**
     * 获得当前用户组权限值
     */
    public static int getGroupPermissionsValue() {
        return 0;
    }

    private void init(String sid, int expireTime) {
        cacher = getCacher();
        this.expireTime = expireTime;
        if (!updateUserInfo(sid)) {
            nLogger.logInfo("sid:" + sid + " ->无效");
        }
    }

    public Session switchUser(String sid) {
        if (!updateUserInfo(sid)) {
            nLogger.logInfo("sid:" + sid + " ->无效");
        }
        return this;
    }

    //更换缓存服务对象
    public Session switchCacher(String newConfigName) {
        cacher = CacheHelper.buildCache(newConfigName);
        return this;
    }

    public boolean checkSession() {
        Object ro = cacher.get(sid);
        return ro != null;
    }

    /**
     * 替换会话数据
     *
     * @return
     */
    public JSONObject setDatas(JSONObject newData) {
        sessionInfo = newData;
        String userName = getUID();
        return JSONObject.toJSON(cacher.getSet(userName, this.expireTime, newData.toJSONString()));
    }

    /**
     * 设置测试模拟环境
     *
     * @param sid
     * @return
     */
    public Session testSession(String sid) {
        HttpContext.current().sid(sid);
        switchUser(sid);
        return this;
    }

    /**
     * 向本次会话内追加数据
     *
     * @param newData
     * @return
     */
    public JSONObject pushAll(JSONObject newData) {
        JSONObject data = getDatas();
        if (data != null) {
            data.putAll(newData);
            setDatas(data);
        }
        return data;
    }

    /**
     * 根据sid删除会话
     */
    public void deleteSession() {
        if (uid != null && sid != null) {//uuid存在，有效
            cacher.delete(uid);
            cacher.delete(sid);
        }
    }

    /**
     * 获得会话全部数据
     *
     * @return
     */
    public JSONObject getDatas() {
        return sessionInfo;
        //return JSONObject.toJSON( cacher.get(uid) );
    }

    /**
     * 向会话里追加数据
     *
     * @return
     */
    public JSONObject push(String key, Object val) {
        JSONObject data = getDatas();
        if (data != null) {
            data.put(key, val);
            setDatas(data);
        }
        return data;
    }

    /**
     * 更新会话内数据
     *
     * @param newData
     * @return
     */
    public JSONObject edit(String key, Object newData) {
        return push(key, newData);
    }

    /**
     * 获得会话内某一项值
     *
     * @param key
     * @return
     */
    public Object get(String key) {
        Object val = null;
        JSONObject data = getDatas();
        if (data != null) {
            val = data.get(key);
        }
        return val;
    }

    /**
     * 获得会话内某一项值
     *
     * @param key
     * @return
     */
    public String getString(String key) {
        Object rd = get(key);
        return (rd instanceof String) ? (String) rd : "";
    }

    /**
     * 获得会话内某一项值
     *
     * @param key
     * @return
     */
    public int getInt(String key) {
        Object rd = get(key);
        return NumberHelper.number2int(rd);
    }

    /**
     * 获得会话内某一项值
     *
     * @param key
     * @return
     */
    public long getLong(String key) {
        Object rd = get(key);
        return NumberHelper.number2int(rd);
    }

    // 延续会话维持时间(20分钟)
    public Session refreshSession() {
        int need_expire_time = sessionInfo.getInt("_GrapeFW_NeedRefresh");
        if ((TimeHelper.build().nowSecond() + expireTime) < need_expire_time) {
            return this;
        }
        cacher.set(sid, expireTime, uid);
        cacher.set(uid, expireTime, sessionInfo.toJSONString());
        return this;
    }

    //更新当前会话有关信息
    private boolean updateUserInfo(String sid) {
        boolean rb = false;
        if (sid != null) {
            this.sid = sid;
            String uid = cacher.get(sid);
            if (uid != null && !uid.isEmpty()) {//返回了用户名
                this.uid = uid;
                sessionInfo = JSONObject.toJSON(cacher.get(uid));
                // 补充会话数据
                if (sessionInfo != null) {
                    this.appid = sessionInfo.getInt(uid + "_GrapeFW_AppInfo_");//获得所属appid
                    this.expireTime = sessionInfo.getInt("_GrapeFW_Expire");
                    this.gid = sessionInfo.getString(PermissionsPowerDef.fatherIDField);//获得所在组ID
                }
                // 更新会话维持时间
                refreshSession();
                rb = true;
            }
        }
        return rb;
    }

    public String getSID() {
        return this.sid;
    }

    public String getUID() {
        return this.uid;
    }

    public String getGID() {
        return this.gid;
    }

    public int getAppID() {
        return this.appid;
    }
}