package com.aav.planner.service;

import com.aav.planner.service.lock.LockAction;
import com.aav.planner.service.log.LogAction;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ReflectionUtils;

public class SchedulerPostProcessor implements BeanPostProcessor {

  private final ThreadPoolTaskScheduler taskScheduler;
  private final LogAction logAction;
  private final LockAction lockAction;
  private final Environment env;

  public SchedulerPostProcessor(ThreadPoolTaskScheduler taskScheduler, LogAction logAction,
      LockAction lockAction, Environment env) {
    this.taskScheduler = taskScheduler;
    this.logAction = logAction;
    this.lockAction = lockAction;
    this.env = env;
  }

  public SchedulerPostProcessor(ThreadPoolTaskScheduler taskScheduler, LogAction logAction,
      LockAction lockAction) {
    this.taskScheduler = taskScheduler;
    this.logAction = logAction;
    this.lockAction = lockAction;
    this.env = null;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    this.configureFieldInjection(bean);
    return bean;
  }

  private void configureFieldInjection(Object bean) {
    Class<?> managedBeanClass = bean.getClass();
    SchedulerMethodCallback callback = new SchedulerMethodCallback(taskScheduler, bean, logAction,
        lockAction, env);
    ReflectionUtils.doWithMethods(managedBeanClass, callback);
  }
}
