# ---- build stage (JDK 21) ----
FROM gradle:8.8-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
# 테스트는 스킵해서 빠르게
RUN gradle bootJar -x test

# ---- run stage (JRE 21) ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# (선택) 한국 시간대
# RUN apk add --no-cache tzdata && ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# 산출물 복사 (파일명 고정 시 더 안전)
# build.gradle에 아래 한 줄을 넣어두면 경로 확정됩니다:
# tasks.bootJar { archiveFileName = "app.jar" }
COPY --from=build /workspace/build/libs/app.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
