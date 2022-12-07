# docker compose 方式部署指导手册

<hr>

# 根目录

目录需要在同一个目录下，不然硬链接创建会失效
这里的根目录是：/opt/ikaros
根目录你可以换成你想要存储ikaros应用数据的目录

# 设置环境变量
你只需要配置好下面的环境变量，修改对应的值，然后直接用给的docker-compose.yml文件启动即可。

- `IKAROS_APP_DIR`: ikaros的数据目录，这里是：/opt/ikaros
- `IKAROS_DB_PASSWORD`: ikaros的数据库密码，建议设置复杂点的密码
- `IKAROS_DOCKER_TAG`: ikaros的dockerhub的tag，可以查看[DockerHub Ikaros Tags](https://hub.docker.com/r/ikarosrun/ikaros/tags)找到您要部署的版本

使用命令进行设置

```shell
export IKAROS_APP_DIR=/opt/ikaros
export IKAROS_DB_PASSWORD=openpostgresql
export IKAROS_DOCKER_TAG=v0.1.0-beta.1
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


如果你网络不好，需要HTTP代理，推荐直接部署一个全局模式的`clash`容器, 
记得使用 host 模式，不然容器间网络隔离很麻烦，HTTP代理连不上。

## jellyfin
这个添加媒体库就行，选择 `/media` 或者你部署jellyfin容器时的更改的目录
