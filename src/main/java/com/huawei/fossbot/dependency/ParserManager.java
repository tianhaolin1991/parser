package com.huawei.fossbot.dependency;

import com.huawei.fossbot.dependency.bean.AnalyzerType;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.factory.ParserFactory;
import com.huawei.fossbot.dependency.parser.DependencyParser;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @authtor t30002128
 * @since 2020/05/24
 */
public class ParserManager{

    public static Map<String,AnalyzerType> profileToArtifact = new HashMap<>();

    static{
        profileToArtifact.put("pom.xml",AnalyzerType.MAVEN);
        profileToArtifact.put("build.gradle",AnalyzerType.GRADLE);
        //TODO-go,python...

    }

    public List<Artifact> startAnalyze(String path){
        return startAnalyze(path,null,null);
    }

    public List<Artifact> startAnalyze(String path, String localRepo, String remoteRepo){
        Path profilePath = findProfile(path);
        AnalyzerType analyzerType = identifyAnalyzerType(profilePath);
        DependencyParser dependencyParser = ParserFactory.newInstance(analyzerType, localRepo, remoteRepo);
        return dependencyParser.parse(profilePath.toString());
    }

    private static AnalyzerType identifyAnalyzerType(Path profilePath) {
        String fileName = profilePath.toFile().getName();
        return profileToArtifact.get(fileName);
    }

    private Path findProfile(String path) {
        Queue<String> profileQueue = generateProfileQueue();
        Path profilePath = null;
        while (!profileQueue.isEmpty() && profilePath == null) {
            profilePath = findProfile(path, profileQueue.poll());
        }
        return profilePath;
    }

    private Queue<String> generateProfileQueue() {
        LinkedList<String> profileTypeQueue = new LinkedList<>();
        profileTypeQueue.offer("pom.xml");
        profileTypeQueue.offer("build.gradle");
        return profileTypeQueue;
    }

    private Path findProfile(String path, String profile) {
        int depth = 1;
        Path pomPath = null;
        while (pomPath == null && depth <= 3) {
            pomPath = DependencyAnalyzeHelper.findFile(Paths.get(path), profile, depth);
            depth++;// 深度由1至3
        }
        return pomPath;
    }
}
