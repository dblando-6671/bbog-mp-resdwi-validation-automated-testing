# language: es
Característica: Validación del archivo plano RESDWI.TXT contra ODS_IDIA_ATTXN.ctl
  Requerimiento CVAPD00685673 | Jira SP-2986 | Componente CICS TS (Subfuente 337)

  @regresion @resdwi
  Escenario: Archivo RESDWI correcto cumple con el CTL
    Dado que cargo el archivo de prueba "RESDWI_OK.TXT"
    Cuando ejecuto la auditoría con 80 transacciones inyectadas
    Entonces el resultado general debe ser "CUMPLE"
    Y la validación "Registros de detalle" debe ser "CUMPLE"
    Y la validación "Longitud de registro" debe ser "CUMPLE"
    Y la validación "Relleno con bytes nulos" debe ser "CUMPLE"
    Y la validación "Trailer - DET_VALOR_ORIGEN" debe ser "CUMPLE"
    Y no debe haber errores a nivel de campo
    Y se genera el informe Excel

  @regresion @resdwi
  Escenario: Cantidad de detalles distinta a las transacciones inyectadas
    Dado que cargo el archivo de prueba "RESDWI_OK.TXT"
    Cuando ejecuto la auditoría con 81 transacciones inyectadas
    Entonces la validación "Registros de detalle" debe ser "NO CUMPLE"
    Y el resultado general debe ser "NO CUMPLE"

  @regresion @resdwi
  Escenario: Archivo RESDWI con errores de estructura y de campos
    Dado que cargo el archivo de prueba "RESDWI_ERRORES.TXT"
    Cuando ejecuto la auditoría con 6 transacciones inyectadas
    Entonces el resultado general debe ser "NO CUMPLE"
    Y la validación "Longitud de registro" debe ser "NO CUMPLE"
    Y la validación "Bytes sobrantes" debe ser "NO CUMPLE"
    Y la validación "Subfuente transaccional" debe ser "NO CUMPLE"
    Y la validación "Relleno con bytes nulos" debe ser "NO CUMPLE"
    Y la validación "Secuencia unica" debe ser "NO CUMPLE"
    Y la validación "Trailer - DET_VALOR_ORIGEN" debe ser "NO CUMPLE"
    Y el campo "ATTXN_SUB_FUENTE" debe reportar error
    Y el campo "ATTXN_VAL_TRANSAC" debe reportar error
    Y el campo "ATTXN_FEC_CONTAB" debe reportar error
    Y el campo "HUECO_500_561" debe reportar error
    Y se genera el informe Excel

  @regresion @resdwi
  Escenario: Archivo RESDWI rellenado con bytes nulos (LOW-VALUES)
    Dado que cargo el archivo de prueba "RESDWI_NULOS.TXT"
    Cuando ejecuto la auditoría con 80 transacciones inyectadas
    Entonces la validación "Relleno con bytes nulos" debe ser "NO CUMPLE"
    Y la validación "Bytes sobrantes" debe ser "NO CUMPLE"
    Y la validación "Trailer - relleno" debe ser "NO CUMPLE"
    Y el resultado general debe ser "NO CUMPLE"

  @regresion @resdwi
  Escenario: Archivo RESDWI vacío
    Dado que cargo el archivo de prueba "RESDWI_VACIO.TXT"
    Cuando ejecuto la auditoría sin informar transacciones inyectadas
    Entonces la validación "Generacion del archivo" debe ser "NO CUMPLE"
    Y el resultado general debe ser "NO CUMPLE"

  @regresion @resdwi @certificacion_10nov
  Escenario: Archivo RESDWI lote 10 de noviembre con hallazgos de Host y transacciones validas
    Dado que cargo el archivo de prueba "RESDWI.TXT"
    Cuando ejecuto la auditoría con 7 transacciones inyectadas
    Entonces la validación "Subfuente transaccional" debe ser "CUMPLE"
    Y la validación "Trailer - DET_VALOR_ORIGEN" debe ser "CUMPLE"
    Y la validación "Relleno con bytes nulos" debe ser "NO CUMPLE"
    Y la validación "Longitud de registro" debe ser "NO CUMPLE"
    Y el resultado general debe ser " NO CUMPLE"
    Y se genera el informe Excel