# Evidencia de migraciones

## Validación disponible

El 30 de septiembre de 2026 se ejecutó `mvn --batch-mode clean verify -DskipITs`.
La prueba `LiquibaseMigrationTest` aplicó los changesets en una base H2 limpia, con
stubs de las tablas núcleo de OpenMRS, y verificó las claves foráneas nuevas y la
restricción de unicidad de `surveillance_case.diagnosis_id`.

## Pendiente: MariaDB 10.11

### Actualización de preflight — 2026-09-30

Docker ya está operativo: servidor Docker Engine 29.8.0 y un contenedor existente
`mariadb:10.11.7`. Ese contenedor y sus datos no se han utilizado para pruebas.
La imagen local SIH.SALUS contiene OpenMRS **2.8.9**, mientras el OMOD compila contra
**2.4.2**. Se requiere confirmar el entorno de integración; la evidencia detallada
y la decisión pendiente están en `NOTAS-PARA-CRISTIAN.md`.

Estado actual: inspección de imágenes completada; migraciones, rollback,
`SHOW CREATE TABLE` y flujo UI-servidor **no ejecutados**. No se afirma validación
MariaDB a partir de la disponibilidad del motor ni de las pruebas H2.

### Bloqueo registrado anteriormente (ya superado)

La validación requerida contra MariaDB 10.11, una base OpenMRS con datos y rollback no
se ejecutó porque Docker Desktop no tiene el daemon iniciado en este equipo:
`npipe:////./pipe/docker_engine` no existe. No se modificó ningún entorno compartido.

Cuando Docker esté disponible, se debe ejecutar los changesets 13 y 14 contra una base
limpia y una copia de OpenMRS, guardar los `SHOW CREATE TABLE individual_record` y
`SHOW CREATE TABLE surveillance_case`, y documentar la ejecución de rollback.
