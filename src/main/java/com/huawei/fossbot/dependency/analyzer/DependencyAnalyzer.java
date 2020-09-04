package com.huawei.fossbot.dependency.analyzer;

import com.huawei.fossbot.dependency.bean.BuildFile;

import java.util.List;

/**
 * Interface of different analyzers
 *
 * @author t30002128 tianhaolin@huawei.com
 * @since 2020/05/11
 */
public interface DependencyAnalyzer {
    /**
     * 生成依赖树
     * @return BuildFile
     * @throws Exception dependencyAnalyzerException
     */
    BuildFile analyze() throws Exception;

    /**
     * 单文件解析(不解析modules)
     * @return BuildFile
     * @throws Exception dependencyAnalyzerException
     */
    BuildFile analyzeRootProject() throws Exception;

    /**
     * 解析直接依赖
     * @return BuildFile
     * @throws Exception dependencyAnalyzerException
     */
    BuildFile directAnalyze() throws Exception;
}
