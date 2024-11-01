# Stage 1: Build Stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create minimal Java runtime with JLink
FROM eclipse-temurin:17-jdk-alpine AS jlink
RUN $JAVA_HOME/bin/jlink \
    --module-path $JAVA_HOME/jmods \
    --add-modules java.base,java.logging,java.xml,java.naming,java.sql,java.desktop,java.security.jgss,java.management \
    --output /javaruntime \
    --compress=2 --no-header-files --no-man-pages

# Stage 3: Final Stage
FROM alpine:3.17
WORKDIR /app
COPY --from=jlink /javaruntime /opt/java-minimal
ENV PATH="/opt/java-minimal/bin:$PATH"
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
