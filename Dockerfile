FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY api /app/api
RUN javac -cp "/app/api/lib/*" /app/api/ExpenseServer.java
EXPOSE 8080
CMD ["java", "-cp", "/app/api/lib/*:/app/api", "ExpenseServer"]
