# Docker file for LIQUIDO

# We are using JAVA 11 ... on MAC
# FROM --platform=linux/arm64 amazoncorretto:11
FROM amazoncorretto:11
VOLUME /tmp

# You MUST pass JAR_FILE via command line parameter, eg. --build-arg JAR_FILE=target/*.jar
# This way the most recently build version can be used.
ADD target/liquido-backend-spring-6.0.0.jar liquido-backend-spring.jar
ADD application-prod.yml application-prod.yml

ENV PORT 8080
EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/liquido-backend-spring.jar", "-Xms64m -Xmx64m", "--spring.profiles.active=prod"]
