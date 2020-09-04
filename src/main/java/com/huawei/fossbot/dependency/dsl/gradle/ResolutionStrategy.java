package com.huawei.fossbot.dependency.dsl.gradle;

public enum ResolutionStrategy{
    BUCKET(false,false),
    RESOLVE(true,false),
    EXPOSED(false,true),
    NOUSE(true,true);

    boolean canBeResolved;
    boolean canBeConsumed;
    private ResolutionStrategy(boolean canBeResolved,boolean canBeConsumed){
        this.canBeResolved = canBeResolved;
        this.canBeConsumed = canBeConsumed;
    }
}