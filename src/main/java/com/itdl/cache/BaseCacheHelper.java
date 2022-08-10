package com.itdl.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class BaseCacheHelper<T> {
    @Resource
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    // 队列
    private final BlockingQueue<T> QUEUE = new ArrayBlockingQueue<>(1024);
    // listener执行次数 计数器
    private final AtomicInteger EXECUTE_COUNT = new AtomicInteger();
    // 事件集合
    private final List<T> eventStorageList = Collections.synchronizedList(new ArrayList<>());

    /**
     * 判断队列是否为空
     */
    public boolean checkQueueIsEmpty() {
        return QUEUE.isEmpty();
    }

    /**
     * 入队方法
     * @param datas 批量入队
     */
    public void producer(List<T> datas) {
        for (T data : datas) {
            producer(data);
        }
    }

    /**
     * 入队方法
     * @param data 单个入队
     */
    public void producer(T data) {
        try {
            if (QUEUE.contains(data)){
                return;
            }
            // 入队 满了则等待
            QUEUE.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("================>>>通用队列：{}：当前队列存在数据：{}", this.getClass().getName(), QUEUE.size());
    }


    @PostConstruct
    public void consumer() {
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
            try {
                // 队列数量达到指定消费批次数量
                if (EXECUTE_COUNT.get() >= getBatchSize()) {
                    doConsumer();
                } else {
                    while (EXECUTE_COUNT.get() < getBatchSize() && QUEUE.size() != 0) {
                        // 加入事件
                        final T take = QUEUE.take();
                        eventStorageList.add(take);
                        EXECUTE_COUNT.incrementAndGet();
                    }

                    // 队列为空了  同样需要处理，及时没有满
                    if (EXECUTE_COUNT.get() < getBatchSize() && QUEUE.size() == 0) {
                        doConsumer();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 3000, getPeriodTime(), TimeUnit.MILLISECONDS);
    }

    /**
     * 消费数据
     */
    protected void doConsumer() {
        // 这里面开始真正的写磁盘
        if (ObjectUtils.isEmpty(eventStorageList)) {
            return;
        }
        // 批处理
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("=========>>>>消费数据{}条", eventStorageList.size());
        for (T t : eventStorageList) {
            StopWatch subStopWatch = new StopWatch();
            subStopWatch.start();
            // 处理每一个消费者的逻辑 用于子类实现
            doHandleOne(t);
            subStopWatch.stop();
        }
        // 重置数据
        EXECUTE_COUNT.set(0);
        eventStorageList.clear();
        stopWatch.stop();
        log.info("=========>>>>通用队列：{}：消费完成，总耗时：{}s<<<<=========", this.getClass().getName(), String.format("%.4f", stopWatch.getTotalTimeSeconds()));
    }

    /**
     * 消费一条数据
     *
     * @param data 处理数据
     */
    protected abstract void doHandleOne(T data);


    /**
     * 批次大小 默认每次消费100条
     *
     * @return 此次大小
     */
    protected Integer getBatchSize() {
        return 100;
    }

    /**
     * 批次大小 执行完任务后 间隔多久再执行 单位 毫秒 默认5秒
     *
     * @return 此次大小
     */
    protected Integer getPeriodTime() {
        return 1000;
    }
}
