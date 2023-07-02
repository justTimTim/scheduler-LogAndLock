package com.aav.planner.utility;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Utils {

  private Utils() {
  }

  public static final String HOST_NAME = initHostname();

  private static String initHostname() {
    try {
      String host = InetAddress.getLocalHost().getHostName();
      return host.substring(host.length() - Math.min(host.length(), 100));
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }
}
