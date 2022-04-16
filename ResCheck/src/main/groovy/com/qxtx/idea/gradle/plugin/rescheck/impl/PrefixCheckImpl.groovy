package com.qxtx.idea.gradle.plugin.rescheck.impl

import com.qxtx.idea.gradle.plugin.rescheck.Consts
import com.qxtx.idea.gradle.plugin.rescheck.Tools
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import com.qxtx.idea.gradle.plugin.rescheck.AndroidTool

import java.text.SimpleDateFormat

/**
 * 资源名称前缀的检查实现
 */
class PrefixCheckImpl extends Base {

    private final String SUB_TAG = "PREFIX_CHECK"

    /**
     * key: module名称
     * value: 未通过检查的资源绝对路径
     */
    private final ArrayList<String> resultList = new ArrayList<>()
    private int projectCount = 0

    @Override
    void start(Project target) {
        target.afterEvaluate {
            def ext = extension.prefixCheck
            if (ext == null) return

            def prefix = ext.targetPrefix
            def needInterrupt = ext.force
            def isRecursive = ext.recursive
            def logFileEnable = extension.logFile

            if (prefix == null || prefix.trim().isEmpty()) {
                //println "${TAG} 无法检查[${target.path}]的资源名称前缀。请在${target.path}\\build.gradle中正确配置'targetPrefix'字段后，重新sync项目。"
                return
            }

            //包括target自己
            projectCount = target.allprojects.size()
            target.allprojects {
                if (!isRecursive && project.path != target.path) return

                project.afterEvaluate {
                    //Project project
                    if (!Tools.isAndroidModule(project)) {
                        println "${TAG} 无法解析${project.path}，因为它不是android library"
                    } else {
                        //获取所有资源目录
                        def sets = AndroidTool.findAllResDir(project)
                        sets.each {checkResPrefix(project.name, it, prefix)}
                    }

                    projectCount--
                    if (projectCount > 0) return

                    def log = null
                    if (resultList.isEmpty()) {
                        log = "\n${TAG}[${SUB_TAG}] 检查通过，未发现异常的资源命名\n\n"
                    } else {
                        def sb = new StringBuilder()
                        resultList.each {sb.append("${it}\n") }
                        resultList.clear()
                        log = "\n============= ${TAG}#${SUB_TAG} 未通过检查 ===================\n" +
                                "${sb.toString()}" +
                                "=============================================================\n\n"
                    }

                    if (logFileEnable) {
                        def parentDir = "${target.projectDir.absolutePath}/${Consts.LOG_DIR}/${SUB_TAG}/"
                        def file = new File(parentDir + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()))
                        if (file.parentFile.exists()) project.delete(file.parentFile)
                        file.parentFile.mkdirs()
                        def time = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date())
                        file.write("${time}\n${log}\n")
                    }

                    if (needInterrupt) {
                        target.logger.error log
                        throw new Exception("${TAG}[${SUB_TAG}] 检查未通过")
                    } else {
                        println log
                    }
                }
            }
        }
    }

    private void checkResPrefix(def projName, def dir, def prefix) {
        try {
            if (dir == null || dir.trim().isEmpty()) return

            //println "${TAG} 检查资源目录：${dir}，指定资源名称前缀：$prefix"
            File f = new File(dir)
            if (!f.exists()) return
            def resFiles = f.listFiles()

            //解析文件名、values中的资源名，排除raw目录
            resFiles.each {
                def name = it.name
                //不检查raw文件
                if (name.startsWith('raw')) return
                if (name.startsWith('values')) {
                    checkValuesDir(projName, it, prefix)
                } else {
                    if (it.isDirectory()) {
                        checkDir(projName, it, prefix)
                    } else if (it.isFile()) {
                        checkFile(projName, it, prefix)
                    }
                }
            }
        } catch (Exception e) {
            println("${TAG} 检查[${projName}]的${dir}目录过程中发生异常：${e.getMessage()}")
        }
    }

    private void checkValuesDir(def projName, File file, String prefix) {
        //检查xml中的资源名
        file.listFiles().each {
            def resName = it.name
            if (!resName.endsWith('.xml')) return
            GPathResult result = new XmlSlurper().parse(it)
            result.'**'.each { node ->
                def name = node.@name
                if (!name.isEmpty() && !name.toString().startsWith(prefix)) {
                    resultList.add("[$projName]: 资源名=$name, 位置=${it.absolutePath}")
                }
            }
        }
    }

    private void checkDir(def projName, File file, String prefix) {
        file.listFiles().each { checkFile(projName, it, prefix) }
    }

    private void checkFile(def projName, File file, String prefix) {
        if (!file.name.startsWith(prefix)) {
            def s = file.absolutePath
            resultList.add("[$projName]: 文件名=${file.name}, 位置=${s.substring(0, s.lastIndexOf('\\') + 1)}")
        }
    }
}