# Verificación local de indicadores — 2026-09-30

Alcance: `/reports` sobre `period_case_count`, filtros de diagnóstico y geografía,
curva diaria y canal endémico. No se ejecutaron migraciones ni escrituras remotas.

## Cambios

- Backend: `AggregateReportCalculator`, servicio, DAO y controlador. Predeterminado
  `CONFIRMADO`; admite `PROBABLE` y `TODOS`. Un único nivel geográfico por consulta.
- El histórico suma por año antes de calcular cuartiles; no sobrescribe las otras
  zonas ni mezcla distritos y centros. Fechas inclusivas, períodos parciales y
  correspondencia de fechas entre años bisiestos cubiertos por pruebas.
- Frontend: filtros de diagnóstico, nivel y dirección jerárquica, títulos según
  selección, limpieza de direcciones dependientes y errores traducidos. RF-22 oculto.
- Contrato: `docs/api-contract.md`. Es necesario actualizar OMOD y frontend juntos.

## Validaciones ejecutadas

| Estado | Comando | Resultado / alcance |
|---|---|---|
| PASSED | `mvn.cmd --batch-mode clean verify` | 84 pruebas, 0 fallos, 0 errores. OMOD generado. Incluye prueba de consulta Hibernate tras limpiar sesión mediante `BaseModuleContextSensitiveTest` (H2, no MariaDB). |
| PASSED | `yarn.cmd workspace @sihsalus/esm-epidemiological-surveillance-app test` | 35 pruebas Vitest y 2 comprobaciones HMR. Aviso React `act` en el selector de infección del formulario de casos. |
| PASSED | `yarn.cmd workspace @sihsalus/esm-epidemiological-surveillance-app typescript` | Comprobación TypeScript sin errores. |
| PASSED | `yarn.cmd workspace @sihsalus/esm-epidemiological-surveillance-app build` | Rspack compila; dos advertencias de tamaño (entrada principal aproximadamente 256 KiB). |
| PASSED | `yarn.cmd prettier --check packages/apps/esm-epidemiological-surveillance-app/README.md` | Formato del README. |
| FAILED | `yarn.cmd workspace @sihsalus/esm-epidemiological-surveillance-app lint` | Dos errores de dependencias de efectos en `infection-address-selector.component.tsx`, no modificado en esta unidad. No se han reproducido sobre `origin/main`; no se afirma que sean preexistentes en esa rama. |
| FAILED | `yarn.cmd verify:changed --base origin/main --head HEAD` | Tras el commit frontend `297f0cc83`, detectó cambios globales de la rama respecto a `origin/main`, anunció `yarn verify` y terminó con código 1 sin resultados de esa verificación. No se atribuye éxito global al monorepo. |
| FAILED | `git diff --check origin/main...HEAD` (frontend) | Señala una línea vacía al final de `src/constants.ts`, no modificado en esta unidad. `git diff --cached --check` del cambio de indicadores pasó antes del commit. |
| NOT RUN | UI → OpenMRS/MariaDB desplegado | Pendiente de instancia coordinada con datos sintéticos y OMOD actualizado. Las pruebas locales no demuestran aceptación clínica. |

La primera ejecución de Maven detectó dos expectativas incorrectas en pruebas nuevas:
1–3 de enero de 2026 pertenecen al año epidemiológico 2025, y el mock de un `Integer`
devuelve cero por defecto, no null. Se corrigieron las expectativas y el fixture, sin
relajar las validaciones del servicio; la ejecución final completa fue exitosa.

## Límites pendientes

Los años sin filas históricas no se convierten en ceros. Debe verificarse la cobertura
de los agregados antes de interpretar el canal. La auditoría externa, la prueba de
migraciones sobre MariaDB y la retirada amplia del flujo heredado continúan pendientes;
no se incluyen en esta unidad. React Form Engine sigue aplazado por decisión del usuario.

Commits de implementación: backend `47b85a7`, frontend `297f0cc83`. Los archivos de
`target/` generados por Maven y los cambios ajenos existentes en el frontend se
conservaron sin incluirlos en los commits. El formateador Maven introduce tabulaciones
en líneas vacías Java; `git diff --check` del backend las reportó como espacios finales.
