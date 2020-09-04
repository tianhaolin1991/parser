package com.huawei.fossbot.dependency.command;

import com.huawei.fossbot.dependency.bean.OS;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 执行gradle命令行
 * @since v1.0.4.4
 * @author t30002128
 */
public class GradleCommand extends Command {
    protected String GRADLE_CMD;
    private  String BUILD;
    private  String CLEAN;
    private  String DEPENDENCY_CMD;
    public GradleCommand(Path gradlewFile){
        if(gradlewFile != null){
            this.GRADLE_CMD = gradlewFile.toString();
        }else{
            this.GRADLE_CMD = GRADLE;
        }
        this.BUILD = GRADLE_CMD + " build --no-build-cache -x test";
        this.CLEAN = GRADLE_CMD + " clean ";
        this.DEPENDENCY_CMD = GRADLE_CMD + " dependencies";
    }
    @Override
    public Process dependencyAnalyze(String profile) throws IOException {
        String sourceRoot = new File(profile).getParent();
        ProcessBuilder pb = new ProcessBuilder();
        if (DependencyAnalyzeHelper.osType() == OS.WINDOWS) {
            pb.command("cmd", "/c", DEPENDENCY_CMD,"-p",sourceRoot);
        } else {
            pb.command("bash", "-c", DEPENDENCY_CMD,"-p",sourceRoot);
        }
        return pb.start();
    }

    @Override
    public void DownloadDependencies(String profile) throws IOException, InterruptedException {
       //do nothing
    }
}
