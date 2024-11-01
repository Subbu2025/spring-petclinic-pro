# Step 1: Build Stage with Maven and JDK 17
FROM maven:3.9.4-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy the pom.xml and download only necessary dependencies to reduce image size
COPY pom.xml ./ 
RUN mvn dependency:go-offline -B

# Copy the rest of the source code and package the application
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Runtime Optimization with JLink
FROM eclipse-temurin:17-jdk-alpine AS jlink

# Use jlink to create a minimal Java runtime, including java.desktop for PropertyEditorSupport
RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.logging,java.sql,java.xml,java.desktop \
    --output /javaruntime \
    --strip-debug --no-man-pages --no-header-files --compress=2

# Step 3: Final Runtime Stage with Minimal Java Runtime
FROM alpine:3.17

WORKDIR /app

# Copy minimal Java runtime and built JAR from previous stages
COPY --from=jlink /javaruntime /javaruntime
COPY --from=build /app/target/spring-petclinic-*.jar /app/app.jar

# Set the PATH to include minimal Java runtime
ENV PATH="/javaruntime/bin:$PATH"

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
