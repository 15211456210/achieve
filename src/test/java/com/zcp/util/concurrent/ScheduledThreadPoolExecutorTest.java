package com.zcp.util.concurrent;

//import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ScheduledThreadPoolExecutorTest {


    public static void main(String[] args) throws InterruptedException {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(10);

        scheduledThreadPoolExecutor.scheduleWithFixedDelay(new Task(1), 0, 3000, TimeUnit.MILLISECONDS);//5S一次
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new Task(2), 1000, 3000, TimeUnit.MILLISECONDS);//3S一次

        Thread.sleep(5000);


        scheduledThreadPoolExecutor.shutdown();

        while (!scheduledThreadPoolExecutor.isTerminated()){

        }
        System.out.println("线程池工作完毕");

    }

    public static class Task implements Runnable {

        int val;

        public Task(int val) {
            this.val = val;
        }

        @Override
        public void run() {
            System.out.println(new Date() + "" + Thread.currentThread() + "  "+val);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}