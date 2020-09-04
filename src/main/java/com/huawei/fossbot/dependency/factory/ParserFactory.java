package com.huawei.fossbot.dependency.factory;

import com.huawei.fossbot.dependency.bean.Type;
import com.huawei.fossbot.dependency.exception.DependencyParserException;
import com.huawei.fossbot.dependency.factory.builder.parser.MavenParserBuilder;
import com.huawei.fossbot.dependency.factory.builder.parser.ParserBuilder;
import com.huawei.fossbot.dependency.parser.DependencyParser;
import java.util.HashMap;
import java.util.Map;

/**
 * @author t30002128 tianhaolin@huawei.com
 * @since 2020/05/25
 */
public class ParserFactory {
    private static Map<Type, ParserBuilder> builderMap = new HashMap<>();

    static {
        builderMap.put(Type.MAVEN, new MavenParserBuilder());
    }


    /**
     * @param type 解析类型
     * @param profile 构建文件
     * @param useGlobalSettings 是否使用项目文件下的settings文件
     * @return 返回对应的DependencyParser
     * @throws DependencyParserException 项目构建类型不支持异常
     */
    public static DependencyParser newInstance(Type type,String profile,boolean useGlobalSettings) throws DependencyParserException {
        if(builderMap.containsKey(type)){
            ParserBuilder parserBuilder = builderMap.get(type);
            return parserBuilder.build(profile,useGlobalSettings);
        }
        throw new DependencyParserException("Type:"+type+" unsupported yet");
    }
}
