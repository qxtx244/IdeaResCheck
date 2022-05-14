package com.qxtx.idea.gradle.plugin.rescheck.extension

import org.gradle.api.Action

/**
 * @author QXTX-WORK
 * <p><b>Create Date</b></p> 2022/3/30 23:25
 * <p><b>Description</b></p> 插件的扩展类
 * @see com.qxtx.idea.gradle.plugin.rescheck.ResCheck
 */
class ResCheckExtension {

    /** 是否将结果输出到文件，文件目录路径默认为目标module的根目录/插件名称/ */
    boolean logFile = false

    /** 资源前缀检查扩展对象 */
    PrefixExtension prefixCheck = null

    /** 重名资源检查扩展对象 */
    DupCheckExtension dupCheck = null

    def logFile(boolean b) {
        logFile = b
    }

    def prefixCheck(Action<PrefixExtension> action) {
        prefixCheck = new PrefixExtension()
        action.execute(prefixCheck)
    }

    def dupCheck(Action<DupCheckExtension> action) {
        dupCheck = new DupCheckExtension()
        action.execute(dupCheck)
    }
}
