package com.mmall.practice.example.redisson;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class RedLockExample {
    private static int clientTotal = 50;

    private static long count = 0;

    public static void main(String[] args) throws Exception {
        RedissonClient redisson = Redisson.create();

        RLock lock1 = redisson.getLock("lock1");
        RLock lock2 = redisson.getLock("lock2");
        RLock lock3 = redisson.getLock("lock3");

        // 同时加锁：lock1 lock2 lock3
        // 红锁在大部分节点上加锁成功就算成功。
        RedissonRedLock lock = new RedissonRedLock(lock1, lock2, lock3);

        final RCountDownLatch latch = redisson.getCountDownLatch("latch1");
        latch.trySetCount(clientTotal);

        ExecutorService exec = Executors.newCachedThreadPool();
        for (int index = 0; index < clientTotal; index++) {
            final int threadNum = index;
            exec.execute(() -> {
                try {
                    lock.lock();
                    try {
                        log.info("threadNum: {}", threadNum);
                        count++;
                    } finally {
                        lock.unlock();
                    }
                    latch.countDown();
                } catch (Exception e) {
                    log.error("exception", e);
                }
            });
        }
        latch.await();
        log.info("count:{}", count);
        exec.shutdown();
        redisson.shutdown();
    }
}
