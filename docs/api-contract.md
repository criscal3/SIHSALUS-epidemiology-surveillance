# Contrato REST — Vigilancia Epidemiológica

Versión: 1.0-draft, 2026-09-30. Base: `/openmrs/ws/rest/v1/sihsalusepidemiologicalsurveillance`.
Autenticación: sesión OpenMRS. Todas las referencias se expresan como UUID de OpenMRS,
excepto `infectionAddressUuid`, que es el UUID de `address_hierarchy_entry`.

## Casos de vigilancia

| Operación | Ruta | Privilegio | Resultado |
|---|---|---|---|
| Crear borrador | `POST /cases` | `Vigilancia Epidemiologica: Registrar Casos` | 201 `SurveillanceCase` |
| Editar borrador | `PUT /cases/{uuid}` | `Vigilancia Epidemiologica: Registrar Casos` | 200 `SurveillanceCase` |
| Consultar caso | `GET /cases/{uuid}` | `Vigilancia Epidemiologica: Ver Casos` | 200 `SurveillanceCase` |
| Cerrar/notificar caso | `POST /cases/{uuid}/close` | `Vigilancia Epidemiologica: Registrar Casos` | 200 `SurveillanceCase` |

`POST` y `PUT` permiten campos epidemiológicos incompletos para conservar un borrador.
La operación `close` no persiste una columna de estado: solo valida RF-07 y deja el caso
listo para el flujo de notificación de iteración 2. Mientras no exista dicho flujo,
`close` no asigna `individualRecordUuid`.

### Cuerpo de creación o edición

```json
{
  "patientUuid": "uuid",
  "encounterUuid": "uuid",
  "providerUuid": "uuid",
  "locationUuid": "uuid",
  "encounterDiagnosisUuid": "uuid",
  "testOrderUuid": "uuid opcional",
  "laboratoryObservationUuid": "uuid temporal de Obs; el servidor usa Obs.order si existe",
  "origin": "AUTOCTONO | IMPORTADO_NACIONAL | IMPORTADO_INTERNACIONAL | INDUCIDO | INTRODUCIDO | RECAIDA | RECRUDESCENCIA",
  "onsetDate": "YYYY-MM-DD opcional",
  "infectionAddressUuid": "uuid de centro poblado opcional",
  "diagnosisType": "CONFIRMADO | PROBABLE | DESCARTADO",
  "vaccinationStatus": "SI | NO | IGN opcional",
  "investigationDate": "YYYY-MM-DD opcional",
  "notificationDate": "YYYY-MM-DD opcional",
  "deathDate": "YYYY-MM-DD opcional",
  "surveillanceType": "PASIVA | BUSQUEDA_ACTIVA opcional"
}
```

El servidor deriva evento notificable y gravedad desde `encounterDiagnosisUuid` y los
conceptos configurados; no acepta esos valores como campos del caso. Nombres, edad, sexo,
residencia y etnia no se aceptan ni se copian: se leen desde `patient`. Gestación se
registra como `Obs` del encuentro mediante el formulario clínico.

### Respuesta `SurveillanceCase`

```json
{
  "uuid": "uuid",
  "patientUuid": "uuid",
  "encounterUuid": "uuid",
  "providerUuid": "uuid",
  "locationUuid": "uuid",
  "encounterDiagnosisUuid": "uuid",
  "testOrderUuid": "uuid o null",
  "origin": "AUTOCTONO",
  "onsetDate": "2026-09-30",
  "infectionAddressUuid": "uuid o null",
  "diagnosisType": "PROBABLE",
  "vaccinationStatus": "IGN",
  "investigationDate": null,
  "notificationDate": null,
  "deathDate": null,
  "surveillanceType": "PASIVA",
  "individualRecordUuid": null
}
```

Errores: 400 JSON malformado; 401 sesión ausente; 403 privilegio insuficiente; 404 una
referencia no existe; 409 ya existe un caso para `encounterDiagnosisUuid`; 422 campos,
enumeraciones, fechas, residencia o centro poblado inválidos. Las respuestas 422 devuelven
`{ "code": "...", "fields": ["..."] }` con mensajes accionables en el cliente.

## Catálogos geográficos

| Operación | Ruta | Resultado |
|---|---|---|
| Provincias | `GET /addresses/provinces` | entradas de Address Hierarchy |
| Distritos | `GET /addresses/districts?parent={uuid}` | hijos de la provincia |
| Centros poblados | `GET /addresses/populated-centers?parent={uuid}` | hijos del distrito |

Solo el último endpoint entrega referencias que el backend acepta como
`infectionAddressUuid`.
