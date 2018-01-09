package lpsolver;

public class SolutionException extends LPException {
  public SolutionException() {}

  public SolutionException(String message) {
    super(message);
  }

  public SolutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
