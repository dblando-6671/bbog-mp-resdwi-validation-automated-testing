# bbog-mp-resdwi-validation-automated-testing

Validación automatizada del archivo plano **RESDWI.TXT** contra el control SQL*Loader
`ODS_IDIA_ATTXN.ctl` (73 campos + hueco 500:561, registro fijo de 819 bytes, trailer `CNTRL`),
más el envío de los **curl** que inyectan las transacciones y la descarga del archivo desde **S3**.

Requerimiento CVAPD00685673 · Jira SP-2986 · Componente CICS TS (Subfuente 337).

Es la migración a Java/Cucumber del script `auditor_resdwi_v2.py`. Hace las mismas validaciones
y genera el mismo informe Excel de 3 hojas (Resumen Ejecutivo, Auditoría 73 Campos, Hallazgos por Campo).

## Primer uso

1. Copiar `.env.example` como `.env` en la raíz del proyecto y llenar los valores
   (credenciales AWS, bucket y ruta del RESDWI, URL y API key de los curl).
2. Pegar el curl real de inyección en `src/test/resources/curl/inyectar_transaccion.curl`
   cambiando secretos y valores únicos por variables (`${API_KEY}`, `${UUID}`, ...).
3. Correr la regresión (no necesita ambiente): `mvn clean test`

## Estructura

```
src/main/java/co/com/bdb/automation
├── client      Configuracion (.env), ArchivoClient, HttpCurlClient, S3ResdwiClient
├── model       CampoCtl, DefinicionResdwi (diseño del .ctl), ArchivoResdwi, LineaArchivo,
│               PeticionCurl, CredencialesAws
├── respuesta   EstadoQa, ValidacionCampo, FilaResumen, FilaDetalle, ResultadoAuditoria,
│               RespuestaHttp, ResultadoCurl
└── service     ResdwiParser, ValidadorCampoService, AuditorResdwiService, ReporteExcelService,
                CurlParser, CurlService, VariablesCurl, AwsSigV4Signer, Enmascarador, Texto

src/test/java/co/com/bdb/automation
├── definitions ResdwiDefinitions, CurlDefinitions, Hooks, Contexto
├── runner      RunnerResdwi (regresión) · RunnerAmbienteCurl · RunnerAmbienteS3 · RunnerAmbienteArchivoLocal
└── utilidades  ServidorEco (servidor local para probar los curl sin ambiente)

src/test/resources
├── curl/                 inyectar_transaccion.curl (plantilla) y ejemplos/
├── features/apiTest/     ResdwiValidation, CurlLocal, AmbienteResdwi
└── testData/resdwi/      RESDWI_OK, RESDWI_ERRORES, RESDWI_NULOS, RESDWI_VACIO (datos simulados)
```

## Ejecutar

| Qué | Comando |
|---|---|
| Regresión (archivos simulados + curl contra servidor local) | `mvn clean test` |
| Enviar el curl de inyección al ambiente (`CURL_REPETICIONES` veces) | `mvn test -Dtest=RunnerAmbienteCurl` |
| Descargar el RESDWI de S3 y auditarlo | `mvn test -Dtest=RunnerAmbienteS3` |
| Auditar un RESDWI que está en el equipo | `mvn test -Dtest=RunnerAmbienteArchivoLocal -Dresdwi.archivo="C:/ruta/RESDWI.TXT"` |

Los runners de ambiente no corren con un `mvn test` normal; solo cuando se piden con `-Dtest`.
Cualquier variable del `.env` se puede reemplazar al ejecutar: `-DCURL_REPETICIONES=10`, `-Dresdwi.esperadas=80`.
En IntelliJ: clic derecho en el runner → *Modify Run Configuration* → *VM options*.

Salidas:
- Informe Excel: `target/reportes-resdwi/Informe_Auditoria_ODS_<escenario>_<fecha>.xlsx`
- Peticiones y respuestas de los curl: `target/respuestas-curl/` (credenciales ocultas)
- Reporte Cucumber: `target/cucumber-reports/*.html`

## Archivo .env

Prioridad de los valores: `-DNOMBRE=valor` > `.env` > variables de entorno de Windows.
El `.env` va antes que Windows para que unas credenciales AWS viejas del sistema no reemplacen
las que se acaban de pegar. Ver todas las variables en `.env.example`.

Las credenciales AWS se usan para:
- descargar el RESDWI de S3 (`RESDWI_S3_BUCKET`, `RESDWI_S3_KEY`, `AWS_REGION`);
- firmar los curl que tengan `--aws-sigv4` (API Gateway con IAM).

Use credenciales temporales (las de acceso programático del portal SSO). Cuando vencen, S3 responde
`ExpiredToken` y basta con pegar unas nuevas en el `.env`.

## Curl

Los curl se envían con el cliente HTTP de Java: no hace falta tener curl instalado.
Se pueden pegar tal como salen de Postman (*Code → cURL*) o Chrome (*Copy as cURL (bash)*).

- Variables: `${NOMBRE}` o `{{NOMBRE}}` se toman del `.env`.
- Automáticas (una por envío): `${UUID}`, `${CONTADOR}` (1..N), `${FECHA}` (yyyyMMdd),
  `${HORA}` (HHmmss), `${FECHA_HORA}`, `${TIMESTAMP}`. Útiles para que cada envío tenga su propio RqUID.
- Cuerpo en archivo aparte: `--data @cuerpos/transaccion.json` (también admite variables).
- Opciones soportadas: `-X -H -d --data-raw --data-binary --json --url -u -L -m --aws-sigv4`.
  `-k` se ignora (el certificado sí se valida); `-F` (multipart) no está soportado.
- Proxy corporativo: `CURL_PROXY=host:puerto` en el `.env`.

Steps:
- `Cuando envío la petición curl "<archivo>"`
- `Cuando envío la petición curl "<archivo>" <n> veces`
- `Cuando envío la petición curl "<archivo>" las veces indicadas en el .env`
- `Entonces el código de respuesta debe ser <código>`
- `Entonces todas las respuestas deben tener código <código>`
- `Y la respuesta debe contener "<texto>"`

## Validaciones del RESDWI

| Validación | Qué revisa |
|---|---|
| Registros de detalle | Cantidad de detalles = transacciones inyectadas |
| Longitud de registro / Bytes sobrantes | Todos los registros miden 819 bytes |
| Subfuente transaccional (001:003) | Todos los detalles traen 337 |
| Relleno con bytes nulos (0x00) | Sin LOW-VALUES; informa posiciones |
| Secuencia única (004:023) | Sin secuencias repetidas |
| Trailer CNTRL | DET_FEC_ORIGEN, DET_NM_ARCHIVO y DET_VALOR_ORIGEN = cantidad de detalles |
| Campo a campo | Fechas (ORA-01861), numéricos (ORA-01722), obligatorios, nulos, hueco no mapeado |

Steps:
- `Dado que cargo el archivo de prueba "<archivo>"`
- `Dado que descargo de S3 el archivo RESDWI configurado en el .env`
- `Dado que descargo de S3 el archivo "<ruta en el bucket>"`
- `Dado que cargo el archivo RESDWI indicado en el .env o por parámetro`
- `Cuando ejecuto la auditoría con <n> transacciones inyectadas`
- `Cuando ejecuto la auditoría con las transacciones inyectadas indicadas en el .env o por parámetro`
- `Entonces el resultado general debe ser "CUMPLE|NO CUMPLE"`
- `Y la validación "<inicio del nombre>" debe ser "CUMPLE|NO CUMPLE|INFO"`
- `Y el campo "<COLUMNA_ODS>" debe reportar error`
- `Y no debe haber errores a nivel de campo`
- `Y se genera el informe Excel`

## Seguridad

No subir el `.env`, planos reales, el `.ctl` del ambiente, informes con datos de clientes ni llaves.
El `.gitignore` ya los excluye. Los reportes ocultan encabezados como `Authorization`, `x-api-key`
y tokens, pero los cuerpos de las respuestas se guardan tal cual.
