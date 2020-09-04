package com.huawei.fossbot.dependency;

import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.bean.BuildFile;
import com.huawei.fossbot.dependency.bean.Pair;
import com.huawei.fossbot.dependency.bean.ParseResult;
import com.huawei.fossbot.dependency.bean.Type;
import com.huawei.fossbot.dependency.factory.AnalyzerFactory;
import com.huawei.fossbot.dependency.factory.ParserFactory;
import com.huawei.fossbot.dependency.parser.DependencyParser;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import com.huawei.fossbot.dependency.util.FileUtils;
import com.huawei.fossbot.dependency.log.Logger;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * @authtor t30002128
 * @since 2020/05/24
 */
public class ParserManager {
    private static Map<String, Type> profileToArtifact = new HashMap<>();
    private static Logger log = Logger.getInstance();
    static {
        // Python...GO...
        profileToArtifact.put("pom.xml", Type.MAVEN);
        profileToArtifact.put("build.gradle", Type.GRADLE);
    }

    /**
     * 使用全局配置文件解析依赖
     *
     * @param path path
     * @param cmd  true使用命令行,false使用自研引擎
     * @return ParseResult 解析结果
     */
    public ParseResult analyze(String path, boolean cmd) {
        return analyze(path, cmd, true);
    }

    /**
     * 基于配置文件解析依赖
     *
     * @param path              path
     * @param useGlobalSettings 是否使用全局settings
     * @param cmd               true使用命令行,false使用自研引擎
     * @return ParseResult      解析结果
     */
    public ParseResult analyze(String path, boolean cmd, boolean useGlobalSettings) {
        // 最多向下寻找4个文件夹
        ParseResult parseResult = new ParseResult();
        parseResult.setBuildFile(new BuildFile(Paths.get(""), new Artifact()));
        if (path == null) {
            log.info("path of the build file should not be null");
            parseResult.setMsg("path of the build file should not be null");
            return parseResult;
        }
        Path profilePath = findProfile(path);
        if (profilePath == null) {
            log.info("failed to find a build file in path:{}", path);
            parseResult.setMsg("failed to find a build file in path:" + path);
            return parseResult;
        }
        log.info("find main build file {}",profilePath.toString());
        Type type = identifyType(profilePath);
        try {
            BuildFile buildFile;
            if (cmd) {
                DependencyAnalyzer dependencyAnalyzer = AnalyzerFactory.newInstance(type,
                        FileUtils.getUniformPathStr(profilePath), useGlobalSettings);
                buildFile = dependencyAnalyzer.analyze();
            } else {
                DependencyParser dependencyParser = ParserFactory.newInstance(type,
                        FileUtils.getUniformPathStr(profilePath), useGlobalSettings);
                buildFile = dependencyParser.parse(profilePath.toString());
            }
            parseResult.setBuildFile(buildFile);
            parseResult.setMsg("success");
            parseResult.setSuccess(true);
        } catch (Exception e) {
            e.printStackTrace();
            parseResult.setMsg(e.getClass().getName() + ":" + e.getMessage());
        }
        return parseResult;
    }

    /**
     * 使用全局配置文件解析(不解析modules)
     *
     * @param path path
     * @param cmd  true使用命令行,false使用自研引擎
     * @return ParseResult      解析结果
     */
    public ParseResult analyzeRootProject(String path, boolean cmd) {
        return analyzeRootProject(path, cmd, true);
    }

    /**
     * 使用配置文件解析(不解析modules)
     *
     * @param path              path
     * @param useGlobalSettings 是否使用全局settings
     * @param cmd               true使用命令行,false使用自研引擎
     * @return ParseResult      解析结果
     */
    private ParseResult analyzeRootProject(String path, boolean cmd, boolean useGlobalSettings) {
        // 最多向下寻找4个文件夹
        ParseResult parseResult = new ParseResult();
        parseResult.setBuildFile(new BuildFile(Paths.get(""), new Artifact()));
        if (path == null) {
            parseResult.setMsg("path of the build file should not be null");
            return parseResult;
        }
        Path profilePath = findProfile(path);
        if (profilePath == null) {
            parseResult.setMsg("failed to find a build file in path:" + path);
            return parseResult;
        }
        Type type = identifyType(profilePath);

        try {
            BuildFile buildFile;
            if (cmd) {
                DependencyAnalyzer dependencyAnalyzer = AnalyzerFactory.newInstance(type
                        , FileUtils.getUniformPathStr(profilePath), useGlobalSettings);
                buildFile = dependencyAnalyzer.analyzeRootProject();
            } else {
                DependencyParser dependencyParser = ParserFactory.newInstance(type,
                        FileUtils.getUniformPathStr(profilePath), useGlobalSettings);
                buildFile = dependencyParser.parseRootProject(profilePath.toString());
            }
            parseResult.setBuildFile(buildFile);
            parseResult.setMsg("success");
            parseResult.setSuccess(true);
        } catch (Exception e) {
            parseResult.setMsg(e.getClass().getName() + ":" + e.getMessage());
        }
        return parseResult;
    }

    /**
     * see directAnalyze(String path, String localRepo, String remoteRepo, boolean cmd)
     */
    public ParseResult directAnalyze(String path, boolean cmd) {
        return directAnalyze(path, cmd, true);
    }

    /**
     * @param path              path
     * @param useGlobalSettings 是否使用全局设置
     * @param cmd               true使用命令行,false使用自研引擎
     */
    public ParseResult directAnalyze(String path, boolean cmd, boolean useGlobalSettings) {
        ParseResult parseResult = new ParseResult();
        parseResult.setBuildFile(new BuildFile(Paths.get(""), new Artifact()));
        if (path == null) {
            parseResult.setMsg("path of the build file should not be null");
            return parseResult;
        }
        // 最多向下寻找4个文件夹
        Path profilePath = findProfile(path);
        if (profilePath == null) {
            parseResult.setMsg("failed to find a build file in path:" + path);
            return parseResult;
        }
        Type type = identifyType(profilePath);

        try {
            BuildFile buildFile;
            if (cmd) {
                DependencyAnalyzer dependencyAnalyzer = AnalyzerFactory.newInstance(type,
                        FileUtils.getUniformPathStr(profilePath), useGlobalSettings);
                buildFile = dependencyAnalyzer.directAnalyze();
            } else {
                DependencyParser dependencyParser = ParserFactory.newInstance(type,
                        FileUtils.getUniformPathStr(profilePath), useGlobalSettings);
                buildFile = dependencyParser.directParse(profilePath.toString());
            }
            parseResult.setBuildFile(buildFile);
            parseResult.setMsg("success");
            parseResult.setSuccess(true);
        } catch (Exception e) {
            e.printStackTrace();
            parseResult.setMsg(e.getClass().getName() + ":" + e.getMessage());
        }
        return parseResult;
    }

    private static Type identifyType(Path profilePath) {
        String fileName = profilePath.toFile().getName();
        return profileToArtifact.get(fileName);
    }

    private Path findProfile(String path) {
        Queue<String> profileQueue = generateProfileQueue();

        List<Pair<Path, Integer>> pathPairs = new ArrayList<>();
        while (!profileQueue.isEmpty()) {
            pathPairs.add(findProfile(path, profileQueue.poll()));
        }
        int depth = pathPairs.get(0).getValue();
        Path profilePath = pathPairs.get(0).getKey();
        for (Pair<Path, Integer> pathPair : pathPairs) {
            if (pathPair.getValue() < depth) {
                profilePath = pathPair.getKey();
            }
        }
        return profilePath;
    }

    private Queue<String> generateProfileQueue() {
        LinkedList<String> profileTypeQueue = new LinkedList<>();
        profileTypeQueue.offer("pom.xml");
        profileTypeQueue.offer("build.gradle");
        return profileTypeQueue;
    }

    private Pair<Path, Integer> findProfile(String path, String profile) {
        int depth = 1;
        Path pomPath = null;
        while (pomPath == null && depth <= 3) {
            pomPath = DependencyAnalyzeHelper.findFile(Paths.get(path), profile, depth);
            depth++; // 深度由1至3
        }

        return new Pair<>(pomPath, --depth);
    }

}
