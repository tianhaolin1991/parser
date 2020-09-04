package com.huawei.fossbot.dependency.dsl.gradle;

import com.huawei.fossbot.dependency.bean.RepoConstant;
import com.huawei.fossbot.dependency.util.RepoPathUtil;
import java.util.ArrayList;
import java.util.List;

public class RepositoryHandler {
    private String mavenLocal;
    private String localRepo = RepoPathUtil.getGradleLocalRepo();
    private List<String> remoteRepo = new ArrayList<>();

    public void mavenLocal() {
        String settings = RepoPathUtil.getMavenSettingPath();
        this.mavenLocal = RepoPathUtil.getMavenLocalRepo(settings);
    }

    public String getLocalRepo(){
        return this.localRepo;
    }
    public void jCenter(){
        remoteRepo.add(RepoConstant.J_CENTER);
    }

    public void mavenCentral(){
        remoteRepo.add(RepoConstant.MAVEN_CENTRAL);
    }

    public void addRemoteRepo(String repo){
        remoteRepo.add(repo);
    }

    public String getMavenLocal() {
        return this.mavenLocal;
    }

    public List<String> getRemoteRepo(){
        return this.remoteRepo;
    }
}
