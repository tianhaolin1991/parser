package com.huawei.fossbot.dependency.analyzer.java;

import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.md5.DependencyMd5Resolver;
import com.huawei.fossbot.dependency.md5.GradleMd5Resolver;
import com.huawei.fossbot.dependency.md5.MavenMd5Resolver;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * GradleAnalyzer
 *
 * @author d00380361 dongkeqin@huawei.com
 * @update t30002128 tianhaolin@huawei.com 2020/05/11
 * @since 2019/10/15 9:16
 */
public class GradleAnalyzer implements DependencyAnalyzer {

    private static Logger logger = LoggerFactory.getLogger(GradleAnalyzer.class);
    private static String GRADLE_CMD = "gradle ";
    private static String DOWNLOAD_CMD = GRADLE_CMD + "dependencyParserDownloadTask -p ";
    private static String DEPENDENCY_CMD = GRADLE_CMD + "dependencies -p ";

    private DependencyMd5Resolver mavenResolver = new MavenMd5Resolver();
    private DependencyMd5Resolver gradleResolver = new GradleMd5Resolver();

    @Override
    public List<Artifact> analyze(String profile) {
        try {
            Runtime runtime = Runtime.getRuntime();
            String sourceRoot = new File(profile).getParent();
            //下载依赖
            downloadDependencies(runtime,profile);

            //生成依赖树
            Process pro = runtime.exec(DEPENDENCY_CMD + sourceRoot);

            Map<String, List<String>> scopeMap = format(pro);

            boolean useMavenLocal = useMavenLocal(sourceRoot);

            return generateArtifacts(scopeMap, useMavenLocal);
        } catch (IOException e) {
            logger.error("analyze profile {} failure,error msg:", profile, e);
            return null;
        }
    }

    private void downloadDependencies(Runtime runtime, String profile) throws IOException {
        File buildFile = new File(profile);
        File enhancedFile = new File(buildFile.getParent(),"gradle-enhanced.build");
        try(FileWriter fileWriter = new FileWriter(enhancedFile,true)) {
            FileUtils.copyFile(buildFile,enhancedFile);
            fileWriter.write("\n\ntask dependencyParserDownloadTask(type: Exec) {\n" +
                    "    configurations.default.files\n" +
                    "    commandLine 'echo', 'Downloaded all dependencies'\n" +
                    "}");
            fileWriter.flush();
            runtime.exec(DOWNLOAD_CMD+enhancedFile);
        } finally {
            enhancedFile.delete();
        }

    }

    private boolean useMavenLocal(String sourceRoot) {
        Path path = Paths.get(sourceRoot).resolve("build.gradle");
        File buildFile = path.toFile();
        if (buildFile.exists()) {
          return useMavenLocal(buildFile);
        }
        return false;
    }

    private boolean useMavenLocal(File buildFile) {

        try(BufferedReader bf = new BufferedReader(new InputStreamReader(
                new FileInputStream(buildFile)))) {
            String line;
            boolean resolve = false;
            while ((line = bf.readLine()) != null) {
                if (line.startsWith("repositories")){
                    resolve =true;
                    continue;
                }
                if(!resolve) continue;

                line = line.trim().replace(" ", "");
                //mavenLocal()和自定义仓库哪个在前
                if(line.equals("mavenLocal()")) return true;
                if(line.equals("mavenCentral()")) return false;
                if(line.equals("maven")||line.equals("maven{")) return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private List<Artifact> generateArtifacts(Map<String, List<String>> scopeMap, boolean useMavenLocal) {
        Queue<String> solveQueue = new LinkedList<>();
        solveQueue.offer("provided");
        solveQueue.offer("compile");
        solveQueue.offer("runtime");
        solveQueue.offer("test");
        List<Artifact> artifacts = new ArrayList<>();
        while (!solveQueue.isEmpty()) {
            String scope = solveQueue.poll();
            mergeArtifacts(scope, scopeMap.get(scope), artifacts, useMavenLocal);
        }
        return artifacts;
    }

    private void mergeArtifacts(String scope, List<String> solvedStrings, List<Artifact> artifacts, boolean useMavenLocal) {
        if (solvedStrings == null) return;

        for (String solvedString : solvedStrings) {
            Artifact artifact = solvedStringToArtifact(scope, solvedString, useMavenLocal);
            if (!artifacts.contains(artifact)) {
                artifacts.add(artifact);
            }
        }
    }

    private Artifact solvedStringToArtifact(String scope, String solvedString, boolean useMavenLocal) {
        String[] informs = solvedString.split(":");
        Artifact artifact = new Artifact();
        artifact.setGroupId(informs[0]);
        artifact.setArtifactId(informs[1]);
        artifact.setVersion(informs[2]);
        artifact.setScope(scope);
        artifact.setMd5(resolveMd5(artifact,useMavenLocal));
        return artifact;
    }

    private String resolveMd5(Artifact artifact, boolean useMavenLocal) {
        String md5 = null;
        if(useMavenLocal){
            md5 = mavenResolver.resolveMd5(artifact);
        }
        if(md5==null){
            md5 = gradleResolver.resolveMd5(artifact);
        }
        return md5;
    }

    private boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    private Map<String, List<String>> format(Process pro) throws IOException {

        Map<String, List<String>> scopeMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));) {
            String line;
            String scope;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("compileClasspath")) {
                    scope = "compile";
                    scopeMap.put(scope, extractValidRows(reader));
                }
                if (line.startsWith("compileOnly")) {
                    scope = "provided";
                    scopeMap.put(scope, extractValidRows(reader));
                }
                if (line.startsWith("runtimeClasspath")) {
                    scope = "runtime";
                    scopeMap.put(scope, extractValidRows(reader));
                }
                if (line.startsWith("testCompileClasspath")) {
                    scope = "test";
                    scopeMap.put(scope, extractValidRows(reader));
                }
            }

        }
        return scopeMap;
    }

    private List<String> extractValidRows(BufferedReader reader) throws IOException {
        String line;
        ArrayList<String> validLines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.equals("")) break;
            if (!checkValid(line)) continue;
            String solvedLine = solve(line);
            validLines.add(solvedLine);
        }
        return validLines;
    }

    private boolean checkValid(String line) {
        String trim = line.trim();
        if (!(trim.startsWith("+") || trim.startsWith("|") || trim.startsWith("\\"))) return false;
        int size = trim.split(":").length;
        if (size != 3) return false;
        if (trim.endsWith("(*)") || trim.endsWith("(n)")) return false;
        return !trim.contains("->");
    }

    private String solve(String line) {
        String regex = "^(\\+|\\\\|)-{3}";
        String solvedLine = line.replace("|", "").replace(" ", "").replaceAll(regex, "");
        return solvedLine;
    }

}
