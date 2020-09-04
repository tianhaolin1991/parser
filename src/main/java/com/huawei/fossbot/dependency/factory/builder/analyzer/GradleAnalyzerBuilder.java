package com.huawei.fossbot.dependency.factory.builder.analyzer;

import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.analyzer.java.GradleAnalyzer;
import com.huawei.fossbot.dependency.ast.GradleFileParser;
import com.huawei.fossbot.dependency.bean.OS;
import com.huawei.fossbot.dependency.command.AndroidCommand;
import com.huawei.fossbot.dependency.command.Command;
import com.huawei.fossbot.dependency.command.GradleCommand;
import com.huawei.fossbot.dependency.dsl.gradle.ProjectDescriptor;
import com.huawei.fossbot.dependency.dsl.gradle.Settings;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 用于创建gradleParser对象
 *
 * @author t30002128 tianhaolin@huawei.com
 * @since 2020/06/17
 */
@Slf4j
public class GradleAnalyzerBuilder implements AnalyzerBuilder {
    @Override
    public DependencyAnalyzer build(String profile, boolean useGlobalSettings) throws DependencyParserException {
        Path settingPath = DependencyAnalyzeHelper.findFile(Paths.get(profile).getParent(), "settings.gradle", 1);
        if (settingPath == null) {
            throw new DependencyParserException("no settings.gradle file found");
        }
        try {
            GradleFileParser parser = new GradleFileParser();
            parser.parse(profile);
            Settings settings = parser.getSettings();
            ProjectDescriptor project = parser.getSettings().getRootProject();
            Path gradlewFile = detectGradlewFile(profile);
            Command command;
            if(project.isAndroid()){
                log.info("command type is android.");
                command = new AndroidCommand(gradlewFile);
            }else{
                log.info("command type is java.");
                command = new GradleCommand(gradlewFile);
            }
            return new GradleAnalyzer(profile, settings, project, command);
        } catch (IOException e) {
            throw new DependencyParserException("parse "+ profile + " failure!" +e.getMessage());
        }
    }

    private Path detectGradlewFile(String profile) {
        Path parent = Paths.get(profile).getParent();
        Path file;
        if (DependencyAnalyzeHelper.osType() == OS.WINDOWS) {
            file = DependencyAnalyzeHelper.findFile(parent, "gradlew.bat", 1);
        } else {
            file = DependencyAnalyzeHelper.findFile(parent, "gradlew", 1);
        }
        return file;
    }
}
