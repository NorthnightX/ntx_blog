package com.ntx.user.controller;

import com.ntx.user.domain.TUser;
import com.ntx.user.service.TUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private TUserService userService;

    @GetMapping("/getUserById/{id}")
    public TUser getUserById(@PathVariable int id){
        return userService.getUserById(id);
    }
}
