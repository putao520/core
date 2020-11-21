package common.java.cache;

import common.java.Config.nConfig;
import common.java.apps.MicroServiceContext;
import common.java.string.StringHelper;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;

/**
 * * {
 * * 	"cacheName": "RedisSingle",
 * * 	"password": "admins",
 * * 	"maxIdle": 60000,
 * * 	"maxTotal": 1000,
 * * 	"maxWaitMillis": 10000,
 * * 	"ssl":true
 * * 	副本集模式
 * * 	"cluster": "123.57.213.15:7000,123.57.213.15:7001,123.57.213.15:7002,123.57.213.15:7003,123.57.213.15:7004,123.57.213.15:7005"
 * * }
 */
public class RedisCluster implements InterfaceCache {
    private RedisAdvancedClusterAsyncCommands<String, String> command;

    public RedisCluster(String configString) {
        init(configString);
    }

    public RedisCluster() {
        init(nConfig.netConfig(MicroServiceContext.current().config().cache()));
    }

    private void init(String config) {
        this.command = ((StatefulRedisClusterConnection<String, String>) RedisConn.build(config).getConnect()).async();
    }

    @Override
    public String get(String objectName) {
        try {
            return command.get(objectName).get();
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
            return command.setnx(objectName, StringHelper.any2String(objectName)).get();
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public String getset(String objectName, Object objectValue) {
        try {
            return command.getset(objectName, objectValue.toString()).get();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getset(String objectName, int expire, Object objectValue) {
        try {
            return command.getset(objectName, objectValue.toString()).get();
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
    public long incby(String objectName, long num) {
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
    public long decby(String objectName, long num) {
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