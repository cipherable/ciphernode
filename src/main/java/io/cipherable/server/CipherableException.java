package io.cipherable.server;

public class CipherableException extends Exception {

  public CipherableException() {
    super();
  }

  public CipherableException(String message) {
    super(message);
  }

  public CipherableException(String message, Throwable cause) {
    super(message, cause);
  }

  public CipherableException(Throwable cause) {
    super(cause);
  }

  protected CipherableException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
