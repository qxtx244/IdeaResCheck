package com.qxtx.idea.gradle.plugin.rescheck.impl

import com.qxtx.idea.gradle.plugin.rescheck.extension.ResCheckExtension
import com.qxtx.idea.gradle.plugin.rescheck.Consts
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 抽象类，定义模板方法{@link #start}
 */
abstract class Base implements Plugin<Project> {

    /** 实现类实现此抽象方法，完成功能 */
    abstract void start(Project target)

    protected final def EXT_NAME = Consts.EXT_NAME
    protected final def TAG = Consts.TAG
    protected ResCheckExtension extension = null

    @Override
    void apply(Project target) {
        def extensions = target.getExtensions()
        extension = extensions.findByName(EXT_NAME)
        if (extension == null) extension = extensions.create(EXT_NAME, ResCheckExtension)

        start(target)
    }
}