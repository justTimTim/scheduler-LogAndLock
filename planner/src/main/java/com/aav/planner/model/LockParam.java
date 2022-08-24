package com.aav.planner.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * lock options.
 * "lockUntil" time until which the lock will be held
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class LockParam {

  private String name;
  private Instant lockUntil;
  private String host;
}
