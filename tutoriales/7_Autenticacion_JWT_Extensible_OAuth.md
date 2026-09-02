# Guía práctica: autenticación con Spring Security y JWT

## 1. Propósito

En las guías anteriores construimos endpoints para administrar usuarios. Ahora
necesitamos identificar quién intenta utilizar la API.

Implementaremos un inicio de sesión con email y contraseña. Cuando las
credenciales sean correctas, la aplicación entregará un JWT que deberá enviarse
para acceder a los endpoints protegidos.

El proceso será:

1. El usuario envía su email y contraseña.
2. `AuthService` busca al usuario y comprueba la contraseña con BCrypt.
3. Si son correctas, la aplicación genera un JWT.
4. El cliente envía ese JWT en las siguientes peticiones.
5. Spring Security valida el JWT antes de permitir el acceso.

> **Idea principal:** nosotros configuramos y conectamos las piezas; Spring
> Security realiza la autenticación y protege las peticiones.

---

## 2. Los dos momentos de la autenticación

~~~mermaid
flowchart TD
    subgraph LOGIN["Momento 1: iniciar sesión"]
        A["Email y contraseña"] --> B["AuthService"]
        B --> C{"¿Credenciales correctas?"}
        C -- No --> D["401 Unauthorized"]
        C -- Sí --> E["Generar JWT"]
    end

    subgraph ACCESS["Momento 2: utilizar la API"]
        F["Petición con JWT"] --> G["Spring Security valida JWT"]
        G --> H{"¿JWT válido?"}
        H -- No --> I["401 Unauthorized"]
        H -- Sí --> J["Controller"]
    end

    E --> F
~~~

En el primer momento se comprueba una contraseña. En el segundo ya no se vuelve
a enviar la contraseña: se presenta el JWT recibido.

---

## 3. Alcance

En esta guía implementaremos:

* Login con email y contraseña.
* Contraseñas protegidas con BCrypt.
* Generación de un JWT.
* Validación automática del JWT.
* Protección de endpoints.
* API sin sesión HTTP.


---

## 4. Componentes principales

| Componente | Explicación sencilla |
| --- | --- |
| `AuthController` | Recibe la petición de login. |
| `AuthService` | Coordina el caso de uso. |
| `UserRepository` | Busca el usuario en la base de datos. |
| `PasswordEncoder` | Compara la contraseña con el hash almacenado. |
| `JwtService` | Genera el JWT después de un login correcto. |
| `JwtDecoder` | Valida los JWT de las peticiones. |
| `SecurityFilterChain` | Define rutas públicas y protegidas. |

En esta implementación básica, `AuthService` ejecutará directamente los dos
pasos del login: buscar al usuario y comparar la contraseña. Spring Security
continuará encargado de BCrypt, la validación del JWT y la protección de rutas.

---

## 5. Dependencias

~~~xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
~~~

Resource Server no significa que implementaremos Google. Lo utilizaremos porque
Spring Security ya sabe leer y validar un JWT enviado como Bearer token.

---

## 6. Estructura propuesta

~~~text
src/main/java/com/uc/ms_security
│
├── controller
│   └── AuthController.java
│
├── dto
│   ├── LoginRequestDTO.java
│   └── AuthResponseDTO.java
│
├── service
│   ├── AuthService.java
│   └── JwtService.java
│
├── config
│   ├── JwtConfig.java
│   └── SecurityConfig.java
│
├── entity
│   └── User.java
└── repository
    └── UserRepository.java
~~~

---

## 7. Preparar las contraseñas

### 7.1 Buscar por email

Agregamos a `UserRepository`:

~~~java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(
            String email,
            Long id
    );

    Optional<User> findByEmail(String email);
}
~~~

---

## 8. Crear los DTO del login

### `LoginRequestDTO`

~~~java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
~~~

No aplicamos al login las reglas para crear una contraseña nueva. El usuario
debe poder ingresar con la contraseña que ya registró.

### `AuthResponseDTO`

~~~java
package com.uc.ms_security.dto;

import java.time.Instant;

public record AuthResponseDTO(
        String accessToken,
        String tokenType,
        Instant expiresAt) {
}
~~~

---

## 9. Configurar la clave del JWT

En `application.properties`:

~~~properties
security.jwt.secret=${JWT_SECRET:clave-local-educativa-de-32-bytes}
~~~

El valor después de `:` permite ejecutar la práctica localmente. En un entorno
real se proporciona `JWT_SECRET` mediante la configuración del entorno.

Creamos `config/JwtConfig.java`:

~~~java
package com.uc.ms_security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(
            @Value("${security.jwt.secret}") String secret) {

        byte[] secretBytes = secret.getBytes(
                StandardCharsets.UTF_8
        );

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "La clave JWT debe tener al menos 32 bytes"
            );
        }

        return new SecretKeySpec(
                secretBytes,
                "HmacSHA256"
        );
    }

    @Bean
    public JwtEncoder jwtEncoder(
            SecretKey jwtSecretKey) {

        OctetSequenceKey jwk =
                new OctetSequenceKey.Builder(
                        jwtSecretKey.getEncoded()
                ).build();

        JWKSource<SecurityContext> jwkSource =
                new ImmutableJWKSet<>(
                        new JWKSet(jwk)
                );

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        "ms-security"
                )
        );

        return decoder;
    }
}
~~~

No es necesario memorizar las clases Nimbus:

~~~text
JwtEncoder → firma y genera el JWT
JwtDecoder → valida el JWT recibido
~~~

---

## 10. Crear `JwtService`

`JwtService` define la información y duración del token, y solicita a
`JwtEncoder` que lo firme.

~~~java
package com.uc.ms_security.service;

import com.uc.ms_security.dto.AuthResponseDTO;
import com.uc.ms_security.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final Duration TOKEN_DURATION =
            Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;

    public AuthResponseDTO generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_DURATION);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ms-security")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        Jwt jwt = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        header,
                        claims
                )
        );

        return new AuthResponseDTO(
                jwt.getTokenValue(),
                "Bearer",
                expiresAt
        );
    }
}
~~~

Claims utilizados:

| Claim | Significado |
| --- | --- |
| `iss` | Emisor del JWT. |
| `sub` | ID estable del usuario. |
| `iat` | Momento de creación. |
| `exp` | Momento de expiración. |
| `jti` | Identificador único del token. |
| `email` | Dato informativo. |

Nunca debemos incluir la contraseña en el JWT.

---

## 11. Crear `AuthService`

`AuthService` coordina el login. Busca al usuario mediante `UserRepository`,
compara la contraseña mediante el `PasswordEncoder` de Spring Security y
solicita la generación del JWT.

~~~java
package com.uc.ms_security.service;

import com.uc.ms_security.dto.AuthResponseDTO;
import com.uc.ms_security.dto.LoginRequestDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponseDTO login(
            LoginRequestDTO dto) {

        User user = userRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(this::unauthorized);

        boolean validPassword =
                passwordEncoder.matches(
                        dto.getPassword(),
                        user.getPassword()
                );

        if (!validPassword) {
            throw unauthorized();
        }

        return jwtService.generateToken(user);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciales incorrectas"
        );
    }
}
~~~

El servicio devuelve el mismo mensaje si el email no existe o si la contraseña
es incorrecta. De esta forma no revela qué usuarios están registrados.

---

## 12. Crear `AuthController`

~~~java
package com.uc.ms_security.controller;

import com.uc.ms_security.dto.AuthResponseDTO;
import com.uc.ms_security.dto.LoginRequestDTO;
import com.uc.ms_security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponseDTO login(
            @Valid
            @RequestBody LoginRequestDTO dto) {

        return authService.login(dto);
    }
}
~~~

---

## 13. Configurar Spring Security

Creamos o actualizamos `config/SecurityConfig.java`:

~~~java
package com.uc.ms_security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(
                                Customizer.withDefaults()
                        )
                );

        return http.build();
    }
}
~~~

Resultado:

~~~text
POST /api/auth/login → público
cualquier otra ruta → requiere un JWT válido
~~~

### ¿Por qué `STATELESS`?

Spring no guardará al usuario en una sesión HTTP. Cada petición debe presentar
su JWT.

### ¿Por qué no creamos un filtro JWT?

Resource Server ya:

* Extrae el token del encabezado `Authorization`.
* Comprueba la firma y expiración.
* Valida el emisor.
* Crea un `Authentication`.
* Lo guarda en `SecurityContextHolder`.
* Responde `401` cuando el token no es válido.

---

## 14. Flujo interno completo

~~~mermaid
flowchart TD
    A["Email y contraseña"] --> B["AuthController"]
    B --> C["AuthService"]
    C --> G["UserRepository"]
    G --> H["PasswordEncoder"]
    H --> I{"¿Credenciales correctas?"}
    I -- No --> J["401"]
    I -- Sí --> K["JwtService"]
    K --> L["JWT"]
    L --> M["Petición Bearer"]
    M --> N["Resource Server"]
    N --> O["JwtDecoder"]
    O --> P{"¿JWT válido?"}
    P -- No --> Q["401"]
    P -- Sí --> R["SecurityContextHolder"]
    R --> S["Controller"]
~~~

---

## 15. Probar el login

Debe existir un usuario cuya contraseña haya sido codificada con BCrypt.

~~~http
POST /api/auth/login
Content-Type: application/json
~~~

~~~json
{
  "email": "ana@mail.com",
  "password": "ClaveSegura123!"
}
~~~

Respuesta:

~~~json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-09-01T15:15:00Z"
}
~~~

---

## 16. Probar credenciales incorrectas

Una contraseña incorrecta o un email inexistente deben producir:

~~~http
401 Unauthorized
~~~

El mensaje no debe revelar cuál credencial falló.

---

## 17. Probar un endpoint protegido

Sin JWT:

~~~http
GET /api/users
~~~

Resultado:

~~~http
401 Unauthorized
~~~

Con JWT:

~~~http
GET /api/users
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
~~~

Resultado:

~~~http
200 OK
~~~

---

## 18. Pruebas mínimas

| Prueba | Resultado esperado |
| --- | --- |
| Login correcto | `200` y JWT. |
| Email inexistente | `401`. |
| Contraseña incorrecta | `401`. |
| Endpoint sin JWT | `401`. |
| Endpoint con JWT válido | `200`. |
| JWT modificado | `401`. |
| JWT vencido | `401`. |

---

## 19. Errores frecuentes

* Comparar la contraseña en el Controller.
* Guardar la contraseña sin `passwordEncoder.encode(...)`.
* Codificar otra vez la contraseña durante el login.
* Guardar la contraseña dentro del JWT.
* Crear un filtro JWT cuando Resource Server ya lo proporciona.
* Informar si el email o la contraseña fue la credencial incorrecta.

---

## 20. ¿Quién hace cada trabajo?

| Pregunta | Respuesta |
| --- | --- |
| ¿Quién recibe el login? | `AuthController`. |
| ¿Quién coordina el caso? | `AuthService`. |
| ¿Quién busca al usuario? | `AuthService` mediante `UserRepository`. |
| ¿Quién compara la contraseña? | `AuthService` mediante `PasswordEncoder`. |
| ¿Quién genera el JWT? | `JwtService` con `JwtEncoder`. |
| ¿Quién valida el JWT? | Resource Server con `JwtDecoder`. |
| ¿Dónde queda el autenticado? | `SecurityContextHolder`. |
| ¿Quién protege las rutas? | `SecurityFilterChain`. |

---

## 21. Reto de comprensión

Explique con sus propias palabras:

1. ¿Por qué `AuthService` utiliza `PasswordEncoder.matches(...)` en vez de comparar dos textos?
2. ¿Cuál es la diferencia entre `JwtEncoder` y `JwtDecoder`?
3. ¿Por qué `/api/auth/login` debe ser público?
4. ¿Por qué cada petición debe enviar nuevamente el JWT?
5. ¿Qué diferencia hay entre una contraseña incorrecta y un JWT inválido?

---

## 22. Extensión futura: proveedores externos

Esta sección es conceptual. No debe implementarse todavía.

En el futuro, el usuario podría identificarse mediante Google, GitHub o
Microsoft. Aunque cambie la forma de iniciar sesión, la API puede seguir
entregando el mismo JWT interno:

~~~mermaid
flowchart TD
    A["Email y contraseña"] --> C["User interno"]
    B["Google, GitHub o Microsoft"] --> C
    C --> D["Generar JWT interno"]
    D --> E["Endpoints protegidos"]
~~~

La autenticación externa puede utilizar OAuth 2.0 u OpenID Connect, según el
proveedor. Será un incremento independiente.

Cuando exista esa necesidad podremos refactorizar:

~~~text
JwtService
    ↓
AccessTokenIssuer
    ↑
JwtAccessTokenIssuer
~~~

La abstracción se introducirá cuando exista más de una forma real de
autenticarse, no antes.

---

## 23. Resumen

~~~text
INICIO DE SESIÓN
────────────────────────
email + contraseña
        ↓
AuthService busca el User
        ↓
PasswordEncoder comprueba la contraseña
        ↓
JwtService genera el JWT


PETICIONES POSTERIORES
────────────────────────
Authorization: Bearer <jwt>
        ↓
Resource Server
        ↓
JwtDecoder valida el token
        ↓
SecurityContextHolder
        ↓
Controller
~~~

El login local utiliza el `PasswordEncoder` de Spring Security, mientras que
Resource Server se encarga de validar el JWT y construir el contexto de
seguridad. La aplicación define cómo buscar al usuario, qué contiene el token y
qué rutas deben protegerse.

---

## 24. Referencias oficiales

* [Arquitectura de autenticación](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html)
* [PasswordEncoder](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
* [Resource Server con JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
