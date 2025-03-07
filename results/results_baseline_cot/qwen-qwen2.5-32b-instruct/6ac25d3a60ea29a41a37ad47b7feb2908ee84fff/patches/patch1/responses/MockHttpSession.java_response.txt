package org.apache.myfaces.tobago.internal.mock.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MockHttpSession implements HttpSession {

  private Map<String, Object> attributes = new HashMap<>();

  @Override
  public Object getAttribute(final String s) {
    return attributes.get(s);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return new Vector<>(attributes.keySet()).elements();
  }

  @Override
  public long getCreationTime() {
    return 0;
  }

  @Override
  public String getId() {
    return null;
  }

  @Override
  public long getLastAccessedTime() {
    return 0;
  }

  @Override
  public int getMaxInactiveInterval() {
    return 0;
  }

  @Override
  public void invalidate() {
  }

  @Override
  public boolean isNew() {
    return false;
  }

  @Override
  public void removeAttribute(final String s) {
    attributes.remove(s);
  }

  @Override
  public void setAttribute(final String s, final Object o) {
    attributes.put(s, o);
  }

  @Override
  public void setMaxInactiveInterval(final int i) {
  }

  @Override
  public ServletContext getServletContext() {
    return null;
  }
}