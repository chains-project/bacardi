* @return true if the project was successfully deleted, false if the project didn't exist
   */
  public synchronized boolean removeProject(String projectId) {
    // Because this method is synchronized, any code that relies on non-atomic read/write operations
    // should not fail if that code is also synchronized.
    policies.remove(checkNotNull(projectId));
    return projects.remove(projectId) != null;
  }
}
