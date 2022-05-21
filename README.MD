# 介绍
娱乐(战略)用泛用机器人——伊卡洛斯。
基于SpringBoot3 + Vue3。

# 编译环境
- JDK: 17
- Gradle: 7.4
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

本地运行，在VM参数里加上如下，
复制`application-dev.yaml`为`application-local.yaml`
```text
-Dspring.profiles.active=local
```

IDEA的checkstyle插件配置，安装好插件`CheckStyle-IDEA`，进行配置

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