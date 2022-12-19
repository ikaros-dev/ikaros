FROM eclipse-temurin:17-jdk-alpine
WORKDIR /opt/ikaros
EXPOSE 40000
#COPY . .
#RUN ["./gradlew", "--no-daemon", "clean", "build", "-x", "test"]
COPY build/libs/ikaros-*.jar ${WORKDIR}/app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar ${WORKDIR}/app.jar"]
#docker run -d \
#-p 40000:40000 \
#--name=demo-ikaros \
#-e "JAVA_OPTS=-Dikaros.log-level=DEBUG" \
#-v /share/container/app/demo-ikaros:/opt/ikaros \
#demo-ikaros