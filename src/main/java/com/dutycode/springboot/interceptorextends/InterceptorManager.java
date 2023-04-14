package com.dutycode.springboot.interceptorextends;

import com.dutycode.springboot.interceptorextends.anno.BaseInterceptor;
import com.dutycode.springboot.interceptorextends.utils.ArrayUtils;
import com.dutycode.springboot.interceptorextends.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterceptorManager {


    private static Logger logger = LoggerFactory.getLogger(InterceptorManager.class);
    private static Map<String, Class<? extends HandlerInterceptor>> interAnnoMap = new HashMap<>();


    /**
     * 初始化拦截器配置
     * @param scanPackage  需要扫描的包, 可以配置多个
     */
    public static void init(InterceptorRegistry registry, String ... scanPackage){


        loadClazz(scanPackage);

        for (String path : scanPackage){

            addInterceptors(registry, path);

        }

    }


    private static void loadClazz(String ... scanPackage) {

        System.out.println("MVCConfig信息");
        //扫描全部包含Interceptor注解的注解(com.dutycode.*)
        List<String> annoClassNames = new ArrayList<>();
        for (String path : scanPackage){
            annoClassNames.addAll(ClassUtils.getClassName(path));
        }

        for (String annoClassName: annoClassNames){

            try {
                logger.info("LOAD {}  class", annoClassName);
                if (annoClassName.contains("BOOT-INF")){
                    annoClassName = annoClassName.replace("BOOT-INF.classes.", "");
                }
                Class annoClass = Class.forName(annoClassName);


                if (!annoClass.isAnnotation()){
                    continue;
                }

                BaseInterceptor interceptor = (BaseInterceptor) annoClass.getAnnotation(BaseInterceptor.class);
                if (interceptor != null){
                    //当前注解包含Interceptor注解,放入缓存中
                    interAnnoMap.put(annoClassName, interceptor.value());
                }

            }catch (Exception e){

            }


        }

    }

    public static void addInterceptors(InterceptorRegistry registry, String packageName) {

        List<String> classNames = ClassUtils.getClassName(packageName);

        if (classNames == null || classNames.size() == 0){
            return;
        }

        for (String className : classNames){

            try {
                if (className.contains("BOOT-INF")){
                    className = className.replace("BOOT-INF.classes.", "");
                }
                Class clazz = Class.forName(className);
                if (clazz == null){
                    continue;
                }

                //Step 1: START 处理类上 拦截器
                //注解上拦截器的实现类
                List<Class<? extends HandlerInterceptor>> interceptorClazzList = new ArrayList<>();
                //处理Class上注解
                BaseInterceptor clazzAnnotation = (BaseInterceptor) clazz.getAnnotation(BaseInterceptor.class);
                if (clazzAnnotation == null){
                    //检查是否有自定义注解
                    Annotation[] annotations = clazz.getAnnotations();
                    interceptorClazzList = getInterceptorClass(annotations);

                }else {
                    interceptorClazzList.add(clazzAnnotation.value());
                }

                List<String> basePaths = new ArrayList<>();
                //获取class上的RequestMapping
                RequestMapping annotation = (RequestMapping) clazz.getAnnotation(RequestMapping.class);

                if (annotation != null){
                    List<String> paths = new ArrayList<>();

                    //RequestMapping中有两个可以标识路径的方式，path或者value。取两者的并集
                    String[] arr  = ArrayUtils.union(String.class, annotation.path(), annotation.value());

                    for (String p : arr){
                        basePaths.add(p);
                        paths.add(p + "/*");
                    }

                    for (Class<? extends HandlerInterceptor> interceptorClazz : interceptorClazzList){
                        if (interceptorClazz != null ){

                            logger.info("添加类拦截器：{}， Path:{}", interceptorClazz.getName(), annotation.path().toString());
                            //添加拦截器实现
                            registry.addInterceptor(interceptorClazz.newInstance()).addPathPatterns(paths);

                        }
                    }


                }

                //Step 1 END: 处理类上 拦截器

                //Step 2: START 处理方法上拦截器
                //获取当前类的方法
                Method[] methods = clazz.getMethods();

                for (Method method : methods){

                    List<Class<? extends HandlerInterceptor>> methodInterceptorClazzList = new ArrayList<>();
                    BaseInterceptor methodAnnotation = (BaseInterceptor) method.getAnnotation(BaseInterceptor.class);

                    if (methodAnnotation == null){
                        //检查是否有自定义注解
                        methodInterceptorClazzList = getInterceptorClass(method.getAnnotations());

                    }else {
                        methodInterceptorClazzList.add(methodAnnotation.value());
                    }


                    RequestMapping methodRequestMappingAnno = (RequestMapping) method.getAnnotation(RequestMapping.class);

                    if (methodRequestMappingAnno != null){

                        List<String> methodsPath = new ArrayList<>();


                        //RequestMapping中有两个可以标识路径的方式，path或者value。取两者的并集
                        String[] requestMappingArr = ArrayUtils.union(String.class, methodRequestMappingAnno.path(), methodRequestMappingAnno.value());

                        for (String mPath : requestMappingArr){
                            if (basePaths != null && basePaths.size() > 0){
                                for (String base: basePaths){
                                    methodsPath.add(base + mPath );
                                }
                            }else {
                                methodsPath.add(mPath);
                            }
                        }

                        for (Class<? extends HandlerInterceptor> methodInterceptorClazz : methodInterceptorClazzList){

                            logger.info("添加方法拦截器：{}， Path:{}", methodInterceptorClazz.getName(), methodsPath.toString());
                            System.out.println("添加方法拦截器：" + methodInterceptorClazz.getName() + "Path:" + methodsPath.toString());
                            //添加拦截器实现
                            registry.addInterceptor(methodInterceptorClazz.newInstance()).addPathPatterns(methodsPath);
                        }

                    }

                    //Step 2 END : 处理方法上拦截器 END

                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }

        }


    }



    private static  List<Class<? extends HandlerInterceptor>> getInterceptorClass(Annotation[] annotations){


        List<Class<? extends HandlerInterceptor>> interceptorClazzList = new ArrayList<>();

        for (Annotation an : annotations){
            String annoClassName = an.annotationType().getName();

            if (interAnnoMap.containsKey(annoClassName)){
                //当前注解，存在拦截器
                Class<? extends HandlerInterceptor> interceptorClazz = interAnnoMap.get(annoClassName);
                interceptorClazzList.add(interceptorClazz);

            }
        }
        return interceptorClazzList;
    }




}
