# 文档状态
WIP
# 编译环境

- JDK: 17
- Gradle: 8.1
- SpringBoot: 3.0.1
- CheckStyle: 9.3
- Vue: 3

执行gradle任务前，默认会走一遍单元测试， 如需跳过，加上`-x test` 即可。

建议先使用`java -version`检查下jdk版本，如本地JDK版本不是17编译会报错。

跳过测试编译(Windows)

```
.\gradlew.bat clean build -x test
```

Mac或者Linux

```
./gradlew clean build -x test
```

本地运行，复制`config/example/resource/application-local.yaml`到`src/main/resource/application-local.yaml`
, 可以根据需要酌情修改，

我建议是本地跑个 [docker for windows](https://docs.docker.com/desktop/install/windows-install/)，装好mariadb方便开发

IDEA的checkstyle插件配置，安装好插件`CheckStyle-IDEA`，进行配置

配置插件过程中版本选择`9.3`，
扫描范围包括测试代码，描述可以填比如`Ikaros Checks`，
还会让填变量值，填入如下值

```text
checkstyle-suppressions.xml
checkstyle-xpath-suppressions.xml
```
下一步直到完成后，勾选这个配置，确定保存。

commit之前，用checkstyle检查下代码，确认没有问题后再commit。
如果没过checkstyle检查的，GitHub的CI过不了。

编译打包，告警可以忽视

```text
.\gradlew.bat clean bootJar
```

## 代码格式化

`Setting` => `Editor` => `Code Style` => `Java`

=> `Scheme` 选择 `Project`

选择右边设置，导入checkstyle文件

最后OK保存

## 本地开发
在`IkarosApplication`的运行配置里，将`Active profiles` 配置成：`dev,win`