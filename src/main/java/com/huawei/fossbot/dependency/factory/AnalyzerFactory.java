package com.huawei.fossbot.dependency.factory;

import com.huawei.fossbot.dependency.analyzer.DependencyAnalyzer;
import com.huawei.fossbot.dependency.bean.Type;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.factory.builder.analyzer.AnalyzerBuilder;
import com.huawei.fossbot.dependency.factory.builder.analyzer.GradleAnalyzerBuilder;
import com.huawei.fossbot.dependency.factory.builder.analyzer.MavenAnalyzerBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author t30002128 tianhaolin@huawei.com
 * @since 2020/05/25
 */
public class AnalyzerFactory {
    private static Map<Type, AnalyzerBuilder> builderMap = new HashMap<>();

    static {
        builderMap.put(Type.MAVEN, new MavenAnalyzerBuilder());
        builderMap.put(Type.GRADLE, new GradleAnalyzerBuilder());
    }

    /**
     * @param type 解析类型
     * @param profile 构建文件
     * @param useGlobalSettings 是否使用项目文件下的settings文件
     * @return 返回对应的DependencyParser
     * @throws DependencyParserException 项目构建类型不支持异常
     */
    public static DependencyAnalyzer newInstance(Type type, String profile, boolean useGlobalSettings) throws Exception {
        if (builderMap.containsKey(type)) {
            AnalyzerBuilder builder = builderMap.get(type);
            return builder.build(profile, useGlobalSettings);
        }
        throw new DependencyParserException("Type:"+type+" unsupported yet");
    }

}
