# language: es
Característica: Pruebas contra el ambiente (curl reales, S3 y archivos reales)
  Usan las variables del archivo .env. Se ejecutan solo con los runners RunnerAmbiente*.

  @ambiente @curl
  Escenario: Inyectar transacciones CICS TS por curl
    Cuando envío la petición curl "inyectar_transaccion.curl" las veces indicadas en el .env
    Entonces todas las respuestas deben tener código 200

  @ambiente @s3
  Escenario: Descargar de S3 el RESDWI.TXT generado y auditarlo
    Dado que descargo de S3 el archivo RESDWI configurado en el .env
    Cuando ejecuto la auditoría con las transacciones inyectadas indicadas en el .env o por parámetro
    Entonces se genera el informe Excel
    Y el resultado general debe ser "CUMPLE"

  @ambiente @resdwi-real
  Escenario: Auditar un RESDWI.TXT que tengo en mi equipo
    Dado que cargo el archivo RESDWI indicado en el .env o por parámetro
    Cuando ejecuto la auditoría con las transacciones inyectadas indicadas en el .env o por parámetro
    Entonces se genera el informe Excel
    Y el resultado general debe ser "CUMPLE"¨
