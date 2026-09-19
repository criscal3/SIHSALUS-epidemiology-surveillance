# Informe de pruebas — Iteración 1 / RE 3.1

Fecha: **2026-09-14**. Entorno: Windows 11, Maven 3.9.16, JDK 25.0.2 (compilación Java 8), OpenMRS 2.4.2, Node 24.20.0, Yarn 4.13.0, Vitest 4.1.11. Datos exclusivamente sintéticos; sin acceso a base clínica real ni despliegue.

## Resultado reproducible

| Suite | Ejecutadas | Exitosas | Fallidas/errores | Omitidas |
| --- | ---: | ---: | ---: | ---: |
| Backend API (JUnit, Spring/Hibernate, H2) | 60 | 60 | 0 | 0 |
| Backend OMOD / controlador | 3 | 3 | 0 | 0 |
| Frontend (7 archivos Vitest) | 35 | 35 | 0 | 0 |
| **Total** | **98** | **98** | **0** | **0** |

Indicador RE 3.1: backend **63/63 = 100%** de pruebas exitosas, superior a 97%; frontend **35/35 = 100%**. Se cuenta la última ejecución de cada suite, sin multiplicar reintentos. Los fallos detectados durante desarrollo se corrigieron antes de estos resultados. El porcentaje no equivale a cobertura ni acredita aceptación clínica.

## Evidencia y comandos

Backend, desde `epidemiologysurveillance`:

```sh
mvn --batch-mode --show-version --no-transfer-progress clean verify
```

Se ejecutó `clean verify` y, tras el ajuste de aislamiento/bloqueo del registro, `verify`: ambos concluyeron correctamente. Reportes XML en `api/target/surefire-reports/TEST-*.xml` y `omod/target/surefire-reports/TEST-*.xml`.

Artefacto: `omod/target/sihsalusepidemiologicalsurveillance-1.0.0-SNAPSHOT.omod`. Se comprobó el descriptor con el ID correcto, presencia de JAR API/mappings/changelogs y bytecode major **52**. SHA-256 de esta compilación:

```text
F938F1CD676B94199E6D2F3DE3EC4A396F265245B4CDB87909EE6822ABF19462
```

Frontend, desde `sihsalus-frontend`:

```sh
yarn workspace @sihsalus/esm-epidemiological-surveillance-app lint
yarn workspace @sihsalus/esm-epidemiological-surveillance-app typescript
yarn workspace @sihsalus/esm-epidemiological-surveillance-app test
yarn workspace @sihsalus/esm-epidemiological-surveillance-app build
node packages/tooling/scripts/validate-error-exposure.js --base HEAD
node packages/tooling/scripts/validate-critical-route-privileges.js
node packages/tooling/scripts/audit-workspaces.js
```

Las comprobaciones de estilo, tipos, compilación, exposición de errores, privilegios de ruta y contratos de workspaces pasan. `git diff --check` se ejecutó dentro del alcance. El build Rspack no mide el tiempo de carga en el HSC. No se ejecutó la suite completa del monorepo ni `verify:changed`, que incluiría el cambio previo ajeno en `yarn.lock`.

## Qué se comprueba por capa

| Paso / pruebas | Cantidad | Evidencia principal |
| --- | ---: | --- |
| Esquema — LiquibaseMigrationTest | 1 | Changelog raíz completo, aplicación repetida, seis tablas, scheduler único, FK reales y unicidad del conteo. H2 con claves nativas sintéticas. |
| Acceso — SurveillanceDaoTest | 3 | Persistencia/UUID, bloqueo parametrizado del paciente, filtro de diagnósticos anulados y ventana de inicio. |
| Persistencia — NativePersistenceTest | 3 | Harness real OpenMRS, mappings/HQL y guardado de Encounter + 6 Obs + Diagnosis; limpieza de sesión y recarga. Metadatos/acceso del caso son fixtures; servicios clínicos y DAO reales. |
| Contenido — MetadataResolverTest | 6 | Referencias y respuestas válidas, ausencia de configuración, tipo erróneo, respuesta ajena, atributo de gestación no Boolean y rechazo de réplica no implementada. |
| Registro — CaseValidatorTest | 16 | Sospechoso, positivo confirmado, negativo descartado, campos/fechas, proveedor ajeno, paciente/visita incorrectos, laboratorio anulado, inicio posterior a defunción y gestación explícita/desconocida. |
| Servicio — SurveillanceServiceTest | 8 | Registro, auditoría, duplicados sin escritura, reintento sin duplicación, conflicto de contenido, permiso denegado, alerta grave y recálculo. Orden de bloqueo previo a escritura. |
| Motor — OutbreakEngineTest | 10 | Alertas inmediatas, Q3, aumento sostenido ≥Q2, foco eliminado, reglas desactivadas e historia insuficiente. |
| Canal/calendario — EndemicChannelTest, EpidemiologicalCalendarTest | 6 | Cuartiles y bordes, ceros, historia insuficiente, cambio de año epidemiológico y fechas bisiestas. |
| Reportes — ReportCalculatorTest | 6 | Curva/ceros, confirmados, edad al inicio/desconocidos, agregación, fechas comparables entre años y periodos parciales. |
| Scheduler — RefreshCountsTaskTest | 1 | Invocación del servicio, limpieza de estado ante fallo y recuperación en ejecución posterior. |
| API — SurveillanceControllerTest | 3 | Delegación y respuestas de error seguras. Son pruebas del controlador, no una instancia HTTP desplegada. |
| Frontend | 35 | Tres pasos, precarga nativa inequívoca, referencias/fechas, aislamiento por usuario, permisos positivos/negativos, paginación, cola/reintentos, cambios de conexión y reportes vacíos/errores. |

## Cobertura de código

JaCoCo 0.8.14, medición separada del indicador de éxito:

| Submódulo | Líneas | Ramas |
| --- | --- | --- |
| API | 801/1039 = **77.09%** | 418/712 = **58.71%** |
| OMOD | 11/19 = **57.89%** | 1/2 = **50.00%** |

HTML/XML/CSV en `api/target/site/jacoco` y `omod/target/site/jacoco`. No se midió cobertura del frontend. Las ramas aún no cubiertas incluyen administración, variaciones de contenido y errores de servicios nativos; no se ocultan excluyendo clases del informe.

## Alcance y aceptación pendiente

La implementación aborda 16/28 RF y RNF-04/05/06 del plan. **No se declara cierre funcional en el entorno clínico** porque faltan estas verificaciones:

1. **Contenido real:** UUID de conceptos/preguntas/respuestas, tipos/roles, mappings CIE-10, laboratorio local y etnia. La plantilla contiene marcadores, no conceptos inventados.
2. **Gestación:** CRED proporciona un episodio obstétrico; no prueba un estado vigente. Confirmar Obs/atributo y su temporalidad. Si el atributo no existe, añadir el changelog después de comprobar su ausencia. Actualmente no se creó uno potencialmente duplicado.
3. **MySQL/MariaDB:** ejecutar migraciones en copia sintética con la versión real y repetirlas; comprobar integridad, concurrencia de duplicados/cruces de umbral, aislamiento READ_COMMITTED y recuperación transaccional. H2 no sustituye esa aceptación.
4. **DEV/QLTY:** verificar versiones/capacidades REST y FHIR2, publicación de endpoints, privilegios nativos y del componente, registro de sospechoso/confirmado/descartado y persistencia después de recargar.
5. **Validación epidemiológica:** calendario, fecha de cobertura histórica, cuartiles, ventana de duplicados, periodicidad/plazos y focos. Contrastar reportes con el origen, revisar embarazo/defunción y convención de diagnóstico anulado del descartado.
6. **Scheduler e historia:** confirmar arranque real, recálculo después de correcciones/anulaciones y tiempo de ejecución con volumen representativo.
7. **Offline real:** simular caída antes/después del POST, pérdida de respuesta, cierre/reapertura del navegador, reintentos y cambio de cuenta; comprobar cifrado/ciclo de vida de la cola compartida. Los mocks no certifican esas propiedades.
8. **Usabilidad:** revisión humana con teclado, lector de pantalla, tamaños móvil/tablet y responsables de epidemiología. No se midió el objetivo de carga de tres segundos ni se ejecutó E2E en navegador real.

La réplica analítica, exportación NOTI, auditoría completa/retención y demás RF excluidos permanecen fuera de iteración 1.

## Actualización local — 2026-09-19

La vigilancia ahora completa la atención de metaxénicas existente y usa el catálogo Java fijo. Se ejecutó `mvn '-Dmaven.repo.local=C:\Users\smith\.m2\repository' package -q`: **64/64 pruebas backend exitosas** (61 API y 3 OMOD) y artefacto OMOD generado. En el ESM se ejecutaron `yarn.cmd workspace @sihsalus/esm-epidemiological-surveillance-app test`, `typescript`, `lint` y `build`: **36/36 pruebas exitosas**, tipos, estilo y compilación correctos. `yarn.cmd prettier --check packages/apps/esm-epidemiological-surveillance-app/README.md` también pasó. Estas cifras sustituyen las de la tabla histórica para el diff actual; no se analizaron métricas de cobertura actualizadas ni un flujo clínico desplegado.

Se contrastaron los 55 UUID del catálogo Java con CSV y el export OCL de diagnósticos de `sihsalus-content`: 53 son contenido publicado y dos son UUID administrativos de eventos del OMOD. Los diez diagnósticos de `docs/vigilancia.json` usaban inicialmente IDs numéricos OCL; ahora usan los `external_id` UUID y códigos CIE-10 del export. Falta comprobar en DEV/QLTY que todo ese contenido esté importado y que el changeset de eventos se haya aplicado después del import; si los conceptos llegaron más tarde, crear los eventos mediante `POST /events`. Sigue pendiente verificar el flujo con pacientes sintéticos, la política temporal de gestación y el calendario/cobertura epidemiológica.
