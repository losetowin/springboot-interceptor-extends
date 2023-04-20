package com.dutycode.springboot.interceptorextends;

import com.dutycode.springboot.interceptorextends.anno.BaseInterceptor;
import com.dutycode.springboot.interceptorextends.utils.ArrayUtils;
import com.dutycode.springboot.interceptorextends.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangzhonghua
 */
public class InterceptorManager {


    private static Logger logger = LoggerFactory.getLogger(InterceptorManager.class);
    private static Map<String, Class<? extends HandlerInterceptor>> interAnnoMap = new HashMap<>();


    /**
     * 初始化拦截器配置
     *
     * @param scanPackage 需要扫描的包, 可以配置多个
     */
    public static void init(InterceptorRegistry registry, String... scanPackage) {


        loadClazz(scanPackage);

        for (String path : scanPackage) {

            addInterceptors(registry, path);

        }

    }


    private static void loadClazz(String... scanPackage) {

        logger.info("扫描指定包下的类信息， scanPackage-->{}", scanPackage);
        List<String> annoClassNames = new ArrayList<>();
        for (String path : scanPackage) {
            annoClassNames.addAll(ClassUtils.getClassName(path));
        }

        for (String annoClassName : annoClassNames) {

            try {
                logger.debug("LOAD {}  class", annoClassName);
                if (annoClassName.contains("BOOT-INF")) {
                    annoClassName = annoClassName.replace("BOOT-INF.classes.", "");
                }
                Class annoClass = Class.forName(annoClassName);

                if (!annoClass.isAnnotation()) {
                    continue;
                }

                BaseInterceptor interceptor = (BaseInterceptor) annoClass.getAnnotation(BaseInterceptor.class);
                if (interceptor != null) {
                    //当前注解包含Interceptor注解,放入缓存中
                    interAnnoMap.put(annoClassName, interceptor.value());
                }

            } catch (Exception e) {
                logger.error("loadClass，处理{}注解异常", annoClassName);
            }


        }

    }

    public static void addInterceptors(InterceptorRegistry registry, String packageName) {

        List<String> classNames = ClassUtils.getClassName(packageName);

        if (classNames == null || classNames.size() == 0) {
            return;
        }

        for (String className : classNames) {

            try {
                if (className.contains("BOOT-INF")) {
                    className = className.replace("BOOT-INF.classes.", "");
                }
                Class clazz = Class.forName(className);
                if (clazz == null) {
                    continue;
                }

                List<String> basePaths = processClassInterceptors(registry, clazz);

                processMethodInteceptors(registry, clazz, basePaths);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }

        }


    }

    /**
     * 处理方法上的拦截器
     *
     * @param registry
     * @param clazz
     * @param basePaths
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private static void processMethodInteceptors(InterceptorRegistry registry, Class clazz, List<String> basePaths) throws InstantiationException, IllegalAccessException {
        //获取当前类的方法
        Method[] methods = clazz.getMethods();

        for (Method method : methods) {

            List<Class<? extends HandlerInterceptor>> methodInterceptorClazzList = new ArrayList<>();
            BaseInterceptor methodAnnotation = (BaseInterceptor) method.getAnnotation(BaseInterceptor.class);

            if (methodAnnotation == null) {
                //检查是否有自定义注解
                methodInterceptorClazzList = getInterceptorClass(method.getAnnotations());

            } else {
                methodInterceptorClazzList.add(methodAnnotation.value());
            }


            List<String> methodsPath = new ArrayList<>();

            //RequestMapping中有两个可以标识路径的方式，path或者value。取两者的并集
            String[] requestMappingArr = getRequestMappingInfo(method.getAnnotations());

            for (String mPath : requestMappingArr) {
                if (basePaths != null && basePaths.size() > 0) {
                    for (String base : basePaths) {
                        methodsPath.add(base + mPath);
                    }
                } else {
                    methodsPath.add(mPath);
                }
            }

            for (Class<? extends HandlerInterceptor> methodInterceptorClazz : methodInterceptorClazzList) {

                logger.info("添加方法拦截器：{}， Path:{}", methodInterceptorClazz.getName(), methodsPath.toString());
                //添加拦截器实现
                registry.addInterceptor(methodInterceptorClazz.newInstance()).addPathPatterns(methodsPath);
            }

        }
    }

    /**
     * 解析Request注解，获取注解中的path和value信息
     *
     * @param annos
     * @return
     */
    private static String[] getRequestMappingInfo(Annotation[] annos) {
        String[] mappingPath = null;
        String[] mappingValue = null;
        for (Annotation anno : annos) {
            if (RequestMapping.class == anno.annotationType()) {
                mappingPath = ((RequestMapping) anno).path();
                mappingValue = ((RequestMapping) anno).value();
            } else if (GetMapping.class == anno.annotationType()) {
                mappingPath = ((GetMapping) anno).path();
                mappingValue = ((GetMapping) anno).value();
            } else if (PostMapping.class == anno.annotationType()) {
                mappingPath = ((PostMapping) anno).path();
                mappingValue = ((PostMapping) anno).value();
            } else if (PutMapping.class == anno.annotationType()) {
                mappingPath = ((PutMapping) anno).path();
                mappingValue = ((PutMapping) anno).value();
            } else if (DeleteMapping.class == anno.annotationType()) {
                mappingPath = ((DeleteMapping) anno).path();
                mappingValue = ((DeleteMapping) anno).value();
            } else if (PatchMapping.class == anno.annotationType()) {
                mappingPath = ((PatchMapping) anno).path();
                mappingValue = ((PatchMapping) anno).value();
            }

        }

        String[] requestMappingArr = ArrayUtils.union(String.class, mappingPath, mappingValue);

        return requestMappingArr;
    }

    /**
     * 处理类上的拦截器
     *
     * @param registry
     * @param clazz
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private static List<String> processClassInterceptors(InterceptorRegistry registry, Class clazz) throws InstantiationException, IllegalAccessException {
        //Step 1: START 处理类上 拦截器
        if (null == clazz || clazz.isAnnotation()){
            //注解类不处理，仅处理class类
            return null;
        }

        //注解上拦截器的实现类
        List<Class<? extends HandlerInterceptor>> interceptorClazzList = new ArrayList<>();
        //处理Class上注解
        BaseInterceptor clazzAnnotation = (BaseInterceptor) clazz.getAnnotation(BaseInterceptor.class);
        if (clazzAnnotation == null) {
            //检查是否有自定义注解
            Annotation[] annotations = clazz.getAnnotations();
            interceptorClazzList = getInterceptorClass(annotations);

        } else {
            interceptorClazzList.add(clazzAnnotation.value());
        }

        List<String> basePaths = new ArrayList<>();
        List<String> paths = new ArrayList<>();

        //RequestMapping中有两个可以标识路径的方式，path或者value。取两者的并集
        String[] arr = getRequestMappingInfo(clazz.getAnnotations());

        for (String p : arr) {
            if (p != null && p.endsWith("/")){
                basePaths.add(p);
            }else {
                basePaths.add(p + "/");
            }
            paths.add(p + "/*");
        }

        for (Class<? extends HandlerInterceptor> interceptorClazz : interceptorClazzList) {
            if (interceptorClazz != null) {

                logger.info("添加类拦截器：{}， Path:{}", interceptorClazz.getName(), paths);
                //添加拦截器实现
                registry.addInterceptor(interceptorClazz.newInstance()).addPathPatterns(paths);

            }
        }

        //Step 1 END: 处理类上 拦截器
        return basePaths;
    }


    private static List<Class<? extends HandlerInterceptor>> getInterceptorClass(Annotation[] annotations) {

        List<Class<? extends HandlerInterceptor>> interceptorClazzList = new ArrayList<>();

        for (Annotation an : annotations) {
            String annoClassName = an.annotationType().getName();

            if (interAnnoMap.containsKey(annoClassName)) {
                //当前注解，存在拦截器
                Class<? extends HandlerInterceptor> interceptorClazz = interAnnoMap.get(annoClassName);
                interceptorClazzList.add(interceptorClazz);

            }
        }
        return interceptorClazzList;
    }


}
