package com.ntx.common.client;

import com.ntx.common.VO.UpdateUserForm;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "blogService")
public interface BlogClient {

    @PutMapping("/blog/updateBLogInMongoDAndES")
    Boolean updateBLogInMongoDAndES(@RequestBody UpdateUserForm userForm);
}
