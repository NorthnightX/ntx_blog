package com.ntx.user.controller;

import com.ntx.user.domain.LoginForm;
import com.ntx.user.domain.TUser;
import com.ntx.user.service.TUserService;
import org.ntx.common.domain.Result;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private TUserService userService;

    @GetMapping("/getUserById/{id}")
    public Result getUserById(@PathVariable int id){
        return Result.success(userService.getUserById(id));
    }

    @GetMapping("/verificationCode")
    public Result verificationCode() throws IOException {
        return userService.getVerificationCode();
    }
    @PostMapping("/login")
    public Result login(@RequestBody LoginForm loginForm){
        return userService.login(loginForm);
    }
}
