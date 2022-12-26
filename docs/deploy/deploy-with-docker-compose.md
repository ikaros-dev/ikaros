# docker compose 方式部署指导手册

<hr>

# 前言
目前**仅供体验**功能，**不推荐使用在生产环境下(正式使用)**

# 文档适用范围
当前只适用于[DockerHub Ikaros Tags](https://hub.docker.com/r/ikarosrun/ikaros/tags)为`dev`的镜像，旧的tag暂时不适用

# 根目录

目录需要在同一个目录下，不然硬链接创建会失效
这里的根目录是：/opt/ikaros
根目录你可以换成你想要存储ikaros应用数据的目录

# 设置环境变量
你只需要配置好下面的环境变量，修改对应的值，然后直接用给的docker-compose.yml文件启动即可。

- `IKAROS_APP_DIR`: ikaros的数据目录，这里是：/opt/ikaros
- `IKAROS_DB_PASSWORD`: ikaros的数据库密码，建议设置复杂点的密码
- `IKAROS_DOCKER_TAG`: ikaros的dockerhub的tag，可以查看[DockerHub Ikaros Tags](https://hub.docker.com/r/ikarosrun/ikaros/tags)找到您要部署的版本
- `IKAROS_APP_URL_PREFIX`: ikaros的服务端URL前缀，如`http://localhost:9090` 或者 `http://demo.ikaros.run`
- `IKAROS_APP_IMPORT_DIR`: ikaros的导入目录，您可以挂载相同文件系统下的一个空目录，ikaros会半小时扫描一次这个目录(需要在 后台/系统设置/应用 开启导入目录自动扫描并保存)，将目录里的文件通过**复制**的方式录入数据库文件管理。比较推荐的方式是：在您存储了大量番剧的目录里建一个子目录比如ikaros，挂载这个子目录给ikaros，需要导入的时候把已有的番剧移动至这个目录即可

使用命令进行设置

```shell
export IKAROS_APP_DIR=/opt/ikaros
export IKAROS_DB_PASSWORD=openpostgresql
export IKAROS_DOCKER_TAG=v0.1.0-rc.2
export IKAROS_APP_URL_PREFIX=http://localhost:50000
export IKAROS_APP_IMPORT_DIR=/share/storage/ikaros
```

- /opt/ikaros: Ikaros应用目录

- /opt/ikaros/tripartite: Ikaros依赖的第三方应用数据目录，你也可以选择不放在这里

- /opt/ikaros/downloads: 下载目录

- /opt/ikaros/media: 媒体目录

## 查看版本

没有安装需要安装下compose，请参考：[Overview | Docker Documentation](https://docs.docker.com/compose/install/)

```shell
docker-compose --version
```

# docker compose
- [docker-compose.yml](docker-compose.yml)
- [docker-compose-no-jellyfin.yml](docker-compose-no-jellyfin.yml)

## 添加yml文件
选择上面的一个yml文件，下载到您的命令行当前目录；
或者使用vi编辑器复制上方的其中一个yml文件内容，保存为您当前命令行的一个yml文件

## 启动docker-compose
执行compose文件，文件名称自定义，我这里是`docker-compose.yml`

``` 
docker-compose up -d
```

默认是当前目录下的`docker-compose.yml`文件启动，如果需要指定文件则命令如下

比如 文件是`docker-compose-no-jellyfin.yml`
``` 
docker-compose -f docker-compose-no-jellyfin.yml up -d
```

# 启动后的配置

## ikaros
启动后的后台配置

可以在后台系统设置里，配置HTTP代理、蜜柑计划RSS、等设置项

## qbittorrent
这个镜像默认的WebUI用户名密码是: admin@adminadmin


## jellyfin
这个添加媒体库就行，选择 `/media` 或者你部署jellyfin容器时的更改的目录

## 网络
如果你网络不好，需要HTTP代理，推荐直接部署一个全局模式的`clash`容器, 
记得使用 host 模式，不然容器间网络隔离很麻烦，HTTP代理连不上。
