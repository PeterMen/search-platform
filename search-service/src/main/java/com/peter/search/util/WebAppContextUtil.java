package com.peter.search.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * bean 集合
 *
 * @author 王海涛
 */
@Component
public class WebAppContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;


	@Autowired
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        storeApplicationContext(applicationContext);
    }


    private static void storeApplicationContext(ApplicationContext appCtx){
        WebAppContextUtil.applicationContext = appCtx;
    }

    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    public static boolean containsBean(String beanName) {
        return applicationContext.containsBean(beanName);
    }

}
