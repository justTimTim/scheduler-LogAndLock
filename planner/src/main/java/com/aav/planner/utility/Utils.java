package com.aav.planner.utility;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Utils {

  private Utils() {
  }

  public static final String HOST_NAME = initHostname();

  private static String initHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }
}
