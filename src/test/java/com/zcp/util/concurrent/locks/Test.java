package com.zcp.util.concurrent.locks;

import java.util.concurrent.locks.LockSupport;

/**
 * @author ：ZCP
 * @date ：2021/9/15
 * @description：
 * @version:
 */
public class Test {
    public static void main(String[] args) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(1);
                    Thread.sleep(5000);
                    System.out.println(123);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        LockSupport.unpark(t);


    }
}
