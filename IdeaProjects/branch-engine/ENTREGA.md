# Entrega — Tuckersoft Branch Engine

## Resumen de estrellas

_(Pega aquí el resumen que imprimen los autotests al correr `./mvnw test` en `autotests/`)_

```
  ──────────────────────────────────────────────
   TUCKERSOFT · CONTROL DE CALIDAD
   ★★★★★   5 / 5
  ──────────────────────────────────────────────
```

## Flujo asíncrono implementado

`POST /api/v1/decisions` guarda la `Decision` (o la marca `ENTRADA_CORRUPTA` sin publicar
nada) dentro de una transacción `@Transactional` en `DecisionService` y responde **201**
de inmediato. Al confirmar PostgreSQL el commit, `BranchNotificationListener`
(`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async("branchExecutor")` +
`@Transactional(propagation = REQUIRES_NEW)`) recibe el `DecisionCommittedEvent`, marca la
decisión como `PROCESANDO`, envía el Informe de Realidad con `JavaMailSender` y, según el
resultado, la deja en `ESTABILIZADA` (con `RealityLog` `SENT`) o `ERROR` (con `RealityLog`
`FAILED`), registrando siempre la línea `[BRANCH-LOG]` con el nombre del hilo
`branch-worker-X`.

## Pendientes

_(Lista aquí lo que no llegó a terminarse)_
