/*% if (feature.DM_DataExport) { %*/
package es.udc.lbd.gema.lps.web.rest.util;

/** Writes CSV (RFC 4180) for the data downloads. */
public final class CsvUtil {

  private static final String SEPARATOR = ",";
  private static final String LINE_END = "\r\n";
  /** A cell that starts like this would run as a formula when the file is opened in a spreadsheet. */
  private static final String FORMULA_STARTS = "=+-@";

  private CsvUtil() {}

  /** One row: cells quoted when they hold a separator, a quote or a line break; null is empty. */
  public static String row(Object... cells) {
    StringBuilder line = new StringBuilder();
    for (int i = 0; i < cells.length; i++) {
      if (i > 0) {
        line.append(SEPARATOR);
      }
      line.append(cell(cells[i]));
    }
    return line.append(LINE_END).toString();
  }

  static String cell(Object value) {
    if (value == null) {
      return "";
    }
    String text = value.toString();
    // Only text can be a formula: numbers (-5) and dates are written as they are
    if (value instanceof CharSequence && !text.isEmpty() && FORMULA_STARTS.indexOf(text.charAt(0)) >= 0) {
      text = "'" + text;
    }
    boolean quote =
        text.contains(SEPARATOR) || text.contains("\"") || text.contains("\n") || text.contains("\r");
    return quote ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
  }
}
/*% } %*/
