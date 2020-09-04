package com.huawei.fossbot.dependency.analyzer.java;

import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.bean.BuildFile;
import com.huawei.fossbot.dependency.bean.OS;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.md5.DependencyMd5Resolver;
import com.huawei.fossbot.dependency.md5.MavenMd5Resolver;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import com.huawei.fossbot.dependency.util.ProcessUtil;
import com.huawei.fossbot.dependency.log.Logger;
import fr.dutra.tools.maven.deptree.core.InputType;
import fr.dutra.tools.maven.deptree.core.Node;
import fr.dutra.tools.maven.deptree.core.ParseException;
import fr.dutra.tools.maven.deptree.core.Parser;
import org.apache.commons.lang3.StringUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;


/**
 * MavenAnalyzer
 *
 * @author d00380361 dongkeqin@huawei.com
 * @update t30002128 tianhaolin@huawei.com 2020/05/11
 * @since 2019/10/15 9:16
 */
public class MavenAnalyzer implements DependencyAnalyzer {
    private static Logger log = Logger.getInstance();
    private static String DEPENDENCY_TREE_PRE = "Wrote dependency tree to:";
    private static String MAVEN_CMD = "mvn ";
    private static String DEPENDENCY_CMD = MAVEN_CMD + "dependency:tree -DoutputFile=mvn_dependency_tree_output -DoutputType=text ";
    private String settings;
    private String localRepo;
    private String remoteRepo;
    private String profile;
    private BuildFile buildFile;
    private DependencyMd5Resolver mavenMd5Resolver;

    public MavenAnalyzer(String profile,String settings,String localRepo,
        String remoteRepo, BuildFile buildFile){
        this.profile = profile;
        this.settings = settings;
        this.localRepo = localRepo;
        this.remoteRepo = remoteRepo;
        this.buildFile = buildFile;
        this.mavenMd5Resolver = new MavenMd5Resolver();
    }


    @Override
    public BuildFile analyze() throws Exception {
        log.info("start analyze,use build file {}", profile);
        List<File> dependencyFiles = generateDependencyFiles();
        File rootFile = dependencyFiles.get(0);
        dependencyFiles.remove(0);
        BuildFile rootBuildFile = resolveDependencyFile(rootFile);
        for (File dependencyFile : dependencyFiles) {
            rootBuildFile.addModule(resolveDependencyFile(dependencyFile));
        }
        return rootBuildFile;
    }

    @Override
    public BuildFile analyzeRootProject() throws Exception {
        List<File> dependencyFiles = generateDependencyFiles();
        File rootFile = dependencyFiles.get(0);
        dependencyFiles.remove(0);
        return resolveDependencyFile(rootFile);
    }

    @Override
    public BuildFile directAnalyze() throws Exception {
        BuildFile rootBuildFile = analyzeRootProject();
        for (BuildFile child : rootBuildFile.getChildren()) {
            child.getChildren().clear();
        }
        return rootBuildFile;
    }

    private BuildFile resolveDependencyFile(File dependencyFile) throws ParseException, IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dependencyFile)))) {
            InputType type = InputType.TEXT;
            Parser parser = type.newParser();
            Node node = parser.parse(reader);
            Path path = dependencyFile.toPath().getParent().resolve("pom.xml");
            BuildFile build = new BuildFile(path, getArtifact(node));
            for (Node childNode : node.getChildNodes()) {
                build.addChild(resolveParserNode(childNode));
            }
            return build;
        } finally {
            dependencyFile.delete();
        }
    }

    private BuildFile resolveParserNode(Node node) {
        Artifact artifact = getArtifact(node);
        String md5 = null;
        if (artifact.getScope().equals("system")) {
            BuildFile systemChild = buildFile.getChild(artifact.getKey());
            if(systemChild!=null){
                String source = buildFile.getChild(artifact.getKey()).getArtifact().getSource();
                md5 = DependencyAnalyzeHelper.fastMd5(source);
            }
        } else {
            md5 = mavenMd5Resolver.resolveMd5(artifact, localRepo);
        }
        artifact.setMd5(md5);
        BuildFile build = new BuildFile(getLocalPathFromNode(node), artifact);
        for (Node childNode : node.getChildNodes()) {
            build.addChild(resolveParserNode(childNode));
        }
        return build;
    }

    private Path getLocalPathFromNode(Node node) {
        Path path = Paths.get(localRepo).resolve(node.getGroupId())
                .resolve(node.getArtifactId()).resolve(node.getVersion())
                .resolve(node.getArtifactId() + "-" + node.getVersion() + ".pom");
        return path;
    }

    private Artifact getArtifact(Node node) {
        Artifact artifact = new Artifact();
        artifact.setGroupId(node.getGroupId());
        artifact.setArtifactId(node.getArtifactId());
        artifact.setVersion(node.getVersion());
        artifact.setScope(node.getScope());
        artifact.setClassifier(node.getClassifier());
        return artifact;
    }

    /**
     * 生成dependency tree的files
     */
    protected List<File> generateDependencyFiles() throws Exception {
        // 每个Module会生成单独的mvn_dependency_tree_output文件
        Process process = executeCmd(profile);
        List<Path> outputPaths = findPaths(process);
        ArrayList<File> dependencyTrees = new ArrayList<>();
        outputPaths.forEach(path -> dependencyTrees.add(path.toFile()));
        return dependencyTrees;
    }


    private List<Path> findPaths(Process pro) throws Exception {
        Future<String> inputMsg = ProcessUtil.getInputMsg(pro);
        Future<String> errorMsg = ProcessUtil.getErrorMsg(pro);
        String msg = inputMsg.get();
        String error = errorMsg.get();
        ArrayList<Path> outputPaths = new ArrayList<>();
        if(error.isEmpty()){
            boolean success = false;
            for (String line : msg.split(System.lineSeparator())) {
                if (line.contains(DEPENDENCY_TREE_PRE)) {
                    Path outputPath = Paths.get(line.split(DEPENDENCY_TREE_PRE)[1].trim());
                    outputPaths.add(outputPath);
                }
                if (line.contains("[INFO] BUILD SUCCESS")) {
                    success = true;
                    break;
                }
            }
            if(!success){
                log.info("parse build file {} has an error {}",buildFile,error);
                String[] failMsg = msg.split(System.lineSeparator());
                StringBuilder sb = new StringBuilder(System.lineSeparator());
                for (String str : failMsg) {
                    if(!logSkip(str)){
                        sb.append(str).append(System.lineSeparator());
                    }
                }
                deleteDependencyFiles(outputPaths);
                throw new DependencyParserException(sb.toString());
            }
        }else{
            throw new DependencyParserException(error);
        }
        return outputPaths;
    }

    private boolean logSkip(String line) {
        String str = line.trim();
        if (StringUtils.isEmpty(str)) {
            return true;
        }
        return !str.startsWith("[INFO]") && !str.startsWith("[ERROR]");
    }

    private void deleteDependencyFiles(ArrayList<Path> outputPaths) {
        for (Path outputPath : outputPaths) {
            if(outputPath.toFile().exists()){
                outputPath.toFile().delete();
            }
        }
    }

    private Process executeCmd(String profile) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(profile).getParentFile());
        if (DependencyAnalyzeHelper.osType() == OS.WINDOWS) {
            pb.command("cmd", "/c", DEPENDENCY_CMD + "-s " +"\""+settings+"\"");
        } else {
            pb.command("bash", "-c", DEPENDENCY_CMD + "-s " +"\""+settings+"\"");
        }
        return pb.start();
    }
}
