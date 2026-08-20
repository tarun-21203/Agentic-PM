FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app
COPY . .

# Windows CRLF -> remove for Linux container execution.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /app/build/libs/agentic-pm-api.jar /app/app.jar

EXPOSE 80
ENTRYPOINT ["java","-jar","/app/app.jar"]

