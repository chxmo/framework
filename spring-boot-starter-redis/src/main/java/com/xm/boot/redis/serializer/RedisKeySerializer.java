package com.xm.boot.redis.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 自定义的Redis键值序列化.
 * 序列化:判断入参Object是否属于JAVA基本数据类型
 * 如果是,则直接转换为string型并getBytes
 * 如果不是,则默认使用JDK自带的序列化方法.
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class RedisKeySerializer implements RedisSerializer<Object> {

    private static final byte[] EMPTY_ARRAY = new byte[0];
    private static SerializerFeature[] features = {SerializerFeature.DisableCircularReferenceDetect};
    private final Charset charset = StandardCharsets.UTF_8;

    @Override
    public Object deserialize(byte[] bytes) {
        throw new SerializationException("Cannot deserialize");
    }

    @Override
    public byte[] serialize(Object object) {
        if (object == null) {
            return EMPTY_ARRAY;
        }
        if (object instanceof String
                || object instanceof Character
                || object instanceof Number
                || object instanceof Boolean) {
            return object.toString().getBytes(charset);
        }
        String str = JSON.toJSONString(object, features);
        str = str.replaceAll(":", "-");
        return str.getBytes(charset);
    }

}