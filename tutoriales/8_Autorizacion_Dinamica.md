# Guía práctica: autorización dinámica con roles y permisos

## 1. Propósito

En la guía anterior implementamos la autenticación:

~~~text
email + contraseña
        ↓
JWT
        ↓
Spring Security valida el JWT
        ↓
SecurityContextHolder
~~~

> Después de identificar al usuario, ¿tiene permiso para ejecutar la operación
> solicitada?

Implementaremos una autorización dinámica en la que:

1. El cliente envía el JWT.
2. Spring Security valida el JWT.
3. Se obtiene el ID del usuario desde el claim `sub`.
4. Se obtiene el método HTTP y se normaliza la URL solicitada.
5. Se consulta en MySQL el permiso actual del usuario por método y URL.
6. Si tiene permiso, la petición llega al Controller.
7. Si no tiene permiso, se responde `403 Forbidden`.

---

## 2. Autenticación y autorización

| Proceso | Pregunta | Resultado negativo |
| --- | --- | --- |
| Autenticación | ¿Quién es el usuario? | `401 Unauthorized` |
| Autorización | ¿Qué puede hacer? | `403 Forbidden` |

~~~mermaid
flowchart TD
    A["Petición con JWT"] --> B["JwtDecoder"]
    B --> C{"¿JWT válido?"}
    C -- No --> D["401 Unauthorized"]
    C -- Sí --> E["SecurityContextHolder"]
    E --> F["DynamicAuthorizationManager"]
    F --> G{"¿Tiene permiso?"}
    G -- No --> H["403 Forbidden"]
    G -- Sí --> I["Controller"]
~~~

---

## 3. Modelo utilizado

La autorización recorrerá estas relaciones:

~~~text
User
  ↓
UserRole
  ↓
Role
  ↓
RolePermission
  ↓
Permission
~~~

La entidad `Permission` ya contiene:

~~~java
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            length = 255
    )
    private String url;

    @Column(
            nullable = false,
            length = 10
    )
    private String method;

    @OneToMany(
            mappedBy = "permission",
            fetch = FetchType.LAZY
    )
    private List<RolePermission> rolePermissions =
            new ArrayList<>();
}
~~~

Cada permiso será identificado mediante:

~~~text
método HTTP + patrón de URL
~~~

Ejemplos:

| Método | URL |
| --- | --- |
| `GET` | `/api/users` |
| `POST` | `/api/users` |
| `GET` | `/api/users/{id}` |
| `PUT` | `/api/users/{id}` |
| `DELETE` | `/api/users/{id}` |

---

## 4. Evitar permisos duplicados

La misma combinación de URL y método no debe repetirse. Podemos agregar una
restricción única:

~~~java
@Entity
@Table(
        name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_permission_url_method",
                        columnNames = {
                                "url",
                                "method"
                        }
                )
        }
)
public class Permission {
    // Atributos actuales
}
~~~

Esto permite:

~~~text
GET    /api/users
POST   /api/users
DELETE /api/users/{id}
~~~

Pero impide registrar dos veces:

~~~text
GET /api/users
GET /api/users
~~~

El método debe almacenarse en mayúsculas:

~~~text
GET, POST, PUT, PATCH, DELETE
~~~

---

## 5. ¿Por qué guardar `{id}`?

Un mismo endpoint puede recibir diferentes tipos de identificador:

~~~text
/api/users/15
/api/users/507f1f77bcf86cd799439011
~~~

Los IDs numéricos y Mongo ObjectId representan:

~~~text
/api/users/{id}
~~~

La autorización no necesita conocer el valor específico de un ID numérico o
Mongo ObjectId. Solamente debe comprobar si el usuario puede consumir ese tipo
de endpoint. Esta guía no normaliza UUID.

| Patrón almacenado | Petición | ¿Coincide? |
| --- | --- | --- |
| `/api/users/{id}` | `/api/users/15` | Sí |
| `/api/users/{id}` | `/api/users/507f1f77bcf86cd799439011` | Sí |
| `/api/users/{id}` | `/api/users` | No |
| `/api/users/{id}` | `/api/users/15/profile` | No |
| `/api/users/{id}/profile` | `/api/users/15/profile` | Sí |

La validación del formato del ID corresponde al Controller o al Service, no al
sistema de permisos.

---

## 6. Componentes que modificaremos y componente nuevo

`PermissionRepository` y `PermissionService` ya existen en el proyecto y se
encargan del CRUD de permisos. No los crearemos otra vez: añadiremos solamente
los métodos necesarios para consultar los permisos de un usuario.

El único componente nuevo será `DynamicAuthorizationManager`.

| Componente | Estado | Cambio o responsabilidad |
| --- | --- | --- |
| `PermissionRepository` | Ya existe | Agregar la consulta por usuario, método HTTP y URL normalizada. |
| `PermissionService` | Ya existe | Normalizar IDs numéricos y Mongo ObjectId antes de consultar. |
| `DynamicAuthorizationManager` | Nuevo | Tomar la decisión final de acceso para cada petición. |

Estructura:

~~~text
src/main/java/com/uc/ms_security
│
├── authorization
│   └── DynamicAuthorizationManager.java
│
├── repository
│   └── PermissionRepository.java
│
├── service
│   └── PermissionService.java
│
└── config
    └── SecurityConfig.java
~~~

---

## 7. Consultar los permisos del usuario

Actualizamos el `PermissionRepository` existente. Conservamos sus métodos de
CRUD y validación de duplicados; agregamos `findByUserIdAndMethodAndUrl(...)`.
La URL se normaliza antes de consultar: `/api/users/15` y un Mongo ObjectId
como `/api/users/507f1f77bcf86cd799439011` se convierten en `/api/users/{id}`.

~~~java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository
        extends JpaRepository<Permission, Long> {

    boolean existsByUrlAndMethod(
            String url,
            String method
    );

    boolean existsByUrlAndMethodAndIdNot(
            String url,
            String method,
            Long id
    );

    @Query("""
            SELECT DISTINCT p
            FROM Permission p
            JOIN p.rolePermissions rp
            JOIN rp.role r
            JOIN r.userRoles ur
            WHERE ur.user.id = :userId
              AND UPPER(p.method) = UPPER(:method)
              AND p.url = :url
            """)
    List<Permission> findByUserIdAndMethodAndUrl(
            @Param("userId") Long userId,
            @Param("method") String method,
            @Param("url") String url
    );
}
~~~

La consulta significa:

~~~text
buscar permisos
        ↓
asociados con roles
        ↓
asignados al usuario
        ↓
filtrar por método HTTP
        ↓
filtrar por URL normalizada
~~~

> La consulta supone que los atributos se llaman
> `RolePermission.role`, `Role.userRoles` y `UserRole.user`. Si las
> entidades usan otros nombres, se deben ajustar esos tres recorridos en JPQL.

Como el servicio reemplaza los IDs por `{id}`, la consulta compara la URL
normalizada con el patrón almacenado de forma exacta.

---

## 8. Actualizar `PermissionService`

`PermissionService` ya existe para administrar el CRUD. Lo actualizaremos con
`hasPermission(...)`, sin eliminar sus métodos actuales (`create`, `findAll`,
`findById`, `update` y `delete`).

El nuevo método responderá:

> ¿El usuario tiene un permiso para el método y la URL normalizada de la petición?

Actualizamos `service/PermissionService.java`:

~~~java
package com.uc.ms_security.service;

import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

        private final PermissionRepository permissionRepository;
        // Se conservan las dependencias y métodos CRUD ya existentes.

    public boolean hasPermission(
            Long userId,
            String httpMethod,
            String requestUrl) {

        String normalizedUrl = normalizeUrl(requestUrl);

        return permissionRepository
                .findByUserIdAndMethodAndUrl(
                        userId,
                        httpMethod.toUpperCase(),
                        normalizedUrl
                )
                .stream()
                .findAny()
                .isPresent();
    }

    private String normalizeUrl(String requestUrl) {
        return requestUrl.replaceAll(
                "(?i)(?<=/)(?:\\d+|[a-f0-9]{24})(?=/|$)",
                "{id}"
        );
    }
}
~~~

La expresión regular normaliza únicamente segmentos completos que sean un ID
numérico o un Mongo ObjectId de 24 caracteres hexadecimales:

~~~text
/api/users/15
    ↓
/api/users/{id}

/api/users/507f1f77bcf86cd799439011
    ↓
/api/users/{id}
~~~

Ejemplo:

~~~text
/api/sessions/{id}

Consulta:
userId + DELETE + /api/users/{id}

Resultado:
existe el permiso y se permite la petición
~~~

---

## 9. Crear `DynamicAuthorizationManager`

Este componente se ejecutará después de validar el JWT y antes de invocar el
Controller.

Creamos `authorization/DynamicAuthorizationManager.java`:

~~~java
package com.uc.ms_security.authorization;

import com.uc.ms_security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class DynamicAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final PermissionService permissionService;

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {

        Authentication authentication =
                authenticationSupplier.get();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return new AuthorizationDecision(false);
        }

        Long userId;

        try {
            userId = Long.valueOf(
                    authentication.getName()
            );
        } catch (NumberFormatException exception) {
            return new AuthorizationDecision(false);
        }

        String method =
                context.getRequest().getMethod();

        String url =
                context.getRequest().getRequestURI();

        boolean permitted =
                permissionService.hasPermission(
                        userId,
                        method,
                        url
                );

        return new AuthorizationDecision(permitted);
    }
}
~~~

### ¿De dónde sale el ID?

En la guía de autenticación generamos el JWT así:

~~~java
.subject(user.getId().toString())
~~~

Después de validar el JWT:

~~~java
authentication.getName()
~~~

obtiene normalmente el claim `sub`. Por ejemplo:

~~~json
{
  "sub": "15",
  "email": "ana@mail.com"
}
~~~

Entonces:

~~~text
authentication.getName() → "15"
Long.valueOf(...)         → 15
~~~

---

## 10. Conectar la autorización en `SecurityConfig`

En la guía anterior teníamos:

~~~java
.anyRequest().authenticated()
~~~

Esa regla solamente preguntaba si el JWT era válido. Ahora la reemplazamos por:

~~~java
.anyRequest()
.access(dynamicAuthorizationManager)
~~~

Configuración completa:

~~~java
package com.uc.ms_security.config;

import com.uc.ms_security.authorization.DynamicAuthorizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final DynamicAuthorizationManager
            dynamicAuthorizationManager;

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

                        .anyRequest()
                        .access(dynamicAuthorizationManager)
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

El orden de las reglas es importante:

~~~text
1. /api/auth/login → permitAll
2. cualquier otra ruta → DynamicAuthorizationManager
~~~

La primera regla que coincide es la que se aplica.

---

## 11. Proceso completo

~~~mermaid
flowchart TD
    A["Authorization: Bearer JWT"] --> B["SecurityFilterChain"]
    B --> C["JwtDecoder"]
    C --> D{"¿JWT válido?"}
    D -- No --> E["401 Unauthorized"]
    D -- Sí --> F["SecurityContextHolder"]
    F --> G["DynamicAuthorizationManager"]
    G --> H["userId desde sub"]
    G --> I["method y URL de la petición"]
    H --> J["PermissionService"]
    I --> J
    J --> K["PermissionRepository"]
    K --> L["User → Role → Permission"]
    L --> M{"¿Coincide method + URL?"}
    M -- No --> N["403 Forbidden"]
    M -- Sí --> O["Controller"]
~~~

---

## 12. Datos de ejemplo

### Permisos

| ID | Método | URL |
| ---: | --- | --- |
| 1 | `GET` | `/api/users` |
| 2 | `GET` | `/api/users/{id}` |
| 3 | `POST` | `/api/users` |
| 4 | `PUT` | `/api/users/{id}` |
| 5 | `DELETE` | `/api/users/{id}` |

### Roles

~~~text
ADMIN
    ├── GET /api/users
    ├── GET /api/users/{id}
    ├── POST /api/users
    ├── PUT /api/users/{id}
    └── DELETE /api/users/{id}

CONSULTOR
    ├── GET /api/users
    └── GET /api/users/{id}
~~~

### Usuarios

~~~text
Ana → ADMIN
Luis → CONSULTOR
~~~

Resultados:

| Usuario | Petición | Resultado |
| --- | --- | --- |
| Ana | `DELETE /api/users/8` | `200/204` |
| Luis | `GET /api/users/8` | `200` |
| Luis | `DELETE /api/users/8` | `403` |

---

## 13. Pruebas mínimas

| Prueba | Resultado esperado |
| --- | --- |
| Petición sin JWT | `401 Unauthorized` |
| JWT inválido | `401 Unauthorized` |
| JWT válido sin permiso | `403 Forbidden` |
| JWT válido con permiso exacto | Acceso permitido |
| JWT válido con ID numérico normalizado | Acceso permitido |
| JWT válido con Mongo ObjectId normalizado | Acceso permitido |
| Mismo URL con método no permitido | `403 Forbidden` |
| Permiso retirado en MySQL | Siguiente petición rechazada |

---

## 14. Permiso funcional y propiedad del recurso

El permiso:

~~~text
GET /api/users/{id}
~~~

indica que el usuario puede consumir ese tipo de endpoint. No necesariamente
significa que pueda consultar cualquier usuario.

Ejemplo:

~~~text
Usuario autenticado: 15

GET /api/users/15 → puede consultar su propia información
GET /api/users/20 → puede requerir un permiso administrativo
~~~

Son dos preguntas diferentes:

| Nivel | Pregunta | Responsable |
| --- | --- | --- |
| Permiso funcional | ¿Puede ejecutar `GET /api/users/{id}`? | `DynamicAuthorizationManager` |
| Regla sobre el recurso | ¿Puede consultar específicamente el usuario 20? | Service o seguridad de métodos |

La propiedad del recurso puede implementarse después como una regla de negocio
o mediante `@PreAuthorize`.

---

## 15. ¿Por qué no guardar permisos en el JWT?

Podríamos incluir:

~~~json
{
  "sub": "15",
  "permissions": [
    "GET:/api/users",
    "POST:/api/users"
  ]
}
~~~

Pero si retiramos un permiso, el JWT anterior conservaría el acceso hasta
expirar.

Con autorización dinámica:

~~~text
cambiar permiso en MySQL
        ↓
siguiente petición
        ↓
consultar permisos actuales
        ↓
aplicar el cambio
~~~

| Estrategia | Actualización | Rendimiento |
| --- | --- | --- |
| Permisos dentro del JWT | Al expirar el token | Alto |
| Consulta MySQL por petición | Inmediata | Menor |
| Caché corta | Después de algunos segundos | Intermedio |

Para esta guía consultaremos MySQL. La caché será una optimización posterior.

---

## 16. Errores frecuentes

### Confundir `401` con `403`

~~~text
401 → identidad ausente o inválida
403 → identidad válida, pero sin permiso
~~~

### Guardar una URL real en lugar de un patrón

Incorrecto:

~~~text
/api/users/15
~~~

Correcto:

~~~text
/api/users/{id}
~~~

### Ignorar el método HTTP

`GET /api/users` y `DELETE /api/users` representan permisos diferentes.

### Validar el tipo del ID en autorización

La autorización reemplaza IDs numéricos y Mongo ObjectId de 24 caracteres por
`{id}`. El Controller o Service valida si el ID corresponde al recurso.

### Proteger también el login

`/api/auth/login` debe aparecer antes de `.anyRequest()` y utilizar
`permitAll()`.

### Permitir acceso cuando la URL no se puede normalizar

Si la ruta no coincide exactamente con un permiso después de normalizarla, la
decisión segura es rechazar el acceso.

---

## 17. ¿Quién hace cada trabajo?

| Pregunta | Responsable |
| --- | --- |
| ¿Quién valida el JWT? | Resource Server con `JwtDecoder` |
| ¿Dónde queda la identidad? | `SecurityContextHolder` |
| ¿Quién obtiene el ID desde `sub`? | `DynamicAuthorizationManager` |
| ¿Quién obtiene método y URL? | `DynamicAuthorizationManager` |
| ¿Quién consulta los permisos? | `PermissionRepository` |
| ¿Quién compara patrones? | `PermissionService` |
| ¿Quién decide permitir o negar? | `DynamicAuthorizationManager` |
| ¿Quién devuelve `403`? | Spring Security |

---

## 18. Retos

1. Crear permisos para usuarios, roles y sesiones.
2. Comprobar que `GET` y `DELETE` se autoricen de forma independiente.
3. Probar IDs numéricos y Mongo ObjectId.
4. Implementar una caché corta por usuario.
5. Invalidar la caché cuando cambien los roles o permisos.
6. Agregar auditoría de accesos denegados.
7. Implementar reglas de propiedad sobre recursos concretos.

---

## 19. Resumen

~~~text
JWT
    ↓
JwtDecoder
    ↓
SecurityContextHolder
    ↓
DynamicAuthorizationManager
    ├── userId desde sub
    ├── método HTTP
    └── URL solicitada
            ↓
PermissionService
            ↓
PermissionRepository
            ↓
User → Role → Permission
            ↓
¿method + url coinciden?
    ├── Sí → Controller
    └── No → 403 Forbidden
~~~

El JWT identifica al usuario. Los permisos permanecen en MySQL para que sus
cambios se reflejen dinámicamente sin esperar a que expire el token.

---

## 20. Referencias oficiales

* [Arquitectura de autorización de Spring Security](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html)
* [Autorización de peticiones HTTP](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
