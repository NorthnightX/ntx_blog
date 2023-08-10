package com.ntx.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.domain.TComment;
import com.ntx.blog.domain.TLikeBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.mapper.TCommentMapper;
import com.ntx.blog.service.TBlogService;
import com.ntx.blog.service.TLikeBlogService;
import com.ntx.common.domain.Result;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.checkerframework.checker.units.qual.A;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

import static com.ntx.blog.common.SystemContent.BLOG_LIKE_KEY;
import static com.ntx.blog.common.SystemContent.BLOG_OPPOSE_KEY;

@Service
public class BlogKafkaQueryListener {
    @Autowired
    private TBlogService blogService;
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private TLikeBlogService likeBlogService;
    @Autowired
    private TCommentMapper commentMapper;
    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * kafka的监听器,增加文章阅读量
     * 功能已完成
     *
     * @param record
     */
    @KafkaListener(topics = "blogView", groupId = "blogView")
    public void topicListener1(ConsumerRecord<String, String> record) throws IOException {
        String value = record.value();
        int id = Integer.parseInt(value);
        TBlog tBlog = blogService.getById(id);
        tBlog.setClickCount(tBlog.getClickCount() + 1);
        //先改数据库
        blogService.updateBlodById(tBlog);
        //修改mongoDB
        Criteria criteria = Criteria.where("_id").is(tBlog.getId());
        Query query = new Query(criteria);
        Update update = new Update().set("clickCount", tBlog.getClickCount());
        mongoTemplate.updateFirst(query, update, BlogDTO.class);
        //修改es的点击量
        UpdateRequest request = new UpdateRequest("blog", String.valueOf(id));
        request.doc("clickCount", tBlog.getClickCount());
        client.update(request, RequestOptions.DEFAULT);
    }

    /**
     * 新增文章
     *
     * @param record
     * @throws IOException
     */
    @KafkaListener(topics = "blogAdd", groupId = "blogAdd")
    public void topicListener2(ConsumerRecord<String, String> record) throws IOException {
        String value = record.value();
        TBlog blog = JSON.parseObject(value, TBlog.class);
        //保存到数据库
        blog.setGmtModified(LocalDateTime.now());
        blog.setGmtModified(LocalDateTime.now());
        blog.setComment(0);
        blog.setLikeCount(0);
        blog.setStampCount(0);
        blog.setCollectCount(0);
        blog.setClickCount(0);
        blog.setDeleted(1);
        blog.setStatus(1);
        blogService.save(blog);
        //保存到ES
        //创建request
        IndexRequest request = new IndexRequest("blog").id(blog.getId().toString());
        //准备json数据
        request.source(JSON.toJSONString(blog), XContentType.JSON);
        //发送请求
        client.index(request, RequestOptions.DEFAULT);
    }


    /**
     * 文章评论
     *
     * @param record
     */
    @KafkaListener(topics = "blogComment", groupId = "blogComment")
    public void topicListener3(ConsumerRecord<String, String> record) throws IOException {
        String value = record.value();
        TComment comment = JSON.parseObject(value, TComment.class);
        comment.setDeleted(1);
        comment.setCreateTime(LocalDateTime.now());
        comment.setModifyTime(LocalDateTime.now());
        commentMapper.saveComment(comment);
        //评论完成后，将blog的评论数+1
        blogService.update().setSql("comment = comment + 1").eq("id", comment.getBlogId()).update();
        TBlog blog = blogService.getById(comment.getBlogId());
        //修改MongoDB评论数
        Criteria criteria = Criteria.where("_id").is(comment.getBlogId());
        Query query = new Query();
        query.addCriteria(criteria);
        Update update = new Update().set("comment", blog.getComment());
        mongoTemplate.updateFirst(query, update, BlogDTO.class);
        //修改ES的评论数
        UpdateRequest updateRequest = new UpdateRequest("blog", String.valueOf(comment.getBlogId()));
        updateRequest.doc("comment", blog.getComment());
        client.update(updateRequest, RequestOptions.DEFAULT);
    }


    /**
     * 文章点赞功能
     *
     * @param record
     */
    @KafkaListener(topics = "blogLike", groupId = "blogLike")
    @Transactional
    public void topicListener4(ConsumerRecord<String, String> record) {
        String value = record.value();
        TLikeBlog likeBlog = JSON.parseObject(value, TLikeBlog.class);
        String blogId = String.valueOf(likeBlog.getBlogId());
        //取消点赞
        if (record.key().equals("cancel")) {
            LambdaQueryWrapper<TLikeBlog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TLikeBlog::getBlogId, likeBlog.getBlogId());
            queryWrapper.eq(TLikeBlog::getUserId, likeBlog.getUserId());
            boolean remove = likeBlogService.remove(queryWrapper);
            if (remove) {
                //更新blog的点赞数量
                boolean update = blogService.update().setSql("like_count = like_count - 1").eq("id", likeBlog.getBlogId()).update();
                if (update) {
                    //修改Redis
                    stringRedisTemplate.opsForSet().remove(BLOG_LIKE_KEY + likeBlog.getUserId(), blogId);
                    //修改MongoDB
                    Query query = new Query();
                    query.addCriteria(Criteria.where("_id").is(likeBlog.getBlogId()));
                    Update updateMongoD = new Update().inc("likeCount", -1);
                    mongoTemplate.updateFirst(query, updateMongoD, BlogDTO.class);
                    //修改ES
                    ESUpdate(likeBlog.getBlogId(), -1);
                }
            }
        }
        //点赞
        else if (record.key().equals("like")) {
            likeBlog.setCreateTime(LocalDateTime.now());
            //点赞
            boolean save = likeBlogService.save(likeBlog);
            //更新blog的点赞数量
            if (save) {
                boolean update = blogService.update().setSql("like_count = like_count + 1").eq("id", likeBlog.getBlogId()).update();
                if (update) {
                    //向redis中添加用户的点赞信息
                    stringRedisTemplate.opsForSet().add(BLOG_LIKE_KEY + likeBlog.getUserId(), String.valueOf(likeBlog.getBlogId()));
                    //修改MongoDB
                    Query query = new Query();
                    query.addCriteria(Criteria.where("_id").is(likeBlog.getBlogId()));
                    Update updateMongoD = new Update().inc("likeCount", 1);
                    mongoTemplate.updateFirst(query, updateMongoD, BlogDTO.class);
                    //修改ES
                    ESUpdate(likeBlog.getBlogId(), 1);
                }
            }
        }
        //取消反对，点赞（反对还未实现）
        else if (record.key().equals("disLikeToLike")) {
            boolean update1 = likeBlogService.update().setSql("is_like = 1").
                    eq("blog_id", likeBlog.getBlogId()).eq("user_id", likeBlog.getUserId()).update();
            //更新成功，写入redis
            if (update1) {
                //删除用户反对set的数据
                stringRedisTemplate.opsForSet().remove(BLOG_OPPOSE_KEY + likeBlog.getUserId(), blogId);
                //向用户点赞set添加数据
                stringRedisTemplate.opsForSet().add(BLOG_LIKE_KEY + likeBlog.getUserId(), String.valueOf(likeBlog.getBlogId()));
                //修改MongoDB
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(likeBlog.getBlogId()));
                Update updateMongoD = new Update().inc("likeCount", 1);
                mongoTemplate.updateFirst(query, updateMongoD, BlogDTO.class);
                //修改ES
                ESUpdate(likeBlog.getBlogId(), 1);
            }
        }
    }


    /**
     * 修改ES
     *
     * @param blogId
     * @param ops
     * @return
     */
    private Boolean ESUpdate(Integer blogId, Integer ops) {
        try {
            // 准备request
            UpdateRequest request = new UpdateRequest("blog", String.valueOf(blogId));

            // 使用 Script 方式更新 like_count 字段
            Script inlineScript = new Script(ScriptType.INLINE, "painless",
                    "ctx._source.likeCount += params.value", Collections.singletonMap("value", ops));
            request.script(inlineScript);

            // 发送请求
            UpdateResponse response = client.update(request, RequestOptions.DEFAULT);

            // 根据更新结果判断操作是否成功
            if (response.getResult() == DocWriteResponse.Result.UPDATED) {
                // 更新成功
                return true;
            } else {
                // 更新失败
                return false;
            }
        } catch (ElasticsearchException | IOException ex) {
            // 更新操作发生异常
            ex.printStackTrace();
            return false;
        }
    }

}
