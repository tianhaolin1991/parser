import com.huawei.fossbot.dependency.ParserManager;
import com.huawei.fossbot.dependency.ast.GradleFileParser;
import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.bean.ArtifactNode;
import com.huawei.fossbot.dependency.bean.BuildFile;
import com.huawei.fossbot.dependency.bean.ParseResult;
import com.huawei.fossbot.dependency.dsl.gradle.GradleScriptParser;
import com.huawei.fossbot.dependency.dsl.gradle.Project;
import com.huawei.fossbot.dependency.dsl.gradle.Settings;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.util.DependencyTreeHelper;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GradleTest {
    @Test
    public void gradleTest() throws IOException {
        String buildFile = "D:\\gradle\\HwRemoteDesktop\\app\\build.gradle";
        String settingsGradle = "D:\\gradle\\HwRemoteDesktop\\settings.gradle";
        GradleFileParser gradleDependencyChanger = new GradleFileParser();
        Settings settings = gradleDependencyChanger.getSettings();
        System.out.println("jja");
    }

    @Test
    public void dslTest() throws DependencyParserException {
        String buildFile = "D:\\gradle\\HwRemoteDesktop\\build.gradle";
        GradleScriptParser gradleScriptParser = new GradleScriptParser();
        Settings settings = gradleScriptParser.parseSettings(Paths.get("D:\\gradle\\HwRemoteDesktop\\settings.gradle"));
        Project project = gradleScriptParser.parseBuildFile(Paths.get(buildFile), settings);
        System.out.println("a");
    }


    @Test
    public void myTest2() throws  IOException{
        String buildFile = "D:\\Dev\\java\\fossbot-ci\\work\\jobs\\THL_Local_Test\\workspace\\Source";
        //String buildFile = "D:\\gradle\\IDebugLog";
        //String buildFile = "D:\\gradle\\overthere";
        //String buildFile = "D:\\gradle\\HwRemoteDesktop";
        ParseResult analyze = new ParserManager().analyze(buildFile, true);
        System.out.println(analyze.getMsg());
        List<ArtifactNode> artifacts = DependencyTreeHelper.generateDependencyTree(analyze.getBuildFile());
        for (ArtifactNode artifact : artifacts) {
            System.out.println(artifact.getGroupId()+":"+artifact.getArtifactId()+":"+artifact.getVersion()+":"+artifact.getScope()+"--"+artifact.getMd5());
        }
    }

}
