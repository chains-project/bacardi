package com.saucelabs.ci.sauceconnect;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import java.net.URL;

/**
 * Handles launching Sauce Connect v4 (binary executable).
 *
 * @author Ross Rowe
 */
public class SauceConnectFourManager extends AbstractSauceTunnelManager implements SauceTunnelManager {
    private boolean useLatestSauceConnect = false;

    /**
     * Remove all created files and directories on exit
     */
    private boolean cleanUpOnExit;

    /**
     * Represents the operating system-specific Sauce Connect binary.
     */
    public enum OperatingSystem {

        OSX("osx", "zip", UNIX_TEMP_DIR),
        WINDOWS("win32", "zip", WINDOWS_TEMP_DIR, "sc.exe"),
        LINUX("linux", "tar.gz", UNIX_TEMP_DIR),
        LINUX_ARM64("linux-arm64", "tar.gz", UNIX_TEMP_DIR);

        private final String directoryEnding;
        private final String archiveExtension;
        private final String executable;
        private final String tempDirectory;

        OperatingSystem(String directoryEnding, String archiveExtension, String tempDirectory, String executable) {
            this.directoryEnding = directoryEnding;
            this.archiveExtension = archiveExtension;
            this.executable = "bin" + File.separatorChar + executable;
            this.tempDirectory = tempDirectory;
        }

        OperatingSystem(String directoryEnding, String archiveExtension, String tempDirectory) {
            this(directoryEnding, archiveExtension, tempDirectory, "sc");
        }

        public static OperatingSystem getOperatingSystem() {
            String os = System.getProperty("os.name").toLowerCase();
            if (isWindows(os)) {
                return WINDOWS;
            }
            if (isMac(os)) {
                return OSX;
            }
            if (isUnix(os)) {
                String arch = System.getProperty("os.arch").toLowerCase();

                if (isArm(arch)) {
                  return LINUX_ARM64;
                }
                return LINUX;
            }
            throw new RuntimeException("Unsupported OS: " + os);
        }

        private static boolean isWindows(String os) {
            return os.contains("win");
        }

        private static boolean isMac(String os) {
            return os.contains("mac");
        }

        private static boolean isUnix(String os) {
            return os.contains("nux");
        }

        private static boolean isArm(String arch) {
            return arch.startsWith("arm") || arch.startsWith("aarch");
        }

        public String getDirectory(boolean useLatestSauceConnect) {
            return SAUCE_CONNECT + getVersion(useLatestSauceConnect) + '-' + directoryEnding;
        }

        public String getFileName(boolean useLatestSauceConnect) {
            return getDirectory(useLatestSauceConnect) + '.' + archiveExtension;
        }

        public String getExecutable() {
            return executable;
        }

        public String getDefaultSauceConnectLogDirectory() {
            return tempDirectory;
        }
    }

    private static final String UNIX_TEMP_DIR = "/tmp";

    private static final String WINDOWS_TEMP_DIR = System.getProperty("java.io.tmpdir");

    public static final String SAUCE_CONNECT_4_STARTED = "Sauce Connect is up, you may start your tests";

    public static final String CURRENT_SC_VERSION = "4.8.0";
    public static final String LATEST_SC_VERSION = getLatestSauceConnectVersion();

    protected String[] generateSauceConnectArgs(String[] args, String username, String apiKey, int port, String options) {
        String[] result = joinArgs(args, "-u", username.trim(), "-k", apiKey.trim(), "-P", String.valueOf(port));
        );
        result = addElement(result, options);
        return result;
    }

    protected String[] addExtraInfo(String[] args) {
        String[] result = joinArgs(args, "--extra-info", "{\"runner\": \"jenkins\"}");
        return result;
    }

    public File extractZipFile(File workingDirectory, OperatingSystem operatingSystem) throws IOException {
        File destination = new File(workingDirectory, operatingSystem.getFileName(useLatestSauceConnect));
        removeFileIfExists(destination, "Unable to delete old zip");
        InputStream inputStream = useLatestSauceConnect ? new URL("https://saucelabs.com/downloads/" + operatingSystem.getFileName(useLatestSauceConnect)).openStream() : getClass().getClassLoader().getResourceAsStream(operatingSystem.getFileName(useLatestSauceConnect));
        FileUtils.copyInputStreamToFile(inputStream, destination);
        return destination;
    }

    private static void removeFileIfExists(File file, String exceptionMessage) throws SauceConnectException {
        if (file.exists() && !file.delete()) {
            throw new SauceConnectException(exceptionMessage);
        }
    }

    protected String getSauceStartedMessage() {
        return SAUCE_CONNECT_4_STARTED;
    }

    protected String getCurrentVersion() {
        return getVersion(useLatestSauceConnect);
    }

    private static String getVersion(boolean useLatestSauceConnect) {
        return useLatestSauceConnect && LATEST_SC_VERSION != null ? LATEST_SC_VERSION : CURRENT_SC_VERSION;
    }

    public File getSauceConnectLogFile(String options) {
        String logfile = getLogfile(options);
        if (logfile != null) {
            File sauceConnectLogFile = new File(logfile);
            if (sauceConnectLogFile.exists()) {
                return sauceConnectLogFile;
            } else {
                return null;
            }
        }

        String fileName = "sc.log";
        File logFileDirectory = new File(OperatingSystem.getOperatingSystem().getDefaultSauceConnectLogDirectory());

        String tunnelName = getTunnelName(options, null);
        if (tunnelName != null) {
            fileName = MessageFormat.format("sc-{0}.log", tunnelName);
        }
        File sauceConnectLogFile = new File(logFileDirectory, fileName);
        if (!sauceConnectLogFile.exists()) {
            sauceConnectLogFile = new File(getSauceConnectWorkingDirectory(), fileName);
            if (!sauceConnectLogFile.exists()) {
                return null;
            }
        }
        return sauceConnectLogFile;
    }

    @Override
    protected Process prepAndCreateProcess(String username, String apiKey, int port, File sauceConnectJar, String options, PrintStream printStream, String sauceConnectPath) throws SauceConnectException {
        File sauceConnectBinary;
        if (sauceConnectPath == null || sauceConnectPath.equals("")) {
            File workingDirectory = sauceConnectJar != null && sauceConnectJar.exists() ? sauceConnectJar.getParentFile() : new File(getSauceConnectWorkingDirectory());
            if (!workingDirectory.canWrite()) {
                throw new SauceConnectException("Can't write to " + workingDirectory.getAbsolutePath());
            }
            OperatingSystem operatingSystem = OperatingSystem.getOperatingSystem();
            File unzipDir = extractZipFile(workingDirectory, operatingSystem);
            if (cleanUpOnExit) {
                unzipDir.deleteOnExit();
            }
            sauceConnectBinary = new File(unzipDir, operatingSystem.getExecutable());
            if (!sauceConnectBinary.exists()) {
                synchronized (this) {
                    if (!sauceConnectBinary.exists()) {
                        extractZipFile(workingDirectory, operatingSystem);
                    }
                }
            }
        } else {
            sauceConnectBinary = new File(sauceConnectPath);
            if (!sauceConnectBinary.exists()) {
                throw new SauceConnectException(sauceConnectPath + " doesn't exist");
            }
        }

        String[] args = { sauceConnectBinary.getPath() };
        args = generateSauceConnectArgs(args, username, apiKey, port, options);
        args = addExtraInfo(args);

        julLogger.log(Level.INFO, "Launching Sauce Connect " + getCurrentVersion() + " " + hideSauceConnectCommandlineSecrets(args));
        return createProcess(args, sauceConnectBinary.getParentFile());
    }

    private void extractArchive(AbstractUnArchiver unArchiver, File archive, File destination) {
        // Removed the enableLogging call as it is no longer supported in the new version of the dependency.
        unArchiver.setSourceFile(archive);
        unArchiver.setDestDirectory(destination);
        unArchiver.extract();
    }
}