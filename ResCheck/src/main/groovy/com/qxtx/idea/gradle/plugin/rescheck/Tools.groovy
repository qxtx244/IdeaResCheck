package com.qxtx.idea.gradle.plugin.rescheck

import org.gradle.api.Project

/**
 * 仅适用于android library，必须在android{}执行完成后调用
 */
class AndroidTool {

    /**
     * 找到包括子project的所有资源目录
     * @return {@link ArrayList}，元素为资源目录绝对路径
     */
    def static findAllResDir(Project project) throws Exception {
        if (!Tools.isAndroidModule(project)) return null

        def androidExt = project.android
        if (androidExt == null) return null
        def sourceSets = androidExt.sourceSets
        if (sourceSets == null) return null

        def sets = new ArrayList<String>()
        sourceSets.each {
            def name = it.name
            if (name == 'androidTest'|| name == 'test') return

            //只找存在的资源目录
            it.res.source.each { path ->
                def absPath = "${project.projectDir.absolutePath}/$path"
                if (new File(absPath).exists()) sets.add(absPath)
            }
        }

        sets
    }

    /**
     * 找到project的flavor和dimension
     * @retrue {@link HashMap}，键为flavor名称，值为dimension
     */
    def static findAllFlavor(Project project) {
        if (!Tools.isAndroidModule(project)) return null

        def result = new HashMap<String, String>()
        def androidExtension = project.android
        if (androidExtension == null) return null
        def flavors = androidExtension.productFlavors
        if (flavors == null) return null

        flavors.each { it -> result.put(it.name, it.dimension) }

        result
    }

    /**
     * 判断目标project是否为android application
     * @param project
     * @return true表示project属于android application，否则为android library
     */
    def static isApplication(Project project) {
        def manager = project.pluginManager
        if (manager == null) return false
        manager.findPlugin("com.android.application") != null
    }

    /**
     * 判断目标project是否为android application
     * @param project
     * @return true表示project属于android library，false表示不属于android library，但也不一定是android application
     */
    def static isLibrary(Project project) {
        project.pluginManager.findPlugin("com.android.library") != null
    }
}

/**
 * 判断目标project是否为android module
 * @param project
 * @return
 */
def static isAndroidModule(Project project) {
    def pluginMgr = project.pluginManager
    def b = (pluginMgr.findPlugin("com.android.application") != null)\
                || (pluginMgr.findPlugin("com.android.library") != null)
    //if (!b) println("project[${project.path}]不是android module")

    b
}