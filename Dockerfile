FROM eclipse-temurin:17-jdk-alpine

ENV PUID=0 \
    PGID=0 \
    UMASK=000 \
    LANG=zh_CN.UTF-8 \
    LANGUAGE=zh_CN:zh \
    LC_ALL=zh_CN.UTF-8 \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-Dikaros.log-level=DEBUG" \
    CONFIG_DIR=/opt/ikaros \
    WORKDIR_DIR=/app

RUN apk add --no-cache \
        su-exec \
        tzdata \
        bash \
    && ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo "${TZ}" > /etc/timezone \
    && mkdir -p \
        ${WORKDIR_DIR} \
        ${CONFIG_DIR}

# file ./config/script/entrypoint.sh base64 string
RUN echo "IyEvYmluL3NoCmNob3duIC1SICIke1BVSUR9IjoiJHtQR0lEfSIgIiR7V09SS0RJUl9ESVJ9IgpjaG93biAiJHtQVUlEfSI6IiR7UEdJRH0iICIke0NPTkZJR19ESVJ9Igp1bWFzayAiJHtVTUFTS30iCmV4ZWMgc3UtZXhlYyAiJHtQVUlEfSI6IiR7UEdJRH0iIGphdmEgJHtKQVZBX09QVFN9IC1qYXIgJHtXT1JLRElSX0RJUn0vYXBwLmphcg==" | base64 -d > /app/entrypoint.sh
RUN chmod 755 /app/entrypoint.sh

COPY --chmod=755 build/libs/ikaros-*.jar ${WORKDIR_DIR}/app.jar

WORKDIR ${CONFIG_DIR}
EXPOSE 50000
VOLUME [ "/opt/ikaros" ]

ENTRYPOINT ["/app/entrypoint.sh"]

#docker run -d \
#-p 50000:50000 \
#--name=demo-ikaros \
#-e "JAVA_OPTS=-Dikaros.log-level=DEBUG" \
#-v /share/container/app/demo-ikaros:/opt/ikaros \
#demo-ikaros
