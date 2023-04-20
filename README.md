#Readme
## 用途

简化Springboot-web拦截器使用开发成本。 
通过注解方式+自定义实现类方式， 专注于业务开发。
代码上直接可以看出来业务逻辑，无需再查看MvcConfig拦截器配置。 

## 使用方式
依赖：
```xml
<dependency>
    <groupId>com.dutycode.springboot</groupId>
    <artifactId>springboot-interceptor-extends</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

1. 在WebConfig中初始化拦截器管理器`InterceptorManager.init(InterceptorRegistry registry, String... scanPackage)`。
参数说明： scanPackage 为要扫描的包路径，可指定多个。 可设定为工程的根包路径， 也可以分别设置注解 和Controller所在的包路径。
<br>
demo:

```java
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorManager.init(registry, "com.dutycode.demo", "com.dutycode.demo2");

    }

}

```
2. 在需要使用拦截器的类或者方法上使用`@BaseInterceptor(value= XXXX.class)`
3. 启动项目即可。

### 进阶用法
使用BaseInterceptor注解方式，可读性不是很直观。 可通过自定义注解方式来使得代码更易理解。 
比如，需要增加一个CORS的拦截器。 可以通过一下方式来实现。 
1. 自定义CROS注解
```java
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@BaseInterceptor(CorsInterceptor.class)
public @interface CORSAnno {

}
```
2. 实现Cors拦截器代码
```java
@Slf4j
@Component
public class CorsInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String origin = request.getHeader("Origin");
        log.info("preHandle.origin-->" + origin);
        if (StringUtils.isEmpty(origin))
            return true;
        String allowedUrl = null;
        for (String url : CorsWConfig.corsUrlList) {
            if (origin.startsWith(url)) {
                allowedUrl = url;
                break;
            }
        }
        if (allowedUrl != null) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Origin", allowedUrl);
            response.setHeader("Access-Control-Allow-Methods", "POST,OPTIONS,GET");
            response.setHeader("Access-Control-Allow-Headers", "accept,x-requested-with,Content-Type,X-Custom-Header");
            response.setHeader("Access-Control-Max-Age", "3600");
        }
        return true;
    }
}
```
3. 在需要拦截的Class上添加`CORSAnno`注解。
```java
@Slf4j
@RestController
@RequestMapping("demo")
@CORSAnno
public class DemoController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @GetMapping("hello")
    public String helloWorld(@RequestParam String user) {
        return " say hello world.";
    }
    

}
```

## Enjoy !

## 发布到maven center
https://issues.sonatype.org/projects/OSSRH/issues/OSSRH-90762?filter=allissues
