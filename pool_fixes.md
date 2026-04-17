# Database Connection and Resource Management Audit

## 1. Identified Issues

### Issue 1: Unsafe EntityManager Lifecycle and Missing Finally Blocks
- **File name:** `SlsTblSaleOrderDAO.java` (Pattern repeated across 76+ DAO files including `SHIntegeration.java`, `CfgTblCustomerDAO.java`, etc.)
- **Class name:** `SlsTblSaleOrderDAO`
- **Method name:** `getAllSaleOrder()`, `addNewSaleOrder()`, `searchSaleOrderDetail()`, and almost all repository methods.
- **Risk level:** High
- **Exact problematic code or pattern:**
  ```java
  EntityManager entityManager = getEntityManager();
  entityManager.getTransaction().begin();
  // ... database queries or persist operations ...
  entityManager.getTransaction().commit();
  entityManager.close();
  ```
- **Why it is dangerous:** When an exception is thrown during database querying, object serialization, or persisting (like constraint violations), the execution skips to the method's end or the caller. The `commit()` and `close()` methods are never reached.
- **Whether it can cause:**
  - leaked connection: Yes
  - connection held too long: Yes
  - transaction not closed properly: Yes
  - session leak: Yes
  - pool starvation: Yes (Primary cause of the HikariPool 30000ms timeout)
- **Exact fix:** Introduce a `try-catch-finally` or `try-finally` block. Rollback the transaction if active, and guarantee that `close()` is called in the `finally` block.
- **Minimal safe patch example:**
  ```java
  public List<SlsTblSaleOrder> getAllSaleOrder() {
      EntityManager entityManager = getEntityManager();
      try {
          entityManager.getTransaction().begin();
          List<SlsTblSaleOrder> SaleOrders = entityManager.createQuery("FROM SlsTblSaleOrder ...").getResultList();
          entityManager.getTransaction().commit();
          return SaleOrders;
      } catch (Exception e) {
          if (entityManager.getTransaction().isActive()) {
              entityManager.getTransaction().rollback();
          }
          throw e; // Or handle appropriately
      } finally {
          if (entityManager.isOpen()) {
              entityManager.close();
          }
      }
  }
  ```

### Issue 2: Swallowed Exception with Leaked EntityManager in Scheduled Jobs
- **File name:** `ScheduledTask.java`
- **Class name:** `ScheduledTask`
- **Method name:** `saveExecutionTaskLog()`
- **Risk level:** High
- **Exact problematic code or pattern:**
  ```java
  public String saveExecutionTaskLog(ScheduledTaskExecutionLog scheduledTaskExecutionLog) {
      EntityManager entityManager = getEntityManager();
      try {
          entityManager.getTransaction().begin();
          entityManager.persist(scheduledTaskExecutionLog);
          entityManager.getTransaction().commit();
          entityManager.close();
          return "Success";
      } catch (Exception e) {
          logger.error(e.getMessage(), e);
          return "Failure";
      }
  }
  ```
- **Why it is dangerous:** The `try-catch` block catches errors such as duplicate keys or database lock timeouts, but `entityManager.close()` and `rollback()` are only inside the `try` block. Upon error, it returns "Failure" but leaves the database transaction open and holds a HikariCP connection indefinitely. Since this runs on a schedule, the pool is guaranteed to exhaust quickly.
- **Whether it can cause:**
  - leaked connection: Yes
  - connection held too long: Yes
  - transaction not closed properly: Yes
  - session leak: Yes
  - pool starvation: Yes
- **Exact fix:** Move `entityManager.close()` to a `finally` block and handle `.rollback()` in the `catch` block.
- **Minimal safe patch example:**
  ```java
  public String saveExecutionTaskLog(ScheduledTaskExecutionLog scheduledTaskExecutionLog) {
      EntityManager entityManager = getEntityManager();
      try {
          entityManager.getTransaction().begin();
          entityManager.persist(scheduledTaskExecutionLog);
          entityManager.getTransaction().commit();
          return "Success";
      } catch (Exception e) {
          if (entityManager.getTransaction().isActive()) {
              entityManager.getTransaction().rollback();
          }
          logger.error(e.getMessage(), e);
          return "Failure";
      } finally {
          if (entityManager.isOpen()) {
              entityManager.close();
          }
      }
  }
  ```

### Issue 3: Ineffective `@Transactional` Mixed with Manual Factory Instantiation
- **File name:** `SlsTblSaleOrderDAO.java`
- **Class name:** `SlsTblSaleOrderDAO`
- **Risk level:** Medium
- **Exact problematic code or pattern:** Class is annotated with `@Transactional`, but it defines:
  ```java
  private EntityManager getEntityManager() {
      return entityManagerFactory.createEntityManager();
  }
  ```
- **Why it is dangerous:** Spring's `@Transactional` relies on an `EntityManager` injected via `@PersistenceContext`. By manually invoking `entityManagerFactory.createEntityManager()`, you strip Spring's interceptor of its ability to auto-close and release connections for this instance. 
- **Whether it can cause:**
  - leaked connection: Indirectly (by disabling Spring's safety nets).
- **Exact fix:** Remove `@Transactional` if you insist on manual orchestration, or adopt Spring Data JPA / `@PersistenceContext` properly (see Plan D).

### Issue 4: Repeated JdbcTemplate Re-instantiation per Database Action
- **File name:** `DBDAO.java`, `DashBoardDAO.java`
- **Class name:** `DBDAO`
- **Method name:** `getJobCardDAO()`, `getJobCardSummaryDAO()`, etc.
- **Risk level:** Low
- **Exact problematic code or pattern:**
  ```java
  public List<Map<String, Object>> getJobCardDAO(ReportDTO dto) {
      this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
      // ...
  }
  ```
- **Why it is dangerous:** While `JdbcTemplate` perfectly manages connection scope natively by opening/closing connections using `DataSourceUtils`, it is unnecessarily wasteful to instantiate new template classes over and over per function call. (No connection leak is actually caused here, which is why it receives a **Low** risk).
- **Minimal safe patch example:** 
  Initialize it once implicitly using constructor injection or `@PostConstruct` instead of repeatedly allocating new objects inside every query method.

---

## A. Code patterns you should search globally in your project

You can search the entire workspace for the following exact phrases (in regex or exact formats):

1. **`\.getTransaction\(\)\.begin\(\)`**
   *Why?* Maps entirely to your manual JDBC/Hibernate transaction management which needs wrapping in a `try-catch-finally`. Look at all 76 files using this.
2. **`\.createEntityManager\(\)` OR `getEntityManager\(\)`**
   *Why?* Identifies all manual allocations of EntityManagers. Wherever this is invoked, an `.close()` MUST be guaranteed in a `finally` block.
3. **`catch\s*\([^{]*\{\s*(?!.*rollback).*?\}`** (Regex)
   *Why?* Identifies try-catch blocks where exceptions are caught, but `rollback()` is distinctly missing.
4. **`new JdbcTemplate\(this.dataSource\)`**
   *Why?* Identifies redundant instantiation of safe Spring templates.
5. **`@Transactional` alongside `getEntityManager()`**
   *Why?* Highlights areas where Spring isn't managing what the annotation advertises.

---

## B. Prioritized Action Plan

**1. What to fix first (Immediate Impact):**
- Locate all usages of `entityManager.close()` that are inside `try` blocks (like `ScheduledTask.java`) or without try-catch blocks whatsoever and move them into `finally` blocks.
- Fix async/scheduled tasks first, because they hit the database concurrently on a schedule.

**2. What to verify next:**
- Audit rollback flows globally. Any `begin()` call MUST have a `.rollback()` available in the `catch` block if the application relies on manual lifecycle controls.
- Check third-party API integration blocks (`SHIntegeration.java`). Network calls happen here. If a network call fails AFTER a connection opens but before it closes, the connection starves.

**3. What can wait:**
- Re-architecting from manual `createEntityManager()` into `@PersistenceContext` injection.
- Moving `new JdbcTemplate()` allocations out of specific methods.

---

## C. Temporary Mitigation Plan Without Major Refactoring

1. **Setup HikariCP Leak Detection & Timeout Mitigation:**
   Configure connection tracking to force close rogue connections dynamically so the JVM doesn't stall completely.
   In `application.properties`/`application.yml`:
   ```properties
   # Drops a connection after it has been sitting unclosed for maxLifetime
   spring.datasource.hikari.max-lifetime=1800000
   
   # Enable leak detection - logs the stack trace where the connection was opened
   spring.datasource.hikari.leak-detection-threshold=15000 
   
   # Ensure connections don't wait forever
   spring.datasource.hikari.connection-timeout=30000
   ```
   *Note: Using `leak-detection-threshold` will log exactly where a leak started in the codebase, proving invaluable for final cleanup.*

2. **Bulk Add Missing Finally-Blocks (Automated Tooling):**
   Wrap your method bodies with an IDE's global structural replace feature to forcibly append the `finally { if (em.isOpen()) em.close(); }` safety net.

---

## D. Long-term Best Practice Plan (Spring Boot + Hibernate + HikariCP)

1. **Remove Manual EntityManager Lifecycles:**
   Do not spawn custom managers.
   Replace:
   ```java
   private EntityManager getEntityManager() {
       return entityManagerFactory.createEntityManager();
   }
   ```
   With:
   ```java
   @PersistenceContext
   private EntityManager entityManager;
   ```
2. **Delegate Transactions to Spring `@Transactional`:**
   Use the native Spring `@Transactional` wrapper on the **Service Layer** methods, omitting `begin()`, `commit()`, and `close()` entirely. Rely on interceptor proxies.
3. **Move to Spring Data JPA:**
   Replace raw DAO iterations manually pulling logic into boilerplate interfaces (`CurdRepository`, `JpaRepository`). Let Spring Data perform runtime boilerplate generations automatically avoiding leaks.
4. **Avoid Cross-Boundary External Calls In Transactions:**
   In methods like `SHIntegeration.java`, separate network (`HTTP POST`) processing from database transactions. Collect data -> execute network task -> spawn new minimal transaction to save the answer, ensuring JDBC handles are not halted by HTTP latency.
