# Fallo de instalación: FK del diagnóstico

## Evidencia y causa

Log aportado por Cristian, 2026-09-30, OpenMRS 2.8.9 / Liquibase 4.32.0:

- 19:14:52: changeset 13 completado; las seis tablas históricas fueron eliminadas.
- 19:14:53: changeset 14 creó las siete tablas nuevas y falló en
  `sc_diagnosis_fk`, con error 1005 / errno 150.
- SQL rechazado: `FOREIGN KEY (diagnosis_id) REFERENCES diagnosis (diagnosis_id)`.
- Los reintentos fallaron con `notifiable_event already exists`.

Las anotaciones de `org.openmrs.Diagnosis` en los JAR locales de OpenMRS 2.4.2 y
2.8.9 señalan `@Table(name="encounter_diagnosis")` y columna PK `diagnosis_id`.
El changelog confundió el nombre de la clase Java con el nombre físico. La prueba
H2 replicaba ese error al crear una tabla sintética `diagnosis`.

## Corrección autorizada (solo código)

El changeset 14 fallido referencia ahora `encounter_diagnosis(diagnosis_id)`.
No se modifica el changeset 13, los mappings Hibernate ni la columna local
`surveillance_case.diagnosis_id`. No se cambia la plataforma objetivo 2.4.2.

La prueba usa `encounter_diagnosis`, contrasta la anotación nativa, inspecciona el
destino real de la FK y comprueba rechazo de diagnóstico inexistente y duplicado.
Antes del arreglo, la prueba corregida falló al aplicar el changeset 14 con
`Table DIAGNOSIS not found`, reproduciendo el defecto de referencia.

Comando de regresión en PowerShell:

```powershell
mvn.cmd --batch-mode -pl api -am test '-Dtest=LiquibaseMigrationTest' '-Dsurefire.failIfNoSpecifiedTests=false'
```

## Riesgo y pendiente

Verificación posterior: `mvn.cmd --batch-mode clean verify` terminó con
`BUILD SUCCESS`: 84 pruebas, cero fallos y cero errores. La regresión H2 pasó
después de cambiar la FK. Se regeneró
`omod/target/sihsalusepidemiologicalsurveillance-1.0.0-SNAPSHOT.omod`.

## Recuperación posterior autorizada

Cristian confirmó el estado parcial, las siete tablas vacías y el respaldo, y
autorizó preparar la recuperación. Se agrega
`13b-recover-empty-partial-surveillance-schema` **antes del 14**, sin modificar
el changeset 13. La clase `RecoverEmptySurveillanceSchema` comprueba el historial,
la presencia de las siete tablas, ausencia de filas y de FK externas o inesperadas
antes del primer `DROP`. Ante una discrepancia detiene la migración.

El alcance exacto es: `epidemiological_focus`, `outbreak_alert`,
`period_case_count`, `surveillance_case`, `outbreak_alert_rule`,
`individual_record` y `notifiable_event`, en ese orden. No elimina tablas clínicas,
no desactiva FK, no usa CASCADE ni altera checksums. En instalación limpia no borra
nada; si el historial registra el 14 completado tampoco elimina tablas.

**Operación:** conservar el respaldo y mantener el módulo detenido, sin escritores
concurrentes, durante la instalación del nuevo OMOD. Al iniciar, Liquibase intentará
la recuperación y luego el 14 corregido. Si falla, detenerse y conservar el log
completo; no borrar historial ni marcar cambios como ejecutados.

La limpieza es de una sola ejecución y no ofrece rollback de las tablas eliminadas.
El DDL de MariaDB no garantiza atomicidad: un error durante los DROP podría dejar
una limpieza incompleta. Si el 14 vuelve a fallar después de un 13b exitoso, este
último no se repetirá automáticamente: requiere un nuevo diagnóstico.

Las pruebas locales H2 cubren las guardas y la secuencia completa de recuperación
seguida de creación del esquema. `mvn.cmd --batch-mode clean verify` terminó con
`BUILD SUCCESS`: 91 pruebas, cero fallos, errores u omitidas (86 API y 5 OMOD).
No se han ejecutado acciones en el servidor.
La validación integrada MariaDB/OpenMRS, incluida la carga de la clase de migración
por el módulo desplegado, continúa omitida por decisión del usuario.
