# bacardi
Fix breaking dependency updates

## Repository Contents

- **[classifier](breaking-classifier)**: classify breaking dependency updates and extract information from the logs
- **[docker-build](docker-build)**: build docker images to reproduce the breaking dependency updates and patches to fix them
- **[core](core)**: core functionality to fix breaking dependency updates
- **[test-failures](test-failures)**: detect constructs that cause test failures
- **[git-manager](git-manager)**: manage git repositories and create new branches for each failure category
- **[extractor](extractor)**: extract information from the client's repository and api diff
 
##  Workflows

### Classify breaking dependency updates

- Werror: treat warnings as errors
- Java version incompatibility: the new version of the dependency is not compatible with the Java version specified in the CI configuration
- Test failures: the new version of the dependency causes test failures
- Compilation errors: the new version of the dependency causes compilation errors
