package com.pixellink.scheduler;

import com.pixellink.mapper.LinkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class LinkCleanupScheduler {

    @Autowired
    private LinkMapper linkMapper;

    // 매일 자정 실행
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredLinks() {
        log.info("만료된 비회원 단축 링크 청소 작업을 시작합니다.");
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = linkMapper.deleteExpiredLinks(now);
        log.info("만료된 비회원 단축 링크 {}개를 정리했습니다.", deletedCount);
    }
}
