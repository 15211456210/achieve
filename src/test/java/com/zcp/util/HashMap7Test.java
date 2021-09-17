package com.zcp.util;

import org.junit.Test;
import org.omg.PortableInterceptor.INACTIVE;

import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.*;

public class HashMap7Test {


    @Test
    public void test() {

        HashMap7<String, Integer> map7 = new HashMap7<String, Integer>();
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        int testTime = 1000000;
        int range = 200;
        System.out.println("测试开始");
        for (int i = 0; i < testTime; i++) {
            int opt = 0;//0:put,1:get,2:remove,3:contains,4:size
            do {
                opt = (int) (Math.random() * 5);
            } while (opt == 2 && map.size() == 0);
            int key = 0;
            int val = 0;
            Integer r1 = 0;
            Integer r2 = 0;
            switch (opt) {
                case 0:
                    key = (int) (Math.random() * range);
                    val = (int) (Math.random() * range);
//                        System.out.println("put:" + key + ":" + val);
                    map7.put(key + "", val);
                    map.put(key + "", val);
                    break;
                case 1:
                    key = (int) (Math.random() * range);
                    r1 = map.get(key + "");
                    r2 = map7.get(key + "");
//                        System.out.println("get:" + key);
                    if ((r1 == null && r2 != null) || (r1 != null && r2 == null) || (r1 != null && r2 != null && !r1.equals(r2))) {
                        System.out.println("出错了");
                    }
                    break;
                case 2:
                    key = (int) (Math.random() * range);
                    r1 = map.remove(key + "");
                    r2 = map7.remove(key + "");
//                        System.out.println("remove:" + key);
                    if ((r1 == null && r2 != null) || (r1 != null && r2 == null) || (r1 != null && r2 != null && !r1.equals(r2))) {
                        System.out.println("出错了");
                    }
                    break;
                case 3:
                    key = (int) (Math.random() * range);
                    if (map.containsKey(key + "") != map7.containsKey(key + "")) {
                        System.out.println("出错了");
                    }
                    break;
                case 4:
                    if (map.size() != map7.size()) {
                        System.out.println("出错了");
                    }
                    break;
            }
        }
        System.out.println("测试结束");


    }

    @Test
    public void test2() {
//        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
////        map.put(1,1);
//        map.remove(123);

        HashMap7<String, Integer> map7 = new HashMap7<String, Integer>();
        map7.put("Aa", 1);
        map7.put("BB", 1);
        System.out.println(map7.containsKey("65"));
    }

}