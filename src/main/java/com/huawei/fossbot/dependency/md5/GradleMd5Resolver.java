package com.huawei.fossbot.dependency.md5;

import com.huawei.fossbot.dependency.bean.Type;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.util.RepoPathUtil;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *  gradle依赖文件md5解析器
 *
 * @author t30002128
 * @since 2020/05/20
 */
public class GradleMd5Resolver extends DependencyMd5Resolver {

    @Override
    public Path resolveDependencyPath(Artifact artifact, String localRepo) {
        if (artifact.getVersion() == null) {
            return null;
        }
        Path repoPath = Paths.get(localRepo);
        Path jarPath = repoPath.resolve(artifact.getGroupId())
                .resolve(artifact.getArtifactId()).resolve(artifact.getVersion());
        if (jarPath.toFile().exists()) {
            Path path = findJar(jarPath.toFile(), getJarName(artifact));
            if(path==null){
                path = findJar(jarPath.toFile(), getAarName(artifact));
            }
            return path;
        }
        return null;
    }

    private String getAarName(Artifact artifact) {
        return artifact.getArtifactId() + "-" + artifact.getVersion() + ".aar";
    }

    private String getJarName(Artifact artifact) {
        return artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";
    }

    private Path findJar(File jarPath, String jarName) {
        File[] files = jarPath.listFiles();
        for (File file : files) {
            File[] jars = file.listFiles(jar -> jar.getName().equals(jarName));
            if (jars != null && jars.length > 0) {
                return jars[0].toPath();
            }
        }
        return null;
    }
}
