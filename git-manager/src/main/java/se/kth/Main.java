package se.kth;

import org.eclipse.jgit.api.Git;

import java.io.File;

public class Main {
    public static void main(String[] args) {


        File project = new File("/Users/frank/Documents/Work/PHD/bacardi/projects/0c60d0b08c999769313bfe2335fa792efcfb0300/IDS-Messaging-Services");

        GitManager gitManager = new GitManager(project);

        Git git = gitManager.checkRepoStatus();


    }
}