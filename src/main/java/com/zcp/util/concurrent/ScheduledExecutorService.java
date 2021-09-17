package com.zcp.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * 定时任务线程池接口类
 */
public interface ScheduledExecutorService extends ExecutorService {

    void schedule(Runnable command, long delay, TimeUnit unit);

    void scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);

    void scheduleAtFixedRate(Runnable command,long initialDelay,long period,TimeUnit unit);

}
