package com.zcp.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * 阻塞队列接口 （定义了一些队列的基本操作）
 */
public interface BlockingQueue<E> {


    boolean add(E e);

    boolean offer(E e);

    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    void put(E e) throws InterruptedException;

    E poll();

    E take() throws InterruptedException;

    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    E peek();

    int size();

    boolean isEmpty();

}
