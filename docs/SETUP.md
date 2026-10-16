# Setup Instructions

## Prerequisites

Before starting, ensure you have the following installed:

- **Java 17** or higher: [Download JDK](https://www.oracle.com/java/technologies/downloads/)
- **Node.js 18** or higher: [Download Node.js](https://nodejs.org/)
- **Maven 3.6** or higher: [Download Maven](https://maven.apache.org/download.cgi)

Verify installations:

```bash
java -version
node -v
npm -v
mvn -v
```

## Repository Setup

1. Clone or download the repository
2. Navigate to the project directory:
   ```bash
   cd "MatchMind AI — Explainable Football Match Intelligence."
   ```

## Backend Setup

### 1. Navigate to Backend Directory

```bash
cd backend
```

### 2. Install Dependencies

```bash
mvn clean install
```

This will download all required dependencies and compile the project.

### 3. Configure Environment (Optional)

For demo mode (default), no configuration is needed.

To enable Gemini integration:

1. Create a `.env` file in the backend directory:
   ```bash
   touch .env
   ```

2. Add your credentials:
   ```env
   GEMINI_API_KEY=your-replacement-key
   GEMINI_MODEL=gemini-2.0-flash
   AI_MODE=GEMINI
   ```

3. Keep the key backend-only and do not commit the `.env` file. The backend calls Gemini over HTTPS and executes the evidence tools locally.

### 4. Start the Backend

```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

**Expected Output:**
```
Started MatchMindApplication in X.XXX seconds
Dataset loaded successfully: 12 matches, 1262 events, 10 teams, 180 players
```

### 5. Verify Backend

Open a new terminal and test the health endpoint:

```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "aiMode": "DEMO",
  "matchesLoaded": 12
}
```

## Frontend Setup

### 1. Navigate to Frontend Directory

Open a new terminal:

```bash
cd frontend
```

### 2. Install Dependencies

```bash
npm install
```

This will download all required packages.

### 3. Start the Frontend

```bash
npm run dev
```

The frontend will start on `http://localhost:5173`

**Expected Output:**
```
VITE v5.4.21  ready in 150 ms
➜  Local:   http://localhost:5173/
```

### 4. Access the Application

Open your browser and navigate to:
```
http://localhost:5173
```

## Dataset

The synthetic dataset is included in the `dataset/` directory. It will be automatically loaded when the backend starts.

**Dataset Location:** `../dataset` (relative to backend)

**Files:**
- `matches.csv` - Match metadata
- `events.csv` - Event log (1,262 events)
- `teams.csv` - Team information (10 teams)
- `players.csv` - Player information (180 players)
- `event_types.csv` - Event type definitions
- `schema.json` - Data schema
- `validation_report.json` - Validation results
- `fixtures/` - Individual match files

## Troubleshooting

### Backend Fails to Start

**Problem:** `Gemini HTTP 401/403` or connection refused

**Solution:** Verify `GEMINI_API_KEY`, `GEMINI_MODEL`, internet access, and Google API quotas. Use `AI_MODE=DEMO` when Gemini is unavailable.

**Problem:** `Dataset loading failed`

**Solution:** Ensure the `dataset/` directory is in the correct location (parent directory of backend)

### Frontend Fails to Start

**Problem:** `Module not found`

**Solution:** Run `npm install` in the frontend directory

**Problem:** Port 5173 already in use

**Solution:** Either:
1. Stop the process using port 5173
2. Or modify `vite.config.ts` to use a different port

### API Calls Fail

**Problem:** CORS errors in browser console

**Solution:** Ensure the backend is running and CORS is configured correctly in `application.properties`

**Problem:** `Network Error`

**Solution:** Check that the backend is running on port 8080

## Building for Production

### Backend

```bash
cd backend
mvn clean package
```

The JAR file will be created at: `target/matchmind-backend-1.0.0.jar`

Run the JAR:
```bash
java -jar target/matchmind-backend-1.0.0.jar
```

### Frontend

```bash
cd frontend
npm run build
```

The build artifacts will be in the `dist/` directory

Deploy the `dist/` directory to any web server (nginx, Apache, AWS S3, etc.)

## Development

### Backend Hot Reload

For development with hot reload, use Spring Boot DevTools (included in spring-boot-starter-web):

```bash
mvn spring-boot:run
```

Changes to Java files will trigger automatic recompilation.

### Frontend Hot Reload

Vite provides hot module replacement by default:

```bash
npm run dev
```

Changes to React/TypeScript files will be reflected immediately.

## Testing

### Backend Tests

```bash
cd backend
mvn test
```

### Frontend Tests

```bash
cd frontend
npm test
```

## Clean Build

To clean all build artifacts:

```bash
# Backend
cd backend
mvn clean

# Frontend
cd frontend
rm -rf node_modules dist
```

## Next Steps

1. Explore the match dashboard
2. Click on a match to see details
3. Navigate through the tabs (Overview, Timeline, Turning Points, AI Insights)
4. Try the Fan and Analyst modes
5. Ask questions in the Q&A section
6. Upload a CSV file on the Upload page

## Support

For issues or questions:
- Check the [API Documentation](API.md)
- Review the [README](../README.md)
- Check application logs for error messages
