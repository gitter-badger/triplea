package games.strategy.triplea.ui;

public class StringUtil {
  public static String plural(final String aString, final int aCount) {
    if (aCount == 1) {
      return aString;
    } else {
      return aString + "s";
    }
  }

  private StringUtil() {}
}
