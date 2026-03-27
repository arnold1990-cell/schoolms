# schoolms

A full-stack School Management System focused on exam lifecycle, marks entry, grading, results, reporting, notifications, and analytics.

## Stack
- Backend: Spring Boot 3 + Java 21 + Maven + Spring Security JWT + JPA + Flyway + PostgreSQL
- Frontend: React + TypeScript + Vite

## Access model
- Only `ADMIN` and `TEACHER` users can authenticate.
- No student or parent authentication/portal.
- Students are managed records only.

## Backend structure
`backend/src/main/java/com/schoolms/`
- `config`, `security`, `auth`, `user`, `teacher`, `student`, `classmanagement`, `subject`, `academicsession`, `term`, `exam`, `marks`, `grading`, `result`, `report`, `notification`, `analytics`, `common`

## Quick start

### 1) PostgreSQL
Create DB:
```sql
CREATE DATABASE schoolms;
```

### 2) Run backend
```bash
cd backend
mvn spring-boot:run
```

Default seeded admin:
- email: `admin@schoolms.com`
- password: `Admin123!`

### 3) Run frontend
```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

## Key APIs
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/admin/dashboard`
- `GET /api/teacher/dashboard`
- `GET/POST /api/teachers`
- `GET/POST/PUT /api/students`
- `GET/POST /api/classes`
- `GET/POST /api/subjects`
- `GET/POST /api/exams`, `PUT /api/exams/{id}`
- `GET /api/marks/exam/{examId}`, `POST /api/marks`, `PUT /api/marks/{id}`
- `GET /api/results/student/{studentId}`
- `GET /api/results/class/{classId}`
- `GET /api/reports/class/{classId}/pdf`
- `GET /api/reports/class/{classId}/excel`
- `GET /api/notifications`, `PUT /api/notifications/{id}/read`
- `GET /api/analytics/overview`

## Notes
- JWT is stateless and sent via `Authorization: Bearer <token>`.
- Frontend persists token as `accessToken`.
- Report generation includes PDF/Excel scaffolding.
