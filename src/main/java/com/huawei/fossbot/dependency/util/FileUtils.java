package com.huawei.fossbot.dependency.util;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUtils {

    public FileUtils() {
    }

    public static String getFileContent(String path) throws IOException {
        return getFileContent(path, StandardCharsets.UTF_8);
    }

    public static String getFileContent(String path, Charset charSet) throws IOException {
        File file = new File(path);
        File absoluteFile = new File(file.getCanonicalPath());
        String code = new String(Files.readAllBytes(absoluteFile.toPath()), charSet);
        return code;
    }

    public static List<String> cutStringToList(String inputString) {
        String[] rnLines;
        if (inputString.contains("\r\n")) {
            rnLines = inputString.split("\n", -1);
            int size = rnLines.length;

            for(int i = 0; i < size; ++i) {
                if (rnLines[i].endsWith("\r")) {
                    rnLines[i] = rnLines[i].substring(0, rnLines[i].length() - 1);
                }
            }

            return new ArrayList(Arrays.asList(rnLines));
        } else if (inputString.contains("\n")) {
            rnLines = inputString.split("\n", -1);
            return new ArrayList(Arrays.asList(rnLines));
        } else {
            rnLines = inputString.split("\r", -1);
            return new ArrayList(Arrays.asList(rnLines));
        }
    }

    /** @deprecated */
    @Deprecated
    public static void writeResults2File(List<String> wordFrequencyList, String fileName, String lineSeparator) {
        writeResults2File(wordFrequencyList, fileName, lineSeparator, Charset.forName("GB2312"));
    }

    public static void writeResults2File(List<String> resultList, String fileName, String newLineSymbol, Charset charset) {
        createFile(fileName);

        try {
            BufferedWriter outfile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), charset));
            Throwable var5 = null;

            try {
                int size = resultList.size();

                for(int line = 0; line < size; ++line) {
                    outfile.write((String)resultList.get(line));
                    if (line < size - 1) {
                        outfile.write(newLineSymbol);
                    }
                }

                outfile.flush();
            } catch (Throwable var16) {
                var5 = var16;
                throw var16;
            } finally {
                if (outfile != null) {
                    if (var5 != null) {
                        try {
                            outfile.close();
                        } catch (Throwable var15) {
                            var5.addSuppressed(var15);
                        }
                    } else {
                        outfile.close();
                    }
                }

            }
        } catch (IOException var18) {

        }

    }

    public static boolean mkdirs(String dirPath) {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path, new LinkOption[0])) {
            try {
                Files.createDirectories(path);
                return true;
            } catch (IOException var3) {
                return false;
            }
        } else if (Files.isDirectory(path, new LinkOption[0])) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean copyFile(String source, String target) {
        Path targetPath = Paths.get(target);
        Path targetParent = targetPath.getParent();
        if (!Files.exists(targetPath, new LinkOption[0])) {
            boolean succeed = mkdirs(targetParent.toString());
            if (!succeed) {
                return false;
            }
        }

        Path sourcePath = Paths.get(source);

        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException var6) {
            return false;
        }
    }

    public static void writeFile(String filePath, String content) {
        writeFile(filePath, content, StandardCharsets.UTF_8.toString());
    }

    public static void writeFile(String filePath, String content, String charset) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, false), charset));
            Throwable var4 = null;

            try {
                writer.write(content + "\r\n");
            } catch (Throwable var14) {
                var4 = var14;
                throw var14;
            } finally {
                if (writer != null) {
                    if (var4 != null) {
                        try {
                            writer.close();
                        } catch (Throwable var13) {
                            var4.addSuppressed(var13);
                        }
                    } else {
                        writer.close();
                    }
                }

            }
        } catch (Exception var16) {
            var16.printStackTrace();
        }

    }

    public static List<String> getFileLines(String path, Charset charSet) throws IOException {
        String code = getFileContent(path, charSet);
        return cutStringToList(code);
    }

    private static boolean createFile(String destFileName) {
        if (destFileName.endsWith(File.separator)) {
            return false;
        } else {
            File file = new File(destFileName);
            if (file.exists()) {
                return false;
            } else if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                return false;
            } else {
                try {
                    return file.createNewFile();
                } catch (IOException var3) {
                    return false;
                }
            }
        }
    }

    public static List<String> getOriginalFileLines(String path, Charset charSet) throws IOException {
        String code = getFileContent(path, charSet);
        String[] rnLines;
        int i;
        if (code.contains("\n")) {
            rnLines = code.split("\n", -1);

            for(i = 0; i < rnLines.length; ++i) {
                rnLines[i] = rnLines[i] + "\n";
            }

            return new ArrayList(Arrays.asList(rnLines));
        } else if (!code.contains("\r")) {
            List<String> res = new ArrayList();
            res.add(code);
            return res;
        } else {
            rnLines = code.split("\r", -1);

            for(i = 0; i < rnLines.length; ++i) {
                rnLines[i] = rnLines[i] + "\r";
            }

            return new ArrayList(Arrays.asList(rnLines));
        }
    }

    public static List<String> getFileLines(String path) throws IOException {
        String code = getFileContent(path);
        return cutStringToList(code);
    }

    public static String getUniformPathStr(Path path){
        return path.toString().replaceAll("\\\\","/");
    }

    public static String getUniformPathStr(File file){
        return file.toString().replaceAll("\\\\","/");
    }

    public static String getUniformPathStr(String path){
        return path.replaceAll("\\\\","/");
    }
}
