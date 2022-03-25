package com.nowcoder.community.service;

import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    public void saveDiscussPost(DiscussPost post) {
        discussRepository.save(post);
    }

    public void deleteDiscussPost(int id) {
        discussRepository.deleteById(id);
    }

    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        // 构造查询条件
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 设置查询的内容，以及被查询的字段
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                // 设置结果的排序方式，优先按类型，其次分数，最后时间
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                // 设置分页
                .withPageable(PageRequest.of(current, limit))
                // 设置搜索结果中的高亮字段，通过preTags和postTags设置高亮部分前后的标签
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        return elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                // 获得搜索结果
                SearchHits hits = response.getHits();

                // 从hits中遍历所有搜索结果，封装进DiscussPost对象并存入list中
                List<DiscussPost> list = new ArrayList<>();
                if (hits != null) {
                    for (SearchHit hit : hits) {
                        DiscussPost post = new DiscussPost();

                        String id = hit.getSourceAsMap().get("id").toString();
                        post.setId(Integer.valueOf(id));

                        String userId = hit.getSourceAsMap().get("userId").toString();
                        post.setUserId(Integer.valueOf(userId));

                        String title = hit.getSourceAsMap().get("title").toString();
                        post.setTitle(title);

                        String content = hit.getSourceAsMap().get("content").toString();
                        post.setContent(content);

                        String status = hit.getSourceAsMap().get("status").toString();
                        post.setStatus(Integer.valueOf(status));

                        String createTime = hit.getSourceAsMap().get("createTime").toString();
                        post.setCreateTime(new Date(Long.valueOf(createTime)));

                        String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                        post.setCommentCount(Integer.valueOf(commentCount));

                        // 处理高亮显示的结果
                        HighlightField titleField = hit.getHighlightFields().get("title");
                        if (titleField != null) {
                            // getFragments返回的是一个数组，因为可能有多个匹配的字段，我们只选取第一个就好
                            post.setTitle(titleField.getFragments()[0].toString());
                        }

                        HighlightField contentField = hit.getHighlightFields().get("content");
                        if (contentField != null) {
                            post.setContent(contentField.getFragments()[0].toString());
                        }

                        list.add(post);
                    }
                    return new AggregatedPageImpl(list, pageable, hits.getTotalHits(), response.getAggregations(), hits.getMaxScore());
                }
                return null;
            }
        });
    }

}
