package com.xm.boot.redis.common;

/**
 * globle constant class
 *
 * @author xiehejun(玄墨)
 * @date 2021/8/13 3:32 下午
 */
public class Constant {

    /**
     * String farmat
     */
    public final static String FORMAT_STRING = ":%s";

    public final static String COLON = ":";

    public final static String COMMA = ",";

    /**
     * ip pattern contains port
     */
    public final static String IP_PATTERN = "^((http|https)://)((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(:([0-9]|[1-9]\\d|[1-9]\\d{2}|[1-9]\\d{3}|[1-5]\\d{4}|6[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$)";

    /**
     * port pattern range 0-65535
     */
    public final static String PORT_PATTERN = "^([0-9]|[1-9]\\d|[1-9]\\d{2}|[1-9]\\d{3}|[1-5]\\d{4}|6[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$";
}
