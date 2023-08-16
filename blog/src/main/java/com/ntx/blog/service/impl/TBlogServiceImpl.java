package com.ntx.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.domain.TLikeBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.dto.CommentDTO;
import com.ntx.blog.mapper.TBlogMapper;
import com.ntx.blog.service.TBlogService;

import com.ntx.blog.service.TLikeBlogService;
import com.ntx.blog.utils.PopulatingBlogDTO;
import com.ntx.common.VO.UpdateUserForm;
import com.ntx.common.client.BlogTypeClient;
import com.ntx.common.client.UserClient;
import com.ntx.common.domain.Result;
import com.ntx.common.domain.TBlogType;
import com.ntx.common.domain.TUser;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ntx.blog.common.SystemContent.*;

/**
* @author NorthnightX
* @description 针对表【t_blog】的数据库操作Service实现
* @createDate 2023-07-24 15:40:56
*/
@Service
public  class TBlogServiceImpl extends ServiceImpl<TBlogMapper, TBlog>
    implements TBlogService {

    @Autowired
    private TBlogMapper blogMapper;
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private BlogTypeClient blogTypeClient;
    @Autowired
    private TLikeBlogService likeBlogService;
    @Autowired
    private UserClient userClient;
    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;
    @Autowired
    private PopulatingBlogDTO populatingBlogDTO;

    /**
     * 更新BLog
     * @param blogDTO
     * @return
     */
    @Override
    public int updateBlodById(BlogDTO blogDTO) throws IOException {
        blogDTO.setGmtModified(LocalDateTime.now());
        //更新数据库
        TBlog blog = new TBlog();
        BeanUtil.copyProperties(blogDTO, blog);
        boolean b = this.updateById(blog);
        if(!b){
            return 2;
        }
        TBlogType byTypeId = blogTypeClient.getByTypeId(blog.getTypeId());
        String name = byTypeId.getName();
        blogDTO.setTypeName(name);
        //根据DTO更新ES
        UpdateRequest request = new UpdateRequest("blog", String.valueOf(blog.getId()));
        request.doc("title", blogDTO.getTitle(),
                "image", blogDTO.getImage(),
                "content", blogDTO.getContent(),
                "typeId", blogDTO.getTypeId(),
                "typeName", blogDTO.getTypeName(),
                "gmtModified", blogDTO.getGmtModified(),
                "isPublic", blogDTO.getIsPublic());
        client.update(request, RequestOptions.DEFAULT);
        //根据DTO更新MongoDB
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(blog.getId()));
        Update update = new Update().set("title", blogDTO.getTitle())
                .set("image", blogDTO.getImage())
                .set("content", blogDTO.getContent())
                .set("typeId", blogDTO.getTypeId())
                .set("typeName", blogDTO.getTypeName())
                .set("gmtModified", blogDTO.getGmtModified())
                .set("isPublic", blogDTO.getIsPublic());
        mongoTemplate.updateFirst(query, update, BlogDTO.class);
        return 1;

    }

    @Override
    public List<TBlog> getPage(Integer pageNum, Integer pageSize, TBlog tBlog) {
        int start = (pageNum - 1) * pageSize;
        return blogMapper.getPage(start, pageSize, tBlog);
    }

    /**
     * 用户关键字搜索
     * @param pageNum
     * @param pageSize
     * @param keyword
     * @return
     * @throws IOException
     */
    @Override
    public Result queryByKeyword(int pageNum, int pageSize, String keyword) throws IOException {
        //1.准备request
        SearchRequest request = new SearchRequest("blog");
        //2.写dsl语句
        //添加过滤条件，私有的，删除的，状态异常的不显示给用户
        BoolQueryBuilder booledQuery = QueryBuilders.boolQuery();
        booledQuery.must(QueryBuilders.matchQuery("text", keyword)).
                must(QueryBuilders.termQuery("isPublic", 1)).
                must(QueryBuilders.termQuery("status", 1)).
                must(QueryBuilders.termQuery("deleted", 1));
        request.source().query(booledQuery);
        request.source().size(pageSize).from((pageNum - 1) * pageSize);
        request.source().highlighter(new HighlightBuilder().
                field("title").requireFieldMatch(false));
        //3.查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //数据处理
        SearchHits searchHits = response.getHits();
        TotalHits totalHits = searchHits.getTotalHits();
        long value = totalHits.value;
        SearchHit[] hits = searchHits.getHits();
        List<BlogDTO> list = new ArrayList<>();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            BlogDTO blogDTO = JSON.parseObject(json, BlogDTO.class);
            Map<String, HighlightField> highlightFields =
                    hit.getHighlightFields();
            if(!CollectionUtils.isEmpty(highlightFields)){
                HighlightField highlightFieldTitle = highlightFields.get("title");
                if(highlightFieldTitle != null){
                    String title = highlightFieldTitle.getFragments()[0].string();
                    blogDTO.setTitle(title);
                }
            }
            list.add(blogDTO);
        }
        Page<BlogDTO> page = new Page<>(pageNum, pageSize);
        page.setTotal(value);
        page.setRecords(list);
        return Result.success(page);
    }

    /**
     * 修改用户信息时，同时修改blog的各种缓存信息
     * @param userForm
     * @return
     */
    @Override
    @Transactional
    public Boolean updateBLogInMongoDAndES(UpdateUserForm userForm) {
        String field = userForm.getField();
        Integer id = userForm.getId();
        Query query = new Query();
        query.addCriteria(Criteria.where("bloggerId").is(id));
        LambdaQueryWrapper<TBlog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TBlog::getBlogger, id);
        List<TBlog> list = this.list(queryWrapper);
        if(field.equals("image"))
        {
            //更新MongoDB
            String image = userForm.getImage();
            Update update = new Update().set("bloggerImage", image);
            mongoTemplate.updateMulti(query, update, BlogDTO.class);
            this.list();
            //更新ES中bloggerId为id的所有的文档的bloggerImage为userForm.getImage()
            list.forEach(blog -> {
                UpdateRequest updateRequest = new UpdateRequest("blog", String.valueOf(blog.getId()));
                updateRequest.doc("bloggerImage", image);
                try {
                    client.update(updateRequest, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        }
        else if(field.equals("nickName"))
        {
            String nickName = userForm.getNickName();
            //更新MongoDB
            Update update = new Update().set("bloggerName", nickName);
            mongoTemplate.updateMulti(query, update, BlogDTO.class);
            //更新ES
            list.forEach(blog -> {
                UpdateRequest request = new UpdateRequest("blog", String.valueOf(blog.getId()));
                request.doc("bloggerName", nickName);
                try {
                    client.update(request, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        }
        return false;
    }


    /**
     * 获取两天阅读排行
     * @return
     */
    @Override
    public Result getMaxWatchInTwoDays() {
        //查找Redis获取两天的阅读排行
        LocalDate today = LocalDate.now();
        String todayKey = BLOG_LEADERBOARD + today;
        String yesterdayKey = BLOG_CLICK + today.minusDays(1);
        // 计算前天的日期
        String dayBeforeYesterdayKey = BLOG_CLICK + today.minusDays(2);
        //求前两天click数据的并集，赋值给今天的排行榜数据
        stringRedisTemplate.opsForZSet().unionAndStore(yesterdayKey, dayBeforeYesterdayKey, todayKey);
        stringRedisTemplate.expire(todayKey, 1L, TimeUnit.DAYS);
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(todayKey, 0, 9);
        if (typedTuples != null) {
            List<String> blogId = typedTuples.stream().
                    map(ZSetOperations.TypedTuple::getValue).collect(Collectors.toList());
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").in(blogId));
            query.fields().include("title");
            List<BlogDTO> blogDTOList = mongoTemplate.find(query, BlogDTO.class);
            return Result.success(blogDTOList);

        }
        return Result.error("网络异常");
    }

    /**
     * 根据用户查询blog
     * @param id
     * @return
     */
    @Override
    public Result blogByUser(int id) {
        //查询mongoDB，查不到查数据库
        Query query = new Query();
        query.addCriteria(Criteria.where("bloggerId").is(id)).addCriteria(Criteria.where("deleted").is(1));
        List<BlogDTO> blogDTOList = mongoTemplate.find(query, BlogDTO.class);
        if (!blogDTOList.isEmpty()) {
            return Result.success(blogDTOList);
        }
        //查询数据库
        LambdaQueryWrapper<TBlog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TBlog::getBlogger, id);
        List<TBlog> list = this.list(queryWrapper);
        //如果为空返回空集合
        if(list.isEmpty()){
            return Result.success(new ArrayList<>());
        }
        List<BlogDTO> dtoList = populatingBlogDTO.PopulatingBlogDTOData(list);
        //将数据储存到MongoDB
        mongoTemplate.insertAll(dtoList);
        return Result.success(dtoList);
    }

    /**
     * 根据文章id查找文章
     * @param id
     * @return
     */
    @Override
    public Result getBlogById(int id) {
        //从mongoDB获取数据
        BlogDTO dto = mongoTemplate.findById(id, BlogDTO.class);
        if (dto != null) {
            kafkaTemplate.send("blogView", "", String.valueOf(id));
            return Result.success(dto);
        }
        //取不到的话查找数据库
        BlogDTO blogDTO = new BlogDTO();
        TBlog blog = this.getById(id);
        if(blog == null){
            return Result.error("文章找不到了~");
        }
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
        //存储到MongoDB
        mongoTemplate.insert(blogDTO);
        kafkaTemplate.send("blogView", "", String.valueOf(id));
        return Result.success(blogDTO);
    }

    /**
     * 查询用户喜欢的博客
     * @param id
     * @return
     */
    @Override
    public Result userLikeBlogs(int id) {
        String redisKey = BLOG_LIKE_KEY + id;
        //查询用户喜欢
        Set<String> members = stringRedisTemplate.opsForSet().members(redisKey);
        Query query = new Query();
        //如果缓存命中
        if (members != null && !members.isEmpty()) {
            if (members.size() == 1 && members.contains("")) {
                return Result.success(new ArrayList<>());
            }
            query.addCriteria(Criteria.where("_id").in(members));
            List<BlogDTO> blogDTOList = mongoTemplate.find(query, BlogDTO.class);
            //如果mongoDB中有
            if (!blogDTOList.isEmpty()) {
                return Result.success(blogDTOList);
            }
            //如果mongoDB中没有
            return Result.success(queryBlogByUserLike(members));
        }
        //缓存未命中，查询用户喜欢
        LambdaQueryWrapper<TLikeBlog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TLikeBlog::getUserId, id);
        queryWrapper.select(TLikeBlog::getBlogId);
        Set<String> blogIds = likeBlogService.list(queryWrapper).
                stream().map(tLikeBlog -> tLikeBlog.getBlogId().toString()).
                collect(Collectors.toSet());
        System.out.println(Arrays.toString(blogIds.toArray()));
        //如果用户没有喜欢的blog,缓存空对象
        if(blogIds.isEmpty()){
            stringRedisTemplate.opsForSet().add(redisKey,"");
            stringRedisTemplate.expire(redisKey, BLOG_LIKE_IS_NULL_TTL, TimeUnit.MINUTES);
            return Result.success(new ArrayList<>());
        }
        //如果查到了，添加缓存
        blogIds.forEach(blogId -> {
            stringRedisTemplate.opsForSet().add(redisKey, blogId);
        });
        query.addCriteria(Criteria.where("_id").is(id));
        List<BlogDTO> blogDTOList = mongoTemplate.find(query, BlogDTO.class);
        if(!blogDTOList.isEmpty()){
            return Result.success(blogDTOList);
        }
        //查询用户喜欢的blog
        return Result.success(queryBlogByUserLike(blogIds));
    }

    @Override
    @Transactional
    public Result deleteBLog(int id) throws IOException {
        //修改数据库
        boolean updateSQL = this.update().eq("id", id).setSql("deleted = 0").update();
        if(!updateSQL){
            return Result.error("您要删除的博客不存在");
        }

        //删除blog，同时要移除es和mongoDB的blog信息，还要在mongoDB中移除该blog的下的所有评论
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, BlogDTO.class);
        //修改ES
        DeleteRequest deleteRequest = new DeleteRequest("blog", String.valueOf(id));
        client.delete(deleteRequest, RequestOptions.DEFAULT);
        //移除MongoDB的评论
        Query queryComment = new Query();
        queryComment.addCriteria(Criteria.where("blogId").is(id));
        mongoTemplate.remove(queryComment, CommentDTO.class);
        return Result.success("删除成功");
    }

    @Override
    public Result recycleBinBlog(int id) {
        LambdaQueryWrapper<TBlog> blogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLambdaQueryWrapper.eq(TBlog::getBlogger, id);
        blogLambdaQueryWrapper.eq(TBlog::getDeleted, 0);
        List<TBlog> list = this.list(blogLambdaQueryWrapper);
        List<BlogDTO> blogDTOList = populatingBlogDTO.PopulatingBlogDTOData(list);
        return Result.success(blogDTOList);
    }

    @Override
    public Result recoverBlog(BlogDTO blogDTO) throws IOException {
        blogDTO.setDeleted(1);
        //修改数据库
        boolean update = this.update().eq("id", blogDTO.getId()).setSql("deleted = 1").update();
        if(!update){
            return Result.error("网络异常");
        }
        //添加ES
        IndexRequest request = new IndexRequest("blog").id(String.valueOf(blogDTO.getId()));
        request.source(JSON.toJSONString(blogDTO), XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
        //添加MongoDB
        mongoTemplate.insert(blogDTO);
        return Result.success("恢复成功");
    }


    private List<BlogDTO> queryBlogByUserLike(Set<String> members) {
        LambdaQueryWrapper<TBlog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(TBlog::getId, members);
        List<TBlog> list = this.list(queryWrapper);
        return populatingBlogDTO.PopulatingBlogDTOData(list);

    }


}




