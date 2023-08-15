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
import org.springframework.core.env.Environment;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@RequiredArgsConstructor
class Task {

  public void init(Method method, ThreadPoolTaskScheduler taskScheduler, Object bean,
      LogAction logAction, LockAction lockAction, Environment env) {

    final Class<?> returnType = method.getReturnType();

    if (!returnType.isAssignableFrom(Map.class)) {
      throw new SchedulerLogAndLockException("The return type must be -> Map<String, Object>");
    }

    String cron = getCron(method);
    if (cron.equals("-")) {
      return;
    } else if (cron.startsWith("$")) {
      if (env == null) {
        throw new SchedulerLogAndLockException(
            "Need to pass Environment to the constructor SchedulerPostProcessor");
      }
      var property = env.getProperty(cron.substring(2, cron.length() - 1));
      if (property == null) {
        throw new SchedulerLogAndLockException(
            String.format("Parameter '%s' not found in app config", cron));
      }
      cron = property;
    }


    Trigger cronTrigger = new CronTrigger(cron);
    var uniqueName = createUniqueName(bean, method);
    var ifLockEnabled = method.getAnnotation(SchedulerLogAndLock.class).lock();
    var ifReplaceLog = method.getAnnotation(SchedulerLogAndLock.class).replace();
    var lockUntil = method.getAnnotation(SchedulerLogAndLock.class).lockUntil();

    taskScheduler.schedule(
        new TaskBody(logAction, lockAction, bean, method, ifLockEnabled, ifReplaceLog, uniqueName,
            lockUntil), cronTrigger);

  }

  private String createUniqueName(Object bean, Method method) {
    String name = bean.getClass().getName() + "." + method.getName();
    return name.substring(name.length() - Math.min(name.length(), 100));
  }


  private String getCron(Method method) {
    String cron = method.getAnnotation(SchedulerLogAndLock.class).cron();
    String[] s = cron.split(" ");
    if (s.length != 6 && !cron.equals("-") && !cron.startsWith("$")) {
      throw new SchedulerLogAndLockException("Invalid cron expression");
    }
    return cron;
  }


  static class TaskBody implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final LogAction logAction;
    private final Object bean;
    private final Method method;
    private final String name;
    private final LockHandler lockHandler;
    private final String lockUntil;

    public TaskBody(LogAction logAction, LockAction lockAction, Object bean, Method method,
        boolean ifLockEnabled, boolean ifReplaceLog, String name, String lockUntil) {
      this.logAction = logAction;
      this.bean = bean;
      this.method = method;
      this.name = name;
      this.lockUntil = lockUntil;
      this.lockHandler = new LockHandler(ifLockEnabled, ifReplaceLog, logAction, lockAction);
    }

    private LockParam createLockParam() {
      return new LockParam(
          name,
          Instant.now().plus(getDuration(lockUntil)),
          HOST_NAME);
    }

    @Override
    public void run() {
      LockParam lockParam = createLockParam();
      try {
        boolean lockAndLog = lockHandler.createLockAndLog(lockParam, name);
        ScheduleParams lastParams = logAction.getLastRow(name);
        Map<String, Object> mapToSave;
        if (lockAndLog) {
          try {
            mapToSave = (Map<String, Object>) method.invoke(bean, lastParams);

            if (lockHandler.updateCurrentRow(mapToSave)) {
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

