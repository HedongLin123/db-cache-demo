package com.itdl.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * @Description 通用配置及
 * @Author itdl
 * @Date 2022/08/09 17:57
 */
@Configuration
public class CommonConfig {
    @Bean("scheduledThreadPoolExecutor")
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        //线程名
        String threadNameStr = "统一可调度线程-%d";
        //线程工厂类就是将一个线程的执行单元包装成为一个线程对象，比如线程的名称,线程的优先级,线程是否是守护线程等线程；
        // guava为了我们方便的创建出一个ThreadFactory对象,我们可以使用ThreadFactoryBuilder对象自行创建一个线程.
        ThreadFactory threadNameVal = new ThreadFactoryBuilder().setNameFormat(threadNameStr).build();
        // 单线程池
        return new ScheduledThreadPoolExecutor(
                // 核心线程池
                4,
                // 最大线程池
                threadNameVal,
                // 使用策略为抛出异常
                new ThreadPoolExecutor.AbortPolicy());
    }
}
