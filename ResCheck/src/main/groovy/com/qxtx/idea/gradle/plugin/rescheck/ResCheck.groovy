package com.qxtx.idea.gradle.plugin.rescheck

import com.qxtx.idea.gradle.plugin.rescheck.impl.Base
import com.qxtx.idea.gradle.plugin.rescheck.impl.DupCheckImpl
import com.qxtx.idea.gradle.plugin.rescheck.impl.PrefixCheckImpl
import org.gradle.api.Project

/**
 * @author QXTX-WIN
 * createDate 2022/3/30 23:20
 * Description gradle插件
 * <pre>
 *  实现对项目中的资源名称的检查
 * </pre>
 */
class ResCheck extends Base {

    /** 检查资源名称前缀的配置对象 */
    private final def prefixCheck = new PrefixCheckImpl()
    /** 检查重名资源的配置对象 */
    private final def dupCheck = new DupCheckImpl()

    @Override
    void start(Project target) {
        prefixCheck.apply(target)
        dupCheck.apply(target)
    }
}