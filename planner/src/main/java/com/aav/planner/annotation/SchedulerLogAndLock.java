package com.aav.planner.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface SchedulerLogAndLock {

  /**
   * takes a standard cron expression
   */
  String cron();

  /**
   * enable scheduler lock
   */
  boolean lock() default false;

  /**
   * the time for which the lock is guaranteed to be held
   */
  String lockUntil() default "0m";

}
