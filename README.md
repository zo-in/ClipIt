# ClipIt: Universal Media Downloader & Editor

**ClipIt** is a full-stack, distributed web application designed to streamline the process of downloading and editing media from major online platforms including YouTube, X (Twitter), Instagram, and TikTok.

Unlike standard downloaders that simply save files, ClipIt implements a **"Universal Download & Edit" pipeline**. It automates the extraction of video/audio streams and provides immediate post-processing capabilities—allowing users to trim content, resize resolutions (1080p, 4K), and re-encode formats (MP4, MOV, WebM) before saving to their local machine.

---

## Key Features

* **Universal Extraction Engine:** Integrates `yt-dlp` to fetch metadata and raw media streams from diverse platforms.


* **Advanced Transcoding:** Wraps `FFmpeg` to normalize disparate video codecs and apply user-defined edits (trimming, format conversion).


* **Distributed Microservices:** Architected for scalability with distinct services for Gateway, Registry, Authentication, and Job Processing.


* **Asynchronous Job Management:** A state-machine module tracks every request lifecycle: `Queued` → `Downloading` → `Transcoding` → `Completed`.


* **Secure Authentication:** Implements stateless session management using **JWT (JSON Web Tokens)** and **BCrypt** password hashing.


* **Responsive UI:** A multi-page React frontend for monitoring active downloads and managing history.



---

## System Architecture

The project follows a microservices architecture comprising 5 major components running locally:

1. **Frontend UI (React Client):** The user interface for submitting links and configuring download options.


2. **Service Registry (Eureka):** Acts as the central directory for dynamic service discovery.


3. **API Gateway:** The entry point that routes requests and handles initial authentication filtering.


4. **Authentication Service:** Manages user identity, security, and JWT issuance.


5. **Job Processing Service:** The core engine for CPU/GPU-intensive media handling.



---

## Technology Stack

### **Frontend**

* **Framework:** React 19 (via Vite) 


* **Styling:** Tailwind CSS 4 


* **HTTP Client:** Axios 


* **Routing:** React Router DOM 



### **Backend**

* **Language:** Java 17 


* **Framework:** Spring Boot 3.5.x 


* **Build Tool:** Maven 


* **Service Discovery:** Spring Cloud Netflix Eureka 


* **Security:** Spring Security & JWT 


* **Database:** MySQL 8 



### **Core Media Engines**

* **yt-dlp:** For universal media extraction.


* **FFmpeg:** For video processing and transcoding.



---

## Prerequisites

Ensure the following are installed and configured on your system:

1. **Java JDK 17+**
2. **Node.js & npm** (for the frontend)
3. **MySQL Server** (running on port `3306`)
4. **FFmpeg** (Added to system PATH)
5. **yt-dlp** (Added to system PATH)

---

## Configuration

The application requires specific environment variables to connect to the database and locate external tools.

### 1. Database Setup

Create the required databases in MySQL:

```sql
CREATE DATABASE clipit_auth;
CREATE DATABASE clipit_jobs;

```

### 2. Environment Variables

Create a `.env` file or set the following system variables. These are referenced in the `application.properties` files.

```properties
# Database Credentials
DB_USERNAME=root
DB_PASSWORD=your_password

# Security
JWT_SECRET=your_super_secure_random_secret_key

# Tool Paths (Use absolute paths)
# Windows Example: C:\Tools\ffmpeg.exe
# Linux/Mac Example: /usr/bin/ffmpeg
YT_DLP_PATH=/path/to/yt-dlp
FFMPEG_PATH=/path/to/ffmpeg

```

---

## Installation & execution

### Backend (Microservices)

The services **must** be started in the specific order below to ensure service discovery works correctly.

**1. Service Registry**

```bash
cd clipit-microservices/service-registry
./mvnw spring-boot:run
# Wait for initialization on port 8761

```

**2. Auth Service**

```bash
cd clipit-microservices/auth-service
./mvnw spring-boot:run
# Runs on port 8081

```

**3. Job Service**

```bash
cd clipit-microservices/job-service
./mvnw spring-boot:run
# Runs on port 8082

```

**4. API Gateway**

```bash
cd clipit-microservices/api-gateway
./mvnw spring-boot:run
# Runs on port 8080 (Entry Point)

```

### Frontend (Client)

Open a new terminal for the React application.

```bash
cd clipit-frontend
npm install
npm run dev
# Accessible at http://localhost:5173 (usually)

```

---

## API Usage

Once the system is running, the API Gateway (`http://localhost:8080`) handles all requests.

### **Authentication**

* **Register:** `POST /api/auth/register`
```json
{ "username": "user", "email": "user@mail.com", "password": "password" }

```


* **Login:** `POST /api/auth/login`
* *Response:* Returns a **JWT Token**. Include this in the `Authorization` header (`Bearer <token>`) for all Job requests.



### **Job Management**

* **Start Job:** `POST /api/jobs/start-job`
```json
{
  "youtubeUrl": "https://youtu.be/example",
  "format": "mp4",
  "startTime": "00:00:10",
  "endTime": "00:00:45",
  "resolution": "1080p"
}

```


* **Check Status:** `GET /api/jobs/status/{jobId}`
* **Download:** `GET /api/jobs/download/{jobId}`

---

## Documentation

Interactive API documentation (Swagger UI) is available when services are running:

* **Auth API:** `http://localhost:8081/swagger-ui/index.html`
* **Job API:** `http://localhost:8082/swagger-ui/index.html`