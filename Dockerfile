# 빌드 스테이지
FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY . .

# 소스 코드를 빌드하라는 명령어
RUN ./gradlew clean build -x test

# 빌드된 파일을 실행하기 위한 베이스 이미지
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 결과물 복사 (build/libs 폴더의 war 파일)
COPY --from=build /app/build/libs/*.jar app.war

# 실행 권한 부여 및 포트 설정
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.war"]