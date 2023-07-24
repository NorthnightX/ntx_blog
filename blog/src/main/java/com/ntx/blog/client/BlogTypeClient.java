package com.ntx.blog.client;

import com.ntx.blogType.domain.TBlogType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("blogTypeService")
public interface BlogTypeClient {
    @GetMapping("/blog/blogType/getByTypeId/{id}")
    TBlogType getByTypeId(@PathVariable("id") int id);
}
