# Vigilancia epidemiológica — SIH Salus

OMOD `io.github.proyecto-santaclotilde:sihsalusepidemiologicalsurveillance`, para OpenMRS 2.4.2 y MariaDB. El frontend asociado es el único microfrontend `@sihsalus/esm-epidemiological-surveillance-app`.

## Diseño vigente

- Una sola base de datos OpenMRS; no hay réplica ni segundo datasource.
- Persistencia de las siete tablas del modelo: `notifiable_event`, `outbreak_alert_rule`, `outbreak_alert`, `period_case_count`, `epidemiological_focus`, `surveillance_case` e `individual_record`.
- Los casos se guardan en `surveillance_case`; los datos del paciente se leen de `patient` y la gestación permanece como `Obs` del encuentro.
- Los conteos se recalculan al registrar o actualizar casos y mediante una acción manual idempotente de configuración.
- El componente no crea una tabla de auditoría ni se integra con NOTI-Web.

Los RF-12, RF-13 y RF-22 están fuera de la iteración 1 por decisión de alcance.

## Registro de casos

El contrato está congelado en [docs/api-contract.md](docs/api-contract.md). `POST /cases` y `PUT /cases/{uuid}` permiten borradores. `POST /cases/{uuid}/close` aplica las validaciones RF-07 sin añadir una columna de estado. La restricción única por diagnóstico impide dos filas para el mismo diagnóstico.

El `test_order_id` puede derivarse temporalmente de una observación de laboratorio: el servidor acepta `laboratoryObservationUuid` y usa `Obs.order` cuando exista. No crea órdenes ni conceptos de laboratorio.

## Eventos y conteos

Un evento notificable es versionado: `POST /events` crea una versión con concepto, periodicidad, norma de referencia y vigencia. `PUT /events/{uuid}/valid-to` cierra solamente esa versión; no se eliminan ni modifican versiones históricas. El nombre se deriva de `concept_name` y el plazo de la periodicidad.

Los casos `CONFIRMADO` y `PROBABLE` desencadenan el recálculo de `period_case_count`. Si se requiere reconciliar datos históricos, un usuario con privilegio de configuración puede ejecutar `POST /counts/refresh`. Solo se cuentan casos no anulados, con fecha de inicio y centro poblado, en su centro poblado y distrito.

## Privilegios

| Caso de uso | Privilegio definido en `config.xml` |
| --- | --- |
| Abrir la aplicación y catálogo | `app:home.epidemiologicalSurveillance` |
| Consultar casos | `Vigilancia Epidemiologica: Ver Casos` |
| Crear, editar o cerrar casos | `Vigilancia Epidemiologica: Registrar Casos` |
| Consultar indicadores | `Vigilancia Epidemiologica: Ver Indicadores` |
| Configurar eventos y ejecutar recálculo | `Vigilancia Epidemiologica: Configurar` |

Además se requieren los privilegios clínicos nativos de OpenMRS para consultar las referencias involucradas. Los nombres no se asignan automáticamente a roles.

## Auditoría

La auditoría corresponde al OMOD existente de auditoría clínica. Este repositorio no contiene ese OMOD, por lo que su API y la captura de cambios por campo no se han podido verificar localmente. El contrato de eventos que debe recibir y el punto pendiente de integración están en [docs/auditoria-integracion.md](docs/auditoria-integracion.md).

## Desarrollo

```sh
mvn --batch-mode clean verify
```

El artefacto es `omod/target/sihsalusepidemiologicalsurveillance-1.0.0-SNAPSHOT.omod`. Los changesets 1–12 ya fueron aplicados y no se modifican. Los changesets 13 y 14 retiran las tablas históricas tras una copia SQL externa autorizada; su rollback no recupera esos datos.
