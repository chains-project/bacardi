package io.jenkins.tools.incrementals.lib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GitHub;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Looks for updates (incremental or otherwise) to a specific artifact.
 */
public final class UpdateChecker {

    private final Log log;
    private final List<String> repos;
    /** keys are {@code groupId:artifactId:currentVersion:branch} */
    private final Map<String, VersionAndRepo> cache = new HashMap<>();

    private final Map<String, String> groupIdCache = new HashMap<>();

    public UpdateChecker(Log log, List<String> repos) {
        this.log = log;
        this.repos = repos;
    }

    public @CheckForNull String findGroupId(String artifactId) throws IOException, InterruptedException {
        String cacheKey = artifactId;
        if (groupIdCache.containsKey(cacheKey)) {
            log.info("Group ID Cache hit on artifact ID: " + artifactId);
            return groupIdCache.get(cacheKey);
        }

        //TODO: implement to support non-Incremental formats
        // Needs to load UC JSON and query it like https://github.com/jenkinsci/docker/pull/668
        return null;
    }

    public @CheckForNull VersionAndRepo find(String groupId, String artifactId, String currentVersion, String branch) throws Exception {
        String cacheKey = groupId + ':' + artifactId + ':' + currentVersion + ':' + branch;
        if (cache.containsKey(cacheKey)) {
            log.info("Cache hit on updates to " + groupId + ":" + artifactId + ":" + currentVersion + " within " + branch);
            return cache.get(cacheKey);
        }
        VersionAndRepo result = doFind(groupId, artifactId, currentVersion, branch);
        cache.put(cacheKey, result);
        return result;
    }

    private @CheckForNull VersionAndRepo doFind(String groupId, String artifactId, String currentVersion, String branch) throws Exception {
        ComparableVersion currentV = new ComparableVersion(currentVersion);
        log.info("Searching for updates to " + groupId + ":" + artifactId + ":" + currentV + " within " + branch);
        SortedSet<VersionAndRepo> candidates = loadVersions(groupId, artifactId);
        if (candidates.isEmpty()) {
            log.info("Found no candidates");
            return null;
        }
        log.info("Found " + candidates.size() + " candidates from " + candidates.first() + " down to " + candidates.last());
        for (VersionAndRepo candidate : candidates) {
            if (candidate.version.compareTo(currentV) <= 0) {
                log.info("Stopping search at " + candidate + " since it is no newer than " + currentV);
                return null;
            }
            log.info("Considering " + candidate);
            GitHubCommit ghc = loadGitHubCommit(candidate);
            if (ghc != null) {
                log.info("Mapped to: " + ghc);
                if (isAncestor(ghc, branch)) {
                    log.info("Seems to be within " + branch + ", so accepting");
                    return candidate;
                } else {
                    log.info("Does not seem to be within " + branch);
                }
            }
        }
        return null;
    }

    private static final class GitHubCommit {
        final String owner;
        final String repo;
        final String hash;
        GitHubCommit(String owner, String repo, String hash) {
            this.owner = owner;
            this.repo = repo;
            this.hash = hash;
        }
        @Override public String toString() {
            return "https://github.com/" + owner + '/' + repo + "/commit/" + hash;
        }
    }

    private static @CheckForNull GitHubCommit loadGitHubCommit(VersionAndRepo vnr) throws Exception {
        String pom = vnr.fullURL("pom");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
        NodeList scmEs = doc.getElementsByTagName("scm");
        if (scmEs.getLength() != 1) {
            throw new Exception("Could not find <scm> in " + pom);
        }
        Element scmE = (Element) scmEs.item(0);
        Element urlE = theElement(scmE, "url", pom);
        String url = urlE.getTextContent();
        Matcher m = Pattern.compile("https?://github[.]com/([^/]+)/([^/]+?)([.]git)?(/.*)?").matcher(url);
        if (!m.matches()) {
            throw new Exception("Unexpected /project/scm/url " + url + " in " + pom + "; expecting https://github.com/owner/repo format");
        }
        Element tagE = theElement(scmE, "tag", pom);
        String tag = tagE.getTextContent();
        String groupId = m.group(1);
        String artifactId = m.group(2).replace("${project.artifactId}", vnr.artifactId);
        if (!tag.matches("[a-f0-9]{40}")) {
            return null;
        }
        return new GitHubCommit(groupId, artifactId, tag);
    }

    private static boolean isAncestor(GitHubCommit ghc, String branch) throws Exception {
        try {
            GHCompare compare = GitHub.connect().getRepository(ghc.owner + '/' + ghc.repo).getCompare(branch, ghc.hash);
            GHCompare.Status status = compare.getStatus(); // Use the getStatus() method instead of accessing the status field directly
            return status == GHCompare.Status.identical || status == GHCompare.Status.behind;
        } catch (FileNotFoundException x) {
            // For example, that branch does not exist in this repository.
            return false;
        }
        // TODO check behavior when the comparison is huge (too many commits or too large diff)
        // and perhaps fall back to cloning into a temp dir and pulling all PR refs https://gist.github.com/piscisaureus/3342247
        // Currently https://developer.github.com/v4/object/commit/ does no better than this.
    }

    private static Element theElement(Document doc, String tagName, String url) throws Exception {
        return theElement(doc.getElementsByTagName(tagName), tagName, url);
    }

    private static Element theElement(Element parent, String tagName, String url) throws Exception {
        return theElement(parent.getElementsByTagName(tagName), tagName, url);
    }

    private static Element theElement(NodeList nl, String tagName, String url) throws Exception {
        if (nl.getLength() != 1) {
            throw new Exception("Could not find <" + tagName + "> in " + url);
        }
        return (Element) nl.item(0);
    }

    public static void main(String... argv) throws Exception {
        if (argv.length != 4) {
            throw new IllegalStateException("Usage: java " + UpdateChecker.class.getName() + " <groupId> <artifactId> <currentVersion> <branch>");
        }
        VersionAndRepo result = new UpdateChecker(
                message -> System.err.println(message),
                Arrays.asList("https://repo.jenkins-ci.org/releases/", "https://repo.jenkins-ci.org/incrementals/")).
            find(argv[0], argv[1], argv[2], argv[3]);
        if (result != null) {
            System.err.println("Found: " + result);
        } else {
            System.err.println("Nothing found.");
        }
    }
}