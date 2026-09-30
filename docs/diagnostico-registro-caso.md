# Diagnóstico del registro de caso

Fecha: 2026-09-30. Estado: **diagnóstico no reproducido; corrección omitida por decisión de Cristian**.

## Síntoma comunicado

El registro de un caso falla y se indicó una posible relación con FHIR2. No se recibió
una fecha, usuario, captura de red, respuesta HTTP ni traza de servidor del incidente.

## Evidencia obtenida

| Comprobación | Resultado | Evidencia |
|---|---|---|
| Instancia local de OpenMRS | No disponible | No hay proceso de OpenMRS/Tomcat ejecutándose en el equipo; solo se observó Java del IDE. No se encontró `openmrs.log`. |
| Entorno compartido | No accesible desde este equipo | `gidis-hsc-dev.inf.pucp.edu.pe` resolvió a `200.16.7.137`, pero la conexión TCP a 443 falló. |
| Envío de registro en el frontend fuente | REST del OMOD, no FHIR2 | `src/api.ts` ejecuta `POST ${restBaseUrl}/sihsalusepidemiologicalsurveillance/cases`; `offline.ts` solo reintenta esa misma función. |
| Lecturas previas al registro | REST Web Services nativo | El formulario consulta `patient`, `encounter`, `obs`, `provider` y `location` bajo `${restBaseUrl}`. |
| Uso de FHIR2 en el paquete | Dependencia declarada solamente | `routes.json` declara `fhir2 >=1.2.0`, pero no hay referencias productivas a `fhirBaseUrl`, `/ws/fhir2` ni `fhir2` en `src/`. |
| Dependencias/módulos del OMOD | Incompleto para FHIR2 | `config.xml` exige únicamente `org.openmrs.module.webservices.rest >=2.2.0`; no declara FHIR2, Address Hierarchy, auditoría ni notificaciones como requeridos o conocidos. |
| Riesgo de classloader FHIR | No probado | El API declara `jackson-databind:2.11.2` como `provided`; sin la lista de JARs/módulos desplegados ni log no puede establecerse compatibilidad con HAPI FHIR/Jackson. |

## Petición y respuesta

No fue posible capturar una petición real ni una respuesta real: no hay instancia local
ni conectividad al servidor compartido. La petición **esperada por el código fuente**,
que no sustituye la captura requerida, es:

```http
POST /openmrs/ws/rest/v1/sihsalusepidemiologicalsurveillance/cases
Content-Type: application/json

{
  "uuid": "<idempotency UUID>",
  "patientUuid": "<uuid>",
  "sourceEncounterUuid": "<uuid>",
  "providerUuid": "<uuid>",
  "locationUuid": "<uuid>",
  "eventUuid": "<uuid>",
  "status": "<value>",
  "severity": "<value>",
  "origin": "<value>",
  "species": "<optional value>",
  "onsetDate": "YYYY-MM-DD",
  "laboratoryResultUuid": "<optional uuid>"
}
```

No hay URL, verbo, cuerpo, estado HTTP, `openmrs.log` ni traza de excepción reales
que adjuntar aún.

## Determinación actual

No se puede atribuir el fallo a FHIR2. En la versión fuente inspeccionada, FHIR2 no
participa en el envío ni en las lecturas del formulario. Las hipótesis pendientes incluyen
que el entorno tenga una versión distinta del frontend, que falle uno de los recursos REST
previos, o que el endpoint del OMOD falle al persistir la transacción. Ninguna está
confirmada.

## Arreglo propuesto y riesgo

No se propone ni aplica un arreglo hasta obtener evidencia del incidente. Cristian indicó
omitir esta corrección y continuar con el registro completo; el posible uso histórico del
nombre `Case` no se considera causa raíz probada. Cambiar
dependencias FHIR2/Jackson, `config.xml`, mappings de conceptos o el endpoint sin la
traza podría ocultar la causa o romper la carga de módulos.

Para desbloquear el diagnóstico se requiere una de estas alternativas:

1. Una instancia local con el OMOD instalado, datos de prueba y acceso a `openmrs.log`.
2. Acceso de solo lectura a los logs y a la lista de módulos/versiones del entorno
   compartido, más un HAR/captura de red de un intento fallido.
3. La petición/respuesta exactas y la traza completa del fallo ya observado.

Con esos artefactos se verificará de forma correlacionada el endpoint que falla, módulos y
versiones, privilegios, conceptos, rollback/FK/UK y el esquema Liquibase/Hibernate antes
de escribir la prueba de regresión y la corrección.
