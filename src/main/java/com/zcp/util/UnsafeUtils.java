package com.zcp.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author ：ZCP
 * @date ：2021/9/13
 * @description：Unsafe工具类
 * @version:
 */
public class UnsafeUtils {

    private static Unsafe unsafe;

    public static Unsafe getUnsafe(){
        if(unsafe == null){
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return (Unsafe) field.get(null);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return unsafe;
    }



}
