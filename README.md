
# ClipIt - Universal Media Downloader & Editor

**ClipIt** is a full-stack microservices-based application designed to streamline the process of downloading and editing media from online platforms like YouTube, X (Twitter), Instagram, and TikTok. Unlike standard downloaders, ClipIt offers a "Universal Download & Edit" pipeline that allows users to extract, trim, resize, and re-encode media before saving it.

## Features

* **Universal Extraction:** Fetches metadata and raw media streams using `yt-dlp`.


* **Advanced Processing:** Transcodes, trims, and edits video using `FFmpeg`.


* **Microservices Architecture:** Scalable and modular design ensuring responsive user experience.


* **Secure Authentication:** User management secured with BCrypt and stateless JWT authentication.


* **Job Management:** Asynchronous state-machine tracks request lifecycles (Queued → Downloading → Transcoding → Completed).



## Architecture

The backend is built with **Java 17** and **Spring Boot 3.x**, managed by a **Maven** multi-module project.

### Core Microservices

| Service | Port | Description |
| --- | --- | --- |
| **Service Registry** | `8761` | Eureka Server for service discovery. |
| **API Gateway** | `8080` | Entry point. Routes requests to internal services and handles initial auth filtering. |
| **Auth Service** | `8081` | Manages user registration, login, and JWT generation. |
| **Job Service** | `8082` | Core engine for downloading and processing media using local binaries. |

### Frontend

* **Tech:** React JS, React Router, Axios, Tailwind CSS.


* *(Note: Frontend code resides in a separate client module not detailed in this backend repository).*

## Technology Stack

* **Backend:** Java 17, Spring Boot 3, Spring Cloud Gateway, Netflix Eureka.


* **Security:** Spring Security, JWT (JSON Web Tokens).


* **Database:** MySQL (Relational storage for users and job history).


* **Media Engines:**
* `yt-dlp`: Media extraction.


* `ffmpeg`: Video transcoding and editing.





## Prerequisites

Before running the application, ensure you have the following installed:

1. **Java 17+** 
2. **Maven** (Wrapper scripts included)
3. **MySQL Server** running on port `3306`
4. **Local Binaries:**
* [yt-dlp](https://github.com/yt-dlp/yt-dlp)
* [FFmpeg](https://ffmpeg.org/download.html)



## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/clipit.git
cd clipit

```

### 2. Configure Environment Variables

Create a `.env` file in the root directory (or set system environment variables) to match your local setup. This is **critical** for database access and tool paths.

**Example `.env` file:**

```properties
# Database Credentials
DB_USERNAME=root
DB_PASSWORD=your_password

# Job Service Tools (Adjust paths to where you installed these tools)
# Windows Example:
YT_DLP_PATH=C:\\Tools\\yt-dlp.exe
FFMPEG_PATH=C:\\Tools\\ffmpeg\\bin\\ffmpeg.exe

# Mac/Linux Example:
# YT_DLP_PATH=/usr/local/bin/yt-dlp
# FFMPEG_PATH=/usr/bin/ffmpeg

# Security
JWT_SECRET=your_very_long_secure_secret_key_here

```

### 3. Database Setup

Log in to your MySQL server and create the required databases:

```sql
CREATE DATABASE clipit_auth;
CREATE DATABASE clipit_jobs;

```

*The services are configured to automatically create tables (`ddl-auto=update`).*

## Running the Application

You must start the services in the following order:

1. **Service Registry**
```bash
cd service-registry
./mvnw spring-boot:run

```


*Wait for it to start on port 8761.*
2. **Auth Service**
```bash
cd auth-service
./mvnw spring-boot:run

```


3. **Job Service**
```bash
cd job-service
./mvnw spring-boot:run

```


4. **API Gateway**
```bash
cd api-gateway
./mvnw spring-boot:run

```



## API Endpoints

All external requests should go through the **API Gateway** (`http://localhost:8080`).

### Authentication (`/api/auth`)

* `POST /register`: Register a new user.
* Body: `{ "username": "user", "email": "email@test.com", "password": "pass" }`


* `POST /login`: Login and receive a JWT.
* Body: `{ "username": "user", "password": "pass" }`



### Jobs (`/api/jobs`)

*Requires `Authorization: Bearer <token>` header.*

* `POST /start-job`: Submit a URL for processing.
* Body:
```json
{
  "youtubeUrl": "https://youtu.be/example",
  "videoId": "137",
  "audioId": "140",
  "format": "mp4"
}

```




* `GET /status/{externalId}`: Check the status of a job (QUEUED, PROCESSING, COMPLETED).
* `GET /download/{externalId}`: Download the final processed file.
* `GET /formats?url=...`: Retrieve available video/audio formats for a specific URL.

## Project Structure

```plaintext
clipit/
├── .env                     # Environment configuration
├── pom.xml                  # Parent Maven POM
├── api-gateway/             # Gateway Service code
├── auth-service/            # Authentication Service code
├── job-service/             # Job Processing Service code
│   └── downloads/           # Temporary download storage
│   └── outputs/             # Final processed files storage
└── service-registry/        # Eureka Server code

```