package com.huawei.fossbot.dependency.parser.java;

import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.bean.BuildFile;
import com.huawei.fossbot.dependency.bean.Profile;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.exception.DownloadException;
import com.huawei.fossbot.dependency.exception.InvalidProperty;
import com.huawei.fossbot.dependency.exception.UndefinedPropertyException;
import com.huawei.fossbot.dependency.exception.UnknownVersionException;
import com.huawei.fossbot.dependency.exception.WrongPathException;
import com.huawei.fossbot.dependency.parser.DependencyParser;
import com.huawei.fossbot.dependency.util.xml.PomHandler;
import com.huawei.fossbot.dependency.util.xml.ProjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * @author t30002128
 * @since 2020/05/20
 */
public class MavenParser implements DependencyParser {
    private static Map<String, Integer> ORDER = new HashMap<>();
    private static final Map<String, String> ENVS = new HashMap<>();

    static {
        ORDER.put("compile", 1);
        ORDER.put("runtime", 2);
        ORDER.put("system", 3);
        ORDER.put("test", 4);
        ORDER.put("provided", 5);
        ENVS.put("jdk", System.getProperty("java.version"));
        ENVS.put("java.home", System.getProperty("java.home"));
        ENVS.put("os.name", System.getProperty("os.name").toLowerCase(Locale.US));
        ENVS.put("os.arch", System.getProperty("os.arch").toLowerCase(Locale.US));
        ENVS.put("os.version", System.getProperty("os.version").toLowerCase(Locale.US));
        ENVS.put("user.home", System.getProperty("user.home"));
    }

    private String localRepo;
    private String remoteRepo;
    private XMLReader xmlReader;
    Map<String, PomFile> readMap;

    public MavenParser(String localRepo, String remoteRepo, XMLReader xmlReader) {
        this.localRepo = localRepo;
        this.remoteRepo = remoteRepo;
        this.xmlReader = xmlReader;
        readMap = new LinkedHashMap<>();
    }


    /**
     * pom文件解析原则:
     * 1.读取文件合并management原则
     * a)dependency->management->parent management->import management
     * 2.version:
     * a)最短路径原则
     * b)声明优先原则
     * 3.scope:
     * a)从management中查找,如果management中的scope!=null,则使用management的scope
     * 注意:management的scope不写不是默认为compile,而是null!!
     * b)从调用链上游(root)解析,如果root.scope<声名的scope,则scop=root.scope
     * c)else:root.scope = root声名的该dependency的scope
     * 3.optional:解析rootPom的optional=true,和所有的optional=false
     * 4.test
     * a)仅有rootPom下的test会被解析
     * b)传递依赖compile的scope会被解析为test(如果尚未被解析的话)
     * 5.system:不向下解析
     * 6.exclusions:
     * a) dependency声名的exclusion
     * b) 如果不是root pom,且dependency没有声名,则查看当前management中的exclusion
     * c) 查看root pom文件中的exclusion,并与当前的exclusion合并
     */
    @Override
    public BuildFile parse(String profile) throws Exception {
        return parseRootProject(profile);
    }

    @Override
    public BuildFile directParse(String profile) throws Exception {
        if (StringUtils.isBlank(profile)) {
            throw new WrongPathException("Pom file path can not be null");
        }
        Path pomPath = Paths.get(profile);
        // pomFile是一个树形结构

        PomFile basePomFile = resolvePomFile(pomPath, null, null);
        BuildFile pom = new BuildFile(pomPath, basePomFile.self);
        for (String key : basePomFile.dependencies.keySet()) {
            Artifact child = basePomFile.dependencies.get(key);
            pom.addChild(new BuildFile(getArtifactPath(child), child));
        }
        return pom;
    }

    @Override
    public BuildFile parseRootProject(String profile) throws Exception {
        if (StringUtils.isBlank(profile)) {
            throw new WrongPathException("Pom file path can not be null");
        }
        Path pomPath = Paths.get(profile);
        // pomFile是一个树形结构
        return parsePom(pomPath);
    }

    private BuildFile parsePom(Path pomPath) throws Exception {

        Queue<String> dependencyQueue = generateParseTaskQueue(pomPath);

        BuildFile pom = generateDependencyTree(dependencyQueue);

        return pom;
    }

    private Queue<String> generateParseTaskQueue(Path pomPath) throws Exception {
        PomFile basePomFile = resolvePomFile(pomPath, null, null);
        Queue<PomFile> resolveQueue = new LinkedList<>();
        // 辅助map
        Map<String, String> dependencyToParse = new LinkedHashMap<>();
        LinkedList<String> parseTaskQueue = new LinkedList<>();
        resolveQueue.offer(basePomFile);
        while (!resolveQueue.isEmpty()) {
            PomFile pomFile = resolveQueue.poll();
            parseTaskQueue.offer(getReadKey(pomFile.self));

            for (Artifact dependency : pomFile.dependencies.values()) {
                String key = dependency.getKey();

                if (dependencyToParse.containsKey(key)) {
                    if (readMap.containsKey(getReadKey(dependency))) {
                        if (hasHigherPriority(dependency.getScope(),
                                readMap.get(getReadKey(dependency)).self.getScope())) {
                            updateReadScope(dependency, dependencyToParse);
                        }
                    }
                    continue;
                }

                if ("system".equals(dependency.getScope())) {
                    PomFile dependencyPomFile = new PomFile(Paths.get(dependency.getSource()));
                    dependencyPomFile.self = dependency;
                    dependencyPomFile.root = pomFile;
                    resolveQueue.offer(dependencyPomFile);
                    dependencyToParse.put(key, getReadKey(dependency));
                    readMap.put(getReadKey(dependency), dependencyPomFile);
                    continue;
                }
                PomFile dependencyPomFile = resolvePomFile(getArtifactPath(dependency),
                        pomFile, basePomFile);
                resolveQueue.offer(dependencyPomFile);
                dependencyToParse.put(key, getReadKey(dependency));
            }
        }

        return parseTaskQueue;
    }

    private String getReadKey(Artifact artifact) {
        return artifact.getKey() + ":" + artifact.getVersion();
    }

    private void updateReadScope(Artifact aNew, Map<String, String> dependencyToParse) {
        String scope = aNew.getScope();
        String key = aNew.getKey();
        if (dependencyToParse.containsKey(key)) {
            PomFile read = readMap.get(dependencyToParse.get(key));
            read.self.setScope(scope);
            for (Artifact dependency : read.dependencies.values()) {
                dependency.setScope(scope);
                updateReadScope(dependency, dependencyToParse);
            }
        }
    }

    private BuildFile generateDependencyTree(Queue<String> dependencyQueue) {
        String rootKey = dependencyQueue.poll();
        PomFile rootPomFile = readMap.get(rootKey);
        BuildFile rootPom = new BuildFile(rootPomFile.path, rootPomFile.self);
        HashMap<String, BuildFile> parsedMap = new HashMap<>();
        parsedMap.put(rootPom.getArtifact().getKey(), rootPom);
        while (!dependencyQueue.isEmpty()) {
            PomFile pomFile = readMap.get(dependencyQueue.poll());
            BuildFile pom = new BuildFile(pomFile.path, pomFile.self);
            BuildFile parentPom = parsedMap.get(pomFile.root.self.getKey());
            parentPom.addChild(pom);
            parsedMap.putIfAbsent(pom.getArtifact().getKey(), pom);
        }
        return rootPom;
    }

    private PomFile resolvePomFile(Path path, PomFile root, PomFile base) throws Exception {
        PomFile pomFile = doResolvePomFile(path, root);
        // 对当前pomFile进行后处理
        if (base == null) {
            base = pomFile;
        }
        adaptPomFile(pomFile, base);
        return pomFile;
    }

    private PomFile doResolvePomFile(Path path, PomFile root) throws Exception {
        checkPomPath(path);

        downloadFilesIfNecessary(path);

        String readKey = pathToArtifactKey(path);

        if (readMap.containsKey(readKey)) {
            return readMap.get(readKey);
        }

        PomFile pomFile = new PomFile(path);

        readMap.put(readKey, pomFile);

        readPomFile(pomFile, root);
        // 将当前pomFile与parent和imports进行合并继承
        mergePomFile(pomFile);

        return pomFile;
    }


    /**
     * 使用xmlReader通过sax形式读取pom.xml
     * 读取完成后进行简单解析,生成pomFile对象
     */
    public void readPomFile(PomFile pomFile, PomFile root) throws IOException, SAXException {
        xmlReader.setContentHandler(new ProjectHandler(xmlReader));
        xmlReader.parse(pomFile.path.toString());
        PomHandler pomHandler = (PomHandler) xmlReader.getContentHandler();
        pomFile.root = root;
        pomFile.properties = pomHandler.getProperties();
        // read parent
        if (pomFile.properties.containsKey("project.parent.groupId")) {
            Artifact parentArtifact = new Artifact();
            parentArtifact.setGroupId(pomFile.getProperty("project.parent.groupId"));
            parentArtifact.setArtifactId(pomFile.getProperty("project.parent.artifactId"));
            parentArtifact.setVersion(pomFile.getProperty("project.parent.version"));
            pomFile.properties.putIfAbsent("project.groupId", pomFile.getProperty("project.parent.groupId"));
            pomFile.properties.putIfAbsent("project.version", pomFile.getProperty("project.parent.version"));
            if (pomFile.properties.containsKey("parent.relativePath")) {
                if ("".equals(pomFile.properties.get("parent.relativePath"))) {
                    pomFile.properties.put("parent.relativePath", "DEFAULT");
                }
            } else {
                pomFile.properties.put("parent.relativePath", "../pom.xml");
            }
            pomFile.parent = parentArtifact;
        }
        // fill normal properties
        pomFile.properties.put("basedir", pomFile.path.getParent().toString().replace("\\", "/"));
        pomFile.properties.put("project.basedir", pomFile.path.getParent().toString().replace("\\", "/"));
        pomFile.properties.put("version", pomFile.properties.get("project.version"));
        pomFile.properties.put("settings.localRepository", this.localRepo);
        pomFile.properties.put("user.home", ENVS.get("user.home"));
        pomFile.properties.put("java.home", ENVS.get("java.home"));
        // fill management
        Map<String, Artifact> managements = pomHandler.getManagements();
        for (String key : managements.keySet()) {
            Artifact management = managements.get(key);
            if ("import".equals(managements.get(key).getScope())) {
                pomFile.imports.putIfAbsent(key, management);
            } else {
                pomFile.management.putIfAbsent(key, management);
            }
        }
        // fill dependencies
        pomFile.dependencies = pomHandler.getDependencies();
        // fill profiles
        pomFile.profiles = pomHandler.getProfiles();
        // fill self artifact
        Artifact self = new Artifact();
        self.setGroupId(pomFile.properties.get("project.groupId"));
        self.setArtifactId(pomFile.properties.get("project.artifactId"));
        self.setVersion(pomFile.properties.get("project.version"));
        pomFile.self = self;
    }

    private void mergePomFile(PomFile pomFile) throws Exception {
        mergeProperties(pomFile);

        detectProfiles(pomFile);

        mergeProfileProperties(pomFile);

        mergeManagement(pomFile);

        mergeDependencies(pomFile);
    }

    private void mergeProfileProperties(PomFile pomFile) {
        for (String key : pomFile.profiles.keySet()) {
            Map<String, String> profileProperties = pomFile.profiles.get(key).getProperties();
            mergeInformation(profileProperties, pomFile.properties);
        }
    }

    private void detectProfiles(PomFile pomFile) {
        Map<String, Profile> profiles = pomFile.profiles;
        List<Profile> defaultProfiles = profiles.values().stream()
                .filter(Profile::isDefault).collect(Collectors.toList());
        Profile defaultProfile = null;
        if (defaultProfiles.size() > 0) {
            defaultProfile = defaultProfiles.get(0);
        }
        profiles.entrySet().removeIf(entry -> !useProfile(entry.getValue()));
        if (profiles.size() == 0 && defaultProfile != null) {
            profiles.put(defaultProfile.getId(), defaultProfile);
        }
    }

    private boolean useProfile(Profile profile) {
        Map<String, String> activation = profile.getActivation();
        if (activation.isEmpty()) {
            return false;
        }
        boolean useProfile = true;
        for (String key : activation.keySet()) {
            if (!useProfile) {
                break;
            }
            String activeCondition = activation.get(key).toLowerCase(Locale.US);
            switch (key) {
                case "jdk":
                    if (ENVS.get(key) == null) {
                        useProfile = false;
                    } else {
                        useProfile = ENVS.get(key).startsWith(activeCondition);
                    }
                    break;
                case "os.name":
                case "os.arch":
                case "os.version":
                    useProfile = ENVS.get(key).equals(activeCondition);
                    break;
                case "os.family":
                    useProfile = ENVS.get("os.name").contains(activeCondition);
                    break;
                case "file.exists":
                    useProfile = Paths.get(activeCondition).toFile().exists();
                    break;
                case "file.missing":
                    useProfile = !Paths.get(activeCondition).toFile().exists();
                    break;
                case "property.name":
                    if (activeCondition.startsWith("!")) {
                        String property = System.getProperty(activeCondition.substring(1));
                        useProfile = property == null;
                    } else {
                        String property = System.getProperty(key);
                        useProfile = property != null;
                    }
                    break;
                case "property.value":
                    useProfile = checkPropertyValue(activation, activeCondition, key);
                    break;
            }
        }
        return useProfile;
    }

    private boolean checkPropertyValue(Map<String, String> activation, String activeCondition, String key) {
        String name = activation.get("property.name");
        if (name == null) {
            return false;
        }
        name = name.toLowerCase(Locale.US);
        boolean useName;
        boolean useValue;
        if (name.startsWith("!")) {
            String nameProperty = System.getProperty(name.substring(1));
            useName = nameProperty == null;
        } else {
            String nameProperty = System.getProperty(name);
            useName = nameProperty != null;
        }
        if (activeCondition.startsWith("!")) {
            String property = System.getProperty(activeCondition.substring(1));
            useValue = property == null;
        } else {
            String property = System.getProperty(key);
            useValue = property != null;
        }
        return useName && useValue;
    }

    private void mergeProperties(PomFile pomFile) throws Exception {
        if (pomFile.parent != null) {
            resolveArtifact(pomFile.parent, pomFile);
            PomFile parentPomFile = doResolvePomFile(getParentPomPath(pomFile), null);
            mergeInformation(parentPomFile.properties, pomFile.properties);
        }
        for (String key : pomFile.imports.keySet()) {
            Artifact importArtifact = pomFile.imports.get(key);
            resolveArtifact(importArtifact, pomFile);
            PomFile importPomFile = doResolvePomFile(getArtifactPath(importArtifact), null);
            mergeInformation(importPomFile.properties, pomFile.properties);
        }
    }

    private void mergeManagement(PomFile pomFile) throws Exception {
        for (String key : pomFile.profiles.keySet()) {
            Map<String, Artifact> profileManagement = pomFile.profiles.get(key).getManagement();
            updateInformation(profileManagement, pomFile.management);
        }
        if (pomFile.parent != null) {
            PomFile parentPomFile = doResolvePomFile(getParentPomPath(pomFile), null);
            mergeArtifacts(parentPomFile.management, pomFile.management);
        }
        for (String key : pomFile.imports.keySet()) {
            PomFile importPomFile = doResolvePomFile(getArtifactPath(pomFile.imports.get(key)),
                    null);
            mergeArtifacts(importPomFile.management, pomFile.management);
        }
        fixManagement(pomFile);
    }

    private void fixManagement(PomFile pomFile) throws Exception {
        Map<String, Artifact> managements = new LinkedHashMap<>();
        for (String key : pomFile.management.keySet()) {
            Artifact management = pomFile.management.get(key);
            resolveArtifact(management, pomFile);
            managements.put(management.getKey(), management);
        }
        pomFile.management = managements;
    }

    private void mergeDependencies(PomFile pomFile) throws Exception {
        for (String key : pomFile.profiles.keySet()) {
            Map<String, Artifact> profileDependencies = pomFile.profiles.get(key).getDependencies();
            mergeArtifacts(profileDependencies, pomFile.dependencies);
        }
        if (pomFile.parent != null) {
            PomFile parentPomFile = doResolvePomFile(getParentPomPath(pomFile), null);
            mergeArtifacts(parentPomFile.dependencies, pomFile.dependencies);
        }
        for (String key : pomFile.imports.keySet()) {
            PomFile importPomFile = doResolvePomFile(getArtifactPath(pomFile.imports.get(key)),
                    null);
            mergeArtifacts(importPomFile.dependencies, pomFile.dependencies);
        }
        fixDependencies(pomFile);
    }

    /**
     * dependencies属性解析
     */
    private void fixDependencies(PomFile pomFile) throws DependencyParserException {
        Map<String, Artifact> dependencies = new LinkedHashMap<>();
        for (String key : pomFile.dependencies.keySet()) {
            Artifact dependency = pomFile.dependencies.get(key);
            resolveArtifact(dependency, pomFile);
            dependencies.put(dependency.getKey(), dependency);
        }
        pomFile.dependencies = dependencies;
    }


    private void adaptPomFile(PomFile pomFile, PomFile base) throws UnknownVersionException, UndefinedPropertyException {
        adaptSelfScope(pomFile, base);

        adaptDependency(pomFile, base);

        adaptExclusions(pomFile, base);
    }

    private void adaptSelfScope(PomFile pomFile, PomFile base) {
        if (pomFile == base) {
            pomFile.self.setScope("compile");
        } else {
            pomFile.self.setScope(pomFile.root.dependencies.get(pomFile.self.getKey()).getScope());
        }
    }

    private void adaptDependency(PomFile pomFile, PomFile base) throws UnknownVersionException, UndefinedPropertyException {
        for (String key : pomFile.dependencies.keySet()) {
            Artifact dependency = pomFile.dependencies.get(key);
            adaptVersions(dependency, pomFile, base);
            adaptScope(dependency, pomFile, base);
            adaptSystemPath(dependency, pomFile, base);
            adaptExclusions(dependency, pomFile, base);
        }
        adaptOptional(pomFile, base);
        dropDependencies(pomFile);
        checkDependencies(pomFile);
    }

    private void checkDependencies(PomFile pomFile) throws UndefinedPropertyException {
        for (Artifact dependency : pomFile.dependencies.values()) {
            checkArtifact(dependency, pomFile);
        }
    }

    private void checkArtifact(Artifact dependency, PomFile pomFile) throws UndefinedPropertyException {
        checkCompleteness(dependency.getGroupId(), pomFile);
        checkCompleteness(dependency.getArtifactId(), pomFile);
        checkCompleteness(dependency.getVersion(), pomFile);
    }

    private void checkCompleteness(String property, PomFile pomFile) throws UndefinedPropertyException {
        if (property.startsWith("${")) {
            throw new UndefinedPropertyException("Property " + property
                    + " is undefined in " + pomFile.path.toString());
        }
    }

    private void dropDependencies(PomFile pomFile) {
        pomFile.dependencies.entrySet()
                .removeIf(entry ->
                        entry.getValue().getScope().equals("drop")
                );
    }

    private void adaptOptional(PomFile pomFile, PomFile base) {
        if (pomFile != base) {
            pomFile.dependencies.entrySet().removeIf(entry -> entry.getValue().isOptional());
        }
    }

    private void adaptSystemPath(Artifact dependency, PomFile pomFile, PomFile base) {
        if ("system".equals(dependency.getScope())) {
            String key = dependency.getKey();
            String systemPath = null;
            if (pomFile != base) {
                if (base.management.containsKey(key)) {
                    systemPath = base.management.get(key).getSource();
                }
            }
            if (systemPath == null) {
                systemPath = dependency.getSource();
            }
            if (systemPath == null) {
                if (pomFile.management.containsKey(key)) {
                    systemPath = pomFile.management.get(key).getVersion();
                }
            }
            dependency.setSource(systemPath);
        }
    }

    /**
     * version确定规则(非root)
     * a)从base(root)的management中查找,若没有b)
     * b)dependency声名的version,若没有c)
     * c)management声名的version
     */
    private void adaptVersions(Artifact dependency, PomFile pomFile, PomFile base) throws UnknownVersionException {
        String key = dependency.getKey();
        String version = null;
        if (pomFile != base) {
            if (base.management.containsKey(key)) {
                version = base.management.get(key).getVersion();
            }
        }
        if (version == null) {
            version = dependency.getVersion();
        }
        if (version == null) {
            if (pomFile.management.containsKey(key)) {
                version = pomFile.management.get(key).getVersion();
            }
        }
        if (version == null) {
            throw new UnknownVersionException("dependency " + dependency.getKey()
                    + " version is unknown.Pom file:" + pomFile.path.toString());
        }
        dependency.setVersion(version);
    }

    private void adaptScope(Artifact dependency, PomFile pomFile, PomFile base) {
        String key = dependency.getKey();
        String dependencyScope = null;

        String rootScope = pomFile.self.getScope();
        dependencyScope = dependency.getScope();

        // 获取self定义的scope
        if (dependencyScope == null) {
            if (pomFile.management.containsKey(key)) {
                dependencyScope = pomFile.management.get(key).getScope();
            }
        }
        if (dependencyScope == null) {
            dependencyScope = "compile";
        }

        if (pomFile != base) {
            // 如果是test或者provided并且当前的pom不是rootPomFile
            if (dependencyScope.equals("test") || dependencyScope.equals("provided")) {
                dependency.setScope("drop");
                return;
            }
            if (base.management.containsKey(key)) {
                String scopeInRoot = base.management.get(key).getScope();
                if (scopeInRoot != null) {
                    dependency.setScope(scopeInRoot);
                    return;
                }
            }
        }
        if (hasHigherPriority(dependencyScope, rootScope)) {
            dependency.setScope(rootScope);
        } else {
            dependency.setScope(dependencyScope);
        }
    }

    /**
     * 从management中获取dependency的exclusions
     * 1.如果不是root pom,则优先从dependency中获取exclude信息,如果没有再从management中找
     * 2.如果是root pom则直接使用dependency中的exclude信息
     * 3.和root pom的management融合
     */
    private void adaptExclusions(Artifact dependency, PomFile pomFile, PomFile base) {
        String key = dependency.getKey();

        // 从management和dependency中选一
        if (pomFile != base) {
            if (dependency.getExcludes().isEmpty()) {
                if (pomFile.management.containsKey(key)) {
                    Set<String> excludesInManagement = pomFile.management.get(key).getExcludes();
                    dependency.getExcludes().addAll(excludesInManagement);
                }
            }
        }
        // 从rootPomFile的management中继承
        if (base.management.containsKey(key)) {
            Set<String> excludesInRoot = base.management.get(key).getExcludes();
            dependency.getExcludes().addAll(excludesInRoot);
        }
    }


    /**
     * 合并pomFile的exclusions属性
     * 当前pom的exclusions由3部分组成
     * 1.当前pom声名的exclusions
     * 2.其引用者的exclusions
     * 3.rootPom management声名的exclusions
     */
    private void adaptExclusions(PomFile pomFile, PomFile base) {
        if (pomFile != base) {
            Artifact dependencyInRoot;
            dependencyInRoot = pomFile.root.dependencies.get(pomFile.self.getKey());
            pomFile.exclusions.addAll(pomFile.root.exclusions);
            pomFile.exclusions.addAll(dependencyInRoot.getExcludes());
            if (base.management.containsKey(pomFile.self.getKey())) {
                dependencyInRoot = base.management.get(pomFile.self.getKey());
                pomFile.exclusions.addAll(dependencyInRoot.getExcludes());
            }
            for (String exclusion : pomFile.exclusions) {
                pomFile.dependencies.remove(exclusion);
            }
        }
    }


    private void resolveArtifact(Artifact artifact, PomFile pomFile) throws DependencyParserException {
        // 解析groupId和artifactId
        artifact.setGroupId(getProperty(artifact.getGroupId(), artifact.getKey(), pomFile));
        artifact.setArtifactId(getProperty(artifact.getArtifactId(), artifact.getKey(), pomFile));
        artifact.setVersion(getProperty(artifact.getVersion(), artifact.getKey(), pomFile));
        artifact.setScope(getProperty(artifact.getScope(), artifact.getKey(), pomFile));
        artifact.setSource(getProperty(artifact.getSource(), artifact.getKey(), pomFile));
        Set<String> exclusions = parseExclusions(artifact, pomFile);
        artifact.setExcludes(exclusions);
    }

    private String getProperty(String property, String key, PomFile pomFile) throws DependencyParserException {
        if (property == null) {
            return null;
        }
        if (!isValidProperty(property)) {
            throw new InvalidProperty("Can not resolve property" + property
                    + " in dependency " + key + ".Pom file:" + pomFile.path.toString());
        }

        StringBuilder stringBuilder = new StringBuilder();
        Stack<Character> cStack = new Stack<>();
        boolean toStack = false;
        for (char c : property.toCharArray()) {
            if (c == '$') {
                toStack = true;
            }
            if (!toStack) {
                stringBuilder.append(c);
            } else {
                cStack.push(c);
                if (c == '}') {
                    toStack = false;
                    StringBuilder tempt = new StringBuilder();
                    while (!cStack.isEmpty()) {
                        tempt.append(cStack.pop());
                    }
                    String propertyInPomFile = pomFile.getProperty(tempt.reverse().toString());
                    if (propertyInPomFile != null) {
                        stringBuilder.append(propertyInPomFile);
                    }
                }
            }
        }
        String result = stringBuilder.toString();
        if (StringUtils.isBlank(result)) {
            return property;
        }
        return result;
    }

    private boolean isValidProperty(String property) {
        if (property == null) {
            return true;
        }
        Stack<Character> characters = new Stack<>();
        char[] chars = property.toCharArray();
        for (char aChar : chars) {
            if (aChar == '$') {
                if (!characters.isEmpty()) {
                    return false;
                }
                characters.push(aChar);
            }
            if (aChar == '{') {
                if (characters.isEmpty()) {
                    return false;
                }
                if (characters.peek() != '$') {
                    return false;
                }
                characters.push(aChar);
            }
            if (aChar == '}') {
                if (characters.isEmpty()) {
                    return false;
                }
                characters.pop();
                characters.pop();
            }
        }
        return characters.isEmpty();
    }

    private Set<String> parseExclusions(Artifact artifact, PomFile pomFile) {
        Set<String> exclusions = new HashSet<>();
        Set<String> originExcludes = artifact.getExcludes();
        for (String exclude : originExcludes) {
            String[] tags = exclude.split(":");
            String groupId = tags[0];
            String artifactId = tags[1];
            groupId = pomFile.getProperty(groupId) == null ? groupId : pomFile.getProperty(groupId);
            artifactId = pomFile.getProperty(artifactId) == null ? artifactId : pomFile.getProperty(artifactId);
            exclusions.add(groupId + ":" + artifactId);
        }
        return exclusions;
    }

    private Path getParentPomPath(PomFile pomFile) {
        Path path = pomFile.path;
        String relativePath = pomFile.getProperty("parent.relativePath");
        Path parentPomPath = null;
        // 有标签没有内容,从仓库中查找
        if (!relativePath.equals("DEFAULT")) {
            parentPomPath = getAbsolutePath(path, relativePath);
        }

        if (parentPomPath == null) {
            Artifact parentArtifact = pomFile.parent;
            if (parentArtifact != null) {
                parentPomPath = getArtifactPath(parentArtifact);
            }
        }
        return parentPomPath;
    }


    private void mergeInformation(Map<String, String> from, Map<String, String> to) {
        for (String key : from.keySet()) {
            // 合并properties->if absent->pomFile的properties在之前已经解析完了
            to.putIfAbsent(key, from.get(key));
        }
    }

    private void mergeArtifacts(Map<String, Artifact> from, Map<String, Artifact> to) {
        for (String key : from.keySet()) {
            to.putIfAbsent(key, from.get(key).copy());
        }
    }

    private <T> void updateInformation(Map<String, T> from, Map<String, T> to) {
        for (String key : from.keySet()) {
            // 合并properties->if absent->pomFile的properties在之前已经解析完了
            to.put(key, from.get(key));
        }
    }

    private String pathToArtifactKey(Path path) throws IOException, SAXException {
        if (path.endsWith("pom.xml")) {
            PomFile pomFile = new PomFile(path);
            readPomFile(pomFile, null);
            return getReadKey(pomFile.self);
        }
        if (path.toString().endsWith(".pom")) {
            String version = path.getParent().toFile().getName();
            path = path.getParent().getParent();
            String artifactId = path.toFile().getName();
            path = path.getParent();
            String groupId = path.toString().replace("\\", "/")
                    .split(localRepo + "/")[1].replace("/", ".");
            return groupId + ":" + artifactId + ":" + version;
        }
        return null;
    }

    private void checkPomPath(Path pomPath) throws WrongPathException {
        if (pomPath == null) {
            throw new WrongPathException("Pom file path can not be null");
        }
        if (!(pomPath.endsWith("pom.xml") || pomPath.toString().endsWith(".pom"))) {
            throw new WrongPathException("Pom file must end with pom.xml or .pom.Pom file" + pomPath.toString());
        }
    }

    private Path getAbsolutePath(Path path, String relative) {
        if (path == null || relative == null) {
            return null;
        }
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

    private void downloadFilesIfNecessary(Path path) throws DownloadException {
        if (!path.toFile().exists()) {
            // 如果文件不存在, 通过远程仓下载文件
            downloadFile(path);
        }
    }

    private void downloadFile(Path path) throws DownloadException {
        URL fileUrl;
        File localDir = path.getParent().toFile();
        localDir.mkdirs();
        String filePath = path.toString().replace("\\", "/");
        filePath = filePath.replace(localRepo + "/", remoteRepo);
        HttpURLConnection httpUrl = null;
        try {
            fileUrl = new URL(filePath);
            httpUrl = (HttpURLConnection) fileUrl.openConnection();
            try (BufferedInputStream bis = new BufferedInputStream(httpUrl.getInputStream());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
                httpUrl.connect();
                int length = 2048;
                byte[] bytes = new byte[length];
                while ((length = bis.read(bytes)) != -1) {
                    bos.write(bytes, 0, length);
                }
                bos.flush();
                bis.close();
                httpUrl.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new DownloadException("Download failure:" + filePath);
        }
    }

    private boolean hasHigherPriority(String late, String before) {
        return getOrder(late) < getOrder(before);
    }

    private Integer getOrder(String scope) {
        return ORDER.get(scope);
    }

    public BuildFile parseCurrentPomFileOnly(String profile) throws Exception {
        PomFile pomFile = new PomFile(Paths.get(profile));
        readPomFile(pomFile, null);
        pomFile.parent = null;
        for (Artifact management : pomFile.management.values()) {
            resolveArtifact(management, pomFile);
        }
        for (Artifact dependency : pomFile.dependencies.values()) {
            String key = dependency.getKey();
            resolveArtifact(dependency, pomFile);
            if (pomFile.management.containsKey(key)) {
                Artifact management = pomFile.management.get(key);
                if (dependency.getVersion() == null) {
                    dependency.setVersion(management.getVersion());
                }
                if (dependency.getScope() == null) {
                    String scope = management.getScope() == null ? "compile" : management.getScope();
                    dependency.setScope(scope);
                }
                if (dependency.getScope().equals("system") && dependency.getSource() == null) {
                    dependency.setSource(management.getSource());
                }
            }
        }
        BuildFile buildFile = new BuildFile(Paths.get(profile), pomFile.self);
        for (String key : pomFile.dependencies.keySet()) {
            buildFile.addChild((new BuildFile(null, pomFile.dependencies.get(key))));
        }
        return buildFile;
    }

    private static class PomFile {
        private Path path;
        private Artifact self;
        private Artifact parent;
        private PomFile root;
        private Map<String, Artifact> imports = new LinkedHashMap<>();
        private Map<String, Artifact> management = new LinkedHashMap<>();
        private Map<String, Artifact> dependencies = new LinkedHashMap<>();
        private Map<String, Profile> profiles = new LinkedHashMap<>();
        private Map<String, String> properties = new HashMap<>();
        private Set<String> exclusions = new HashSet<>();

        public PomFile(Path path) {
            this.path = path;
        }

        private String getProperty(String property) {
            property = this.properties.get(uniform(property));
            while (isEL(property)) {
                property = this.properties.get(uniform(property));
            }
            return property;
        }

        private String uniform(String property) {
            if (property == null) {
                return null;
            }
            if (property.startsWith("${") && property.endsWith("}")) {
                return property.substring(2, property.length() - 1);
            }
            return property;
        }

        private boolean isEL(String property) {
            if (property == null) {
                return false;
            }
            return property.startsWith("${") && property.endsWith("}");
        }

        @Override
        public String toString() {
            return this.self.toString() + ":" + this.self.getScope();
        }
    }

}