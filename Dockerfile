# Build the fat JAR and a trimmed JRE (jdeps can't analyse this JAR, so the module list is fixed)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
# The wrapper's default 10s download timeout is too short on slow networks.
RUN sed -i 's/^networkTimeout=.*/networkTimeout=120000/' gradle/wrapper/gradle-wrapper.properties \
 && (./gradlew fatJar --no-daemon -x test || ./gradlew fatJar --no-daemon -x test) \
 && cp build/libs/icloud-calendar-mcp-*-all.jar /app.jar \
 && jlink --add-modules java.se,jdk.unsupported,jdk.crypto.ec \
      --strip-debug --no-man-pages --no-header-files --compress=zip-6 --output /jre

FROM debian:bookworm-slim
COPY --from=build /jre /opt/jre
COPY --from=build /app.jar /app/icloud-calendar-mcp.jar
RUN useradd -r -u 10001 mcp
USER mcp
ENV MCP_TRANSPORT=http MCP_PORT=8000 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k"
EXPOSE 8000
# Needs ICLOUD_USERNAME, ICLOUD_PASSWORD and MCP_AUTH_TOKEN at runtime
CMD ["/opt/jre/bin/java", "-jar", "/app/icloud-calendar-mcp.jar"]
