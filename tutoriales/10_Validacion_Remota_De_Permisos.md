# Guía práctica: validación remota de permisos entre microservicios

## 1. Objetivo

`ms_security` ya valida JWT y autoriza sus propias rutas mediante permisos
dinámicos. En una arquitectura de microservicios, otro servicio también puede
necesitar responder esta pregunta antes de ejecutar una operación:

> ¿El usuario identificado por este JWT puede invocar `POST /movies`?

En esta guía crearemos un endpoint interno que reutiliza la lógica existente
`PermissionService.hasPermission(...)`.

Ejemplo de flujo:

~~~text
Cliente
  |
  | Authorization: Bearer <JWT>
  v
Cinema API
  |
  | POST ms-security/api/internal/security/permissions-validation
  | Authorization: Bearer <mismo JWT>
  | { "url": "/cinema/movies", "method": "POST" }
  v
SecurityGuard
  |
  | { "allowed": true }
  v
Cinema API ejecuta o rechaza la operación
~~~

La URL consultada debe ser la ruta canónica registrada en la tabla
`permissions`. Si varios microservicios usan rutas iguales, incluya un prefijo
de servicio, por ejemplo `/cinema/movies` y `/billing/movies`.

---

## 2. Autenticación y decisión de permiso

El endpoint no es público aunque pueda ser consumido por varios
microservicios. Debe requerir el JWT del usuario.

| Situación | Responsable | Resultado |
| --- | --- | --- |
| No se envía JWT | Spring Security | `401 Unauthorized` |
| JWT inválido, expirado o con issuer incorrecto | `JwtDecoder` | `401 Unauthorized` |
| JWT válido, permiso existente | Controller + `PermissionService` | `200 { "allowed": true }` |
| JWT válido, permiso inexistente | Controller + `PermissionService` | `200 { "allowed": false }` |

El microservicio consumidor debe convertir `allowed: false` en `403 Forbidden`
para su endpoint de negocio. El endpoint de validación no devuelve `403` por
un permiso ausente, porque su única responsabilidad es informar la decisión.

---

## 3. Contrato HTTP

Usaremos una ruta interna explícita:

~~~http
POST /api/internal/security/permissions-validation
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "url": "/cinema/movies",
  "method": "POST"
}
~~~

Respuesta permitida:

~~~json
{
  "allowed": true
}
~~~

Respuesta denegada:

~~~json
{
  "allowed": false
}
~~~

No use una ruta como `/api/public/...`: ese nombre comunica que una petición
sin autenticación puede acceder y facilita una configuración incorrecta.

---

## 4. Crear los DTO

Cree `dto/PermissionValidationRequestDTO.java`:

~~~java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionValidationRequestDTO {

    @NotBlank(message = "La url es obligatoria")
    @Size(max = 255, message = "La url no puede superar 255 caracteres")
    private String url;

    @NotBlank(message = "El método es obligatorio")
    @Pattern(
            regexp = "GET|POST|PUT|PATCH|DELETE",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "El método HTTP no es válido"
    )
    private String method;
}
~~~

La validación del DTO evita enviar una URL o método vacíos. La autorización
continúa comparando la URL normalizada contra los permisos almacenados.

Cree `dto/PermissionValidationResponseDTO.java`:

~~~java
package com.uc.ms_security.dto;

public record PermissionValidationResponseDTO(boolean allowed) {
}
~~~

La respuesta no expone roles, permisos asignados ni datos del usuario: solo la
decisión necesaria para que el microservicio consumidor continúe o rechace la
solicitud.

---

## 5. Conservar `PermissionService.hasPermission`

No cree otra consulta para este endpoint. El método actual ya normaliza la URL,
convierte el método a mayúsculas y consulta los permisos del usuario:

~~~java
public boolean hasPermission(
        Long userId,
        String httpMethod,
        String requestUrl) {

    String normalizedUrl = normalizeUrl(requestUrl);

    return permissionRepository
            .findByUserIdAndMethodAndUrl(
                    userId,
                    httpMethod.toUpperCase(Locale.ROOT),
                    normalizedUrl
            )
            .stream()
            .findAny()
            .isPresent();
}
~~~

Este mismo método seguirá siendo usado por
`DynamicAuthorizationManager` para las rutas propias de `ms_security` y por el
nuevo controller para las rutas de otros microservicios.

Por ejemplo, una consulta de `DELETE /cinema/movies/15` se normaliza a
`DELETE /cinema/movies/{id}` antes de consultarse en la base de datos.

---

## 6. Crear el controller interno

Cree `controller/PermissionValidationController.java`:

~~~java
package com.uc.ms_security.controller;

import com.uc.ms_security.dto.PermissionValidationRequestDTO;
import com.uc.ms_security.dto.PermissionValidationResponseDTO;
import com.uc.ms_security.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/security")
@RequiredArgsConstructor
public class PermissionValidationController {

    private final PermissionService permissionService;

    @PostMapping("/permissions-validation")
    public PermissionValidationResponseDTO validate(
            @Valid @RequestBody PermissionValidationRequestDTO dto,
            JwtAuthenticationToken authentication) {

        Long userId = Long.valueOf(authentication.getToken().getSubject());

        boolean allowed = permissionService.hasPermission(
                userId,
                dto.getMethod(),
                dto.getUrl()
        );

        return new PermissionValidationResponseDTO(allowed);
    }
}
~~~

El `JwtAuthenticationToken` solo estará disponible si Spring Security validó
el token. El claim `sub` contiene el ID del usuario porque `JwtService` genera
el token con `subject(user.getId().toString())`.

No reciba `userId` en el cuerpo de la petición. Permitir que el consumidor lo
envíe permitiría consultar permisos de otro usuario.

---

## 7. Proteger la nueva ruta en `SecurityConfig`

Actualmente, todas las rutas distintas de `/api/auth/login` pasan por
`DynamicAuthorizationManager`. Eso no funciona para este endpoint: dicho
manager intentaría validar el permiso para invocar
`POST /api/internal/security/permissions-validation`, cuando realmente se
quiere evaluar la URL recibida en el cuerpo.

Agregue una regla autenticada antes de `anyRequest()` en
`config/SecurityConfig.java`:

~~~java
import org.springframework.http.HttpMethod;
~~~

Luego ajuste el bloque de autorización:

~~~java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/login")
        .permitAll()
        .requestMatchers(
                HttpMethod.POST,
                "/api/internal/security/permissions-validation"
        )
        .authenticated()
        .anyRequest()
        .access(dynamicAuthorizationManager)
)
~~~

El orden es obligatorio:

1. El login no requiere token.
2. El validador remoto requiere un JWT válido, pero no pasa por la autorización dinámica de su propia URL.
3. Las demás rutas conservan la autorización dinámica existente.

La configuración de Resource Server no cambia:

~~~java
.oauth2ResourceServer(resourceServer -> resourceServer
        .jwt(Customizer.withDefaults())
)
~~~

Por ello, el JWT se valida antes de que se ejecute el controller.

---

## 8. Flujo completo

~~~mermaid
flowchart TD
    A["Cinema API recibe POST /cinema/movies"] --> B["Reenvía el JWT a SecurityGuard"]
    B --> C["JwtDecoder valida firma, issuer y expiración"]
    C -->|JWT inválido| D["401"]
    C -->|JWT válido| E["PermissionValidationController"]
    E --> F["userId desde claim sub"]
    E --> G["url y method del cuerpo"]
    F --> H["PermissionService.hasPermission"]
    G --> H
    H --> I["Normaliza URL y consulta permisos actuales"]
    I --> J{"¿Permitido?"}
    J -->|Sí| K["200 allowed: true"]
    J -->|No| L["200 allowed: false"]
    K --> M["Cinema API ejecuta la operación"]
    L --> N["Cinema API responde 403"]
~~~

---

## 9. Ejemplo desde Cinema API

El microservicio que posee `/cinema/movies` conserva la responsabilidad final
de proteger su operación. Antes de crear una película, consulta SecurityGuard
con el mismo JWT recibido del cliente.

Pseudocódigo:

~~~java
PermissionValidationResponse response = securityGuardClient.validate(
        authorizationHeader,
        new PermissionValidationRequest("/cinema/movies", "POST")
);

if (!response.allowed()) {
    throw new AccessDeniedException("No tiene permiso para crear películas");
}

movieService.create(request);
~~~

Si SecurityGuard presenta timeout, error de red o respuesta no válida, el
microservicio debe fallar cerrado: no ejecuta la operación y responde un error
de infraestructura, normalmente `503 Service Unavailable`. Nunca debe asumir
que el permiso existe cuando no puede comprobarlo.

---

## 10. Pruebas manuales

Primero obtenga un token con el endpoint de login. Luego pruebe la validación:

~~~bash
curl -X POST http://localhost:8080/api/internal/security/permissions-validation \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"url":"/cinema/movies","method":"POST"}'
~~~

Pruebas esperadas:

| Caso | Resultado esperado |
| --- | --- |
| No enviar `Authorization` | `401 Unauthorized` |
| Enviar JWT inválido o expirado | `401 Unauthorized` |
| Enviar URL o método vacío | `400 Bad Request` |
| JWT válido con permiso | `200 { "allowed": true }` |
| JWT válido sin permiso | `200 { "allowed": false }` |
| Método en minúsculas, por ejemplo `post` | Se evalúa como `POST` |
| URL con ID, por ejemplo `/cinema/movies/15` | Se compara como `/cinema/movies/{id}` |

---

## 11. Seguridad entre servicios

El JWT autentica al usuario, no al microservicio que llama a SecurityGuard.
En producción, limite quién puede alcanzar esta ruta mediante controles de
infraestructura adicionales:

1. Mantenga SecurityGuard en una red privada o detrás de un API gateway.
2. Use mTLS o credenciales de cliente para identificar el microservicio consumidor.
3. Establezca timeouts cortos para la llamada remota.
4. Registre decisiones denegadas y errores de comunicación sin guardar el JWT.
5. Aplique el principio de fallo cerrado cuando SecurityGuard no esté disponible.

---

## 12. Resumen

La solución no duplica la lógica de permisos ni convierte el endpoint en
público:

~~~text
JWT del usuario
    ↓
Spring Security valida el JWT
    ↓
PermissionValidationController obtiene sub
    ↓
PermissionService.hasPermission(userId, method, url)
    ↓
{ "allowed": true | false }
~~~

`PermissionService.hasPermission(...)` se conserva sin cambios y centraliza la
normalización de URL y la consulta de permisos actuales en la base de datos.
