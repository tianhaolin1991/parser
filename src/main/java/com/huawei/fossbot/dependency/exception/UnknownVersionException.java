package com.huawei.fossbot.dependency.exception;

/**
 * @author t30002128
 * @since 2020/05/30
 */
public class UnknownVersionException extends DependencyParserException {
    public UnknownVersionException(String msg) {
        super(msg);
    }
}
