FROM eclipse-temurin:25.0.2_10-jdk-noble AS build
ENV JAVA_TOOL_OPTIONS="--enable-preview"
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25.0.2_10-jre-noble AS runtime
WORKDIR /app
VOLUME /app/config
COPY --from=build /app/build/libs/AsyncLevelling-*.jar app.jar
ENTRYPOINT ["java", "--enable-preview", "-jar", "/app/app.jar"]
