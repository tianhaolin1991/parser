package com.huawei.fossbot.dependency.command;

import com.huawei.fossbot.dependency.bean.OS;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import com.huawei.fossbot.dependency.util.ProcessUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 执行Android gradle命令行
 * @since v1.0.4.4
 * @author t30002128
 */
public class AndroidCommand extends GradleCommand {

    private String ANDROID_DEPENDENCIES;

    public AndroidCommand(Path gradlewFile) {
        super(gradlewFile);
        this. ANDROID_DEPENDENCIES = GRADLE_CMD + " androidDependencies ";
    }

    @Override
    public void DownloadDependencies(String profile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(profile).getParentFile());
        if (DependencyAnalyzeHelper.osType() == OS.WINDOWS) {
            ProcessUtil.waitFor(pb.command("cmd", "/c", ANDROID_DEPENDENCIES).start());
        } else {
            ProcessUtil.waitFor(pb.command("bash", "-c", ANDROID_DEPENDENCIES).start());
        }
    }
}
