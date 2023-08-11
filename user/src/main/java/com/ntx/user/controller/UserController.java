package com.ntx.user.controller;

import com.ntx.common.domain.TUser;
import com.ntx.common.VO.UpdateUserForm;
import com.ntx.user.domain.LoginForm;
import com.ntx.user.service.TUserService;
import com.ntx.common.domain.Result;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
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

    /**
     * 获取用户信息集合
     * @param ids
     * @return
     */
    @GetMapping("/getByIds")
    public List<TUser> getByIds(@RequestParam List<Integer> ids){
        return userService.listByIds(ids);
    }

    /**
     * 更新用户数据
     * @param userForm
     * @return
     */
    @PutMapping("/update")
    public Result userUpdate(@RequestBody UpdateUserForm userForm){
        return userService.updateByUserById(userForm);
    }


    /**
     * 查询登陆用户信息
     * @param id
     * @return
     */
    @GetMapping("/getLoginUser/{id}")
    public Result getLoginUser(@PathVariable int id){
        return userService.getLoginUser(id);

    }
}
