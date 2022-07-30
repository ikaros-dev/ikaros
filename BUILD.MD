# 编译环境

- JDK: 17
- Gradle: 7.5
- SpringBoot: 3.0.0
- CheckStyle: 8.39
- Vue: 3

执行gradle任务前，默认会走一遍单元测试， 如需跳过，加上`-x test` 即可。

建议先使用`java -version`检查下jdk版本，如本地JDK版本不是17编译会报错。

安装git-hooks，为了在commit之前进行代码检查

```
.\gradlew.bat clean updateGitHooks -x test
```

跳过测试编译(Windows)

```
.\gradlew.bat clean build -x test
```

Mac或者Linux

```
./gradlew clean build -x test
```

本地运行，复制`config/resource/application-dev.yaml`到`src/main/resource/application-dev.yaml`
, 可以根据需要酌情修改

再在VM参数里加上如下:
```text
-Dspring.profiles.active=dev
```

IDEA的checkstyle插件配置，安装好插件`CheckStyle-IDEA`，进行配置

版本选择 8.39

配置插件过程中会让填变量值，填入如下值

```text
checkstyle-suppressions.xml
checkstyle-xpath-suppressions.xml
```

commit之前，用checkstyle检查下代码，确认没有问题后再commit。

编译打包，告警可以忽视

```text
.\gradlew.bat clean bootJar
```