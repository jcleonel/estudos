I want to understand the Batch implementation that now exists in this repository.

This is a **read-only code analysis and teaching task**.

Do not modify any source code, configuration, tests, Markdown files, or Git state.
Do not create documentation files.
Do not generate a new implementation plan.
Do not use subagents or agent teams.

Your job is to inspect the actual implementation and teach me how it works.

## Source of truth

The **implemented code is the primary source of truth**.

Do not explain what the system was supposed to implement based only on `SPEC.md` or `EXECUTION.md`.

First inspect the actual Batch implementation and the existing project code it depends on.

You may consult:

- `CLAUDE.md`
- `docs/batch/SPEC.md`
- `docs/batch/EXECUTION.md`

only as secondary context, especially when useful to explain why a design decision was made or to identify differences between specification and implementation.

If the implementation differs from the specification, explicitly tell me.

## Scope

Focus on the Batch capability created by the Lean Context / Execution Waves implementation and the existing application components that it directly integrates with.

Do not analyze unrelated parts of the repository.

## Language

Explain everything in **Brazilian Portuguese**.

Keep Java/Spring/AWS technical terminology, class names, method names, package names, table names, configuration properties, queue names, and file paths exactly as they appear in the project.

## Teaching style

Assume I am a senior Java/Spring developer who understands Java, Spring, REST, PostgreSQL, AWS, SQS and Maven, but who did not personally implement this Batch code.

Do not give me superficial descriptions such as:

"BatchService contains the batch business logic."

Instead explain:

- what the component actually does;
- who calls it;
- what it calls;
- what data enters and leaves it;
- why this responsibility is separated there;
- how it participates in the complete runtime flow.

Whenever you explain an important component, include its real file path and class/interface name so I can open it in IntelliJ while following your explanation.

Do not dump large sections of source code. Refer to methods/classes and explain their behavior.

---

# 1. First give me the mental model

Start with a concise architectural overview of what was actually built.

Explain:

- where the Batch module fits in the modular monolith;
- what responsibilities belong to the Orchestrator;
- what responsibilities remain outside this repository;
- which external systems are involved;
- the main entry and exit points.

Give me a small ASCII diagram based on the actual implementation.

For example, conceptually:

Client
  -> REST
  -> application/service
  -> validation
  -> S3
  -> PostgreSQL + Outbox
  -> Relay
  -> SQS
  -> Batch

But do not assume this exact flow. Build the diagram from the code you actually find.

---

# 2. Explain the package structure

Walk through the Batch packages that were created.

For each relevant package explain:

- its path;
- its responsibility;
- the important classes/interfaces inside it;
- why those classes belong together;
- why that package exists separately from the others.

I specifically want to understand why the implementation was divided this way.

If the structure follows an existing project convention, point that out.

If a package exists mainly to create a boundary between infrastructure, application logic, persistence, integration, API, etc., explain that boundary.

Do not mechanically list trivial classes without explaining their role.

---

# 3. Explain the main classes and interfaces

For each important class/interface, explain:

**Class/interface**
`actual.package.ClassName`

**Path**
`actual/path/ClassName.java`

**Responsibility**
What it really does.

**Called by**
What invokes it.

**Calls / depends on**
Its important collaborators.

**Why it exists**
Why this responsibility was separated into this class/interface instead of being placed somewhere else.

**Runtime role**
Where it participates in the end-to-end flow.

For interfaces, explain especially:

- why an interface was introduced;
- which implementation implements it;
- where dependency injection binds the abstraction to the implementation;
- whether it represents an architectural boundary, testability seam, existing project convention, or simply an abstraction;
- if the interface currently has only one implementation, explain why keeping the interface still appears useful — or tell me if it looks unnecessary.

Do not assume the reason. Distinguish between:

- reasons directly supported by the code/project conventions;
- your architectural inference.

---

# 4. Trace the upload flow end-to-end

Follow one successful batch upload from the HTTP request until the Orchestrator considers it durably accepted.

Show the real sequence of components and methods.

Explain step by step:

1. REST endpoint;
2. request DTO/input;
3. authenticated client/tenant resolution;
4. file-format detection;
5. CSV/XLSX parsing;
6. header validation;
7. row validation;
8. CPF/CNPJ rules;
9. 20,000-row limit;
10. invalid-row handling;
11. S3 storage;
12. PostgreSQL persistence;
13. Transactional Outbox creation;
14. HTTP response.

For each step identify the actual class/method responsible.

Explain transaction boundaries clearly.

In particular tell me:

- what happens before the PostgreSQL transaction;
- what happens inside it;
- where S3 fits;
- where the Outbox record is created;
- what happens if S3 fails;
- what happens if database persistence fails.

If the actual implementation differs, explain the real behavior instead.

---

# 5. Explain CSV and XLSX handling

Show me exactly how file parsing was implemented.

Explain:

- common abstractions, if any;
- CSV parser implementation;
- XLSX parser implementation;
- how the correct parser is chosen;
- where headers are defined/validated;
- how CPF and CNPJ layouts differ;
- how required vs optional fields are handled;
- how original line numbers are preserved;
- how row validation errors are represented;
- how 100%-invalid files are handled;
- how the maximum row count is enforced.

Explain why the parser/validator classes were split the way they were.

Also identify any unresolved physical CSV decisions or assumptions present in the code.

---

# 6. Explain the persistence model

Explain the actual implementation of:

- `batch_credit_requests`;
- `batch_credit_request_items`;
- `batch_credit_request_item_attempts`;
- `batch_credit_request_status_history`;
- `batch_credit_outbox`.

For each one explain:

- entity/model;
- repository;
- important relationships;
- important constraints/indexes;
- lifecycle;
- which code writes it;
- which code reads it.

Then explain how these tables work together during a real batch.

Explain the status enums and aggregate/item distinction.

Also show me where the database migrations are and what they create.

---

# 7. Explain Transactional Outbox in THIS implementation

Do not give me a generic Transactional Outbox tutorial first.

Explain the implementation in this repository.

Trace:

business operation
    -> database changes
    -> Outbox entity
    -> commit
    -> Outbox Relay
    -> SQS publisher
    -> publication state update

Identify the actual classes and methods.

Explain:

- how pending events are found;
- how they are claimed/selected;
- how publication works;
- how success is recorded;
- what happens when SQS fails;
- how retry works;
- how duplicate publication is tolerated;
- what prevents or reduces concurrent relay problems.

Then, after explaining the code, briefly explain why this solves the PostgreSQL/SQS dual-write problem.

---

# 8. Explain the SQS contracts

Identify every SQS contract implemented by this repository.

For each queue/event explain:

- direction;
- purpose;
- producer;
- consumer;
- message DTO/event class;
- important fields;
- event identity/correlation;
- serialization;
- configuration/property defining the queue;
- idempotency strategy.

Pay special attention to:

### Orchestrator -> Batch

Explain exactly what gets sent and why the full file is not sent in the message.

### Batch -> Orchestrator

Explain exactly how progress/result messages are consumed.

Trace one incoming event from the SQS listener through persistence and aggregate-state update.

Also explain how duplicate SQS Standard messages are handled.

If Batch -> Workers exists only outside this repository, make that clear.

---

# 9. Explain batch status calculation

Show me how the current batch status/progress API works.

Trace:

HTTP request
    -> controller
    -> service/query
    -> repository/database
    -> aggregation/mapping
    -> response DTO

Explain:

- total;
- processed;
- successful;
- failed;
- pending;
- retryable;
- UNKNOWN;
- percentage/progress calculation;
- terminal states.

Tell me whether counters are stored or calculated dynamically and why.

Explain protections against inconsistent states.

---

# 10. Explain final result/download

Trace the final-result request end-to-end.

Explain:

- endpoint;
- validation of batch state;
- query/projection;
- ordering by original input row;
- mapping;
- output generation;
- response/download.

Explain what fields are exposed and what internal fields are intentionally excluded.

Explain what happens for:

- all-success;
- mixed success/failure;
- 100% invalid;
- UNKNOWN;
- request before processing is complete.

Also tell me what physical result format was finally implemented and where that decision is represented.

---

# 11. Explain configuration

Map every Batch-related configuration required to run the feature.

Group by:

- PostgreSQL;
- S3;
- SQS;
- Outbox Relay;
- scheduling/background execution;
- AWS;
- feature-specific properties.

For every relevant property tell me:

- property name;
- configuration class;
- where it is consumed;
- whether a default exists;
- what I would need to configure in a real environment.

Also explain which existing application configuration was reused rather than recreated.

---

# 12. Explain error handling and security

Explain how the implementation handles:

- invalid request;
- invalid file;
- invalid row;
- S3 failure;
- database failure;
- SQS publication failure;
- malformed incoming SQS event;
- duplicate event;
- UNKNOWN PC Crédito result where applicable.

Then explain the measures related to CPF/CNPJ:

- persistence;
- logs;
- exceptions;
- SQS messages;
- REST responses/results.

Point to the classes responsible.

---

# 13. Explain the tests

Do not list every test method.

Group the tests by responsibility and explain:

- what behavior they protect;
- what classes they test;
- what integration boundaries are mocked;
- whether there are integration tests;
- which business/architectural rules have explicit coverage.

Point out important behavior that appears to have weak or missing test coverage.

---

# 14. Compare implementation with SPEC

Only after understanding the code, compare the actual implementation against `docs/batch/SPEC.md`.

Create three groups:

### Implemented as specified
Important requirements clearly present in code.

### Implemented differently
Requirements where the implementation chose another design or behavior.

### Still open / incomplete
Requirements, TODOs, placeholders, configuration dependencies, or unresolved business rules that remain.

Do not mark something missing merely because you did not find it immediately. Search for it first.

---

# 15. Architectural critique

Now act as a senior Java/Spring software architect reviewing the implementation.

Tell me:

- what design decisions are especially good;
- what appears more complex than necessary;
- where an abstraction is justified;
- where an abstraction may be excessive;
- possible coupling problems;
- transaction risks;
- idempotency risks;
- concurrency risks;
- performance risks for 20,000 rows;
- maintenance concerns;
- anything surprising in the generated implementation.

Do not change the code.

Separate clearly:

- verified issue;
- potential risk;
- subjective design preference.

---

# 16. Finish with the mental model I should remember

At the end, give me a concise section called:

`Como pensar nesse Batch`

Explain the implementation as a mental model I can retain and later explain verbally to another developer.

Include:

- the 5–10 most important components;
- the main runtime flow;
- the main persistence idea;
- the messaging idea;
- where reliability/idempotency comes from;
- where I should start debugging if something goes wrong.

The objective is not to produce formal documentation.

The objective is for me to **understand the code well enough to maintain it, debug it, review future changes, and explain why it was designed this way**.
