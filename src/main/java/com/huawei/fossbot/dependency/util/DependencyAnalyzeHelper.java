package com.huawei.fossbot.dependency.util;

import com.huawei.fossbot.dependency.bean.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyAnalyzeHelper {
    private static final OS TYPE = detectOSType();
    private static Logger log = LoggerFactory.getLogger(DependencyAnalyzeHelper.class);

    public static OS osType() {
        return TYPE;
    }

    private static OS detectOSType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.WINDOWS;
        } else if (osName.contains("mac")) {
            return OS.MAC;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.indexOf("aix") > 0) {
            return OS.UNIX;
        } else if (osName.contains("sunos")) {
            return OS.SOLARIS;
        } else {
            return OS.UNKONW;
        }
    }

    public static Path findFile(Path path, String fileName, int depth) {
        try {
            List<Path> poms = Files.find(path, depth,
                    (p, attr) -> fileName.equals(p.getFileName().toString()), FileVisitOption.FOLLOW_LINKS)
                    .collect(Collectors.toList());
            if (poms.size() > 0) {
                return poms.get(0);
            }
        } catch (IOException e) {
            log.error("error occurs when try to find {} in path {}", fileName, path.toString(), e);
            return null;
        }
        return null;
    }

    public static List<Path> findFiles(Path path, String fileName, int depth) throws IOException {
        System.out.println("findFiles path: " + path);
        System.out.println("findFiles fileName: " + fileName);
        List<Path> paths = Files.find(path, depth,
                (p, attr) -> fileName.equals(p.getFileName().toString()),
                FileVisitOption.FOLLOW_LINKS).collect(Collectors.toList());
        return paths;
    }

    public static Path findMavenRepoPath() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        switch (osType()) {
            case WINDOWS:
                processBuilder.command("cmd.exe", "/c", "mvn help:evaluate -Dexpression=settings.localRepository");
                break;
            case UNIX:
                processBuilder.command("bash", "-c", "mvn help:evaluate -Dexpression=settings.localRepository");
                break;
            default:
                log.error("unsupport os type {}", osType());
                return null;
        }

        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    try {
                        if (!line.startsWith("[") && Paths.get(line).toFile().exists()) break;
                    } catch (InvalidPathException e) { // 校验一下路径
                        continue;
                    }
                }
                int exitVal = process.waitFor();
                if (exitVal == 0 && null != line) {
                    return Paths.get(line);
                } else {
                    //abnormal...
                }
            }
        } catch (IOException | InterruptedException e) {
            log.error("error occurs when exec mvn", e);
        }

        return null;
    }

  /*  public static Path getRepoJarPath(Path repoPath, FileInfo dependency) {
        Path jarPath = repoPath;
        for (String d : dependency.getGroupId().split("\\.")) {
            jarPath = jarPath.resolve(d);
        }
        if (dependency.getVersion() == null) {
            return null;
        }
        jarPath = jarPath.resolve(dependency.getArtifactId()).resolve(dependency.getVersion())
                .resolve(dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar");
        return jarPath;
    }*/

    /**
     * user.home下创建子文件夹
     *
     * @param suffix 子文件夹名称
     * @return
     */
    public static Path path(String suffix) {
        String userHome = System.getProperty("user.home");
        Path path = Paths.get(userHome, ".fossbotAgent", suffix);
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }
        return path;
    }

    public static boolean executable(Path target) {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        try {
            Files.setPosixFilePermissions(target, perms);
        } catch (IOException e) {
            log.error("chmod failed!", e);
            return false;
        }
        return true;
    }



}
