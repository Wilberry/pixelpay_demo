# PixelWallet API - Deployment & Architecture Guide

## 🚀 Quick Start with Docker Compose

```bash
# Start PostgreSQL
docker-compose up -d

# Verify database is ready
docker-compose exec postgres pg_isready -U postgres

# Build application
mvn clean install

# Run application
mvn spring-boot:run
```

Application starts on `http://localhost:8080`

---

## 🔧 Configuration Management

### Development Environment
```properties
# application-dev.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pixelwallet
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
logging.level.com.pixelwallet=DEBUG
```

### Production Environment
```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://prod-db-host:5432/pixelwallet
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
jwt.secret=${JWT_SECRET}
logging.level.com.pixelwallet=WARN
```

### Environment Variables
```bash
export DB_USER=postgres
export DB_PASSWORD=secure_password_here
export JWT_SECRET=your_very_long_secret_key_here_minimum_256_chars
export SPRING_PROFILES_ACTIVE=prod
```

---

## 🏗️ Database Setup

### Manual PostgreSQL Setup
```sql
-- Connect as admin
psql -U postgres

-- Create database
CREATE DATABASE pixelwallet;

-- Create application user
CREATE USER pixelwallet_user WITH PASSWORD 'secure_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE pixelwallet TO pixelwallet_user;

-- Connect to database
\c pixelwallet

-- Hibernate will auto-create tables on first run
-- Or manually import schema.sql if you prefer
```

### Database Backup & Restore
```bash
# Backup
pg_dump -U postgres pixelwallet > backup.sql

# Restore
psql -U postgres pixelwallet < backup.sql

# Backup with compression
pg_dump -U postgres -F c pixelwallet > backup.dump

# Restore from compressed
pg_restore -U postgres -d pixelwallet backup.dump
```

---

## 📦 Deployment Options

### Option 1: Standalone JAR
```bash
# Build fat JAR
mvn clean package

# Run JAR
java -jar target/pixelwallet-api-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/pixelwallet \
  --spring.datasource.username=postgres \
  --spring.datasource.password=postgres
```

### Option 2: Docker Container
```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/pixelwallet-api-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build image
docker build -t pixelwallet-api:1.0.0 .

# Run container
docker run -d \
  --name pixelwallet-api \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pixelwallet \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  --link pixelwallet-postgres:postgres \
  pixelwallet-api:1.0.0
```

### Option 3: Kubernetes
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pixelwallet-api
  labels:
    app: pixelwallet
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pixelwallet
  template:
    metadata:
      labels:
        app: pixelwallet
    spec:
      containers:
      - name: api
        image: pixelwallet-api:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        livenessProbe:
          httpGet:
            path: /api/auth/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/auth/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: pixelwallet-api-service
spec:
  type: LoadBalancer
  selector:
    app: pixelwallet
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

---

## 🔒 Security Checklist

- [ ] Change JWT secret in production
- [ ] Disable `spring.jpa.show-sql` in production
- [ ] Use HTTPS/TLS for all connections
- [ ] Enable CORS only for trusted domains
- [ ] Implement rate limiting
- [ ] Add request logging for audit trail
- [ ] Use secrets management (HashiCorp Vault, AWS Secrets Manager)
- [ ] Enable database encryption at rest
- [ ] Regular security scanning (OWASP dependency checker)
- [ ] Implement API key authentication for service-to-service calls

---

## 📊 Monitoring & Logging

### Add Actuator for Health Checks
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```properties
# application.properties
management.endpoints.web.exposure.include=health,metrics
management.endpoint.health.show-details=always
```

### Application Metrics
```
GET /actuator/health
GET /actuator/metrics
GET /actuator/env
```

### Integration with ELK Stack
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>
```

```xml
<!-- logback-spring.xml -->
<configuration>
  <!-- File logging -->
  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/pixelwallet.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>logs/pixelwallet.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
      <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder>
      <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  
  <root level="INFO">
    <appender-ref ref="FILE"/>
  </root>
</configuration>
```

---

## ⚡ Performance Optimization

### Database Connection Pooling
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
```

### Hibernate Batch Processing
```properties
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

### Caching
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

## 🚨 Disaster Recovery

### Database Backup Strategy
- Daily full backups
- Hourly incremental backups
- Store backups in separate region
- Test restore procedure monthly

### Application Backup
- Version control (Git) for all code
- Immutable container images with tags
- Infrastructure as Code (Terraform, CloudFormation)

### High Availability
- Multiple application instances behind load balancer
- Database replication with failover
- Read replicas for reporting queries
- Circuit breaker for external service calls

---

## 📈 Scaling Strategy

### Vertical Scaling (Current Instance)
1. Increase JVM heap size: `-Xmx2g -Xms1g`
2. Tune database connection pool
3. Increase PostgreSQL shared buffers
4. Use SSD storage for database

### Horizontal Scaling
1. Deploy multiple instances
2. Use load balancer (Nginx, HAProxy, AWS ELB)
3. Share database across instances
4. Use Redis for session management
5. Implement distributed caching

### Database Scaling
1. Read replicas for analytics
2. Sharding by user ID for partition tolerance
3. Connection pooling (PgBouncer)
4. Query optimization and indexing

---

## 🔍 Troubleshooting

### Application Won't Start
```bash
# Check logs
tail -f logs/pixelwallet.log

# Verify database connection
psql -h localhost -U postgres -d pixelwallet -c "SELECT 1"

# Check port availability
lsof -i :8080
```

### Slow Transfers
```sql
-- Check slow queries
SELECT query, mean_time FROM pg_stat_statements 
ORDER BY mean_time DESC LIMIT 10;

-- Add missing indexes
CREATE INDEX idx_wallet_balance ON wallets(balance);
```

### High Memory Usage
```bash
# Check JVM memory
jmap -heap <pid>

# Increase heap size if needed
export JAVA_OPTS="-Xmx2g -Xms1g"
```

---

## 📝 License

Professional deployment guide for PixelWallet API.
