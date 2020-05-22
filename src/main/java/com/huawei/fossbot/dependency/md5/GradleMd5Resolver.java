package com.huawei.fossbot.dependency.md5;

import com.huawei.fossbot.dependency.bean.AnalyzerType;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.util.RepoPathUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GradleMd5Resolver extends DependencyMd5Resolver {
    @Override
    protected Path getRepoPath() {
        return Paths.get(RepoPathUtil.getRepoPath(AnalyzerType.GRADLE));
    }

    @Override
    protected Path resolveDependencyPath(Path repoPath, Artifact artifact) {
       Path jarPath = repoPath.resolve(artifact.getGroupId()).resolve(artifact.getArtifactId()).resolve(artifact.getVersion());
       if(jarPath.toFile().exists()){
           return findJar(jarPath.toFile(),getJarName(artifact));
       }
       return null;
    }

    private String getJarName(Artifact artifact) {
        return artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";
    }

    private Path findJar(File jarPath, String jarName) {
        File[] files = jarPath.listFiles();
        for (File file : files) {
            File[] jars = file.listFiles(jar -> jar.getName().equals(jarName));
            if(jars!=null&&jars.length>0){
                return jars[0].toPath();
            }
        }
        return null;
    }
}
