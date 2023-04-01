# syntax=docker/dockerfile:1.3
# NOTE: Requires DOCKER_BUILDKIT=1 to be set in the environment
ARG COMPILATION_DIR=/opt/build

# In the first stage, build the .jar with all dependencies included
FROM maven:3-eclipse-temurin-17-alpine as build
ARG COMPILATION_DIR
WORKDIR ${COMPILATION_DIR}
COPY src/ ./src/
COPY pom.xml ./
# The --mount=type=cache option allows us to re-use the downloaded maven packages between builds, greatly reducing build times
# after the first run.
RUN --mount=type=cache,target=/root/.m2 mvn package

# The second stage is the final docker container that will be shipped to the repo
# It is much more light-weight than the first stage, since it doesn't contain an entire JDK plus maven and its package
# repository - only the JRE and the JAR running on it.
FROM eclipse-temurin:17-jre-alpine
ARG COMPILATION_DIR
COPY --from=build ${COMPILATION_DIR}/target/*.jar /app.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

VOLUME /config
EXPOSE 4840/tcp
ENTRYPOINT ["java","-jar","/app.jar"]
