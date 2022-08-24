package com.aav.planner.service;

import com.aav.planner.service.lock.LockAction;
import com.aav.planner.service.log.LogAction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ReflectionUtils;

@RequiredArgsConstructor
public class SchedulerPostProcessor implements BeanPostProcessor {

  private final ThreadPoolTaskScheduler taskScheduler;
  private final LogAction logAction;
  private final LockAction lockAction;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    this.configureFieldInjection(bean);
    return bean;
  }

  private void configureFieldInjection(Object bean) {
    Class<?> managedBeanClass = bean.getClass();
    SchedulerMethodCallback callback = new SchedulerMethodCallback(taskScheduler, bean, logAction,
        lockAction);
    ReflectionUtils.doWithMethods(managedBeanClass, callback);
  }
}
