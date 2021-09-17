package com.zcp.util;

import java.security.PublicKey;
import java.util.Comparator;

/**
 * @author ：ZCP
 * @date ：2021/9/15
 * @description：堆实现的优先级队列
 * @version:
 */
public class PriorityQueue<E> implements Queue<E> {

    /**
     * 存放队列的元素
     */
    private Object[] queue;

    /**
     * 队列容量
     */
    private int capacity;

    /**
     * 当前队列中元素个数
     */
    private int size;

    private Comparator<E> comparator;

    /**
     * 默认的大小容量
     */
    private final static int DEFAULT_CAPACITY = 8;

    public PriorityQueue() {
        this.capacity = DEFAULT_CAPACITY;
        this.queue = new Object[capacity];
    }

    public PriorityQueue(int size) {
        this.capacity = size;
        this.queue = new Object[capacity];
    }

    public PriorityQueue(Comparator comparator) {
        this.capacity = DEFAULT_CAPACITY;
        this.queue = new Object[capacity];
        this.comparator = comparator;
    }

    public PriorityQueue(int size, Comparator comparator) {
        this.capacity = size;
        this.queue = new Object[capacity];
        this.comparator = comparator;
    }

    public Object[] getQueue() {
        return queue;
    }

    @Override
    public boolean add(E o) {
        if (size == capacity) {
            throw new IllegalStateException("队列满了");
        }
        enqueue(o);
        return true;
    }

    private void enqueue(E o) {
        queue[size++] = o;
        heapInsert(size - 1);
    }

    /**
     * 对指定下标位置的元素进行 调整
     *
     * @param index 可以不加这个参数，这是为了方便以后做加强堆
     */
    private void heapInsert(int index) {
        int pIndex = (index - 1) / 2;
        if (comparator != null) {
            //自带比较器的比较
            while (pIndex > 0) {
                //只要满足 index > 0  并且 当前节点 比 父节点小，交换
                if (comparator.compare((E) queue[index], (E) queue[pIndex]) >= 0) {
                    break;
                }
                swap(index, pIndex);
                index = pIndex;
                pIndex = (index - 1) / 2;
            }
            if (comparator.compare((E) queue[index], (E) queue[pIndex]) < 0) {
                swap(index, pIndex);
            }
        } else {
            //不带比较器的比较
            while (pIndex > 0) {
                Comparable<? super E> key = (Comparable<? super E>) queue[index];
                if (key.compareTo((E) queue[pIndex]) >= 0) {
                    break;
                }
                swap(index, pIndex);
                index = pIndex;
                pIndex = (index - 1) / 2;
            }
            Comparable<? super E> key = (Comparable<? super E>) queue[index];
            Object parent = queue[pIndex];
            if (key.compareTo((E) parent) < 0) {
                swap(index, pIndex);
            }
        }
    }

    private void swap(int i, int j) {
        Object tmp = queue[i];
        queue[i] = queue[j];
        queue[j] = tmp;
    }

    @Override
    public boolean offer(E o) {
        if (size == capacity) {
            //扩容
            addition();
        }
        enqueue(o);
        return true;
    }

    /**
     * 扩容
     */
    private void addition() {
        capacity = capacity << 1;
        Object[] newQueue = new Object[capacity];
        System.arraycopy(queue, 0, newQueue, 0, size);
        queue = newQueue;
    }

    /**
     * 检索并删除此队列的头，如果此队列为空，则返回 null 。
     *
     * @return
     */
    @Override
    public E poll() {
        if (size == 0) {
            return null;
        }
        return dequeue();
    }

    /**
     * 出队
     *
     * @return
     */
    private E dequeue() {
        Object ans = queue[0];
        //头 和 尾交换  然后 heapify
        swap(--size, 0);
        heapify(0);
        return (E) ans;
    }

    /**
     * 向下调整
     *
     * @param index
     */
    private void heapify(int index) {
        int left = index * 2 + 1;
        if (comparator != null) {
            //自带比较器的比较
            while (left < size) {
                int right = left + 1;
                if (right < size && comparator.compare((E) queue[right], (E) queue[left]) < 0 && comparator.compare((E) queue[right], (E) queue[index]) < 0) {
                    swap(right, index);
                    index = right;
                } else if (comparator.compare((E) queue[left], (E) queue[index]) < 0) {
                    swap(left, index);
                    index = left;
                } else {
                    //index 优先于 left和right
                    break;
                }
                left = index * 2 + 1;
            }
        } else {
            //不带比较器的比较
            while (left < size) {
                int right = left + 1;
                Comparable<? super E> leftKey = (Comparable<? super E>) queue[left];
                if (right < size && ((Comparable<? super E>) queue[right]).compareTo((E) queue[left]) < 0 && ((Comparable<? super E>) queue[right]).compareTo((E) queue[index]) < 0) {
                    swap(right, index);
                    index = right;
                } else if (leftKey.compareTo((E) queue[index]) < 0) {
                    swap(left, index);
                    index = left;
                } else {
                    break;
                }
                left = index * 2 + 1;
            }
        }
    }

    @Override
    public E peek() {
        if (size > 0) {
            return (E)queue[0];
        }
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public int getCapacity(){
        return capacity;
    }


}
