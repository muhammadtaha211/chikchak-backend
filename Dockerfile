# ===== Stage 1: Build the application =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy only what's needed to download dependencies first.
# This layer gets cached — dependencies re-download ONLY when pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Now copy source code and build the jar.
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ===== Stage 2: Run the application =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-Xss512k", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=60.0", "-jar", "app.jar"]