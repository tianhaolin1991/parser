package com.huawei.fossbot.dependency.factory.builder.analyzer;

import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.exception.DependencyParserException;

/**
 * @author t30002128 tianhaolin@huawei.com
 * @since 2020/06/17
 */
public interface AnalyzerBuilder {

    DependencyAnalyzer build(String profile, boolean useGlobalSettings) throws DependencyParserException;

}
