import com.huawei.fossbot.dependency.analyzer.java.MavenAnalyzer;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.parser.java.MvnParser;
import org.junit.Test;

import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MavenParserTest {


    @Test
    public void parseTest() throws Exception {
        MavenAnalyzer mavenAnalyzer = new MavenAnalyzer();
        List<Artifact> analyzers = mavenAnalyzer.analyze("D:\\Development\\maven\\child\\pom.xml");
        //List<MavenParser.Artifact> analyzers = MavenParser.mavenParse("D:\\Development\\agent\\pom.xml");
        ArrayList<String> analyzerStrs = new ArrayList<>();
        analyzers.forEach(artifact -> {
            analyzerStrs.add(artifact.getKey() + ":" + artifact.getVersion() + ":" + artifact.getScope());
            //System.out.println(artifact.getKey()+":"+artifact.getVersion()+":"+artifact.getScope());
        });

        MvnParser mvnParser = new MvnParser();
        List<Artifact> artifactList = mvnParser.parse("D:\\Development\\maven\\child\\pom.xml");
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
        MvnParser mvnParser = new MvnParser();
        List<Artifact> artifactList = mvnParser.parse("D:\\Development\\maven\\child\\pom.xml");
        for (Artifact artifact : artifactList) {
            System.out.println(artifact.getKey()+":"+artifact.getVersion()+":"+artifact.getScope());
        }
    }
}
