package com.aav.planner.service;

import com.aav.planner.annotation.SchedulerLogAndLock;
import com.aav.planner.service.lock.LockAction;
import com.aav.planner.service.log.LogAction;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ReflectionUtils.MethodCallback;

@RequiredArgsConstructor
public class SchedulerMethodCallback implements MethodCallback {

  private final ThreadPoolTaskScheduler taskScheduler;
  private final Object bean;
  private final LogAction logAction;
  private final LockAction lockAction;

  @Override
  public void doWith(Method method) throws IllegalArgumentException {

    if (method.isAnnotationPresent(SchedulerLogAndLock.class)) {
      Task task = new Task();
      task.init(method, taskScheduler, bean, logAction, lockAction);
    }

  }

}
