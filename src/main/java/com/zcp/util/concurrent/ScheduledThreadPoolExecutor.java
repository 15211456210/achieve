package com.zcp.util.concurrent;


import com.zcp.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author ：ZCP
 * @date ：2021/9/17
 * @description：定时任务线程池类
 * @version:
 */
public class ScheduledThreadPoolExecutor implements ScheduledExecutorService {


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
    private BlockingQueue<Task> workQueue;

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
            ctlOffset = unsafe.objectFieldOffset(ScheduledThreadPoolExecutor.class.getDeclaredField("ctl"));
            optOffset = unsafe.objectFieldOffset(ScheduledThreadPoolExecutor.class.getDeclaredField("opt"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public ScheduledThreadPoolExecutor(int corePoolSize){
        this.corePoolSize = corePoolSize;
        this.workQueue = new PriorityBlockingQueue<Task>((o1, o2) -> (int) (o1.nextTime - o2.nextTime));//优先级阻塞队列
        this.threadFactory = new ScheduledThreadPoolExecutor.ThreadFactory();
        this.handler = new ScheduledThreadPoolExecutor.RejectedExecutionHandler();
        this.ctl = ThreadPoolState.RUNNING;
        this.threadCount = 0;
    }

    /**
     * 构造方法
     *
     * @param corePoolSize  核心线程数
     * @param unit          时间单位
     * @param threadFactory 线程创建工厂
     * @param handler       饱和策略
     */
    public ScheduledThreadPoolExecutor(int corePoolSize,
                                       TimeUnit unit,
                                       ThreadFactory threadFactory,
                                       RejectedExecutionHandler handler) {
        this.corePoolSize = corePoolSize;
        this.workQueue = new PriorityBlockingQueue<Task>((o1, o2) -> (int) (o1.nextTime - o2.nextTime));//优先级阻塞队列
        this.threadFactory = threadFactory;
        this.handler = handler;
        this.ctl = ThreadPoolState.RUNNING;
        this.threadCount = 0;
    }

    /**
     * 延迟delay的时间后执行一次任务
     *
     * @param command
     * @param delay
     * @param unit
     */
    @Override
    public void schedule(Runnable command, long delay, TimeUnit unit) {
        if (this.ctl != ThreadPoolState.RUNNING) {
            //如果线程池状态 不是 Running将不再接收任务
            return;
        }
        long nanoTime = System.nanoTime();//记录当前的时间
        //抢锁
        for (; ; ) {
            if (unsafe.compareAndSwapInt(this, optOffset, 0, 1)) {
                //得到了线程池的操作权限
                //先判断当前线程数
                if (threadCount < corePoolSize) {
                    //如果当前线程数<核心线程数 创建线程，安排工作
                    workQueue.offer(new Task(command, nanoTime + unit.toNanos(delay), 0, 0l));
                    addWorker(null, true);
                    threadCount++;
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
     * 从initialDelay后每个delay时间执行一次任务（不包含执行任务的时间）
     *
     * @param command
     * @param initialDelay
     * @param delay
     * @param unit
     */
    @Override
    public void scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (this.ctl != ThreadPoolState.RUNNING) {
            //如果线程池状态 不是 Running将不再接收任务
            return;
        }
        long nanoTime = System.nanoTime();//记录当前的时间
        //抢锁
        for (; ; ) {
            if (unsafe.compareAndSwapInt(this, optOffset, 0, 1)) {
                //得到了线程池的操作权限
                //先判断当前线程数
                if (threadCount < corePoolSize) {
                    //如果当前线程数<核心线程数 创建线程，安排工作
                    workQueue.offer(new Task(command, nanoTime + unit.toNanos(initialDelay), 1, unit.toNanos(delay)));
                    addWorker(null, true);
                    threadCount++;
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
     * @param command
     * @param initialDelay
     * @param period
     * @param unit
     */
    @Override
    public void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (this.ctl != ThreadPoolState.RUNNING) {
            //如果线程池状态 不是 Running将不再接收任务
            return;
        }
        long nanoTime = System.nanoTime();//记录当前的时间
        //抢锁
        for (; ; ) {
            if (unsafe.compareAndSwapInt(this, optOffset, 0, 1)) {
                //得到了线程池的操作权限
                //先判断当前线程数
                if (threadCount < corePoolSize) {
                    //如果当前线程数<核心线程数 创建线程，安排工作
                    workQueue.offer(new Task(command, nanoTime + unit.toNanos(initialDelay), 2, unit.toNanos(period)));
                    addWorker(null, true);
                    threadCount++;
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
     * 线程执行类
     *
     * @param command
     */
    @Override
    public void execute(Runnable command) {
    }

    /**
     * 添加worker
     * 通过线程工厂创建线程，将线程和任务互相绑定
     *
     * @param task
     * @param core 是否是核心线程
     */
    private void addWorker(Task task, boolean core) {
        if (ctl != ThreadPoolState.RUNNING) {
            return;
        }
        Worker worker = new Worker(task, core);
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

            //下面是具体获取任务 处理任务的代码
            //如果该线程是第一次进来，那么会携带第一次提交的任务去执行，之后会从workQueue中获取任务执行
            //不管是第一次创建的线程带进来的任务 还是 之后从队列中获取的任务，都在这块执行
            Task firstTask = worker.task;
            Task task = null;
            while (firstTask != null || (task = getTask()) != null) {

                if (firstTask != null) {
                    firstTask.runnable.run();
                    //执行完第一个任务后需要置空，不然会一直执行firstTask
                    worker.task = null;
                    firstTask = null;
                } else if (task != null) {
                    //实际上只会走进这个方法
                    //时间差(还需等待的时间)：如果大于0 说明还不能执行该方法 需要等待
                    long def = task.nextTime - System.nanoTime();
                    while (def > 0) {
                        LockSupport.parkNanos(def);
                        def = task.nextTime - System.nanoTime();
                    }
                    long startTime = System.nanoTime();
                    task.runnable.run();
                    //如果task type == 1 继续放入队列 等待下次执行
                    if (task.type == 1) {
                        task.nextTime = System.nanoTime() + task.betweenTime;
                        workQueue.offer(task);
                    } else if (task.type == 2) {
                        task.nextTime = startTime + task.betweenTime;
                        workQueue.offer(task);
                    }

                }

                //判断线程池是否shutdown
                if (ctl != ThreadPoolState.RUNNING) {
                    //如果已经调用了shutdown()方法，那么就不应该继续接受新的任务，最终所有线程都退出循环 threadCount会=0
                    while (!unsafe.compareAndSwapInt(this, optOffset, 0, 1)) {
                    }
                    threadCount--;
                    unsafe.compareAndSwapInt(this, optOffset, 1, 0);
                    return;
                }

            }
        }

    }

    /**
     * 从队列中获取任务
     *
     * @return
     */
    private Task getTask() {
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
        Task task;

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


        public Worker(Task task, boolean core) {
            this.task = task;
            this.core = core;
            this.latestEndTime = System.nanoTime();
        }

        public Worker(Task task, Thread t) {
            this.task = task;
            this.t = t;
        }


        @Override
        public void run() {
            //先检查一下当前的线程池的状态
            runWorker(this);
        }

    }

    /**
     * 优先级队列中存储的节点
     */
    public static class Task {

        /**
         * 任务对象
         */
        Runnable runnable;

        /**
         * 下一次执行开始时间
         */
        long nextTime;

        /**
         * 0：执行一次
         * 1：执行N次delay
         * 2.执行N次period
         */
        int type;

        /**
         * 间隔时间 nanos
         */
        long betweenTime;

        public Task(Runnable runnable, long nextTime, int type) {
            this.runnable = runnable;
            this.nextTime = nextTime;
            this.type = type;
        }

        public Task(Runnable runnable, long startTime, int type, long betweenTime) {
            this.runnable = runnable;
            this.nextTime = startTime;
            this.type = type;
            this.betweenTime = betweenTime;
        }
    }

    /**
     * 线程工厂类
     */
    public static class ThreadFactory {

        public Thread newThread(Worker worker, int threadCount) {
            Thread thread = new Thread(worker);
            thread.setName("ScheduleThread-Pool-ZCP-" + threadCount);
            return thread;
        }
    }

    /**
     * 饱和策略，这一块比较简单，可以根据自己的想法定义接口然后实现，这里就简单实现一个
     */
    public static class RejectedExecutionHandler {

        public void handle(Runnable runnable, ScheduledThreadPoolExecutor executor) {
            System.out.println("Task " + runnable.toString() +
                    " rejected from " +
                    executor.toString());
        }

    }


}
