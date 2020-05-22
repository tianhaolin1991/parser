import com.huawei.fossbot.dependency.DependencyAnalyzerManager;
import com.huawei.fossbot.dependency.bean.AnalyzerType;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.parser.java.MavenParser;
import com.huawei.fossbot.dependency.util.RepoPathUtil;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


public class DependencyAnalyzerMangerTest {

    @Test
    public void startAnalyzeTest(){
        List<Artifact> artifacts = DependencyAnalyzerManager.startAnalyze("D:\\Development\\com.tianhaolin.learn\\gradle");
        artifacts.forEach(System.out::println);
        List<Artifact> artifacts1 = DependencyAnalyzerManager.startAnalyze("D:\\Development\\com.tianhaolin.learn.jdk8");
        System.out.println(artifacts1);
    }


    @Test
    public void mavenParserTest() throws IOException, SAXException, ParserConfigurationException {
      /*  List<MavenParser.Artifact> artifacts = MavenParser.mavenParse("D:\\Development\\com.tianhaolin.learn.jdk8\\pom.xml");
        artifacts.forEach(System.out::println);*/
    }
}
