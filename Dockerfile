# Step 1: Build stage - using a smaller JDK base image for Maven build
FROM maven:3.9.4-eclipse-temurin-17-jre-alpine AS build

WORKDIR /app

# Copy only the pom.xml and download dependencies to cache them in a separate layer
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy the rest of the project files and build the application
COPY src ./src
RUN mvn clean package -DskipTests -Ddockerfile.skip=true \
    && rm -rf /root/.m2/repository  # Remove Maven cache after building

# Step 2: Runtime stage - using a slimmer JRE-only base image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/spring-petclinic-*.jar /app/app.jar

# Expose the application port
EXPOSE 8080

# Define the entry point for the container
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
