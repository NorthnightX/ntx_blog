package com.ntx.blog.controller;

import cn.hutool.core.bean.BeanUtil;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.service.TBlogService;
import com.ntx.feign.client.BlogTypeClient;
import com.ntx.feign.domain.TBlogType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


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


}
