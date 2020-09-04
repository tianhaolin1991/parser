package com.huawei.fossbot.dependency.dsl.gradle;

import com.huawei.fossbot.dependency.util.FileUtils;
import java.io.File;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *  denote settings.gradle
 *
 * @author t30002128
 * @since 2020/06/04
 */
public class Settings {
    private String settingsPath;
    private ProjectDescriptor rootProjectDescriptor;
    private Map<String, ProjectDescriptor> registeredProjects = new LinkedHashMap<>();

    public Settings(String settingsDir) {
        this.settingsPath = FileUtils.getUniformPathStr(settingsDir);
        File dir = new File(settingsPath).getParentFile();
        this.rootProjectDescriptor =
                createProjectDescriptor(null,dir.getName(),dir);
    }

    /**
     * include line
     */
    public void include(String... projectNames) {
        for (String projectPath : projectNames) {
            String subPath = "";
            String[] pathElements = removeTrailingColon(projectPath).split(":");
            ProjectDescriptor parentProject = rootProjectDescriptor;
            for (String pathElement : pathElements) {
                String projectDir = getProjectSubDir(parentProject, pathElement);
                ProjectDescriptor projectDescriptor;
                if(registeredProjects.containsKey(projectDir)){
                    projectDescriptor = registeredProjects.get(projectDir);
                }else{
                    projectDescriptor = createProjectDescriptor(parentProject, pathElement,
                            new File(parentProject.getProjectDir(), pathElement));
                }
                subPath = subPath + ":" + pathElement;
                parentProject.getChildren().add(projectDescriptor);
                parentProject = projectDescriptor;
            }
        }
    }

    private String getProjectSubDir(ProjectDescriptor project,String subPath) {
        String projectDir = project.getProjectDir();
        return FileUtils.getUniformPathStr(Paths.get(projectDir, subPath,project.getBuildFileName()));
    }

    /**
     * includeFlat line
     */
    public void includeFlat(String... projectNames) {
        for (String projectName : projectNames) {
            ProjectDescriptor projectDescriptor = createProjectDescriptor(rootProjectDescriptor, projectName,
                    Paths.get(rootProjectDescriptor.getProjectDir()).getParent().resolve(projectName).toFile());
            rootProjectDescriptor.getChildren().add(projectDescriptor);
        }
    }

    private String removeTrailingColon(String projectPath) {
        if (projectPath.startsWith(":")) {
            return projectPath.substring(1);
        }
        return projectPath;
    }

    private ProjectDescriptor createProjectDescriptor(ProjectDescriptor parent, String name, File dir) {
        ProjectDescriptor project = new ProjectDescriptor(parent, name, dir);
        registeredProjects.put(project.getBuildFilePath(),project);
        return project;
    }

    /**
     * rootProject. line
     */
    public ProjectDescriptor getRootProject() {
        return this.rootProjectDescriptor;
    }
    public Map<String, ProjectDescriptor> getProjects(){
        return this.registeredProjects;
    }

    public ProjectDescriptor getProject(String buildPath){
        return registeredProjects.get(buildPath);
    }
}

