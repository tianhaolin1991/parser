package com.huawei.fossbot.dependency.md5;

import com.huawei.fossbot.dependency.bean.Artifact;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * maven的md5解析器
 *
 * @author t30002128
 * @since 2020/05/20
 */
public class MavenMd5Resolver extends DependencyMd5Resolver {


    @Override
    public Path resolveDependencyPath(Artifact artifact,String localRepo) {
        if (artifact.getVersion() == null) {
            return null;
        }

        Path jarPath = Paths.get(localRepo);
        for (String d : artifact.getGroupId().split("\\.")) {
            jarPath = jarPath.resolve(d);
        }

        jarPath = jarPath.resolve(artifact.getArtifactId()).resolve(artifact.getVersion())
                .resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar");
        return jarPath;
    }
}
