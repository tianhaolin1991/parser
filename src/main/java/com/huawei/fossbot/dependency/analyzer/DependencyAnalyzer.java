package com.huawei.fossbot.dependency.analyzer;

import com.huawei.fossbot.dependency.bean.Artifact;

import java.util.List;

/**
 * Interface of different analyzers
 *
 * @author t30002128 tianhaolin@huawei.com
 * @since 2020/05/11
 */
public interface DependencyAnalyzer {

    /**
     * @param profile 依赖配置文件路径
     *
     * @return artifact集合
     *         null:something is wrong
     */
    List<Artifact> analyze(String profile);

}
