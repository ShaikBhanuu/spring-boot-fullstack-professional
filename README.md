# DeepThought HRMS

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.4.3-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-blue)
![Redis](https://img.shields.io/badge/Redis-Cache-red)

A Worker Attendance and Overtime Settlement backend built for the construction industry.
Site supervisors can track who is on-site in real time. Payroll gets accurate overtime numbers at month-end. Workers get notified only after their settlement is confirmed in the database.

---

## Forked From
[amigoscode/spring-boot-fullstack-professional](https://github.com/amigoscode/spring-boot-fullstack-professional)
Chosen because it has a clean Spring Boot + JPA + PostgreSQL base with minimal boilerplate — easy to extend without rewriting from scratch.

---

## Tech Stack
- Java 17, Spring Boot 2.4.3
- Hibernate / JPA
- Supabase (PostgreSQL, Transaction Pooler port 6543)
- Redis (active worker caching)
- HikariCP (connection pooling)

---

## How to Run

**1. Start Redis**
```bash
redis-server
```

**2. Update your Supabase credentials in `application.properties`**
```properties
spring.datasource.url=jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:6543/postgres?sslmode=require
spring.datasource.username=postgres.YOUR_PROJECT_ID
spring.datasource.password=YOUR_PASSWORD
```

**3. Run the app**
```bash
mvn spring-boot:run
```
Runs on `http://localhost:8080`

---

## API Endpoints

### Workers
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/workers` | Create a worker |
| GET | `/api/workers` | List all workers |

### Sites
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/sites` | Create a site |
| GET | `/api/sites` | List all sites |

### Attendance
| Method | Endpoint | Description                                          |
|--------|----------|------------------------------------------------------|
| POST | `/api/attendance/clock-in` | Clock in a worker                                    |
| POST | `/api/attendance/clock-out` | Clock out a worker                                   |
| GET | `/api/attendance/active` | All currently clocked-in workers - served from Redis |
| GET | `/api/attendance/log?workerId=1&from=2026-05-01T00:00:00&to=2026-05-31T23:59:59` | Attendance history with pagination                   |

### Overtime
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/overtime/summary/{workerId}?month=2026-05` | Monthly overtime summary |
| POST | `/api/overtime/settle/{workerId}?month=2026-04` | Settle overtime for a past month |

## Postman Collection
Import `DeepThought-HRMS.postman_collection.json` from the root of this repo into Postman to test all endpoints.

File → Import → select the file.

---

## AI Tools Used
I used Claude (Anthropic) - for thinking through schema design, Redis strategy, ticket debugging, and reviewing business logic. I wrote all the code myself and can explain every decision.

---

## Design Decisions

**OvertimeEntry is a separate table:**
Overtime is a financial record with its own settlement lifecycle. Mixing it into AttendanceLog would make payroll queries messy and make it harder to enforce settlement rules cleanly.

**Redis only caches active workers:**
Not everything - just the one thing that needs to be fast and real-time. If Redis goes down, clock-in and clock-out still work. The system degrades gracefully, it doesn't crash.

**SMS fires after the DB commits, not during:**
This was the core fix for LF-204. If the settlement transaction rolls back, the worker should never have received a message. I used Spring's `@TransactionalEventListener(AFTER_COMMIT)` for this.

**External API call happens before the transaction opens:**
For LF-205, a slow government API was holding a DB connection open inside `@Transactional`. Moved it outside so it fetches first, then the transaction opens only for DB work.

**HikariCP max-lifetime set to 4 minutes:**
Supabase silently kills idle connections after 5 minutes. Setting `max-lifetime` shorter means HikariCP retires the connection before Supabase kills it, preventing dead connections from being handed out.

---

## What I'd Do Differently With More Time
- Replace `redisTemplate.keys()` with a Redis Set for active worker tracking — `KEYS *` is O(n) and blocks Redis under load
- Add Spring Security with JWT for authentication
- Move credentials to environment variables instead of properties files
- Write integration tests for overtime edge cases — the 60-hour cap and the 1.5x vs 2x rate boundary

---

## Ticket Fixes

🟢 **LF-201 — CORS**
Created `CorsConfig.java` with a `CorsFilter` bean. CORS now runs before Spring Security touches the request, so OPTIONS preflight passes without auth headers. Allowed origins are in `application.properties`, not hardcoded.

🟡 **LF-202 — Redis crash on startup**
Set 2-second timeouts on Redis connect and read. Added error handling in the service layer so Redis failures don't kill the request. App starts and serves requests even when Redis is completely offline.

🔴 **LF-203 — Slow attendance endpoint**
Added `Pageable` to the repository query. Added `@EntityGraph` to fetch Worker and Site in one JOIN instead of a separate query per record. Default page size is 20, max is 100.

🟠 **LF-204 — Partial settlement and wrong SMS**
Wrapped the entire settlement in one `@Transactional`. SMS moved to `@TransactionalEventListener(AFTER_COMMIT)`. If the DB rolls back, no SMS goes out. No partial state, no premature messages.

🔵 **LF-205 — Connection exhaustion on staging**
HikariCP tuned with `max-lifetime=240s` and `keepalive-time=60s` for Supabase. External wage API call moved outside the transaction. Staging-specific settings isolated in `application-staging.properties`.

## Author
**MoulaBhanu Shaik**
- GitHub: [ShaikBhanuu](https://github.com/ShaikBhanuu)
- LinkedIn: [shaik-moulabhanu](https://linkedin.com/in/shaik-moulabhanu)
- Email: shaikmoulabhanu@gmail.com