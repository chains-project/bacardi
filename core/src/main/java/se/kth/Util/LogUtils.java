package se.kth.Util;

import org.slf4j.Logger;

public class LogUtils {

    public static void logWithBox(Logger log, String message) {
        // Calculate the length of the message
        int length = message.length();

        // Generate the top and bottom border dynamically
        String border = "|" + "-".repeat(length + 2) + "|";

        // Log the message with the box
        log.info("");
        log.info(border);
        log.info("| {} |", message);
        log.info(border);
        log.info("");
    }
}
