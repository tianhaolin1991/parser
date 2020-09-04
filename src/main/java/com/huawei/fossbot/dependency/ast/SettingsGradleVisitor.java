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
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import java.io.File;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * build gradle visitor
 * to read variable version in gradle
 *
 * @author
 * @since
 */
public class SettingsGradleVisitor extends CodeVisitorSupport {

    // The currently parsed node type stack
    Stack<String> currentVisitedNodeTypes = new Stack<>();

    // gradle currentFileLines
    List<String> currentFileLines;

    //line break charachtor
    String currentFileLineBreak = System.lineSeparator();

    Pattern includePattern = Pattern.compile("(:{0,1}[A-Za-z0-9_\\-]+)+");

    private Settings settings;

    SettingsGradleVisitor(List<String> currentFileLines, String buildFilePath) {
        this.currentFileLines = currentFileLines;
        this.settings = new Settings(buildFilePath);
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {
        // get methodName
        String methodName = call.getMethodAsString();

        if (methodName == null) {
            return;
        }
        if (methodName.equals("includeFlat")) {
            // repositories nodes
            String text =  call.getArguments().getText();
            Matcher matcher = includePattern.matcher(text);
            while (matcher.find()){
                settings.includeFlat(matcher.group());
            }
        } else if(methodName.equals("include")){
            String text =  call.getArguments().getText();
            Matcher matcher = includePattern.matcher(text);
            while (matcher.find()){
                settings.include(matcher.group());
            }
        } else{
            String text = call.getText();
            System.out.println(text);
        }
    }

    @Override
    public void visitBinaryExpression(BinaryExpression expression) {
        String leftChildRaw = getRawContent(expression.getLeftExpression());
        String rightChildRaw = getRawContent(expression.getRightExpression())
                .replace("'", "");
        if (leftChildRaw.equals("rootProject.name")) {
            settings.getRootProject().setName(rightChildRaw);
        }
    }

    String getRawContent(ASTNode node) {
        // Get the starting and ending line numbers and column numbers
        int startLineNumber = node.getLineNumber();
        int startColumnNumber = node.getColumnNumber();
        int endLineNumber = node.getLastLineNumber();
        int endColumnNumber = node.getLastColumnNumber();

        // Concatenate each line of string
        if (endLineNumber == startLineNumber) {
            String line = this.currentFileLines.get(startLineNumber - 1);
            line = line.substring(startColumnNumber - 1, endColumnNumber - 1);
            return line;
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append(this.currentFileLines.get(startLineNumber - 1).substring(startColumnNumber - 1))
                    .append(this.currentFileLineBreak);
            for (int i = startLineNumber; i < endLineNumber - 1; i++) {
                sb.append(this.currentFileLines.get(i)).append(this.currentFileLineBreak);
            }
            sb.append(this.currentFileLines.get(endLineNumber - 1).substring(0, endColumnNumber - 1));
            return sb.toString();
        }
    }

    public Settings getSettings(){
        return this.settings;
    }
}
