# Notas para Cristian

## 2026-09-30 — filtros de indicadores aprobados e implementados

Se implementa `diagnosisType=CONFIRMADO|PROBABLE|TODOS`, predeterminado `CONFIRMADO`,
y `zoneLevel=DISTRITO|CENTRO_POBLADO`, con `address` opcional del nivel seleccionado.
Sin dirección se suman todas las zonas de ese nivel, nunca ambos niveles. Curva y canal
leen `period_case_count`; RF-22 no se muestra en el frontend. No se modifican changesets.
El histórico suma las filas seleccionadas por año antes de calcular cuartiles y no
convierte años ausentes en ceros. La validación desplegada y de cobertura histórica
completa continúa pendiente; las pruebas locales no sustituyen esa evidencia.

## 2026-09-30 — contradicción de alcance de iteración 1

`requisitos.md` indica que los RF incluidos en la iteración 1 son RF-01 a RF-07,
RF-10, RF-11, RF-17, RF-18, RF-19 y RF-27; deja RF-12, RF-13 y RF-22 para iteración 2.
En cambio, `plan-iteracion-1.md` declara como incluidos RF-12, RF-13 y RF-22.

**Decisión de Cristian (2026-09-30):** prevalece `requisitos.md`; RF-12, RF-13 y
RF-22 no se implementan en esta iteración. Se conserva esta nota como trazabilidad.

## 2026-09-30 — ciclo de vida de `surveillance_case` para RF-07

`modelo-datos.md` permite que `onset_date` e `infection_address` sean nulos para completar
el caso después y prohíbe agregar una columna de estado sin consultar. RF-07 exige esos
datos solo para **cerrar/notificar**, pero el contrato actual únicamente tiene
`POST /cases` y no define una operación o señal de cierre/notificación.

Se requiere decidir si `POST /cases` crea siempre un caso cerrado (y por tanto exige todos
los campos RF-07), o si crea/actualiza borrador y cuál endpoint/acción explícita lo cierra.
No se añadirá un campo o estado implícito hasta recibir esa decisión.

**Decisión de Cristian (2026-09-30):** `POST /cases` crea y actualiza borradores. El
contrato propone `POST /cases/{uuid}/close` para aplicar RF-07, sin columna de estado.

## 2026-09-30 — plataforma OpenMRS y `encounter_diagnosis`

El `openmrs-api:2.4.2` usado por el OMOD no contiene `org.openmrs.EncounterDiagnosis`.
El modelo vigente exige una FK real `surveillance_case.encounter_diagnosis_id` a la tabla
`encounter_diagnosis`. Se requiere verificar en la instancia desplegada si esa tabla/API
existe y, si no, decidir una actualización de plataforma o una corrección del modelo. No
se sustituirá por `Diagnosis` ni se eliminará la FK sin decisión explícita.

**Decisión de Cristian (2026-09-30):** por compatibilidad con OpenMRS 2.4.2 se usa
`Diagnosis` y FK `diagnosis.diagnosis_id` si no existe `encounter_diagnosis`. Esta es una
excepción explícita al modelo vigente; se valida que el diagnóstico pertenezca al encounter.

## 2026-09-30 — laboratorio temporal

Por decisión de Cristian, mientras Laboratorio no esté implementado completamente, el
contrato acepta `laboratoryObservationUuid` para pruebas. No se persiste esa observación:
el backend solo toma `Obs.order` y guarda `test_order_id` cuando existe; de lo contrario
queda nulo, como permite el modelo.
## 2026-09-30 â€” pendiente: esquema React Form Engine para registro de casos

El React Form Engine del frontend está diseñado para crear/editar encuentros y `Obs`.
Para llevar la gestación al encuentro, como exige el modelo, su esquema JSON necesita el
UUID real del concepto de gestación (y de sus respuestas). No se han proporcionado esos
UUID ni está disponible la BD de referencia, por lo que no se puede construir un esquema
ejecutable sin inventar metadatos. El formulario actual persiste los atributos propios de
`surveillance_case` por el contrato REST y debe conectarse al esquema de encuentro when configuration is available.

## 2026-09-30 - migration of the seven current tables

Changesets 1-12, already applied, created historical tables
`surveillance_notifiable_event`, `surveillance_outbreak_alert_rule`,
`surveillance_period_case_count`, `surveillance_epidemiological_focus`,
`surveillance_audit`, and `surveillance_noti_notification`. The current model instead
requires seven physical tables: `notifiable_event`, `outbreak_alert_rule`,
`outbreak_alert`, `period_case_count`, `epidemiological_focus`, `surveillance_case`,
and `individual_record`; it eliminates the component audit and notification tables.

Cristian confirmed changesets 13 and 14 have not run and they can be rewritten. Creating
the seven new tables without migrating or removing the historical six would create
duplicates and violate the requirement for exactly seven. Explicit approval is required
for incremental changesets that rename/transform the historical tables and remove or
migrate the component audit and notification tables. Changesets 1-12 will not be edited.

**Decisión de Cristian (2026-09-30):** después de realizar una copia SQL externa,
autoriza descartar los datos históricos de esas seis tablas y aplicar los changesets 13 y
14 que las retiran y crean exactamente las siete tablas vigentes. El rollback de esta
operación no puede recuperar los datos descartados; la restauración de datos corresponde
a la copia SQL externa.

## 2026-09-30 — ciclo de vida de `notifiable_event`

`modelo-datos.md` exige versionar el evento ante un cambio normativo: se agrega una fila
con un nuevo `valid_from` y no se elimina la anterior. El código heredado todavía expone
`PUT /events/{uuid}` que modifica la misma fila y `DELETE /events/{uuid}` que usa los
campos transitorios `retired`, `name` y `deadlineDays`; esos campos no existen en el
esquema vigente (el nombre viene de `concept_name` y el plazo se deriva de
`periodicity`).

Se requiere definir el contrato administrativo para la versión nueva: si el `POST` debe
crear cada versión y cómo se cierra una vigencia (`valid_to`). No se modificará ese
contrato ni se reintroducirán columnas heredadas sin esa decisión.

## 2026-09-30 — integración con auditoría clínica no verificable localmente

La arquitectura exige reutilizar `openmrs-module-sihsalus-audit` y entregar los eventos de consulta, edición (campo, valor anterior y nuevo), exportación y reporte. Ese OMOD no está disponible en este workspace, ni se conoce todavía su identificador/versión ni su API pública. Por tanto no se puede declarar `require_module` ni afirmar que intercepte las entidades Hibernate propias de vigilancia sin inventar metadatos.

Se documentó el contrato requerido en `docs/auditoria-integracion.md`. Se necesita el repositorio o una instancia de referencia con ese módulo para completar la integración; si no captura `surveillance_case`, la extensión corresponde a dicho OMOD.
