package com.huawei.fossbot.dependency.util;

import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.bean.ArtifactNode;
import com.huawei.fossbot.dependency.bean.BuildFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成依赖树工具类
 * @since v1.0.4.4
 * @author t30002128
 */
public class DependencyTreeHelper {
    private static Map<String, Integer> orderMap = new HashMap<>();
    static {
        orderMap.put("compile",1);
        orderMap.put("runtime",2);
        orderMap.put("test",3);
    }

    public static List<ArtifactNode> generateDependencyTree(BuildFile buildFile) {
        List<BuildFile> modules = new ArrayList<>();
        modules.add(buildFile);
        getModules(buildFile, modules);
        List<ArtifactNode> dependencyTree = new ArrayList<>();
        Map<String,ArtifactNode> parsed = new LinkedHashMap<>();
        for (BuildFile module : modules) {
            getDependencyTree(module, dependencyTree,parsed);
        }
        List<String> moduleArtifacts = new ArrayList<>();
        modules.forEach(module-> {
            Artifact artifact = module.getArtifact();
            moduleArtifacts.add(artifact.toString());
        });
        dependencyTree.forEach(artifactNode -> {
            if(moduleArtifacts.contains(artifactNode.getParentCoordinate())){
                artifactNode.setParentCoordinate("");
            }
        });
        return dependencyTree;
    }

    private static void getDependencyTree(BuildFile module, List<ArtifactNode> dependencyTree, Map<String,ArtifactNode> parsed) {
        module.getChildren().forEach(buildFile -> {
            Artifact artifact = buildFile.getArtifact();
            if(parsed.containsKey(artifact.toString())){
                updateScope(parsed,artifact,dependencyTree);
            }else{
                ArtifactNode dependency = new ArtifactNode()
                        .artifact(artifact)
                        .parentCoordinate(getParentCoordinate(buildFile));
                dependencyTree.add(dependency);
                parsed.put(dependency.getKey(),dependency);
            }
            getDependencyTree(buildFile, dependencyTree, parsed);
        });
    }

    private static void updateScope(Map<String, ArtifactNode> parsed, Artifact artifact, List<ArtifactNode> dependencyTree) {
        ArtifactNode parsedArtifact = parsed.get(artifact.toString());
        if(orderMap.get(parsedArtifact.getScope()) > orderMap.get(artifact.getScope())){
            parsedArtifact.setScope(artifact.getScope());
        }
    }

    private static String getParentCoordinate(BuildFile buildFile) {
        Artifact artifact = buildFile.getParent().getArtifact();
        return artifact.getKey() + ":" + artifact.getVersion();
    }

    private static void getModules(BuildFile buildFile, List<BuildFile> modules) {
        modules.addAll(buildFile.getModules());
        for (BuildFile module : buildFile.getModules()) {
            getModules(module, modules);
        }
    }
}
