package com.qxtx.idea.gradle.plugin.rescheck.extension

/**
 * 重复资源名称检查的扩展
 */
class DupCheckExtension {

    /** 当检查未通过时，是否打断gradle工作。true为打断，false为不打断，默认为false */
    boolean force = false

    /**
     * 目标module依赖的其它module的"path"，即被依赖module的project.path的值
     * 示例1：在gradle工程"Sample"中，主module依赖moduleA。A的绝对路径表示为xxx/Sample/A，project.path=':A'，则配置：
     *  deptModules ':A'
     * 示例2：在gradle工程"Sample"中，主module依赖moduleA。A的绝对路径表示为xxx/Sample/logic/A，project.path=':logic:A'，则配置：
     *  deptModules ':logic:A'
     * 示例3：在gradle工程"Sample"中，主module依赖moduleA，module B。A的绝对路径表示为xxx/Sample/A，project.path=':logic:A'，
     *        B的绝对路径表示为xxx/Sample/B，project.path=':B'，则配置：
     *  deptModules ':A', ':B'
     */
    ArrayList<String> deptModules = new ArrayList<>()

    //TODO: 后续实现
//    /**
//     * 忽略的路径列表，使用相对（工程目录的）路径
//     * 示例1：希望检查时忽略moduleA中的【src/main/res/layout】目录，A的path为【':A'】
//     *  excludeDirs [':A':'/src/main/res/layout']
//     * 示例2：希望检查时忽略moduleA中的【src/main/res/layout】和【src/flavor1/res/layout】目录，A的path为【':A'】
//     *  excludeDirs ['A': 'src/main/res/layout', 'A':'src/flavor1/res/layout']
//     */
//    HashMap<String, String> excludeDirs = new ArrayList<>()
//
//    def excludeDirs(HashMap<String, String> map) {
//        excludeDirs.clear()
//        excludeDirs.putAll(map)
//    }

    def force(boolean b) {
        force = b
    }

    def deptModules(String... array) {
        deptModules.clear()
        if (array != null) deptModules.addAll(array)
    }
}