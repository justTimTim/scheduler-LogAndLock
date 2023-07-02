package com.aav.planner.service.log;

import com.aav.planner.model.ScheduleParams;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * available actions for logging events in the database
 */
public interface LogAction {

  /**
   * this method to create a new entry in the event log when the job starts
   *
   * @return true if successful
   */
  boolean create(UUID id, String name);

  /**
   * this method to update the results of running jobs
   *
   * @param id   log id
   * @param info useful information to save
   * @return true if successful
   */
  boolean update(UUID id, Map<String, Object> info);

  /**
   * query parameters written during the last job
   *
   * @param name method name
   * @return ScheduleParams
   */
  ScheduleParams getLastRow(String name);

  /**
   * Tries to find the last entry with the specified name and replace the values in it, if the entry
   * with the same name does not exist, then tries to create a new one.
   *
   * @param name method name
   * @return row id
   */
  Optional<UUID> replace(String name);

}
