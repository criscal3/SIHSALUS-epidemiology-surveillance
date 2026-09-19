# Vigilancia epidemiológica — SIH Salus

OMOD de la iteración 1 (RE 3.1). Identidad: `io.github.proyecto-santaclotilde:sihsalusepidemiologicalsurveillance:1.0.0-SNAPSHOT`. OpenMRS **2.4.2**, parent SDK **1.1.1**, bytecode Java **8**.

Implementa registro de casos, alertas inmediatas y de brote, conteos programados y reportes. Frontend: `@sihsalus/esm-epidemiological-surveillance-app`. Terminología: visita = consulta; encounter = atención.

**Estado:** implementación y pruebas locales con datos sintéticos. El catálogo de UUID está en `SurveillanceCatalog.java`; el OMOD comprueba que los conceptos y eventos existan antes de operar. El [informe RE 3.1](docs/informe-pruebas-iteracion-1.md) distingue pruebas locales de aceptación pendiente.

## Alcance

| Requisitos | Implementación |
| --- | --- |
| RF-01 a RF-07, RF-26 | Validadores y servicio: estado, laboratorio, gravedad, origen, CIE-10, datos fuente y duplicados. |
| RF-11 | Periodicidad y plazo del catálogo; alertas inmediatas prevalecen con plazo cero. |
| RF-12, RF-13 | Motor con alertas inmediatas separadas de las tres condiciones de brote. |
| RF-17 a RF-19, RF-22 | Curva, canal endémico y siete dimensiones demográficas. |
| RF-27 | Privilegios explícitos en servicios y pantallas. |
| RNF-04 a RNF-06 | Registro en tres pasos, mensajes accionables, alertas con texto e iconos. |

Son 16 de 28 RF previstos; esto indica **alcance implementado, no aceptación clínica**. UUID reales, fuente temporal de gestación y reglas aprobadas requieren verificación. NOTI/exportaciones, edición, padrón de febriles, clasificación automática de focos y auditoría completa corresponden a iteración 2.

## Arquitectura

- `api/`: entidades Hibernate, DAO, servicio transaccional, validadores, cálculos y scheduler.
- `omod/`: descriptor y controlador Spring MVC siguiendo la referencia imaging; publica `/openmrs/ws/rest/v1/sihsalusepidemiologicalsurveillance`.
- El registro **completa la atención existente «Atención de Enfermedades Metaxénicas»** (`ff7327ce-f06e-4a2e-ba2a-8ef0cf7b89d7`): agrega Obs nativas y un Diagnosis solo si falta. Conserva UUID, fecha, visita, localidad, profesional y datos previos de esa atención. La referencia textual al resultado conserva el TestOrder.
- El `uuid` de la solicitud identifica el intento idempotente; el `uuid` de la respuesta identifica la atención ya existente. La huella del intento queda en la Obs de inicio de síntomas.
- Un descartado conserva su clasificación en Obs y no genera un diagnóstico nuevo; tampoco anula diagnósticos previos de la atención. Los reportes solo cuentan confirmados. Esta convención requiere aceptación clínica.
- Las seis tablas nuevas son `evento_notificable`, `regla_alerta_brote`, `conteo_casos_periodo`, `foco_epidemiologico`, `auditoria_vigilancia` y `notificacion_noti`. UUID únicos y claves foráneas reales; conteo único por evento/periodo/año/número.
- Se conserva el changeset original del scaffold por compatibilidad; su tabla no se utiliza. Changesets nuevos incrementales 1–7; sin borrado de auditoría.
- Todo acceso usa la **base original**. Solo se admite `analyticsDatasource: "primary"`; `"replica"` se rechaza. El datasource de réplica requiere implementación y pruebas posteriores.

## Preparación del contenido

No se usa un archivo JSON ni una global property de metadatos en ejecución. `docs/vigilancia.json` sirvió para contrastar los valores con `sihsalus-content`; sus identificadores numéricos OCL de diagnósticos fueron sustituidos por los `external_id` UUID de OpenMRS y códigos CIE-10 publicados en ese contenido. El catálogo Java es la fuente de ejecución.

1. Cargar el contenido de `sihsalus-content` y comprobar en la instancia los conceptos, respuestas, tipo de atención, rol y atributo Etnia. Los UUID de diagnósticos vienen de `external_id` en el export OCL; la columna `uuid` de ese export es un identificador OCL numérico.
2. Instalar el OMOD. El changeset 7 crea los eventos administrativos de dengue y malaria solo si los conceptos respectivos existen; nunca supone un `concept_id` local. `GET /metadata` falla con 503 si faltan eventos o referencias requeridas.
3. Comprobar que los códigos de diagnóstico y la atención existente correspondan al contenido cargado. El servidor valida preguntas, respuestas y referencias antes del registro.
4. Crear reglas mediante `POST /rules`. Poblar focos revisados por epidemiología; no hay clasificación automática ni pantalla administrativa de focos todavía.
5. Establecer fecha inicial de **cobertura completa y verificada**, calendario, ventana de duplicados y años históricos. No declarar cobertura antigua para fabricar ceros.
6. Asignar privilegios, recalcular con `POST /counts/refresh` y verificar catálogo, casos y reportes con pacientes sintéticos.

Preguntas requeridas: `event,status,severity,origin,species,pregnancy,ethnicity` Coded; `onset` Date; `laboratoryResult` Text. Verdadero/falso se resuelven desde los conceptos booleanos nativos. La pregunta de evento no tiene respuestas declaradas en `sihsalus-content`; el servidor restringe el valor a los dos eventos del catálogo fijo. Si falta contenido técnico, se requiere un changelog de contenido; no sustituir conceptos por semejanza.

Claves de estado: `SUSPECTED,CONFIRMED,DISCARDED`. Orígenes: `AUTOCHTHONOUS,IMPORTED_NATIONAL,IMPORTED_INTERNATIONAL,INDUCED,INTRODUCED,RELAPSE,RECRUDESCENCE`. La gravedad grave usa `SEVERE`; etiquetas y conceptos son configurables. Malaria requiere especies y sus mappings; dengue puede omitir especies. Configurar cada prueba local (gota gruesa/PDR, serología/NS1) con concepto de orden, pregunta de resultado y respuestas positivas/negativas verificadas. No se interpreta texto libre como resultado.

### Etnia y gestación

Etnia: PersonAttributeType `8d871386-c2cc-11de-8d13-0010c6dffd0f` de tipo Concept, u Obs Coded de la atención. Gestación: Obs Coded explícita de esa atención; no se configura un atributo de persona sin confirmar una fuente temporal. Lo desconocido sigue siendo desconocido.

Se revisó `esm-crecimiento-desarrollo-app/src/hooks/useCurrentPregnancy.ts`: obtiene un episodio prenatal asociado a `OBST-002-EMBARAZO ACTUAL`. Eso no demuestra por sí solo gestación vigente; tampoco se infiere a partir de FUM. No se confirmó el atributo real ni se creó uno potencialmente duplicado. Si se verifica su ausencia, queda pendiente incorporarlo por changelog y acordar su actualización al terminar el embarazo. RF-12/RF-22 requieren aceptación de esta fuente temporal.

## Registro y laboratorio

El UUID del payload es estable en reintentos. El registro usa aislamiento READ_COMMITTED y bloquea la fila del paciente antes de comprobar duplicados; también bloquea el evento antes de persistir y evaluar cruces de umbral entre pacientes. Misma atención, UUID de solicitud, autor y contenido devuelve `replayed: true`; discrepancias producen 409. Una huella SHA-256 del payload en la Obs de inicio permite comparar reintentos. Los llamadores internos deben preservar ese aislamiento; probar concurrencia en la versión MySQL/MariaDB de destino.

La ventana de posibles duplicados es simétrica e inclusiva respecto al inicio, para el paciente y diagnóstico de la enfermedad. No hay override. Fecha válida entre nacimiento y atención, sin superar hoy ni defunción registrada. Atención/visita deben pertenecer al paciente y no estar anuladas; proveedor asociado al usuario autenticado.

Confirmados/descartados exigen resultado codificado coherente, no anulado, del mismo paciente y episodio, con TestOrder y fecha compatible: positivo → confirmado; negativo → descartado. No se integra NetLab ni se considera un adjunto como evidencia estructurada.

## Alertas e indicadores

- Inmediatas: grave, defunción registrada, foco eliminado/no receptivo o caso confirmado con gestación explícita. Descartados no generan alertas.
- Brote: cruce de Q3 por nuevo confirmado; incremento estricto durante dos semanas consecutivas ≥Q2; confirmado autóctono en foco eliminado. Activación por evento y ventana sostenida de 2–12 semanas. Umbrales RF-13 fijos Q3=75/Q2=50.
- Se devuelve la evaluación y se guarda una alerta nativa dirigida al autor. No equivale a envío a NOTI ni a un destinatario institucional externo.
- Reportes/conteos de **confirmados**, por inicio de síntomas. Edad al inicio, grupos 0–4/5–11/12–17/18–29/30–59/60+, sexo, enfermedad, etnia, gestación y periodo. Sin nombres ni UUID de pacientes.
- Cuartiles por interpolación lineal tipo 7; 5 años anteriores por defecto, mínimo 3. Día por mes/día, semana por año epidemiológico y número, sin desfasar años bisiestos.
- Calendario inicial: America/Lima, domingo, cuatro días mínimos en primera semana; **confirmar con epidemiología**.
- Sin historia suficiente: `INSUFFICIENT_HISTORY`, umbrales nulos. Periodo incompleto: `PARTIAL_PERIOD`, sin zona definitiva. Periodo completo: >Q3 epidemia; ≥Q2 alerta; ≥Q1 seguridad; por debajo éxito. Cero actual e historia toda cero es éxito.
- Rango de hasta 731 días de diferencia; periodos `dia,semana,mes,trimestre,semestre`. No se rellenan con ceros años anteriores a cobertura ni el primer periodo parcialmente cubierto.

### Scheduler

Changeset 7: **SIH Salus surveillance counts**, `RefreshCountsTask`, cada 3600 segundos, inicio al arrancar OpenMRS. Tras instalación en caliente, iniciar desde la administración del scheduler o reiniciar después de configurar metadatos. Usa el daemon nativo y el servicio transaccional; `POST /counts/refresh` permite ejecución manual autorizada.

Reconstrucción atómica de las cinco periodicidades, con bloqueo por evento y sin borrar datos clínicos. Correcciones/anulaciones requieren recálculo. La fecha de generación del reporte no garantiza la frescura del último refresco histórico. Dimensionar duración e índices con el volumen real.

## Seguridad

[Contrato REST v1](docs/rest-contract.md).

| Acción | Privilegio exacto |
| --- | --- |
| Ruta/catálogo | `app:home.epidemiologicalSurveillance` |
| Consultar caso | `Vigilancia Epidemiologica: Ver Casos` |
| Registrar/sincronizar | `Vigilancia Epidemiologica: Registrar Casos` y `Vigilancia Epidemiologica: Ver Casos` |
| Reportes | `Vigilancia Epidemiologica: Ver Indicadores` |
| Eventos/reglas/recálculo | `Vigilancia Epidemiologica: Configurar` |

Cadenas sin tilde; no se asignan automáticamente a roles. Los servicios nativos y FHIR exigen además sus propios privilegios de lectura/escritura clínica. No se añaden privilegios proxy.

Auditoría básica de operaciones exitosas: usuario, fecha, acción, entidad e ID. No guarda payload clínico ni implementa aún diffs, accesos denegados o retención de 20 años. Errores REST seguros, sin excepciones nativas. Los agregados también requieren autorización.

## Desarrollo y validación

```sh
mvn --batch-mode --show-version --no-transfer-progress clean verify
```

Artefacto: `omod/target/sihsalusepidemiologicalsurveillance-1.0.0-SNAPSHOT.omod`. Resultados en `api/target/surefire-reports` y `omod/target/surefire-reports`; cobertura JaCoCo en `target/site/jacoco/index.html` de cada submódulo.

Harness Spring/Hibernate de OpenMRS Core, H2 sintético, JUnit/Mockito. El perfil JDK moderno conserva `--release 8` y abre paquetes requeridos por dependencias antiguas de pruebas. JaCoCo 0.8.14 admite JDK 25 ([historial oficial](https://www.jacoco.org/jacoco/trunk/doc/changes.html)). Formatter OpenMRS e impsort siguen imaging. La licencia institucional sigue pendiente según convenciones; no se impone un encabezado legal nuevo.

Antes de instalar: probar changelogs en copia sintética MySQL/MariaDB de la versión real, comprobar REST/FHIR2, privilegios y contenido, y realizar la aceptación del informe. No se desplegó ni se conectó a una base clínica real.
