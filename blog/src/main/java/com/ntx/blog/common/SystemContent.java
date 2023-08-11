package com.ntx.blog.common;

public class SystemContent {
    //评论key
    public static final String BLOG_COMMENT_KEY = "blog:comment:";
    //用户喜欢的博客的key
    public static final String BLOG_LIKE_KEY = "blog:like:";
    //用户反对的博客的key
    public static final String BLOG_OPPOSE_KEY = "blog:oppose:";
    //每天的各博客阅读量的key
    public static final String BLOG_CLICK = "blog:click:";
    //当天的24小时阅读量排行榜
    public static final String BLOG_LEADERBOARD = "blog:leaderboard:";
    //用户喜欢查不到时，缓存空对象的ttl
    public static final Long BLOG_LIKE_IS_NULL_TTL = 1L;
}
