# 编译环境

- JDK: 17
- Gradle: 7.5
- SpringBoot: 2.5.12
- CheckStyle: 8.39
- Vue: 2

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

我建议是本地跑个 [docker for windows](https://docs.docker.com/desktop/install/windows-install/))，装好mariadb方便开发

IDEA的checkstyle插件配置，安装好插件`CheckStyle-IDEA`，进行配置

配置插件过程中版本选择`8.39`，还会让填变量值，填入如下值

```text
checkstyle-suppressions.xml
checkstyle-xpath-suppressions.xml
```

commit之前，用checkstyle检查下代码，确认没有问题后再commit。

编译打包，告警可以忽视

```text
.\gradlew.bat clean bootJar
```

git modules 初始化，需要初始化是由于主题使用的是git module关联的另一个仓库

```text
git submodule init
git submodule update
```

## 代码格式化

`Setting` => `Editor` => `Code Style` => `Java` 

=> `Scheme` 选择 `Project`

选择右边设置，导入checkstyle文件

最后OK保存