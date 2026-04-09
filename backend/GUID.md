# TaskFlow Backend Guide

### Run command
To start the backend server without errors (ensuring correct memory allocation), use the following command:

```bash
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx512m -Xms256m"
```

### Database configuration
Make sure your PostgreSQL server is running on localhost:5432 with the database 'Task-Flow'.
