package com.ntx.blog.documentTest;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.service.TBlogService;
import com.ntx.client.BlogTypeClient;
import com.ntx.common.domain.TBlogType;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
public class DocumentTest {
    private RestHighLevelClient restClient;

    @Autowired
    private TBlogService blogService;
    @Autowired
    private BlogTypeClient blogTypeClient;

    @BeforeEach
    void setUp(){
        this.restClient = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("192.168.203.131:9200")
        ));
    }
    @AfterEach
    void close() throws IOException {
        restClient.close();
    }

    /**
     * 新增blog文档
     * @throws IOException
     */
    @Test
    void bulkDocument() throws IOException {
        //1.创建request
        BulkRequest request = new BulkRequest();
        //2.添加数据
        List<TBlog> list = blogService.list();
        List<Integer> typeIdList = list.stream().map(TBlog::getTypeId).collect(Collectors.toList());
        List<TBlogType> byTypeIds = blogTypeClient.getByTypeIds(typeIdList);
        Map<Integer, String> typeMap =
                byTypeIds.stream().collect(Collectors.toMap(TBlogType::getId, TBlogType::getName));
        //填充数据
        list.forEach((blog) -> {
            BlogDTO blogDTO = new BlogDTO();
            BeanUtil.copyProperties(blog, blogDTO);
            blogDTO.setTypeName(typeMap.get(blogDTO.getTypeId()));
            request.add(new IndexRequest("blog").
                    id(String.valueOf(blogDTO.getId())).
                    source(JSON.toJSONString(blogDTO), XContentType.JSON));
        });
        //3.发送请求
        restClient.bulk(request, RequestOptions.DEFAULT);
    }
}
