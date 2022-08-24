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
    if (isLockEnable && !lockAction.lock(lockParam)) {
      // if the lock already exists, check the deadline
      LockParam lockInfo = lockAction.getLockInfo(lockParam);
      if (lockInfo == null) {
        return false;
      }
      if (lockInfo.getLockUntil().isBefore(Instant.now()) && lockAction.unlock(lockParam)
          && !lockAction.lock(lockParam)) {
        log.debug("Lock already exists!");
        return false;
      }
    }
    return logAction.create(rowId, methodName);
  }

  public boolean updateCurrentRow(LockParam lockParam, Map<String, Object> info) {
    if (isLockEnable) {
      lockAction.unlock(lockParam);
    }
    return logAction.update(rowId, info);
  }

}
