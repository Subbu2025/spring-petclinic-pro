# Initial Dockerfile: Build and Run in One Step (Non-Optimized)
FROM maven:3.9.4-eclipse-temurin-17-alpine

WORKDIR /app

# Copy source code and build the application
COPY . .
RUN mvn clean package -DskipTests

# Run the application
ENTRYPOINT ["java", "-jar", "target/spring-petclinic-*.jar"]
