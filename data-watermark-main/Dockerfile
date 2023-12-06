FROM maven:3.6.3 as mvn-builder
COPY pom.xml /build/pom.xml
WORKDIR /build
RUN mvn verify --fail-never
COPY . /build
WORKDIR /build
RUN mvn package -Dmaven.test.skip=true


FROM openjdk:8-jdk-alpine
ARG JAR_FILE=/build/target/*.jar
COPY --from=mvn-builder ${JAR_FILE} /root/app.jar
WORKDIR /root
RUN mkdir -p dw
ENTRYPOINT ["java","-jar","-Duser.timezone=GMT+08","app.jar"]
