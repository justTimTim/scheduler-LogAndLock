package com.aav.planner.service;

import com.aav.planner.exception.SchedulerLogAndLockException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationHandler {

  private DurationHandler() {
  }

  private static final Pattern PATTERN = Pattern.compile("^([\\+]?\\d{1,10})([a-zA-Z]{0,1})$");

  private static final Map<String, ChronoUnit> UNIT_MAP;

  static {
    UNIT_MAP = Map.of(
        "s", ChronoUnit.SECONDS,
        "m", ChronoUnit.MINUTES,
        "h", ChronoUnit.HOURS,
        "", ChronoUnit.SECONDS
    );
  }

  public static Duration getDuration(String value) {
    Matcher matcher = PATTERN.matcher(value);
    if (matcher.matches()) {
      long amount = Long.parseLong(matcher.group(1));
      ChronoUnit unit = getTemporal(matcher.group(2));
      return Duration.of(amount, unit);
    }
    return Duration.of(0, ChronoUnit.SECONDS);
  }

  private static ChronoUnit getTemporal(String key) {
    return Optional.of(UNIT_MAP.get(key))
        .orElseThrow(() -> new SchedulerLogAndLockException("wrong delay time value"));
  }
}
