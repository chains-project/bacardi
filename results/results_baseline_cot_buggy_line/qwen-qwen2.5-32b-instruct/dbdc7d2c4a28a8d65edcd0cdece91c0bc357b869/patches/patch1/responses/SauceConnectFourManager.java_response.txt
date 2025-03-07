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

    private boolean cleanUpOnExit;

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

    public SauceConnectFourManager() {
        this(false);
    }

    public SauceConnectFourManager(boolean quietMode) {
        super(quietMode);
    }

    protected String[] generateSauceConnectArgs(String[] args, String username, String apiKey, int port, String options) {
        String[] result = joinArgs(args, "-u", username.trim(), "-k", apiKey.trim(), "-P", String.valueOf(port));
        result = addElement(result, options);
        return result;
    }

    protected String[] addExtraInfo(String[] args) {
        String[] result = joinArgs(args, "--extra-info", "{\"runner\": \"jenkins\"}");
        return result;
    }

    public File extractZipFile(File workingDirectory, OperatingSystem operatingSystem) throws IOException {
        File zipFile = extractFile(workingDirectory, operatingSystem.getFileName(useLatestSauceConnect));
        if (cleanUpOnExit) {
            zipFile.deleteOnExit();
        }
        AbstractUnArchiver unArchiver;
        if (operatingSystem == OperatingSystem.OSX || operatingSystem == OperatingSystem.WINDOWS) {
            unArchiver = new ZipUnArchiver();
        } else if (operatingSystem == OperatingSystem.LINUX) {
            removeOldTarFile(zipFile);
            unArchiver = new TarGZipUnArchiver();
        } else {
            throw new RuntimeException("Unknown operating system: " + operatingSystem.name());
        }
        // Removed unArchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_DEBUG, "Sauce"));
        unArchiver.setSourceFile(zipFile);
        unArchiver.setDestDirectory(workingDirectory);
        unArchiver.extract();
        File unzipDir = getUnzipDir(workingDirectory, operatingSystem);
        if (cleanUpOnExit) {
            unzipDir.deleteOnExit();
        }
        return unzipDir;
    }

    private File getUnzipDir(File workingDirectory, OperatingSystem operatingSystem) {
        return new File(workingDirectory, operatingSystem.getDirectory(useLatestSauceConnect));
    }

    private void removeOldTarFile(File zipFile) throws SauceConnectException {
        File tarFile = new File(zipFile.getParentFile(), zipFile.getName().replaceAll(".gz", ""));
        removeFileIfExists(tarFile, "Unable to delete old tar");
    }

    private void extractArchive(AbstractUnArchiver unArchiver, File archive, File destination) {
        unArchiver.setSourceFile(archive);
        unArchiver.setDestDirectory(destination);
        unArchiver.extract();
    }

    private File extractFile(File workingDirectory, String fileName) throws IOException {
        File destination = new File(workingDirectory, fileName);
        removeFileIfExists(destination, "Unable to delete old zip");
        InputStream inputStream = useLatestSauceConnect ? new URL("https://saucelabs.com/downloads/" + fileName)
            .openStream() : getClass().getClassLoader().getResourceAsStream(fileName);
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
}