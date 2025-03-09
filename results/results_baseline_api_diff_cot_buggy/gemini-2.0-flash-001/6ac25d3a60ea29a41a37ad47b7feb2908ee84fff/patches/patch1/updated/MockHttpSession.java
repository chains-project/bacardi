import jakarta.servlet.http.HttpSessionContext;

public class MockHttpSession implements HttpSession {
  ...
  @Override
  public HttpSessionContext getSessionContext() {
    return null;
  }