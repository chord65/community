package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AlphaService {

    @Autowired
    private AlphaDao dao;

    private static final Logger logger = LoggerFactory.getLogger(AlphaService.class);

    public void init() {
        System.out.println("Service 初始化");
    }

    public String select() {
        System.out.println("Service select");
        return dao.select();
    }

    //@Async
    public void execute1() {
        logger.debug("execute1");
    }

    //@Scheduled(initialDelay = 3000, fixedRate = 1000)
    public void execute2() {
        logger.debug("execute2");
    }

}
