package com.qxtx.idea.gradle.plugin.rescheck.impl

import com.qxtx.idea.gradle.plugin.rescheck.Consts
import com.qxtx.idea.gradle.plugin.rescheck.Tools
import com.qxtx.idea.gradle.plugin.rescheck.extension.DupCheckExtension
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import com.qxtx.idea.gradle.plugin.rescheck.AndroidTool
import org.gradle.api.logging.Logger

import java.text.SimpleDateFormat

/**
 * 重复资源检查的实现
 */
class DupCheckImpl extends Base {

    private final String SUB_TAG = "DUPLICATE_CHECK"

    /**
     * 重复的资源集
     * 相同module且相同dimension的资源目录，不做比较
     */
    private final HashMap<String, HashSet<String>> dupMap = new HashMap<>()

    /**
     * 检索的资源文件集
     * K：flavor组合列表（如['main'], ['ax', 'cx']）
     * V：文件/资源路径列表（src/main/layout/xxx.xml或src/main/values/strings.xml#name1）
     */
    private final HashMap<String, HashSet<String>> resMap = new HashMap<>()

    private DupCheckExtension ext = null

    private Project target = null

    @Override
    void start(Project target) {
        this.target = target
        target.afterEvaluate {
            if (!Tools.isAndroidModule(target)) return

            ext = extension.dupCheck
            if (ext == null) return

            def logFileEnable = extension.logFile
            def needInterrupt = ext.force
            def modulePaths = ext.deptModules

            if (modulePaths == null) modulePaths = new ArrayList<String>()
            modulePaths.add(target.path)
            def moduleCount = modulePaths.size()

            //主project的所有flavor组合，如['ax', 'cx']和['bx', 'cx']
            //  让无flavor的library也能成功完成流程
            def primaryFlavorMixList = new ArrayList<String>()

            //遍历全部project，找到匹配名称的project
            target.rootProject.allprojects {
                Project proj = project
                proj.gradle.buildFinished {
                    if (!modulePaths.contains(proj.path)) return

                    if (Tools.isAndroidModule(proj)) {
                        def mainFlavor = 'main'
                        def androidExt = proj.android
                        def mainSet = androidExt.sourceSets.find { it.name == mainFlavor }
                        def mainSources = mainSet.res.source
                        //所有的main资源入口目录（如/xxx/src/main/res，/yyy/src/main/res，/zzz/src/main2/res等）
                        def mainDirSets = new HashSet<String>()
                        //可能没有资源目录
                        if (mainSources != null) {
                            mainSources.each { mainDir -> mainDirSets.add("${proj.projectDir.absolutePath}/$mainDir") }
                        }

                        //收集。map中的元素为【tag:srcDirs】，如'['ax', 'cx']':资源入口目录集
                        HashMap<String, HashSet<String>> map = new HashMap<>()
                        def flavorNameList = new ArrayList<String>()

                        //variants不会为空，因为至少有一个”debug“的buildType
                        def variants = AndroidTool.isApplication(proj)
                                ? androidExt.applicationVariants : androidExt.libraryVariants
                        variants.each { variant ->
                            def varSources = variant.variantData.variantSources

                            //只需要buildType为”debug“的项，避免重复工作
                            if (varSources.buildTypeSourceProvider.name != 'debug') return

                            def flavorSourceProvider = varSources.flavorSourceProviders

                            //如果有，则将main的资源目录全部放入每一个element中，如果没有，则创造和target project一样多的flavor组合，放进去
                            if (flavorSourceProvider == null) return

                            def flavorDirSets = new HashSet<String>()
                            flavorSourceProvider.each { provider ->
                                flavorNameList.add(provider.name)
                                def subSources = provider.res.source
                                if (subSources != null) {
                                    subSources.each { flavorDir -> flavorDirSets.add("${proj.projectDir.absolutePath}/$flavorDir") }
                                }
                            }

                            if (!flavorDirSets.isEmpty()) {
                                def flavorMixName = flavorNameList.toListString()
                                //保存主project的flavor组合
                                if (proj.path == target.path) {
                                    primaryFlavorMixList.add(flavorMixName)
                                }

                                map.put(flavorMixName, flavorDirSets)
                            }
                            flavorNameList.clear()
                        }

                        if (map.isEmpty()) {
                            if (primaryFlavorMixList.isEmpty()) {
                                //如果连主project的flavor组合也没有，则说明只有一个main，甚至没有任何资源目录
                                if (!mainDirSets.isEmpty()) map.put("['main']", mainDirSets)
                            } else {
                                //println "project[${proj.path}]没有flavor组合，只有main，手动添加所有flavor组合，并塞入main资源目录"
                                primaryFlavorMixList.each { tag -> map.put(tag, mainDirSets) }
                            }
                        } else {
                            //println "往每一个flavor组合数据集中塞入main资源目录..."
                            map.each {tag, srcDirSet -> srcDirSet.addAll(mainDirSets) }
                        }

                        //开始分析flavor组合中所有资源目录的资源
                        map.each {tag, srcDirSet ->
                            //println "${TAG} 解析${proj.path}: tag=${tag}, 资源入口目录=${srcDirSet.toListString()}"
                            parseAllResource(tag, srcDirSet)
                        }
                    }

                    if (--moduleCount > 0) return

                    onCheckFinished(logFileEnable, needInterrupt)
                }
            }
        }
    }

    /** 遍历资源目录 */
    private void parseAllResource(String tag, HashSet<String> resDirSets) {
        resDirSets.each {dir ->
            File f = new File(dir)
            if (!f.exists()) return
            def resFiles = f.listFiles()
            if (resFiles == null) return

            //解析文件名、values中的资源名
            resFiles.each {file ->
                if (file.name.startsWith('values')) {
                    parseValuesDir(dir, tag, file)
                } else {
                    if (file.isDirectory()) {
                        parseNonValuesDir(dir, tag, file)
                    } else if (file.isFile()) {
                        parseFileName(dir, tag, file)
                    }
                }
            }
        }
    }

    private void parseValuesDir(String resDir, String tag, File file) {
        def subFiles = file.listFiles()
        if (subFiles == null) return

        //检查xml中的资源名
        subFiles.each {it ->
            def resName = it.name
            if (!resName.endsWith('.xml')) return

            GPathResult result = new XmlSlurper().parse(it)
            result.'**'.each { node ->
                String name = node.@name
                if (name.isEmpty()) return

                def path = it.absolutePath

                def resDirSets = resMap.get(tag)
                if (resDirSets ==  null) {
                    resDirSets = new HashSet<String>()
                    resMap.put(tag, resDirSets)
                }

                //取【文件名@字段名】做资源重名检查
                String uri = "${path.substring(resDir.length() + 1)}@$name"
                def truthPath = "${path}@$name"

                def duplicatePath = resDirSets.find {it.endsWith(uri) }
                if (duplicatePath == null) {
                    resDirSets.add(truthPath)
                } else {
                    //println "重复资源字段! ${truthPath}, $duplicatePath"
                    def sets = dupMap.get(uri)
                    if (sets == null) {
                        sets = new HashSet<String>()
                        dupMap.put(uri, sets)
                    }
                    sets.add(duplicatePath)
                    sets.add(truthPath)
                }
            }
        }
    }

    private void parseNonValuesDir(String resDir, String tag, File file) {
        def files = file.listFiles()
        if (files != null) {
            files.each { parseFileName(resDir, tag, it) }
        }
    }

    private void parseFileName(String resDir, String tag, File file) {
        def path = file.absolutePath
        def cmpName = path.substring(resDir.length() + 1)

        def resSets = resMap.get(tag)
        if (resSets == null) {
            resSets = new HashSet<String>()
            resMap.put(tag, resSets)
        }

        def duplicatePath = resSets.find { it.endsWith(cmpName) }
        if (duplicatePath == null) {
            resSets.add(path)
        } else {
            //println "重复资源文件! $path, $duplicatePath"
            def sets = dupMap.get(cmpName)
            if (sets == null) {
                sets = new HashSet<String>()
                dupMap.put(cmpName, sets)
            }
            sets.add(duplicatePath)
            sets.add(path)
        }
    }

    /**
     * 检索完成，开始整理结果（格式化输出控制台日志，输出到文件，打断gradle等）
     * @param logFileEnable 是否输出结果到文件
     * @param needInterrupt 是否打断gradle流程
     * @see #start(Project)
     */
    private void onCheckFinished(boolean logFileEnable, boolean needInterrupt) {
        //println "${TAG} 所有project的资源收集完成"
        def resultInfo = peekResult()

        String log
        if (resultInfo.isEmpty()) {
            log = "\n${TAG}#${SUB_TAG} 检查通过，未发现重名资源"
        } else {
            log = "\n========== ${TAG}#${SUB_TAG} 未通过检查 ============\n" +
                    resultInfo +
                    "===================================================\n\n"
        }

        if (logFileEnable) {
            def parentDir = "${target.projectDir.absolutePath}/${Consts.LOG_DIR}/${SUB_TAG}/"
            def file = new File(parentDir + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()))
            //如果文件正在被占用（比如正在被其它应用打开），则将删除失败
            if (file.parentFile.exists()) {
                try {
                    target.delete(file.parentFile)
                } catch (Exception e) {
                    println "无法输出日志文件，因为旧日志文件（${file.parentFile}）删除失败，请检查文件是否正在被使用！"
                    throw e
                }
            }
            file.parentFile.mkdirs()
            def time = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date())
            file.write("${time}\n${log}\n")
        }

        resMap.clear()
        dupMap.clear()

        if (needInterrupt) {
            target.logger.error log
            throw new Exception("${TAG}#${SUB_TAG} 检查未通过")
        } else {
            println log
        }
    }

    private def peekResult() {
        def sb = new StringBuilder()
        dupMap.each {k, v ->
            sb.append("目标=[${k.substring(k.lastIndexOf('\\') + 1)}], 位置=[\n")
            v.each {
                def s
                if (it.contains('->'))
                    s = it.substring(0, it.lastIndexOf('->'))
                else
                    s = it.substring(0, it.lastIndexOf('\\'))
                sb.append("\t${s}\n")
            }
            sb.append(']\n')
        }

        sb.toString()
    }
}