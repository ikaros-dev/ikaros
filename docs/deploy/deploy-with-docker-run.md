# docker run 方式部署指导手册

<hr>

# 部署的目录

目录需要在同一个目录下，不然硬链接创建会失效
这里的根目录是：/opt/ikaros
根目录你可以换成你想要存储ikaros应用数据的目录

- /opt/ikaros: Ikaros应用目录
- /opt/ikaros/tripartite: Ikaros依赖的第三方应用数据目录，你也可以选择不放在这里
- /opt/ikaros/downloads: 下载目录
- /opt/ikaros/media: 媒体目录

## qbittorrent

```shell
docker run -d \
--name=ikaros_qbittorrent \
-e PUID=0 \
-e PGID=0 \
-e TZ=Asia/Shanghai \
-e WEBUI_PORT=9091 \
-p 9091:9091 \
-p 6881:6881 \
-p 6881:6881/udp \
-v /opt/ikaros/tripartite/qbittorrent:/config \
-v /opt/ikaros/downloads:/downloads \
-v /opt/ikaros/media:/media \
--restart=always \
linuxserver/qbittorrent:latest
```

这个镜像默认的WebUI用户名密码是: admin@adminadmin
局域网添加IP段跳过身份验证
`设置` => `WebUI` => 选中`对 IP 子网白名单中的客户端跳过身份验证`, 并填入下面内容

```text
192.168.1.0/24
192.168.2.0/24
192.168.3.0/24
10.0.0.0/24
10.0.3.0/24
```

最后拉到最下方点击保存

## jellyfin

jellyfin 你可以不部署，也可以直接使用其它的实例，只需要挂载ikaros媒体目录即可(/opt/ikaros/media)

```shell
docker run -d \
--name=ikaros_jellyfin \
-e PUID=0 \
-e PGID=0 \
-e LANG=zh_CN.UTF-8 \
-e LANGUAGE=zh_CN:zh \
-e LC_ALL=zh_CN.UTF-8 \
-e TZ=Asia/Shanghai \
-p 8096:8096 \
-v /opt/ikaros/tripartite/jellyfin/config:/config \
-v /opt/ikaros/tripartite/jellyfin/cache:/cache \
-v /opt/ikaros/media:/media \
--restart=always  \
jellyfin/jellyfin
```

## ikaros

```shell
docker pull liguohaocn/ikaros:dev
```

```shell
docker run -d \
-p 9090:9090 \
--name=ikaros_dev \
-e PUID=0 \
-e PGID=0 \
-e JAVA_OPTS=-Dikaros.log-level=DEBUG \
-v /opt/ikaros:/opt/ikaros \
--restart=always \
liguohaocn/ikaros:dev
```

启动后的后台配置

可以在后台系统设置里，配置HTTP代理、蜜柑计划RSS、等设置项

如果你网络不好，需要HTTP代理，推荐直接部署一个全局模式的`clash`容器, 
记得使用 host 模式，不然容器间网络隔离很麻烦，HTTP代理连不上。
