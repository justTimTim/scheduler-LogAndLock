package com.aav.planner.service;

import static com.aav.planner.service.DurationHandler.getDuration;
import static com.aav.planner.utility.Utils.HOST_NAME;

import com.aav.planner.annotation.SchedulerLogAndLock;
import com.aav.planner.exception.SchedulerLogAndLockException;
import com.aav.planner.model.LockParam;
import com.aav.planner.model.ScheduleParams;
import com.aav.planner.service.lock.LockAction;
import com.aav.planner.service.log.LogAction;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@RequiredArgsConstructor
public class Task {

  public void init(Method method, ThreadPoolTaskScheduler taskScheduler, Object bean,
      LogAction logAction, LockAction lockAction) {

    final Class<?> returnType = method.getReturnType();

    if (!returnType.isAssignableFrom(Map.class)) {
      throw new SchedulerLogAndLockException("The return type must be -> Map<String, Object>");
    }

    String cron = getCron(method);
    if (cron.equals("-")) {
      return;
    }

    CronTrigger cronTrigger = new CronTrigger(cron);
    final LockParam lockParam = createLockParam(bean, method);
    final String uniqueName = createUniqueName(bean, method);
    final boolean ifLockEnabled = method.getAnnotation(SchedulerLogAndLock.class).lock();

    taskScheduler.schedule(
        new TaskBody(logAction, lockAction, bean, method, ifLockEnabled, lockParam, uniqueName),
        cronTrigger);

  }

  private String createUniqueName(Object bean, Method method) {
    return bean.getClass().getName() + "." + method.getName();
  }

  private LockParam createLockParam(Object bean, Method method) {
    String lockUntil = method.getAnnotation(SchedulerLogAndLock.class).lockUntil();

    return new LockParam(
        createUniqueName(bean, method),
        Instant.now().plus(getDuration(lockUntil)),
        HOST_NAME);
  }

  private String getCron(Method method) {
    String cron = method.getAnnotation(SchedulerLogAndLock.class).cron();
    String[] s = cron.split(" ");
    if (s.length != 6 && !cron.equals("-")) {
      throw new SchedulerLogAndLockException("Invalid cron expression");
    }
    return cron;
  }


  static class TaskBody implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final boolean ifLockEnabled;
    private final LogAction logAction;
    private final LockAction lockAction;
    private final Object bean;
    private final Method method;
    private final LockParam lockParam;
    private final String name;

    public TaskBody(LogAction logAction, LockAction lockAction, Object bean,
        Method method, boolean ifLockEnabled, LockParam lockParam, String name) {
      this.logAction = logAction;
      this.lockAction = lockAction;
      this.bean = bean;
      this.method = method;
      this.lockParam = lockParam;
      this.name = name;
      this.ifLockEnabled = ifLockEnabled;
    }

    @Override
    public void run() {
      final LockHandler lockHandler = new LockHandler(ifLockEnabled, logAction, lockAction);
      try {
        boolean lockAndLog = lockHandler.createLockAndLog(lockParam, name);
        ScheduleParams lastParams = logAction.getLastRow(name);
        Map<String, Object> mapToSave = null;
        if (lockAndLog) {
          try {
            mapToSave = (Map<String, Object>) method.invoke(bean, lastParams);

            if (lockHandler.updateCurrentRow(lockParam, mapToSave)) {
              log.debug("Job {} successfully completed", name);
            }
          } catch (Exception e) {
            log.error("Exception while executing task {} ", name, e);
          }
        }
      } catch (Exception e) {
        log.error("It's a failure!", e);
      }
    }
  }
}
