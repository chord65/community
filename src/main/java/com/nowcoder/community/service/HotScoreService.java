package com.nowcoder.community.service;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

@Service
public class HotScoreService implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(HotScoreService.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${community.site_creation_time}")
    private String siteCreationTime;


    @Scheduled(initialDelay = 30000, fixedRate = 30000)
    public void updatePostScore() {

        String redisKey = RedisKeyUtil.getPostScoreKey();
        Set<Integer> set = redisTemplate.opsForSet().members(redisKey);

        if (set != null) {
            for (int postId : set) {
                DiscussPost post = discussPostService.findDiscussPostById(postId);
                try {
                    double score = calculatePostScore(post);
                    // 更新分数
                    discussPostService.updateScore(postId, score);
                } catch (ParseException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    private double calculatePostScore(DiscussPost post) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        Date fromTime = dateFormat.parse(siteCreationTime);

        Date toTime = post.getCreateTime();

        // 计算帖子创建时间与建站时间的时间差，单位为天
        int timeDifference = (int) ((toTime.getTime() - fromTime.getTime()) / (1000 * 60 * 60 * 24));

        // 计算热度分数
        // 精华分数
        int wonderfulScore = post.getStatus() == 1 ? 100 : 0;
        // 评论数
        long commentCount = (long) post.getCommentCount();
        // 点赞数
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
        // 收藏数
        long followCount = followService.findFollowerCount(ENTITY_TYPE_POST, post.getId());

        double score = timeDifference + Math.log(wonderfulScore + commentCount * 10 + likeCount * 2 + followCount * 2);
        return score;
    }

}
