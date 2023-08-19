package com.ntx.common.client;

import com.ntx.common.VO.UpdateUserForm;
import feign.Request;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "blogService")
public interface BlogClient {

    @PutMapping("/blog/updateBLogInMongoDAndES")
    Boolean updateBLogInMongoDAndES(@RequestBody UpdateUserForm userForm);

}
