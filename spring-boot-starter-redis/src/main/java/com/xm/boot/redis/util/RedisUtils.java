package com.xm.boot.redis.util;

import com.xm.boot.redis.exception.RedisExceptionEnum;
import com.xm.boot.redis.exception.RedisRuntimeException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * redis 缓存操作类
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class RedisUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(RedisUtils.class);

    /**
     * 模拟匹配符号
     */
    private final static String VAGUE_SYMBOL = "*";
    /**
     * 批次大小
     */
    private final static int BATCH_SIZE = 1000;
    private static RedisTemplate template = null;
    /**
     * 前缀
     */
    private static String prefix;

    /**
     * 原始前缀
     */
    private static int prefixCount;

    public static void init(String originPrefixName, String prefixName, RedisTemplate redisTemplate) {
        prefixCount = originPrefixName.length() + 1;//缓存前缀+":"字符长度
        prefix = prefixName;
        template = redisTemplate;
    }

    /**
     * put object to redis,key exist by default time(1h)
     *
     * @param key
     * @param value
     */
    public static void put(final String key, final Object value) {
        put(key, value, 60 * 60);
    }

    /**
     * put object to redis,key exist by set time
     * json序列化
     *
     * @param key
     * @param value
     */
    public static void put(final String key, final Object value, final Integer seconds) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        template.execute((RedisConnection connection) -> {
            String json = JSON.toJSONString(value);
            byte[] keyBytes = SafeEncoder.encode(rekey);
            connection.set(keyBytes, SafeEncoder.encode(json));
            connection.expire(keyBytes, seconds);
            LOGGER.debug("setObject key={},value={}", rekey, json);
            return null;
        });
    }

    /**
     * get object by id and key
     *
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(final String key, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        return (T) template.execute((RedisConnection connection) -> {
            byte[] keyBytes = connection.get(SafeEncoder.encode(rekey));
            if (keyBytes == null || keyBytes.length == 0) {
                return null;
            }
            String value = SafeEncoder.encode(keyBytes);
            return JSONObject.parseObject(value, clazz);
        });
    }

    /**
     * remove object by key
     *
     * @param key
     * @return
     */
    public static Long remove(final String key) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("removeObject:{}", rekey);
        return (Long) template.execute((RedisConnection connection) -> connection.del(SafeEncoder.encode(rekey)));
    }

    /**
     * 通过模糊前缀匹配删除，keyprefix带*表示模糊匹配
     *
     * @param keyPrefix 如test:* 表示包含前缀test的key
     */
    public static Integer removeByPrefix(String keyPrefix) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, keyPrefix);//重写key
        if (!rekey.endsWith(VAGUE_SYMBOL)) {
            rekey = rekey + VAGUE_SYMBOL;
        }
        String newKey = rekey;
        LOGGER.debug("removeByPrefix keyPrefix:{}", newKey);
        return (Integer) template.execute((RedisConnection connection) -> {
            Integer result = 0;
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(newKey).count(
                    BATCH_SIZE).build());
            List<byte[]> byteList = new ArrayList<>(BATCH_SIZE);
            while (cursor.hasNext()) {
                byte[] key = cursor.next();
                byteList.add(key);
                if (byteList.size() == BATCH_SIZE) {
                    byte[][] bytes = byteList.toArray(new byte[byteList.size()][]);
                    connection.del(bytes);
                    LOGGER.debug("批量删除完成，size:{}", byteList.size());
                    byteList.clear();
                    result += byteList.size();
                }
            }
            if (byteList.size() > 0) {
                byte[][] bytes = byteList.toArray(new byte[byteList.size()][]);
                LOGGER.debug("批量删除完成，size:{}", byteList.size());
                connection.del(bytes);
                result += byteList.size();
            }
            byteList = null;
            return result;
        });
    }

    /**
     * get key exist time
     *
     * @param key
     * @return
     */
    public static Long ttl(final String key) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        return (Long) template.execute((RedisConnection connection) -> connection.ttl(SafeEncoder.encode(rekey)));
    }

    /**
     * 设置此key的生存时间，单位秒(s)
     */
    public static void setExpire(final String key, final int seconds) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("set key={} expire time={}s", rekey, seconds);
        template.execute((RedisConnection connection) -> connection.expire(SafeEncoder.encode(rekey), seconds));
    }

    /**
     * 设置此key为永不过期，ttl为-1
     */
    public static void setPersist(final String key) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("set key={} persist", rekey);
        template.execute((RedisConnection connection) -> connection.persist(SafeEncoder.encode(rekey)));
    }

    /**
     * 判断key是否存在
     *
     * @param key
     * @return
     */
    public static Boolean exists(final String key) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("query exist key={}", rekey);
        return (Boolean) template.execute((RedisConnection connection) -> connection.exists(SafeEncoder.encode(rekey)));
    }

    //******************hash操作封装********************//

    /**
     * hash 在指定key的置顶域中设置value
     */
    public static void hput(final String key, final String field, final Object value) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        String valueStr = JSON.toJSONString(value);
        LOGGER.debug("set value to field={},key={}", field, rekey);
        template.execute((RedisConnection connection) ->
                connection.hSet(SafeEncoder.encode(rekey), SafeEncoder.encode(field), SafeEncoder.encode(valueStr)));
    }

    /**
     * hash 获取指定key中的指定field中的value
     */
    @SuppressWarnings("unchecked")
    public static <T> T hget(final String key, final String field, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        return (T) template.execute((RedisConnection connection) -> {
            byte[] value = connection.hGet(SafeEncoder.encode(rekey), SafeEncoder.encode(field));
            LOGGER.debug("hget value from key={},fiedl={}", rekey, field);
            if (value == null || value.length == 0) {
                return null;
            }
            return JSONObject.parseObject(value, clazz);
        });
    }

    /**
     * hash 移除指定key中的指定field
     */
    public static void hremove(final String key, final String field) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("gremove value from key={},field={}", rekey, field);
        template.execute(
                (RedisConnection connection) -> connection.hDel(SafeEncoder.encode(rekey), SafeEncoder.encode(field)));
    }

    /**
     * 获取hash的count
     *
     * @param key
     * @return
     */
    public static Long hcount(final String key) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("hcount,key={}", rekey);
        return (Long) template.execute((RedisConnection connection) -> {
            return connection.hLen(SafeEncoder.encode(rekey));
        });
    }

    /**
     * hash-hageAll基类，如果指定class，所有的值都会自动转换为T，业务放在用这个方法前一定要检查自己的value是否是同一个类型，
     * 推荐用 RedisUtils#hgetAll(java.lang.String)
     *
     * @param key
     * @param field
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> Map<String, T> hgetAll(final String key, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("hgetAll,key={}", rekey);
        return (Map<String, T>) template.execute((RedisConnection connection) -> {
            Map<byte[], byte[]> result = connection.hGetAll(SafeEncoder.encode(rekey));
            if (CollectionUtils.isEmpty(result)) {
                return new HashMap<>(0);
            }
            Map<String, T> ans = new HashMap<>(result.size());
            for (Map.Entry<byte[], byte[]> entry : result.entrySet()) {
                ans.put(new String(entry.getKey()), JSON.parseObject(entry.getValue(), clazz));
            }
            return ans;
        });

    }

    /**
     * hash-hgetall命令
     *
     * @param key
     * @return
     */
    public static Map<String, Object> hgetAll(final String key) {
        return hgetAll(key, Object.class);
    }

    //******************list操作封装********************//

    /**
     * list rpush操作 从右边加入，类似先进先出
     *
     * @param key
     * @param value
     * @return
     */
    public static Long rpush(final String key, final Object value) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        String valueStr = JSON.toJSONString(value);
        LOGGER.debug("rpush value with key={},field={}", rekey, value);
        return (Long) template.execute((RedisConnection connection) -> connection.rPush(SafeEncoder.encode(rekey),
                SafeEncoder.encode(valueStr)));
    }

    /**
     * list rpop操作 右边弹出操作
     *
     * @param key
     * @return
     */
    public static <T> T rpop(final String key, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("rpop value with key={}", rekey);
        return (T) template.execute((RedisConnection connection) -> {
            byte[] bytes = connection.rPop(SafeEncoder.encode(rekey));
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return JSONObject.parseObject(bytes, clazz);
        });
    }

    /**
     * 列表的阻塞式弹出尾部元素
     *
     * @param key
     * @param timeout seconds 如果为0则一直阻塞到有数据获取，一定要小于redis配置中的timeout超时时间
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T brpop(final String key, int timeout, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("brpop value with key={}", rekey);
        return (T) template.execute((RedisConnection connection) -> {
            List<byte[]> list = connection.bRPop(timeout, SafeEncoder.encode(rekey));
            if (list == null || list.size() == 0) {
                return null;
            }
            return JSONObject.parseObject(list.get(1), clazz);
        });
    }

    /**
     * 列表的阻塞式弹出头元素
     *
     * @param key
     * @param timeout seconds 如果为0则一直阻塞到有数据获取，一定要小于redis配置中的timeout超时时间
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T blpop(final String key, int timeout, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("brpop value with key={}", rekey);
        return (T) template.execute((RedisConnection connection) -> {
            List<byte[]> list = connection.bLPop(timeout, SafeEncoder.encode(rekey));
            if (list == null || list.size() == 0) {
                return null;
            }
            return JSONObject.parseObject(list.get(1), clazz);
        });
    }

    /**
     * list lpush操作 从左边加入，类似后进先出
     *
     * @param key
     * @param value
     * @return
     */
    public static Long lpush(final String key, final Object value) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        String valueStr = JSON.toJSONString(value);
        LOGGER.debug("rpush value with key={},field={}", rekey, value);
        return (Long) template.execute((RedisConnection connection) -> connection.lPush(SafeEncoder.encode(rekey),
                SafeEncoder.encode(valueStr)));
    }

    /**
     * list lpop操作 左边弹出操作
     *
     * @param key
     * @return
     */
    public static <T> T lpop(final String key, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("lpop value with key={}", rekey);
        return (T) template.execute((RedisConnection connection) -> {
            byte[] bytes = connection.lPop(SafeEncoder.encode(rekey));
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return JSONObject.parseObject(bytes, clazz);
        });
    }

    /**
     * list 获取list的数据，start下标从0开始，end为-1时，表示最后一个元素
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> lrange(final String key, final int start, final int end, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        return (List<T>) template.execute((RedisConnection connection) -> {
            List<T> values = new ArrayList<T>();
            List<byte[]> list = connection.lRange(SafeEncoder.encode(rekey), start, end);
            for (int i = 0; null != list && i < list.size(); i++) {
                values.add(JSONObject.parseObject(list.get(i), clazz));
            }
            return values;
        });
    }

    /**
     * list 获取长度
     *
     * @param key
     * @return
     */
    public static Long lcount(final String key) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("lcount,key={}", rekey);
        return (Long) template.execute((RedisConnection connection) -> {
            return connection.lLen(SafeEncoder.encode(rekey));
        });
    }

    /**
     * list lset命令：重新设置索引为index的值
     *
     * @param key
     * @param index index从0开始;当 index 参数超出范围，或对一个空列表( key 不存在)进行 LSET 时，返回一个错误。
     * @param value 值
     */
    public static void lset(final String key, final int index, final Object value) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        String valueStr = JSON.toJSONString(value);
        LOGGER.debug("lset,key={},index={},value={}", rekey, index, valueStr);
        template.execute((RedisConnection connection) -> {
            connection.lSet(SafeEncoder.encode(rekey), index, SafeEncoder.encode(valueStr));
            return null;
        });
    }

    /**
     * list lrem命令：删除list中值为value
     *
     * @param key
     * @param count 移除与 value 相等的元素数量；当count为0时，移除表中所有与 value 相等的值
     * @param value
     * @return
     */
    public static Long lrem(final String key, final int count, final Object value) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        String valueStr = JSON.toJSONString(value);
        LOGGER.debug("lrem,key={},count={},value={}", rekey, count, valueStr);
        return (Long) template.execute((RedisConnection connection) -> {
            return connection.lRem(SafeEncoder.encode(rekey), count, SafeEncoder.encode(valueStr));
        });
    }

    //******************zset操作操作封装********************//

    /**
     * zadd 将一个元素及其score值加入到有序集key当中。
     *
     * @param key
     * @param value
     * @param score
     * @return
     */
    public static void zadd(String key, Object value, double score) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        byte[] rawKey = SafeEncoder.encode(rekey);
        byte[] rawValue = SafeEncoder.encode(JSON.toJSONString(value));
        boolean bol = (Boolean) template.execute(connection -> {
            return connection.zAdd(rawKey, score, rawValue);
        }, true);
        LOGGER.debug("zadd key:{} value:{}---resule:{}", rekey, value, bol);
    }

    /**
     * 返回有序集key中，指定区间内的成员,其中成员的位置按score值递增(从小到大)来排序
     * 下标参数start和stop都以0为底，也就是说，以0表示有序集第一个成员，以1表示有序集第二个成员，以此类推。
     * 你也可以使用负数下标，以-1表示最后一个成员，-2表示倒数第二个成员，以此类推。
     * 出范围的下标并不会引起错误。
     * 比如说，当start的值比有序集的最大下标还要大，或是start > stop时，ZRANGE命令只是简单地返回一个空列表。
     * 另一方面，假如stop参数的值比有序集的最大下标还要大，那么Redis将stop当作最大下标来处理。
     *
     * @param key
     * @param start 下标从0开始
     * @param end   下标从0开始
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> zrange(String key, long start, long end, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        byte[] rawKey = SafeEncoder.encode(rekey);
        return (List<T>) template.execute(connection -> {
            List<T> values = new ArrayList<T>();
            Set<byte[]> list = connection.zRange(rawKey, start, end);
            Iterator<byte[]> result = list.iterator();
            while (result.hasNext()) {
                byte[] bytes = result.next();
                values.add(JSONObject.parseObject(bytes, clazz));
            }
            return values;
        }, true);
    }

    /**
     * 返回有序集key中，指定区间内的成员,其中成员的位置按score值递减(从大到小)来排列
     *
     * @param key
     * @param start
     * @param end
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> zrevrange(String key, long start, long end, final Class<T> clazz) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        byte[] rawKey = SafeEncoder.encode(rekey);
        return (List<T>) template.execute(connection -> {
            List<T> values = new ArrayList<T>();
            Set<byte[]> list = connection.zRevRange(rawKey, start, end);
            Iterator<byte[]> result = list.iterator();
            while (result.hasNext()) {
                byte[] bytes = result.next();
                values.add(JSONObject.parseObject(bytes, clazz));
            }
            return values;
        }, true);
    }

    /**
     * sortset 返回有序集key中，score值在min和max之间(默认包括score值等于min或max)的成员
     *
     * @param key
     * @return
     */
    public static Long zcount(final String key, double min, double max) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        byte[] rawKey = SafeEncoder.encode(rekey);
        LOGGER.debug("zcount,key={}", rekey);
        return (Long) template.execute((RedisConnection connection) -> {
            return connection.zCount(rawKey, min, max);
        });
    }

    /**
     * sortset 移除有序集key中的一个或多个成员，不存在的成员将被忽略 当key存在但不是有序集类型时，返回一个错误
     *
     * @param key
     * @param value
     * @return
     */
    public static Long zrem(final String key, Object value) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        byte[] rawKey = SafeEncoder.encode(rekey);
        byte[] rawValue = SafeEncoder.encode(JSON.toJSONString(value));
        LOGGER.debug("zrem,key={}", rekey);
        return (Long) template.execute((RedisConnection connection) -> {
            return connection.zRem(rawKey, rawValue);
        });
    }

    //******************incr操作操作封装********************//

    /**
     * 原子增加key的值+1，如果key不存在，则创建并赋值为1。如果存在值但不是integer类型的则会报错
     *
     * @param key 键
     * @return 返回原子+1后的数值
     */
    public static Long incr(final String key) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("incrBy key={}", rekey);
        return (Long) template.execute((RedisConnection connection) -> connection.incr(SafeEncoder.encode(rekey)));
    }

    /**
     * 原子增加key的值+num
     *
     * @param key 键
     * @param num 原子增加的数量，不能带小数部分
     * @return 返回原子+num后的数值
     */
    public static Long incrBy(final String key, final long num) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("incrBy key={},num={}", rekey, num);
        return (Long) template.execute((RedisConnection connection) -> {
            return connection.incrBy(SafeEncoder.encode(rekey), num);
        });
    }

    /**
     * 原子增加key的值+num
     *
     * @param key 键
     * @param num 原子增加的数量，不能带小数部分
     * @return 返回原子+num后的数值
     */
    public static Double incrByFloat(final String key, final double num) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("incrByFloat key={},num={}", rekey, num);
        return (Double) template.execute(
                (RedisConnection connection) -> connection.incrBy(SafeEncoder.encode(rekey), num));
    }

    /**
     * 原子增加key的值-1
     *
     * @param key 键
     * @return 返回原子-1后的数值
     */
    public static Long decr(final String key) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("decr key={}", rekey);
        return (Long) template.execute((RedisConnection connection) -> connection.decr(SafeEncoder.encode(rekey)));
    }

    /**
     * 原子增加key的值-num
     *
     * @param key 键
     * @param num 原子增加的数量
     * @return 返回原子+num后的数值
     */
    public static Long decrBy(final String key, final int num) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        LOGGER.debug("decrBy key={},num={}", rekey, num);
        return (Long) template.execute((RedisConnection connection) -> connection.decrBy(SafeEncoder.encode(rekey), num));
    }

    //******************set操作操作封装********************//

    /**
     * set if not exists object to redis, key exist by default time(1h)
     *
     * @param key   键
     * @param value 值
     * @return Integer reply, specifically: 1 if the key was set；0 if the key was not set
     */
    public static Long setnx(final String key, final Object value) {
        return setnx(key, value, 60 * 60);
    }

    /**
     * set if not exists object to redis
     *
     * @param key     键
     * @param value   值
     * @param seconds 有效期秒数
     * @return Integer reply, specifically: 1 if the key was set；0 if the key was not set
     */
    public static Long setnx(final String key, final Object value, final Integer seconds) {
        return setnx(key, value, seconds, true);
    }

    /**
     * 与上面方法一致，refreshExpireTime为true的时候，每次set会重置key的生存时间，false不会重置
     *
     * @param key
     * @param value
     * @param seconds
     * @param refreshExpireTime
     * @return
     */
    public static Long setnx(final String key, final Object value, final Integer seconds, final Boolean refreshExpireTime) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, key);//重写key
        return (Long) template.execute((RedisConnection connection) -> {
            String json = JSON.toJSONString(value);
            byte[] keyBytes = SafeEncoder.encode(rekey);
            Boolean ret = connection.setNX(keyBytes, SafeEncoder.encode(json));
            if (refreshExpireTime || ret) {
                connection.expire(keyBytes, seconds);
            }
            LOGGER.debug("setnx key={},value={}", rekey, json);
            return ret ? 1L : 0L;
        });
    }

    /**
     * 通过keyprefix前缀来查询所有的前缀匹配key值
     * 1. scan命令并非只执行一次，因为其游标的移动以及scan命令的执行已封装在了cursor.hasNext中，再通过这里的while循环获取到的将是所有匹配的数据
     * 2. 业务应用需要注意每次迭代name值可能会重复，需要做冪等；
     * 3. 不保证中间操作过程中插入的新的key；
     * <code>
     * List<String> list = RedisUtils.scanListByKeyPrefix("test");
     * </code>
     *
     * @param keyPrefix
     * @return
     */
    public static List<String> scanListByKeyPrefix(String keyPrefix) {
        if (null == keyPrefix || keyPrefix.length() == 0) {
            throw new RedisRuntimeException(RedisExceptionEnum.REDIS_PARAM_KEY_NOT_EMPTY);
        }
        Cursor<byte[]> cursor = scan(keyPrefix + VAGUE_SYMBOL);
        List<String> result = new ArrayList<>();
        while (cursor.hasNext()) {
            String value = SafeEncoder.encode(cursor.next());
            result.add(value.substring(prefixCount));
        }
        return result;
    }

    /**
     * 通过scan命令实现获取模糊匹配查找特定的key名字
     * 1. 业务应用需要注意每次迭代name值可能会重复，需要做冪等；
     * 2. 不保证中间操作过程中插入的新的key；
     * <code>
     * Cursor<byte[]> cursor = RedisUtils.scan("test*");//查找以test为开头的所有key name
     * while (cursor.hasNext()){
     * String keyName = new String(cursor.next());
     * //业务逻辑处理,最好用批处理方式
     * }
     * </code>
     *
     * @param keyPrefix 如test:* 表示包含前缀test的key
     * @return
     */
    public static Cursor<byte[]> scan(String keyPrefix) {
        String rekey = KeyUtils.generteKeyWithPlaceholder(prefix, keyPrefix);//重写key
        LOGGER.debug("scanByLimit keyPrefix:{}", rekey);
        return (Cursor<byte[]>) template.execute((RedisConnection connection) -> {
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(rekey).count(
                    BATCH_SIZE).build());
            return cursor;
        });
    }

    /**
     * Executes the given action object within a connection, which can be exposed or not.
     */
    @SuppressWarnings("unchecked")
    public static <T> T execute(RedisCallback<T> action) {
        return (T) template.execute(action);
    }
}
