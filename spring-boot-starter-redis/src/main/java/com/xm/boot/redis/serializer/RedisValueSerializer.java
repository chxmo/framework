package com.xm.boot.redis.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.Objects;

/**
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class RedisValueSerializer implements RedisSerializer<Object> {

    private static final byte[] EMPTY_ARRAY = new byte[0];

    private static SerializerFeature[] features = {SerializerFeature.WriteClassName};

    // 因安全漏洞考虑，关闭autosupport
//    static {
//        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
//    }

    private static boolean isEmpty(byte[] data) {
        return data == null || data.length == 0;
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (isEmpty(bytes)) {
            return null;
        }
        try {
            return JSON.parseObject(bytes, Object.class);
        } catch (Exception ex) {
            throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (Objects.isNull(object)) {
            return EMPTY_ARRAY;
        }
        try {
            return JSON.toJSONBytes(object, features);
        } catch (Exception ex) {
            throw new SerializationException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

}
