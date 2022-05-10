**IdeaResCheck**
=============

## **概述**
gradle插件，主要用于项目中资源的检查。检查会在sync阶段进行，而不需要等待漫长的rebuild过程。  
甚至，gradle不会提示一些非致命的重名文件行为，而是自动覆盖重名资源，导致打包成apk后，运行过程中出现隐患。  
因此，仅靠android插件自带resourcesPrefix true配置，作用十分有限（仅在打开目标资源文件时，才会提示，不会主动提示或打断编译）。  
插件现有功能如下：
+ 在sync阶段，对项目中资源的名称前缀检查
+ 在sync阶段，对项目中重名资源的检查

`注意，当前版本不支持远程依赖和aar文件中的资源检查`

## **使用**

### **· 导入插件**
  1. 打开AS右侧Gradle面板，依次展开IdeaResCheck/ResCheck/Tasks；
  2. 展开build任务组，双击执行“jar”选项，jar文件输出目录：IdeaResCheck/ResCheck/jars
  3. 在目标工程的根build.gradle中添加：
     ```
     buildScript {
        dependencies {
            classpath files('插件jar的路径') //依赖插件
        }
     }
     ```

### **· 使插件工作**
1. **插件配置**  
   在目标module的build.gradle中，添加以下代码  
   添加插件：
   ```
   apply plugin: 'res-check'
   ```
   启用和配置插件：
   ```
   ResCheck {
        logFile true    //是否将结果输出到文件，文件目录路径默认为目标module的根目录/插件名称/。默认为false
        
        prefixCheck {   //配置资源名称前缀检查     
            force true  //当检查未通过时，是否打断gradle工作。默认为false
            targetPrefix 'xxx'   //指定一个名称前缀。如果未指定一个有效的字符串，则此功能将不工作
            recursive true       //是否递归检查子module。默认为false
        }
   
        dupCheck {      //配置重名资源检查
            force true  //当检查未通过时，是否打断gradle工作。默认为false
            
            /*
             * 目标module依赖的其它module，以project path表示，可配置多个。默认为空。     
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
    执行完成后，将在"Build"面板中输出结果，如已启用日志文件，则结果输出到本地文件

## **demo**
demo工程将演示使用ResCheck插件的效果，在demo/build.gradle中添加ResCheck配置。
  + 强制要求资源名称前缀，包括资源文件名称和xml字段名称（不检查assets和raw目录）
  + sync阶段的重名资源检查
  + 检查结果输出到文件（位置：目标module的根目录\ResCheck_LOG）

`只有顺利通过检查，或者将插件的“force”参数都置为false，gradle才能顺利完成sync`
