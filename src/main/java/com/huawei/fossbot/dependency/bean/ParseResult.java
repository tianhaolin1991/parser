package com.huawei.fossbot.dependency.bean;

import java.nio.file.Path;

/**
 * 解析结果
 *
 * @since 2020/06/03
 */
public class ParseResult {
    private String msg;
    private Path path;
    private boolean success = false;
    private BuildFile buildFile;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public BuildFile getBuildFile() {
        return buildFile;
    }

    public void setBuildFile(BuildFile buildFile) {
        this.buildFile = buildFile;
    }

    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
