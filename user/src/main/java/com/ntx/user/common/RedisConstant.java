package com.ntx.user.common;

public class RedisConstant {
    public static final String LOGIN_CODE = "login:code:";
    public static final String PHONE_CODE = "phone:code:";
    public static final String LOGIN_USER = "login:user:";
    public static final String LOGIN_USER_COUNT = "login:userCount";
    public static final Long LOGIN_USER_COUNT_TTL = 1L;
    public static final Long LOGIN_USER_TTL = 7L;
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String TIME_LOCK = "time:lock";
}
