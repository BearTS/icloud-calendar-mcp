# Build the fat JAR and a minimal JRE
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew fatJar --no-daemon -x test \
 && cp build/libs/icloud-calendar-mcp-*-all.jar /app.jar \
 && jlink --add-modules "$(jdeps --ignore-missing-deps --multi-release 21 --print-module-deps /app.jar),jdk.crypto.ec,jdk.unsupported,java.naming" \
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
