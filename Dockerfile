FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
ADD ${JAR_FILE} liquido-backend-spring.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/liquido-backend-spring.jar"]