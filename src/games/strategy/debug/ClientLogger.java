package games.strategy.debug;

import java.io.PrintStream;

public class ClientLogger {
  private static final PrintStream developerOutputStream = System.out;
  private static final PrintStream userOutputStream = System.err;

  public static void logQuietly(final Throwable e) {
    log(developerOutputStream, e);
  }

  private static void log(final PrintStream stream, final Throwable e) {
    stream.println("Exception: " + e.getMessage());
    for (final StackTraceElement stackTraceElement : e.getStackTrace()) {
      stream.println(stackTraceElement.toString());
    }
  }

  public static void logQuietly(final String msg) {
    developerOutputStream.println(msg);
  }

  public static void logError(final Exception e) {
    log(userOutputStream, e);
  }

  public static void logError(final String msg) {
    userOutputStream.println(msg);
  }

  public static void logQuietly(final String msg, final Exception e) {
    logQuietly(msg);
    logQuietly(e);
  }

}
