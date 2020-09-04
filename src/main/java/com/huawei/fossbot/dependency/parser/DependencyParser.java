package com.huawei.fossbot.dependency.parser;

import com.huawei.fossbot.dependency.bean.BuildFile;

/**
 * 自研解析器接口
 *
 * @author t30002128
 * @since 2020/05/20
 */
public interface DependencyParser {
    /**
     * 生成依赖树
     *
     * @param profile build file path string
     * @return BuildFile
     * @throws Exception dependencyAnalyzerException
     */
    BuildFile parse(String profile) throws Exception;

    /**
     * 单文件解析(不解析modules)
     *
     * @param profile build file path string
     * @return BuildFile
     * @throws Exception dependencyAnalyzerException
     */
    BuildFile parseRootProject(String profile) throws Exception;

    /**
     * 解析直接依赖
     *
     * @param profile build file path string
     * @return BuildFile
     * @throws Exception dependencyAnalyzerException
     */
    BuildFile directParse(String profile) throws Exception;
}
