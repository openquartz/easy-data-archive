FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY easyarchive-common ./easyarchive-common
COPY easyarchive-core ./easyarchive-core
COPY easyarchive-starter ./easyarchive-starter

RUN mvn -pl easyarchive-starter -am clean package -DskipTests -Dmaven.javadoc.skip=true

FROM eclipse-temurin:11-jre
WORKDIR /app

COPY --from=build /workspace/easyarchive-starter/target/easyarchive-starter.jar /app/easyarchive-starter.jar

EXPOSE 8789
ENTRYPOINT ["java", "-jar", "/app/easyarchive-starter.jar"]
