package com.aav.planner.exception;

public class SchedulerLogAndLockException extends RuntimeException {

  public SchedulerLogAndLockException(String message) {
    super(message);
  }

  public SchedulerLogAndLockException(String message, Throwable throwable) {
    super(message, throwable);
  }

}
