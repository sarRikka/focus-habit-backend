package com.atomic.focus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 自控力与目标管理 APP 后端服务启动类。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan(basePackages = "com.atomic.focus.modules", markerInterface = BaseMapper.class)
public class AtomicFocusApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtomicFocusApplication.class, args);
    }
}
