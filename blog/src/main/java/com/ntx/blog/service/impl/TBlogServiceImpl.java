package com.ntx.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.mapper.TBlogMapper;
import com.ntx.blog.service.TBlogService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import com.ntx.common.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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


    @Override
    public int updateBlodById(TBlog blog) {
        return blogMapper.updateBlogById(blog);
    }

    /**
     *
     * @param pageSize
     * @param tBlog
     * @return
     */
    @Override
    public List<TBlog> getPage(Integer pageNum, Integer pageSize, TBlog tBlog) {

        int start = (pageNum - 1) * pageSize;
        return blogMapper.getPage(start, pageSize, tBlog);
    }

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

//    @Override
//    public Result saveBlog(TBlog blog) throws IOException {
//        //保存到数据库
//        blog.setGmtModified(LocalDateTime.now());
//        blog.setGmtModified(LocalDateTime.now());
//        blog.setComment(0);
//        blog.setLikeCount(0);
//        blog.setStampCount(0);
//        blog.setCollectCount(0);
//        blog.setClickCount(0);
//        blog.setDeleted(1);
//        blog.setStatus(1);
//        boolean save = save(blog);
//        if(!save){
//            return Result.error("保存失败");
//        }
//        //保存到ES
//        //创建request
//        IndexRequest request = new IndexRequest("blog").id(blog.getId().toString());
//        //准备json数据
//        request.source(JSON.toJSONString(blog), XContentType.JSON);
//        //发送请求
//        IndexResponse index = client.index(request, RequestOptions.DEFAULT);
//        return index.status().getStatus() == 201 ? Result.success("保存成功") : Result.error("保存失败");
//
//    }

}




