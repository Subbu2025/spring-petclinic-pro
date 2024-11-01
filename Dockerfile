# Step 1: Build stage
FROM maven:3.9.4-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies (dependencies are cached if unchanged)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests -Ddockerfile.skip=true \
    && rm -rf /root/.m2/repository  # Remove Maven cache to reduce size

# Step 2: Runtime stage with a smaller base image
FROM openjdk:17-jdk-alpine

WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/spring-petclinic-*.jar /app/app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
