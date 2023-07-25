package com.ntx.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.user.DTO.UserDTO;
import com.ntx.user.domain.LoginForm;
import com.ntx.user.domain.TUser;
import com.ntx.user.mapper.TUserMapper;
import com.ntx.user.service.TUserService;
import com.ntx.user.common.ImageVerificationCode;
import org.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.ntx.user.common.RedisConstant.LOGIN_CODE;
import static com.ntx.user.common.RedisConstant.LOGIN_CODE_TTL;

/**
 * @author NorthnightX
 * @description 针对表【t_user】的数据库操作Service实现
 * @createDate 2023-07-24 15:02:51
 */
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser> implements TUserService {

    @Autowired
    private TUserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public TUser getUserById(int id) {
        return userMapper.queryUserById(id);
    }

    /**
     * 登录
     * @param loginForm
     * @return
     */
    @Override
    public Result login(LoginForm loginForm) {
        //判断用户用过手机还是账号登录
        if (loginForm.getPhone() != null) {
            //手机登录
            if (loginForm.getPhoneCode() != null) {
                //手机验证码登录
                return null;
            } else {
                //手机密码登录
                return null;
            }

        }
        //账号登陆
//        String redisCode = stringRedisTemplate.opsForValue().get(loginForm.getCodeKey());
//        if (redisCode == null) {
//            return Result.error("验证码过期");
//        } else if (!redisCode.equals(loginForm.getCode())) {
//            return Result.error("验证码不正确");
//        }
        //加密密码
        String MD5Password = MD5.create().digestHex(loginForm.getPassword());
        System.out.println(MD5Password);
        LambdaQueryWrapper<TUser> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(TUser::getName, loginForm.getUsername());
        TUser user = this.getOne(userLambdaQueryWrapper);
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        //如果密码相等
        if (user.getPassword().equals(MD5Password)) {
            return Result.success(userDTO);
        } else {
            return Result.error("密码错误");
        }

    }

    /**
     * 获取验证码
     * @return
     * @throws IOException
     */
    @Override
    public Result getVerificationCode() throws IOException {
        Map<String, String> map = new HashMap<>();
        //获取验证码对象
        ImageVerificationCode imageVerificationCode = new ImageVerificationCode();
        BufferedImage image = imageVerificationCode.getImage();
        String text = imageVerificationCode.getText();
        //生成验证码id
        String str = UUID.randomUUID().toString().replace("-", "");
        String redisKey = LOGIN_CODE + str;
        stringRedisTemplate.opsForValue().set(redisKey, text, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //向网页传输验证码图片
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "JPEG", outputStream);
        byte[] byteArray = outputStream.toByteArray();
        //转成base64格式
        String encode = Base64.getEncoder().encodeToString(byteArray);
        String prefix = "data:image/jpeg;base64,";
        String baseStr = prefix + encode;
        map.put("redisKey", redisKey);
        map.put("base64Str", baseStr);
        return Result.success(map);
    }
}




