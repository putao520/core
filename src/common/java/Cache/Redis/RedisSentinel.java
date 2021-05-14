package common.java.Cache.Redis;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Cache.Common.InterfaceCache;
import common.java.Config.Config;
import common.java.Number.NumberHelper;
import common.java.String.StringHelper;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * * {
 * * 	"cacheName": "RedisSingle",
 * * 	"password": "admins",
 * * 	"maxIdle": 60000,
 * * 	"maxTotal": 1000,
 * * 	"maxWaitMillis": 10000,
 * * 	"ssl":true
 * * 	副本集模式
 * * 	"sentinel": "123.57.213.15:7000,123.57.213.15:7001,123.57.213.15:7002,123.57.213.15:7003,123.57.213.15:7004,123.57.213.15:7005"
 * * }
 */
public class RedisSentinel implements InterfaceCache {
    private RedisAsyncCommands<String, Object> command;

    public RedisSentinel(String configString) {
        init(configString);
    }

    public RedisSentinel() {
        init(Config.netConfig(MicroServiceContext.current().config().cache()));
    }

    private void init(String config) {
        this.command = ((StatefulRedisConnection<String, Object>) RedisConn.build(config).getConnect()).async();
    }

    @Override
    public Object get(String objectName) {
        try {
            return command.get(objectName).get();
        } catch (Exception e) {
            return null;
        }
    }

    public int getInt(String objectName) {
        return NumberHelper.number2int(get(objectName));
    }

    public long getLong(String objectName) {
        return NumberHelper.number2long(get(objectName));
    }

    public float getFloat(String objectName) {
        return NumberHelper.number2float(get(objectName));
    }

    public double getDouble(String objectName) {
        return NumberHelper.number2double(get(objectName));
    }

    public boolean getBoolean(String objectName) {
        return Boolean.parseBoolean(StringHelper.toString(get(objectName)));
    }

    public String getString(String objectName) {
        return StringHelper.toString(get(objectName));
    }

    public BigDecimal getBigDecimal(String objectName) {
        return BigDecimal.valueOf(getLong(objectName));
    }

    public BigInteger getBigInteger(String objectName) {
        return BigInteger.valueOf(getInt(objectName));
    }

    public JSONObject getJson(String objectName) {
        try {
            return JSONObject.toJSON(command.get(objectName).get().toString());
        } catch (Exception e) {
            return null;
        }
    }

    public JSONArray getJsonArray(String objectName) {
        try {
            return JSONArray.toJSONArray(command.get(objectName).get().toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param objectName
     * @param expire     秒
     * @return
     */
    @Override
    public boolean setExpire(String objectName, int expire) {
        try {
            return command.expire(objectName, Integer.valueOf(expire).longValue()).get();
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public String set(String objectName, Object objectValue) {
        try {
            return command.set(objectName, objectValue.toString()).get();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String set(String objectName, int expire, Object objectValue) {
        try {
            return command.setex(objectName, Integer.valueOf(expire).longValue(), objectValue.toString()).get();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean setNX(String objectName, Object objectValue) {
        try {
            return command.setnx(objectName, StringHelper.toString(objectName)).get();
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public Object getSet(String objectName, Object objectValue) {
        try {
            return command.getset(objectName, objectValue.toString()).get();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object getSet(String objectName, int expire, Object objectValue) {
        try {
            Object r = command.getset(objectName, objectValue.toString()).get();
            setExpire(objectName, expire);
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public long inc(String objectName) {
        try {
            return command.incr(objectName).get();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public long incBy(String objectName, long num) {
        try {
            return command.incrby(objectName, num).get();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public long dec(String objectName) {
        try {
            return command.decr(objectName).get();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public long decBy(String objectName, long num) {
        try {
            return command.decrby(objectName, num).get();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public long delete(String objectName) {
        try {
            return command.del(objectName).get();
        } catch (Exception e) {
            return -1;
        }
    }
}
