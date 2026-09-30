# Brecha entre el código y el diseño vigente

Fecha de revisión: 2026-09-30. Fuente de verdad: `00-contexto.md`,
`requisitos.md`, `arquitectura.md`, `modelo-datos.md`, `convenciones.md` y
`plan-iteracion-1.md`; por decisión de Cristian prevalece `requisitos.md` para el
alcance, por lo que RF-12, RF-13 y RF-22 están fuera de esta iteración.

Esta revisión no modifica código de producción. Ambos repositorios contienen cambios no
confirmados que se preservaron durante la inspección.

| Elemento del spec | Estado | Evidencia en código | Acción propuesta |
|---|---|---|---|
| Identidad Maven y paquete Java | Parcial | Root POM usa el `groupId`, `artifactId`, package base y plataforma 2.4.2 establecidos. Los nombres/descripciones de `api` y `omod` aún son los del scaffold (`Epidemiologysurveillance`). | Ajustar metadatos de submódulos en un cambio pequeño. |
| OMOD/OpenMRS/Hibernate, una sola BD MariaDB | Parcial | Es OMOD con DAO Hibernate y `dbSessionFactory`; no hay segundo datasource. Persisten `analyticsDatasource` y documentación de réplica aunque solo aceptan `primary`. | Retirar la abstracción/configuración textual de réplica al aprobar la limpieza. |
| Liquibase: exactamente 7 tablas del modelo vigente | Contradice | Crea tablas históricas en español y luego seis tablas `surveillance_*`; además queda la tabla scaffold. Faltan `surveillance_case`, `outbreak_alert` e `individual_record`; sobran auditoría y notificación propias. Tipos, columnas estándar OpenMRS y FKs no coinciden con el Anexo G. | No reescribir changesets aplicados. Primero comprobar `databasechangelog` compartido; luego diseñar changesets aditivos/migratorios tras aprobación. |
| Dominio y mappings Hibernate de las 7 entidades | Contradice | Mapea `NotifiableEvent`, `OutbreakAlertRule`, `PeriodCaseCount`, `EpidemiologicalFocus`, `SurveillanceAudit` y `NotiNotification`; no existen `SurveillanceCase`, `OutbreakAlert` ni `IndividualRecord`. | Sustituir el modelo por las siete entidades vigentes mediante migración incremental aprobada. |
| Registro de caso | Contradice | `CaseRequest` y `SurveillanceServiceImpl` completan un `Encounter` existente con `Obs` y `Diagnosis`; no persisten una fila `surveillance_case`. | Implementar el servicio/DAO y REST sobre `surveillance_case` después de aprobar la migración de esquema. |
| Atributos completos de `surveillance_case` | Falta | El payload solo contiene UUID de paciente, encounter, provider, location, evento, estado, gravedad, origen, especie, inicio y laboratorio. No incluye `encounter_diagnosis_id`, `infection_address`, `diagnosis_type`, vacunación, fechas de investigación/notificación/defunción, tipo de vigilancia ni `individual_record_id`. | Definir el contrato REST congelado y añadir DTO, validación y persistencia conforme al modelo. |
| Datos de paciente/gestación | Parcial | Lee paciente y gestación como `Obs`, pero también copia etnia como `Obs`; no valida residencia ni usa Address Hierarchy. | Leer residencia/etnia desde Patient; gestación solo como Obs del encounter; integrar Address Hierarchy y validar centro poblado. |
| Validación RF-07 y unicidad por diagnóstico | Parcial | Valida referencias y algunas fechas/resultado de laboratorio. La unicidad vigente por `encounter_diagnosis_id` no existe; hay detección de duplicados por ventana. | Sustituir por validaciones del servicio y UK real del modelo. |
| Febriles y duplicados retirados | Contradice | `possibleDuplicates`, `duplicateWindowDays`, error `POSSIBLE_DUPLICATE` y pruebas siguen bloqueando registros por ventana. No se halló funcionalidad de febriles expuesta. | Retirar la lógica de duplicados con aprobación, al sustituir el flujo de registro. |
| RF-12/RF-13/RF-22 fuera de iteración | Contradice | `OutbreakEngine` genera alertas inmediatas/de brote y los reportes incluyen demografía; README declara alertas implementadas. | No ampliar esas capacidades; decidir en la limpieza si se desactiva/elimina el código ya presente. |
| Scheduler OpenMRS | Cumple | `RefreshCountsTask` extiende `AbstractTask` y `SurveillanceActivator` usa `SchedulerService`; no hay `@Scheduled`. | Mantener patrón, pero reimplementar cálculo con `period_case_count` vigente. |
| `period_case_count`: cálculo e idempotencia del modelo | Contradice | `replaceCounts` borra todos los conteos de un evento y reinserta; no hay zona Address Hierarchy, nivel, fechas de rango ni tipos CONFIRMADO/PROBABLE. | Implementar upsert por la UK del modelo una vez aprobada la tabla. |
| Auditoría | Contradice | Existe tabla/mapping `SurveillanceAudit` y método `audit()` propio. | Eliminar/deprecar la auditoría propia mediante migración aprobada y delegar al OMOD de auditoría. |
| Notificaciones y NOTI-Web | Contradice | Existe `NotiNotification`, almacenamiento de contenido exportable y motores de alerta. | No ampliar; migrar a `individual_record` sin contenido persistido cuando corresponda a iteración 2. |
| Privilegios por caso de uso | Parcial | `config.xml` declara ver, registrar, indicadores y configurar, pero no uno explícito para editar, exportar ni registro individual; los endpoints carecen de anotaciones y delegan a servicio. | Completar la matriz/privilegios cuando se congele el contrato REST. |
| Dependencias declaradas | Parcial | OMOD requiere REST Web Services; frontend declara FHIR2. No se declaran Address Hierarchy, auditoría ni notificaciones. | Confirmar versiones y contratos de los módulos en instancia antes de declarar dependencias necesarias. |
| Frontend: un único paquete | Cumple | Solo existe `@sihsalus/esm-epidemiological-surveillance-app` para el componente. | Mantener un único paquete. |
| Frontend: JSON React Form Engine, máximo 3 pasos y jerarquía geográfica | Contradice | `CaseForm` es un formulario React manual de tres pasos; no hay esquema JSON ni selector provincia → distrito → centro poblado. | Reemplazar la captura por el esquema JSON y selector Address Hierarchy al implementar el nuevo contrato. |
| Pruebas | Parcial | Hay pruebas unitarias con mocks y algunas de persistencia sintética; no se usa el harness requerido `BaseModuleContextSensitiveTest` para el flujo completo, ni existe regresión del incidente no reproducido. | Añadir pruebas context-sensitive para RF-07, UK del diagnóstico e idempotencia, tras el nuevo modelo. |

## Cambios grandes que requieren visto bueno

1. Migrar las tablas históricas a las siete tablas del `modelo-datos.md`, preservando
   changesets ya aplicados y datos existentes.
2. Reemplazar el flujo que modifica `Encounter`/`Obs` como representación del caso por
   `surveillance_case`; los `Obs` se conservarían solo para gestación y datos clínicos.
3. Retirar la tabla/servicio de auditoría propios y la lógica de duplicados retirada.
4. Retirar o aislar alertas y demografía fuera de alcance, sin ampliar RF-12, RF-13 ni RF-22.

No se ejecutará ninguno de esos cambios hasta recibir aprobación explícita y confirmar el
estado de los changesets en el entorno compartido.
