# 部署的目录
目录需要在同一个根目录下，不然硬链接创建会失效，这里给个例子

- /share/test/app: 容器应用目录
- /share/test/app/ikaros: Ikaros应用目录
- /share/test/app/ikaros/downloads: 下载目录
- /share/test/app/ikaros/media: 媒体目录

## ikaros
``` shell
docker run -d \
-p 9093:9090 \
--name=ikaros_demo \
-e PUID=0 \
-e PGID=0 \
-e "JAVA_OPTS=-Dikaros.log-level=DEBUG" \
-v /share/test/app/ikaros:/opt/ikaros \
--restart=always \
ikaros:dev
```

## qbittorrent
``` shell
docker run -d \
--name=ikaros_qbittorrent \
-e PUID=0 \
-e PGID=0 \
-e TZ=Asia/Shanghai \
-e WEBUI_PORT=9091 \
-p 9091:9091 \
-p 47796:47796 \
-p 47796:47796/udp \
-v /share/test/app/qbittorrent:/config \
-v /share/test/app/ikaros/downloads:/downloads \
-v /share/test/app/ikaros/media:/media \
--restart=always \
linuxserver/qbittorrent:latest
```
这个镜像默认的WebUI用户名密码是: admin@adminadmin
局域网添加IP段跳过身份验证
`设置` => `WebUI` => 选中`对 IP 子网白名单中的客户端跳过身份验证`, 并填入下面内容
```text
192.168.2.0/24
192.168.3.0/24
10.0.0.0/24
10.0.3.0/24
```
最后拉到最下方点击保存

## jellyfin
```shell
docker run -d \
--name=ikaros_jellyfin \
-e PUID=0 \
-e PGID=0 \
-e LANG=zh_CN.UTF-8 \
-e LANGUAGE=zh_CN:zh \
-e LC_ALL=zh_CN.UTF-8 \
-e TZ=Asia/Shanghai \
-p 9092:8096 \
-v /share/test/app/jellyfin/config:/config \
-v /share/test/app/jellyfin/cache:/cache \
-v /share/test/app/ikaros/media:/media \
--device /dev/dri:/dev/dri  \
--restart=always  \
jellyfin/jellyfin
```

