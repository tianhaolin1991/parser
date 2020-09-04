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

import com.huawei.fossbot.dependency.dsl.gradle.ProjectDescriptor;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
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
public class BuildGradleVisitor extends CodeVisitorSupport {

    // The currently parsed node type stack
    Stack<String> currentVisitedNodeTypes = new Stack<>();

    // gradle currentFileLines
    List<String> currentFileLines;

    //line break charachtor
    String currentFileLineBreak = System.lineSeparator();

    // pattern
    Pattern repositoryPattern = Pattern.compile("[\"'].*[\"']");

    private ProjectDescriptor project;

    BuildGradleVisitor(List<String> currentFileLines, ProjectDescriptor project) {
        this.currentFileLines = currentFileLines;
        this.project = project;
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {
        // get methodName
        String methodName = call.getMethodAsString();

        if (methodName == null) {
            return;
        }

        if(methodName.equals("apply")){
            //旧语法
            detectAndroidProject(call);
        }
        if(methodName.equals("allprojects")||
                methodName.equals("buildscript")) {
            enterNode(methodName);
        }
        if(methodName.equals("fileTree")){
            getLibDir(call);
        }
        if(methodName.equals("classpath")){
            if(currentVisitedNodeTypes.size() > 0){
                detectAndroidProject(call);
            }
        }
        if (methodName.equals("repositories")) {
            if(currentVisitedNodeTypes.size() > 0){
                enterNodeSpecifyParent(methodName,"allprojects");
            }else {
                enterNode(methodName);
            }
        } else if (currentVisitedNodeTypes.size() > 0) {
            // Get the parent node type of the current node
            String parentNodeType = currentVisitedNodeTypes.peek();
            if (parentNodeType.equals("repositories")) {
                // If the parent node type is a repository nodes
                visitRepositoriesChildNode(call);
            }
        }
        // Traversing child nodes
        super.visitMethodCallExpression(call);
        if(methodName.equals("buildscript")||
            methodName.equals("allProjects")||
            methodName.equals("repositories")) {
            leaveNode(methodName);
        }
    }

    private void getLibDir(MethodCallExpression call) {
        String text = call.getArguments().getText();
        String args = text.replaceAll("[\\[\\]\\(\\)]", "").trim();
        String relativeLibDir = args.replaceAll(".*dir['\"]*:", "").trim();
        this.project.setLibDir(relativeLibDir);
    }

    private void enterNodeSpecifyParent(String methodName, String parent) {
        String parentNodeType = currentVisitedNodeTypes.peek();
        if(parentNodeType.equals(parent)){
            // repositories nodes
            enterNode(methodName);
        }
    }

    private void detectAndroidProject(MethodCallExpression call) {
        String text = call.getArguments().getText();
        String args = text.replaceAll("[\\[\\]\\(\\)]", "").trim();
        if(args.contains("android.tools.build")){
            project.isAndroid(true);
        }
    }

    void enterNode(String parent) {
        currentVisitedNodeTypes.push(parent);
    }

    void visitRepositoriesChildNode(ASTNode node) {
        // Get the original string of the node
        String text = getRawContent(node).trim();
        if(text.startsWith("mavenLocal()")){
            project.getRepo().mavenLocal();
        }
        if (text.startsWith("maven")) {
            // get url
            Matcher matcher = repositoryPattern.matcher(text);
            if (matcher.find()) {
                String url = matcher.group(0);
                project.getRepo().getRemoteRepo().add(url);
            }
        }
    }

    void leaveNode(String type) {
        if(currentVisitedNodeTypes.isEmpty()){
            return;
        }
        if(currentVisitedNodeTypes.peek().equals(type)){
            currentVisitedNodeTypes.pop();
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
    public ProjectDescriptor getProject() {
        return this.project;
    }
}
