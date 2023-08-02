package com.ntx.client;



import com.ntx.common.domain.TUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("userService")
public interface UserClient {
    @GetMapping("/user/getByIds")
    List<TUser> getByIds(@RequestParam List<Integer> ids);
}
