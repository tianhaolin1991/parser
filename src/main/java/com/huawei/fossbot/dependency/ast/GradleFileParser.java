/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.huawei.fossbot.dependency.ast;

import com.huawei.fossbot.dependency.dsl.gradle.Settings;
import com.huawei.fossbot.dependency.util.FileUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.control.CompilePhase;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import static com.huawei.fossbot.dependency.util.FileUtils.getUniformPathStr;

/**
 * build gradle visitor
 * to read variable version in gradle
 *
 * @author
 * @since
 */
public class GradleFileParser {

    private Settings settings;

    public void parse(String buildFilePath) throws IOException {
        File settingsFile = Paths.get(buildFilePath).getParent()
                .resolve("settings.gradle").toFile();
        if(!settingsFile.exists()){
            getDefaultSettings(settingsFile,buildFilePath);
        }else {
            parseGradleFile(getUniformPathStr(settingsFile));
        }
    }

    private void getDefaultSettings(File settingsFile, String buildFilePath) {
        this.settings = new Settings(getUniformPathStr(settingsFile));
        settings.include(new File(buildFilePath).getParent());
    }

    /**
     * used to parse gradle file
     * visit all nodes
     */
    private void parseGradleFile(String settingFilePath) throws IOException {
        AstBuilder builder = new AstBuilder();
        parseSettingsFile(builder,settingFilePath);
        for (String path : this.settings.getProjects().keySet()) {
            parseBuildFile(builder,path);
        }
    }

    private void parseSettingsFile(AstBuilder builder, String settingsFile) throws IOException {
        String fileContent = FileUtils.getFileContent(settingsFile);
        List<ASTNode> nodes = builder.buildFromString(CompilePhase.CONVERSION, true, fileContent);
        for (ASTNode node : nodes) {
            SettingsGradleVisitor settingsVisitor = new SettingsGradleVisitor(getOriginalFileLines(settingsFile), settingsFile);
            node.visit(settingsVisitor);
            this.settings = settingsVisitor.getSettings();
        }
    }

    private void parseBuildFile(AstBuilder builder, String buildFilePath) throws IOException {
        String fileContent = FileUtils.getFileContent(buildFilePath);
        List<ASTNode> nodes = builder.buildFromString(CompilePhase.CONVERSION, true, fileContent);
        for (ASTNode node : nodes) {
            getOriginalFileLines(buildFilePath);
            BuildGradleVisitor buildVisitor = new BuildGradleVisitor(getOriginalFileLines(buildFilePath),
                    this.settings.getProject(buildFilePath));
            node.visit(buildVisitor);
        }
    }

    private List<String> getOriginalFileLines(String filePath) throws IOException {
        return FileUtils.getFileLines(filePath, StandardCharsets.UTF_8);
    }

    public Settings getSettings() {
        return this.settings;
    }

}
