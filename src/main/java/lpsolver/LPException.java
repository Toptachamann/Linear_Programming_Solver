/** Created by Timofey on 8/9/2017. */
package lpsolver;

public class LPException extends Exception {
  public LPException() {}

  public LPException(String message) {
    super(message);
  }

  public LPException(String message, Throwable cause) {
    super(message, cause);
  }
}
