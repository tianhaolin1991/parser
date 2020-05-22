package com.huawei.fossbot.dependency.md5;

import com.huawei.fossbot.dependency.bean.AnalyzerType;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.util.RepoPathUtil;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MavenMd5Resolver extends DependencyMd5Resolver {

    @Override
    protected Path getRepoPath() {
        return Paths.get(RepoPathUtil.getRepoPath(AnalyzerType.MAVEN));
    }

    @Override
    protected Path resolveDependencyPath(Path repoPath, Artifact artifact) {

        if (artifact.getVersion() == null) {
            return null;
        }

        Path jarPath = repoPath;
        for (String d : artifact.getGroupId().split("\\.")) {
            jarPath = jarPath.resolve(d);
        }

        jarPath = jarPath.resolve(artifact.getArtifactId()).resolve(artifact.getVersion())
                .resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar");
        return jarPath;
    }
}
