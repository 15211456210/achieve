package com.zcp.util.concurrent.locks;

import com.zcp.util.PriorityQueue;
import com.zcp.util.concurrent.ArrayBlockingQueue;
import com.zcp.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.ArrayBlockingQueue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author ：ZCP
 * @date ：2021/9/14
 * @description：
 * @version:
 */
public class BlockingQueueTest {

    public static void main(String[] args) throws InterruptedException {
//        nanosTest();//过期时间测试
//        demoTest();//高并发测试
        priorityTest();
    }

    private static void priorityTest() throws InterruptedException {
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>();
        for (int i = 1; i <= 3; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int k = 0; k < 30; k++) {
                        Integer integer = 3 * k + Integer.valueOf(Thread.currentThread().getName());
                        System.out.println(Thread.currentThread() + " offer :: " + integer);
                        priorityQueue.offer(integer);
                    }
                }
            }, String.valueOf(i)).start();
        }

        Thread.sleep(1000);

        int count = 0;
        while (!priorityQueue.isEmpty()){
            System.out.println(priorityQueue.poll());
            count++;
        }
        System.out.println("count:"+count);

    }

    private static void demoTest() {
        //        ArrayBlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(10);
//        ArrayBlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(10);
        LinkedBlockingQueue<String> blockingQueue = new LinkedBlockingQueue<String>(10);
        for (int i = 0; i < 80; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int k = 0; k < 1000; k++) {
                        System.out.println(Thread.currentThread() + ": 生产:" + Thread.currentThread().getName() + "生产了" + k);
                        blockingQueue.put("" + Thread.currentThread().getName() + "生产的" + k);
                    }
                }
            }, "线程" + i).start();
        }


        for (int i = 0; i < 80; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int k = 0; k < 1000; k++) {
                        Object take = blockingQueue.take();
                        System.out.println(Thread.currentThread() + ": 消费:" + take);
                    }
                }
            }, "线程" + i).start();
        }
//        Thread.sleep(1000);
        System.out.println();
    }

    private static void nanosTest() {
        //        ArrayBlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(10);
//        ArrayBlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(10);
        LinkedBlockingQueue<String> blockingQueue = new LinkedBlockingQueue<String>(10);
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int k = 0; k < 15; k++) {
                        System.out.println(Thread.currentThread() + ": 生产:" + Thread.currentThread().getName() + "生产了" + k);
                        try {
                            blockingQueue.offer("" + Thread.currentThread().getName() + "生产的" + k, 5000, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            }, "线程" + i).start();
        }


        for (int i = 0; i < 9; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int k = 0; k < 15; k++) {
                        Object take = null;
                        try {
                            take = blockingQueue.poll(2000, TimeUnit.MILLISECONDS);
                            System.out.println(Thread.currentThread() + ": 消费:" + take);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }

                    }
                }
            }, "线程" + i).start();
        }
//        Thread.sleep(1000);
        System.out.println();
    }
}
