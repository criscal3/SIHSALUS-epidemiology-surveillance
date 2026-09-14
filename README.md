# Vigilancia epidemiológica — SIH Salus

OMOD de la iteración 1 (RE 3.1). Identidad: `io.github.proyecto-santaclotilde:sihsalusepidemiologicalsurveillance:1.0.0-SNAPSHOT`. OpenMRS **2.4.2**, parent SDK **1.1.1**, bytecode Java **8**.

Implementa registro de casos, alertas inmediatas y de brote, conteos programados y reportes. Frontend: `@sihsalus/esm-epidemiological-surveillance`. Terminología: visita = consulta; encounter = atención.

**Estado:** implementación y pruebas locales con datos sintéticos. Las operaciones clínicas permanecen bloqueadas hasta configurar metadatos existentes y verificados. No se precargan UUID clínicos de producción ni datos demo. El [informe RE 3.1](docs/informe-pruebas-iteracion-1.md) distingue pruebas locales de aceptación pendiente.

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
- Un caso crea **Encounter, Diagnosis y Obs nativos**, asociado a la visita fuente. Clasificación codificada, inicio de síntomas Date y referencias Text a UUID de atención/resultado; la última conserva el TestOrder.
- El encounter usa la fecha de la atención fuente para permitir captura retrospectiva y sincronización después de cerrar la visita. Creación y auditoría registran el guardado efectivo.
- Un descartado conserva su clasificación en Obs; su Diagnosis queda anulado y excluido de diagnósticos activos/conteos. Esta convención requiere aceptación clínica.
- Las seis tablas nuevas son `evento_notificable`, `regla_alerta_brote`, `conteo_casos_periodo`, `foco_epidemiologico`, `auditoria_vigilancia` y `notificacion_noti`. UUID únicos y claves foráneas reales; conteo único por evento/periodo/año/número.
- Se conserva el changeset original del scaffold por compatibilidad; su tabla no se utiliza. Changesets nuevos incrementales 1–7; sin borrado de auditoría.
- Todo acceso usa la **base original**. Solo se admite `analyticsDatasource: "primary"`; `"replica"` se rechaza. El datasource de réplica requiere implementación y pruebas posteriores.

## Preparación del contenido

La global property `sihsalusepidemiologicalsurveillance.metadata` contiene JSON versión 1. Véase [metadata.example.json](docs/metadata.example.json): los marcadores `<...>` **no son UUID ni un seed ejecutable**.

1. Consultar Concept, ConceptSource, EncounterType, EncounterRole y PersonAttributeType existentes mediante [OpenMRS REST](https://rest.openmrs.org/) o una exportación autorizada. Verificar nombres, tipos, respuestas y mappings CIE-10; no deducir identidad por nombres similares.
2. Crear eventos administrativos con `POST /events`, vinculados a conceptos clínicos existentes. El UUID del evento es nuevo; el concepto asociado ya debe existir y no puede cambiarse posteriormente.
3. Completar la global property mediante la administración de OpenMRS. El servidor valida referencias, tipos de preguntas, respuestas permitidas y mappings inequívocos por gravedad/especie.
4. Crear reglas mediante `POST /rules`. Poblar focos revisados por epidemiología; no hay clasificación automática ni pantalla administrativa de focos todavía.
5. Establecer fecha inicial de **cobertura completa y verificada**, calendario, ventana de duplicados y años históricos. No declarar cobertura antigua para fabricar ceros.
6. Asignar privilegios, recalcular con `POST /counts/refresh` y verificar catálogo, casos y reportes con pacientes sintéticos.

Preguntas requeridas: `event,status,severity,origin,species` Coded; `onset` Date; `pregnancy` Boolean; `sourceEncounter,laboratoryResult,ethnicity` Text. Verdadero/falso se resuelven desde los conceptos booleanos nativos. Si falta contenido técnico de procedencia, preparar un changelog después de verificar su ausencia; no sustituir conceptos por semejanza.

Claves de estado: `SUSPECTED,CONFIRMED,DISCARDED`. Orígenes: `AUTOCHTHONOUS,IMPORTED_NATIONAL,IMPORTED_INTERNATIONAL,INDUCED,INTRODUCED,RELAPSE,RECRUDESCENCE`. La gravedad grave usa `SEVERE`; etiquetas y conceptos son configurables. Malaria requiere especies y sus mappings; dengue puede omitir especies. Configurar cada prueba local (gota gruesa/PDR, serología/NS1) con concepto de orden, pregunta de resultado y respuestas positivas/negativas verificadas. No se interpreta texto libre como resultado.

### Etnia y gestación

Etnia: PersonAttributeType configurado o Obs Text de la atención fuente. Gestación: Obs Boolean explícita de esa atención; opcionalmente un PersonAttributeType **Boolean verificado**. Lo desconocido sigue siendo desconocido.

Se revisó `esm-crecimiento-desarrollo-app/src/hooks/useCurrentPregnancy.ts`: obtiene un episodio prenatal asociado a `OBST-002-EMBARAZO ACTUAL`. Eso no demuestra por sí solo gestación vigente; tampoco se infiere a partir de FUM. No se confirmó el atributo real ni se creó uno potencialmente duplicado. Si se verifica su ausencia, queda pendiente incorporarlo por changelog y acordar su actualización al terminar el embarazo. RF-12/RF-22 requieren aceptación de esta fuente temporal.

## Registro y laboratorio

El UUID del payload es estable en reintentos. El registro usa aislamiento READ_COMMITTED y bloquea la fila del paciente antes de comprobar duplicados; también bloquea el evento antes de persistir y evaluar cruces de umbral entre pacientes. Mismo UUID, paciente, autor y contenido devuelve `replayed: true`; discrepancias producen 409. Una huella SHA-256 del payload en la Obs de inicio permite comparar reintentos. Los llamadores internos deben preservar ese aislamiento; probar concurrencia en la versión MySQL/MariaDB de destino.

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
