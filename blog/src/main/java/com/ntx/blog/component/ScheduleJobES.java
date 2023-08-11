package com.ntx.blog.component;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.service.TBlogService;
import com.ntx.common.client.BlogTypeClient;
import com.ntx.common.client.UserClient;
import com.ntx.common.domain.Result;
import com.ntx.common.domain.TBlogType;
import com.ntx.common.domain.TUser;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ScheduleJobES {
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private TBlogService blogService;
    @Autowired
    private BlogTypeClient blogTypeClient;
    @Autowired
    private UserClient userClient;
    @Autowired
    private MongoTemplate mongoTemplate;
    /**
     * 定时任务，两小时一次,定期向es中写入数据
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 2)
    public void fixedDelayTask() throws IOException {
        //1.创建request
        BulkRequest request = new BulkRequest();
        //2.添加数据
        List<TBlog> list = blogService.list();
        List<Integer> typeIdList = list.stream().map(TBlog::getTypeId).collect(Collectors.toList());
        List<Integer> bloggerIdList = list.stream().map(TBlog::getBlogger).collect(Collectors.toList());
        List<TBlogType> byTypeIds = blogTypeClient.getByTypeIds(typeIdList);
        System.out.println(byTypeIds);
        List<TUser> userList = userClient.getByIds(bloggerIdList);
        Map<Integer, TUser> userMap =
                userList.stream().collect(Collectors.toMap(TUser::getId, tUser -> tUser));
        Map<Integer, String> typeMap =
                byTypeIds.stream().collect(Collectors.toMap(TBlogType::getId, TBlogType::getName));
        //填充数据
        list.forEach((blog) -> {
            BlogDTO blogDTO = new BlogDTO();
            BeanUtil.copyProperties(blog, blogDTO);
            blogDTO.setBloggerId(blog.getBlogger());
            blogDTO.setTypeName(typeMap.get(blogDTO.getTypeId()));
            TUser user = userMap.get(blog.getBlogger());
            blogDTO.setBloggerImage(user.getImage());
            blogDTO.setBloggerId(user.getId());
            blogDTO.setBloggerName(user.getName());
            //更新mongoDB
            mongoTemplate.save(blogDTO);
            //index方式会替换掉原本的文档，create如果文档存在会返回错误，update是局部更新
            request.add(new IndexRequest("blog").
                    id(String.valueOf(blogDTO.getId())).
                    source(JSON.toJSONString(blogDTO), XContentType.JSON));
        });
        //3.发送请求
        client.bulk(request, RequestOptions.DEFAULT);
    }

}
