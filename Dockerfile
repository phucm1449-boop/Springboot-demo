# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-jammy AS build

# Copy source code and pom.xml file to /app folder
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src

# Build source code with maven
RUN ./mvnw -B clean package -DskipTests

# Stage 2: Create image, Runtime stage
FROM eclipse-temurin:17-jre-jammy

# Set working folder to App and copy compiled file from above step
WORKDIR /app
RUN useradd --create-home --shell /bin/false appuser
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser

EXPOSE 8088
# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
