package com.xm.boot.redis.util;


import java.io.UnsupportedEncodingException;

/**
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class SafeEncoder {

    public static byte[] encode(final String str) {
        try {
            if (str == null) {
                throw new Exception("value sent to redis cannot be null");
            }
            return str.getBytes("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encode(final byte[] data) {
        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
