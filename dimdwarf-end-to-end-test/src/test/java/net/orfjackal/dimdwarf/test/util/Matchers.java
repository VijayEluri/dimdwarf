// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.test.util;

import org.hamcrest.*;

import java.io.File;

import static org.hamcrest.Matchers.hasItemInArray;

public class Matchers {

    public static Matcher<File> isJarFile() {
        return new TypeSafeMatcher<File>() {
            public boolean matchesSafely(File file) {
                return file.isFile() && file.getName().endsWith(".jar");
            }

            public void describeTo(Description description) {
                description.appendText("a .jar file");
            }
        };
    }

    public static Matcher<File> containsFile(final String filename) {
        return new TypeSafeMatcher<File>() {
            public boolean matchesSafely(File dir) {
                return hasItemInArray(filename).matches(dir.list());
            }

            public void describeTo(Description description) {
                description.appendText("directory containing ").appendValue(filename);
            }
        };
    }

    public static Matcher<File> containsFile(final File file) {
        return new TypeSafeMatcher<File>() {
            public boolean matchesSafely(File dir) {
                return hasItemInArray(file).matches(dir.listFiles());
            }

            public void describeTo(Description description) {
                description.appendText("directory containing ").appendValue(file);
            }
        };
    }

    static Matcher<File> isDirectory() {
        return new TypeSafeMatcher<File>() {
            public boolean matchesSafely(File dir) {
                return dir.isDirectory();
            }

            public void describeTo(Description description) {
                description.appendText("is directory");
            }
        };
    }
}
