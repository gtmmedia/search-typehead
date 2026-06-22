# ==========================================
# Stage 1: Build the Spring Boot application
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application jar file
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Create the runtime container
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a directory for the H2 database files (mounted as a volume)
RUN mkdir -p /app/data

# Copy the built jar file from the build stage
COPY --from=build /app/target/search-typeahead-1.0.0.jar app.jar

# Persist the H2 database across container restarts
VOLUME /app/data

# Expose backend port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
