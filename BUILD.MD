# 编译环境

- JDK: 17
- Gradle: 8.1
- SpringBoot: 3.0.1
- CheckStyle: 9.3
- Vue: 3
- IDE: IntelliJ IDEA

您可以通过`git`拉取代码库
```shell
git clone https://github.com/ikaros-dev/ikaros.git
cd ikaros
```

拉取代码后需要进行
git module 初始化

需要初始化是由于主题使用的是git module关联的另一个仓库

```git
git submodule init
git submodule update
```

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

本地运行，复制并更名`config/server/resource/application-local.yaml.example`到`src/main/resource/application-local.yaml`
, 可以根据需要酌情修改，

我建议是本地跑个 [docker for windows](https://docs.docker.com/desktop/install/windows-install/)
，装好mariadb方便开发，但本地开发用H2也行。

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
.\gradlew.bat clean bootJar -x test
```

## 代码格式化

`Setting` => `Editor` => `Code Style` => `Java`

=> `Scheme` 选择 `Project`

选择右边设置，导入checkstyle文件
(如没有该项，则需要先安装checkstyle-idea插件)

最后OK保存

## 本地开发

在`IkarosApplication`的运行配置里，将`Active profiles` 配置成：`dev,win-dev,local`

这里的`local`请先确保您已经进行了上面的`application-local.yaml.example`实例文件复制移动重命名

### Console编译
需要先运行任务进行Console的前端文件编译：

```text
./gradlew buildFrontend -x test
```

## IDEA格式化配置

1. 打开 `Setting` => `Editor` => `Code Style` => `Java`
2. 选择 `Scheme` => 选择 `Project`
3. 点击右边小齿轮 => `Import Scheme` => `Checkstyle configuration` 
4. 选择项目目录 `config/checkstyle` 下的 配置文件`checkstyle.xml` 保存导入
5. 保存设置

## Fast Jar
需要有`Java17`的运行环境

请先按照上放先进行`build`

打`Fast Jar`包，在项目根目录运行下方命令
```shell
# linux
./gradlew clean bootJar -x test
# windows
./gradlew.bat clean bootJar -x test
```
打包后的文件在`server/build/libs/`目录下

在打包文件所在目录，`Linux` 运行
```shell
java -jar ./ikaros-server.jar
```

在打包文件所在目录，`Windwos` 运行，
需要加上额外参数`--spring.profiles.active=win`
```shell
java -jar ikaros-server.jar --spring.profiles.active=win
```

# 提交PR后

更新本地主分支
```
git checkout master | git pull upstream master | git push origin master | git remote prune origin
```

查看本地分支，并删除工作分支
```
git branch
git branch -D {you_branch_name}
```

上游PR合并后，更新本地主分支

如已添加上游仓库可跳过此命令
```text
git remote add upstream https://github.com/ikaros-dev/ikaros.git
```

```text
git checkout master
git pull upstream master
```

# 更多
<https://docs.ikaros.run>
