package com.ntx.blogType.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntx.blogType.domain.TBlogType;
import com.ntx.blogType.service.TBlogTypeService;
import jodd.util.ArraysUtil;
import org.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @GetMapping("/queryAllType")
    public Result getAllType(@RequestParam(required = false) Integer pageNum,
                             @RequestParam(required = false) Integer pageSize){
        if(pageSize != null && pageNum != null){
            Page<TBlogType> page = new Page<>(pageNum, pageSize);

        }
        List<TBlogType> list = blogTypeService.list();
        Map<Integer, String> collect =
                blogTypeService.list().stream().
                        collect(Collectors.toMap(TBlogType::getId, TBlogType::getName));
        return Result.success(collect);
    }

    @GetMapping("/getByTypeIds")
    public List<TBlogType> getByTypeIds(@RequestParam List<Long> ids){
        List<TBlogType> tBlogTypes = blogTypeService.getByIds(ids);
        return tBlogTypes;
    }

}
