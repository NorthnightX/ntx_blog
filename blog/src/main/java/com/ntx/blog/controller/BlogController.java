package com.ntx.blog.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.service.TBlogService;


import com.ntx.client.BlogTypeClient;
import com.ntx.client.UserClient;
import com.ntx.common.domain.Result;
import com.ntx.common.domain.TBlogType;
import com.ntx.common.domain.TUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
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

    @Autowired
    private UserClient userClient;


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

    /**
     * blog分页查询
     * @param pageNum
     * @param pageSize
     * @param title
     * @param typeId
     * @return
     */
    @GetMapping("/getBlogPage")
    public Result getBlogPage(@RequestParam(required = false, defaultValue = "1") int pageNum,
                              @RequestParam(required = false, defaultValue = "10") int pageSize,
                              @RequestParam(required = false) String title,
                              @RequestParam(required = false) Long typeId){
        //条件查询
        LambdaQueryWrapper<TBlog> queryWrapper = new LambdaQueryWrapper<>();
        TBlog tBlog = new TBlog();
        if(title != null){
            queryWrapper.like(TBlog::getTitle, title);
            tBlog.setTitle("%" + title + "%");
        }
        if(typeId != null){
            queryWrapper.eq(TBlog::getTypeId, typeId);
            tBlog.setTypeId(Math.toIntExact(typeId));
        }
        //获取查询的结果
        List<TBlog> pageInfo = blogService.getPage(pageNum, pageSize, tBlog);
        //使用stream获取typeId
        List<Integer> typeIds = pageInfo.stream().
                map(TBlog::getTypeId).distinct().collect(Collectors.toList());
        //远程调用blogType模块查询typeName
        if(typeIds.size() == 0){
            return Result.error("找不到指定内容");
        }
        //异步调用blogTypeClient获取数据
        List<TBlogType> byTypeIds = blogTypeClient.getByTypeIds(typeIds);

        //获取用户数据
        List<Integer> userList = pageInfo.stream().map(TBlog::getBlogger).
                distinct().collect(Collectors.toList());
        List<TUser> users = userClient.getByIds(userList);
        Map<Integer, TUser> tUserMap = users.stream().collect(Collectors.toMap(TUser::getId, tUser -> tUser));
        //获取blogType的数据
        Map<Integer, String> typesMap = byTypeIds.stream().
                collect(Collectors.toMap(TBlogType::getId, TBlogType::getName));
        //将结果转成map
        //stream流进行数据处理，返回blogDTO集合
        List<BlogDTO> blogDTOList = pageInfo.stream().map((item) -> {
            BlogDTO blogDTO = new BlogDTO();
            BeanUtil.copyProperties(item, blogDTO);
            TUser user = tUserMap.get(item.getBlogger());
            blogDTO.setBloggerId(user.getId());
            blogDTO.setBloggerName(user.getName());
            blogDTO.setBloggerImage(user.getImage());
            blogDTO.setTypeName(typesMap.get(blogDTO.getTypeId()));
            return blogDTO;
        }).collect(Collectors.toList());
        //数据封装
        Page<BlogDTO> page = new Page<>(pageNum, pageSize);
        int count = blogService.count(queryWrapper);
        page.setTotal(count);
        page.setRecords(blogDTOList);
        return Result.success(page);
    }



    /**
     * 更新blog信息
     * @param blog
     * @return
     */
    @PutMapping("/updateBlog")
    public Result updateBlog(@RequestBody TBlog blog){
        blog.setGmtModified(LocalDateTime.now());
        int updated = blogService.updateBlodById(blog);
        if(updated == 1){
            return Result.success("修改成功");
        }
        else{
            return Result.error("修改失败");
        }
    }

    /**
     * 使用es进行查询
     * @param pageNum
     * @param pageSize
     * @param keyword
     * @return
     * @throws IOException
     */
    @GetMapping("/queryByKeyword")
    public Result queryByKeyWord(@RequestParam(required = false, defaultValue = "1") int pageNum,
                                 @RequestParam(required = false, defaultValue = "10") int pageSize,
                                 @RequestParam(required = false) String keyword) throws IOException {

        return blogService.queryByKeyword(pageNum, pageSize, keyword);
    }


}
