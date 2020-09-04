package com.huawei.fossbot.dependency.md5;

import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.util.DependencyAnalyzeHelper;
import java.nio.file.Path;

/**
 * md5解析器
 *
 * @author t30002128
 * @since 2020/05/20
 */
public abstract class DependencyMd5Resolver {

    /**
     * 解析md5
     */
    public String resolveMd5(Artifact artifact, String localRepo) {
        Path dependencyPath = resolveDependencyPath(artifact, localRepo);
        return md5(dependencyPath);
    }

    /**
     * get dependency path
     *
     * @param artifact  artifact
     * @param localRepo 本地仓库地址
     * @return path
     */
    public abstract Path resolveDependencyPath(Artifact artifact, String localRepo);

    public String md5(Path filePath) {
        if(filePath!=null){
            return DependencyAnalyzeHelper.fastMd5(filePath.toString());
        }
        return null;
    }
}
