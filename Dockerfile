# Stage 1: Build file .jar bằng Maven
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy thư mục source và file pom.xml vào container
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src src

# Phân quyền cho file mvnw có thể chạy được (đề phòng lỗi permission denied)
RUN chmod +x ./mvnw

# Chạy lệnh build project và skip các test (vì đã test ở bước trước đó trong CI)
RUN ./mvnw clean package -DskipTests

# Stage 2: Tạo image chạy ứng dụng siêu nhẹ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar đã build từ Stage 1 sang Stage 2
COPY --from=build /app/target/*.jar app.jar

# Mở cổng 8080 (cổng mặc định của Spring Boot)
EXPOSE 8080

# Chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
