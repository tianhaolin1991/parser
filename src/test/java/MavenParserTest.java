import com.huawei.fossbot.dependency.analyzer.java.MavenAnalyzer;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.parser.java.MParser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MavenParserTest {


    @Test
    public void parseTest() throws Exception {
        String path = "/Users/tianhaolin1991/Desktop/test/pom.xml";
        MavenAnalyzer mavenAnalyzer = new MavenAnalyzer();
        List<Artifact> analyzers = mavenAnalyzer.analyze(path);
        //List<MavenParser.Artifact> analyzers = MavenParser.mavenParse("D:\\Development\\agent\\pom.xml");
        ArrayList<String> analyzerStrs = new ArrayList<>();
        analyzers.forEach(artifact -> {
            analyzerStrs.add(artifact.getKey() + ":" + artifact.getVersion() + ":" + artifact.getScope());
            //System.out.println(artifact.getKey()+":"+artifact.getVersion()+":"+artifact.getScope());
        });

        MParser mvnParser = new MParser();
        List<Artifact> artifactList = mvnParser.parse(path);
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
        List<String > collect2 = parserList.stream().filter(analyzer -> {
            return !analyzerStrs.contains(analyzer);
        }).collect(Collectors.toList());
        collect2.forEach(artifact -> {
            System.out.println(artifact);
        });
    }

    @Test
    public void MvnReadPomFileTest() throws Exception {
        //String path = "/Users/tianhaolin1991/Desktop/test/pom.xml";
        String path = "/Users/tianhaolin1991/IdeaProjects/learn/dependencies/child/pom.xml";
        MParser mvnParser = new MParser();
        List<Artifact> artifactList = mvnParser.parse(path);
        for (Artifact artifact : artifactList) {
            System.out.println(artifact.getKey()+":"+artifact.getVersion()+":"+artifact.getScope());
        }
    }
}
