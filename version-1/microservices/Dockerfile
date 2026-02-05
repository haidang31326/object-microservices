# 1. Chọn môi trường chạy Java 21 (giống trong video)
FROM docker.io/eclipse-temurin:21-jdk

# 2. Copy file .jar đã build vào trong container
# Lưu ý: Em phải chạy 'mvn clean package' trước để có file trong thư mục target/
COPY target/*.jar app.jar

# 3. Lệnh chạy ứng dụng khi container khởi động
ENTRYPOINT ["java", "-jar", "/app.jar"]