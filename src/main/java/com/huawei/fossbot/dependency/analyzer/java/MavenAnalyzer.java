package com.huawei.fossbot.dependency.analyzer.java;

import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.md5.DependencyMd5Resolver;
import com.huawei.fossbot.dependency.md5.MavenMd5Resolver;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import fr.dutra.tools.maven.deptree.core.InputType;
import fr.dutra.tools.maven.deptree.core.Node;
import fr.dutra.tools.maven.deptree.core.Parser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * MavenAnalyzer
 *
 * @author d00380361 dongkeqin@huawei.com
 * @update t30002128 tianhaolin@huawei.com 2020/05/11
 * @since 2019/10/15 9:16
 */
public class MavenAnalyzer implements DependencyAnalyzer {
    private static Logger logger = LoggerFactory.getLogger(MavenAnalyzer.class);
    private static String MAVEN_CMD = "mvn ";
    private static String DEPENDENCY_CMD = MAVEN_CMD + "dependency:tree -DoutputFile=mvn_dependency_tree_output -DoutputType=text";
    private DependencyMd5Resolver resolver = new MavenMd5Resolver();

    @Override
    public List<Artifact> analyze(String profile) {
        List<Artifact> artifacts = null;
        try {
            List<File> dependencyTrees = generateDependencyTrees(profile);
            artifacts = solveDependencyTrees(dependencyTrees);
        } catch (IOException e) {
            logger.error("analyze profile {} failure,error msg:", profile, e);
            return null;
        }
        return artifacts;
    }

    protected List<File> generateDependencyTrees(String profile) throws IOException {
        String outputFileName = "mvn_dependency_tree_output";
        try {
            executeCmd(profile);// 每个Module会生成单独的mvn_dependency_tree_output文件
        } catch (IOException e) {
            e.printStackTrace();
            return null;// mvn命令执行异常
        }
        List<Path> outputPaths = DependencyAnalyzeHelper.findFiles(Paths.get(new File(profile).getParent()), outputFileName, 5);
        ArrayList<File> dependencyTrees = new ArrayList<>();
        outputPaths.forEach(path -> dependencyTrees.add(path.toFile()));
        return dependencyTrees;
    }

    protected List<Artifact> solveDependencyTrees(List<File> dependencyTrees) {

        Set<Artifact> artifacts = new LinkedHashSet<>();

        for (File dependencyTree : dependencyTrees) {
            List<Artifact> resolved = resolveDependencyTree(dependencyTree);
            if (CollectionUtils.isNotEmpty(resolved)) {
                artifacts.addAll(resolved);
            }
        }
        return new ArrayList<>(artifacts);
    }

    private List<Artifact> resolveDependencyTree(File dependencyTree) {

        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dependencyTree)))) {
            InputType type = InputType.TEXT;
            Parser parser = type.newParser();
            Node node = parser.parse(reader);
            Queue<Node> nodeQueue = new LinkedList<>();
            nodeQueue.add(node);
            List<Artifact> artifacts = new ArrayList<>();
            List<Node> visited = new ArrayList<>();

            boolean firstNode = true;
            while (CollectionUtils.isNotEmpty(nodeQueue)) {
                unFoldNodeList(nodeQueue, artifacts, visited, firstNode);
                firstNode = false;
            }
            visited.clear();
            return artifacts;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            dependencyTree.delete();
        }
    }

    private String getUniqueInformation(Node node) {
        return node.getGroupId() + ":" + node.getArtifactId() + ":" + node.getVersion();
    }

    private void unFoldNodeList(Queue<Node> nodeQueue, List<Artifact> artifacts, List<Node> visited, boolean firstNode) {
        Node rootNode = nodeQueue.poll();
        if (rootNode == null || visited.contains(rootNode)) return;
        visited.add(rootNode);
        LinkedList<Node> childNodes = rootNode.getChildNodes();
        childNodes.forEach(childNode -> {
            artifacts.add(generateArtifact(childNode, rootNode, !firstNode));
            nodeQueue.offer(childNode);
        });
    }

    private Artifact generateArtifact(Node node, Node parent, boolean setSource) {
        Artifact artifact = new Artifact();
        artifact.setGroupId(node.getGroupId());
        artifact.setArtifactId(node.getArtifactId());
        artifact.setVersion(node.getVersion());
        artifact.setScope(node.getScope());
        if (setSource) {
            artifact.setSource(getUniqueInformation(parent));
        }
        artifact.setMd5(resolver.resolveMd5(artifact));
        return artifact;
    }

    private void executeCmd(String profile) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Path path = Paths.get(profile).getParent();
        runtime.exec(DEPENDENCY_CMD,null,path.toFile());
    }

}
