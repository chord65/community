package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息，用于代替Session对象
 * 使用ThreadLocal存放数据，使得数据可以被线程独有，从而避免多个请求间的用户信息出现冲突
 */
@Component
public class HostHolder {

    private ThreadLocal<User> userLocal = new ThreadLocal<User>();

    public void setUser(User user) {
        userLocal.set(user);
    }

    public User getUser() {
        return userLocal.get();
    }

    public void clear() {
        userLocal.remove();
    }
}
