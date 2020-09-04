package com.huawei.fossbot.dependency.factory.builder.parser;

import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.parser.DependencyParser;

/**
 * @author t30002128 tianhaolin@huawei.com
 * @since 2020/06/17
 */
public interface ParserBuilder {

    DependencyParser build(String profile, boolean useGlobalSettings) throws DependencyParserException;

}
