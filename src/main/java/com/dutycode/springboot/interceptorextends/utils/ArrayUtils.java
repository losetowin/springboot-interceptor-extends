package com.dutycode.springboot.interceptorextends.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ArrayUtils {

    /**
     * 数组求并集，并返回
     *
     * @param clazz 数组类型
     * @param obj   多个数组
     * @param <T>
     * @return
     */
    public static <T> T[] union(Class<?> clazz, T[]... obj) {

        List<T> list = new ArrayList<>();
        int len = 0;
        if (obj == null) {
            return null;
        }
        for (T[] t : obj) {
            if (t == null) {
                continue;
            }
            for (T tt : t) {
                if (tt == null) {
                    continue;
                }
                list.add(tt);
            }
        }

        T[] arr = (T[]) Array.newInstance(clazz, list.size());

        return list.toArray(arr);

    }

}
