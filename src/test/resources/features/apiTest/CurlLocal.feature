# language: es
Característica: Envío de peticiones curl desde la automatización
  Pruebas sin ambiente: los curl se envían a un servidor local para comprobar
  que se interpretan bien (método, encabezados, cuerpo, variables y firma AWS).

  Antecedentes:
    Dado que tengo un servidor de prueba local

  @regresion @curl-local
  Escenario: Enviar un curl con variables del .env y variables automáticas
    Cuando envío la petición curl "ejemplos/eco_local.curl"
    Entonces el código de respuesta debe ser 200
    Y la respuesta debe contener "\"estado\":\"OK\""
    Y el servidor de prueba recibió el método "POST" y un cuerpo que contiene "\"secuencia\": \"1\""
    Y el servidor de prueba recibió el encabezado "x-api-key" con valor "clave-de-prueba-123456"
    Y el servidor de prueba recibió el encabezado "X-RqUID"

  @regresion @curl-local
  Escenario: Enviar el mismo curl varias veces con un RqUID distinto en cada una
    Cuando envío la petición curl "ejemplos/eco_local.curl" 5 veces
    Entonces todas las respuestas deben tener código 200
    Y el servidor de prueba recibió 5 peticiones
    Y cada petición recibida tiene un "X-RqUID" distinto

  @regresion @curl-local
  Escenario: Enviar un curl firmado con las credenciales AWS
    Dado que uso credenciales AWS de prueba
    Cuando envío la petición curl "ejemplos/sigv4_local.curl"
    Entonces el código de respuesta debe ser 200
    Y la petición recibida está firmada con AWS SigV4

  @regresion @curl-local
  Escenario: Detectar una respuesta de error del servicio
    Cuando envío la petición curl "ejemplos/error_local.curl"
    Entonces el código de respuesta debe ser 500
