package com.zcp.util.concurrent.locks;

import com.zcp.util.concurrent.locks.ReentrantLock;
import org.omg.PortableInterceptor.LOCATION_FORWARD;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ：ZCP
 * @date ：2021/9/14
 * @description：
 * @version:
 */
public class ReentrantLockTest {
    static volatile int count = 0;
    static volatile int count2 = 0;
    static int threadNum = 8;
    static int forTime = 100000;
    static int a = 0;

    public static void main(String[] args) throws InterruptedException {
        java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();
        long time1 = 0;
        long time2 = 0;
        int result1 = 0;
        int result2 = 0;
        ExecutorService es = Executors.newFixedThreadPool(threadNum);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (int j = 0; j < forTime; j++) {
                    try {
                        lock.lock();
                        ++count;
                        System.out.println(Thread.currentThread() + "-" + count);
                    } finally {
                        lock.unlock();
                    }
                }
            }
        };
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadNum; i++) {
            es.submit(r);
        }
        es.shutdown();
        while (!es.isTerminated()){}

        result1 = count;
        time1 = System.currentTimeMillis() - start;

        es = Executors.newFixedThreadPool(threadNum);
        count = 0;
        ReentrantLock lock2 = new ReentrantLock();
        Runnable r2 = new Runnable() {
            @Override
            public void run() {
                for (int j = 0; j < forTime; j++) {
                    try {
                        lock2.lock();
                        ++count;
                        System.out.println(Thread.currentThread() + "-" + count);
                    } finally {
                        lock2.unlock();
                    }
                }
            }
        };
        start = System.currentTimeMillis();
        for (int i = 0; i < threadNum; i++) {
            es.submit(r2);
        }
        es.shutdown();
        while (!es.isTerminated()){}
        result2 = count;
        time2 = System.currentTimeMillis() - start;

        System.out.println("JUC ReentrantLock执行结果：" + result1);
        System.out.println("JUC ReentrantLock执行时间：" + time1);

        System.out.println("ZCP ReentrantLock执行结果：" + result2);
        System.out.println("ZCP ReentrantLock执行时间：" + time2);
    }
}
