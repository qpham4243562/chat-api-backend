# Giai đoạn build - sử dụng phiên bản Maven khác
FROM maven:3.8-openjdk-17 AS build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

# Build ứng dụng
RUN mvn clean package -DskipTests

# Giai đoạn runtime
FROM openjdk:17-jdk-alpine

WORKDIR /app

# Sao chép file JAR từ giai đoạn build
COPY --from=build /app/target/*.jar app.jar

# Thiết lập lệnh chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
