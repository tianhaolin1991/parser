package com.huawei.fossbot.dependency.util;


import com.huawei.fossbot.dependency.bean.AnalyzerType;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class RepoPathUtil {

    private static Map<AnalyzerType, String> pathMap = new HashMap<>();
    private static Map<AnalyzerType, String> remotePathMap = new HashMap<>();

    static {
        //TODO-优化?
        parseMavenRepoPath();
        parseGradleRepoPath();
    }

    public static String getRepoPath(AnalyzerType analyzerType) {
        return pathMap.get(analyzerType);
    }

    public static String getRemoteRepoPath(AnalyzerType analyzerType) {
        return remotePathMap.get(analyzerType);
    }


    private static void parseGradleRepoPath() {
        Path gradleRepoPath = Paths.get(System.getProperty("GRADLE_USER_HOME"), ".gradle");
        if (!gradleRepoPath.toFile().exists()) {
            gradleRepoPath = Paths.get(System.getProperty("user.home"), ".gradle");
        }
        Path gradlePath = gradleRepoPath.resolve("caches").resolve("modules-2").resolve("files-2.1");
        pathMap.put(AnalyzerType.GRADLE, gradlePath.toString().replace("\\", "/"));
    }


    private static void parseMavenRepoPath() {
        Path settingPath = Paths.get(System.getProperty("user.home"), ".m2").resolve("settings.xml");
        if (!settingPath.toFile().exists())
            settingPath = Paths.get(System.getenv("M2_HOME"), "conf").resolve("settings.xml");
        if (!settingPath.toFile().exists())
            settingPath = Paths.get(System.getenv("m2_home"), "conf").resolve("settings.xml");
        if (!settingPath.toFile().exists())
            settingPath = Paths.get(System.getenv("MAVEN_HOME"), "conf").resolve("settings.xml");
        if (!settingPath.toFile().exists())
            settingPath = Paths.get(System.getenv("maven_home"), "conf").resolve("settings.xml");
        if (!settingPath.toFile().exists()) {
            String path = System.getenv("path");
            if (StringUtils.isBlank(path)) {
                path = System.getenv("PATH");
            }

            if (StringUtils.isNotBlank(path)) {
                String[] paths;
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    paths = StringUtils.split(path, ':');
                } else {
                    paths = StringUtils.split(path, ';');
                }
                for (String dir : paths) {
                    Path mvnPath = Paths.get(dir, "mvn");
                    if (mvnPath.toFile().exists()) {
                        settingPath = Paths.get(dir, "..").resolve("conf").resolve("settings.xml");
                    }
                }
            }
        }
        if (!settingPath.toFile().exists()) {
            String mavenRepo = Paths.get(System.getProperty("user.home") + "/.m2").resolve("repository")
                    .toString().replace("\\", "/");
            String mavenRemoteRepo = "http://repo1.maven.org/maven2/";
            pathMap.put(AnalyzerType.MAVEN, mavenRepo);
            remotePathMap.put(AnalyzerType.MAVEN, mavenRemoteRepo);
        } else {
            parseMavenRepoPath(settingPath);
        }

    }

    private static void parseMavenRepoPath(Path settingPath) {

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(settingPath.toFile());
            if (!remotePathMap.containsKey(AnalyzerType.MAVEN)) {
                // 设置远程仓地址
                NodeList urlList = doc.getElementsByTagName("url");
                if (urlList.getLength() > 0) {
                    for (int i = 0; i < urlList.getLength(); i++) {
                        if ("mirror".equals(urlList.item(i).getParentNode().getNodeName())) {
                            String remoteRepo = urlList.item(i).getTextContent();
                            remotePathMap.put(AnalyzerType.MAVEN, remoteRepo);
                        }
                    }
                }
            }
            NodeList nodeList = doc.getElementsByTagName("localRepository");
            if (nodeList.getLength() > 0) {
               String mavenRepo = nodeList.item(0).getTextContent().trim();
               pathMap.put(AnalyzerType.MAVEN,mavenRepo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
