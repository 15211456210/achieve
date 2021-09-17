package com.zcp.util;

/**
 * 队列接口
 * @param <E>
 */
public interface Queue<E> {

    boolean add(E e);

    boolean offer(E e);

    E poll();

    E peek();

    int size();

    boolean isEmpty();

}
