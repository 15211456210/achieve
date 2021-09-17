package com.zcp.util.concurrent.locks;

import com.zcp.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.util.concurrent.locks.LockSupport;

/**
 * @author ：ZCP
 * @date ：2021/9/13
 * @description：基于CAS 无锁算法实现的 同步锁
 * @version:
 */
public class ReentrantLock {

    /**
     * 当前占有的线程
     */
    private Thread owner;

    /**
     * 标记位
     * 0：当前锁未被线程占有
     * 1：当前锁已被一个线程占有
     * >1：指当前锁被一个线程占有多次，state的值表示该线程加锁的次数（可重入）
     */
    private volatile int state = 0;

    /**
     * 阻塞队列标记位置
     * 0：阻塞队列当前没有线程在进行操作
     * 1：阻塞队列正在被某个线程操作
     */
    private volatile int waitStatus = 0;

    /**
     * 等待队列头节点
     */
    private Node head;

    /**
     * 等待队列尾巴
     */
    private Node tail;

    /**
     * 这些个是魔法类
     */
    static final Unsafe unsafe = UnsafeUtils.getUnsafe();
    private static long stateOffset;
    private static long waitStatusOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset(ReentrantLock.class.getDeclaredField("state"));
            waitStatusOffset = unsafe.objectFieldOffset(ReentrantLock.class.getDeclaredField("waitStatus"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加锁
     */
    public void lock() {
        acquire(1);
    }

    private void acquire(int val) {
        for (; ; ) {
            if (owner != null && owner == Thread.currentThread()) {
                //如果是当前得到锁的线程再次抢锁，直接state+1
                int c = state;
                unsafe.compareAndSwapInt(this, stateOffset, c, c + 1);
                break;
            } else if (unsafe.compareAndSwapInt(this, stateOffset, 0, val)) {
                //CAS抢到锁
                owner = Thread.currentThread();
                break;
            } else {
                //没有抢到锁的 进入等到队列 如果一进来就阻塞 会影响一部分性能
                //TODO 解决方案：如果抢不到锁，先自旋几次，多抢几次锁，如果依然失败才加入阻塞队列
                addWaiter();
            }
        }

    }

    /**
     * 将线程加入等待队列（将线程阻塞）
     */
    private void addWaiter() {
        for (; ; ) {
            if (unsafe.compareAndSwapInt(this, waitStatusOffset, 0, 1)) {
                Node node = new Node(Thread.currentThread());
                if (tail == null) {
                    //初始化等待队列
                    tail = node;
                    head = node;
                } else {
                    //将新节点添加至队列尾部
                    tail.next = node;
                    node.pre = tail;
                    tail = tail.next;
                }
                //线程释放锁 然后阻塞 等待唤醒
                unsafe.compareAndSwapInt(this, waitStatusOffset, 1, 0);
                LockSupport.park();
                //线程被唤醒跳出循环 继续尝试抢锁
                break;
            }
        }

    }

    /**
     * 解锁
     */
    public void unlock() {
        release(1);
    }

    /**
     * 释放标志位
     * @param val
     */
    private void release(int val) {
        int c = state - val;
        if (c <= 0) {
            c = 0;
            owner = null;
        }
        state = c;
        //如果等待队列不为空 则唤醒头节点
        unparkWaiter();
    }

    /**
     * 从等待队列唤醒阻塞线程
     */
    private void unparkWaiter() {
        while (!unsafe.compareAndSwapInt(this, waitStatusOffset, 0, 1)){}
        if (tail != null) {
            Node node = head;
            if (head == tail) {
                //只有一个节点时,清空
                head = null;
                tail = null;
            } else {
                Node next = head.next;
                //指针设置空，便于GC
                head.next = null;
                next.pre = null;
                //head节点移动到下一个节点
                head = next;
            }
            //释放锁，唤醒线程
            LockSupport.unpark(node.t);
        }
        unsafe.compareAndSwapInt(this, waitStatusOffset, 1, 0);
    }

    /**
     * 双向链表实现的等待队列
     * 封装了阻塞线程信息
     */
    public final static class Node {
        Thread t;
        Node pre;
        Node next;

        public Node(Thread t) {
            this.t = t;
        }

        public Node(Thread t, Node pre, Node next) {
            this.t = t;
            this.pre = pre;
            this.next = next;
        }
    }


}
