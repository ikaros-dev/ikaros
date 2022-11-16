### [暂时废弃] 本地环境Docker运行参考

**废弃原因**：容器内无法访问和宿主机同局域网的其他主机

我使用的是 Docker Windows Desktop + WSL2

安装docker，配置好镜像源，并开启远程访问

验证远程访问是否成功，浏览器访问 http://ip:2375/version , 正常出现JSON字符串代表成功

## 目录说明

- C:\Users\li-guohao\mariadb: 开发用数据库
- C:\Users\li-guohao\qbbitorrent: qbbitorrent应用数据目录
- C:\Users\li-guohao\ikaros：开发用应用根目录
- C:\Users\li-guohao\ikaros/downloads: 下载目录，要求在ikaros应用目录下，不然文件硬链接会出问题
- C:\Users\li-guohao\ikaros/media: 媒体目录，要求在ikaros应用目录下，不然文件硬链接会出问题

## 部署说明

建议使用IDEA进行配置部署，

## mariadb

拉取镜像

```shell
docker pull mariadb
```

运行容器

```shell
docker run -it -d \
--name mariadb \
-p 3306:3306 \
-v C:\Users\li-guohao\mariadb\conf:/etc/mysql \
-v C:\Users\li-guohao\mariadb\mariadb\data:/var/lib/mysql \
-v C:\Users\li-guohao\mariadb\mariadb\logs:/var/log/mysql \
-e MYSQL_ROOT_PASSWORD=123456  \
--restart=always \
mariadb
```

创建数据库

```shell
create database ikaros;
```

### qbittorrent

拉取镜像

```shell
docker pull linuxserver/qbittorrent
```

运行容器

```shell
docker run -d \
--name=qbittorrent \
-e PUID=0 \
-e PGID=0 \
-e TZ=Asia/Shanghai \
-e WEBUI_PORT=9091 \
-p 9091:9091 \
-p 6881:6881 \
-p 6881:6881/udp \
-v C:\Users\li-guohao\qbbitorrent:/config \
-v C:\Users\li-guohao\ikaros\downloads:/downloads \
--restart=always \
linuxserver/qbittorrent
```

这个镜像默认的WebUI用户名密码是: admin@adminadmin  
局域网添加IP段跳过身份验证  
`设置` => `WebUI` => 选中`对 IP 子网白名单中的客户端跳过身份验证`, 并填入下面内容  

```text
192.168.2.0/24
192.168.3.0/24
10.0.0.0/24
10.0.3.0/24
172.17.0.0/24
```

最后拉到最下方点击保存

### ikaros

idea里添加docker连接，TCP套接字那填入 tcp://IP:2375 即可

建议先拉取下基础镜像

```shell
docker pull eclipse-temurin:17-jdk-alpine
```

如果国内镜像源拉取不了官方镜像源又访问不了，建议开个美区的VPS，拉取好，通过docker命令把镜像保存到文件，再把文件传回本地，上传到虚拟机，再通过命令进行加载镜像文件，最后给文件打好标签。

编译镜像

先执行`DockfileBuild2Dev`进行 `ikaros:dev` 的docker镜像编译，目标选择刚刚添加的docker连接。

```shell
docker run -d \
-p 9090:9090 \
--name=ikaros \
-e PUID=0 \
-e PGID=0 \
-e IKAROS_ENV=local \
-e IKAROS_LOG_LEVEL=DEBUG \
-e IKAROS_SUB_MIKAN_RSS="https://mikanani.me/RSS/MyBangumi?token={token}" \
-v C:\Users\li-guohao\ikaros:/opt/ikaros \
--restart=always \
ikaros:dev
```

### jellyfin

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
-v C:\Users\li-guohao\ikaros\jellyfin/config:/config \
-v C:\Users\li-guohao\ikaros\jellyfin/cache:/cache \
-v C:\Users\li-guohao\ikaros\media:/media \
--restart=always \
jellyfin/jellyfin
```