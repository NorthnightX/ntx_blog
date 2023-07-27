package com.ntx.blogType.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntx.blogType.domain.TBlogType;
import com.ntx.blogType.service.TBlogTypeService;
import jodd.util.ArraysUtil;
import org.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    /**
     * blogType分页
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/queryTypePage")
    public Result getAllType(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                             @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) String name){

        Page<TBlogType> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TBlogType> queryWrapper = new LambdaQueryWrapper<>();
        if(name != null && name.length() > 0){
            queryWrapper.like(TBlogType::getName, name);
            name = "%" + name + "%";
        }
        List<TBlogType> pageInfo = blogTypeService.getPage(pageNum, pageSize, name);
        int count = blogTypeService.count(queryWrapper);
        page.setRecords(pageInfo);
        page.setTotal(count);
        return Result.success(page);
    }

    /**
     * 用于blog查询获取blogTypeName
     * @param ids
     * @return
     */
    @GetMapping("/getByTypeIds")
    public List<TBlogType> getByTypeIds(@RequestParam List<Long> ids){
        List<TBlogType> tBlogTypes = blogTypeService.getByIds(ids);
        return tBlogTypes;
    }

    /**
     * 类型更新
     * @param blogType
     * @return
     */
    @PutMapping("/updateBlogType")
    public Result updateBlogType(@RequestBody TBlogType blogType){
        blogType.setGmtModified(LocalDateTime.now());
        boolean updated = blogTypeService.updateById(blogType);
        if(updated){
            return Result.success("修改成功");
        }
        return Result.error("网络异常");
    }

    /**
     * 前端请求所有类型
     * @return
     */
    @GetMapping("/queryAllType")
    public Result getAllType(){
        Map<Integer, String> collect =
                blogTypeService.list().stream().
                        collect(Collectors.toMap(TBlogType::getId, TBlogType::getName));
        return Result.success(collect);
    }

}
