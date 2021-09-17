package com.zcp.util.concurrent;

import com.zcp.util.UnsafeUtils;
import com.zcp.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ：ZCP
 * @date ：2021/9/16
 * @description：线程池类
 * @version:
 */
public class ThreadPoolExecutor implements ExecutorService {


    /**
     * 核心线程数
     */
    private int corePoolSize;

    /**
     * 最大线程数
     */
    private int maximumPoolSize;

    /**
     * 最大等待时长
     */
    private long keepAliveTime;

    /**
     * 时间单位
     */
    private TimeUnit unit;

    /**
     * 等待队列
     */
    private BlockingQueue<Runnable> workQueue;

    /**
     * 线程创建工厂
     */
    private ThreadFactory threadFactory;

    /**
     * 线程池饱和策略
     */
    private RejectedExecutionHandler handler;

    /**
     * 当前线程数
     */
    private volatile int threadCount;

    /**
     * 线程池状态
     * RUNNING：-1
     * SHUTDOWN = 0
     * STOP = 1
     * TIDYING = 2
     * TERMINATED = 3
     */
    private volatile int ctl;

    /**
     * 0:为加锁
     * 1：加了锁
     */
    private volatile int opt;

    static final Unsafe unsafe = UnsafeUtils.getUnsafe();
    private static long ctlOffset;
    private static long optOffset;

    static {
        try {
            ctlOffset = unsafe.objectFieldOffset(ThreadPoolExecutor.class.getDeclaredField("ctl"));
            optOffset = unsafe.objectFieldOffset(ThreadPoolExecutor.class.getDeclaredField("opt"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


    /**
     * 构造方法
     *
     * @param corePoolSize    核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime   最大等待时间
     * @param unit            时间单位
     * @param workQueue       等待队列
     * @param threadFactory   线程创建工厂
     * @param handler         饱和策略
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.workQueue = workQueue;
        this.threadFactory = threadFactory;
        this.handler = handler;
        this.ctl = ThreadPoolState.RUNNING;
        this.threadCount = 0;
    }

    /**
     * 线程执行类
     *
     * @param command
     */
    @Override
    public void execute(Runnable command) {
        if (this.ctl != ThreadPoolState.RUNNING) {
            //如果线程池状态 不是 Running将不再接收任务
            return;
        }
        //抢锁
        for (; ; ) {
            if (unsafe.compareAndSwapInt(this, optOffset, 0, 1)) {
                //得到了线程池的操作权限
                //先判断当前线程数
                if (threadCount < corePoolSize) {
                    //如果当前线程数<核心线程数 创建线程，安排工作
                    addWorker(command, true);
                    threadCount++;
                } else if (threadCount < maximumPoolSize) {
                    //当前线程数>核心线程数  <最大线程数，并且队列满了，开始创建新的非核心线程
                    if (!workQueue.offer(command)) {
                        //队列满了 创建非核心线程
                        addWorker(command, false);
                        threadCount++;
                    }
                } else {
                    //队列满了，当前线程数也达到了最大线程数，交给拒绝策略处理
                    handler.handle(command, this);
                }
                unsafe.compareAndSwapInt(this, optOffset, 1, 0);
                break;
            }
        }
    }

    /**
     * 添加worker
     * 通过线程工厂创建线程，将线程和任务互相绑定
     *
     * @param command
     * @param core    是否是核心线程
     */
    private void addWorker(Runnable command, boolean core) {
        if (ctl != ThreadPoolState.RUNNING) {
            return;
        }
        Worker worker = new Worker(command, core);
        //将 worker 和 thread 相互关联
        Thread thread = threadFactory.newThread(worker, threadCount);
        worker.t = thread;
        //执行任务
        worker.t.start();
    }


    /**
     * 线程工作类
     *
     * @param worker
     */
    private void runWorker(Worker worker) {
        for (; ; ) {
            //先判断一下线程池的状态
            if (ctl != ThreadPoolState.RUNNING) {
                //如果已经调用了shutdown()方法，那么就不应该继续接受新的任务，最终所有线程都退出循环 threadCount会=0
                while (!unsafe.compareAndSwapInt(this, optOffset, 0, 1)) {
                }
                threadCount--;
                unsafe.compareAndSwapInt(this, optOffset, 1, 0);
                return;
            }
            //如果是非核心线程，需要判断 最后一次执行任务结束时间 到现在为止是否以及超出了最大空闲时间
            //如果超出了就结束循环，该线程就结束了
            //如果未超出，继续循环去执行任务
            long nanoTime = System.nanoTime();//当前系统时间
            if (!worker.core && nanoTime - worker.latestEndTime > unit.toNanos(keepAliveTime)) {
                //由于长时间没有任务，回收非核心线程
                while (!unsafe.compareAndSwapInt(this, optOffset, 0, 1)) {
                }
                threadCount--;
                unsafe.compareAndSwapInt(this, optOffset, 1, 0);
                return;
            }


            //下面是具体获取任务 处理任务的代码
            //如果该线程是第一次进来，那么会携带第一次提交的任务去执行，之后会从workQueue中获取任务执行
            //不管是第一次创建的线程带进来的任务 还是 之后从队列中获取的任务，都在这块执行
            Runnable firstTask = worker.runnable;
            Runnable task = null;
            while (firstTask != null || (task = getTask()) != null) {
                if (firstTask != null) {
                    firstTask.run();
                    //执行完第一个任务后需要置空，不然会一直执行firstTask
                    worker.runnable = null;
                    firstTask = null;
                } else if (task != null) {
                    task.run();
                    //每次任务执行结束，更新worker.latestEndTime
                    worker.latestEndTime = System.nanoTime();
                }
            }
        }

    }

    /**
     * 从队列中获取任务
     *
     * @return
     */
    private Runnable getTask() {
        return workQueue.poll();
    }

    @Override
    public void shutdown() {
        ctl = ThreadPoolState.SHUTDOWN;
    }

    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    /**
     * 线程池是否已经停止接受新任务
     *
     * @return
     */
    @Override
    public boolean isShutdown() {
        return ctl == ThreadPoolState.SHUTDOWN;

    }

    /**
     * 线程池所有线程是否都已经都跑完任务了
     *
     * @return
     */
    @Override
    public boolean isTerminated() {
        boolean isTerminated = false;
        while (!unsafe.compareAndSwapInt(this, ctlOffset, 0, 1)) ;
        if (threadCount == 0) {
            ctl = ThreadPoolState.TERMINATED;
            isTerminated = true;
        }
        unsafe.compareAndSwapInt(this, ctlOffset, 1, 0);

        return isTerminated;
    }

    /**
     * 线程池状态
     * RUNNING = -1
     * SHUTDOWN = 0
     * STOP = 1
     * TIDYING = 2
     * TERMINATED = 3
     */
    public static class ThreadPoolState {
        public static int RUNNING = -1;
        public static int SHUTDOWN = 0;
        public static int STOP = 1;
        public static int TIDYING = 2;
        public static int TERMINATED = 3;

    }

    /**
     * 工作者 有单独的线程
     * 不断的从队列中获取任务执行
     */
    public class Worker implements Runnable {

        /**
         * 具体的任务
         */
        Runnable runnable;

        /**
         * 执行任务的线程
         */
        Thread t;

        /**
         * 是否是核心线程
         */
        boolean core;

        /**
         * 最后一次任务执行结束的时间
         */
        long latestEndTime;


        public Worker(Runnable runnable, boolean core) {
            this.runnable = runnable;
            this.core = core;
            this.latestEndTime = System.nanoTime();
        }

        public Worker(Runnable runnable, Thread t) {
            this.runnable = runnable;
            this.t = t;
        }


        @Override
        public void run() {
            //先检查一下当前的线程池的状态
            runWorker(this);
        }

    }

    /**
     * 线程工厂类
     */
    public static class ThreadFactory {

        public Thread newThread(Worker worker, int threadCount) {
            Thread thread = new Thread(worker);
            thread.setName("Thread-Pool-ZCP-" + threadCount);
            return thread;
        }
    }

    /**
     * 饱和策略，这一块比较简单，可以根据自己的想法定义接口然后实现，这里就简单实现一个
     */
    public static class RejectedExecutionHandler {

        public void handle(Runnable runnable, ThreadPoolExecutor executor) {
            System.out.println("Task " + runnable.toString() +
                    " rejected from " +
                    executor.toString());
        }

    }
}
