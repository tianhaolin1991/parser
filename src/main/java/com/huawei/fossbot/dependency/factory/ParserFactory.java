package com.huawei.fossbot.dependency.factory;


import com.huawei.fossbot.dependency.bean.AnalyzerType;
import com.huawei.fossbot.dependency.parser.DependencyParser;
import com.huawei.fossbot.dependency.parser.java.MParser;
import com.huawei.fossbot.dependency.util.RepoPathUtil;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * @author t30002128 tianhaolin@huawei.com
 * @since 2020/05/25
 */
public class ParserFactory {

    private static Map<AnalyzerType, Class<? extends DependencyParser>> parserClassMap = new HashMap<>();

    static {
        parserClassMap.put(AnalyzerType.MAVEN, MParser.class);
    }

    public static DependencyParser newInstance(AnalyzerType type) {
        return newInstance(type, null, null);
    }

    public static DependencyParser newInstance(AnalyzerType type, String localRepo, String remoteRepo) {
        try {
            if (localRepo == null) {
                localRepo = RepoPathUtil.getRepoPath(type);
            }
            if (remoteRepo == null) {
                remoteRepo = RepoPathUtil.getRepoPath(type);
            }
            Class<? extends DependencyParser> parserClass = parserClassMap.get(type);

            Constructor<? extends DependencyParser> constructor = parserClass.getConstructor(String.class, String.class);
            return constructor.newInstance(localRepo, remoteRepo);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
