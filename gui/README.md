# Toolbox Desktop

Tauri 2 + React + TypeScript + Java JAR sidecar skeleton.

## Requirements

- Node.js 20+
- Rust stable
- Tauri CLI
- JDK 21 for building/running the example backend
- Maven 3.9+

## Development

Build the Java backend:

```bash
cd backend
mvn package
```

Copy the generated JAR:

```bash
mkdir -p ../src-tauri/resources/backend
cp target/toolbox-backend-1.0.0.jar ../src-tauri/resources/backend/toolbox-backend.jar
```

Install frontend dependencies:

```bash
cd ../frontend
npm install
```

Run Tauri:

```bash
npm run tauri:dev
```

The Tauri process starts the Java JAR automatically, waits for `/api/system/info`, and then opens the React UI.

## Important

The sample Java backend listens on a fixed development port `47831`.
For production, change the backend to support `--server.port=0` or pass a free port selected by Tauri.

The skeleton intentionally keeps the Java JAR as a Tauri resource instead of assuming the JAR itself is executable. For a no-Java-installed distribution, bundle a private JRE and point `backend.javaPath` to it in `src-tauri/tauri.conf.json`.
