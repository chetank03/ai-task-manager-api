# Task Manager API

A personal task manager REST API built with Java 17, Spring Boot 3.4, Maven, and an H2 in-memory database. Includes a minimal single-page frontend and AI-powered endpoints for suggesting, summarizing, and breaking down tasks.

---

## Setup

**Prerequisites**

- Java 17+
- No other dependencies — Maven downloads everything automatically via the wrapper.

**Clone / unzip the project, then from the project root:**

```bash
chmod +x mvnw   # first time only on Unix/macOS
```

---

## Run

```bash
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**.

Open **http://localhost:8080** in a browser to use the frontend.

The H2 console (useful for inspecting the database) is available at **http://localhost:8080/h2-console**
(JDBC URL: `jdbc:h2:mem:taskdb`, user: `sa`, password: leave blank).

---

## Run Tests

```bash
./mvnw test
```

All tests run without any external dependencies or API keys.

---

## API Endpoints

| Method | Path                    | Description                                      |
|--------|-------------------------|--------------------------------------------------|
| POST   | `/tasks`                | Create a task                                    |
| GET    | `/tasks`                | List all tasks                                   |
| GET    | `/tasks/{id}`           | Get a single task                                |
| PUT    | `/tasks/{id}`           | Replace a task                                   |
| DELETE | `/tasks/{id}`           | Delete a task                                    |
| POST   | `/tasks/suggest`        | AI-powered task suggestion from plain English    |
| POST   | `/tasks/{id}/summarize` | AI-powered plain-language summary of a task      |
| POST   | `/tasks/{id}/breakdown` | AI-powered suggested subtasks for a complex task |

### Task fields

| Field         | Type                          | Required |
|---------------|-------------------------------|----------|
| `title`       | string                        | yes      |
| `description` | string                        | no       |
| `dueDate`     | date (`YYYY-MM-DD`)           | no       |
| `priority`    | enum: `LOW`, `MEDIUM`, `HIGH` | yes      |
| `status`      | enum: `TODO`, `IN_PROGRESS`, `DONE` | yes |

---

## AI-Powered Endpoints

### `POST /tasks/suggest`

### What it does

Accepts a plain-English description of something you need to do and returns a fully structured task suggestion (title, description, dueDate, priority, status). The suggestion is **not saved** — it is returned for review so the user can optionally save it via `POST /tasks`.

### Design: two implementations behind one interface

`AiSuggestionService` is an interface with two implementations selected at startup:

| Condition | Implementation | Behaviour |
|-----------|---------------|-----------|
| `GEMINI_API_KEY` env var **set** | `GeminiSuggestionService` | Calls Gemini `gemini-2.5-flash` via `generateContent` |
| `GEMINI_API_KEY` env var **absent** | `MockAiSuggestionService` | Keyword-based local analysis (no network call) |

**Why this approach for a take-home?**
Reviewers can run the full app — including the suggest endpoint — immediately without any external credentials. The mock gives a meaningfully derived response (it extracts urgency keywords, relative date references, and cleans up filler phrases from the input) rather than a static stub, which demonstrates the feature intent. Swapping to the real provider is purely a config change. In tests, the interface is simply mocked with `@MockBean`.

### Example request

```bash
curl -X POST http://localhost:8080/tasks/suggest \
  -H "Content-Type: application/json" \
  -d '{"text": "remind me to submit the quarterly report before Friday"}'
```

### Example response

```json
{
  "id": null,
  "title": "Submit the quarterly report before Friday",
  "description": "Auto-suggested from: \"remind me to submit the quarterly report before Friday\"",
  "dueDate": "2025-01-10",
  "priority": "MEDIUM",
  "status": "TODO"
}
```

---

### `POST /tasks/{id}/summarize`

Returns a short plain-language summary of an existing task. This is useful in the UI when a task has enough detail that the user wants a quick explanation without editing the task itself.

```bash
curl -X POST http://localhost:8080/tasks/1/summarize
```

Example response:

```json
{
  "taskId": 1,
  "summary": "The medium-priority project is due on 2026-05-08 and is still marked TODO."
}
```

### `POST /tasks/{id}/breakdown`

Returns a suggested list of subtasks for an existing task. The endpoint does not create those subtasks automatically; the frontend shows them first and lets the user choose whether to create them.

```bash
curl -X POST http://localhost:8080/tasks/1/breakdown
```

Example response:

```json
{
  "taskId": 1,
  "subtasks": [
    "Clarify the expected deliverable",
    "Collect the required notes or files",
    "Draft the first version",
    "Review and revise",
    "Submit the final version"
  ]
}
```

---

## Design Decisions

**DTOs instead of exposing the JPA entity directly**

The controller accepts `TaskRequest` and returns `TaskResponse` instead of exposing `Task` directly. This keeps persistence details out of the API contract, gives validation a clear home, and makes it safer to change the entity later without accidentally changing the JSON shape reviewers or clients depend on.

**Mock AI as a real keyword parser**

`MockAiSuggestionService` is intentionally more than a static fallback. It extracts simple dates, priority hints, status hints, and task titles from natural language. That means the app still demonstrates the AI workflow with no credentials, while the real Gemini implementation can be enabled by configuration.

**AI suggestions are reviewed before persistence**

`/tasks/suggest` returns a structured task but does not save it automatically. The AI output is treated as a draft because generated titles, dates, and priorities may be wrong. The user reviews the result first, then saves it through the normal `POST /tasks` path.

**Integration tests use `@DirtiesContext`**

The CRUD integration tests assert concrete IDs and end-to-end behavior through the Spring context. `@DirtiesContext` resets the in-memory H2 state between methods so the tests stay predictable and independent instead of relying on execution order.

---

## Using a Real Gemini Key

Set the environment variable before running:

```bash
export GEMINI_API_KEY=<your_gemini_api_key>
./mvnw spring-boot:run
```

Or pass it inline:

```bash
GEMINI_API_KEY=<your_gemini_api_key> ./mvnw spring-boot:run
```

> ⚠️ **Never commit an API key to source control.** The key is read exclusively from the environment and is not present in any source file or `application.properties`.

---

## Project Structure

```
src/main/java/com/taskmanager/
├── TaskManagerApplication.java
├── config/
│   └── AiServiceConfig.java          # selects AI impl based on env var
├── controller/
│   └── TaskController.java           # all REST endpoints
├── dto/
│   ├── TaskRequest.java              # inbound DTO (validated)
│   ├── TaskResponse.java             # outbound DTO
│   └── SuggestRequest.java           # POST /tasks/suggest body
├── entity/
│   └── Task.java                     # JPA entity
├── enums/
│   ├── Priority.java
│   └── Status.java
├── exception/
│   ├── TaskNotFoundException.java
│   └── GlobalExceptionHandler.java   # 404 / 400 / 500 error shaping
├── repository/
│   └── TaskRepository.java
└── service/
    ├── TaskService.java
    └── ai/
        ├── AiSuggestionService.java      # interface
        ├── MockAiSuggestionService.java  # keyword-based fallback
        └── GeminiSuggestionService.java  # real Gemini call

src/main/resources/
├── application.properties
└── static/
    └── index.html                    # minimal frontend

src/test/java/com/taskmanager/
├── service/TaskServiceTest.java                   # unit tests (Mockito)
├── service/MockAiSuggestionServiceTest.java       # fallback AI parser tests
├── controller/TaskControllerIntegrationTest.java  # full-context integration tests
└── ai/AiSuggestionControllerTest.java             # AI endpoint with @MockBean
```
