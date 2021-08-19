package com.xm.boot.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import java.time.Duration;

/**
 * poperties of xm.redis.conf
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
@Validated
@ConfigurationProperties(prefix = "xm.redis.conf")
@Data
public class RedisConfig {
    /**
     * Database index used by the connection factory.
     */
    private int database = 0;

    /**
     * cache data used by prefix
     */
    @NotEmpty(message = "prefix must be not null in xm.redis.conf")
    private String prefix;

    /**
     * Redis server host.
     */
    private String host;

    /**
     * Login password of the redis server.
     */
    private String password;

    /**
     * Redis server port.
     */
    private int port = 6379;

    /**
     * Enable SSL.
     */
    private boolean ssl;

    /**
     * Connection timeout in milliseconds.
     */
    private Duration timeout = Duration.ofMillis(3000);

    /**
     * Connection closed timeout in milliseconds.
     */
    private Duration shutdownTimeout = Duration.ofMillis(3000);

    /**
     * lettuce客户端共享连接配置，默认为true；请谨慎修改这个参数！！！
     * true：表示共享redis连接，但是在blpop阻塞命令\事物操作中，会新建连接；这种方式占用连接数少，性能比jedis客户端高、稍微比连接池版本性能低点；
     * false：表示每次去factory去获取连接的时候，都会新建连接；
     */
    private Boolean shareNativeConnection = true;

    /**
     * pool.conf
     */
    private Pool pool;

    /**
     * sentinel used in sentinel.conf seted
     */
    private Sentinel sentinel;

    /**
     * cluster used in cluster.conf seted
     */
    private Cluster cluster;

    /**
     * redis useable or not
     */
    private Boolean enabled;

    /**
     * Pool properties.
     */
    @Data
    public static class Pool {

        /**
         * Max number of "idle" connections in the pool. Use a negative value to indicate
         * an unlimited number of idle connections.
         */
        private int maxIdle = 8;

        /**
         * Target for the minimum number of idle connections to maintain in the pool. This
         * setting only has an effect if it is positive.
         */
        private int minIdle = 0;

        /**
         * Max number of connections that can be allocated by the pool at a given time.
         * Use a negative value for no limit.
         */
        private int maxActive = 10;

        /**
         * Maximum amount of time (in milliseconds) a connection allocation should block
         * before throwing an exception when the pool is exhausted. Use a negative value
         * to block indefinitely.
         */
        private Duration maxWait = Duration.ofMillis(-1);

    }

    /**
     * Cluster properties.
     */
    @Data
    public static class Cluster {

        /**
         * Comma-separated list of "host:port" pairs to bootstrap from. This represents an
         * "initial" list of cluster nodes and is required to have at least one entry.
         */
        private String nodes;

        /**
         * Maximum number of redirects to follow when executing commands across the
         * cluster.
         */
        private Integer maxRedirects;

    }

    /**
     * Redis sentinel properties.
     */
    @Data
    public static class Sentinel {

        /**
         * Name of Redis server.
         */
        private String master;

        /**
         * Comma-separated list of "host:port" pairs to bootstrap from. This represents an
         * "initial" list of sentinel nodes and is required to have at least three entry.
         */
        private String nodes;

    }
}
