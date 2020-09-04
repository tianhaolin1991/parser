package com.huawei.fossbot.dependency.dsl.gradle;

import com.huawei.fossbot.dependency.exception.DependencyParserException;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import java.io.File;
import java.nio.file.Path;

/**
 * Gradle Settings 解析器
 * 用于解析settings.gradle
 *
 * @author t30002128
 * @since 2020/06/04
 */
public class GradleScriptParser {

    private static final String SETTING_GROOVY = "com.huawei.fossbot.dependency.dsl.gradle.SettingsGroovy";
    private static final String PROJECT_GROOVY = "com.huawei.fossbot.dependency.dsl.gradle.ProjectGroovy";

    /**
     * 解析settings.gradle文件
     */
    public Settings parseSettings(Path settingFile) throws DependencyParserException {
        try {
            File setting = settingFile.toFile();
            CompilerConfiguration conf = new CompilerConfiguration();
            conf.setScriptBaseClass(SETTING_GROOVY);
            GroovyShell groovyShell = new GroovyShell(conf);
            Script settingScript = groovyShell.parse(setting);
            settingScript.invokeMethod("initSettings", setting);
            settingScript.run();
            return (Settings)settingScript.invokeMethod("getSettings",null);
        } catch (Exception e) {
            throw new DependencyParserException("parse "+settingFile.toString()+" failure:"+e.getMessage());
        }
    }

    /**
     * 解析settings.gradle文件
     */
    public Project parseBuildFile(Path buildFile,Settings settings) throws DependencyParserException {
        try {
            File build = buildFile.toFile();
            CompilerConfiguration conf = new CompilerConfiguration();
            conf.setScriptBaseClass(PROJECT_GROOVY);
            GroovyShell groovyShell = new GroovyShell(conf);
            Script buildScript = groovyShell.parse(build);
            buildScript.setBinding(new GradleBinding());
            buildScript.invokeMethod("init",settings);
            buildScript.run();
            return (Project)buildScript.invokeMethod("getProject",null);
        } catch (Exception e) {
            throw new DependencyParserException("parse "+buildFile.toString()+" failure:"+e.getMessage());
        }
    }
}
