package com.huawei.fossbot.dependency.parser;


import com.huawei.fossbot.dependency.bean.Artifact;

import java.util.List;

/**
 * 自研解析器接口
 */
public interface DependencyParser {

     List<Artifact> parse(String profile);

}
