# ---- Build Stage (Java 21) ----
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom and download dependencies (go-offline speeds second builds)
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source code
COPY src ./src

# Package the application
RUN mvn -B -DskipTests package

# ---- Run Stage (Java 21) ----
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Start using Render's $PORT
CMD ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]
