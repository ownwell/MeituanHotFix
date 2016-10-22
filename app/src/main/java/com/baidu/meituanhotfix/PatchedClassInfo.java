package com.baidu.meituanhotfix;

/**
 * Created by lixingyun on 16/9/29.
 */

public class PatchedClassInfo {

    String clazz;
    String pathRedicIMpl;

    public PatchedClassInfo(String clazz, String pathRedicIMpl) {
        this.clazz = clazz;
        this.pathRedicIMpl = pathRedicIMpl;
    }

    public String getClazz() {
        return clazz;
    }

    public String getPathRedicIMpl() {
        return pathRedicIMpl;
    }
}
