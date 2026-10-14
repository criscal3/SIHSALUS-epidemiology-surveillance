# Copias del paciente y gestación en el caso

`POST /cases` captura `patientGivenNames`, `patientPaternalSurname`,
`patientMaternalSurname`, `patientSex`, `patientAge` y `patientResidence` desde
OpenMRS. Son atributos de respuesta, no entradas del cliente. Se conservan en
`PUT /cases/{uuid}` y sus columnas Hibernate tienen `update="false"`.

Nombres reúne `givenName` y `middleName`; los apellidos proceden de `familyName`
y `familyName2`. Residencia copia la dirección preferida disponible: provincia,
distrito, centro poblado, `address1` y `address2`. Edad son años cumplidos entre
nacimiento y fecha de investigación; sin nacimiento queda sin valor.

`pregnant` es booleano. Solo admite verdadero si el sexo copiado es F y la edad
copiada está entre 10 y 55 inclusive. En los demás casos se persiste falso.
El formulario ofrece Sí/No y bloquea No cuando no se cumplen las condiciones.

Investigación se deriva del encuentro del diagnóstico en la zona del catálogo;
el servidor ignora el valor enviado por el cliente. Investigación y notificación
no pueden superar hoy, e investigación no puede superar la notificación cuando
se consigna. Se mantienen las validaciones del inicio de síntomas y defunción.

La migración `liquibase-case-snapshot.xml` agrega columnas sin rellenar registros
históricos con datos actuales. Esos casos conservan copias nulas incluso al
editarlos. Actualizar backend y frontend juntos. No se despliega automáticamente.

Pruebas sintéticas: `CaseSnapshotTest`, `NativePersistenceTest`, validaciones
de fechas/edad del formulario y bloqueo de controles. La prueba integrada en
DEV/QLTY queda pendiente de una sesión coordinada; las pruebas locales no la sustituyen.

Validación local (2026-10-09):

- PASSED: `mvn -pl api -am test -Dformatter.skip=true -Dimpsort.skip=true -Dlicense.skip=true`, 104 pruebas, sin omisiones.
- PASSED: `yarn workspace @sihsalus/esm-epidemiological-surveillance-app test`, 87 pruebas Vitest y una comprobación HMR.
- PASSED: scripts `lint` y `build` del paquete.
- PASSED: `node ../../../node_modules/typescript/bin/tsc6 --noEmit` desde el paquete. El script `typescript` no encuentra `tsc`; se usó la alternativa documentada.
- NOT RUN: smoke clínico DEV/QLTY, sin sesión coordinada.

La prueba Hibernate se integró en `NativePersistenceTest`. Una clase de contexto
separada inicializaba primero el cargador de PowerMock y fallaba por ausencia de
`GlobalConfiguration`; la suite final completa pasó usando la estructura existente.
