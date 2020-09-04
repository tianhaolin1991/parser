package com.huawei.fossbot.dependency.factory.builder.analyzer;

import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.analyzer.java.MavenAnalyzer;
import com.huawei.fossbot.dependency.bean.BuildFile;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.parser.java.MavenParser;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import com.huawei.fossbot.dependency.util.RepoPathUtil;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 用于创建mavenParser对象
 *
 * @author t30002128 tianhaolin@huawei.com
 * @since 2020/06/17
 */
public class MavenAnalyzerBuilder implements AnalyzerBuilder {
    @Override
    public DependencyAnalyzer build(String profile, boolean useGlobalSettings) throws DependencyParserException {
        String settings;
        if (useGlobalSettings) {
            settings = RepoPathUtil.getMavenSettingPath();
        } else {
            Path settingPath = DependencyAnalyzeHelper.findFile(Paths.get(profile).getParent(), "settings.xml", 1);
            if (settingPath != null) {
                settings = settingPath.toString().replace("\\", "/");
            } else {
                settings = RepoPathUtil.getMavenSettingPath();
            }
        }
        if (settings == null) {
            throw new DependencyParserException("no settings.xml file found");
        }
        String localRepo = RepoPathUtil.getMavenLocalRepo(settings);
        String remoteRepo = RepoPathUtil.getMavenRemoteRepo(settings);

        BuildFile buildFile;
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            buildFile = new MavenParser(localRepo, remoteRepo, xmlReader).parseCurrentPomFileOnly(profile);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DependencyParserException("build error:" + e.getMessage());
        }
        return new MavenAnalyzer(profile, settings, localRepo, remoteRepo, buildFile);
    }

}
