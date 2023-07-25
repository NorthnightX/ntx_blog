package com.ntx.blogType.controller;

import com.ntx.blogType.domain.TBlogType;
import com.ntx.blogType.service.TBlogTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blogType")
public class BlogTypeController {
    @Autowired
    private TBlogTypeService blogTypeService;

    /**
     * 获取单个blog
     * @param id
     * @return
     */
    @GetMapping("/getByTypeId/{id}")
    public TBlogType getByTypeId(@PathVariable int id){
        return blogTypeService.getTypeById(id);
    }
}
