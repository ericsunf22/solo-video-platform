---
name: java-springboot-code-review
description: "Perform structured, expert-level code reviews for Java 21 + Spring Boot 3.2.x projects.
  Use this skill whenever the user asks to review, audit, inspect, or improve Java or Spring Boot code —
  including pull requests, single files, classes, controllers, services, repositories, configs, or entire modules.
  Also trigger when the user says things like \"check my code\", \"does this follow best practices\",
  \"is there anything wrong with this\", \"help me refactor\", or pastes Java/Spring code into the chat.
  Covers: Spring Boot 3.2.x idioms, Java 21 features, SOLID/design principles, and Alibaba Java coding standards."
---

# Java + Spring Boot Code Review Skill
 
Produce thorough, actionable code reviews grounded in four authoritative sources:
 
| Source | Focus Area |
|---|---|
| Spring Boot 3.2.x Official Docs | Framework idioms, config, auto-configuration, actuator, security |
| Java 21 Official Features (OpenJDK) | Language features: records, sealed classes, pattern matching, virtual threads, text blocks |
| Design Patterns / SOLID Principles | Architecture, extensibility, cohesion, coupling |
| Alibaba Java Development Manual (Huangshan Edition) | Naming, formatting, exception handling, concurrency, DB access, logging |
 
---
 
## Review Workflow
 
Follow these steps in order:
 
### 1. Understand Context
Before reviewing, identify:
- Is this a Controller / Service / Repository / Config / Entity / Utility / Test?
- Is it Spring MVC (servlet) or Spring WebFlux (reactive)?
- What Java version is targeted? (Assume Java 21 unless stated otherwise)
- What is the apparent purpose of the code?
### 2. Run Through All Six Review Dimensions
Apply **all six dimensions** below to every review. Even if one dimension has no issues, confirm it explicitly.
 
### 3. Output Format
Structure the review as:
 
```
## Code Review: [ClassName / filename]
 
### Summary
One paragraph: what the code does, overall quality impression.
 
### Issues Found
 
#### 🔴 Critical  (must fix — bugs, security, data loss risk)
#### 🟠 Major     (should fix — performance, bad practice, design flaw)
#### 🟡 Minor     (nice to fix — style, naming, readability)
#### 🟢 Positive  (what was done well — always include at least one)
 
### Refactored Snippet (if applicable)
Provide corrected code for the most impactful issues.
 
### Checklist
Quick pass/fail for each dimension.
```
 
---
 
## Six Review Dimensions
 
---
 
### Dimension 1 — Spring Boot 3.2.x Best Practices
 
**Configuration**
- Prefer `@ConfigurationProperties` over `@Value` for grouped settings; always annotate the class with `@Validated` and bind with `@EnableConfigurationProperties`.
- Do not hardcode environment-specific values. Use `application-{profile}.yml` / `application-{profile}.properties` with Spring profiles.
- Externalize secrets — never commit credentials. Reference environment variables or Spring Cloud Config / Vault.
- Prefer YAML over `.properties` for hierarchical configs.
**Bean & Dependency Injection**
- Always use **constructor injection** (not field injection with `@Autowired`). Enables immutability, testability, and avoids null-injection issues.
- Mark injected dependencies `final` in constructor-injected classes.
- Do not use `ApplicationContext.getBean()` at runtime — this breaks IoC.
- Qualify beans with `@Primary` or `@Qualifier` explicitly when multiple implementations exist.
**Auto-Configuration**
- Don't disable auto-configuration classes blindly. Understand what they provide before excluding.
- Custom auto-configuration must be registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Boot 3.x convention — not `spring.factories`).
**Web Layer (MVC / REST)**
- Use `@RestController` = `@Controller` + `@ResponseBody`. Don't combine redundantly.
- Return proper HTTP status codes. Use `ResponseEntity<T>` or `@ResponseStatus` — never return 200 OK for errors.
- Use `@Valid` / `@Validated` on request body/path/query parameters. Handle `MethodArgumentNotValidException` with a global `@RestControllerAdvice`.
- Prefer Problem Details (RFC 9457) for error responses — Spring Boot 3.x supports this via `spring.mvc.problemdetails.enabled=true`.
- Do not put business logic in controllers. Controllers are routing + validation only.
**Data / JPA**
- Use `@Transactional` at the service layer, not repository layer.
- Avoid `FetchType.EAGER` — it leads to N+1 query problems. Use `JOIN FETCH` JPQL or `@EntityGraph`.
- Use Spring Data Projections or DTOs — never return raw `@Entity` objects from REST endpoints (exposes DB structure, causes Jackson serialization issues with lazy proxies).
- Paginate large result sets — never return unbounded `List<Entity>` from endpoints.
**Actuator & Observability**
- Enable health/info/metrics endpoints explicitly. Do not expose all actuator endpoints publicly.
- Integrate Micrometer metrics with appropriate tags. Use `@Timed` or `MeterRegistry` for custom metrics.
- Structured logging (JSON) is preferred in production. Configure via Logback with `logstash-logback-encoder`.
**Security**
- Use Spring Security 6.x lambda DSL (`http.authorizeHttpRequests(auth -> auth...)`). The old `WebSecurityConfigurerAdapter` is removed in Boot 3.x.
- Always configure CSRF, CORS, and session management explicitly for APIs.
- Hash passwords with `BCryptPasswordEncoder` — never store plaintext.
---
 
### Dimension 2 — Java 21 Feature Usage
 
Flag missed opportunities to use Java 21 features, and flag incorrect usage.
 
**Records** (`record`)
- Use `record` for immutable DTOs, value objects, query results, and configuration holders.
- Do not use records for mutable state or JPA entities.
- Compact constructors can include validation logic.
```java
// ✅ Good
public record CreateUserRequest(
    @NotBlank String username,
    @Email String email
) {}
 
// ❌ Bad — using a class with boilerplate for a DTO
public class CreateUserRequest {
    private String username;
    private String email;
    // getters, setters, equals, hashCode...
}
```
 
**Sealed Classes & Pattern Matching**
- Use `sealed interface` / `sealed class` to model closed type hierarchies (e.g., result types, domain events, error types).
- Use `switch` expressions with pattern matching and exhaustiveness checking instead of `instanceof` chains.
```java
// ✅ Good
sealed interface PaymentResult permits Success, Failure, Pending {}
 
String describe(PaymentResult result) {
    return switch (result) {
        case Success s  -> "Paid: " + s.amount();
        case Failure f  -> "Failed: " + f.reason();
        case Pending p  -> "Pending";
    };
}
```
 
**Text Blocks**
- Use `"""..."""` text blocks for multi-line strings: SQL, JSON, HTML, XML templates. Never string-concatenate multi-line content.
**Virtual Threads (Project Loom)**
- Spring Boot 3.2 supports virtual threads via `spring.threads.virtual.enabled=true`.
- Virtual threads are ideal for I/O-bound workloads. Do not use them for CPU-intensive computation.
- Do not use `ThreadLocal` carelessly with virtual threads — virtual threads are cheap but `ThreadLocal` state is still per-carrier-thread in some implementations.
- Avoid mixing virtual threads with `synchronized` blocks on shared monitors at high concurrency — prefer `ReentrantLock`.
**Pattern Matching for `instanceof`**
- Replace `if (x instanceof Foo) { Foo f = (Foo) x; }` with `if (x instanceof Foo f)`.
**Sequenced Collections**
- Use `SequencedCollection`, `SequencedSet`, `SequencedMap` interfaces (Java 21) when ordered access to first/last element is needed.
---
 
### Dimension 3 — SOLID Principles
 
**S — Single Responsibility Principle**
- Each class should have one reason to change.
- Flag: God classes, services with 10+ methods spanning unrelated concerns, controllers that do DB access.
- Fix: Extract cohesive responsibilities into dedicated classes.
**O — Open/Closed Principle**
- Classes should be open for extension, closed for modification.
- Flag: Large `if-else` / `switch` blocks that must be modified every time a new variant is added.
- Fix: Use Strategy pattern, polymorphism, or `sealed` hierarchies + pattern matching.
**L — Liskov Substitution Principle**
- Subtypes must be substitutable for their base types without altering correctness.
- Flag: Overridden methods that throw unexpected exceptions, return `null` when base type returns a value, or weaken postconditions.
**I — Interface Segregation Principle**
- Prefer narrow, role-specific interfaces over fat interfaces.
- Flag: Interfaces with 10+ methods where most implementors leave many as `throw new UnsupportedOperationException()`.
- Fix: Split into focused interfaces.
**D — Dependency Inversion Principle**
- Depend on abstractions (interfaces), not concrete implementations.
- Flag: `new ConcreteService()` inside business logic; direct `static` utility calls where the behavior should be injectable/testable.
- Fix: Inject via interface; use Spring beans.
**Additional Principles (from refactoring.guru)**
- **DRY** (Don't Repeat Yourself): Flag copy-pasted logic. Extract shared code to utilities or base classes.
- **KISS**: Flag over-engineered solutions. Prefer simple, readable code over clever code.
- **Composition over Inheritance**: Prefer interface + delegation over deep inheritance trees.
- **Encapsulate What Varies**: Isolate the parts of code that change frequently behind abstractions.
---
 
### Dimension 4 — Alibaba Java Development Manual (Huangshan Edition)
 
#### Naming Conventions
- Class names: `UpperCamelCase`. Abstract classes: prefix `Abstract`. Exception classes: suffix `Exception`. Test classes: suffix `Test`.
- Method/variable names: `lowerCamelCase`. Constants: `UPPER_SNAKE_CASE` in `static final` fields.
- Boolean variables: no `is` prefix for fields (e.g., use `deleted`, not `isDeleted`) — POJO getters with `is` prefix can confuse serialization frameworks.
- Package names: all lowercase, no underscores.
- Do not use `l` (lowercase L), `O` (uppercase O), `I` (uppercase i) as variable names.
- Meaningful names only: no `a`, `b`, `tmp`, `data`, `info` without context.
- Enum class names: suffix `Enum`. Enum member names: `UPPER_SNAKE_CASE`.
#### Code Style
- Line length: max **120 characters**. Indent with **4 spaces** (no tabs).
- Braces: always use, even for single-line `if/else/for/while`.
- No trailing whitespace. One blank line between methods.
- Wrap long method chains — one call per line after the first.
#### OOP Rules
- Do not override `clone()` — prefer copy constructors or factory methods.
- Use `String.valueOf()` not `"" + obj` for string conversion.
- Use `equals` on constants/literals first to avoid NPE: `"expected".equals(variable)`.
- Use `Objects.equals()` for null-safe comparison.
- Use `BigDecimal` for financial/precise decimal arithmetic — never `float`/`double`.
- Do not modify collections while iterating (use iterator's `remove()` or collect to new list).
#### Collections
- Initialize collections with estimated capacity to avoid rehashing: `new ArrayList<>(16)`, `new HashMap<>(16)`.
- Use `Collections.unmodifiableList()` or `List.of()` for read-only collections returned from APIs.
- Do not use raw types — always parameterize generics.
- `toArray(new T[0])` is preferred over `toArray(new T[size])` for defensive copying.
#### Exception Handling
- **Never** swallow exceptions with empty `catch` blocks. At minimum log the exception.
- **Never** use exceptions for flow control.
- Catch specific exceptions — avoid catching `Exception` or `Throwable` unless at the top level.
- Wrap and re-throw checked exceptions as runtime exceptions at service boundaries.
- Custom exceptions: checked for caller-recoverable conditions; unchecked (`RuntimeException`) for programming errors.
- Always include the original exception as cause: `new ServiceException("msg", e)`.
- Release resources in `finally` block or use try-with-resources (`try (Resource r = ...)`).
- Log exceptions at the point where they are **caught and handled**, not at every rethrow.
#### Concurrency
- Thread names must be meaningful: use `ThreadFactory` or `new Thread(runnable, "meaningful-name")`.
- Always use thread pools (`ExecutorService`) — never `new Thread().start()` in production code.
- Define and document thread pool parameters explicitly — do not use `Executors.newFixedThreadPool()` which uses an unbounded queue.
- Use `volatile` for simple visibility, `AtomicXxx` for compound operations, `synchronized` / `Lock` for compound check-act sequences.
- Acquire multiple locks in consistent order to avoid deadlock.
- Prefer `ConcurrentHashMap` over `Collections.synchronizedMap(new HashMap<>())`.
- `SimpleDateFormat` is not thread-safe — use `java.time` API (Java 8+) exclusively.
#### Logging
- Use SLF4J facade — never `System.out.println` or `e.printStackTrace()` in production code.
- Do not concatenate strings in log calls — use parameterized form: `log.debug("User {} logged in", userId)`.
- Use appropriate log levels: ERROR for unrecoverable, WARN for recoverable anomalies, INFO for business events, DEBUG for diagnostics.
- Do not log sensitive data: passwords, tokens, card numbers, personal identifiers.
- Log at entry/exit of service methods for key business operations (INFO level).
#### Database / MyBatis / JPA
- Table names: `lower_snake_case`. Column names: `lower_snake_case`.
- Never use `SELECT *` — enumerate required columns.
- Avoid full table scans. Ensure indexed columns are used in `WHERE` clauses.
- Use parameterized queries — never string-concatenated SQL (SQL injection risk).
- Large batch inserts: use batch APIs, not individual `INSERT` in a loop.
- Do not perform calculations on indexed columns in `WHERE` (defeats index usage): use `WHERE create_time > ?` not `WHERE YEAR(create_time) = ?`.
#### Unit Testing
- Follow **AIR** principle: Automatic (no manual verification), Independent (no test depends on another), Repeatable (same result every run).
- One logical assertion per test method.
- Test method naming: `methodName_scenario_expectedBehavior`.
- Mock external dependencies — unit tests must not hit real databases or network.
- Test coverage target: core business logic ≥ 70% line coverage.
---
 
### Dimension 5 — Code Smells & Common Anti-Patterns
 
Flag any of these on sight:
 
| Anti-Pattern | Description |
|---|---|
| God Class | One class doing everything — split it |
| Anemic Domain Model | Domain objects with no behavior, only getters/setters |
| Service Locator | Using `ApplicationContext` to pull beans — invert the dependency |
| Magic Numbers/Strings | Unnamed literals in logic — extract to named constants |
| Primitive Obsession | Using `String` for email/phone/ID — create value types / records |
| Long Parameter Lists | >4 parameters — introduce a parameter object / record |
| Shotgun Surgery | One change requires edits to many classes — consolidate responsibility |
| Dead Code | Unused methods, fields, imports — remove |
| Leaking Internals | Returning mutable internal state, exposing `List` field directly |
| Overuse of `Optional` | `Optional` as field type, method parameter, or in collections — use only as return type |
 
---
 
### Dimension 6 — Security Checklist
 
Always verify:
 
- [ ] No hardcoded credentials or secrets in source code
- [ ] All user inputs validated and sanitized (including path variables, query params, headers)
- [ ] SQL queries are parameterized (no string concatenation)
- [ ] Sensitive data is not logged
- [ ] HTTP responses do not leak stack traces or internal details
- [ ] Authentication and authorization applied to all non-public endpoints
- [ ] Dependencies checked for known CVEs (mention if `spring-boot-starter-parent` BOM is used — it manages secure versions)
- [ ] File upload endpoints restrict file type and size
- [ ] Rate limiting or throttling is in place for public-facing endpoints
---
 
## Quick Reference — Common Fixes
 
### Constructor Injection (not field injection)
```java
// ❌ Avoid
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
}
 
// ✅ Prefer
@Service
@RequiredArgsConstructor  // Lombok, or write constructor manually
public class OrderService {
    private final OrderRepository orderRepository;
}
```
 
### Exception Handling
```java
// ❌ Swallowed exception
try {
    process();
} catch (Exception e) {
    // nothing
}
 
// ✅ Log and rethrow or handle
try {
    process();
} catch (SpecificException e) {
    log.error("Failed to process order {}: {}", orderId, e.getMessage(), e);
    throw new ServiceException("Order processing failed", e);
}
```
 
### Global Error Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
 
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setDetail(ex.getBindingResult().getAllErrors()
                .stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", ")));
        return problem;
    }
}
```
 
### BigDecimal for Money
```java
// ❌ Never
double price = 0.1 + 0.2; // 0.30000000000000004
 
// ✅ Always
BigDecimal price = new BigDecimal("0.1").add(new BigDecimal("0.2"));
```
 
### Parameterized Logging
```java
// ❌ String concatenation (wasteful even if log level is off)
log.debug("Processing user: " + user.getId());
 
// ✅ Lazy evaluation
log.debug("Processing user: {}", user.getId());
```
 
---
 
## Severity Definitions
 
| Level | Criteria |
|---|---|
| 🔴 Critical | Bug, security vulnerability, data corruption risk, exception swallowing hiding real failures |
| 🟠 Major | Performance issue, SOLID violation causing maintainability risk, missing transaction management, N+1 queries |
| 🟡 Minor | Naming deviation, style inconsistency, missed Java 21 opportunity, could-be-cleaner logic |
| 🟢 Positive | Well-structured code, good test coverage, correct use of framework, thoughtful design |
 
---
 
## Notes on Tone
 
- Be constructive, not critical. Frame issues as improvements, not failures.
- Explain *why* something is problematic, not just *what* to fix.
- For junior developers: provide more explanation and context.
- For senior developers: be concise — they understand the reasoning.
- Always find something positive to note. Even imperfect code usually has redeeming qualities.