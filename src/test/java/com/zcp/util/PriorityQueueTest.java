package com.zcp.util;

import java.util.Comparator;

import static org.junit.Assert.*;

public class PriorityQueueTest {

    public static void main(String[] args) {
//        addTest();

        offerAndPoll();

    }

    public static class Stu {
        String name;
        int age;

        public Stu(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Stu{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }

    private static void offerAndPoll() {

        PriorityQueue<Stu> priorityQueue = new PriorityQueue<Stu>(new Comparator<Stu>() {
            @Override
            public int compare(Stu o1, Stu o2) {
                return Integer.valueOf(o1.name) - Integer.valueOf(o2.name);
            }
        });
        for (int i = 0; i < 100; i++) {
            priorityQueue.offer(new Stu(i+"", (int) (Math.random() * 100) + 1));
        }

        for (int i = 0; i < 100; i++) {
            System.out.println(priorityQueue.poll());
        }

    }

    public static void addTest() {
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<Integer>(8);

        priorityQueue.add(2);
        priorityQueue.add(4);
        priorityQueue.add(1);
        priorityQueue.add(0);
        priorityQueue.add(8);
        System.out.println();

        System.out.println(priorityQueue.poll());
        System.out.println(priorityQueue.poll());
        System.out.println(priorityQueue.poll());
        System.out.println(priorityQueue.poll());
        System.out.println(priorityQueue.poll());

    }


}