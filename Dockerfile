FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY api/ExpenseServer.java .
RUN javac ExpenseServer.java
EXPOSE 8080
CMD ["java", "ExpenseServer"]
