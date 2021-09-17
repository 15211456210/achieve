package com.zcp.util;

import java.util.*;

/**
 * @author ：ZCP
 * @date ：2021/9/12
 * @description：java7 hashMap简单实现 数组+单链表
 * @version:
 */
public class HashMap7<K, V> {

    private Node[] array;

    /**
     * 数组默认大小
     */
    private final static int INIT_ARRAY_SIZE = 8;

    /**
     * 扩容因子
     */
    private final static float FACTOR = 0.75f;

    private int size;

    public HashMap7() {
        this.array = new Node[INIT_ARRAY_SIZE];
        size = 0;
    }

    public final static class Node<K, V> {
        int hash;
        K key;
        V val;
        Node<K, V> next;

        public Node(int hash, K key, V val) {
            this.hash = hash;
            this.key = key;
            this.val = val;
        }
    }

    public final static class Values<V> extends AbstractCollection<V> {
        /**
         * 此处迭代器留给你们实现吧  嘿嘿、
         * @return
         */
        public Iterator<V> iterator() {
            return null;
        }

        public int size() {
            return size();
        }
    }

    public void put(K key, V val) {
        int length = array.length;
        int index = hash(key) & (length - 1);
        Node head = array[index];
        if (head == null) {
            Node<K, V> node = new Node<K, V>(hash(key), key, val);
            node.next = null;
            array[index] = node;
            size++;
        } else {
            //检查是否可以扩容了
            if (size > array.length * FACTOR) {
                transfer();//扩容
            }
            //先检索一边链表，是否已经存在Key
            Node findNode = get(hash(key),key);
            if (findNode != null) {
                //已存在直接 修改值
                findNode.val = val;
            } else {
                //不存在，插入新的节点
                Node<K, V> newNode = new Node<K, V>(hash(key), key, val);
                index = hash(key) & (array.length - 1);
                head = array[index];
                if (head != null) {
                    newNode.next = head;
                }
                array[index] = newNode;
                size++;
            }
        }

    }

    public Collection<V> values() {
        Values<V> vs = new Values<V>();
        for (int i = 0; i < array.length; i++) {
            Node<K, V> head = array[i];
            while (head != null) {
                vs.add(head.val);
                head = head.next;
            }
        }
        return vs;
    }

    public Set<K> keySet() {
        HashSet<K> vs = new HashSet<K>();
        for (int i = 0; i < array.length; i++) {
            Node<K, V> head = array[i];
            while (head != null) {
                vs.add(head.key);
                head = head.next;
            }
        }
        return vs;
    }

    /**
     * 将原来的数组扩容成2倍，并且重新计算hash
     */
    private void transfer() {
        Node<K, V>[] newArray = new Node[array.length << 1];
        for (int i = 0; i < array.length; i++) {
            Node head = array[i];
            while (head != null) {
                Node next = head.next;
                head.next = null;
                int index = hash(head.key) & (newArray.length - 1);
                Node<K, V> cur = newArray[index];
                if (cur != null) {
                    head.next = cur;
                }
                newArray[index] = head;
                head = next;
            }
        }
        array = newArray;
    }

    public V get(K key) {
        if (key == null) {
            throw new RuntimeException("key is null");
        }
        Node<K, V> node = get(hash(key),key);
        return node == null ? null : node.val;
    }

    public int size() {
        return size;
    }

    public V remove(K key) {
        if (!containsKey(key)) {
            return null;
        } else {
            int length = array.length;
            int index = hash(key) & (length - 1);
            Node<K, V> node = array[index];
            Node pre = null;
            Node<K, V> find = null;
            while (node != null) {
                if (node.hash == hash(key) && node.key.equals(key)) {
                    if (pre != null) {
                        pre.next = node.next;
                    } else {
                        //删除头节点
                        array[index] = node.next;
                    }
                    node.next = null;
                    size--;
                    return node.val;
                }
                pre = node;
                node = node.next;
            }
        }
        return null;
    }

    public boolean containsKey(K key) {
        int hash = hash(key);
        return get(hash,key) != null;
    }

    /**
     * 存在 不同字符串hashcode相同的情况，概率百万分之一，因此获取的时候还需要判断值是否真的相等。
     * @param hash
     * @return
     */
    private Node get(int hash,K key) {
        int length = array.length;
        int index = hash & (length - 1);
        Node node = array[index];
        if (node == null) {
            return null;
        }
        while (node != null) {
            if (node.hash == hash && node.key.equals(key)) {
                return node;
            }
            node = node.next;
        }
        return null;
    }

    /**
     * 核心hash算法
     * 对于 hashcode相同的 字符串的解决方案：使用equals()再加一层比较，在get()方法中体现
     * @param o
     * @return
     */
    private int hash(Object o) {
        int h;
        // h 和 高16位 亦或运算 可以使结果更加散列
        return (h = o.hashCode()) ^ (h >>> 16);
//        if (o instanceof Integer) {
//            return ((Integer) o).intValue();
//        } else if (o instanceof String) {
//            char[] chars = ((String) o).toCharArray();
//            int hash = 0;
//            for (int i = 0; i < chars.length; i++) {
//                hash = (hash << 31) +  chars[i];
//            }
//            return hash;
//        }else {
//            return o.hashCode();
//        }
    }


}
