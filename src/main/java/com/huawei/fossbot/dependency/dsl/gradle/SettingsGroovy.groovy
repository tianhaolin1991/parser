package com.huawei.fossbot.dependency.dsl.gradle

abstract class SettingsGroovy extends Script {

    def settings

    def initSettings(File settingPath) {
        settings = new Settings(settingPath)
    }

    def includeFlat(String... projectNames) {
        settings.includeFlat(projectNames)
    }

    def include(String... projectNames) {
        settings.include(projectNames)
    }

    Object getProperty(String propertyName) {
        try {
            return super.getProperty(propertyName)
        } catch (MissingPropertyException e) {
            try {
                return settings.metaClass.getProperty(settings,propertyName)
            } catch (MissingPropertyException mpe) {
                return null;
            }
        }
    }

    Object invokeMethod(String name, Object args) {
        try{
            super.invokeMethod(name,args)
        }catch (MissingMethodException mme){
            return null
        }
    }
}

