 package com.qxtx.idea.gradle.plugin.rescheck.extension

 /**
  * 资源名称前缀检查功能的扩展类
  */
 class PrefixExtension {

     /** 是否在检查未通过时，打断gradle编译，默认为false */
     boolean force = false

     /** 名称前缀 */
     String targetPrefix = null

     /** 是否检查子project，默认为false */
     boolean recursive = false

     def force(boolean b) {
         force = b
     }

     def targetPrefix(String s) {
         targetPrefix = s
     }

     def recursive(boolean b) {
         recursive = b
     }
 }