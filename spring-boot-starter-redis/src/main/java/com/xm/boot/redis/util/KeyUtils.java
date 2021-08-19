package com.xm.boot.redis.util;


import com.xm.boot.redis.init.RedisContext;
import org.springframework.util.StringUtils;

import static com.xm.boot.redis.common.Constant.FORMAT_STRING;

/**
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class KeyUtils {

    public static String generteKeyWithPlaceholder(String keyModule, Object... key) {
        String redisContextPrefix = RedisContext.getRedisContext().getPrefix();
        RedisContext.clear();
        if (StringUtils.isEmpty(redisContextPrefix)) {
            return String.format(keyModule, key);
        } else {
            //rediscontextprefix 不为空的时候设置
            return String.format(redisContextPrefix + FORMAT_STRING, key);
        }

    }

}
