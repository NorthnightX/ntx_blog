package com.ntx.blog.controller;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
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
    private BlogTypeClient blogTypeClient;

    @Autowired
    private UserClient userClient;
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

        BlogDTO dto = mongoTemplate.findById(id, BlogDTO.class);
        if (dto != null) {
            kafkaTemplate.send("blogView", "", String.valueOf(id));
            return Result.success(dto);
        }
        BlogDTO blogDTO = new BlogDTO();
        TBlog blog = blogService.getById(id);
        Integer typeId = blog.getTypeId();
        TBlogType blogType = blogTypeClient.getByTypeId(typeId);
        if (blogType != null) {
            blogDTO.setTypeName(blogType.getName());
        }
        Integer blogger = blog.getBlogger();
        List<Integer> list = new ArrayList<>();
        list.add(blogger);
        List<TUser> userList = userClient.getByIds(list);
        //设置返回DTO属性
        BeanUtil.copyProperties(blog, blogDTO);
        Map<Integer, TUser> userMap = userList.stream().collect(Collectors.toMap(TUser::getId, tUser -> tUser));
        blogDTO.setBloggerName(userMap.get(blogger).getName());
        blogDTO.setBloggerImage(userMap.get(blogger).getImage());
        blogDTO.setBloggerId(blogger);
        kafkaTemplate.send("blogView", "", String.valueOf(id));
        return Result.success(blogDTO);
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

//        //条件查询
//        LambdaQueryWrapper<TBlog> queryWrapper = new LambdaQueryWrapper<>();
//        TBlog tBlog = new TBlog();
//        if(title != null){
//            queryWrapper.like(TBlog::getTitle, title);
//            tBlog.setTitle("%" + title + "%");
//        }
//        if(typeId != null){
//            queryWrapper.eq(TBlog::getTypeId, typeId);
//            tBlog.setTypeId(Math.toIntExact(typeId));
//        }
//        //获取查询的结果
//        List<TBlog> pageInfo = blogService.getPage(pageNum, pageSize, tBlog);
//        //使用stream获取typeId
//        List<Integer> typeIds = pageInfo.stream().
//                map(TBlog::getTypeId).distinct().collect(Collectors.toList());
//        //远程调用blogType模块查询typeName
//        if(typeIds.size() == 0){
//            return Result.error("找不到指定内容");
//        }
//        //异步调用blogTypeClient获取数据
//        List<TBlogType> byTypeIds = blogTypeClient.getByTypeIds(typeIds);
//
//        //获取用户数据
//        List<Integer> userList = pageInfo.stream().map(TBlog::getBlogger).
//                distinct().collect(Collectors.toList());
//        List<TUser> users = userClient.getByIds(userList);
//        Map<Integer, TUser> tUserMap = users.stream().collect(Collectors.toMap(TUser::getId, tUser -> tUser));
//        //获取blogType的数据
//        Map<Integer, String> typesMap = byTypeIds.stream().
//                collect(Collectors.toMap(TBlogType::getId, TBlogType::getName));
//        //将结果转成map
//        //stream流进行数据处理，返回blogDTO集合
//        List<BlogDTO> blogDTOList = pageInfo.stream().map((item) -> {
//            BlogDTO blogDTO = new BlogDTO();
//            BeanUtil.copyProperties(item, blogDTO);
//            TUser user = tUserMap.get(item.getBlogger());
//            blogDTO.setBloggerId(user.getId());
//            blogDTO.setBloggerName(user.getName());
//            blogDTO.setBloggerImage(user.getImage());
//            blogDTO.setTypeName(typesMap.get(blogDTO.getTypeId()));
//            return blogDTO;
//        }).collect(Collectors.toList());
//        //数据封装
//        Page<BlogDTO> page = new Page<>(pageNum, pageSize);
//        int count = blogService.count(queryWrapper);
//        page.setTotal(count);
//        page.setRecords(blogDTOList);
//        return Result.success(page);
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
        //查询mongoDB，查不到查数据库
        Query query = new Query();
        query.addCriteria(Criteria.where("bloggerId").is(id)).addCriteria(Criteria.where("deleted").is(1));
        List<BlogDTO> blogDTOList = mongoTemplate.find(query, BlogDTO.class);
        return Result.success(blogDTOList);
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
     * @return
     */
    @GetMapping("/readNumMaxInTwoDays")
    public Result readNumMaxInTwoDays(){

        return blogService.getMaxWatchInTwoDays();
    }

}
