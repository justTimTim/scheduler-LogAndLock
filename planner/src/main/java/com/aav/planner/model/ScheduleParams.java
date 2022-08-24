package com.aav.planner.model;

import java.sql.Timestamp;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * information about the running time of the job and useful information transmitted by the user,
 * which is returned when the method is started again.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class ScheduleParams {

  private Timestamp start;
  private Timestamp end;
  private Map<String, Object> info;

}
