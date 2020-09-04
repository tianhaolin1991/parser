package com.huawei.fossbot.dependency.util;

import com.huawei.fossbot.dependency.bean.RepoConstant;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author t30002128
 * @since 2020/05/20
 */
public class RepoPathUtil {

    private static String DEFAULTMAVENLOCALREPO = Paths.get(System.getProperty("user.home") + "/.m2").resolve("repository")
            .toString().replace("\\", "/");

    public static String getGradleLocalRepo(){
        Path gradleRepoPath = Paths.get(System.getProperty("GRADLE_USER_HOME"), ".gradle");
        if (!gradleRepoPath.toFile().exists()) {
            gradleRepoPath = Paths.get(System.getProperty("user.home"), ".gradle");
        }
        Path gradlePath = gradleRepoPath.resolve("caches").resolve("modules-2").resolve("files-2.1");
        return gradlePath.toString().replace("\\","/");
    }


    public static String getMavenSettingPath() {
        Path settingPath = Paths.get(System.getProperty("user.home"), ".m2").resolve("settings.xml");
        if (!settingPath.toFile().exists()) {
            settingPath = Paths.get(System.getenv("M2_HOME"), "conf").resolve("settings.xml");
        }
        if (!settingPath.toFile().exists()) {
            settingPath = Paths.get(System.getenv("m2_home"), "conf").resolve("settings.xml");
        }
        if (!settingPath.toFile().exists()) {
            settingPath = Paths.get(System.getenv("MAVEN_HOME"), "conf").resolve("settings.xml");
        }
        if (!settingPath.toFile().exists()) {
            settingPath = Paths.get(System.getenv("maven_home"), "conf").resolve("settings.xml");
        }
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
        if(!settingPath.toFile().exists()){
            return null;
        }else{
            return settingPath.toString().replace("\\","/");
        }
    }

    public static String getMavenLocalRepo(String settings){
        if(settings==null){
            return DEFAULTMAVENLOCALREPO;
        }else{
            return parseMavenLocalRepo(settings);
        }
    }

    public static String getMavenRemoteRepo(String settings){
        if(settings==null){
            return RepoConstant.MAVEN_CENTRAL;
        }else{
            return parseMavenRemoteRepo(settings);
        }
    }

    private static String parseMavenLocalRepo(String settings) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            Document doc = dbf.newDocumentBuilder().parse(new File(settings));
            NodeList nodeList = doc.getElementsByTagName("localRepository");

            if (nodeList.getLength() > 0) {
                String localRepo =  nodeList.item(0).getTextContent().trim();
                String absolutePath = new File(localRepo).getCanonicalPath();
                return absolutePath.replace("\\","/");
            }

        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
        return DEFAULTMAVENLOCALREPO;
    }

    private static String parseMavenRemoteRepo(String settings) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            Document doc = dbf.newDocumentBuilder().parse(new File(settings));
            // 设置远程仓地址
                NodeList urlList = doc.getElementsByTagName("url");
                if (urlList.getLength() > 0) {
                    for (int i = 0; i < urlList.getLength(); i++) {
                        if ("mirror".equals(urlList.item(i).getParentNode().getNodeName())) {
                            return urlList.item(i).getTextContent();
                        }
                    }
                }
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
        return DEFAULTMAVENLOCALREPO;
    }
}
