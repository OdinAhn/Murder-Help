FROM --platform=$BUILDPLATFORM node:24-alpine AS frontend
WORKDIR /frontend

RUN corepack enable && corepack prepare pnpm@10.34.3 --activate

# 의존성 설치를 소스 복사보다 먼저 해야 프론트 소스만 바뀌었을 때
# install 레이어가 캐시에서 재사용된다.
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY frontend ./
RUN pnpm run build

FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

COPY --from=frontend /frontend/dist ./src/main/resources/static

RUN chmod +x gradlew  && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --create-home --shell /bin/bash app

COPY --from=builder /workspace/build/libs/*.jar app.jar

USER app

EXPOSE 8080

# 배포 환경(EC2 등)에서는 Parameter Store 값을 env-file 로 주입만 하면 되도록
# prod 프로파일을 기본값으로 고정한다. 필요 시 컨테이너 실행 시 -e SPRING_PROFILES_ACTIVE=... 로 덮어쓸 수 있다.
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]