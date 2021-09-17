package com.zcp.util.concurrent;

import java.util.concurrent.TimeUnit;


public class ThreadPoolExecutorTest {


    public static void main(String[] args) throws InterruptedException {
        ExecutorService es = new ThreadPoolExecutor(5,
                10,
                2000,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadPoolExecutor.ThreadFactory(),
                new ThreadPoolExecutor.RejectedExecutionHandler());
//        ThreadPoolExecutor es = new java.util.concurrent.ThreadPoolExecutor(5,
//                10,
//                2000,
//                TimeUnit.MILLISECONDS,
//                new java.util.concurrent.ArrayBlockingQueue<>(10),
//                Executors.defaultThreadFactory(),
//                new ThreadPoolExecutor.AbortPolicy());

        for (int i = 0; i < 25; i++) {
            es.execute(new Task(i));
        }


        Thread.sleep(5000);
        System.out.println("在提交10个任务");
        for (int i = 0; i < 10; i++) {
            es.execute(new Task(i));
        }

//        es.shutdown();
//        while (!es.isTerminated()){}
        System.out.println("所有线程都已执行完，线程池关闭");


    }

    public static class Task implements Runnable {

        int val;

        public Task(int val) {
            this.val = val;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread() + " print " + val);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}