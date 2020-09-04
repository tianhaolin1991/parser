import com.huawei.fossbot.dependency.ParserManager;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.bean.BuildFile;
import com.huawei.fossbot.dependency.bean.ParseResult;

import com.huawei.fossbot.dependency.dsl.gradle.GradleScriptParser;
import com.huawei.fossbot.dependency.dsl.gradle.Project;
import com.huawei.fossbot.dependency.dsl.gradle.Settings;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.util.ProcessUtil;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


public class DependencyAnalyzerMangerTest {

    @Test
    public void gradleParserTest() throws DependencyParserException {
        String settingFile = "D:\\Dev\\java\\gradle_test\\settings.gradle";
        String profile = "D:\\Dev\\java\\gradle_test\\build.gradle";
        GradleScriptParser gradleSettingsParser = new GradleScriptParser();
        Settings settings = gradleSettingsParser.parseSettings(Paths.get(settingFile));
        System.out.println(settings);
        Project project = gradleSettingsParser.parseBuildFile(Paths.get(profile).getParent().resolve("build.gradle"),settings);
        System.out.println(project);
    }
    @Test
    public void analyzeTest() throws Exception {
        String path = "D:\\Dev\\java\\fossbot-server\\";
        //String path = "D:\\Development\\agent\\pom.xml";
        ParserManager parserManager = new ParserManager();
        ParseResult result = parserManager.analyze(path, true,true);
        BuildFile buildFile = result.getBuildFile();
        for (Artifact dependency : buildFile.getDependencies()) {
            System.out.println(dependency.getKey()+"-"+dependency.getVersion()+"-"+dependency.getMd5());
        }
    }

    @Test
    public void parseTest() throws Exception {
        //String path = "D:\\Development\\maven\\test\\pom.xml";
        String path = "C:\\Users\\t30002128\\Desktop\\new\\pom.xml";
        //String path = "D:\\Development\\agent\\pom.xml";
        ParserManager parserManager = new ParserManager();
        List<Artifact> analyzers = parserManager.analyze(path, true).getBuildFile().getDependencies();
        //List<MavenParser.Artifact> analyzers = MavenParser.mavenParse("D:\\Development\\agent\\pom.xml");
        ArrayList<String> analyzerStrs = new ArrayList<>();
        analyzers.forEach(artifact -> {
            analyzerStrs.add(artifact.getKey() + ":" + artifact.getVersion() + ":" + artifact.getScope());
            //System.out.println(artifact.getKey()+":"+artifact.getVersion()+":"+artifact.getScope());
        });


        List<Artifact> artifactList = parserManager.analyze(path, false).getBuildFile().getDependencies();
        ArrayList<String> parserList = new ArrayList<>();
        for (Artifact artifact : artifactList) {
            parserList.add(artifact.getKey() + ":" + artifact.getVersion() + ":" + artifact.getScope());
            //System.out.println(artifact.getKey()+":"+artifact.getVersion()+":"+artifact.getScope());
        }

        List<String> collect = analyzerStrs.stream().filter(analyzer -> {
            return !parserList.contains(analyzer);
        }).collect(Collectors.toList());
        collect.forEach(artifact -> {
            System.out.println(artifact);
        });
        System.out.println("=============================================================================");
        List<String> collect2 = parserList.stream().filter(analyzer -> {
            return !analyzerStrs.contains(analyzer);
        }).collect(Collectors.toList());
        collect2.forEach(artifact -> {
            System.out.println(artifact);
        });
    }

    @Test
    public void MvnReadPomFileTest() throws Exception {
        //String path = "C:\\Users\\t30002128\\Desktop\\new\\pom.xml";
        String path = "D:\\Dev\\java\\agent";
        //String path = "D:\\Dev\\java\\com.tianhaolin.learn.jdk8\\pom.xml";
        ParserManager parserManager = new ParserManager();
        ParseResult parseResult = parserManager.analyze(path, true);

        System.out.println(parseResult.getMsg());
        List<Artifact> artifactList = parseResult.getBuildFile().getDependencies();
        for (Artifact artifact : artifactList) {
            System.out.println(artifact.getKey() + ":" + artifact.getVersion() + ":" + artifact.getScope()+"-"+artifact.getMd5());
        }
    }
    @Test
    public void GradleReadPomFileTest() throws Exception {
        //String path = "D:\\Development\\fossbot-server\\pom.xml";
        String path = "D:\\Dev\\java\\gradle_test";
        //String path = "D:\\Development\\maven\\child\\pom.xml";
        ParserManager parserManager = new ParserManager();
        ParseResult parseResult = parserManager.analyze(path, true);

        System.out.println(parseResult.getMsg());
        List<Artifact> artifactList = parseResult.getBuildFile().getDependencies();

        for (Artifact artifact : artifactList) {
            System.out.println(artifact.getKey() + ":" + artifact.getVersion() + ":" + artifact.getScope()+",md5:"+artifact.getMd5());
        }
    }

    @Test
    public void GradleDirectParseTest() throws Exception {
        //String path = "D:\\Development\\fossbot-server\\pom.xml";
        String path = "D:\\Development\\com.tianhaolin.learn\\gradle";
        //String path = "D:\\Development\\maven\\child\\pom.xml";
        ParserManager parserManager = new ParserManager();
        ParseResult parseResult = parserManager.directAnalyze(path, true);
       // System.out.println(end-start);

       // System.out.println(parseResult.getMsg());
        List<Artifact> artifactList = parseResult.getBuildFile().getDependencies();
        for (Artifact artifact : artifactList) {
            System.out.println(artifact.getKey() + ":" + artifact.getVersion() + ":" + artifact.getScope()+",md5:"+artifact.getMd5());
        }
        System.out.println("====================================================");
        ParseResult parseResult2 = parserManager.directAnalyze(path, false);
        // System.out.println(end-start);

        // System.out.println(parseResult.getMsg());
        List<Artifact> artifactList2 = parseResult2.getBuildFile().getDependencies();
        for (Artifact artifact : artifactList2) {
            System.out.println(artifact.getKey() + ":" + artifact.getVersion() + ":" + artifact.getScope()+",md5:"+artifact.getMd5());
        }
    }

    @Test
    public void processTest() throws IOException, ExecutionException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("cmd.exe","/c","mvn -v");
        Process start = pb.start();
        Future<String> inputMsg = ProcessUtil.getInputMsg(start);
        Future<String> errorMsg = ProcessUtil.getErrorMsg(start);
        String s = inputMsg.get();
        String s1 = errorMsg.get();
        System.out.println("msg:"+s);
        System.out.println("error:"+s1);
    }

    @Test
    public void regxTest(){
        List<String> lines = new ArrayList<>();
        lines.add("+--- org.springframework:spring-context:5.2.4.RELEASE");
        lines.add("|    +--- org.springframework:spring-aop:5.2.4.RELEASE");
        lines.add("|    |    +--- org.springframework:spring-beans:5.2.4.RELEASE");
        lines.add("|    +--- org.springframework:spring-beans:5.2.4.RELEASE (*)");
        lines.add("|    +--- org.springframework:spring-core:5.2.4.RELEASE (n)");
        lines.add("+--- org.springframework:spring-context:5.1.9.RELEASE -> 5.2.4.RELEASE (*)");
        lines.add("+--- spring-context:commons-lang3:3.1 FAILED");
        String regex = "^(\\+|\\\\|)-{3}";
        for (String line : lines) {
            String resolved = line.replace("|", "").trim();
            if(resolved.contains("->")){
                int versionIndex = resolved.lastIndexOf(":")+1;
                int arrowIndex = resolved.indexOf("->")+2;
                resolved = resolved.replace(resolved.substring(versionIndex), resolved.substring(arrowIndex).trim());
            }
            if(resolved.contains("(n)")){

            }
            resolved = resolved.split(" ")[1];
        }


    }
}
