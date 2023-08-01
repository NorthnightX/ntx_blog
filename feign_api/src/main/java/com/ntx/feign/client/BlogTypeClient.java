package com.ntx.feign.client;


import com.ntx.feign.domain.TBlogType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

/**
 * blogType的feign客户端
 */
@FeignClient("blogTypeService")
public interface BlogTypeClient {
    @GetMapping("/blogType/getByTypeId/{id}")
    TBlogType getByTypeId(@PathVariable("id") int id);

    @GetMapping("/blogType/getByTypeIds")
    List<TBlogType> getByTypeIds(@RequestParam List<Integer> ids);
}
