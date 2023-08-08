package com.ntx.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.service.TBlogService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class BlogUpdateQueryListener {
    @Autowired
    private TBlogService blogService;

    /**
     * kafka的监听器
     * @param record
     *
     */
    @KafkaListener(topics = "blog", groupId = "blog")
    public void topicListener1(ConsumerRecord<String, String> record) {
        String value = record.value();
        TBlog tBlog = JSON.parseObject(value, TBlog.class);
        tBlog.setClickCount(tBlog.getClickCount() + 1);
        blogService.updateBlodById(tBlog);
    }

}
