# Dependency and Version Matrix

This document outlines the software dependencies, libraries, and tools utilized in the MedRoute AI application.

## Core Stack

| Component | Version | Notes |
| --- | --- | --- |
| Java | 17 (LTS) | Compatible with all listed dependencies. |
| Apache Tomcat | 9.0.x | Uses `javax.servlet` API (Jakarta EE 8). Tomcat 10+ requires `jakarta.servlet`. |
| MySQL | 8.0.x | Relational Database Management System. |
| Maven | 3.9.x | Build and dependency management tool. |

## Required Dependencies

| Group ID | Artifact ID | Version | Scope | Purpose | License |
| --- | --- | --- | --- | --- | --- |
| `org.springframework` | `spring-webmvc` | 5.3.x | compile | MVC Web Framework | Apache 2.0 |
| `org.springframework` | `spring-context` | 5.3.x | compile | Core IoC Container | Apache 2.0 |
| `org.springframework` | `spring-jdbc` | 5.3.x | compile | JDBC Data Access | Apache 2.0 |
| `org.springframework` | `spring-tx` | 5.3.x | compile | Transaction Management | Apache 2.0 |
| `javax.servlet` | `javax.servlet-api` | 4.0.1 | provided | Servlet API | CDDL/GPLv2+CE |
| `javax.servlet` | `jstl` | 1.2 | compile | JSP Standard Tag Library | CDDL/GPLv2+CE |
| `com.mysql` | `mysql-connector-j` | 8.0.x | runtime | JDBC Driver (works w/ 8.0 & 8.4) | GPLv2 |
| `com.fasterxml.jackson.core`| `jackson-databind` | 2.15.x | compile | JSON Serialization/Deserialization| Apache 2.0 |
| `com.fasterxml.jackson.core`| `jackson-core` | 2.15.x | compile | JSON Serialization/Deserialization| Apache 2.0 |
| `com.fasterxml.jackson.core`| `jackson-annotations`| 2.15.x | compile | JSON Serialization/Deserialization| Apache 2.0 |
| `org.mindrot` | `jbcrypt` | 0.4 | compile | Password Hashing (BCrypt) | ISC / BSD |
| `com.sun.mail` | `javax.mail` | 1.6.x | compile | JavaMail API | CDDL/GPLv2+CE |
| `org.springframework` | `spring-context-support`| 5.3.x | compile | JavaMailSender support | Apache 2.0 |
| `org.slf4j` | `slf4j-api` | 2.0.x | compile | Logging Facade | MIT |
| `ch.qos.logback` | `logback-classic` | 1.4.x | compile | Logging Implementation | EPL/LGPL |
| `org.junit.jupiter` | `junit-jupiter` | 5.10.x | test | Unit Testing Framework | EPL 2.0 |
| `org.mockito` | `mockito-core` | 5.x | test | Mocking Framework | MIT |
| `org.apache.commons` | `commons-lang3` | 3.14.x | compile | Utility classes (Strings, etc.) | Apache 2.0 |
| `commons-io` | `commons-io` | 2.15.x | compile | I/O Utility classes | Apache 2.0 |
| `io.github.cdimascio` | `dotenv-java` | 3.0.x | compile | .env file loading | MIT |

*Note: Spring 5.3.x is the last minor version supporting `javax.servlet` (Jakarta EE 8 namespace).*

## Frontend Libraries (via CDN)

| Library | Version | Purpose |
| --- | --- | --- |
| Bootstrap | 5.3.x | UI Framework and Styling |
| Bootstrap Icons | 1.11.x | Vector Iconography |
| Chart.js | 4.4.x | Data Visualization |
| Leaflet.js | 1.9.x | Interactive Maps |
| Inter Font | latest | Primary Typography (Google Fonts)|

## Maven `pom.xml` Skeleton

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.medroute</groupId>
    <artifactId>medroute-ai</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>war</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <spring.version>5.3.31</spring.version>
        <jackson.version>2.15.3</jackson.version>
        <slf4j.version>2.0.9</slf4j.version>
        <logback.version>1.4.11</logback.version>
        <junit.version>5.10.1</junit.version>
    </properties>

    <dependencies>
        <!-- Spring Framework Core & MVC -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
            <version>${spring.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <version>${spring.version}</version>
        </dependency>

        <!-- Spring Data Access -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
            <version>${spring.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
            <version>${spring.version}</version>
        </dependency>

        <!-- Servlet & JSP APIs -->
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>4.0.1</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>jstl</artifactId>
            <version>1.2</version>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.0.33</version>
            <scope>runtime</scope>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- Security & Utilities -->
        <dependency>
            <groupId>org.mindrot</groupId>
            <artifactId>jbcrypt</artifactId>
            <version>0.4</version>
        </dependency>
        
        <!-- Email -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context-support</artifactId>
            <version>${spring.version}</version>
        </dependency>
        <dependency>
            <groupId>com.sun.mail</groupId>
            <artifactId>javax.mail</artifactId>
            <version>1.6.2</version>
        </dependency>

        <!-- Configuration & Commons -->
        <dependency>
            <groupId>io.github.cdimascio</groupId>
            <artifactId>dotenv-java</artifactId>
            <version>3.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.14.0</version>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.15.1</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.8.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```
