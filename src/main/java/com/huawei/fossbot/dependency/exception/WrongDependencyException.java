package com.huawei.fossbot.dependency.exception;

public class WrongDependencyException extends Exception {
    private static String message = " :profile or dependency must end with pom.xml or .pom";
    public WrongDependencyException(String msg){
        super(msg + message);
    }
}
