# Project Instructions

## Project context

This repository is an existing Java/Spring application organized as a Maven multi-module modular monolith.

The application was already refactored into modules. Preserve the existing architecture and module boundaries.

A new `batch` capability is being added to this application. Do not redesign unrelated parts of the system.

The existing source code, Maven configuration, tests, and established project conventions are the source of truth for implementation details that can be discovered directly from the repository.

## Technology constraints

- Use the Java version already configured by the project.
- Use the Spring Boot version already configured by the project.
- Use the Maven dependency versions already managed by the project.
- Do not upgrade frameworks, plugins, dependencies, or Java unless explicitly required by the task.
- Reuse existing libraries and infrastructure before introducing new dependencies.
- Add a dependency only when there is a clear technical necessity.

## Architecture

- Preserve the existing modular-monolith architecture.
- The new batch functionality must respect existing module boundaries.
- Avoid unnecessary architectural layers or abstractions.
- Prefer simple, explicit designs over speculative extensibility.
- Do not introduce Hexagonal Architecture, Clean Architecture, CQRS, Event Sourcing, or similar architectural patterns unless the existing project already requires them or the specification explicitly requires them.
- Follow existing package structure and naming conventions whenever they are adequate.
- Do not move or refactor unrelated existing code only to match personal preferences.

## Code quality

- Follow SOLID principles where they improve the design.
- Prefer small and cohesive classes.
- Keep responsibilities explicit.
- Avoid god classes and oversized services.
- Avoid premature abstractions.
- Use interfaces when they provide a meaningful boundary or follow an existing project convention; do not create interfaces mechanically for every class.
- Prefer readable code over clever code.
- Follow the style already established by the repository.

## Existing behavior

Preserve all existing application behavior unless the current specification explicitly requires a change.

Do not modify unrelated business rules.

Do not make broad refactors while implementing a feature.

If existing code must be changed to support the new batch module, make the smallest safe change necessary.

## Data and security

The batch functionality may process CPF/CNPJ and other business data.

- Treat CPF/CNPJ as sensitive data.
- Do not add sensitive data to logs.
- Do not expose sensitive values unnecessarily in exceptions or diagnostic messages.
- Reuse the application's existing security and observability conventions.
- Do not weaken existing validation or security controls.

## Implementation workflow

For each execution wave:

1. Work only on the scope requested by the current wave.
2. Read only the repository areas necessary to understand and implement that scope.
3. Prefer targeted searches and targeted file reads over repeatedly exploring the entire repository.
4. Inspect existing implementations before creating parallel infrastructure.
5. Implement the complete requested scope.
6. Add or update relevant tests.
7. Run the relevant Maven tests.
8. Fix compilation errors and test failures caused by the implementation.
9. Do not proceed into future waves unless explicitly requested.

Do not repeatedly redesign decisions that are already defined in the project specification.

## Context and token efficiency

This project intentionally uses a lean-context workflow.

- Do not perform broad repository exploration unless necessary.
- Do not repeatedly reread files that are already understood during the current session.
- Do not generate large planning documents.
- Do not generate detailed task trees unless explicitly requested.
- Do not create additional Markdown documentation unless explicitly requested.
- Do not create ADRs, design documents, reports, summaries, or implementation journals unless explicitly requested.
- Do not use subagents or agent teams unless explicitly requested.
- Prefer implementing code over producing explanatory output.

The persistent project context is intentionally small. Additional specifications will be provided only when relevant to the current task.

## Testing

Before declaring an execution wave complete:

- ensure the modified code compiles;
- run the tests relevant to the modified modules;
- fix failures introduced by the implementation;
- preserve existing tests unless the specification legitimately changes their expected behavior.

For expensive full-project test suites, run targeted tests during implementation and run broader verification when requested by the execution wave.

Do not hide failing tests, disable tests, weaken assertions, or remove coverage merely to obtain a successful build.

## Error handling

Follow the application's existing exception-handling conventions.

Do not silently swallow failures.

Return or persist enough information to diagnose business and integration failures without exposing sensitive data.

## Git

- Do not commit automatically.
- Do not push automatically.
- Do not create branches automatically.
- Do not rewrite Git history.
- Leave changes available for human review unless explicitly instructed otherwise.

## Scope discipline

When requirements are clear, implement them instead of creating additional planning phases.

When something can be reliably inferred from existing code, inspect the code instead of asking the user.

When an ambiguity would materially change business behavior, data integrity, security, or a public contract, stop that specific decision rather than inventing a business rule.

Do not expand the scope beyond the current specification.

----------------------------------------------------------------------------------------------------------------

# Batch Orchestrator — Specification

## 1. Purpose

This document is the source of truth for the new batch orchestration capability to be implemented in the existing Java/Spring modular monolith.

The capability receives CSV/XLSX files, validates and stores them, persists batch state, communicates asynchronously with the external Batch application through Amazon SQS, exposes processing status, and provides the final result for download.

This document defines required behavior and architectural constraints.

It is not a roadmap, task list, ADR, implementation journal, or historical record.

---

## 2. Scope

The implementation belongs to a new `batch` module inside the existing modular monolith.

The Orchestrator must provide:

- batch upload through REST;
- CSV input support;
- XLSX input support;
- request/file validation;
- batch persistence in PostgreSQL;
- original file storage in Amazon S3;
- reliable publication to Amazon SQS using Transactional Outbox;
- consumption of Batch progress/result events;
- aggregate batch status;
- final result download;
- idempotency and duplicate-delivery protection;
- automated tests relevant to the implemented behavior.

The implementation must preserve the architecture, conventions, security mechanisms, observability, and behavior already present in the repository.

---

## 3. Out of Scope

Do not implement:

- the external Batch application itself;
- PC Crédito proposal processing inside the Orchestrator;
- frontend/UI;
- email sending;
- `.xls` support;
- unrelated refactors;
- migration to microservices;
- Kafka or RabbitMQ;
- CQRS or Event Sourcing;
- distributed transactions between PostgreSQL, S3, and SQS;
- speculative infrastructure not required by this feature.

---

## 4. System Responsibilities

### 4.1 Orchestrator

The application in this repository is responsible for:

1. receiving the request and file;
2. identifying the authenticated client/tenant using existing application mechanisms;
3. validating the request and file;
4. registering the batch in PostgreSQL;
5. storing the original input file in Amazon S3;
6. reliably notifying Batch that a file is available;
7. consuming Batch progress/result events;
8. persisting the current state required by its APIs;
9. exposing batch status;
10. exposing the final result for download.

### 4.2 External Batch application

The external Batch application is responsible for:

- consuming the file-available event;
- loading the batch/input;
- enforcing processing admission rules;
- processing batch items;
- coordinating proposal processing;
- calling PC Crédito;
- handling retries according to processing rules;
- reporting progress/results back to the Orchestrator.

The Orchestrator must not duplicate Batch execution responsibilities.

---

## 5. Messaging Topology

The architecture contains three logical SQS flows.

### Queue A — Orchestrator → Batch

Purpose:

> notify Batch that a batch/file is durably available for processing.

The message must reference the batch and its durable input. It must not contain the entire CSV/XLSX file.

### Queue B — Batch → Orchestrator

Purpose:

> communicate progress, item results, aggregate state, or completion information required by the Orchestrator.

The Orchestrator must consume these events idempotently and persist the state needed by the status and download APIs.

### Queue C — Batch → Workers

Purpose:

> distribute individual proposal-processing work inside the Batch solution.

Each proposal message represents one logical input row/proposal.

Queue C belongs to the Batch processing architecture and is not implemented by the Orchestrator repository unless explicitly required by a future scope.

### Queue type

The current target is Amazon SQS Standard.

Therefore, the system must not depend on:

- exactly-once delivery;
- strict message ordering.

Consumers must tolerate duplicate delivery.

---

## 6. High-Level Flow

```text
Client
  |
  | upload CSV/XLSX
  v
Orchestrator
  |
  +--> validate request/file
  |
  +--> Amazon S3
  |      original input file
  |
  +--> PostgreSQL
  |      batch state
  |      items / validation results
  |      status history
  |      transactional outbox
  |
  +--> Outbox Relay
          |
          v
     SQS: Orchestrator -> Batch
          |
          v
        Batch
          |
          +--> processing / PC Crédito
          |
          +--> SQS: Batch -> Workers
          |
          +--> SQS: Batch -> Orchestrator
                         |
                         v
                    Orchestrator
                         |
                         v
                    PostgreSQL

Client
  |
  +--> GET status
  |
  +--> GET final result
```

---

## 7. Input Formats

Accepted formats:

- CSV;
- XLSX.

Not accepted:

- legacy `.xls`.

CSV and XLSX represent the same logical business layout.

The request must explicitly declare its document type:

- `CPF`; or
- `CNPJ`.

CPF and CNPJ must never be mixed in the same file.

---

## 8. Input Layout

Each record contains nine business columns in the defined order.

### 8.1 CPF file

1. `DV-Application.IDservico`
2. `DV-Applicant.Applicant[1].CPF`
3. `DV-Application.ProdutoSolicitato.Produto`
4. `DV-Application.ProdutoSolicitato.Subproduto1`
5. `DV-Application.ProdutoSolicitato.Subproduto2`
6. `DV-Application.CDL.ScorePreCalculado`
7. `DV-Application.ProdutoSolicitato.ValorCreditoSolicitado`
8. `DV-Applicant.Applicant[1].FaturamentoLiquido`
9. `DV-Application.Fonte`

### 8.2 CNPJ file

1. `DV-Application.IDservico`
2. `DV-Applicant.Applicant[1].CNPJ`
3. `DV-Application.ProdutoSolicitato.Produto`
4. `DV-Application.ProdutoSolicitato.Subproduto1`
5. `DV-Application.ProdutoSolicitato.Subproduto2`
6. `DV-Application.CDL.ScorePreCalculado`
7. `DV-Application.ProdutoSolicitato.ValorCreditoSolicitado`
8. `DV-Applicant.Applicant[1].FaturamentoLiquido`
9. `DV-Application.Fonte`

The document column is the only layout difference between CPF and CNPJ files.

---

## 9. Required Fields

Required:

- `DV-Application.IDservico`;
- CPF or CNPJ according to the declared request type;
- `DV-Application.ProdutoSolicitato.Produto`;
- `DV-Application.ProdutoSolicitato.Subproduto1`;
- `DV-Application.ProdutoSolicitato.Subproduto2`;
- `DV-Application.Fonte`.

Optional:

- `DV-Application.CDL.ScorePreCalculado`;
- `DV-Application.ProdutoSolicitato.ValorCreditoSolicitado`;
- `DV-Applicant.Applicant[1].FaturamentoLiquido`.

An empty optional field must not invalidate a row solely because it is empty.

---

## 10. CSV and XLSX Physical Rules

Both formats must preserve:

- the same logical columns;
- the same column order;
- the same required/optional rules;
- the same CPF/CNPJ separation rule.

The exact CSV delimiter, encoding/BOM policy, numeric representation rules, and other physical parsing details are not finalized by this specification.

Do not silently invent these business contracts.

If the existing application already has an authoritative convention, reuse it. Otherwise keep parsing concerns isolated so these rules can be finalized without redesigning the batch domain.

For XLSX, do not create a different business contract from CSV.

---

## 11. Record Limit

Maximum number of business records per file:

`20,000`

The header does not count as a business record.

Files above the maximum must be rejected before normal Batch processing.

---

## 12. Processing Window

Processing admission follows this rule:

- `1–1,000` records: processing may start at any time;
- `1,001–20,000` records: processing may start only from `20:00` until `06:00`, using `America/Sao_Paulo`.

The window controls when processing may start.

A batch that starts before `06:00` may continue after `06:00`.

The Batch application owns this admission policy.

The Orchestrator accepts and durably registers the request; it must not implement a second competing scheduler.

---

## 13. Tenant Model

The domain supports:

- `STANDARD`;
- `MASTER`;
- `MICRO`.

Each client has its own `clientId`.

A MICRO client may share the infrastructure of a MASTER environment while still having its own client identity.

The authenticated/request context is the source for tenant/client identity.

Do not infer tenant identity from file contents.

Tenant/client identity must remain traceable throughout the batch lifecycle and messaging flow.

---

## 14. Product and Subproduct Validation

The Orchestrator must not validate:

- Produto;
- Subproduto1;
- Subproduto2

against a centralized product catalog.

These values may be customized by client.

The submitted values must continue through the processing flow.

If PC Crédito rejects the combination, the resulting business failure/reason must be preserved and exposed in the final result.

---

## 15. Validation Model

Validation has two levels.

### 15.1 Request/file-level failures

Examples:

- file missing;
- unsupported extension;
- unreadable/corrupted structure;
- invalid declared document type;
- incompatible CPF/CNPJ layout;
- missing/invalid headers;
- more than 20,000 records.

A file-level failure prevents normal dispatch to Batch.

### 15.2 Row-level failures

Examples:

- missing required field;
- missing document;
- document incompatible with the declared file type;
- row-specific invalid data.

A row-level error must remain associated with the original row number.

One invalid row must not automatically invalidate every other structurally valid row.

---

## 16. File With 100% Invalid Rows

A structurally valid file may contain zero valid business rows.

If every row is invalid:

- preserve each row and/or validation result as required by the data model;
- preserve the reason for each rejection;
- finish the batch with an error/failure outcome;
- expose the reasons in the final result;
- do not dispatch invalid rows for PC Crédito proposal processing.

This scenario must not be reported as successful processing.

---

## 17. Sensitive Data

CPF/CNPJ is sensitive data.

Requirements:

- do not unnecessarily log CPF/CNPJ;
- do not include raw documents in technical exception messages;
- mask sensitive values when diagnostic exposure is required;
- avoid CPF/CNPJ in SQS messages when an internal identifier is sufficient;
- use existing application security and observability conventions;
- persist documents using a representation that supports CPF and CNPJ correctly.

---

## 18. Amazon S3

Amazon S3 stores the original uploaded file.

Requirements:

- associate the stored object with the batch request;
- generate object identity on the server side;
- do not depend on the original filename for uniqueness;
- do not depend on S3 versioning for batch uniqueness;
- reuse existing S3 clients/configuration/abstractions when appropriate;
- do not create parallel AWS infrastructure unnecessarily.

The original filename may be retained as metadata for business/user-facing purposes.

---

## 19. PostgreSQL Domain Model

The logical model contains:

```text
batch_credit_requests
        |
        | 1:N
        v
batch_credit_request_items
        |
        | 1:N
        v
batch_credit_request_item_attempts

batch_credit_requests
        |
        | 1:N
        v
batch_credit_request_status_history

business transaction
        |
        | same PostgreSQL transaction
        v
batch_credit_outbox
```

Use the database migration mechanism already present in the project.

Do not introduce another migration framework.

---

## 20. Batch Request

`batch_credit_requests` represents one uploaded batch.

It must support the information necessary to identify and track at least:

- batch/request identity;
- client/tenant;
- document type;
- input format;
- original filename;
- S3 reference;
- aggregate status;
- record totals/counters;
- creation time;
- relevant processing/completion timestamps.

The exact physical schema must follow the already-defined project database conventions.

---

## 21. Batch Item

`batch_credit_request_items` represents one logical business row.

It must retain enough information for:

- parent batch identification;
- original line/row number;
- submitted business data required for processing;
- document;
- item status;
- proposal number/identifier when returned;
- proposal status when returned;
- response/error code when available;
- business failure reason when available.

The original line number must remain stable.

The logical identity of an input item is based on:

`batchId + lineNumber`

or an equivalent database identity protected by a uniqueness constraint.

---

## 22. Item Attempts

`batch_credit_request_item_attempts` preserves processing-attempt history.

The model must support tracing:

- item;
- attempt number;
- attempt time;
- outcome/status;
- relevant error classification.

Attempt history must not replace the current item state.

---

## 23. Status History

`batch_credit_request_status_history` records meaningful aggregate batch status transitions for traceability/audit.

It must not replace the current status on the batch request.

---

## 24. Item Statuses

The processing model supports:

- `PENDING`;
- `PUBLISHED`;
- `IN_FLIGHT`;
- `SUCCESS`;
- `FAILED`;
- `RETRYABLE`;
- `NON_RETRYABLE`;
- `UNKNOWN`.

Reuse existing domain enums/models if equivalent concepts already exist.

Do not create duplicate status models unnecessarily.

---

## 25. Error and Retry Baseline

Known baseline classifications:

- HTTP `503` from PC Crédito → `RETRYABLE`;
- HTTP `400` from PC Crédito → `NON_RETRYABLE`.

Detailed processing/retry execution belongs to Batch.

The Orchestrator must not create a competing retry policy.

---

## 26. UNKNOWN State

PC Crédito is not guaranteed to be idempotent.

A failure can occur after the request may have reached PC Crédito but before the caller can determine whether the proposal was created.

Such a case must be representable as:

`UNKNOWN`

`UNKNOWN` must not be blindly retried.

Avoiding duplicate proposals has precedence over automatic retry when the outcome is uncertain.

---

## 27. Idempotency

Idempotency is mandatory.

The design must tolerate:

- SQS Standard duplicate delivery;
- Outbox publication retry;
- duplicate Batch progress/result events;
- repeated receipt of the same batch notification;
- repeated receipt of the same logical item event.

Use durable identifiers and database constraints where appropriate.

Do not rely only on in-memory deduplication.

PC Crédito itself must not be assumed to be idempotent.

---

## 28. Transactional Outbox

The Orchestrator must not implement the critical publication flow as:

```text
save database
then
send SQS directly
```

The batch business state and corresponding Outbox record must be committed in the same PostgreSQL transaction.

Conceptual flow:

```text
Application Service
       |
       | one PostgreSQL transaction
       |
       +--> batch business data
       |
       +--> batch_credit_outbox
                |
                | after commit
                v
          Outbox Relay
                |
                v
          Amazon SQS
```

If the database transaction rolls back, no corresponding valid event may be published.

If SQS publication fails after the database transaction commits, the Outbox event remains available for a later publication attempt.

---

## 29. Outbox Requirements

`batch_credit_outbox` must support at least:

- unique event/outbox identifier;
- aggregate/batch identifier;
- event type;
- serialized payload;
- creation timestamp;
- publication state or equivalent control;
- publication timestamp when applicable;
- retry/attempt information when required;
- deterministic protection against duplicate logical events.

The relay must tolerate uncertain or repeated publication attempts.

Downstream consumers must remain idempotent.

---

## 30. Orchestrator → Batch Event

The logical event means:

> the batch is durably registered and available for Batch.

The event must be emitted only through the reliable Outbox flow.

Prefer references instead of business payload duplication.

The message should contain only the information required to locate and correlate the batch, for example:

- event ID;
- event type;
- batch ID;
- client ID;
- S3/object reference when necessary;
- correlation ID;
- event/version metadata when required by existing conventions.

Do not place the entire input file in SQS.

Avoid CPF/CNPJ in this message.

---

## 31. Batch → Orchestrator Events

The Orchestrator must consume the progress/result contract supplied by Batch.

Processing must be idempotent.

Events must carry stable identifiers allowing the Orchestrator to associate them with:

- the correct batch;
- the correct item/line when item-specific;
- the correct logical processing event.

The persisted result must be sufficient to support:

- current item states;
- aggregate status;
- counters/progress;
- final result generation;
- business failure reasons.

Do not expose the SQS transport contract directly as the REST API model.

---

## 32. Aggregate Status

The Orchestrator must expose batch status through REST.

The response must represent business state, not Spring Batch internal metadata.

It must support at least:

- batch identifier;
- current aggregate status;
- total records;
- processed records;
- successful records;
- failed/error records;
- pending records;
- retryable records when applicable;
- unknown records when applicable;
- progress percentage or equivalent progress information.

Counters must remain logically consistent.

Examples of invalid states that must not be produced:

- processed count greater than total;
- terminal success while pending items remain.

---

## 33. Final Batch Outcome

A completed batch may contain both successful and failed items.

Batch completion does not mean every proposal succeeded.

The aggregate model must be able to distinguish at least conceptually:

- full success;
- completion with item errors;
- failure;
- result containing `UNKNOWN` items when applicable.

Use existing project naming conventions when equivalent aggregate statuses already exist.

---

## 34. Final Result

After terminal processing, the client must be able to download the consolidated business result.

The result must come from persisted business state, not transient in-memory state.

Preserve the original row ordering.

The business result must be able to include:

- original line/row identification;
- original business fields needed to identify the input;
- document;
- proposal number/identifier when available;
- proposal status when available;
- item processing result/status;
- return/error code when applicable;
- failure/rejection reason when applicable.

Do not expose internal control fields such as:

- meaningless database primary keys;
- Outbox state;
- locking data;
- infrastructure retry controls.

The exact physical output format is not fixed by this specification.

Do not invent it if the repository contains no authoritative convention.

---

## 35. REST API

The feature requires at least three external operations:

1. upload a batch;
2. query batch status;
3. download the final result.

Exact URI paths and API versioning must follow existing application conventions.

Do not create a new REST naming/versioning style only for this module.

### Upload behavior

A successful upload returns a stable batch/request identifier and current state.

The HTTP request must not wait for all PC Crédito processing to finish.

Upload success means the batch was accepted and durably registered for asynchronous processing.

### Status behavior

Status queries must read persisted state.

Do not use process-local/in-memory state as the source of truth.

### Download behavior

The final result is available only when the batch is in an appropriate terminal state.

Before that, follow the existing API error/status convention instead of returning a misleading result.

---

## 36. Duplicate File Submission

Accidental duplicate uploads must be protected against using durable application data.

Do not use S3 versioning as the duplicate-detection mechanism.

The exact business time window for considering two semantically identical files duplicates is not currently finalized.

Do not invent this time window.

Design the duplicate-detection responsibility so the final business rule can be introduced without redesigning the module.

---

## 37. Concurrency and Fairness

Processing concurrency, throughput, fairness, and rate limiting belong primarily to Batch.

The baseline assumes controlled processing by client and PC Crédito environment rather than unlimited concurrency.

The Orchestrator must preserve the identifiers Batch requires to apply those policies.

Do not implement PC Crédito worker concurrency inside the Orchestrator.

---

## 38. Transactions and External Systems

Use PostgreSQL transactions where atomic database behavior is required.

The important atomic boundary is:

```text
business database changes
+
Outbox event
```

S3 and SQS do not participate in the PostgreSQL transaction.

Their failure modes must be handled explicitly.

Do not model external operations as if they shared a distributed ACID transaction with PostgreSQL.

---

## 39. Failure Handling

### Database transaction failure

Business changes and the corresponding Outbox event must roll back consistently.

### SQS publication failure

Keep the committed Outbox event eligible for retry.

Do not recreate the original business transaction merely to retry message publication.

### Duplicate SQS delivery

Processing the same logical event more than once must not create duplicate logical work.

### S3 failure during upload

Do not expose the batch as ready for Batch when its required durable input is unavailable.

### Invalid row

Preserve the failure reason and continue with other valid rows when the file itself remains structurally usable.

---

## 40. Observability

Reuse existing project logging, tracing, metrics, and correlation mechanisms.

Prefer identifiers such as:

- `batchId`;
- `clientId`;
- `correlationId`;
- `eventId`.

Do not log complete file contents.

Do not log CPF/CNPJ in plain text unless an existing approved application policy explicitly requires it.

---

## 41. Configuration

Environment-specific configuration must use the mechanism already established by the application.

Examples:

- S3 bucket;
- SQS queue identifiers/URLs;
- Outbox relay settings;
- processing thresholds when configurable;
- integration settings.

Do not hardcode environment-specific AWS resource names.

---

## 42. Implementation Reuse

Before creating new infrastructure, inspect the repository for existing:

- AWS/S3 clients;
- AWS/SQS clients;
- credentials/configuration;
- REST conventions;
- exception handling;
- validation utilities;
- PostgreSQL/JPA/JDBC conventions;
- migration tooling;
- security/authentication;
- client/tenant identification;
- JSON configuration;
- logging/tracing/metrics.

Reuse suitable existing infrastructure.

Make the smallest safe change to existing modules when integration requires it.

---

## 43. Testing Requirements

Automated tests must cover the behavior introduced by the implementation.

Relevant coverage includes:

- valid CSV;
- valid XLSX;
- CPF file;
- CNPJ file;
- CPF/CNPJ mismatch;
- missing required fields;
- optional empty fields;
- invalid structure/header;
- more than 20,000 records;
- 100% invalid rows;
- S3 storage interaction;
- batch persistence;
- Outbox creation with business state;
- Outbox relay behavior;
- duplicate-event/idempotency handling;
- Batch progress/result consumption;
- aggregate status;
- final-result generation/download;
- error handling.

Run targeted tests during each execution wave.

Broader project verification belongs to the final verification wave.

Do not disable or weaken tests merely to obtain a passing build.

---

## 44. Performance Principles

Design for up to 20,000 records without unnecessary complexity.

Avoid:

- repeatedly loading the complete input file;
- unnecessary copies of large file contents;
- placing complete files in SQS;
- N+1 persistence patterns where material;
- serializing the complete batch into events;
- one transaction spanning the complete asynchronous lifecycle.

Use batching/streaming where existing libraries and architecture make it useful.

Do not introduce complex optimization without evidence it is necessary.

---

## 45. Security Principles

The implementation must:

- preserve existing authentication/authorization;
- validate request input;
- validate file type and structure;
- avoid trusting user filenames as storage paths;
- prevent path traversal;
- protect CPF/CNPJ;
- avoid sensitive information in logs/errors;
- avoid exposing internal stack traces;
- reuse approved AWS credential mechanisms;
- preserve existing application security controls.

---

## 46. Definition of Done

The feature is complete when:

1. CSV upload works.
2. XLSX upload works.
3. CPF/CNPJ file types are enforced.
4. required-field and structural validation works.
5. the 20,000-record limit is enforced.
6. 100%-invalid files finish with an error outcome and preserved reasons.
7. the original input is durably stored in S3.
8. batch state is durably persisted in PostgreSQL.
9. the file-available event is created through Transactional Outbox.
10. the Outbox Relay can publish reliably to SQS.
11. duplicate delivery does not create duplicate logical processing.
12. Batch progress/result events can be consumed idempotently.
13. aggregate batch status is exposed by REST.
14. item results and business failure reasons are preserved.
15. the final business result can be downloaded.
16. sensitive document data is not unnecessarily exposed.
17. relevant automated tests pass.
18. affected modules compile successfully.
19. unrelated existing behavior remains unchanged.

---

## 47. Open Decisions — Do Not Invent

These items are intentionally not finalized.

### 47.1 CSV physical contract

Still requires authoritative definition if none exists in the repository:

- delimiter;
- encoding;
- BOM handling;
- detailed numeric formatting/parsing rules.

### 47.2 Duplicate-file business window

Duplicate protection is required, but the exact time interval/business policy is not finalized.

### 47.3 Final download format

The result content is defined, but the physical output format is not yet fixed.

### 47.4 Exact REST paths

Required operations are defined, but URI paths/versioning must follow the existing application's convention.

If an open decision blocks the current execution wave and cannot be resolved from existing authoritative project code/configuration, do not silently create a business rule.

---

## 48. Architectural Invariants

These rules must remain true throughout implementation:

- the Orchestrator is the external entry point;
- the feature is implemented inside the existing modular monolith;
- Batch is a separate processing application;
- processing is asynchronous;
- CSV and XLSX are supported;
- `.xls` is not supported;
- CPF and CNPJ are never mixed in the same file;
- the request declares the document type;
- maximum batch size is 20,000 records;
- batches above 1,000 records are admitted by Batch only in the 20:00–06:00 window;
- a process that starts before 06:00 may continue afterward;
- Produto/Subproduto are not validated against a central catalog;
- a file with 100% invalid rows finishes with an error outcome and reasons;
- S3 stores the original input;
- PostgreSQL stores durable operational state;
- SQS transports messages/references, not complete files;
- SQS Standard duplicate delivery must be tolerated;
- consumers are idempotent;
- business state and its Outbox event are committed atomically;
- Outbox publication occurs after the database commit;
- UNKNOWN is not blindly retried;
- PC Crédito is not assumed to be idempotent;
- email sending is out of scope;
- unrelated modules must not be redesigned;
- no additional planning/SDD documents are required by this specification.

--------------------------------------------------------------------------------------------------------------------------------------------------------

# Batch Orchestrator — Execution Plan

## 1. Purpose

This file controls implementation of the Batch Orchestrator specification using the Lean Context / Execution Waves workflow.

It is intentionally small and operational.

The sources of truth are:

1. `CLAUDE.md` — permanent repository working rules.
2. `docs/batch/SPEC.md` — functional and architectural requirements.
3. this file — implementation waves and current execution state.
4. the repository itself — existing code, conventions, configuration, tests, and build structure.

Do not create additional roadmap, feature, task, design, ADR, or progress documents unless explicitly requested.

---

## 2. Execution Strategy

Implementation is divided into five waves.

Each wave must be executed in a fresh Claude Code context whenever possible.

Recommended lifecycle:

```text
new session / /clear
        |
        v
read CLAUDE.md automatically
        |
        v
read current wave in EXECUTION.md
        |
        v
read only SPEC.md sections referenced by the wave
        |
        v
inspect only relevant repository areas
        |
        v
implement complete wave
        |
        v
run targeted tests
        |
        v
fix failures caused by the wave
        |
        v
update this file minimally
        |
        v
human review / cost check
        |
        v
next fresh session
```

Do not execute more than one wave unless explicitly instructed.

Do not redesign completed waves unless a verified defect or requirement conflict requires it.

---

## 3. Global Execution Rules

For every wave:

- work only on the current wave;
- do not create detailed task trees;
- do not create new planning documents;
- do not use subagents or agent teams unless explicitly requested;
- do not explore the entire repository without need;
- prefer targeted file searches and reads;
- reuse existing project infrastructure and conventions;
- preserve existing module boundaries;
- preserve unrelated behavior;
- do not upgrade Java, Spring Boot, Maven plugins, or unrelated dependencies;
- add dependencies only when technically necessary;
- run targeted tests before declaring the wave complete;
- fix compilation/test failures introduced by the wave;
- do not disable tests to make the build pass;
- do not commit or push unless explicitly requested;
- keep final conversational output concise.

If an open business decision from `SPEC.md` blocks part of a wave and cannot be resolved from authoritative repository conventions, do not invent the rule.

Complete all non-blocked work in the wave and record the blocker in the wave handoff.

---

## 4. Status Values

Each wave uses one of:

- `PENDING`
- `IN_PROGRESS`
- `BLOCKED`
- `DONE`

Only change a wave to `DONE` after its acceptance criteria and verification requirements are satisfied.

---

# Wave 1 — Module Foundation and Persistence

**Status:** `PENDING`

## Goal

Create the structural and persistence foundation required by every later batch capability while preserving the existing modular-monolith architecture.

## Read from SPEC.md

Focus on:

- §1 Purpose
- §2 Scope
- §3 Out of Scope
- §4 System Responsibilities
- §13 Tenant Model
- §17 Sensitive Data
- §19 PostgreSQL Domain Model
- §20 Batch Request
- §21 Batch Item
- §22 Item Attempts
- §23 Status History
- §24 Item Statuses
- §25 Error and Retry Baseline
- §26 UNKNOWN State
- §27 Idempotency
- §41 Configuration
- §42 Implementation Reuse
- §45 Security Principles
- §48 Architectural Invariants

Do not reread unrelated SPEC sections unless an implementation dependency requires it.

## Repository Inspection

Inspect only what is necessary to determine:

- Maven parent/module organization;
- Spring Modulith/module conventions, if present;
- persistence technology and repository conventions;
- database migration mechanism;
- entity/table naming conventions;
- ID generation conventions;
- auditing/timestamp conventions;
- exception and validation patterns;
- test conventions;
- package/module boundaries.

## Scope

Implement:

1. the new `batch` module using the repository's existing modular structure;
2. Maven/module wiring required for the application to build;
3. core batch domain types required by persistence;
4. aggregate request persistence;
5. item persistence;
6. item-attempt persistence;
7. aggregate status-history persistence;
8. Outbox persistence model/table;
9. required indexes and database uniqueness constraints;
10. database migrations using the existing migration framework;
11. repository/data-access abstractions consistent with the project;
12. targeted persistence/domain tests.

The logical database model must support the requirements in `SPEC.md`.

Protect logical item identity using the durable equivalent of:

`batchId + lineNumber`

Do not create SQS consumers, S3 upload behavior, REST endpoints, parsers, or Outbox publication relay in this wave.

## Acceptance Criteria

- the new module follows existing project boundaries;
- the project recognizes/builds the module;
- required batch tables are reproducibly created by project migrations;
- request, item, attempt, status-history, and Outbox persistence are represented;
- important uniqueness/integrity rules are enforced durably;
- CPF/CNPJ storage supports both document lengths;
- status types required by the specification are representable;
- no unrelated module is redesigned;
- targeted tests pass.

## Verification

Run the smallest Maven command(s) that compile and test the affected module(s).

If the project architecture requires a parent build for module validation, run that build only as broadly as necessary.

## Handoff

When complete, update only:

```text
Status: DONE

Result:
- <maximum 5 concise bullets>
```

Record only information future waves truly need, such as an unavoidable naming choice or an unresolved blocker.

---

# Wave 2 — Upload, Parsing, Validation, S3 and Intake Transaction

**Status:** `PENDING`

## Goal

Implement the external batch intake path from REST upload through durable file storage and database registration, ending with a pending Outbox event ready for later publication.

## Read from SPEC.md

Focus on:

- §4 System Responsibilities
- §6 High-Level Flow
- §7 Input Formats
- §8 Input Layout
- §9 Required Fields
- §10 CSV and XLSX Physical Rules
- §11 Record Limit
- §13 Tenant Model
- §14 Product and Subproduct Validation
- §15 Validation Model
- §16 File With 100% Invalid Rows
- §17 Sensitive Data
- §18 Amazon S3
- §27 Idempotency
- §28 Transactional Outbox
- §29 Outbox Requirements
- §30 Orchestrator → Batch Event
- §35 REST API
- §36 Duplicate File Submission
- §38 Transactions and External Systems
- §39 Failure Handling
- §40 Observability
- §41 Configuration
- §42 Implementation Reuse
- §44 Performance Principles
- §45 Security Principles

## Repository Inspection

Inspect only relevant existing implementations for:

- REST controllers and response conventions;
- authentication/client identification;
- multipart/file upload;
- validation;
- S3 client/service/configuration;
- AWS credentials/configuration;
- error handling;
- JSON serialization;
- transaction management;
- batch persistence created in Wave 1.

## Scope

Implement:

1. upload REST operation following existing API conventions;
2. request document-type declaration (`CPF` or `CNPJ`);
3. CSV ingestion;
4. XLSX ingestion;
5. header/layout validation;
6. required/optional-field validation;
7. CPF/CNPJ layout separation;
8. 20,000-record maximum;
9. preservation of original row numbers;
10. row-level validation results;
11. 100%-invalid-file outcome;
12. original input storage in existing/reused S3 infrastructure;
13. durable batch and item registration;
14. creation of the logical `batch available` Outbox record in the same PostgreSQL business transaction;
15. upload response containing stable batch identity/current state;
16. targeted tests for intake behavior.

Do not publish the Outbox event to SQS yet. Publication belongs to Wave 3.

Do not implement Batch-to-Orchestrator consumption or final download in this wave.

## Open-Decision Handling

`SPEC.md` intentionally leaves some CSV physical rules undefined.

If the repository contains an authoritative existing convention, reuse it.

If no authoritative convention exists:

- isolate the parser behavior cleanly;
- do not hide the uncertainty;
- do not invent a business rule that would be difficult to change;
- record the specific blocker only if it prevents correct execution.

The duplicate-file business time window is also not finalized.

Do not invent the window.

Implement only the durable data/mechanism that can safely support the rule once finalized.

## Transaction Requirement

The durable business state and corresponding Outbox record must be committed atomically in PostgreSQL.

S3 is external to that transaction.

Handle S3/database failure ordering explicitly so a batch is never exposed as ready for processing when its required durable input is unavailable.

## Acceptance Criteria

- supported CSV/XLSX uploads are accepted;
- `.xls` is rejected;
- CPF/CNPJ file types are enforced;
- required fields are validated;
- optional fields may be empty;
- row errors retain original row identity/reason;
- files above 20,000 records are rejected;
- 100%-invalid files finish with an error outcome and are not dispatched for proposal processing;
- original file is durably stored in S3;
- batch/items are durably persisted;
- Outbox event is persisted atomically with business state;
- no direct SQS send is performed by the intake transaction;
- sensitive document data is not unnecessarily logged;
- targeted tests pass.

## Verification

Run targeted parser, validation, persistence, S3-adapter, service, and controller tests.

Compile the affected module(s).

## Handoff

When complete, update only:

```text
Status: DONE

Result:
- <maximum 5 concise bullets>
```

---

# Wave 3 — Outbox Relay and Orchestrator → Batch SQS

**Status:** `PENDING`

## Goal

Reliably publish pending batch-available Outbox events to the Orchestrator → Batch SQS Standard queue.

## Read from SPEC.md

Focus on:

- §5 Messaging Topology
- §27 Idempotency
- §28 Transactional Outbox
- §29 Outbox Requirements
- §30 Orchestrator → Batch Event
- §38 Transactions and External Systems
- §39 Failure Handling
- §40 Observability
- §41 Configuration
- §42 Implementation Reuse
- §44 Performance Principles
- §48 Architectural Invariants

## Repository Inspection

Inspect only relevant existing implementations for:

- SQS clients/configuration;
- message serialization;
- scheduled/background execution conventions;
- locking/concurrency patterns;
- transaction handling;
- metrics/logging;
- retry configuration;
- Outbox persistence from Wave 1.

## Scope

Implement:

1. Outbox Relay;
2. retrieval/claiming of publishable events;
3. serialization of the batch-available event;
4. publication to the configured Orchestrator → Batch SQS Standard queue;
5. successful-publication state transition;
6. failed-publication retry eligibility;
7. protection against concurrent relay processing where necessary;
8. deterministic event identity;
9. logging/metrics using non-sensitive correlation identifiers;
10. targeted tests including failed send and repeated relay execution.

The relay must tolerate uncertain/repeated publication attempts.

Do not assume SQS Standard guarantees exactly-once delivery or strict ordering.

Do not place the complete file or CPF/CNPJ in the event when identifiers/references are sufficient.

## Acceptance Criteria

- committed Outbox events can be discovered and published;
- database rollback cannot produce a valid publishable event for the failed business transaction;
- SQS send failure leaves the event eligible for retry;
- success is durably recorded;
- repeated relay execution does not create duplicate business state;
- stable event identifiers allow downstream idempotency;
- AWS queue identifiers are externally configured;
- targeted tests pass.

## Verification

Run targeted Outbox and SQS adapter tests.

Run integration tests available in the repository when they do not require unavailable external infrastructure.

Compile the affected module(s).

## Handoff

When complete, update only:

```text
Status: DONE

Result:
- <maximum 5 concise bullets>
```

---

# Wave 4 — Batch → Orchestrator Events and Status API

**Status:** `PENDING`

## Goal

Consume progress/result events from Batch idempotently, update durable batch state, and expose aggregate status through REST.

## Read from SPEC.md

Focus on:

- §5 Messaging Topology
- §6 High-Level Flow
- §19 PostgreSQL Domain Model
- §20 Batch Request
- §21 Batch Item
- §22 Item Attempts
- §23 Status History
- §24 Item Statuses
- §25 Error and Retry Baseline
- §26 UNKNOWN State
- §27 Idempotency
- §31 Batch → Orchestrator Events
- §32 Aggregate Status
- §33 Final Batch Outcome
- §35 REST API
- §39 Failure Handling
- §40 Observability
- §41 Configuration
- §42 Implementation Reuse
- §45 Security Principles

## Repository Inspection

Inspect only relevant implementations for:

- SQS listeners/consumers;
- message deserialization;
- idempotency conventions;
- transaction handling;
- REST status endpoints;
- DTO/mapping conventions;
- exception/error response conventions;
- persistence created in previous waves.

## Scope

Implement:

1. Batch → Orchestrator SQS consumer;
2. stable event correlation;
3. duplicate-event detection/idempotent processing;
4. item status/result updates;
5. attempt-history persistence when supplied by the event contract;
6. aggregate status-history updates;
7. consistent aggregate counters;
8. final/terminal aggregate-state calculation required by current data;
9. status REST operation;
10. status DTO/projection;
11. targeted tests for duplicates, status transitions, counters, failures, and UNKNOWN.

Do not implement Queue C (Batch → Workers).

Do not implement Batch's PC Crédito processing logic.

Do not expose raw SQS transport DTOs as REST response DTOs.

## State Consistency Requirements

Do not produce impossible aggregate states.

At minimum:

- `processed <= total`;
- a terminal successful aggregate cannot retain pending items;
- duplicate events must not increment counters twice;
- item result transitions must remain traceable;
- `UNKNOWN` must remain distinguishable.

## Acceptance Criteria

- valid Batch events update the correct batch/item;
- duplicate events are safe;
- progress counters are consistent;
- status history is preserved;
- required business failure details are retained;
- UNKNOWN is represented without blind retry;
- REST status exposes business state rather than infrastructure metadata;
- targeted tests pass.

## Verification

Run targeted consumer, domain/service, persistence, and REST status tests.

Compile the affected module(s).

## Handoff

When complete, update only:

```text
Status: DONE

Result:
- <maximum 5 concise bullets>
```

---

# Wave 5 — Final Result, Integration Verification and Hardening

**Status:** `PENDING`

## Goal

Complete the user-facing result flow and verify the entire Orchestrator feature as an integrated implementation.

## Read from SPEC.md

Focus on:

- §16 File With 100% Invalid Rows
- §17 Sensitive Data
- §24 Item Statuses
- §26 UNKNOWN State
- §32 Aggregate Status
- §33 Final Batch Outcome
- §34 Final Result
- §35 REST API
- §39 Failure Handling
- §40 Observability
- §43 Testing Requirements
- §44 Performance Principles
- §45 Security Principles
- §46 Definition of Done
- §47 Open Decisions — Do Not Invent
- §48 Architectural Invariants

Also reread only the SPEC sections directly implicated by defects found during verification.

## Repository Inspection

Inspect only what is needed for:

- download/streaming REST conventions;
- output-file conventions, if any;
- DTO/projection patterns;
- security validation;
- integration tests;
- full application build.

## Scope

Implement:

1. persisted final-result projection;
2. original-row ordering;
3. final-result download REST operation;
4. safe behavior when the batch is not yet in a downloadable terminal state;
5. result handling for successful, failed, mixed, and UNKNOWN items;
6. result handling for 100%-invalid files;
7. efficient generation appropriate for up to 20,000 rows;
8. final security/PII review of the batch implementation;
9. final observability review;
10. integration-level tests;
11. broader Maven verification;
12. fixes required to satisfy the specification.

## Final Download Format Decision

The physical download format is an open decision in `SPEC.md`.

Before implementing the physical renderer:

1. inspect whether the repository/product already defines an authoritative convention;
2. if yes, reuse it;
3. if no, do not silently invent a business contract.

If this is the only unresolved blocker, complete the final-result projection and all other Wave 5 work, set this wave to `BLOCKED`, and record only that decision as the blocker.

## Final Verification

Verify the implementation against `SPEC.md §46 Definition of Done`.

At minimum, ensure coverage for:

- CSV;
- XLSX;
- CPF;
- CNPJ;
- invalid layouts;
- required/optional fields;
- >20,000 records;
- 100% invalid rows;
- S3 persistence;
- PostgreSQL persistence;
- Transactional Outbox;
- Outbox relay;
- duplicate SQS handling;
- Batch progress/result consumption;
- aggregate status;
- final result;
- sensitive-data handling;
- existing behavior in affected modules.

Run the broadest practical Maven verification required for confidence in the affected application.

Do not fix unrelated pre-existing failures unless they prevent verification and the change is explicitly approved.

## Acceptance Criteria

- final result is generated from persisted business state;
- original row order is preserved;
- internal infrastructure fields are not leaked;
- non-terminal download behavior follows existing API conventions;
- sensitive data handling complies with the specification;
- all Definition of Done items that are not explicitly blocked by an open product decision are satisfied;
- targeted tests pass;
- broader affected-project verification passes;
- no unrelated behavior is intentionally changed.

## Handoff

When complete:

```text
Status: DONE

Result:
- <maximum 5 concise bullets>
```

If blocked only by an unresolved product decision:

```text
Status: BLOCKED

Blocker:
- <one concise description>

Completed:
- <maximum 5 concise bullets>
```

---

# 5. Execution State

This section is the only cross-wave progress summary.

Keep it compact.

```text
Wave 1 — Module Foundation and Persistence:                  PENDING
Wave 2 — Upload, Parsing, Validation, S3 and Intake:        PENDING
Wave 3 — Outbox Relay and Orchestrator → Batch SQS:         PENDING
Wave 4 — Batch → Orchestrator Events and Status API:        PENDING
Wave 5 — Final Result, Integration Verification/Hardening:  PENDING
```

---

# 6. Cost / Context Guardrails

The purpose of this workflow is to avoid the token amplification of the previous SDD approach.

For each wave:

1. start from a fresh context when practical;
2. load only the SPEC sections explicitly referenced by that wave;
3. inspect only code required by the scope;
4. avoid subagents/agent teams;
5. avoid generating task breakdowns;
6. avoid long implementation summaries;
7. stop after the wave is complete;
8. check Claude Code usage/context before starting the next wave.

If one wave consumes unexpectedly high resources, do not automatically start the next wave.

Review why the context or implementation expanded first.

---

# 7. Human Checkpoints

Human review occurs after every wave.

The reviewer should check:

- cost/usage;
- Git diff;
- test result;
- whether scope remained inside the wave;
- whether an open business decision was invented;
- whether unrelated files were modified.

Only after that review should the next wave begin.

---

# 8. No Automatic Continuation

Completion of one wave is not permission to begin the next.

Claude must stop after:

- implementation;
- targeted verification;
- necessary fixes;
- minimal update to this file;
- concise result summary.

Wait for an explicit instruction before starting another wave.


------------------------------------------------------------------------------------

Execute **Wave 1 — Module Foundation and Persistence** from `docs/batch/EXECUTION.md`.

Follow `CLAUDE.md` strictly.

Use `docs/batch/SPEC.md` as the source of truth, but read only the sections explicitly referenced by Wave 1 unless an implementation dependency makes another section necessary.

Follow the scope, acceptance criteria, verification requirements, context guardrails, and handoff rules defined in `EXECUTION.md`.

Important constraints:

- Do not create a new implementation plan.
- Do not decompose the wave into a large task tree.
- Do not create additional Markdown documentation.
- Do not use subagents or agent teams.
- Do not broadly explore the repository.
- Inspect only repository areas required for this wave.
- Reuse existing project conventions and infrastructure.
- Do not implement future waves.
- Do not commit or push.
- Implement the complete wave, run the relevant tests, and fix failures caused by your changes.
- Update `EXECUTION.md` only as instructed by its handoff section.

Stop after Wave 1 is complete or if a genuine unresolved requirement blocks correct implementation.
