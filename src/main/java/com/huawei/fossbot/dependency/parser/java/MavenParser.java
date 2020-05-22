package com.huawei.fossbot.dependency.parser.java;

import com.huawei.avenuesearch.utility.TimeMeter;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MavenParser
 *
 * @author d00380361 dongkeqin@huawei.com
 * @since 2019/10/15 9:11
 */
public class MavenParser {
    private static Path repoPath;
    private static String remoteRepo;

    public static Path getRepoPath() {
        return repoPath;
    }

    static {
        Path settingPath = Paths.get(System.getProperty("user.home"), ".m2").resolve("settings.xml");
        parseRepoPath(settingPath);
        if (repoPath == null) {
            settingPath = Paths.get(System.getenv("m2_home"), "conf").resolve("settings.xml");
            parseRepoPath(settingPath);
        }
        if (repoPath == null) {
            settingPath = Paths.get(System.getenv("maven_home"), "conf").resolve("settings.xml");
            parseRepoPath(settingPath);
        }
        if (repoPath == null) {
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
                        parseRepoPath(settingPath);
                        if (repoPath != null) break;
                    }
                }
            }
        }
        if (repoPath == null) {
            repoPath = Paths.get(System.getProperty("user.home") + "/.m2").resolve("repository");
        }
    }

    /**
     * 解析本地仓库地址
     *
     * @param settingPath settings.xml路径
     */
    private static void parseRepoPath(Path settingPath) {
        if (!settingPath.toFile().exists()) {
            return;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(settingPath.toFile());
            if (StringUtils.isBlank(remoteRepo)) {
                // 设置远程仓地址
                NodeList urlList = doc.getElementsByTagName("url");
                if (urlList.getLength() > 0) {
                    for (int i = 0; i < urlList.getLength(); i++) {
                        if ("mirror".equals(urlList.item(i).getParentNode().getNodeName())) {
                            remoteRepo = urlList.item(i).getTextContent();
                        }
                    }
                }
            }

            NodeList nodeList = doc.getElementsByTagName("localRepository");
            if (nodeList.getLength() > 0) {
                repoPath = Paths.get(nodeList.item(0).getTextContent().trim());
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析一个pom文件的依赖
     *
     * @param path pom.xml路径
     * @return 解析结果
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private static PomFile parsePomWithDep(Path path) throws IOException, SAXException, ParserConfigurationException {
        Map<Path, PomFile> parsedMap = new HashMap<>();
        PomFile pomFile = parsePom(path, parsedMap);
        if (pomFile == null) return null;
        Queue<Pair<Set<String>, Artifact>> artifacts = new LinkedList<>();
        addDependencyArtifacts(artifacts, pomFile, pomFile, new HashSet<>());
        Set<PomFile> usedPoms = new HashSet<>();
        usedPoms.add(pomFile);
        while (!artifacts.isEmpty()) {
            Pair<Set<String>, Artifact> pair = artifacts.poll();
            Set<String> excludes = pair.getLeft();
            Artifact artifact = pair.getRight();
            PomFile artPom = parsePom(repoPath.resolve(artifact.getPomPath()), parsedMap);
            if (artPom == null) continue;
            if (usedPoms.contains(artPom)) continue;
            usedPoms.add(artPom);
            artPom.dependencies.values().forEach(a -> {
                a.setSource(artifact);
            });
            artPom.dependencies.values().forEach(a -> a.setSource(artifact));
            if (artifact.excludes != null)
                excludes.addAll(artifact.excludes);
            addDependencyArtifacts(artifacts, pomFile, artPom, excludes);
        }
        return pomFile;
    }

    /**
     * 返回一个pom文件的依赖pom的路径列表
     *
     * @param path pom文件路径
     * @return 依赖pom的路径列表
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static List<String> listDependencies(
            Path path) throws IOException, ParserConfigurationException, SAXException {
        PomFile pomFile = parsePomWithDep(path);
        return pomFile.dependencies.values().stream()
                .map(a -> repoPath.resolve(a.getJarPath()).toAbsolutePath().toString())
                .collect(Collectors.toList());
    }

    /**
     * 在队列中添加一个pom文件的依赖
     *
     * @param artifacts 队列
     * @param pomFile   目标pom文件
     * @param artDep    添加的pom文件
     * @param excludes  排除的pom
     */
    private static void addDependencyArtifacts(Queue<Pair<Set<String>, Artifact>> artifacts, PomFile pomFile,
                                               PomFile artDep, Set<String> excludes) {
        for (Artifact artifact : artDep.dependencies.values()) {
            if (excludes.contains(artifact.getKey())) continue;
            if (artifact.optional) continue;
            if (artifact.scope != null && artifact.source != null) {
                if (artifact.scope.equals("test")) continue;
                if (artifact.scope.equals("provided")) continue;
            }
            if (!pomFile.addDependency(artifact) && pomFile != artDep) continue;
            Set<String> newExcludes = excludes;
            if (artifact.excludes != null && !artifact.excludes.isEmpty()) {
                newExcludes = new HashSet<>(excludes);
                newExcludes.addAll(artifact.excludes);
            }
            artifacts.add(Pair.of(newExcludes, artifact));
        }
    }

    /**
     * 解析一个pom文件，结果放进parsedMap
     *
     * @param path      pom文件路径
     * @param parsedMap 已解析的所有pom文件
     * @return 当前pom的解析结果
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private static PomFile parsePom(Path path, Map<Path, PomFile> parsedMap)
            throws ParserConfigurationException, IOException, SAXException {
        if (!path.toFile().exists()) {
            // 如果文件不存在, 通过远程仓下载文件
            downloadFile(remoteRepo, path.getParent().toFile().getAbsolutePath() + "/", path.getFileName().toString());
            if (!path.toFile().exists()) {
                return null;// 下载失败
            }
        }
        if (parsedMap.containsKey(path)) return parsedMap.get(path);
        parsedMap.put(path, null);
        PomFile pomFile = new PomFile(path);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(path.toFile());
        PomFile parentDep = null;
        Path parentPom = getParentPomPath(doc.getElementsByTagName("parent"), path);
        if (parentPom != null) {
            parentDep = parsePom(parentPom, parsedMap);
            if (parentDep != null) {
                parentDep.properties.forEach((key, value) -> pomFile.properties.putIfAbsent(key, value));
            }
        }
        NodeList versions = doc.getElementsByTagName("version");
        for (int i = 0; i < versions.getLength(); i++) {
            Node vnode = versions.item(i);
            if (!vnode.getParentNode().getNodeName().equals("project")) continue;
            pomFile.properties.put("project.version", vnode.getTextContent());// 设置工程version
            break;
        }
        NodeList groupIds = doc.getElementsByTagName("groupId");
        for (int i = 0; i < groupIds.getLength(); i++) {
            Node vnode = groupIds.item(i);
            if (!vnode.getParentNode().getNodeName().equals("project")) continue;
            pomFile.properties.put("project.groupId", vnode.getTextContent());// 设置工程groupId
            break;
        }
        NodeList properties = doc.getElementsByTagName("properties");
        for (int i = 0; i < properties.getLength(); i++) {
            Node pnode = properties.item(i);
            if (!pnode.getParentNode().getNodeName().equals("project")) continue;
            for (int j = 0; j < pnode.getChildNodes().getLength(); j++) {
                Node node = pnode.getChildNodes().item(j);
                String name = node.getNodeName();
                String value = node.getTextContent();
                if (name.equals("#text")) continue;
                pomFile.properties.put(name, value);
            }
        }
        NodeList dependencies = doc.getElementsByTagName("dependencies");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Node node = dependencies.item(i);
            Node parentNode = node.getParentNode();
            if (parentNode.getNodeName().equals("dependencyManagement")) {
                if (parentNode.getParentNode().getNodeName().equals("profile")) {
                    Node profile = parentNode.getParentNode();
                    if (!checkActivation(profile, pomFile)) {
                        continue;
                    }
                }
                NodeList nodeList = node.getChildNodes();
                List<Artifact> artifacts = parseArtifacts(nodeList);
                for (Artifact art : artifacts) {
                    pomFile.addDependencyManagement(art, parsedMap);
                }
            }
        }
        if (parentDep != null) {
            for (Artifact art : parentDep.dependencyManagement.values()) {
                pomFile.addDependencyManagement(art, parsedMap);
            }
        }
        for (int i = 0; i < dependencies.getLength(); i++) {
            Node node = dependencies.item(i);
            if (node.getParentNode().getNodeName().equals("profile")) {
                Node profile = node.getParentNode();
                if (checkActivation(profile, pomFile)) {
                    NodeList nodeList = node.getChildNodes();
                    parseArtifacts(nodeList).forEach(pomFile::addDependency);
                }
            }
        }
        for (int i = 0; i < dependencies.getLength(); i++) {
            Node node = dependencies.item(i);
            if (node.getParentNode().getNodeName().equals("project")) {
                NodeList nodeList = node.getChildNodes();
                parseArtifacts(nodeList).forEach(pomFile::addDependency);
            }
        }
        if (parentDep != null) {
            parentDep.dependencies.values().forEach(pomFile::addDependency);
        }
        parsedMap.put(path, pomFile);
        return pomFile;
    }

    private static void downloadFile(String repository, String localDir, String fileName) {
        URL fileUrl;
        HttpURLConnection httpUrl;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        File file = new File(localDir);
        file.mkdirs();
        file = new File(localDir + fileName);
        try {
            localDir = localDir.replace("\\", "/");
            //fileUrl = new URL(repository + localDir.split("m2/repository/")[1] + fileName);
            fileUrl = new URL(repository + localDir.split("D:/Maven/")[1] + fileName);
            httpUrl = (HttpURLConnection) fileUrl.openConnection();
            httpUrl.connect();
            bis = new BufferedInputStream(httpUrl.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(file));
            int len = 2048;
            byte[] b = new byte[len];
            while ((len = bis.read(b)) != -1) {
                bos.write(b, 0, len);
            }
            bos.flush();
            bis.close();
            httpUrl.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查当前profile是否有效
     *
     * @param profile profile节点
     * @param pomFile pom文件
     * @return 是否有效
     */
    private static boolean checkActivation(Node profile, PomFile pomFile) {
        boolean active = false;
        Node activationNode = getChild(profile, "activation");
        if (activationNode == null) return active;
        NodeList activationList = activationNode.getChildNodes();
        for (int i = 0; i < activationList.getLength(); i++) {
            Node tnode = activationList.item(i);
            if (tnode.getNodeName().equals("property")) {
                Node name = getChild(tnode, "name");
                Node value = getChild(tnode, "value");
                if (name.getTextContent().startsWith("!")) {
                    String key = name.getTextContent().substring(1);
                    if (!pomFile.properties.containsKey(key)) {
                        active = true;
                    }
                } else {
                    String pstr = pomFile.properties.get(name.getTextContent());
                    if (value == null) {
                        if (pstr != null) {
                            active = true;
                        }
                    } else if (value.getTextContent().startsWith("!")) {
                        String vstr = value.getTextContent().substring(1);
                        if (!vstr.equals(pstr)) {
                            active = true;
                        }
                    } else {
                        String vstr = value.getTextContent();
                        if (vstr.equals(pstr)) {
                            active = true;
                        }
                    }
                }
            } else if (tnode.getNodeName().equals("jdk")) {
                //todo
            } else if (tnode.getNodeName().equals("os")) {
                //todo
            } else if (tnode.getNodeName().equals("file")) {
                //todo
            } else if (tnode.getNodeName().equals("activeByDefault")) {
                if (tnode.getTextContent().equals("true")) {
                    active = true;
                }
            }
        }
        return active;
    }

    /**
     * 按名字返回一个节点的子节点
     *
     * @param node 父节点
     * @param name 节点名
     * @return 子节点
     */
    private static Node getChild(Node node, String name) {
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node tnode = nodeList.item(i);
            if (tnode.getNodeName().equals(name)) {
                return tnode;
            }
        }
        return null;
    }

    /**
     * 解析<dependencies></dependencies>中的列表
     *
     * @param nodeList
     * @return
     */
    private static List<Artifact> parseArtifacts(NodeList nodeList) {
        List<Artifact> artifacts = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (!node.getNodeName().equals("dependency")) continue;
            artifacts.add(parseArtifact(node));
        }
        return artifacts;
    }

    /**
     * 解析<exclusions></exclusions>中的列表
     *
     * @param nodeList
     * @return
     */
    private static List<Artifact> parseExclusions(NodeList nodeList) {
        List<Artifact> artifacts = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (!node.getNodeName().equals("exclusion")) continue;
            artifacts.add(parseArtifact(node));
        }
        return artifacts;
    }

    /**
     * 解析一个依赖项，即<dependency></dependency>中的内容
     *
     * @param node
     * @return
     */
    private static Artifact parseArtifact(Node node) {
        Artifact artifact = new Artifact();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            String name = children.item(i).getNodeName();
            switch (name) {
                case "groupId": {
                    artifact.groupId = children.item(i).getTextContent();
                    break;
                }
                case "artifactId": {
                    artifact.artifactId = children.item(i).getTextContent();
                    break;
                }
                case "version": {
                    artifact.version = children.item(i).getTextContent();
                    break;
                }
                case "classifier": {
                    artifact.classifier = children.item(i).getTextContent();
                    break;
                }
                case "exclusions": {
                    artifact.excludes = parseExclusions(children.item(i).getChildNodes())
                            .stream().map(Artifact::getKey).collect(Collectors.toSet());
                    break;
                }
                case "scope": {
                    artifact.scope = children.item(i).getTextContent();
                    break;
                }
                case "optional": {
                    String value = children.item(i).getTextContent();
                    if (value.equals("true"))
                        artifact.optional = true;
                    break;
                }
            }
        }
        return artifact;
    }

    /**
     * 得到父pom的路径
     *
     * @param nodeList
     * @param pomPath
     * @return
     */
    private static Path getParentPomPath(NodeList nodeList, Path pomPath) {
        if (nodeList.getLength() > 0) {
            String parentGroupId = null;
            String parentArtifactId = null;
            String parentVersion = null;
            String relativePath = null;
            NodeList children = nodeList.item(0).getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                String name = children.item(i).getNodeName();
                switch (name) {
                    case "groupId": {
                        parentGroupId = children.item(i).getTextContent();
                        break;
                    }
                    case "artifactId": {
                        parentArtifactId = children.item(i).getTextContent();
                        break;
                    }
                    case "version": {
                        parentVersion = children.item(i).getTextContent();
                        break;
                    }
                    case "relativePath": {
                        relativePath = children.item(i).getTextContent();
                        break;
                    }
                }
            }
            Path path = null;
            if (relativePath == null) {
                path = pomPath.getParent().getParent().resolve("pom.xml");
            } else {
                if (!relativePath.isEmpty()) {
                    path = pomPath.getParent().resolve(relativePath).resolve("pom.xml");
                }
            }
            if (path == null || !path.toFile().exists()) {
                path = repoPath
                        .resolve(parentGroupId.replace(".", "/"))
                        .resolve(parentArtifactId)
                        .resolve(parentVersion)
                        .resolve(parentArtifactId + "-" + parentVersion + ".pom");
            }
            return path;
        }
        return null;
    }

    /**
     * pom文件
     */
    public static class PomFile {
        Path path;
        Map<String, Artifact> dependencies = new ListOrderedMap<>();
        Map<String, Artifact> dependencyManagement = new HashMap<>();
        Map<String, String> properties = new HashMap<>();

        PomFile(Path path) {
            this.path = path;
        }

        boolean addDependency(Artifact artifact) {
            artifact.applyProperties(this);
            String key = artifact.getKey();
            if (dependencyManagement.containsKey(key)) {
                Artifact ma = dependencyManagement.get(key);
                artifact.version = ma.version;
                if (artifact.excludes == null)
                    artifact.excludes = ma.excludes;
                if (artifact.scope == null)
                    artifact.scope = ma.scope;
                artifact.classifier = ma.classifier;
            }
            if (!dependencies.containsKey(key)) {
                dependencies.put(key, artifact);
                return true;
            }
            return false;
        }

        boolean addDependencyManagement(Artifact artifact, Map<Path, PomFile> parsedMap)
                throws IOException, SAXException, ParserConfigurationException {
            artifact.applyProperties(this);
            if ("import".equals(artifact.scope)) {
                PomFile artPom = parsePom(repoPath.resolve(artifact.getPomPath()), parsedMap);
                for (Artifact art : artPom.dependencyManagement.values()) {
                    addDependencyManagement(art, parsedMap);
                }
                return true;
            }
            String key = artifact.getKey();
            if (!dependencyManagement.containsKey(key)) {
                dependencyManagement.put(key, artifact);
                return true;
            }
            return false;
        }
    }

    /**
     * 依赖项
     */
    private static class Artifact implements Comparable<Artifact> {
        String groupId;
        String artifactId;
        String version;
        String classifier;
        String scope;
        boolean optional = false;
        Set<String> excludes;
        String source;

        @Override
        public int compareTo(Artifact o) {
            if (groupId.equals(o.groupId)) {
                return artifactId.compareTo(o.artifactId);
            }
            return groupId.compareTo(o.groupId);
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + (version == null ? ":" : ":" + version);
        }

        public String getKey() {
            return groupId + ":" + artifactId;
        }

        Artifact copy() {
            Artifact artifact = new Artifact();
            artifact.groupId = groupId;
            artifact.artifactId = artifactId;
            artifact.version = version;
            artifact.classifier = classifier;
            artifact.optional = optional;
            return artifact;
        }

        String getPomPath() {
            StringBuilder sb = new StringBuilder();
            sb.append(groupId.replace(".", "/"));
            sb.append("/");
            sb.append(artifactId);
            sb.append("/");
            sb.append(version);
            sb.append("/");
            sb.append(artifactId);
            sb.append("-");
            sb.append(version);
            sb.append(".pom");
            return sb.toString();
        }

        String getJarPath() {
            StringBuilder sb = new StringBuilder();
            sb.append(groupId.replace(".", "/"));
            sb.append("/");
            sb.append(artifactId);
            sb.append("/");
            sb.append(version);
            sb.append("/");
            sb.append(artifactId);
            sb.append("-");
            sb.append(version);
            if (classifier != null) {
                sb.append("-");
                sb.append(classifier);
            }
            sb.append(".jar");
            return sb.toString();
        }
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Artifact)) return false;
            Artifact artifact = (Artifact) obj;
            return StringUtils.equals(groupId, artifact.groupId)
                    && StringUtils.equals(artifactId, artifact.artifactId)
                    && StringUtils.equals(version, artifact.version)
                    && StringUtils.equals(classifier, artifact.classifier);
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (groupId != null) hash += groupId.hashCode();
            if (artifactId != null) hash += artifactId.hashCode();
            if (version != null) hash += version.hashCode();
            if (classifier != null) hash += classifier.hashCode();
            return hash;
        }

        void applyProperties(PomFile pom) {
            groupId = applyProperty(groupId, pom);
            artifactId = applyProperty(artifactId, pom);
            version = applyProperty(version, pom);
            classifier = applyProperty(classifier, pom);
        }

        private static String applyProperty(String s, PomFile pom) {
            if (s == null) return null;
            int p1 = s.indexOf("${");
            if (p1 == -1) return s;
            int p2 = s.indexOf("}", p1);
            if (p2 == -1) return s;
            String value = s.substring(p1 + 2, p2);
            value = pom.properties.get(value);
            if (value == null)
                return s;
            if (value.contains("${"))
                value = applyProperty(value, pom);
            return s.substring(0, p1) + value + applyProperty(s.substring(p2 + 1), pom);
        }

        public void setSource(Artifact artifact) {
            source = artifact.toString();
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getClassifier() {
            return classifier;
        }

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public boolean isOptional() {
            return optional;
        }

        public void setOptional(boolean optional) {
            this.optional = optional;
        }

        public Set<String> getExcludes() {
            return excludes;
        }

        public void setExcludes(Set<String> excludes) {
            this.excludes = excludes;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    public static List<Artifact> mavenParse(String pomFilePath) throws ParserConfigurationException, SAXException, IOException {
        if (StringUtils.isBlank(pomFilePath)) {
            return null;
        }
        Path pom = Paths.get(pomFilePath);
        TimeMeter tm = new TimeMeter();
        PomFile pomFile = parsePomWithDep(pom);
        tm.println("parse");
        List<Artifact> artifacts = new ArrayList<>(pomFile.dependencies.values());
        artifacts.sort(null);
        return artifacts;
    }
}
