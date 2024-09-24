package se.kth;

import se.kth.model.BreakingUpdate;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = "/home/leonard/code/java/bump/data/benchmark/";
        List<BreakingUpdate> testFailureUpdates = TestFailuresProvider.getTestFailuresFromResources(filePath);
        System.out.println(testFailureUpdates);
    }
}