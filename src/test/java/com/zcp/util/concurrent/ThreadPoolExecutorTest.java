package com.zcp.util.concurrent;

import javax.crypto.spec.PSource;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ThreadPoolExecutorTest {


    public static void main(String[] args) throws InterruptedException {
        ExecutorService es = new ThreadPoolExecutor(5,
                10,
                2000,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadPoolExecutor.ThreadFactory(),
                new ThreadPoolExecutor.RejectedExecutionHandler());

        for (int i = 0; i < 25; i++) {
            es.execute(new Task(i));
        }


        Thread.sleep(4000);
        System.out.println();
    }

    public static class Task implements Runnable {

        int val;

        public Task(int val) {
            this.val = val;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " print " + val);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}