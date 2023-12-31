package com.ntx.blog.controller;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.utils.UserHolder;
import com.ntx.common.VO.UpdateUserForm;
import com.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;



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
     * @param blogDTO
     * @return
     */
    @PutMapping("/updateBlog")
    public Result updateBlog(@RequestBody BlogDTO blogDTO) throws IOException {
        int updated = blogService.updateBlodById(blogDTO);
        if (updated == 1) {
            return Result.success("修改成功");
        } else if (updated == 3) {
            return Result.error("您没有权限");
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
        try {
            Integer id = UserHolder.getUser().getId();
            blog.setBlogger(id);
            String jsonString = JSON.toJSONString(blog);
            kafkaTemplate.send("blogAdd", "", jsonString);
            return Result.success("发布成功");
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 获取用户的博客
     * @return
     */
    @GetMapping("/blogByUser")
    public Result blogByUser() {
        try {
            Integer id = UserHolder.getUser().getId();
            return blogService.blogByUser(id);
        } finally {
            UserHolder.removeUser();
        }
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

    /**
     * 喜欢博客
     * @return
     */
    @GetMapping("/userLikeBlogs")
    public Result userLikeBlogs(){
        Integer id;
        try {
            id = UserHolder.getUser().getId();
            return blogService.userLikeBlogs(id);
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 删除博客
     * @param id
     * @return
     */
    @PutMapping("/deleteBlog/{id}")
    public Result deleteBlog(@PathVariable int id) throws IOException {
        return blogService.deleteBLog(id);
    }

    /**
     * 查询用户删除的blog

     * @return
     */
    @GetMapping("/recycleBinBlog")
    public Result recycleBinBlog(){
        try {
            Integer id = UserHolder.getUser().getId();
            return blogService.recycleBinBlog(id);
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 恢复blog
     * @return
     */
    @PutMapping("/recoverBlog/{blogId}")
    public Result recoverBlog(@PathVariable Integer blogId) throws IOException {
        return blogService.recoverBlog(blogId);
    }
}
