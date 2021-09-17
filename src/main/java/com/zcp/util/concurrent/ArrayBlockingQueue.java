package com.zcp.util.concurrent;

import com.zcp.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author ：ZCP
 * @date ：2021/9/14
 * @description：基于数组的阻塞队列
 * @version:
 */
public class ArrayBlockingQueue<E> implements BlockingQueue<E> {

    private Object[] queue;

    /**
     * 入队下标
     */
    private int inIndex;

    /**
     * 出队下标
     */
    private int outIndex;

    /**
     * 当前队列中元素个数
     */
    private int size;
    /**
     * 操作自身队列锁
     */
    private ReentrantLock lock = new ReentrantLock();

    /**
     * 生产者条件等待队列
     */
    private Condition<E> inCondition = new Condition<>();

    /**
     * 消费者条件等待队列
     */
    private Condition<E> outCondition = new Condition<>();

    public ArrayBlockingQueue(int size) {
        this.queue = new Object[size];
        this.size = 0;
        inIndex = 0;
        outIndex = 0;
    }

    /**
     * 添加元素
     * 如果入队成功返回true，如果队列满了抛出 IllegalStateException
     *
     * @param o
     * @return
     */
    @Override
    public boolean add(E o) {
        try {
            lock.lock();
            if (size == queue.length) {
                throw new IllegalStateException("队列满了");
            }
            enqueue(o);
            return true;
        } catch (IllegalStateException e) {
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 入队
     * 成功返回true ，队列满了返回false
     *
     * @param o
     * @return
     */
    @Override
    public boolean offer(E o) {
        try {
            lock.lock();
            if (size == queue.length) {
                return false;
            }
            enqueue(o);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 带等待时间的阻塞 入队方法
     * 如果队列满了，等待，知道时间到了 抛出InterruptedException异常
     *
     * @param o
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {
        try {
            long nanos = unit.toNanos(timeout);//最初的时间 2000000000
            lock.lock();
            while (size == queue.length) {
                //如果满了，加入生产者等待队列阻塞,并且提醒消费者进行消费
                inCondition.enq(o);
                signalOut();
                if (awaitNanos(nanos) < 0) {
                    //超时了，抛出异常
                    throw new InterruptedException("超时了");
                }
            }
            enqueue(o);
        } finally {
            //此处也需要去唤醒消费者 why？你们自己去调试就知道了
            signalOut();
            lock.unlock();
        }
        return false;
    }

    /**
     * 返回值如果是负数 代表已超时
     *
     * @param timeout
     * @return
     */
    private long awaitNanos(long timeout) {
        //释放锁
        lock.unlock();
        long deadLine = System.nanoTime() + timeout;//到期时间点
        //线程阻塞
        LockSupport.parkNanos(timeout);
        //当线程被唤醒时重新加锁
        lock.lock();
        long ans = deadLine - System.nanoTime();
        return ans;
    }

    /**
     * 阻塞方法
     * 入队成功返回true，如果队列满了阻塞等待
     *
     * @param o
     * @throws InterruptedException
     */
    @Override
    public void put(E o) {
        try {
            lock.lock();
            while (size == queue.length) {
                //如果满了，加入生产者等待队列阻塞,并且提醒消费者进行消费
                inCondition.enq(o);
                signalOut();
                await();
            }
            enqueue(o);
        } finally {
            //此处也需要去唤醒消费者 why？你们自己去调试就知道了
            signalOut();
            lock.unlock();
        }

    }

    /**
     * 入队
     *
     * @param o
     */
    private void enqueue(E o) {
        queue[inIndex] = o;
        inIndex = nextIndex(inIndex);
        size++;
    }

    /**
     * 等待唤醒，类似Object的await()
     * 释放当前锁，进入阻塞，等待被唤醒
     */
    private void await() {
        //释放锁
        lock.unlock();
        //线程阻塞
        LockSupport.park();
        //当线程被唤醒时重新加锁
        lock.lock();
    }

    /**
     * 去唤醒消费者线程
     */
    public void signalOut() {
        Node<E> signalNode = outCondition.signal();
        while (signalNode != null) {
            //去唤醒消费者线程
            LockSupport.unpark(signalNode.t);
            signalNode = outCondition.signal();
        }
    }

    /**
     * 去唤醒消费者线程
     */
    public void signalIn() {
        Node<E> signalNode = inCondition.signal();
        while (signalNode != null) {
            //去唤醒消费者线程
            LockSupport.unpark(signalNode.t);
            signalNode = signalNode = inCondition.signal();
        }
    }

    /**
     * 获取队头元素
     * 如果队列为空时返回null
     *
     * @return
     */
    @Override
    public E poll() {
        try {
            lock.lock();
            if (size > 0) {
                Object ans = dequeue();
                return (E)ans;
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回对头元素
     * 如果队列为空 阻塞等待知道队列不为空 返回
     *
     * @return
     * @throws InterruptedException
     */
    @Override
    public E take() {
        try {
            lock.lock();
            while (size == 0) {
                //队列为空，提醒生产者生产
                outCondition.enq(null);
                signalIn();
                await();
            }
            return (E)dequeue();
        } finally {
            signalIn();
            lock.unlock();
        }
    }

    /**
     * 出队
     *
     * @return
     */
    private Object dequeue() {
        Object ans = queue[outIndex];
        outIndex = nextIndex(outIndex);
        size--;
        return ans;
    }


    /**
     * 带超时时长的 出队
     * 如果队列为空 阻塞等待 直到超时 抛出InterruptedException异常
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            lock.lock();
            long nanos = unit.toNanos(timeout);
            while (size == 0) {
                //队列为空，提醒生产者生产
                outCondition.enq(null);
                signalIn();
                if (awaitNanos(nanos) < 0) {
                    //表示已超时
                    throw new InterruptedException("超时了");
                }
            }
            return (E) dequeue();
        } finally {
            signalIn();
            lock.unlock();
        }
    }

    @Override
    public E peek() {
        try {
            lock.lock();
            if (size > 0) {
                return (E) queue[0];
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    private int nextIndex(int index) {
        return (index + 1) < queue.length ? (index + 1) : 0;
    }

    /**
     * 条件阻塞队列
     */
    public static class Condition<E> {
        private ReentrantLock lock = new ReentrantLock();
        Node<E> head;
        Node<E> tail;

        //阻塞入队
        public void enq(Object e) {
            try {
                lock.lock();
                Node<E> node = new Node<>(e, Thread.currentThread());
                if (tail == null) {
                    head = node;
                    tail = node;
                } else {
                    tail.next = node;
                    node.pre = tail;
                    tail = node;
                }
            } finally {
                lock.unlock();
            }
        }

        //唤醒出队
        public Node<E> signal() {
            try {
                lock.lock();
                if (tail == null) {
                    return null;
                }
                if (tail == head) {
                    Node node = head;
                    head = null;
                    tail = null;
                    return node;
                } else {
                    Node pre = tail.pre;
                    pre.next = null;
                    tail.pre = null;
                    Node node = tail;
                    tail = pre;
                    return node;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public static class Node<E> {
        Object ele;
        Thread t;
        Node pre;
        Node next;

        public Node(Object ele, Thread t) {
            this.ele = ele;
            this.t = t;
        }
    }
}
