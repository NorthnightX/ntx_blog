package com.ntx.user.controller;

import com.alibaba.fastjson.JSON;
import com.ntx.common.domain.TUser;
import com.ntx.common.VO.UpdateUserForm;
import com.ntx.common.utils.JwtUtils;
import com.ntx.user.domain.LoginForm;
import com.ntx.user.service.TUserService;
import com.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static com.ntx.user.common.RedisConstant.LOGIN_USER_COUNT;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private TUserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


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
    public Result userUpdate(@RequestBody UpdateUserForm userForm, HttpServletRequest request){
        String authorization = request.getHeader("Authorization");
        String userFromToken = JwtUtils.getUserFromToken(authorization);
        TUser user = JSON.parseObject(userFromToken, TUser.class);
        if(user == null){
            return Result.error("网络异常");
        }
        userForm.setId(user.getId());
        return userService.updateByUserById(userForm);
    }


    /**
     * 查询登陆用户信息
     * @return
     */
    @GetMapping("/getLoginUser")
    public Result getLoginUser(HttpServletRequest request){
        String authorization = request.getHeader("Authorization");
        String userFromToken = JwtUtils.getUserFromToken(authorization);
        TUser user = JSON.parseObject(userFromToken, TUser.class);
        if(user == null){
            return Result.error("网络异常");
        }
        return userService.getLoginUser(user.getId());
    }

    /**
     * 查询当日用户登录数量
     * @return
     */
    @GetMapping("/getActiveUserToday")
    public Result getActiveUserToday(){
        Long size = stringRedisTemplate.opsForHyperLogLog().size(LOGIN_USER_COUNT);
        return Result.success(size);
    }
}
