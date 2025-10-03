# ---- build stage (JDK 21) ----
FROM gradle:8.8-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle clean bootJar -x test

# ---- run stage (JRE 21) ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# healthcheck에서 사용할 curl 설치
RUN apk add --no-cache curl

# (선택) 한국 시간대 설정
# RUN apk add --no-cache tzdata && ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# 비루트 권장
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# JAR 복사 (파일명 고정 권장: 아래 '참고' 참고)
COPY --from=build /workspace/build/libs/app.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
