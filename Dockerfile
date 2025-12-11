# Dockerfile for Spring Boot JVM (Classic) build
# Uses Java 25 on Distroless

FROM gcr.io/distroless/java25-debian13:nonroot

# Set the port environment variable (Spring Boot will pick this up)
ENV SERVER_PORT=80

# Copy the built JAR file
# Note: The context must include build/libs/ containing the jar
COPY build/libs/*.jar /app/app.jar

# Expose port 80
EXPOSE 80

# Run the jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
