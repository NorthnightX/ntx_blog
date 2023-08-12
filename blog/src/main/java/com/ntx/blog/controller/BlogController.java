package com.ntx.blog.controller;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.service.TBlogService;
import com.ntx.common.VO.UpdateUserForm;
import com.ntx.common.client.BlogTypeClient;
import com.ntx.common.client.UserClient;
import com.ntx.common.domain.Result;
import com.ntx.common.domain.TBlogType;
import com.ntx.common.domain.TUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private MongoTemplate mongoTemplate;


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
     * (完成)
     * 封装blogDTO返回，根据blogId查找其基本信息
     *
     * @param id
     * @return
     */
    @GetMapping("/getBlogById/{id}")
    public Result getBlogById(@PathVariable int id) {
        return blogService.getBlogById(id);

    }


    /**
     * (完成)
     * blog分页查询
     * 使用MongoDB分页
     *
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
                              @RequestParam(required = false) Long typeId) {
        Query query = new Query();
        Page<BlogDTO> page = new Page<>(pageNum, pageSize);
        if (typeId != null) {
            query.addCriteria(Criteria.where("typeId").is(typeId));
        }
        if (title != null && !title.isEmpty()) {
            //i:不区分大小写
            query.addCriteria(Criteria.where("title").regex(title, "i"));
        }
        //过滤私有和已删除的
        query.addCriteria(Criteria.where("deleted").is(1));
        query.addCriteria(Criteria.where("isPublic").is(1));
        //查询总条数
        long count = mongoTemplate.count(query, BlogDTO.class);
        //查询文章内容，私有的，删除的不查询
        query.skip((long) (pageNum - 1) * pageSize).limit(pageSize).with(Sort.by(Sort.Direction.DESC, "clickCount"));
        List<BlogDTO> blogDTOList = mongoTemplate.find(query, BlogDTO.class);
        page.setRecords(blogDTOList);
        page.setTotal(count);
        return Result.success(page);
    }


    /**
     * 更新blog信息
     *
     * @param blog
     * @return
     */
    @PutMapping("/updateBlog")
    public Result updateBlog(@RequestBody TBlog blog) {
        blog.setGmtModified(LocalDateTime.now());
        int updated = blogService.updateBlodById(blog);
        if (updated == 1) {
            return Result.success("修改成功");
        } else {
            return Result.error("修改失败");
        }
    }

    /**
     * (完成)
     * 使用es进行查询(用户关键字搜索)
     *
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

    /**
     * 保存blog
     *
     * @param blog
     * @return
     */
    @PostMapping("/addBlog")
    public Result addBlog(@RequestBody TBlog blog) throws IOException {

        String jsonString = JSON.toJSONString(blog);
        kafkaTemplate.send("blogAdd", "", jsonString);
        return Result.success("发布成功");
    }

    /**
     * 获取用户的博客
     *
     * @param id
     * @return
     */
    @GetMapping("/blogByUser/{id}")
    public Result blogByUser(@PathVariable int id) {
        return blogService.blogByUser(id);

    }

    /**
     * 更新ES和mongoD（只用更新用户的nickName和image）
     * bloggerName;
     * bloggerImage;
     *
     * @param userForm
     * @return
     */
    @PutMapping("/updateBLogInMongoDAndES")
    public Boolean updateBLogInMongoDAndES(@RequestBody UpdateUserForm userForm) throws IOException {
        return blogService.updateBLogInMongoDAndES(userForm);
    }


    /**
     * 选出两天内阅读量最多的文章
     *
     * @return
     */
    @GetMapping("/readNumMaxInTwoDays")
    public Result readNumMaxInTwoDays() {

        return blogService.getMaxWatchInTwoDays();
    }

    @GetMapping("/userLikeBlogs/{id}")
    public Result userLikeBlogs(@PathVariable int id){
        return blogService.userLikeBlogs(id);
    }

}
