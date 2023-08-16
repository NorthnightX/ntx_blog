package com.ntx.blog.test;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.service.TBlogService;
import com.ntx.common.client.BlogTypeClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Field;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@SpringBootTest
public class DocumentTest {
    private RestHighLevelClient restClient;

    @Autowired
    private TBlogService blogService;
    @Autowired
    private BlogTypeClient blogTypeClient;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void testFindAll(){
        Query query = new Query();
        List<BlogDTO> blogDTOList = mongoTemplate.find(query, BlogDTO.class);
        blogDTOList.forEach(System.out::println);
    }

    @Test
    void testQuery(){
        Query query = new Query();
        // in查询
        Criteria criteriaIn = Criteria.where("_id").in(Arrays.asList(1, 2, 3));
        // and查询
        Criteria criteriaTitle = Criteria.where("title").is("数据库");
        Criteria criteriaStatus = Criteria.where("status").is(1);
        Criteria criteriaAnd = new Criteria().andOperator(criteriaTitle, criteriaStatus);
        // or查询
        Criteria criteriaOr = new Criteria().orOperator(criteriaTitle, criteriaStatus);
        // 逻辑运算符查询
        Criteria criteria = Criteria.where("clickCount").gt(10).lte(0);
        // 正则表达式查询
        String regex = "^好*";
        Criteria criteriaRegex = Criteria.where("title").regex(regex);
        BlogDTO blogDTO = mongoTemplate.findOne(query, BlogDTO.class);
        System.out.println(blogDTO);
    }



    @Test
    void page(){
        Query query = new Query();
        //从第10条开始，查3条
        query.skip(10).limit(3);
        //只查询title和clickCount两个字段
        query.fields().include("title").include("clickCount");
        //根据clickCount排序
        query.with(Sort.by(Sort.Direction.ASC, "clickCount"));
        List<BlogDTO> blogDTOList = mongoTemplate.find(query, BlogDTO.class);
        System.out.println(blogDTOList.size());
        blogDTOList.forEach(System.out::println);
    }

    @Test
    void insertOne(){
        BlogDTO blogDTO = new BlogDTO();
        // 如果数据库中存在和当前存储对象id相同的，会报错
        mongoTemplate.insert(blogDTO);
        // 如果数据库中存在和当前存储对象id相同的，会更新
        mongoTemplate.save(blogDTO);
    }

    @Test
    void insertMany(){
        List<BlogDTO> list = new ArrayList<>();
        mongoTemplate.insertAll(list);
    }

    @Test
    void deleteTest(){
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(22));
        // 删除符合条件的所有文档
        mongoTemplate.remove(query, BlogDTO.class);
        // 删除符合条件的所有文档并返回删除的文档
        mongoTemplate.findAllAndRemove(query, BlogDTO.class);
        // 删除符合条件的第一条文档并返回删除的文档
        mongoTemplate.findAndRemove(query, BlogDTO.class);
    }

    @Test
    void updateTest(){
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(22));
        Update update = new Update();
        // 字段自增
        update.inc("clickCount", 1);
        // 更新的字段
        update.set("title", "test");
        mongoTemplate.updateFirst(query, update, BlogDTO.class);
        mongoTemplate.updateMulti(query, update, BlogDTO.class);
    }
}
