package com.aav.planner.service.lock;

import com.aav.planner.model.LockParam;

/**
 * available actions for blocking jobs through the database
 */
public interface LockAction {

  /**
   * create lock
   *
   * @param param params
   * @return true if successful
   */
  boolean lock(LockParam param);

  /**
   * update lock
   *
   * @param param params
   * @return true if successful
   */
  boolean updateLock(LockParam param);


  /**
   * unlock
   *
   * @param param params
   * @return true if successful
   */
  boolean unlock(LockParam param);

  /**
   * getting information about an existing lock
   *
   * @param param params
   * @return information about lock
   */
  LockParam getLockInfo(LockParam param);

}
