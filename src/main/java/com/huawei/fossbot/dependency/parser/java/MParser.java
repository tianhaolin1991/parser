package com.huawei.fossbot.dependency.parser.java;

import com.huawei.fossbot.dependency.bean.AnalyzerType;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.util.RepoPathUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * pom文件解析原则:
 * 1.依赖管理优先级:dependency->management->parent management->import management(按顺序)
 * 2.依赖解析优先级:
 * a)最短路径原则
 * b)声明优先原则
 * 说明:parent/import的依赖优先级高于同深度下一顺序的依赖,处理时将parent/imports依赖与当前pom依赖合并处理
 * 即顺序A,B,B依赖了C2,A的parent/imports依赖了C1则最终选用C1
 * 3.optional:当前紧当rootPom(包括其所有parent)的optional为true时会进行依赖解析
 * 4.test:
 * a)仅有rootPom下的test会被解析
 * b)传递依赖compile的scope会被解析为test(如果尚未被解析的话)
 * 5.exclusions:
 * 最近的management.dependency add all dependency
 */
public class MParser {

    private static Map<String,Integer> orderMap = new HashMap<>();
    static{
        orderMap.put("compile",1);
        orderMap.put("runtime",2);
        orderMap.put("test",3);
        orderMap.put("provided",4);
    }

    private String localRepo;
    private String remoteRepo;
    private SAXReader saxReader = new SAXReader();
    private PomFile rootPomFile;
    private Map<String, PomFile> parsedMap = new LinkedHashMap<>();
    private Map<String, PomFile> readMap = new LinkedHashMap<>();
    private Map<String, String> versionMap = new LinkedHashMap<>();
    private Queue<PomFile> readQueue = new LinkedList<>();
    private Queue<PomFile> parseQueue = new LinkedList<>();

    public MParser() {
        localRepo = RepoPathUtil.getRepoPath(AnalyzerType.MAVEN);
        remoteRepo = RepoPathUtil.getRemoteRepoPath(AnalyzerType.MAVEN);
    }

    public MParser(String localRepo, String remoteRepo) {
        this.localRepo = localRepo;
        this.remoteRepo = remoteRepo;
    }

    public List<Artifact> parse(String profile) {
        try {
            if (StringUtils.isBlank(profile)) {
                return null;
            }
            Path pomPath = Paths.get(profile);
            //pomFile是一个树形结构
            Pom pom = parsePomWithDep(pomPath);
            HashMap<String, Artifact> artifactMap = new HashMap<>();
            mergePomDependencies(pom, artifactMap, new HashMap<>(), 0);
            return new ArrayList<>(artifactMap.values());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 合并所有的artifacts(包括依赖冲突解决)
     */
    private void mergePomDependencies(Pom pom, HashMap<String, Artifact> artifactMap, HashMap<String, Integer> depthMap, Integer depth) {
        Map<String, Pom> dependencies = pom.dependencies;

        for (Pom dependency : dependencies.values()) {
            if (artifactMap.containsKey(dependency.artifact.getKey())) {
                resolveConflicts(dependency.artifact, artifactMap, depthMap, depth);
            } else {
                artifactMap.put(dependency.artifact.getKey(), dependency.artifact);
                depthMap.put(dependency.artifact.getKey(), depth);
            }
            mergePomDependencies(dependency, artifactMap, depthMap, depth++);
        }
    }

    /**
     * 1.路径最短原则:由depth来决定
     * 2.优先声名原则:由于在解析dependencies时是orderedMap,所以该原则自动满足根据声名顺序来的
     *
     * @param depth : 声名的深度
     */
    private void resolveConflicts(Artifact newArtifact, HashMap<String, Artifact> artifactMap, HashMap<String, Integer> depthMap, Integer depth) {
        Integer originDepth = depthMap.get(newArtifact.getKey());
        if (originDepth > depth) {//路径最短原则
            artifactMap.put(newArtifact.getKey(), newArtifact);
        }
    }

    public Pom parsePomWithDep(Path pomPath) throws Exception {

        Pom pom = parsePom(pomPath);

        return pom;
    }

    private Pom parsePom(Path pomPath) throws Exception {

        generateParseTaskQueue(pomPath);

        while (!parseQueue.isEmpty()) {
            PomFile poll = parseQueue.poll();
            Artifact artifact = poll.self;
            System.out.println(artifact.getKey() + ":jar:" + artifact.getVersion() + ":" + artifact.getScope());
        }

        return null;

        //return doParsePom(pomFile, inheritedExclusions);
    }

    /**
     * 1.路径最短原则
     * 2.优先声名原则
     *
     * @param
     */
    private void generateParseTaskQueue(Path pomPath) throws Exception {
        PomFile rootPomFile = resolvePomFile(pomPath, null);
        this.rootPomFile = rootPomFile;
        readQueue.offer(rootPomFile);
        while (!readQueue.isEmpty()) {
            PomFile pomFile = readQueue.poll();
            parseQueue.offer(pomFile);
            parsedMap.put(pomFile.self.getKey(),pomFile);
            Map<String, Set<String>> exclusions = pomFile.exclusions;
            for (Artifact dependency : pomFile.dependencies.values()) {
                String key = dependency.getKey();

                dependency = getRealDependency(dependency);
                if (exclusions.containsKey(pomFile.self.getKey())) {
                    if (exclusions.get(pomFile.self.getKey()).contains(key))
                        continue;
                }
                if (dependency.isOptional()) {
                    //如果是optional,则代表依赖不继承
                    if (pomFile != this.rootPomFile) {
                        continue;
                    }
                }

                //如果已经读过,则不再放入readQueue
                if (readMap.containsKey(key)) {
                    Artifact read = readMap.get(key).self;
                    if(hasHigherPriority(dependency.getScope(),read.getScope())){
                        read.setScope(dependency.getScope());
                    }
                    continue;
                }

                if ("provided".equals(dependency.getScope())) continue;
                if ("test".equals(dependency.getScope())) {
                    if (pomFile.tests.contains(key)) {
                        PomFile dependencyPomFile = resolvePomFile(getArtifactPath(dependency), pomFile);
                        readQueue.offer(dependencyPomFile);
                    }
                    continue;
                }

                PomFile dependencyPomFile = resolvePomFile(getArtifactPath(dependency), pomFile);
                readQueue.offer(dependencyPomFile);

            }
        }
    }

    private PomFile resolveTestPomFile(Artifact dependency, PomFile root) {
        PomFile pomFile = new PomFile(getArtifactPath(dependency));
        pomFile.root = root;
        pomFile.self = dependency;
        return pomFile;
    }

    private Map<String, Set<String>> inheritExclusions(Map<String, Set<String>> rootExclusions, PomFile pomFile) {
        Map<String, Set<String>> originExclusions = pomFile.exclusions;
        for (String key : rootExclusions.keySet()) {
            if (originExclusions.containsKey(key)) {
                originExclusions.get(key).addAll(rootExclusions.get(key));
            } else {
                originExclusions.putIfAbsent(key, rootExclusions.get(key));
            }
        }
        return originExclusions;
    }

    /*
    private Pom doParsePom(PomFile pomFile, Map<String, Set<String>> exclusions) throws Exception {

        Path pomPath = pomFile.path;

        //TODO-THROW EXCEPTION
        if (!checkPomPath(pomPath)) return null;

        if (parsedMap.containsKey(pomPath)) {
            return null;
            //return parsedMap.get(pomPath);
        }

        if (!downloadFilesIfNecessary(pomPath)) {
            //TODO--EXCEPTION
            return null;
        }
        Pom pom = new Pom(pomPath);
        parsedMap.putIfAbsent(pomPath, pom);

        //parseDependencies(pom, pomFile, exclusions);

        return pom;
    }

    private void parseDependencies(Pom pom, PomFile pomFile, Map<String, Set<String>> exclusions) throws Exception {
        Map<String, Artifact> dependencies = pomFile.dependencies;
        for (String key : dependencies.keySet()) {
            Artifact dependency = getRealDependency(dependencies.get(key));
            //Artifact dependency = dependencies.get(key);
            if (exclusions.containsKey(pomFile.self.getKey())) {
                if (exclusions.get(pomFile.self.getKey()).contains(dependency.getKey()))
                    continue;
            }
            if (dependency.isOptional()) {
                //如果是optional,则代表依赖不继承
                if (pomFile != this.rootPomFile) {
                    continue;
                }
            }

            if ("provided".equals(dependency.getScope())) continue;
            if ("test".equals(dependency.getScope())) {
                if(pomFile==this.rootPomFile){
                    //将该dependency的基本信息解析
                    Path dependencyPath = getArtifactPath(dependency);
                    Pom dependencyPom = doParseTestDependency(dependencyPath, dependency, pom);

                    PomFile dependencyPomFile = resolvePomFile(dependencyPath);
                    for (Artifact ddpendency : dependencyPomFile.dependencies.values()) {
                        doParseTestDependency(getArtifactPath(ddpendency),ddpendency,dependencyPom);
                    }
                }
                continue;
            }

            //开始解析

            doParseDependency(dependency, pom, exclusions);

        }
    }

    private Pom doParseTestDependency(Path dependencyPath, Artifact dependency, Pom pom) {
        Pom dependencyPom = new Pom(dependencyPath);
        dependencyPom.artifact = dependency;
        parsedMap.putIfAbsent(dependencyPath, dependencyPom);
        dependencyPom.artifact = dependency;
        pom.dependencies.putIfAbsent(dependency.getKey(), dependencyPom);
        return dependencyPom;
    }


    private void doParseDependency(Artifact dependency, Pom pom, Map<String, Set<String>> exclusions) throws Exception {
        Path dependencyPath = getArtifactPath(dependency);
        Pom dependencyPom = parsePom(dependencyPath, exclusions);

        if (dependencyPom != null) {
            dependencyPom.artifact = dependency;
            pom.dependencies.putIfAbsent(dependency.getKey(), dependencyPom);
        }
    }
    */

    private Artifact getRealDependency(Artifact artifact) {
        Artifact real = artifact.copy();
        real.setVersion(versionMap.get(real.getKey()));
        return real;
    }


    private PomFile resolvePomFile(Path path, PomFile root) throws Exception {


        PomFile pomFile = doResolvePomFile(path, root);

        adaptOptional(pomFile);

        collectTests(pomFile);
        //将优先级最高的artifact保存起来
        fillVersionMap(pomFile);
        //TODO-doc是否需要关闭?saxReader是否需要关闭?
        return pomFile;
    }

    private void adaptOptional(PomFile pomFile) {
        if(pomFile.root!=null){
            pomFile.dependencies.entrySet().removeIf(entry -> entry.getValue().isOptional());
        }
    }

    private PomFile doResolvePomFile(Path path, PomFile root) throws Exception {
        //TODO--THROW EXCEPTION
        if (!checkPomPath(path)) return null;

        if (!downloadFilesIfNecessary(path)) {
            //TODO--EXCEPTION
            return null;
        }

        String key = pathToArtifactKey(path);

        if (readMap.containsKey(key)) {
            return readMap.get(key);
        }

        PomFile pomFile = new PomFile(path);
        pomFile.root = root;

        readMap.putIfAbsent(key, pomFile);

        Document doc = saxReader.read(path.toFile());
        Element rootElement = doc.getRootElement();


        resolveProperties(rootElement, pomFile, root);

        resolveManagement(rootElement, pomFile);

        resolveDependencies(rootElement, pomFile);

        //分析完dependencies之后最终确定management信息
        //changeManagement(pomFile);
        collectExclusions(pomFile);
        return pomFile;
    }


    private void resolvePostProcess(PomFile pomFile) {


    }

    /**
     * 说明:该方法用于识别最终可能被解析为test的key值
     *  1)rootPomFile的test会被解析,所以将scope=test的放入pomFile.tests
     *  2)非rootPomFile会从其root的tests中查找自己,如果找到了,则证明是需要解析的,那么compile和runtime类型将被解析为test
     *  说明:非rootPomFile在开始时tests是空的,如果他的root最终不是来自于rootPomFile,他的test将不会被解析
     * @param pomFile
     */
    private void collectTests(PomFile pomFile) {
        if (this.rootPomFile == null) {
            //将pomFile的dependencies中的test提取出来,用于后续解析
            for (Artifact dependency : pomFile.dependencies.values()) {
                if ("test".equals(dependency.getScope())) {
                    pomFile.tests.add(dependency.getKey());
                }
            }
        }else{
            PomFile root = pomFile.root;
            if(root.tests.contains(pomFile.self.getKey())){
                for (Artifact artifact : pomFile.dependencies.values()) {
                    //将所有不是test和provided的dependency提取出来,用作后续解析,
                    if (!artifact.getScope().equals("test") && !artifact.getScope().equals("provided")) {
                        pomFile.tests.add(artifact.getKey());
                        artifact.setScope("test");
                    }
                }
            }
        }
    }

    private void collectExclusions(PomFile pomFile) {
        for (Artifact artifact : pomFile.management.values()) {
            Set<String> exclusions = artifact.getExcludes();
            if (CollectionUtils.isNotEmpty(exclusions)) {
                pomFile.exclusions.putIfAbsent(artifact.getKey(), exclusions);
            }
        }
        for (Artifact artifact : pomFile.dependencies.values()) {
            Set<String> excludes = artifact.getExcludes();
            String key = artifact.getKey();
            if (pomFile.exclusions.containsKey(key)) {
                pomFile.exclusions.get(key).addAll(excludes);
            } else {
                pomFile.exclusions.putIfAbsent(key, excludes);
            }
        }
        if (pomFile.root != null) {
            PomFile rootPomFile = pomFile.root;
            for (String key : rootPomFile.exclusions.keySet()) {
                if (pomFile.exclusions.containsKey(key)) {
                    pomFile.exclusions.get(key).addAll(rootPomFile.exclusions.get(key));
                } else {
                    pomFile.exclusions.putIfAbsent(key, rootPomFile.exclusions.get(key));
                }
            }
        }

    }

    private void resolveManagement(Element root, PomFile pomFile) throws Exception {
        readManagement(root, pomFile);
        if (pomFile.parent != null) {
            PomFile parentPomFile = doResolvePomFile(getParentPomPath(pomFile), null);
            mergeInformation(parentPomFile.management, pomFile.management);
        }
        for (String key : pomFile.imports.keySet()) {
            PomFile importPomFile = doResolvePomFile(getArtifactPath(pomFile.imports.get(key)), null);
            mergeInformation(importPomFile.management, pomFile.management);
        }
        fixManagement(pomFile);
    }

    private <T> void mergeInformation(Map<String, T> from, Map<String, T> to) {
        for (String key : from.keySet()) {
            //合并properties->if absent->pomFile的properties在之前已经解析完了
            to.putIfAbsent(key, from.get(key));
        }
    }

    /**
     * 规则:1.由最接近root的depedency->management决定最终版本号
     *
     * @param pomFile
     */
    private void fillVersionMap(PomFile pomFile) {
        for (String key : pomFile.dependencies.keySet()) {
            versionMap.putIfAbsent(key, pomFile.dependencies.get(key).getVersion());
        }
        for (String key : pomFile.management.keySet()) {
            versionMap.putIfAbsent(key, pomFile.management.get(key).getVersion());
        }
    }

    private void fixManagement(PomFile pomFile) {
        for (String key : pomFile.management.keySet()) {
            Artifact management = pomFile.management.get(key);
            if (management.getScope() == null) {
                //如果最终解析scope仍然为null,那么scope为默认值("compile")
                management.setScope("compile");
            }
        }
    }

    private String pathToArtifactKey(Path path) throws Exception {
        if (path.endsWith("pom.xml")) {
            Element root = saxReader.read(path.toFile()).getRootElement();
            PomFile temp = new PomFile(path);
            readBaseInformation(root, temp, null);
            return temp.self.getKey();
        }
        if (path.toString().endsWith(".pom")) {
            path = path.getParent().getParent();
            String artifactId = path.toFile().getName();
            path = path.getParent();
            String groupId = path.toString().replace("\\", "/").split(localRepo + "/")[1].replace("/", ".");
            return groupId + ":" + artifactId;
        }
        //TODO-THROWEXCEPTION
        return null;
    }

    private void resolveProperties(Element rootElement, PomFile pomFile, PomFile rootPomFile) throws Exception {
        readBaseInformation(rootElement, pomFile, rootPomFile);
        readProperties(rootElement, pomFile);
        mergeProperties(pomFile);
    }

    private void mergeProperties(PomFile pomFile) throws Exception {
        if (pomFile.parent != null) {
            PomFile parentPomFile = doResolvePomFile(getParentPomPath(pomFile), null);
            mergeInformation(parentPomFile.properties, pomFile.properties);
        }
        for (String key : pomFile.imports.keySet()) {
            PomFile importPomFile = doResolvePomFile(getArtifactPath(pomFile.imports.get(key)), null);
            mergeInformation(importPomFile.properties, pomFile.properties);
        }
    }

    private void resolveDependencies(Element root, PomFile pomFile) throws Exception {
        readDependencies(root, pomFile);
        if (pomFile.parent != null) {
            PomFile parentPomFile = doResolvePomFile(getParentPomPath(pomFile), null);
            mergeInformation(parentPomFile.dependencies, pomFile.dependencies);
        }
        for (String key : pomFile.imports.keySet()) {
            PomFile importPomFile = doResolvePomFile(getArtifactPath(pomFile.imports.get(key)), null);
            mergeInformation(importPomFile.dependencies, pomFile.dependencies);
        }
        fixDependencies(pomFile);
    }

    /**
     * 优先级
     * 1.dependencies信息
     * 2.management信息
     * 3.parent信息
     * 4.imports信息
     *
     * @param pomFile
     */
    private void changeManagement(PomFile pomFile) {
        for (String key : pomFile.dependencies.keySet()) {
            Artifact management = pomFile.dependencies.get(key);
            pomFile.management.put(management.getKey(), management);
        }
    }

    private void fixDependencies(PomFile pomFile) {
        for (String key : pomFile.dependencies.keySet()) {
            Artifact dependency = pomFile.dependencies.get(key);
            //填充不完整信息
            if (pomFile.management.containsKey(dependency.getKey())) {
                Artifact management = pomFile.management.get(dependency.getKey());
                String version = dependency.getVersion();
                if (version == null) {
                    dependency.setVersion(management.getVersion());
                }
                String scope = dependency.getScope();
                if (scope == null) {
                    dependency.setScope(management.getScope());
                }
                Set<String> excludes = dependency.getExcludes();
                excludes.addAll(management.getExcludes());
            } else {
                if (dependency.getScope() == null) {
                    dependency.setScope("compile");
                }
            }
        }
    }

    private void mergeDependencies(PomFile from, PomFile to) {
        for (String key : from.dependencies.keySet()) {
            Artifact dependency = from.dependencies.get(key);
            to.dependencies.putIfAbsent(dependency.getKey(), dependency);
        }
    }


    private void readProperties(Element root, PomFile pomFile) {
        Element propertiesTag = root.element("properties");
        if (propertiesTag != null) {
            List<Element> propertyTags = propertiesTag.elements();
            for (Element propertyTag : propertyTags) {
                pomFile.properties.putIfAbsent(propertyTag.getName(), propertyTag.getTextTrim());
            }
        }
    }

    private void readBaseInformation(Element root, PomFile pomFile, PomFile rootPomFile) {

        Element groupIdTag = root.element("groupId");

        if (groupIdTag != null) {
            String groupId = groupIdTag.getTextTrim();
            if (groupId != null) pomFile.properties.putIfAbsent("project.groupId", groupId);
        }
        Element artifactIdTag = root.element("artifactId");
        if (artifactIdTag != null) {
            String artifactId = artifactIdTag.getTextTrim();
            if (artifactId != null) pomFile.properties.putIfAbsent("project.artifactId", artifactId);
        }
        Element versionTag = root.element("version");
        if (versionTag != null) {
            String version = versionTag.getTextTrim();
            if (version != null) pomFile.properties.putIfAbsent("project.version", version);
        }

        Element parentTag = root.element("parent");
        if (parentTag != null) {
            pomFile.parent = getPomArtifact(parentTag, pomFile);
            pomFile.properties.putIfAbsent("project.groupId", pomFile.parent.getGroupId());
            pomFile.properties.putIfAbsent("project.parent.groupId", pomFile.parent.getGroupId());

            pomFile.properties.putIfAbsent("project.parent.artifactId", pomFile.parent.getArtifactId());

            pomFile.properties.putIfAbsent("project.version", pomFile.parent.getVersion());
            pomFile.properties.putIfAbsent("project.parent.version", pomFile.parent.getVersion());

            Element relativePathTag = parentTag.element("relativePath");
            if (relativePathTag != null) {
                String relativePath = relativePathTag.getTextTrim();
                if (relativePath == null) {
                    pomFile.properties.putIfAbsent("relativePath", "DEFAULT");
                } else {
                    pomFile.properties.putIfAbsent("relativePath", relativePath);
                }
            }
        }
        Artifact self = new Artifact();
        self.setGroupId(pomFile.properties.get("project.groupId"));
        self.setArtifactId(pomFile.properties.get("project.artifactId"));
        self.setVersion(pomFile.properties.get("project.version"));
        if (rootPomFile == null) {
            self.setScope("compile");
        } else {
            self.setScope(rootPomFile.dependencies.get(self.getKey()).getScope());
        }
        pomFile.self = self;
    }

    private void readManagement(Element root, PomFile pomFile) {
        Element managementTag = root.element("dependencyManagement");
        if (managementTag != null) {
            List<Artifact> managements = getDependenciesFromElement(managementTag, pomFile);
            for (Artifact management : managements) {
                if ("import".equals(management.getScope())) {
                    pomFile.imports.putIfAbsent(management.getKey(), management);
                } else {
                    pomFile.management.putIfAbsent(management.getKey(), management);
                }
            }
        }
    }


    private void readDependencies(Element root, PomFile pomFile) {
        List<Artifact> dependencies = getDependenciesFromElement(root, pomFile);
        for (Artifact dependency : dependencies) {
            pomFile.dependencies.putIfAbsent(dependency.getKey(), dependency);
        }
    }

    private List<Artifact> getDependenciesFromElement(Element node, PomFile pomFile) {
        Element dependenciesTag = node.element("dependencies");
        List<Artifact> artifacts = new ArrayList<>();
        if (dependenciesTag != null) {
            List<Element> dependencyTags = dependenciesTag.elements("dependency");
            for (Element dependencyTag : dependencyTags) {
                Artifact artifact = getPomArtifact(dependencyTag, pomFile);
                artifacts.add(artifact);
            }
        }
        return artifacts;
    }


    private Artifact getPomArtifact(Element node, PomFile pomFile) {
        //解析groupId和artifactId
        Element groupIdTag = node.element("groupId");
        String groupId = pomFile.getProperty(groupIdTag.getTextTrim());
        Element artifactIdTag = node.element("artifactId");
        String artifactId = pomFile.getProperty(artifactIdTag.getTextTrim());
        Element versionTag = node.element("version");
        String version = null;
        if (versionTag != null) {
            version = pomFile.getProperty(versionTag.getTextTrim());
        }
        Element scopeTag = node.element("scope");
        String scope = null;
        if (scopeTag != null) {
            scope = pomFile.getProperty(scopeTag.getTextTrim());
        }
        Element optionalTag = node.element("optional");
        boolean optional = false;
        if (optionalTag != null) {
            optional = Boolean.parseBoolean(pomFile.getProperty(optionalTag.getTextTrim()));
        }
        //TODO-优化
        Set<String> exclusions = parseExclusions(node.element("exclusions"));
        Artifact artifact = new Artifact();
        artifact.setGroupId(groupId);
        artifact.setArtifactId(artifactId);
        artifact.setVersion(version);
        artifact.setScope(scope);
        artifact.setExcludes(exclusions);
        artifact.setOptional(optional);
        return artifact;
    }

    private Set<String> parseExclusions(Element exclusionsTag) {
        Set<String> exclusions = new HashSet<>();
        if (exclusionsTag == null) return exclusions;
        List<Element> exclusionTags = exclusionsTag.elements("exclusion");
        for (Element exclusionTag : exclusionTags) {
            Element groupIdTag = exclusionTag.element("groupId");
            if (groupIdTag == null) continue;
            Element artifactIdTag = exclusionTag.element("artifactId");
            if (artifactIdTag == null) continue;
            exclusions.add(groupIdTag.getTextTrim() + ":" + artifactIdTag.getTextTrim());
        }
        return exclusions;
    }

    private boolean checkPomPath(Path pomPath) {
        return pomPath != null && (pomPath.endsWith("pom.xml") || pomPath.toString().endsWith(".pom"));
    }

    private Path getParentPomPath(PomFile pomFile) {
        Path pomPath = pomFile.path;
        String relativePath = pomFile.properties.get("relativePath");
        Path parentPomPath;
        if (relativePath == null) {
            //如果没有relativePath标签,则默认为"../pom.xml"
            parentPomPath = getAbsolutePath(pomPath, "../pom.xml");
        } else if (relativePath.equals("DEFAULT")) {//有标签没有内容,从仓库中查找
            parentPomPath = null;
        } else {
            parentPomPath = getAbsolutePath(pomPath, relativePath);
        }
        if (parentPomPath == null) {
            Artifact parentArtifact = pomFile.parent;
            if (parentArtifact == null) return null;
            parentPomPath = getArtifactPath(parentArtifact);
        }
        return parentPomPath;
    }

    private Path getAbsolutePath(Path path, String relative) {
        if (path == null || relative == null) return null;
        Path absolutePath = path.getParent();
        while (relative.contains("../")) {
            relative = relative.replaceFirst("../", "");
            absolutePath = absolutePath.getParent();
        }
        absolutePath = absolutePath.resolve(relative);
        return absolutePath.toFile().isFile() ? absolutePath : null;
    }

    private Path getArtifactPath(Artifact artifact) {

        String parentPom = Paths.get(localRepo).toString()
                + File.separator + artifact.getGroupId().replace(".", File.separator)
                + File.separator + artifact.getArtifactId()
                + File.separator + artifact.getVersion()
                + File.separator + artifact.getArtifactId() + "-" + artifact.getVersion() + ".pom";

        return Paths.get(parentPom);
    }

    private boolean downloadFilesIfNecessary(Path path) {
        if (!path.toFile().exists()) {
            // 如果文件不存在, 通过远程仓下载文件
            downloadFile(path);
            if (!path.toFile().exists()) {
                return false;// 下载失败
            }
        }
        return true;
    }

    public void downloadFile(Path path) {
        URL fileUrl;
        File localDir = path.getParent().toFile();
        localDir.mkdirs();
        String filePath = path.toString().replace("\\", "/");
        HttpURLConnection httpUrl = null;
        try {
            fileUrl = new URL(filePath.replace(localRepo + "/", remoteRepo));
            httpUrl = (HttpURLConnection) fileUrl.openConnection();
            try (BufferedInputStream bis = new BufferedInputStream(httpUrl.getInputStream());
                 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
                httpUrl.connect();
                int len = 2048;
                byte[] b = new byte[len];
                while ((len = bis.read(b)) != -1) {
                    bos.write(b, 0, len);
                }
                bos.flush();
                bis.close();
                httpUrl.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean hasHigherPriority(String late,String before){
        return getOrder(late)<getOrder(before);
    }

    private Integer getOrder(String scope){
        return orderMap.get(scope);
    }
    class PomFile {
        private Path path;
        private Artifact self;
        private Artifact parent;
        private PomFile root;
        private Map<String, Artifact> imports = new LinkedHashMap<>();
        private Map<String, Artifact> management = new LinkedHashMap<>();
        private Map<String, Artifact> dependencies = new LinkedHashMap<>();
        private Map<String, Set<String>> exclusions = new LinkedHashMap<>();
        private Map<String, String> properties = new HashMap<>();
        private Set<String> tests = new HashSet<>();

        private PomFile(Path path) {
            this.path = path;
        }

        private String getProperty(String property) {

            while (!checkPropertyLegality(property)) {
                //查看是否是由"${}"包裹的结果,如果是重新查找
                String result = this.properties.get(uniform(property));
                if (result == null) break;
                property = result;
            }
            return property;
        }

        private String uniform(String property) {
            if (property == null) return null;
            if (property.startsWith("${") && property.endsWith("}")) {
                return property.substring(2, property.length() - 1);
            }
            return property;
        }

        private boolean checkPropertyLegality(String property) {
            if (property == null) return true;
            return !property.startsWith("${") || !property.endsWith("}");
        }

        @Override
        public String toString() {
            return this.self.toString()+":"+this.self.getScope();
        }
    }

    //TODO--修改为private
    public class Pom {
        Path path;
        Artifact artifact;
        Map<String, Pom> dependencies = new LinkedHashMap<>();

        Pom(Path path) {
            this.path = path;
        }
    }

}