package com.nowcoder.community.Controller.advice;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

// annotations用于指定Spring框架只去扫描Controller注解下的类
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器异常：" + e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }

        // 判断一下服务器收到的是普通网页请求，还是异步请求
        // 如果是异步请求，就将错误信息以JSON的格式返回给浏览器
        // 如果是普通页面请求，就重定向到错误页面
        // 通过请求头中的"x-requested-with"字段可以判断接收到的请求类型，如果是字段值是"XMLHttpRequest"说明是异步请求
        String xRequestedWith = request.getHeader("x-requested-with");
        if(xRequestedWith != null && xRequestedWith.equals("XMLHttpRequest")) {
            // 设置响应的内容为普通文本，字符集为utf-8
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常"));
        }
        else {
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }

}
