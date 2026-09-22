# Contrato REST de vigilancia — versión 1

Base externa: `/openmrs/ws/rest/v1/sihsalusepidemiologicalsurveillance` (adaptar contexto OpenMRS). Autenticación de sesión nativa; JSON. El DTO de contenido tiene `version: 1`. No existe token o identidad alternativos del componente.

## Endpoints

| Método y ruta | Privilegio adicional | Éxito |
| --- | --- | --- |
| GET /catalog | app:home.epidemiologicalSurveillance | 200 `{catalog: ClinicalCatalog, events: Event[]}` |
| GET /healthcheck | app:home.epidemiologicalSurveillance | 200 `{"status":"UP"}` |
| GET /events | app:home.epidemiologicalSurveillance | 200 `{results: Event[]}`; `includeRetired=true` incluye retirados |
| GET /events/{uuid} | app:home.epidemiologicalSurveillance | 200 Event, incluso retirado |
| PUT /events/{uuid} | Configurar | 200 Event actualizado |
| DELETE /events/{uuid} | Configurar | 204; baja lógica idempotente |
| POST /cases | Registrar Casos + Ver Casos | 200 CaseResult; replayed indica reintento |
| GET /cases/{uuid} | Ver Casos | 200 CaseResult |
| GET /reports | Ver Indicadores | 200 SurveillanceReport |
| POST /events | Configurar | 200 Event |
| POST /rules | Configurar | 200 `{uuid}` |
| POST /counts/refresh | Configurar | 200 `{refreshed: true}` |

Los nombres abreviados corresponden a `Vigilancia Epidemiologica: <nombre>`. No se codifican nombres de roles.

## CaseRequest

```json
{
  "uuid": "<UUID estable de la solicitud>",
  "patientUuid": "<paciente existente>",
  "sourceEncounterUuid": "<atención existente de cualquier tipo ligada a visita>",
  "providerUuid": "<proveedor del usuario>",
  "locationUuid": "<localidad activa>",
  "eventUuid": "<evento configurado>",
  "status": "SUSPECTED",
  "severity": "<clave configurada>",
  "origin": "AUTOCHTHONOUS",
  "species": null,
  "onsetDate": "2026-01-19",
  "laboratoryResultUuid": null
}
```

Marcadores ilustrativos, no seed. Especie obligatoria cuando la enfermedad configura especies. Resultado obligatorio para CONFIRMED/DISCARDED. El servidor rechaza campos obligatorios incompletos, referencias inválidas, fecha incoherente, proveedor ajeno, resultado/orden/episodio incompatible o diagnóstico sin mapping único.

CaseResult: `uuid, diagnosisConceptUuid, icd10, periodicity, deadlineDays, replayed, immediateAlerts: string[], outbreakAlerts: string[], warnings: string[]`. Periodicidad `semanal,inmediata,diaria`. Los arrays se devuelven incluso vacíos.

Alerta inmediata: `SEVERE_CASE,DEATH,ELIMINATED_FOCUS,NON_RECEPTIVE_FOCUS,CONFIRMED_PREGNANCY`. Brote: `EPIDEMIC_THRESHOLD,SUSTAINED_INCREASE,AUTOCHTHONOUS_ELIMINATED`. Advertencias: `FOCUS_UNKNOWN,PREGNANCY_UNKNOWN,INSUFFICIENT_HISTORY`.

El POST es transaccional e idempotente por atención+UUID de solicitud+autor+contenido. Completa la atención existente; no crea otra. `CaseResult.uuid` y `GET /cases/{uuid}` usan el UUID de esa atención. Una atención ya completada con otra solicitud/contenido produce 409. El GET vuelve a evaluar el registro con la información disponible; no sustituye el historial identificable de OpenMRS.

## Reporte

Query requerida: `event,from,to`; `period` opcional, defecto `semana`. Fechas ISO, rango inclusivo; periodos `dia,semana,mes,trimestre,semestre`.

```json
{
  "eventUuid": "<evento>",
  "from": "2026-01-01",
  "to": "2026-01-01",
  "period": "dia",
  "generatedAt": "<instante UTC>",
  "total": 0,
  "curve": [{"date":"2026-01-01","cases":0}],
  "channel": [{"date":"2026-01-01","cases":0,"year":2026,"number":1,"q1":null,"q2":null,"q3":null,"sampleSize":0,"zone":"INSUFFICIENT_HISTORY"}],
  "demographics": {"age":{},"ageGroup":{},"sex":{},"disease":{},"ethnicity":{},"pregnancy":{},"period":{}},
  "warnings": ["INSUFFICIENT_HISTORY"]
}
```

Cada dimensión es un mapa categoría → conteo. Embarazo: YES/NO/UNKNOWN; faltantes: UNKNOWN; enfermedad: UUID evento; periodo: fecha de inicio. Zonas: SUCCESS/SAFETY/ALERT/EPIDEMIC/INSUFFICIENT_HISTORY/PARTIAL_PERIOD. Periodos parciales conservan conteo/umbrales disponibles, sin clasificación definitiva.

## Administración

Event entrada: `{uuid, name, conceptUuid, periodicity, deadlineDays}`. La respuesta añade `retired`. POST crea un evento nuevo; UUID o concepto duplicado producen 409. PUT requiere un evento existente y todos los campos salvo `uuid`, que se toma de la ruta; un UUID de cuerpo distinto produce 409. El concepto nativo es inmutable. Periodicidad: `semanal`, `diaria` o `inmediata`; plazo entre 0 y 365 días.

DELETE retira el evento y registra auditoría. Conserva casos, reglas, conteos y referencias históricas; no permite nuevos casos ni editar el evento retirado. Repetir DELETE devuelve 204. Los reportes históricos siguen disponibles por UUID. GET/PUT/DELETE sobre un UUID inexistente devuelven 404 `EVENT_NOT_FOUND`.

Ejemplo de PUT `/events/{uuid}`:

```json
{"name":"Dengue","conceptUuid":"41cb3fbf-f50c-4d7d-87bc-1b5dd963f8d0","periodicity":"semanal","deadlineDays":7}
```

Crear un evento administrativo no añade automáticamente diagnósticos, especies ni reglas clínicas al catálogo fijo de enfermedades.

El healthcheck comprueba que el servicio responde y autoriza la sesión; no certifica que todo el contenido clínico esté cargado. El catálogo procede de constantes Java y no de un archivo ni de una global property.

Rule entrada: `{uuid, eventUuid, condition, active, windowWeeks?}`. Condiciones: EPIDEMIC, SUSTAINED, ELIMINATED_FOCUS. Ventana 2–12, defecto 2; se usa únicamente para SUSTAINED. Umbral almacenado 75 para EPIDEMIC, 50 para SUSTAINED, null para foco. No hay CRUD de focos ni notificaciones NOTI.

## Errores

`{"code":"REQUIRED_FIELDS","fields":["origin"]}`, con nombres de campos opcionales/vacíos. Nunca enviar mensajes de excepción, SQL ni valores clínicos en el error. Consultar las traducciones del ESM para acciones sugeridas.

HTTP: 400 JSON inválido; 401 sesión requerida; 403 permiso/proveedor inválido; 404 caso o evento ausente; 409 posible duplicado, conflicto de idempotencia, evento retirado o conflicto de identidad; 422 validación; 503 referencia clínica requerida ausente (`CLINICAL_CONCEPT_UNAVAILABLE`), tipo incompatible (`CLINICAL_DATATYPE_MISMATCH`) o mapping/regla inválidos; 500 fallo inesperado seguro. Los errores clínicos identifican la pregunta en `fields` cuando corresponde y se evalúan al utilizarla.

No hay edición ni eliminación de casos por esta API. La lectura/escritura nativa OpenMRS y sus permisos siguen vigentes.
