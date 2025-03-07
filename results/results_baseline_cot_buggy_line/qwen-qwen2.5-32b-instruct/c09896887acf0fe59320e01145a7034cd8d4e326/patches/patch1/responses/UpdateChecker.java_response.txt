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

public final class UpdateChecker {

    // ... (rest of the class remains unchanged)

    private static boolean isAncestor(GitHubCommit ghc, String branch) throws Exception {
        try {
            GHCompare compare = GitHub.connect().getRepository(ghc.owner + '/' + ghc.repo).getCompare(branch, ghc.hash);
            return compare.getStatus() == GHCompare.Status.identical || compare.getStatus() == GHCompare.Status.behind;
        } catch (FileNotFoundException x) {
            // For example, that branch does not exist in this repository.
            return false;
        }
        // TODO check behavior when the comparison is huge (too many commits or too large diff)
        // and perhaps fall back to cloning into a temp dir and pulling all PR refs https://gist.github.com/piscisaureus/3342247
        // Currently https://developer.github.com/v4/object/commit/ does no better than this.
    }

    // ... (rest of the class remains unchanged)
}