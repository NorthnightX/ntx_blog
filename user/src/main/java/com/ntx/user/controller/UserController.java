package com.ntx.user.controller;

import cn.hutool.core.bean.BeanUtil;
import com.ntx.common.domain.TUser;
import com.ntx.user.DTO.UserDTO;
import com.ntx.user.domain.LoginForm;
import com.ntx.user.service.TUserService;
import com.ntx.common.domain.Result;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private TUserService userService;

    @GetMapping("/getUserById/{id}")
    public Result getUserById(@PathVariable int id){
        return Result.success(userService.getUserById(id));
    }

    /**
     * 登录验证码
     * @return
     * @throws IOException
     */
    @GetMapping("/verificationCode")
    public Result verificationCode() throws IOException {
        return userService.getVerificationCode();
    }

    /**
     * 登录
     * @param loginForm
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginForm loginForm){
        return userService.login(loginForm);
    }

    @GetMapping("/phoneCode")
    public Result phoneCode(String phone){
        return userService.phoneCode(phone);
    }

    @GetMapping("/getByIds")
    public List<TUser> getByIds(@RequestParam List<Integer> ids){
        return userService.listByIds(ids);
    }

    @PutMapping("/update")
    public Result userUpdate(@RequestBody TUser tUser){
        tUser.setGmtModified(LocalDateTime.now());
        return userService.updateById(tUser) ? Result.success("修改成功") : Result.error("网络异常");
    }



    @GetMapping("/getLoginUser/{id}")
    public Result getLoginUser(@PathVariable int id){
        TUser userById = userService.getUserById(id);
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(userById, userDTO);
        return Result.success(userDTO);
    }
}
