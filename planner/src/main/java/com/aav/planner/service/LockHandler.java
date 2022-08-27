package com.aav.planner.service;

import com.aav.planner.model.LockParam;
import com.aav.planner.service.lock.LockAction;
import com.aav.planner.service.log.LogAction;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockHandler {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final boolean isLockEnable; // lock is used
  private final LogAction logAction;
  private final LockAction lockAction;

  // this is the ID of the new entry in the logging log
  private UUID rowId;

  public LockHandler(boolean isLockEnable, LogAction logAction,
      LockAction lockAction) {
    this.isLockEnable = isLockEnable;
    this.logAction = logAction;
    this.lockAction = lockAction;
  }

  public boolean createLockAndLog(LockParam lockParam, String methodName) {
    rowId = UUID.randomUUID();

    if (isLockEnable) {
      LockParam currentLock = lockAction.getLockInfo(lockParam);

      if (currentLock == null || currentLock.getName() == null) {
        if (!lockAction.lock(lockParam)) {
          return false;
        }
      } else {
        if (currentLock.getLockUntil().isAfter(Instant.now())) {
          if (currentLock.getHost().equals(lockParam.getHost())) {
            if (!lockAction.updateLock(lockParam) && !lockAction.lock(lockParam)) {
              return false;
            }
          } else {
            return false;
          }
        } else {
          lockAction.unlock(lockParam);
          if (!lockAction.lock(lockParam)) {
            log.debug("Lock already exists!");
            return false;
          }
        }
      }

    }

    return logAction.create(rowId, methodName);
  }

  public boolean updateCurrentRow(Map<String, Object> info) {
    return logAction.update(rowId, info);
  }

}
