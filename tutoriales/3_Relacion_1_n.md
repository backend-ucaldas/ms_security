# Guía práctica: Relación 1:N entre `User` y `Session`

## 1. Objetivo

Continuaremos el mismo proyecto agregando la entidad:

```text
Session
──────────────────
id          : Long
token       : String
expiration  : Date
code2FA     : String
```

La relación será:

```text
User 1 ───────── N Session
```

Esto significa:

> Un usuario puede tener muchas sesiones.

Pero:

> Cada sesión pertenece solamente a un usuario.

Ejemplo:

```text
Juan
 │
 ├── Session 1
 │
 ├── Session 2
 │
 └── Session 3
```

---

# 2. Modelo relacional

En MySQL tendremos:

```text
users
────────────────
id
name
email
password
```

Y:

```text
sessions
────────────────
id
token
expiration
code_2fa
user_id    ← FK
```

La FK estará en:

```text
sessions.user_id
```

Por tanto:

```text
User
 PK id
   │
   │ 1
   │
   │ N
   ▼
Session
 FK user_id
```

---

# 3. ¿Quién es el dueño de la relación?

En este caso será:

```text
Session
```

porque contiene:

```text
user_id
```

Por tanto, en `Session` tendremos:

```java
@ManyToOne
@JoinColumn(name = "user_id")
```

Y en `User`:

```java
@OneToMany(mappedBy = "user")
```

Visualmente:

```text
User
 │
 │ @OneToMany
 │ mappedBy = "user"
 │
 │ 1
 │
 │ N
 ▼
Session
 │
 │ @ManyToOne
 │
 └── user_id
```

---

# 4. Estructura del proyecto

Agregaremos:

```text
src/main/java/com/example/users
│
├── controller
│   ├── UserController.java
│   ├── ProfileController.java
│   └── SessionController.java
│
├── service
│   ├── UserService.java
│   ├── ProfileService.java
│   └── SessionService.java
│
├── repository
│   ├── UserRepository.java
│   ├── ProfileRepository.java
│   └── SessionRepository.java
│
├── entity
│   ├── User.java
│   ├── Profile.java
│   └── Session.java
│
├── dto
│   ├── ...
│   ├── SessionRequestDTO.java
│   └── SessionResponseDTO.java
│
├── mapper
│   ├── UserMapper.java
│   ├── ProfileMapper.java
│   └── SessionMapper.java
│
└── UsersApplication.java
```

---

# 5. Tipo de dato para `expiration`

Aunque conceptualmente tenemos:

```text
expiration : Date
```

para una fecha de expiración es mejor utilizar:

```java
Instant
```

porque representa un instante exacto en el tiempo.

Importamos:

```java
java.time.Instant;
```

Por ejemplo:

```text
2026-08-19T23:30:00Z
```

Esto será especialmente útil cuando posteriormente implementemos JWT.

---

# 6. Crear Entity `Session`

Creamos:

```text
entity/Session.java
```

```java
package com.example.users.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
        nullable = false,
        unique = true,
        length = 500
    )
    private String token;

    @Column(
        nullable = false
    )
    private Instant expiration;

    @Column(
        name = "code_2fa",
        length = 20
    )
    private String code2FA;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    private User user;
}
```

La parte fundamental es:

```java
@ManyToOne
@JoinColumn(
    name = "user_id",
    nullable = false
)
private User user;
```

Esto crea conceptualmente:

```text
sessions

id
token
expiration
code_2fa
user_id
   │
   └── FK → users.id
```

---

# 7. ¿Por qué `@ManyToOne` está en Session?

Porque desde el punto de vista de `Session`:

```text
Muchas sesiones
       ↓
pertenecen a
       ↓
un usuario
```

Por eso:

```java
@ManyToOne
```

Mientras que desde `User`:

```text
Un usuario
    ↓
tiene
    ↓
muchas sesiones
```

Por eso:

```java
@OneToMany
```

Una relación:

```text
1:N
```

vista desde el otro lado es:

```text
N:1
```

---

# 8. Modificar Entity `User`

Agregamos una colección:

```java
@OneToMany(
    mappedBy = "user",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY
)
private List<Session> sessions =
    new ArrayList<>();
```

Necesitamos:

```java
import java.util.ArrayList;
import java.util.List;
```

Nuestra entidad `User` quedaría conceptualmente:

```java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    private String name;

    private String email;

    private String password;

    @OneToOne(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private Profile profile;

    @OneToMany(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<Session> sessions =
        new ArrayList<>();
}
```

Ahora tenemos:

```text
              Profile
                 ▲
                 │
                 │ 1:1
                 │
                User
                 │
                 │ 1:N
                 ▼
              Session
```

---

# 9. Entender `mappedBy`

Tenemos:

```java
mappedBy = "user"
```

porque en `Session` existe:

```java
private User user;
```

Por tanto:

```text
User.java

private List<Session> sessions;
             │
             │
      mappedBy = "user"
             │
             ▼

Session.java

private User user;
             ▲
             │
      dueño relación
```

---

# 10. Entender `cascade`

Tenemos:

```java
cascade = CascadeType.ALL
```

y:

```java
orphanRemoval = true
```

Esto significa que si eliminamos:

```text
User
```

también pueden eliminarse sus sesiones:

```text
User
 │
 ├── Session 1  X
 ├── Session 2  X
 └── Session 3  X
```

Esto tiene sentido porque una sesión no debería existir sin usuario.

---

# 11. Crear DTO de entrada

Creamos:

```text
dto/SessionRequestDTO.java
```

```java
package com.example.users.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class SessionRequestDTO {

    @NotBlank(
        message = "El token es obligatorio"
    )
    private String token;

    @NotNull(
        message = "La fecha de expiración es obligatoria"
    )
    @Future(
        message = "La fecha de expiración debe estar en el futuro"
    )
    private Instant expiration;

    private String code2FA;
}
```

Aquí utilizamos:

```java
@Future
```

porque una sesión nueva debería tener una fecha futura de expiración.

---

# 12. Crear DTO de respuesta

Creamos:

```text
dto/SessionResponseDTO.java
```

```java
package com.example.users.dto;

import lombok.Value;

import java.time.Instant;

@Value
public class SessionResponseDTO {

    Long id;

    String token;

    Instant expiration;

    String code2FA;
}
```

No incluimos:

```text
User
```

dentro del DTO.

Así evitamos:

```text
User
 └── Sessions
      └── User
           └── Sessions
                └── User
```

---

# 13. Crear `SessionMapper`

Creamos:

```text
mapper/SessionMapper.java
```

```java
package com.example.users.mapper;

import com.example.users.dto.SessionRequestDTO;
import com.example.users.dto.SessionResponseDTO;
import com.example.users.entity.Session;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SessionMapper {

    public Session toEntity(
            SessionRequestDTO dto) {

        Session session =
            new Session();

        session.setToken(
            dto.getToken()
        );

        session.setExpiration(
            dto.getExpiration()
        );

        session.setCode2FA(
            dto.getCode2FA()
        );

        return session;
    }

    public void updateEntity(
            SessionRequestDTO dto,
            Session session) {

        session.setToken(
            dto.getToken()
        );

        session.setExpiration(
            dto.getExpiration()
        );

        session.setCode2FA(
            dto.getCode2FA()
        );
    }

    public SessionResponseDTO toResponseDTO(
            Session session) {

        return new SessionResponseDTO(
            session.getId(),
            session.getToken(),
            session.getExpiration(),
            session.getCode2FA()
        );
    }

    public List<SessionResponseDTO> toResponseDTOList(
            List<Session> sessions) {

        return sessions
            .stream()
            .map(this::toResponseDTO)
            .toList();
    }
}
```

---

# 14. Crear Repository

Creamos:

```text
repository/SessionRepository.java
```

```java
package com.example.users.repository;

import com.example.users.entity.Session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository
        extends JpaRepository<Session, Long> {

    List<Session> findByUserId(
        Long userId
    );

    Optional<Session> findByIdAndUserId(
        Long id,
        Long userId
    );

    boolean existsByToken(
        String token
    );
}
```

Tenemos tres consultas útiles:

```text
findByUserId
       ↓
todas las sesiones
de un usuario
```

```text
findByIdAndUserId
       ↓
una sesión específica
que pertenezca al usuario
```

```text
existsByToken
       ↓
verificar que el token
no esté repetido
```

---

# 15. Crear `SessionService`

Creamos:

```text
service/SessionService.java
```

```java
package com.example.users.service;

import com.example.users.dto.SessionRequestDTO;
import com.example.users.dto.SessionResponseDTO;
import com.example.users.entity.Session;
import com.example.users.entity.User;
import com.example.users.mapper.SessionMapper;
import com.example.users.repository.SessionRepository;
import com.example.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    private final UserRepository userRepository;

    private final SessionMapper sessionMapper;
}
```

---

# 16. Crear una Session para un User

Agregamos:

```java
public SessionResponseDTO create(
        Long userId,
        SessionRequestDTO dto) {

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado"
                )
            );

    if (
        sessionRepository.existsByToken(
            dto.getToken()
        )
    ) {

        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "El token ya existe"
        );
    }

    Session session =
        sessionMapper.toEntity(dto);

    session.setUser(user);

    Session savedSession =
        sessionRepository.save(session);

    return sessionMapper
        .toResponseDTO(savedSession);
}
```

La línea clave es:

```java
session.setUser(user);
```

porque establece:

```text
Session.user_id
       ↓
     User.id
```

---

# 17. ¿Qué sucede en la base de datos?

Por ejemplo:

```text
users

id | name
──────────────
1  | Juan
```

Creamos tres sesiones:

```text
sessions

id | token | user_id
────────────────────
1  | AAA   | 1
2  | BBB   | 1
3  | CCC   | 1
```

Tenemos:

```text
        Juan
         │
     ┌───┼───┐
     ▼   ▼   ▼
    S1  S2   S3
```

Ese es precisamente el:

```text
1:N
```

---

# 18. Listar sesiones de un usuario

Agregamos:

```java
public List<SessionResponseDTO> findAllByUser(
        Long userId) {

    if (
        !userRepository.existsById(userId)
    ) {

        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Usuario no encontrado"
        );
    }

    List<Session> sessions =
        sessionRepository
            .findByUserId(userId);

    return sessionMapper
        .toResponseDTOList(sessions);
}
```

---

# 19. Buscar una sesión específica

```java
public SessionResponseDTO findById(
        Long userId,
        Long sessionId) {

    Session session =
        findSession(
            userId,
            sessionId
        );

    return sessionMapper
        .toResponseDTO(session);
}
```

---

# 20. Método privado para buscar Session

```java
private Session findSession(
        Long userId,
        Long sessionId) {

    return sessionRepository
        .findByIdAndUserId(
            sessionId,
            userId
        )
        .orElseThrow(
            () -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Sesión no encontrada"
            )
        );
}
```

Esto garantiza no solamente que:

```text
Session existe
```

sino también que:

```text
Session pertenece a User
```

---

# 21. Actualizar una Session

```java
public SessionResponseDTO update(
        Long userId,
        Long sessionId,
        SessionRequestDTO dto) {

    Session session =
        findSession(
            userId,
            sessionId
        );

    if (
        !session.getToken()
            .equals(dto.getToken())
        &&
        sessionRepository.existsByToken(
            dto.getToken()
        )
    ) {

        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "El token ya existe"
        );
    }

    sessionMapper.updateEntity(
        dto,
        session
    );

    Session updatedSession =
        sessionRepository.save(session);

    return sessionMapper
        .toResponseDTO(updatedSession);
}
```

---

# 22. Eliminar una Session

```java
public void delete(
        Long userId,
        Long sessionId) {

    Session session =
        findSession(
            userId,
            sessionId
        );

    sessionRepository.delete(session);
}
```

---

# 23. `SessionService` completo

```java
package com.example.users.service;

import com.example.users.dto.SessionRequestDTO;
import com.example.users.dto.SessionResponseDTO;
import com.example.users.entity.Session;
import com.example.users.entity.User;
import com.example.users.mapper.SessionMapper;
import com.example.users.repository.SessionRepository;
import com.example.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    private final UserRepository userRepository;

    private final SessionMapper sessionMapper;

    public SessionResponseDTO create(
            Long userId,
            SessionRequestDTO dto) {

        User user =
            userRepository
                .findById(userId)
                .orElseThrow(
                    () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                    )
                );

        if (
            sessionRepository.existsByToken(
                dto.getToken()
            )
        ) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El token ya existe"
            );
        }

        Session session =
            sessionMapper.toEntity(dto);

        session.setUser(user);

        Session savedSession =
            sessionRepository.save(session);

        return sessionMapper
            .toResponseDTO(savedSession);
    }

    public List<SessionResponseDTO> findAllByUser(
            Long userId) {

        if (
            !userRepository.existsById(userId)
        ) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            );
        }

        return sessionMapper
            .toResponseDTOList(
                sessionRepository
                    .findByUserId(userId)
            );
    }

    public SessionResponseDTO findById(
            Long userId,
            Long sessionId) {

        return sessionMapper
            .toResponseDTO(
                findSession(
                    userId,
                    sessionId
                )
            );
    }

    public SessionResponseDTO update(
            Long userId,
            Long sessionId,
            SessionRequestDTO dto) {

        Session session =
            findSession(
                userId,
                sessionId
            );

        if (
            !session.getToken()
                .equals(dto.getToken())
            &&
            sessionRepository.existsByToken(
                dto.getToken()
            )
        ) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El token ya existe"
            );
        }

        sessionMapper.updateEntity(
            dto,
            session
        );

        Session updatedSession =
            sessionRepository.save(session);

        return sessionMapper
            .toResponseDTO(updatedSession);
    }

    public void delete(
            Long userId,
            Long sessionId) {

        Session session =
            findSession(
                userId,
                sessionId
            );

        sessionRepository.delete(session);
    }

    private Session findSession(
            Long userId,
            Long sessionId) {

        return sessionRepository
            .findByIdAndUserId(
                sessionId,
                userId
            )
            .orElseThrow(
                () -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Sesión no encontrada"
                )
            );
    }
}
```

---

# 24. Crear `SessionController`

Como una Session pertenece a un User, utilizaremos rutas anidadas:

```text
/api/users/{userId}/sessions
```

Creamos:

```text
controller/SessionController.java
```

```java
package com.example.users.controller;

import com.example.users.dto.SessionRequestDTO;
import com.example.users.dto.SessionResponseDTO;
import com.example.users.service.SessionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
    "/api/users/{userId}/sessions"
)
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponseDTO create(
            @PathVariable Long userId,
            @Valid
            @RequestBody SessionRequestDTO dto) {

        return sessionService.create(
            userId,
            dto
        );
    }

    @GetMapping
    public List<SessionResponseDTO> findAll(
            @PathVariable Long userId) {

        return sessionService
            .findAllByUser(userId);
    }

    @GetMapping("/{sessionId}")
    public SessionResponseDTO findById(
            @PathVariable Long userId,
            @PathVariable Long sessionId) {

        return sessionService.findById(
            userId,
            sessionId
        );
    }

    @PutMapping("/{sessionId}")
    public SessionResponseDTO update(
            @PathVariable Long userId,
            @PathVariable Long sessionId,
            @Valid
            @RequestBody SessionRequestDTO dto) {

        return sessionService.update(
            userId,
            sessionId,
            dto
        );
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId,
            @PathVariable Long sessionId) {

        sessionService.delete(
            userId,
            sessionId
        );
    }
}
```

---

# 25. Endpoints resultantes

## Crear sesión

```http
POST /api/users/{userId}/sessions
```

## Listar sesiones de un usuario

```http
GET /api/users/{userId}/sessions
```

## Consultar una sesión

```http
GET /api/users/{userId}/sessions/{sessionId}
```

## Actualizar sesión

```http
PUT /api/users/{userId}/sessions/{sessionId}
```

## Eliminar sesión

```http
DELETE /api/users/{userId}/sessions/{sessionId}
```

---

# 26. Crear una sesión

Primero debemos tener un usuario:

```http
POST /api/users
```

```json
{
  "name": "Juan Pérez",
  "email": "juan@gmail.com",
  "password": "12345678"
}
```

Supongamos que obtenemos:

```json
{
  "id": 1,
  "name": "Juan Pérez",
  "email": "juan@gmail.com"
}
```

Ahora:

```http
POST /api/users/1/sessions
```

```json
{
  "token": "token-abc-123",
  "expiration": "2026-08-20T04:00:00Z",
  "code2FA": "452891"
}
```

Respuesta:

```json
{
  "id": 1,
  "token": "token-abc-123",
  "expiration": "2026-08-20T04:00:00Z",
  "code2FA": "452891"
}
```

---

# 27. Crear una segunda sesión

El mismo usuario puede tener otra sesión:

```http
POST /api/users/1/sessions
```

```json
{
  "token": "token-xyz-789",
  "expiration": "2026-08-21T04:00:00Z",
  "code2FA": null
}
```

Ahora MySQL tendrá:

```text
sessions

id | token         | user_id
──────────────────────────────
1  | token-abc-123 | 1
2  | token-xyz-789 | 1
```

Y tenemos:

```text
User 1
 │
 ├── Session 1
 └── Session 2
```

---

# 28. Listar sesiones del usuario

```http
GET /api/users/1/sessions
```

Respuesta:

```json
[
  {
    "id": 1,
    "token": "token-abc-123",
    "expiration": "2026-08-20T04:00:00Z",
    "code2FA": "452891"
  },
  {
    "id": 2,
    "token": "token-xyz-789",
    "expiration": "2026-08-21T04:00:00Z",
    "code2FA": null
  }
]
```

---

# 29. Incluir Sessions al consultar un usuario

Como anteriormente decidimos que:

```http
GET /api/users/{id}
```

representa el **detalle completo del usuario**, podemos agregar las sesiones.

Actualmente tenemos:

```text
UserDetailResponseDTO
─────────────────────
id
name
email
profile
```

Lo modificamos:

```java
package com.example.users.dto;

import lombok.Value;

import java.util.List;

@Value
public class UserDetailResponseDTO {

    Long id;

    String name;

    String email;

    ProfileResponseDTO profile;

    List<SessionResponseDTO> sessions;
}
```

Ahora tendremos:

```text
UserDetailResponseDTO
│
├── id
├── name
├── email
│
├── profile
│    ├── id
│    ├── phone
│    └── birthDate
│
└── sessions
     ├── Session
     ├── Session
     └── Session
```

---

# 30. Modificar `UserMapper`

Agregamos:

```java
private final SessionMapper sessionMapper;
```

Nuestra clase tendrá:

```java
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ProfileMapper profileMapper;

    private final SessionMapper sessionMapper;

    ...
}
```

Modificamos:

```java
public UserDetailResponseDTO toDetailResponseDTO(
        User user) {

    return new UserDetailResponseDTO(
        user.getId(),
        user.getName(),
        user.getEmail(),

        profileMapper.toResponseDTO(
            user.getProfile()
        ),

        sessionMapper.toResponseDTOList(
            user.getSessions()
        )
    );
}
```

---

# 31. Cargar Profile + Sessions al consultar User

Podemos modificar nuestro método del Repository:

```java
@EntityGraph(
    attributePaths = {
        "profile",
        "sessions"
    }
)
Optional<User> findWithDetailsById(
    Long id
);
```

Nuestro `UserRepository` tendrá:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    boolean existsByEmail(
        String email
    );

    boolean existsByEmailAndIdNot(
        String email,
        Long id
    );

    @EntityGraph(
        attributePaths = {
            "profile",
            "sessions"
        }
    )
    Optional<User> findWithDetailsById(
        Long id
    );
}
```

Entonces:

```text
GET /api/users/1

        ↓

UserRepository

        ↓

User
├── Profile
└── Sessions
```

---

# 32. Modificar `UserService`

Nuestro método:

```java
public UserDetailResponseDTO findById(
        Long id) {

    User user =
        userRepository
            .findWithDetailsById(id)
            .orElseThrow(
                () -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado"
                )
            );

    return userMapper
        .toDetailResponseDTO(user);
}
```

---

# 33. Resultado al consultar un usuario

```http
GET /api/users/1
```

Respuesta:

```json
{
  "id": 1,
  "name": "Juan Pérez",
  "email": "juan@gmail.com",
  "profile": {
    "id": 1,
    "phone": "3001234567",
    "birthDate": "1995-05-20"
  },
  "sessions": [
    {
      "id": 1,
      "token": "token-abc-123",
      "expiration": "2026-08-20T04:00:00Z",
      "code2FA": "452891"
    },
    {
      "id": 2,
      "token": "token-xyz-789",
      "expiration": "2026-08-21T04:00:00Z",
      "code2FA": null
    }
  ]
}
```

Mientras que:

```http
GET /api/users
```

puede seguir devolviendo solamente:

```json
[
  {
    "id": 1,
    "name": "Juan Pérez",
    "email": "juan@gmail.com"
  }
]
```

Así mantenemos:

```text
LISTADO
↓
liviano


DETALLE
↓
User
+ Profile
+ Sessions
```

---

# 34. Verificar MySQL

Podemos ejecutar:

```sql
SELECT * FROM sessions;
```

Por ejemplo:

```text
id | token         | expiration | code_2fa | user_id
──────────────────────────────────────────────────────
1  | token-abc-123 | ...        | 452891   | 1
2  | token-xyz-789 | ...        | NULL     | 1
```

También podemos hacer manualmente el JOIN:

```sql
SELECT
    u.id,
    u.name,
    s.id AS session_id,
    s.token
FROM users u
INNER JOIN sessions s
    ON u.id = s.user_id;
```

Resultado conceptual:

```text
Juan ─── Session 1
Juan ─── Session 2
Juan ─── Session 3
```

---

# 35. Diferencia entre 1:1 y 1:N

Ahora podemos comparar las dos relaciones que ya tenemos.

## User — Profile

```text
User
 │
 │ 1
 │
 │ 0..1
 ▼
Profile
```

La FK:

```text
profiles.user_id
```

tiene:

```text
UNIQUE
```

porque un usuario solo puede aparecer una vez.

---

## User — Session

```text
User
 │
 │ 1
 │
 │ N
 ▼
Session
```

La FK:

```text
sessions.user_id
```

**NO es UNIQUE**.

Por ejemplo:

```text
user_id

1
1
1
2
2
```

Eso permite que el mismo usuario tenga muchas sesiones.

Esta es una diferencia fundamental.

---

# 36. Comparación de las anotaciones

## Relación 1:1

### Profile

```java
@OneToOne
@JoinColumn(
    name = "user_id",
    unique = true
)
private User user;
```

### User

```java
@OneToOne(
    mappedBy = "user"
)
private Profile profile;
```

---

## Relación 1:N

### Session

```java
@ManyToOne
@JoinColumn(
    name = "user_id"
)
private User user;
```

### User

```java
@OneToMany(
    mappedBy = "user"
)
private List<Session> sessions;
```

---

# 37. Diagrama general del proyecto

Nuestro modelo ahora tiene:

```text
                    Profile
                    ───────
                    id
                    phone
                    birthDate
                       ▲
                       │
                       │ 1 : 0..1
                       │
                     User
                     ────
                     id
                     name
                     email
                     password
                       │
                       │ 1
                       │
                       │ N
                       ▼
                    Session
                    ───────
                    id
                    token
                    expiration
                    code2FA
```

En base de datos:

```text
             users
              │
       ┌──────┴─────────┐
       │                │
       │ 1              │ 1
       │                │
       ▼                ▼
   profiles          sessions
      0..1               N
```

---

# 38. Qué debe aprender el estudiante

Al terminar esta práctica debe poder explicar:

### `@OneToMany`

```text
Una entidad puede tener
muchos registros relacionados.
```

### `@ManyToOne`

```text
Muchos registros pertenecen
a una misma entidad.
```

### `mappedBy`

```text
Indica que el otro lado
es el propietario de la relación.
```

### `@JoinColumn`

```text
Define dónde se almacena
la llave foránea.
```

### `List<Session>`

```text
Representa la colección de
sesiones de un usuario.
```

### Diferencia entre 1:1 y 1:N

```text
1:1
FK UNIQUE


1:N
FK NO UNIQUE
```

---

# 39. Importante sobre Session

Por ahora estamos creando manualmente:

```text
token
expiration
code2FA
```

porque el objetivo de esta práctica es aprender:

```text
@OneToMany
@ManyToOne
@JoinColumn
mappedBy
```

Posteriormente, cuando lleguemos al módulo:

```text
Autenticación
Login
JWT
2FA
```

el flujo debería cambiar.

El frontend no debería decir:

```json
{
  "token": "yo-elijo-el-token"
}
```

sino que será el backend quien genere esos datos:

```text
Login correcto
      ↓
Backend
      ↓
Generar JWT
      ↓
Calcular expiración
      ↓
Crear Session
      ↓
MySQL
```

Conceptualmente:

```text
email + password
       │
       ▼
   Autenticación
       │
       ▼
   generar token
       │
       ▼
    Session
       │
       ├── token
       ├── expiration
       ├── code2FA
       └── user_id
```

Por ahora lo hacemos manualmente únicamente para concentrarnos en la relación **1:N**.

---

# 40. Evolución del proyecto

Ya tenemos:

```text
            Profile
               ▲
               │ 1:1
               │
              User
               │
               │ 1:N
               ▼
            Session
```

El siguiente paso natural para practicar la tercera relación importante sería:

```text
User
 │
 │ N:N
 ▼
Role
```

Así el proyecto permitirá enseñar progresivamente:

```text
1 : 1
User ─── Profile

1 : N
User ─── Session

N : N
User ─── Role
```

Con esas tres relaciones el estudiante ya tendría prácticamente todo el fundamento necesario para trabajar asociaciones entre entidades con JPA.
