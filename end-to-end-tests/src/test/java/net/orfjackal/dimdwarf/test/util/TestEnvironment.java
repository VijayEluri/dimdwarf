// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.test.util;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestEnvironment {

    private static final String FILE_NOT_SPECIFIED = "/dev/null";

    private static final String uniqueJvmId = new SimpleDateFormat("yyyy-MM-dd-HHmmss.SSS").format(new Date());
    private static final AtomicInteger tempDirCounter = new AtomicInteger(0);
    private static final File sandboxDir;
    private static final File serverHomeDir;
    private static final File applicationBaseJar;

    static {
        Properties p = testEnvironmentProperties();
        sandboxDir = canonicalFile(p.getProperty("test.sandbox", FILE_NOT_SPECIFIED));
        serverHomeDir = canonicalFile(p.getProperty("test.serverHome", FILE_NOT_SPECIFIED));
        applicationBaseJar = canonicalFile(p.getProperty("test.applicationBaseJar", FILE_NOT_SPECIFIED));
    }

    private static Properties testEnvironmentProperties() {
        InputStream in = TestEnvironment.class.getResourceAsStream("TestEnvironment.properties");
        if (in == null) {
            throw new RuntimeException("Properties not found");
        }
        try {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (IOException e) {
            throw new RuntimeException("Reading properties failed", e);
        }
    }

    private static File canonicalFile(String path) {
        try {
            return new File(path).getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File createTempDir() {
        File dir = new File(sandboxDir, "temp_" + uniqueJvmId + "_" + tempDirCounter.incrementAndGet());
        if (!dir.mkdir()) {
            throw new IllegalStateException("Unable to create directory: " + dir);
        }
        return dir;
    }

    public static void deleteTempDir(File dir) {
        if (!dir.getParentFile().equals(sandboxDir)) {
            throw new IllegalArgumentException("I did not create that file, deleting it would be dangerous: " + dir);
        }
        try {
            FileUtils.forceDelete(dir);
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete directory: " + dir, e);
        }
    }

    public static File getSandboxDir() {
        return sandboxDir;
    }

    public static File getServerHomeDir() {
        return serverHomeDir;
    }

    public static File getApplicationBaseJar() {
        return applicationBaseJar;
    }
}
