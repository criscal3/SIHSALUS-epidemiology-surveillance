# Brecha entre el código y el diseño vigente

Fecha de revisión: 2026-09-30. Fuentes de verdad: `00-contexto.md`,
`requisitos.md`, `arquitectura.md`, `modelo-datos.md`, `convenciones.md` y
`plan-iteracion-1.md`. Por decisión de Cristian prevalece `requisitos.md`: RF-12,
RF-13 y RF-22 quedan fuera de la iteración 1.

| Elemento del spec | Estado | Evidencia actual | Acción pendiente |
| --- | --- | --- | --- |
| Identidad OMOD, OpenMRS/Hibernate y una sola MariaDB | Cumple | OMOD con DAO Hibernate y `dbSessionFactory`; no hay segundo datasource. | Mantener. |
| Siete tablas vigentes y nombres físicos en inglés | Parcial | Changesets 13/14 crean las siete tablas y FKs. Se usa `diagnosis_id` por excepción aprobada a OpenMRS 2.4.2, en lugar de `encounter_diagnosis_id`. | Ejecutar y evidenciar migración/rollback en MariaDB 10.11 real. |
| Dominio y mappings Hibernate | Cumple | `Surveillance.hbm.xml` mapea las siete entidades del modelo. | Mantener pruebas de mapeo. |
| Registro de `surveillance_case` completo | Parcial | DTO, servicio y REST persisten referencias, variables epidemiológicas y borradores; `individual_record_id` no se asigna. | Completar prueba context-sensitive y validar con instancia OpenMRS real. |
| RF-07, fechas, enumeraciones y unicidad | Parcial | Servicio valida al cerrar y la UK es `diagnosis_id`; el borrador permite campos incompletos. | Añadir cobertura `BaseModuleContextSensitiveTest` para las referencias reales. |
| Residencia, dirección de infección y gestación | Parcial | El backend valida centro poblado y residencia al cierre; el formulario actual tiene selector Address Hierarchy. La gestación requiere el esquema React Form Engine y UUID reales. | Implementar el esquema cuando estén disponibles los metadatos de gestación. |
| Febriles y deduplicación retirada | Contradice | Persisten `CaseRequest`, `registerCase`, `possibleDuplicates` y `POSSIBLE_DUPLICATE`. | Retirar el flujo heredado tras confirmar que ningún consumidor lo usa. |
| RF-12/RF-13/RF-22 fuera de iteración | Contradice | Persisten `OutbreakEngine`, reglas y partes de reportes heredados. | Aislar o retirar sin ampliar alertas ni demografía. |
| Reconciliación manual y `period_case_count` | Cumple | El recálculo bajo demanda hace upsert para centro poblado/distrito, sin tareas programadas del componente. | Probar idempotencia contra MariaDB real. |
| Reportes sobre la BD original | Cumple en implementación local | `/reports` lee `period_case_count`, separa distrito/centro poblado y permite confirmados, probables o ambos. La curva y el canal no consultan encuentros. | Cotejar resultados contra una instancia OpenMRS/MariaDB con datos sintéticos. |
| Auditoría | Parcial | No hay tabla ni servicio de auditoría propios. El OMOD externo no está disponible para verificar su API. | Integrar mediante el punto descrito en `docs/auditoria-integracion.md`. |
| Notificaciones / NOTI-Web | Parcial | No hay integración nueva; `outbreak_alert.alert_id` referencia el núcleo para iteración 2. | No ampliar en iteración 1. |
| Privilegios por caso de uso | Parcial | Los cinco privilegios reales están declarados en `config.xml` y documentados en README/API. | Aplicar controles por endpoint/servicio y probar 403 con sesión real. |
| Frontend único | Cumple | Solo existe `esm-epidemiological-surveillance-app`. | Mantener. |
| Formulario JSON React Form Engine, máximo tres pasos | Parcial | El formulario actual mantiene tres pasos y selector geográfico, pero no usa React Form Engine por decisión temporal y faltan UUID de gestación. | Implementar cuando exista configuración de metadatos. |
| Contrato REST | Parcial | `docs/api-contract.md` cubre casos, eventos, geografía e indicadores con los filtros aprobados. | Completar validación integrada frontend/OMOD antes de publicar como contrato estable. |
| Pruebas | Parcial | Pruebas unitarias y migración H2 existen. | Ejecutar `mvn --batch-mode clean verify` final y pruebas MariaDB/context-sensitive exigidas. |

## Cambio amplio pendiente de aprobación

La retirada de `CaseRequest`/`registerCase`, su DAO de deduplicación, `OutbreakEngine`,
rutas de reglas y las pruebas asociadas es una limpieza amplia. Se debe confirmar que no
hay frontend, integración ni instancia que consuma esas rutas antes de eliminarla. No se
incluye en esta revisión documental.
