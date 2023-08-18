package com.ntx.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.common.VO.UpdateUserForm;
import com.ntx.common.client.BlogClient;
import com.ntx.common.domain.Result;
import com.ntx.common.domain.TUser;
import com.ntx.user.DTO.UserDTO;
import com.ntx.user.common.ImageVerificationCode;
import com.ntx.user.domain.LoginForm;
import com.ntx.user.mapper.TUserMapper;
import com.ntx.user.service.TUserService;
import com.ntx.user.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.ntx.user.common.RedisConstant.*;

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
    @Autowired
    private BlogClient blogClient;


    @Override
    public TUser getUserById(int id) {
        return userMapper.queryUserById(id);
    }

    /**
     * 登录
     *
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
                String phone = loginForm.getPhone();
                String phoneCode = loginForm.getPhoneCode();
                String redisKey = PHONE_CODE + phone;
                if (!phoneCode.equals(stringRedisTemplate.opsForValue().get(redisKey))) {
                    return Result.error("验证码错误");
                }
                //验证码正确
                LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TUser::getPhone, phone);
                TUser user = this.getOne(queryWrapper);
                UserDTO userDTO = setUserInfoForReturn(user);
                return Result.success(userDTO);
            }
        }
        //账号登陆
        String redisCode = stringRedisTemplate.opsForValue().get(loginForm.getCodeKey());
        if (redisCode == null) {
            return Result.error("验证码过期");
        } else if (!redisCode.equalsIgnoreCase(loginForm.getCode())) {
            return Result.error("验证码不正确");
        }

        //加密密码
        String MD5Password = MD5.create().digestHex(loginForm.getPassword());
        System.out.println(MD5Password);
        LambdaQueryWrapper<TUser> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(TUser::getName, loginForm.getUsername());
        TUser user = this.getOne(userLambdaQueryWrapper);
        if (user == null) {
            return Result.error("未找到此用户，请先注册");
        }
        //如果密码相等
        if (user.getPassword().equals(MD5Password)) {
            UserDTO userDTO = setUserInfoForReturn(user);
            String s = JwtUtils.generateToken(JSON.toJSONString(user), 10000);
            System.out.println(s);
            boolean b = JwtUtils.validateToken(s);
            System.out.println(b);
            String userFromToken = JwtUtils.getUserFromToken(s);
            System.out.println(userFromToken);
            return Result.success(userDTO);
        } else {
            return Result.error("密码错误");
        }
    }

    /**
     * 设置userDTO
     *
     * @param user
     * @return
     */
    private UserDTO setUserInfoForReturn(TUser user) {
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        Integer id = userDTO.getId();
        String redisKey = LOGIN_USER + id;
        Map<String, Object> userMap = BeanUtil.beanToMap(
                userDTO, new HashMap<>(), CopyOptions.create().
                        setIgnoreNullValue(true).setFieldValueEditor((fieldKey, fieldValue) -> {
                            if (fieldValue == null) {
                                fieldValue = "0";
                            } else {
                                fieldValue = fieldValue + "";
                            }
                            return fieldValue;
                        }));
        stringRedisTemplate.opsForHash().putAll(redisKey, userMap);
        stringRedisTemplate.expire(redisKey, LOGIN_USER_TTL, TimeUnit.DAYS);
        stringRedisTemplate.opsForHyperLogLog().add(LOGIN_USER_COUNT, String.valueOf(user.getId()));
        //更新ttl为当日剩余时间
        //加锁
        Boolean lock = getLock();
        if (lock) {
            try {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
                Duration duration = Duration.between(now, endOfDay);
                stringRedisTemplate.expire(LOGIN_USER_COUNT, duration.getSeconds(), TimeUnit.SECONDS);
            } finally {
                closeLock();
            }
        }
        return userDTO;
    }

    private Boolean getLock() {
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(TIME_LOCK, "1");
        if (Boolean.TRUE.equals(aBoolean)) {
            stringRedisTemplate.expire(TIME_LOCK, 5, TimeUnit.SECONDS);
        }
        return aBoolean;
    }

    private void closeLock() {
        stringRedisTemplate.delete(TIME_LOCK);
    }

    /**
     * 获取验证码
     *
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

    /**
     * 生成手机验证码
     *
     * @param phone
     * @return
     */
    @Override
    public Result phoneCode(String phone) {
        LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TUser::getPhone, phone);
        TUser user = this.getOne(queryWrapper);
        if (user == null) {
            return Result.error("请先注册");
        }
        String redisKey = PHONE_CODE + phone;
        String randomed = RandomUtil.randomNumbers(6);
        System.out.println(randomed);
        stringRedisTemplate.opsForValue().set(redisKey, randomed, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        return Result.success(randomed);
    }


    @Override
    public Result getLoginUser(int id) {
        TUser userById = this.getUserById(id);
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(userById, userDTO);
        return Result.success(userDTO);
    }

    /**
     * 更新用户数据
     *
     * @param userForm
     * @return
     */
    @Override
    @Transactional
    public Result updateByUserById(UpdateUserForm userForm) {
        String field = userForm.getField();
        Integer id = userForm.getId();
        if (field.equals("image")) {
            String image = userForm.getImage();
            boolean update = this.update().
                    eq("id", id).setSql("image = " + " \" " + image + " \" ").
                    setSql("gmt_modified = " + " \" " + LocalDateTime.now() + " \" ").update();
            if (update) {
                Boolean mongoDAndES = updateBLogInMongoDAndES(userForm);
                if (mongoDAndES) {
                    return Result.success("修改成功");
                }
            }
        }
        return Result.error("网络异常");
    }

    private Boolean updateBLogInMongoDAndES(UpdateUserForm userForm) {
        return blogClient.updateBLogInMongoDAndES(userForm);
    }
}




