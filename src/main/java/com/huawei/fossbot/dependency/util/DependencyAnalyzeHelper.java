package com.huawei.fossbot.dependency.util;

import com.huawei.fossbot.dependency.bean.OS;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import com.twmacinta.util.MD5;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author t30002128
 * @since 2020/05/01
 */
public class DependencyAnalyzeHelper {
    private static final OS TYPE =detectOSType();

    /**
     * detect the os type
     */
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

    /**
     * @param path filepath
     * @param fileName filename
     * @param depth 查找的深度
     * @return 找到的文件路径
     */
    public static Path findFile(Path path, String fileName, int depth) {
        // TODO - throw exception
        try {
            List<Path> poms = Files.find(path, depth,
                    (pa, attr) -> fileName.equals(pa.getFileName().toString()), FileVisitOption.FOLLOW_LINKS)
                    .collect(Collectors.toList());
            if (poms.size() > 0) {
                return poms.get(0);
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * 从路径中查找files
     */
    public static List<Path> findFiles(Path path, String fileName, int depth) throws IOException {
        List<Path> paths = Files.find(path, depth,
                (pa, attr) -> fileName.equals(pa.getFileName().toString()),
                FileVisitOption.FOLLOW_LINKS).collect(Collectors.toList());
        return paths;
    }

    /**
     * user.home下创建子文件夹
     *
     * @param suffix 子文件夹名称
     */
    public static Path path(String suffix) {
        String userHome = System.getProperty("user.home");
        Path path = Paths.get(userHome, ".fossbotAgent", suffix);
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }
        return path;
    }

    /**
     * as the method name
     */
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
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * user.home下创建子文件夹
     *
     * @param path 要解析的file路径
     * @return String md5的string值
     * @throws IOException
     */
    public static String fastMd5(String path){
        String md5;
        try {
            md5 = MD5.asHex(MD5.getHash(new File(path)));
        } catch (IOException e) {
            //如果文件不存在,则返回null
            md5 = null;
        }
        return md5;
    }

}
