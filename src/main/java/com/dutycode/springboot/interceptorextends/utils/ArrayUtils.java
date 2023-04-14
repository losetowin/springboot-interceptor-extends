package com.dutycode.springboot.interceptorextends.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ArrayUtils {

    /**
     * 数组求并集，并返回
     * @param clazz 数组类型
     * @param obj 多个数组
     * @param <T>
     * @return
     */
    public static <T> T[] union(Class<?> clazz, T[] ...obj){

        List<T> list = new ArrayList<>();
        int len = 0;
        for (T[] t : obj){
           for (T tt : t){
               list.add(tt);
           }
        }

        T[] arr = (T[]) Array.newInstance(clazz,list.size());

        return list.toArray(arr);

    }


//    public static void main(String[] args) {
//        String[] a1 = new String[2];
//        a1[0]="ab1cd"; a1[1]="ab1cd";;
//        String[] a2 = new String[2];
//        a2[0]="ab2cd"; a2[1]="ab2cd";
//
//        String[] aaa = union(String.class, a1, a2);
//
//        System.out.println(aaa);
//    }
}
