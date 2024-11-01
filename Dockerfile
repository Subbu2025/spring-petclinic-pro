# Step 1: Build stage - using a Maven image with JDK 17 for Alpine
FROM maven:3.9.4-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy the pom.xml and download dependencies separately to leverage Docker caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy the source code after dependencies are cached
COPY src ./src

# Package the application
RUN mvn clean package -DskipTests -Ddockerfile.skip=true \
    && rm -rf /root/.m2/repository  # Remove Maven cache after building

# Step 2: Runtime stage - using a slim JRE-only base image for runtime
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/spring-petclinic-*.jar /app/app.jar

# Expose the application port
EXPOSE 8080

# Define the entry point for the container
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
