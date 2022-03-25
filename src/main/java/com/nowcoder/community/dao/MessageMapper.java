package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

    // 查询当前用户的会话列表，针对每个会话只返回最新的一条私信
    // 因为可能有很多，所以要有分页
    List<Message> selectConversations(int userId, int offset, int limit);

    // 查询当前用户会话数量
    int selectConversationCount(int userId);

    // 查询某个会话含有的消息列表
    List<Message> selectLetters(String conversationId, int offset, int limit);

    // 查询某个私信包含的的消息数量
    int selectLetterCount(String conversationId);

    // 查询未读的消息数量
    // 注意这里的conversationId不一定必须要有值，有值的时候返回针对某个会话的未读数量，没有值的时候返回该用户所有的未读数量
    int selectLetterUnreadCount(int userId, String conversationId);

    // 添加私信
    int insertMessage(Message message);

    // 修改状态（未/已读）
    int updateStatus(List<Integer> ids, int status);

    // 查询某个主题下最新的通知
    Message selectLatestNotice(int userId, String topic);

    // 查询某个主题所包含的通知数量
    int selectNoticeCount(int userId, String topic);

    // 查询未读的通知数量
    // 这里topic可以为null，表示查询所有主题下总的未读数量
    int selectNoticeUnreadCount(int userId, String topic);

    // 查询某个主题包含的通知列表
    List<Message> selectNotices(int userId, String topic, int offset, int limit);

}
