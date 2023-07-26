package com.ntx.blog.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.service.TBlogService;
import com.ntx.feign.client.BlogTypeClient;
import com.ntx.feign.domain.TBlogType;
import jodd.util.ArraysUtil;
import org.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/blog")
public class BlogController {
    @Autowired
    private TBlogService blogService;
    //restTemplate用于远程调用
//    @Autowired
//    private RestTemplate restTemplate;
    // feign用于远程调用
    @Autowired
    private BlogTypeClient blogTypeClient ;

    /**
     * 使用restTemplate用于远程调用
     * @param id
     * @return
     */
//    @GetMapping("/getBlogById")
//    public BlogDTO getBlogById(@RequestParam int id){
//        BlogDTO blogDTO = new BlogDTO();
//        TBlog blog = blogService.getById(id);
//        Integer typeId = blog.getTypeId();
//        String url = "http://blogTypeService/blog/blogType/getByTypeId/" + typeId;
//        TBlogType blogType = restTemplate.getForObject(url, TBlogType.class);
//        if (blogType != null) {
//            blogDTO.setTypeName(blogType.getName());
//        }
//        BeanUtil.copyProperties(blog, blogDTO);
//        return blogDTO;
//    }


    /**
     * 使用feign用于远程调用
     * 封装blogDTO返回，根据blogId查找其基本信息
     * @param id
     * @return
     */
    @GetMapping("/getBlogById")
    public BlogDTO getBlogById(@RequestParam int id,
                               @RequestHeader(value = "NTX", required = false) String NTX) {
        BlogDTO blogDTO = new BlogDTO();
        TBlog blog = blogService.getById(id);
        Integer typeId = blog.getTypeId();
        System.out.println("ntx:" + NTX);
        TBlogType blogType = blogTypeClient.getByTypeId(typeId);
        if (blogType != null) {
            blogDTO.setTypeName(blogType.getName());
        }
        BeanUtil.copyProperties(blog, blogDTO);
        return blogDTO;
    }

    @GetMapping("/getBlogPage")
    public Result getBlogPage(@RequestParam(required = false, defaultValue = "1") int pageNum,
                              @RequestParam(required = false, defaultValue = "10") int pageSize,
                              @RequestParam(required = false) String title,
                              @RequestParam(required = false) Long typeId){
        //新建page对象
        Page<TBlog> page = new Page<>(pageNum, pageSize);
        //条件查询
        LambdaQueryWrapper<TBlog> blogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLambdaQueryWrapper.like(title != null && title.length() > 0,
                TBlog::getTitle, title);
        blogLambdaQueryWrapper.eq(typeId != null,
                TBlog::getTypeId, typeId);
        blogService.page(page, blogLambdaQueryWrapper);
        //获取查询的结果
        List<TBlog> records = page.getRecords();
        //使用stream获取typeId
        List<Integer> typeIds = records.stream().map(TBlog::getTypeId).distinct().collect(Collectors.toList());
        //远程调用blogType模块查询typeName
        List<TBlogType> byTypeIds = blogTypeClient.getByTypeIds(typeIds);
        Map<Integer, String> typesMap = byTypeIds.stream().collect(Collectors.toMap(TBlogType::getId, TBlogType::getName));
        //将结果转成map
        //stream流进行数据处理，返回blogDTO集合
        List<BlogDTO> blogDTOList = records.stream().map((item) -> {
            BlogDTO blogDTO = new BlogDTO();
            BeanUtil.copyProperties(item, blogDTO);
            blogDTO.setTypeName(typesMap.get(blogDTO.getTypeId()));
            return blogDTO;
        }).collect(Collectors.toList());
        //数据封装
        Page<BlogDTO> pageInfo = new Page<>();
        BeanUtil.copyProperties(page, pageInfo, "records");
        pageInfo.setRecords(blogDTOList);
        return Result.success(pageInfo);

    }


}
