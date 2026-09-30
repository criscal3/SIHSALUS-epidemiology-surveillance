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

Este cambio no repara las tablas parcialmente creadas en la instancia afectada.
La recuperación debe revisar esquema, datos e historial con el administrador.
No se han borrado tablas, alterado checksums ni marcado la migración como ejecutada.
Un changeset posterior por sí solo no evita el fallo del 14 que lo precede.
No instalar como si fuera una recuperación automática. La validación integrada
MariaDB/OpenMRS continúa omitida por decisión del usuario.
