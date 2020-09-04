package com.huawei.fossbot.dependency.command;

import java.io.IOException;

/**
 * 执行依赖解析命令行
 * @since v1.0.4.4
 * @author t30002128
 */
public abstract class Command{
    protected static String GRADLE = "gradle";
    /**
     * 使用命令执行依赖解析
     * @param profile build文件路径
     * @return Process
     */
    public abstract Process dependencyAnalyze(String profile) throws IOException;

    /**
     * 使用命令行下载依赖
     * @param profile build文件路径
     * @return Process
     */
    public abstract void DownloadDependencies(String profile) throws IOException, InterruptedException;


}
