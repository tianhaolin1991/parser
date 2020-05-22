package com.huawei.fossbot.dependency;

import com.huawei.fossbot.dependency.bean.AnalyzerType;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.analyzer.java.GradleAnalyzer;
import com.huawei.fossbot.dependency.analyzer.java.MavenAnalyzer;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DependencyAnalyzerManager {

    private static Logger logger = LoggerFactory.getLogger(DependencyAnalyzerManager.class);
    private static Map<AnalyzerType, DependencyAnalyzer> analyzers = new HashMap<>();

    static {
        analyzers.put(AnalyzerType.MAVEN, new MavenAnalyzer());
        analyzers.put(AnalyzerType.GRADLE, new GradleAnalyzer());
        analyzers.put(AnalyzerType.GO, new MavenAnalyzer());
    }

    public static List<Artifact> startAnalyze(String path) {
        Path profilePath = findProfile(path);
        AnalyzerType analyzerType = identifyAnalyzerType(profilePath);
        if (analyzerType == null) {
            logger.error("no build profile found in path {}", path);
            return null;
        }
        DependencyAnalyzer dependencyAnalyzer = analyzers.get(analyzerType);
        return dependencyAnalyzer.analyze(profilePath.toString());
    }

    private static Path findProfile(String path) {
        Queue<String> profileQueue = generateProfileQueue();
        Path profilePath = null;
        while (!profileQueue.isEmpty() && profilePath == null) {
            profilePath = findProfile(path, profileQueue.poll());
        }
        return profilePath;
    }

    private static AnalyzerType identifyAnalyzerType(Path profilePath) {
        if (profilePath != null){
            if (profilePath.endsWith("pom.xml")) {
                return AnalyzerType.MAVEN;
            }
            if (profilePath.endsWith("build.gradle")) {
                return AnalyzerType.GRADLE;
            }
            //TODO-go,python...
        }
        return null;
    }

    private static Queue<String> generateProfileQueue() {
        LinkedList<String> profileTypeQueue = new LinkedList<>();
        profileTypeQueue.offer("pom.xml");
        profileTypeQueue.offer("build.gradle");
        return profileTypeQueue;
    }

    private static Path findProfile(String path, String profile) {
        int depth = 1;
        Path pomPath = null;
        while (pomPath == null && depth <= 3) {
            pomPath = DependencyAnalyzeHelper.findFile(Paths.get(path), profile, depth);
            depth++;// 深度由1至3
        }
        return pomPath;
    }

}
