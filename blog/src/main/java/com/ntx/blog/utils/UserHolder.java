package com.ntx.blog.utils;


import com.ntx.common.domain.TUser;

public class UserHolder {
    private static final ThreadLocal<TUser> tl = new ThreadLocal<>();

    public static void saveUser(TUser user){
        tl.set(user);
    }

    public static TUser getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
