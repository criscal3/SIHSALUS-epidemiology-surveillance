# Integración con el OMOD de auditoría clínica

Fecha: 2026-09-30.

## Resultado de la verificación

El repositorio del OMOD de auditoría clínica (`openmrs-module-sihsalus-audit`) no está disponible en este workspace ni figura como dependencia declarada del OMOD de vigilancia. Por ello no es posible verificar su API, su identificador de módulo, ni si intercepta cambios de entidades Hibernate propias como `surveillance_case`.

No se creó una tabla ni un servicio de auditoría alterno en este componente.

## Eventos que debe recibir el OMOD de auditoría

| Evento | Entidad | Datos del evento |
| --- | --- | --- |
| Consulta identificable | `surveillance_case` | UUID del caso consultado |
| Edición | `surveillance_case` | UUID, campo, valor anterior y valor nuevo |
| Preparación/descarga de registro individual | `individual_record` | UUID del registro individual y del caso |
| Exportación de consolidados | reporte | tipo, rango y filtros no identificables |
| Generación de reporte | reporte | tipo, rango y filtros no identificables |

El OMOD de auditoría debe obtener del contexto OpenMRS el usuario, instante y resultado de la acción. Los valores de edición se deben comparar antes/después de persistir la entidad; no se registra el payload completo ni se duplican datos clínicos.

## Punto de integración requerido

1. Confirmar en el OMOD de auditoría su servicio público o extensión soportada para publicar eventos de negocio.
2. Añadir ese OMOD como `require_module` con su identificador y versión reales.
3. Publicar los eventos anteriores desde el servicio de vigilancia después de la operación exitosa, dentro de la semántica transaccional que recomiende dicho OMOD.
4. Añadir una prueba de integración con el módulo instalado que verifique los valores anterior/nuevo de una edición de `surveillance_case`.

Si el OMOD no admite entidades propias ni diferencias por campo, la ampliación debe realizarse en ese OMOD, no en este componente.
