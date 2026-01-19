FROM eclipse-temurin:22-jre-alpine

WORKDIR /app

RUN addgroup -S javauser && adduser -S javauser -G javauser

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

RUN chown javauser:javauser /app/app.jar
USER javauser

EXPOSE 8085 9099

ENTRYPOINT ["java", "-jar", "app.jar"]