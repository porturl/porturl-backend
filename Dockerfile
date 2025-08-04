FROM eclipse-temurin:21

RUN mkdir /opt/app

COPY build/libs/*.jar /opt/app/app.jar

EXPOSE 80
CMD ["java", "-jar", "/opt/app/app.jar"]
