package com.aav.planner.service;

import com.aav.planner.model.LockParam;
import com.aav.planner.service.lock.LockAction;
import com.aav.planner.service.log.LogAction;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockHandler {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final boolean isLockEnable; // lock is used
  private final boolean isReplaceLog;
  private final LogAction logAction;
  private final LockAction lockAction;

  // this is the ID of the new entry in the logging log
  private UUID rowId;

  public LockHandler(boolean isLockEnable, boolean isReplaceLog, LogAction logAction,
      LockAction lockAction) {
    this.isLockEnable = isLockEnable;
    this.isReplaceLog = isReplaceLog;
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
        if (tryUpdateLock(currentLock, lockParam)) {
          if (isReplaceLog) {
            return tryReplaceLog(methodName);
          }
          return logAction.create(rowId, methodName);
        } else {
          return false;
        }
      }
    }

    return isReplaceLog ? tryReplaceLog(methodName) : logAction.create(rowId, methodName);
  }

  private boolean tryReplaceLog(String methodName) {
    final Optional<UUID> replace = logAction.replace(methodName);
    if (replace.isPresent()) {
      rowId = replace.get();
      return true;
    } else {
      return false;
    }
  }

  public boolean updateCurrentRow(Map<String, Object> info) {
    return logAction.update(rowId, info);
  }

  private boolean tryUpdateLock(LockParam currentLock, LockParam lockParam) {
    if (currentLock.getLockUntil().isAfter(Instant.now())) {
      return tryLockIfCurrentHost(currentLock, lockParam);
    } else {
      lockAction.unlock(lockParam);
      if (!lockAction.lock(lockParam)) {
        log.debug("Lock already exists!");
        return false;
      } else {
        return true;
      }
    }
  }


  private boolean tryLockIfCurrentHost(LockParam currentLock, LockParam lockParam) {
    if (currentLock.getHost().equals(lockParam.getHost())) {
      return lockAction.updateLock(lockParam) || lockAction.lock(lockParam);
    } else {
      return false;
    }
  }

}
