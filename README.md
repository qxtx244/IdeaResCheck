**ResCheck**
=============

## **概述**
gradle插件，主要用于项目中资源的检查。检查会在sync阶段进行，而不需要等待漫长的rebuild过程
（甚至gradle不会提示一些非致命的重名文件行为，而是自动覆盖重名资源）。主要功能如下：
+ 对项目中资源的名称前缀检查
+ 对项目中重名资源的检查

`注意，当前版本不支持远程依赖和aar文件中的资源检查`

## **使用**
### 1. **发布/打包插件**
打开AS右侧Gradle面板，依次展开ResCheck/ResCheck/Tasks
+ **方案一**：打包成jar包
  > 1. 展开build任务组，双击执行“jar”选项
  > 2. 生成的jar文件位于项目build/jars/ResCheck.jar
+ **方案二**：发布到本地maven仓库
  > 1. 展开”build“任务组，双击执行“assemble”选项，然后再展开”upload“任务组，双击执行”uploadArchives“选项，可发布发到本地windows用户目录/.mavenCentral。
        如果还想打包库，执行“extension”任务组中的“uploadArchives”即可，zip包输出到目标module/build目录下。

### 2. **导入插件**
+ **jar包形式**
  ```
  dependencies {
     classpath files('插件jar的绝对路径')
  }
  ```
+ **依赖形式**  
  在目标工程根project的build.gradle中添加：
  ```
  buildScript {
      repositories {
          maven {       
            //本地maven仓库位于 windows用户目录\.mavenCentral        
            url uri("${System.getProperties().getProperty('user.home')}/.mavenCentral")
          }
      }
      dependencies {
          classpath 'com.qxtx.idea.gradle.plugin.rescheck:ResCheck:1.0.0'
      }
  }
  ```

### 3. **使插件工作**
1. **插件配置**  
  在目标module的build.gradle中，添加以下代码
    ```
   //添加插件
   apply plugin: 'res-check'
   
   //启用插件
   ResCheck {
        //是否将结果输出到文件，文件目录路径默认为目标module的根目录/插件名称/。默认为false
        logFile true
        
        //启用资源名称前缀检查     
        prefixCheck {
            //当检查未通过时，是否打断gradle工作。默认为false
            force true
            //指定一个名称前缀。如果未指定一个有效的字符串，则此功能将不工作
            targetPrefix 'xxx'         
            //是否递归检查子module。默认为false
            recursive true                  
        }
   
        dupCheck {
            //当检查未通过时，是否打断gradle工作。默认为false
            force true
            /*
             * 目标module依赖的其它module，以project path表示，可配置多个。默认为空。
             *          
             * 示例1：在gradle工程"Sample"中，主module依赖moduleA。A的绝对路径表示为xxx/Sample/A，project.path=':A'，则配置：
             *        deptModules ':A'        
             * 示例2：在gradle工程"Sample"中，主module依赖moduleA。A的绝对路径表示为xxx/Sample/logic/A，project.path=':logic:A'，则配置：
             *        deptModules ':logic:A'       
             * 示例3：在gradle工程"Sample"中，主module依赖moduleA，module B。A的绝对路径表示为xxx/Sample/A，project.path=':logic:A'，
             *     B的绝对路径表示为xxx/Sample/B，project.path=':B'，则配置：
             *        deptModules ':A', ':B'    
             */          
            deptModules ':A', ':xxx:B', ...
        }
   }
    ```
2. **执行sync**
    执行完成后，将在"Build"面板中输出结果，如启用日志文件，则将结果输出到本地文件

### **4. demo工程**
demo工程将演示使用ResCheck插件的效果，在demo/build.gradle中添加ResCheck配置。
  + 强制要求资源名称前缀，包括资源文件名称和xml字段名称（不检查assets和raw目录）
  + sync阶段的重名资源检查
  + 检查结果输出到文件（位置：目标module的根目录\ResCheck_LOG）

`只有符合要求，或者将“force”参数置为false，gradle才能顺利完成sync`
