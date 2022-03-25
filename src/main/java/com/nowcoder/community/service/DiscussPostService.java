package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${caffeine.posts.max-size}")
    private int maxCacheSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int cacheExpireSeconds;

    // 热帖列表缓存
    private LoadingCache<Integer, DiscussPost> hotPostCache;

    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        hotPostCache = Caffeine.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterWrite(cacheExpireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, DiscussPost>() {
                    @Override
                    public @Nullable DiscussPost load(@NonNull Integer id) throws Exception {
                        String redisKey = RedisKeyUtil.getHotPostKey(id);
                        DiscussPost post = (DiscussPost) redisTemplate.opsForValue().get(redisKey);
                        if (post == null) {
                            // 如果Redis缓存失效则从数据库中获取
                            List<DiscussPost> postList = discussPostMapper.selectDiscussPostsByScore(id, 1);
                            if (postList != null && postList.size() > 0) {
                                post = postList.get(0);
                                // 更新Redis
                                redisTemplate.opsForValue().set(redisKey, post, (long) cacheExpireSeconds, TimeUnit.SECONDS);
                            }
                        }
                        return post;
                    }
                });

    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit) {

        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }

    public List<DiscussPost> findHottestDiscussPosts(int offset, int limit) {

        int fromId = offset;
        int toId = offset + limit;

        // 如果请求的帖子在TOP-k热帖缓存范围内，则从缓存中获取
        if (fromId >= 0 && toId <= maxCacheSize) {
            logger.debug("load post from Cache");
            List<DiscussPost> list = new ArrayList<>();
            for (int i = fromId; i <= toId; i++) {
                DiscussPost post = hotPostCache.get(i);
                if (post != null) {
                    list.add(hotPostCache.get(i));
                }
            }
            return list;
        }

        return discussPostMapper.selectDiscussPostsByScore(offset, limit);
    }

    public int findDiscussPostRows(int userId) {
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost post) {
        if(post == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        // 转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        // 存入数据库
        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }

}
