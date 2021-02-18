package org.helllabs.android.xmp.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public final class FileUtils {

    private FileUtils() {
        // do nothing
    }

    public static void writeToFile(final File file, final String[] lines) throws IOException {
        final BufferedWriter out = new BufferedWriter(new FileWriter(file, true), 512);
        for (final String line : lines) {
            out.write(line);
            out.newLine();
        }
        out.close();
    }

    public static void writeToFile(final File file, final String line) throws IOException {
        final String[] lines = {line};
        writeToFile(file, lines);
    }

    public static String readFromFile(final File file) throws IOException {
        final BufferedReader in = new BufferedReader(new FileReader(file), 512);
        final String line = in.readLine();
        in.close();
        return line;
    }

    public static boolean removeLineFromFile(final File file, final int num) throws IOException {
        final int[] nums = {num};
        return removeLineFromFile(file, nums);
    }

    public static boolean removeLineFromFile(final File file, final int[] num) throws IOException {

        final File tempFile = new File(file.getAbsolutePath() + ".tmp");

        final BufferedReader reader = new BufferedReader(new FileReader(file), 512);
        final PrintWriter writer = new PrintWriter(new FileWriter(tempFile));

        String line;
        boolean flag;
        for (int lineNum = 0; (line = reader.readLine()) != null; lineNum++) {
            flag = false;
            for (int n : num) {
                if (lineNum == n) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                writer.println(line);
                writer.flush();
            }
        }
        writer.close();
        reader.close();

        // Delete the original file
        if (!file.delete()) {
            return false;
        }

        // Rename the new file to the filename the original file had.
        return tempFile.renameTo(file);

    }

    static public String basename(final String pathname) {
        if (pathname != null && !pathname.isEmpty()) {
            return new File(pathname).getName();
        } else {
            return "";
        }
    }
}
