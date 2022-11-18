### 生产环境Docker运行参考

我使用的是NAS自带的容器环境，开启远程访问，验证是否成功

浏览器访问 http://ip:2375/version , 正常出现JSON字符串代表成功

## 目录说明

- /share/container/app/ikaros_qbbitorrent: qbbitorrent应用数据目录
- /share/container/app/ikaros：应用根目录
- /share/container/app/ikaros/downloads: 下载目录，要求在ikaros应用目录下，不然文件硬链接会出问题
- /share/container/app/ikaros/media: 媒体目录，要求在ikaros应用目录下，不然文件硬链接会出问题

### qbittorrent

拉取镜像

```shell
docker pull linuxserver/qbittorrent
```

运行容器

```shell
docker run -d \
--name=ikaros_qbbitorrent \
-e PUID=0 \
-e PGID=0 \
-e TZ=Asia/Shanghai \
-e WEBUI_PORT=9091 \
-p 9091:9091 \
-p 6881:6881 \
-p 6881:6881/udp \
-v /share/container/app/ikaros_qbbitorrent:/config \
-v /share/container/app/ikaros/downloads:/downloads \
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

```shell
docker run -d \
-p 9090:9090 \
--name=ikaros \
-e PUID=0 \
-e PGID=0 \
-e IKAROS_QB_URL=http://192.168.2.229:9091/api/v2/ \
-e IKAROS_LOG_LEVEL=DEBUG \
-e IKAROS_SUB_MIKAN_RSS="https://mikanani.me/RSS/MyBangumi?token={token}" \
-v /share/container/app/ikaros:/opt/ikaros \
--restart=always \
ikaros:0.1.0
```

