package com.jrasp.agent.core.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 可路由的URLClassLoader
 *
 * @author luanjia@taobao.com
 */
public class RoutingURLClassLoader extends URLClassLoader {

    private final static Logger logger = Logger.getLogger(RoutingURLClassLoader.class.getName());
    private final ClassLoadingLock classLoadingLock = new ClassLoadingLock();
    private final Routing[] routingArray;

    public RoutingURLClassLoader(final URL[] urls,
                                 final Routing... routingArray) {
        super(urls);
        this.routingArray = routingArray;
    }

    public RoutingURLClassLoader(final URL[] urls,
                                 final ClassLoader parent,
                                 final Routing... routingArray) {
        super(urls, parent);
        this.routingArray = routingArray;
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (null != url) {
            return url;
        }
        url = super.getResource(name);
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = findResources(name);
        if (null != urls) {
            return urls;
        }
        urls = super.getResources(name);
        return urls;
    }


    @Override
    protected Class<?> loadClass(final String javaClassName, final boolean resolve) throws ClassNotFoundException {
        return classLoadingLock.loadingInLock(javaClassName, new ClassLoadingLock.ClassLoading() {
            @Override
            public Class<?> loadClass(String javaClassName) throws ClassNotFoundException {
                // 优先查询类加载路由表,如果命中路由规则,则优先从路由表中的ClassLoader完成类加载
                if ((routingArray != null && routingArray.length != 0)) {
                    for (final Routing routing : routingArray) {
                        if (!routing.isHit(javaClassName)) {
                            continue;
                        }
                        final ClassLoader routingClassLoader = routing.classLoader;
                        try {
                            return routingClassLoader.loadClass(javaClassName);
                        } catch (Exception cause) {
                            // 如果在当前routingClassLoader中找不到应该优先加载的类(应该不可能，但不排除有就是故意命名成同名类)
                            // 此时应该忽略异常，继续往下加载
                            // ignore...
                        }
                    }
                }

                // 先走一次已加载类的缓存，如果没有命中，则继续往下加载
                final Class<?> loadedClass = findLoadedClass(javaClassName);
                if (loadedClass != null) {
                    return loadedClass;
                }

                try {
                    Class<?> aClass = findClass(javaClassName);
                    if (resolve) {
                        resolveClass(aClass);
                    }
                    return aClass;
                } catch (Exception cause) {
                    DelegateBizClassLoader delegateBizClassLoader = BusinessClassLoaderHolder.getBusinessClassLoader();
                    try {
                        if (null != delegateBizClassLoader) {
                            return delegateBizClassLoader.loadClass(javaClassName, resolve);
                        }
                    } catch (Exception e) {
                        //忽略异常，继续往下加载
                    }
                    return RoutingURLClassLoader.super.loadClass(javaClassName, resolve);
                }
            }
        });
    }


    /**
     * 类加载路由匹配器
     */
    public static class Routing {

        private final Collection<String/*REGEX*/> regexExpresses = new ArrayList<String>();
        private final ClassLoader classLoader;

        /**
         * 构造类加载路由匹配器
         *
         * @param classLoader       目标ClassLoader
         * @param regexExpressArray 匹配规则表达式数组
         */
        Routing(final ClassLoader classLoader, final String... regexExpressArray) {
            if (regexExpressArray != null && regexExpressArray.length > 0) {
                regexExpresses.addAll(Arrays.asList(regexExpressArray));
            }
            this.classLoader = classLoader;
        }

        /**
         * 当前参与匹配的Java类名是否命中路由匹配规则
         * 命中匹配规则的类加载,将会从此ClassLoader中完成对应的加载行为
         *
         * @param javaClassName 参与匹配的Java类名
         * @return true:命中;false:不命中;
         */
        private boolean isHit(final String javaClassName) {
            for (final String regexExpress : regexExpresses) {
                try {
                    if (javaClassName.matches(regexExpress)) {
                        return true;
                    }
                } catch (Throwable cause) {
                    logger.log(Level.WARNING, "routing " + javaClassName + " failed, regex-express=" + regexExpress + ".", cause);
                }
            }
            return false;
        }

    }

}
