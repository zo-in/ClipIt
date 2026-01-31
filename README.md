

# ClipIt: Universal Media Downloader and Editor

## Project Overview

ClipIt is a full-stack web application designed to streamline the process of downloading and editing media from various online platforms, including YouTube, X (Twitter), Instagram, and TikTok. Unlike standard downloaders, ClipIt implements a "Universal Download and Edit" pipeline. This allows users to not only extract video and audio streams but also perform post-processing tasks such as trimming content, resizing resolutions, and re-encoding formats before saving the final media to their local machine.

The system is built on a distributed microservices architecture, ensuring modularity and scalability. It decouples resource-intensive media transcoding tasks from user management to maintain a responsive user experience.

## Key Features

* **Universal Media Extraction:** Utilizes `yt-dlp` to fetch metadata and raw media streams from a diverse range of platforms.
* **Advanced Video Processing:** Integrates `FFmpeg` to normalize video codecs, transcode formats (e.g., converting .m3u8 or .webm to .mp4), and apply user-defined edits.
* **GPU Acceleration:** Supports hardware-accelerated encoding using NVIDIA `h264_nvenc` for improved performance during video processing tasks.
* **Microservices Architecture:** Composed of distinct services for Gateway, Registry, Authentication, and Job Processing to ensure separation of concerns.
* **Secure Authentication:** Implements stateless session management using JSON Web Tokens (JWT) and secures user passwords with BCrypt hashing.
* **Asynchronous Job Management:** Features a state-machine module that tracks the lifecycle of every request through Queued, Downloading, Transcoding, and Completed states.
* **Automatic File Cleanup:** Includes a scheduled service to remove old processed files after 24 hours to manage storage efficiently.

## System Architecture

The backend is powered by Java 17 and Spring Boot 3.x, organized as a multi-module Maven project. The system comprises four core microservices:

1. **Service Registry (Port 8761):** A Netflix Eureka Server that acts as the central directory for service discovery, enabling communication between microservices.
2. **API Gateway (Port 8080):** The entry point for all external requests. It routes `/api/auth/**` to the Auth Service and `/api/jobs/**` to the Job Service while handling initial authentication filtering.
3. **Auth Service (Port 8081):** Manages user identity, registration, login, and JWT generation.
4. **Job Service (Port 8082):** The core processing engine responsible for handling media downloads and CPU/GPU-intensive transcoding operations.

## Technology Stack

* **Language:** Java 17
* **Framework:** Spring Boot 3.x
* **Build Tool:** Maven
* **Service Discovery:** Spring Cloud Netflix Eureka
* **Gateway:** Spring Cloud Gateway
* **Security:** Spring Security, JWT (JSON Web Tokens)
* **Database:** MySQL 8 (Relational storage for users and job history)
* **External Tools:**
* `yt-dlp` (Media Extraction)
* `FFmpeg` (Media Processing)



## Prerequisites

Before running the application, ensure the following software is installed and configured on your machine:

1. **Java Development Kit (JDK) 17** or higher.
2. **Maven** (or use the provided `mvnw` wrapper scripts).
3. **MySQL Server** running on port `3306`.
4. **FFmpeg:** Installed and added to your system PATH.
5. **yt-dlp:** Installed and added to your system PATH.

## Configuration

You must configure the environment variables to match your local setup. Create a `.env` file in the root directory or set the following system environment variables as referenced in the `application.properties` files:

```properties
# Database Configuration
DB_USERNAME=root
DB_PASSWORD=your_password

# External Tools Configuration
# Provide absolute paths to the executables
YT_DLP_PATH=/path/to/yt-dlp
FFMPEG_PATH=/path/to/ffmpeg

# Security Configuration
JWT_SECRET=your_secure_random_secret_key

```

*Note: For Windows users, ensure paths use double backslashes (e.g., C:\Tools\ffmpeg.exe).*

## Installation and Setup

1. **Clone the Repository:**
Download the source code to your local machine.
2. **Database Initialization:**
Log in to your MySQL server and create the required databases. The application is configured to automatically create the necessary tables on startup.
```sql
CREATE DATABASE clipit_auth;
CREATE DATABASE clipit_jobs;

```


3. **Build the Project:**
Navigate to the project root and build all modules using Maven.
```bash
./mvnw clean install

```



## Running the Application

The services must be started in the specific order listed below to ensure dependencies (like the Service Registry) are available.

1. **Start Service Registry:**
```bash
cd service-registry
./mvnw spring-boot:run

```


*Wait for the Eureka server to initialize on port 8761.*

2. **Start Auth Service:**
```bash
cd auth-service
./mvnw spring-boot:run

```


3. **Start Job Service:**
```bash
cd job-service
./mvnw spring-boot:run

```


4. **Start API Gateway:**
```bash
cd api-gateway
./mvnw spring-boot:run

```



Once all services are running, the API Gateway will be accessible at `http://localhost:8080`.

## API Documentation

The Job Service is configured with OpenAPIDefinition and exposes interactive API documentation via Swagger UI. You can access the documentation using the following URLs when the Job Service is running:

* **Swagger UI (Auth Service):** `http://localhost:8081/swagger-ui/index.html`
* **Swagger UI (Job Service):** `http://localhost:8082/swagger-ui/index.html`

## API Usage Examples

### Authentication

**Register User**

* **Endpoint:** `POST /api/auth/register`
* **Body:**
```json
{
  "username": "newuser",
  "email": "user@example.com",
  "password": "securepassword"
}

```



**Login**

* **Endpoint:** `POST /api/auth/login`
* **Body:**
```json
{
  "username": "newuser",
  "password": "securepassword"
}

```


* **Response:** Returns a JWT token required for accessing protected endpoints.

### Job Management

*All Job endpoints require the `Authorization: Bearer <token>` header.*

**Start a New Job**

* **Endpoint:** `POST /api/jobs/start-job`
* **Body:**
```json
{
  "youtubeUrl": "https://www.youtube.com/watch?v=example",
  "videoId": "137",
  "format": "mp4",
  "startTime": "00:00:10",
  "endTime": "00:01:30",
  "resolution": "1920x1080"
}

```



**Check Job Status**

* **Endpoint:** `GET /api/jobs/status/{externalId}`
* **Description:** Returns the current status of the job (e.g., QUEUED, PROCESSING, COMPLETED).

**Download File**

* **Endpoint:** `GET /api/jobs/download/{externalId}`
* **Description:** Downloads the processed file if the job is completed.

**Get Available Formats**

* **Endpoint:** `GET /api/jobs/formats?url={videoUrl}`
* **Description:** Retrieves a list of available video and audio formats for the provided URL.