package com.huawei.fossbot.dependency.analyzer.java;

import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.bean.BuildFile;
import com.huawei.fossbot.dependency.bean.Pair;
import com.huawei.fossbot.dependency.command.Command;
import com.huawei.fossbot.dependency.dsl.gradle.ProjectDescriptor;
import com.huawei.fossbot.dependency.dsl.gradle.Settings;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.md5.DependencyMd5Resolver;
import com.huawei.fossbot.dependency.md5.GradleMd5Resolver;
import com.huawei.fossbot.dependency.md5.MavenMd5Resolver;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import com.huawei.fossbot.dependency.util.ProcessUtil;
import com.huawei.fossbot.dependency.log.Logger;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * GradleAnalyzer
 *
 * @author d00380361 dongkeqin@huawei.com
 * @update t30002128 tianhaolin@huawei.com 2020/05/11
 * @since 2019/10/15 9:16
 */
public class GradleAnalyzer implements DependencyAnalyzer {
    private static Logger log = Logger.getInstance();
    private Command command;
    private DependencyMd5Resolver mavenResolver;
    private DependencyMd5Resolver gradleResolver;
    private String profile;
    private Settings settings;
    private ProjectDescriptor project;
    private String localRepo;

    public GradleAnalyzer(String profile, Settings settings, ProjectDescriptor project, Command command) {
        this.profile = profile;
        this.settings = settings;
        this.project = project;
        this.localRepo = getLocalRepo();
        mavenResolver = new MavenMd5Resolver();
        gradleResolver = new GradleMd5Resolver();
        this.command = command;
    }

    private String getLocalRepo() {
        return project.getRepo().getLocalRepo();
    }

    @Override
    public BuildFile analyze() throws Exception {
        log.info("start analyze,use build file {}", profile);
        downloadDependencies(profile);
        BuildFile rootBuildFile = resolveDependencyFile(settings.getRootProject());
        resolveModules(rootBuildFile, settings.getRootProject());
        return rootBuildFile;
    }

    private void resolveModules(BuildFile rootBuildFile, ProjectDescriptor root) throws Exception {
        //ProjectDescriptor's child is a module of project
        for (ProjectDescriptor child : root.getChildren()) {
            BuildFile childBuildFile = resolveDependencyFile(child);
            rootBuildFile.addModule(childBuildFile);
            resolveModules(childBuildFile, child);
        }
    }

    @Override
    public BuildFile analyzeRootProject() throws Exception {
        downloadDependencies(profile);
        return resolveDependencyFile(settings.getRootProject());
    }

    @Override
    public BuildFile directAnalyze() throws Exception {
        downloadDependencies(profile);
        BuildFile rootBuildFile = analyzeRootProject();
        for (BuildFile child : rootBuildFile.getChildren()) {
            child.getChildren().clear();
        }
        return rootBuildFile;
    }

    private BuildFile resolveDependencyFile(ProjectDescriptor projectDescriptor) throws Exception {
        log.info("start to resolve build file {}", projectDescriptor.getBuildFilePath());
        String buildFilePath = projectDescriptor.getBuildFilePath();
        boolean useMavenLocal = useMavenLocal(project);
        Node node = parseDependency(buildFilePath, projectDescriptor);
        Path path = Paths.get(buildFilePath);
        BuildFile buildFile = new BuildFile(path, node.artifact);
        for (Node childNode : node.getChildNodes()) {
            buildFile.addChild(resolveParserNode(childNode, useMavenLocal));
        }
        addFileTreeDependencies(buildFile, projectDescriptor);
        return buildFile;
    }

    private boolean useMavenLocal(ProjectDescriptor project) {
        String mavenLocal = project.getRepo().getMavenLocal();
        return mavenLocal != null;
    }

    private BuildFile resolveParserNode(Node node, boolean useMavenLocal) {
        node.artifact.setMd5(resolveMd5(node.artifact, useMavenLocal));
        BuildFile buildFile = new BuildFile(getLocalPathFromNode(node, useMavenLocal), node.artifact);
        for (Node childNode : node.getChildNodes()) {
            buildFile.addChild(resolveParserNode(childNode, useMavenLocal));
        }
        return buildFile;
    }

    private Path getLocalPathFromNode(Node node, boolean useMavenLocal) {
        if (useMavenLocal) {
            return mavenResolver.resolveDependencyPath(node.artifact, localRepo);
        } else {
            return gradleResolver.resolveDependencyPath(node.artifact, localRepo);
        }
    }

    private Node parseDependency(String buildFile, ProjectDescriptor projectDescriptor) throws Exception {
        Process pro = executeCmd(buildFile);
        Map<String, String> keyMap = getDependencyKeyMap();
        Map<String, List<String>> scopeMap = getContext(buildFile, pro, keyMap);
        Map<String, Node> nodeMap = new LinkedHashMap<>();
        Node rootNode = getNodeByArtifactId(projectDescriptor.getName());
        for (String key : scopeMap.keySet()) {
            if (CollectionUtils.isNotEmpty(scopeMap.get(key))) {
                NodeParser nodeParser = new NodeParser(scopeMap.get(key), key, rootNode.artifact);
                Node node = nodeParser.parseInternal(0);
                nodeMap.put(key, node);
            }
        }
        scopeMap.clear();
        return mergeNodes(rootNode, nodeMap);
    }

    /**
     * 将fileTree中的dependencies添加到第一层
     *
     * @param buildFile
     * @param projectDescriptor
     */
    private void addFileTreeDependencies(BuildFile buildFile, ProjectDescriptor projectDescriptor) {
        String libDirStr = projectDescriptor.getLibDir();
        if (libDirStr == null) {
            return;
        }
        File libDir = new File(libDirStr);
        if (libDir.isDirectory()) {
            File[] files = libDir.listFiles();
            if (files == null) {
                return;
            }
            Arrays.stream(files)
                    .filter(libFile -> libFile.getName().endsWith(".jar") || libFile.getName().endsWith(".aar"))
                    .forEach(libFile -> {
                        Artifact dependency = createArtifactByLibFile(projectDescriptor.getName(), libFile);
                        buildFile.addChild(new BuildFile(libFile.toPath(), dependency));
                    });
        }
    }

    private Artifact createArtifactByLibFile(String name, File libFile) {
        Artifact artifact = new Artifact();
        artifact.setGroupId(name);
        artifact.setVersion("unknown");
        artifact.setArtifactId(libFile.getName());
        artifact.setMd5(DependencyAnalyzeHelper.fastMd5(libFile.getPath()));
        artifact.setScope("compile");
        return artifact;
    }

    private Node getNodeByArtifactId(String artifactId) {
        Artifact artifact = new Artifact();
        artifact.setGroupId("project");
        artifact.setArtifactId(artifactId);
        artifact.setVersion("");
        return new Node(artifact);
    }

    private Node mergeNodes(Node root, Map<String, Node> nodeMap) {
        mergeNode(root, nodeMap.get("compile"));
        mergeNode(root, nodeMap.get("runtime"));
        mergeNode(root, nodeMap.get("test"));
        nodeMap.clear();
        return root;
    }

    private void mergeNode(Node root, Node toMerge) {
        if (toMerge == null) {
            return;
        }
        List<Node> nodes = toMerge.getChildNodes();
        for (Node node : nodes) {
            if (!root.containsNode(node.getKey())) {
                root.addChildNode(node);
            } else {
                Node rootNodeChild = root.getChild(node.getKey());
                mergeNode(rootNodeChild, node);
            }
        }
    }

    private Map<String, List<String>> getContext(String buildFile, Process pro, Map<String, String> keyMap) throws Exception {
        Map<String, List<String>> scopeMap = new LinkedHashMap<>();
        scopeMap.put("compile", new ArrayList<>());
        scopeMap.put("runtime", new ArrayList<>());
        scopeMap.put("test", new ArrayList<>());
        Future<String> errorMsg = ProcessUtil.getErrorMsg(pro);
        Future<String> inputMsg = ProcessUtil.getInputMsg(pro);
        String error = errorMsg.get();
        String input = inputMsg.get();
        log.info("dependencies info {}",input);
        String[] inputs = input.split("\n");
        String scope;
        for (int index = 0; index < inputs.length; index++) {
            String line = inputs[index];
            if (line.startsWith(keyMap.get("compile"))) {
                scope = "compile";
                Pair<Integer, List<String>> pair = extractValidRows(inputs, index);
                scopeMap.get(scope).addAll(pair.getValue());
                index = pair.getKey();
                continue;
            }
            if (line.startsWith(keyMap.get("runtime"))) {
                scope = "runtime";
                Pair<Integer, List<String>> pair = extractValidRows(inputs, index);
                scopeMap.get(scope).addAll(pair.getValue());
                index = pair.getKey();
                continue;
            }
            if (line.startsWith(keyMap.get("test"))) {
                scope = "test";
                Pair<Integer, List<String>> pair = extractValidRows(inputs, index);
                scopeMap.get(scope).addAll(pair.getValue());
                index = pair.getKey();
            }
        }
        if(!error.isEmpty()){
            log.info("parse build file {} has an error {}",buildFile,error);
        }
        hasError(scopeMap, error, buildFile);
        return scopeMap;
    }

    private void hasError(Map<String, List<String>> scopeMap, String error, String buildFile) throws DependencyParserException {
        boolean testError = true;
        for (String key : scopeMap.keySet()) {
            List<String> context = scopeMap.get(key);
            if (!context.isEmpty()) {
                testError = false;
                break;
            }
        }
        if (!error.isEmpty() && testError) {
            throw new DependencyParserException("module:"
                    + new File(buildFile).getParentFile().getName()
                    + ",error:" + error);
        }
    }

    protected Map<String, String> getDependencyKeyMap() {
        Map<String, String> keyMap = new HashMap<>();
        if (this.project.isAndroid()) {
            keyMap.put("compile", "releaseCompileClasspath");
            keyMap.put("runtime", "releaseRuntimeClasspath");
            keyMap.put("test", "releaseUnitTestCompileClasspath");
        } else {
            keyMap.put("compile", "compileClasspath");
            keyMap.put("runtime", "runtimeClasspath");
            keyMap.put("test", "testCompileClasspath");
        }
        return keyMap;
    }


    private Process executeCmd(String profile) throws IOException {
        return command.dependencyAnalyze(profile);
    }

    protected void downloadDependencies(String profile) throws Exception {
        command.DownloadDependencies(profile);
    }

    private String resolveMd5(Artifact artifact, boolean useMavenLocal) {
        String md5 = null;
        if (useMavenLocal) {
            md5 = mavenResolver.resolveMd5(artifact, project.getRepo().getMavenLocal());
        }
        if (md5 == null) {
            md5 = gradleResolver.resolveMd5(artifact, localRepo);
        }
        return md5;
    }

    private Pair<Integer, List<String>> extractValidRows(String[] inputs, int index) {
        String line = inputs[index];
        ArrayList<String> validLines = new ArrayList<>();
        while (StringUtils.isNotBlank(line)) {
            line = inputs[++index];
            if (checkValid(line)) {
                validLines.add(line);
            }
        }
        return new Pair<>(index, validLines);
    }

    private boolean checkValid(String line) {
        String trim = line.trim();
        if (!(trim.startsWith("+") || trim.startsWith("|") || trim.startsWith("\\"))) {
            return false;
        }
        return trim.contains(":");
    }

    private static class NodeParser {
        private List<String> lines;
        private Integer lineIndex = -1;
        private String scope;
        private Artifact root;
        private Set<String> parsed = new HashSet<>();

        public NodeParser(List<String> lines, String scope, Artifact root) {
            this.lines = lines;
            this.scope = scope;
            this.root = root;
        }

        private Node parseInternal(int depth) {
            Node node;
            if (depth == 0) {
                node = new Node(root);
            } else {
                node = this.parseLine();
            }
            ++this.lineIndex;

            while (this.lineIndex < this.lines.size() && this.computeDepth(this.lines.get(this.lineIndex)) > depth) {
                Node child = this.parseInternal(depth + 1);
                if (checkEffective(child)) {
                    node.addChildNode(child);
                }
            }
            return node;
        }

        private boolean checkEffective(Node node) {
            if (parsed.contains(node.getKey())) {
                return false;
            }
            parsed.add(node.getKey());
            return true;
        }

        private Node parseLine() {
            String artifactContext = lines.get(lineIndex);
            artifactContext = resolve(artifactContext);
            String[] tokens = artifactContext.split(":");
            Artifact artifact = new Artifact();
            if (artifactContext.startsWith("project")) {
                artifact.setGroupId(tokens[0].trim());
                artifact.setArtifactId(tokens[1].trim());
                artifact.setVersion("");
                artifact.setScope(scope);
                return new Node(artifact);
            } else {
                artifact.setGroupId(tokens[0].trim());
                artifact.setArtifactId(tokens[1].trim());
                artifact.setVersion(tokens[2].trim());
                artifact.setScope(this.scope);
                return new Node(artifact);
            }

        }

        private String resolve(String line) {
            String preRegx = "^(\\+|\\\\|)-{3}";
            String postRegx = "\\(.\\)";
            String resolved = line.replace("|", "").trim();
            if (resolved.contains("->")) {
                int versionIndex = resolved.lastIndexOf(":") + 1;
                int arrowIndex = resolved.indexOf("->") + 2;
                resolved = resolved.replace(resolved.substring(versionIndex),
                        resolved.substring(arrowIndex).trim());
            }
            resolved = resolved.replaceAll(preRegx, "").trim()
                    .replaceAll(postRegx, "").trim();
            return resolved;
        }

        private int computeDepth(String line) {
            int num = 0;
            label:
            while (num < line.length()) {
                char aChar = line.charAt(num);
                switch (aChar) {
                    case ' ':
                    case '+':
                    case '-':
                    case '\\':
                    case '|':
                        ++num;
                        break;
                    default:
                        break label;
                }
            }
            return num / 5;
        }
    }

    private static class Node {
        Artifact artifact;
        private Node parent;
        private final List<Node> childNodes = new LinkedList();

        public Node(Artifact artifact) {
            this.artifact = artifact;
        }

        private void addChildNode(Node child) {
            this.childNodes.add(child);
            child.setParent(this);
        }

        private List<Node> getChildNodes() {
            return childNodes;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        /**
         * @return key
         */
        public String getKey() {
            return this.artifact.getKey();
        }

        private boolean containsNode(String key) {
            for (Node childNode : this.childNodes) {
                if (childNode.getKey().equals(key)) {
                    return true;
                }
            }
            return false;
        }

        private Node getChild(String key) {
            for (Node childNode : childNodes) {
                if (childNode.getKey().equals(key)) {
                    return childNode;
                }
            }
            return null;
        }

        private void removeChild(Node child) {
            this.childNodes.remove(child);
        }
    }
}
