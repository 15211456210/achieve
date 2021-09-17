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
                    addWorker(command,true);
                    threadCount++;
                } else if (threadCount < maximumPoolSize) {
                    //当前线程数>核心线程数  <最大线程数，并且队列满了，开始创建新的非核心线程
                    if (!workQueue.offer(command)){
                        System.out.println("队列满了，创建新的非核心线程");
                        addWorker(command,false);
                        threadCount++;
                    }
                } else {
                    System.out.println("队列满了 线程数也达到了最大线程数，走拒绝策略");
                }
                unsafe.compareAndSwapInt(this, optOffset, 1, 0);
                break;
            }
        }
    }

    /**
     * 添加worker
     * 通过线程工厂创建线程，将线程和任务互相绑定
     * @param command
     * @param core 是否是核心线程
     */
    private void addWorker(Runnable command,boolean core) {
        if (ctl != ThreadPoolState.RUNNING) {
            return;
        }
        Worker worker = new Worker(command);
        //将 worker 和 thread 相互关联
        Thread thread = threadFactory.newThread(worker);
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
        //先判断一下线程池的状态
        if (ctl != ThreadPoolState.RUNNING) {
            return;
        }
        Runnable firstTask = worker.runnable;
        Runnable task = null;
        //不管是第一次创建的线程带进来的任务 还是 之后从队列中获取的任务，都在这块执行
        while (firstTask != null || (task = getTask()) != null) {
            if (firstTask != null) {
                firstTask.run();
                //执行完第一个任务后需要置空，不然会一直执行firstTask
                firstTask = null;
            } else if (task != null) {
                task.run();
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

    }

    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
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

        Runnable runnable;

        Thread t;

        ReentrantLock lock = new ReentrantLock();

        public Worker(Runnable runnable) {
            this.runnable = runnable;
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

        public Thread newThread(Worker worker) {
            Thread thread = new Thread(worker);
            thread.setName("Thread-Pool-ZCP");
            return thread;
        }
    }

    public static class RejectedExecutionHandler {

    }
}
