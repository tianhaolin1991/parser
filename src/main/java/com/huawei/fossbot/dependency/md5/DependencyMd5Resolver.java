package com.huawei.fossbot.dependency.md5;

import com.huawei.fossbot.dependency.bean.Artifact;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;

public abstract class DependencyMd5Resolver {

    public String resolveMd5(Artifact artifact) {
        Path repoPath = getRepoPath();
        Path dependencyPath = resolveDependencyPath(repoPath, artifact);
        return md5(dependencyPath);
    }

    protected abstract Path getRepoPath();

    protected abstract Path resolveDependencyPath(Path repoPath, Artifact artifact);

    private String md5(Path filePath) {
        if(filePath==null) return null;
        File file = filePath.toFile();
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte[] buffer = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }
}
