# Guía práctica: Relación 1:N entre `User` y `Session`

## 1. Objetivo

Continuaremos el proyecto anterior agregando la entidad:

```text
Session
──────────────────
id          : Long
token       : String
expiration  : Date
code2FA     : String
```

Un usuario podrá tener muchas sesiones, pero cada sesión pertenecerá a un solo usuario:

```text
User 1 ───────── N Session
```

Visualmente:

```text
User                    Session
────                    ───────
id                      id
name                    token
email                   expiration
password                code2FA
                        user_id
   │                       │
   └──────── 1 : N ────────┘
```

La base de datos tendrá:

```text
users
────────────────
id
name
email
password


sessions
────────────────
id
token
expiration
code_2fa
user_id  ← FK
```

El requisito principal será:

> Cuando consultemos un usuario con sus sesiones, se deben devolver todas las sesiones que le pertenecen.

---

# 2. Comprender la cardinalidad

La relación:

```text
User 1 ───────── N Session
```

significa que:

```text
Un User
puede tener
cero, una o muchas Session
```

pero:

```text
Cada Session
pertenece exactamente
a un User
```

Por ejemplo:

```text
User 1
 ├── Session 10
 ├── Session 11
 └── Session 12

User 2
 └── Session 13
```

La cardinalidad más precisa será:

```text
User 1 ───────── 0..N Session
```

---

# 3. ¿Quién será el dueño de la relación?

Haremos que `Session` sea el dueño de la relación porque allí estará la llave foránea:

```text
sessions.user_id
```

Por tanto, en `Session` tendremos:

```java
@ManyToOne
@JoinColumn(...)
private User user;
```

Y en `User` tendremos:

```java
@OneToMany(mappedBy = "user")
private List<Session> sessions;
```

Visualmente:

```text
User
│
│ mappedBy = "user"
│
│ 1
│
│ N
▼
Session
│
└── user_id FK
```

---

# 4. Diferencia con la relación 1:1

En la relación entre `User` y `Profile` utilizamos:

```java
@OneToOne
```

y la llave foránea tenía:

```java
unique = true
```

En esta relación utilizaremos:

```java
@OneToMany
@ManyToOne
```

y `user_id` no será único.

Esto permite almacenar:

```text
session.id | session.user_id
────────────────────────────
1          | 5
2          | 5
3          | 5
```

Las tres sesiones pertenecen al mismo usuario.

---

# 5. Estructura del proyecto

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
│   ├── BaseUserDTO.java
│   ├── CreateUserDTO.java
│   ├── UpdateUserDTO.java
│   ├── UserResponseDTO.java
│   ├── UserDetailResponseDTO.java
│   ├── UserSessionsResponseDTO.java
│   │
│   ├── ProfileRequestDTO.java
│   ├── ProfileResponseDTO.java
│   │
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

# 6. Tipo de dato para `expiration`

Aunque conceptualmente tenemos:

```text
expiration : Date
```

una expiración necesita fecha y hora. En esta práctica utilizaremos:

```java
LocalDateTime
```

Importaremos:

```java
java.time.LocalDateTime;
```

Un valor tendrá esta forma:

```text
2026-08-28T18:30:00
```

MySQL lo almacenará como:

```text
DATETIME
```

> En un sistema distribuido o con usuarios en distintas zonas horarias conviene utilizar `Instant` y almacenar los tiempos en UTC.

---

# 7. Crear Entity `Session`

Creamos:

```text
entity/Session.java
```

```java
package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private LocalDateTime expiration;

    @Column(
            name = "code_2fa",
            length = 10
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

La parte importante es:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(
    name = "user_id",
    nullable = false
)
private User user;
```

Observa que no usamos:

```java
unique = true
```

porque un mismo `user_id` debe poder aparecer en muchas filas de `sessions`.

---

# 8. ¿Por qué el `token` sí es único?

La columna:

```java
unique = true
```

se aplicó a `token`, no a `user_id`.

Esto garantiza que no existan dos sesiones con el mismo token:

```text
Session 1 ── token ABC123
Session 2 ── token ABC123   ← no permitido
```

Sin embargo, ambas sesiones sí pueden pertenecer al mismo usuario:

```text
Session 1 ── user_id 5
Session 2 ── user_id 5      ← permitido
```

---

# 9. Modificar Entity `User`

Agregaremos una colección de sesiones:

```java
@OneToMany(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
)
private List<Session> sessions = new ArrayList<>();
```

También necesitaremos:

```java
import java.util.ArrayList;
import java.util.List;
```

La entidad completa, incluyendo la relación anterior con `Profile`, quedaría:

```java
package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @Column(
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            nullable = false,
            unique = true,
            length = 150
    )
    private String email;

    @Column(
            nullable = false
    )
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
    private List<Session> sessions = new ArrayList<>();
}
```

---

# 10. Entender `mappedBy`

Tenemos:

```java
mappedBy = "user"
```

Ese `"user"` corresponde exactamente al atributo de `Session`:

```java
private User user;
```

Es decir:

```text
User.java

private List<Session> sessions;
              │
              │ mappedBy = "user"
              ▼

Session.java

private User user;
             ▲
             │
        dueño de la relación
```

`mappedBy` no contiene el nombre de la columna de MySQL. Contiene el nombre del atributo Java ubicado en la entidad propietaria.

---

# 11. ¿Por qué usamos una lista?

En una relación 1:N, un usuario no tiene una sola sesión:

```java
private Session session;
```

Tiene una colección:

```java
private List<Session> sessions;
```

La inicializamos para evitar que sea `null`:

```java
private List<Session> sessions = new ArrayList<>();
```

Así, un usuario sin sesiones tendrá:

```json
"sessions": []
```

en lugar de:

```json
"sessions": null
```

---

# 12. Entender `cascade` y `orphanRemoval`

Utilizamos:

```java
cascade = CascadeType.ALL
```

porque las sesiones dependen del usuario.

Por ejemplo:

```text
Eliminar User
    ↓
Eliminar sus Session
```

También utilizamos:

```java
orphanRemoval = true
```

Esto permite eliminar una sesión que sea retirada de la colección del usuario.

Tiene sentido si consideramos que:

> Una `Session` no debe existir sin estar asociada a un `User`.

---

# 13. Métodos auxiliares para sincronizar la relación

En relaciones bidireccionales es recomendable mantener sincronizados ambos lados.

Podemos agregar en `User`:

```java
public void addSession(Session session) {
    sessions.add(session);
    session.setUser(this);
}

public void removeSession(Session session) {
    sessions.remove(session);
    session.setUser(null);
}
```

Así, al ejecutar:

```java
user.addSession(session);
```

se actualizan los dos lados en memoria:

```text
User.sessions contiene Session
Session.user apunta a User
```

En el servicio de esta guía guardaremos directamente `Session`, por lo que bastará con:

```java
session.setUser(user);
```

---

# 14. DTO de entrada de Session

Creamos:

```text
dto/SessionRequestDTO.java
```

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private LocalDateTime expiration;

    @Size(
            min = 6,
            max = 10,
            message = "El código 2FA debe tener entre 6 y 10 caracteres"
    )
    private String code2FA;
}
```

Aquí utilizamos:

```java
@Future
```

para impedir que se cree una sesión que ya haya expirado.

---

# 15. DTO de respuesta de Session

Creamos:

```text
dto/SessionResponseDTO.java
```

```java
package com.uc.ms_security.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class SessionResponseDTO {

    Long id;

    String token;

    LocalDateTime expiration;

    String code2FA;
}
```

Observa que no agregamos:

```text
user
```

dentro del DTO de sesión. Esto evita un ciclo como:

```text
User
 ↓
Session
 ↓
User
 ↓
Session
 ...
```

> En una aplicación real, normalmente no se devuelve el código 2FA y puede ser conveniente no exponer el token completo. Se incluye aquí únicamente para comprender el CRUD y la relación.

---

# 16. Crear un DTO para ver el usuario con sus sesiones

El DTO usado para el listado puede continuar siendo:

```text
UserResponseDTO
────────────────
id
name
email
```

Crearemos:

```text
dto/UserSessionsResponseDTO.java
```

```java
package com.uc.ms_security.dto;

import lombok.Value;

import java.util.List;

@Value
public class UserSessionsResponseDTO {

    Long id;

    String name;

    String email;

    List<SessionResponseDTO> sessions;
}
```

Así tendremos:

```text
GET /api/users

UserResponseDTO
────────────────
id
name
email
```

Pero:

```text
GET /api/users/1/detail-with-sessions

UserSessionsResponseDTO
───────────────────────
id
name
email
sessions
   ├── Session 1
   ├── Session 2
   └── Session 3
```

---

# 17. Ejemplo de respuesta

```json
{
  "id": 1,
  "name": "Juan Pérez",
  "email": "juan@gmail.com",
  "sessions": [
    {
      "id": 10,
      "token": "token-dispositivo-a",
      "expiration": "2026-08-29T18:30:00",
      "code2FA": "381924"
    },
    {
      "id": 11,
      "token": "token-dispositivo-b",
      "expiration": "2026-08-30T10:00:00",
      "code2FA": "794215"
    }
  ]
}
```

---

# 18. Crear `SessionMapper`

Creamos:

```text
mapper/SessionMapper.java
```

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.SessionRequestDTO;
import com.uc.ms_security.dto.SessionResponseDTO;
import com.uc.ms_security.entity.Session;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SessionMapper {

    public Session toEntity(SessionRequestDTO dto) {
        Session session = new Session();
        session.setToken(dto.getToken());
        session.setExpiration(dto.getExpiration());
        session.setCode2FA(dto.getCode2FA());
        return session;
    }

    public void updateEntity(
            SessionRequestDTO dto,
            Session session) {

        session.setToken(dto.getToken());
        session.setExpiration(dto.getExpiration());
        session.setCode2FA(dto.getCode2FA());
    }

    public SessionResponseDTO toResponseDTO(Session session) {
        return new SessionResponseDTO(
                session.getId(),
                session.getToken(),
                session.getExpiration(),
                session.getCode2FA()
        );
    }

    public List<SessionResponseDTO> toResponseDTOList(
            List<Session> sessions) {

        return sessions.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
```

---

# 19. Modificar `UserMapper`

Ahora también debe convertir:

```text
User
 ↓
UserSessionsResponseDTO
```

Inyectamos `SessionMapper`:

```java
private final SessionMapper sessionMapper;
```

Una versión completa, conservando el soporte para `Profile`, quedaría:

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.*;
import com.uc.ms_security.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ProfileMapper profileMapper;
    private final SessionMapper sessionMapper;

    public User toEntity(CreateUserDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        return user;
    }

    public void updateEntity(UpdateUserDTO dto, User user) {
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        if (dto.getPassword() != null) {
            user.setPassword(dto.getPassword());
        }
    }

    public UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public UserDetailResponseDTO toDetailResponseDTO(User user) {
        return new UserDetailResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                profileMapper.toResponseDTO(user.getProfile())
        );
    }

    public UserSessionsResponseDTO toSessionsResponseDTO(User user) {
        return new UserSessionsResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                sessionMapper.toResponseDTOList(user.getSessions())
        );
    }

    public List<UserResponseDTO> toResponseDTOList(List<User> users) {
        return users.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
```

---

# 20. Crear `SessionRepository`

Creamos:

```text
repository/SessionRepository.java
```

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository
        extends JpaRepository<Session, Long> {

    List<Session> findAllByUserId(Long userId);

    Optional<Session> findByIdAndUserId(
            Long sessionId,
            Long userId
    );

    boolean existsByToken(String token);

    boolean existsByTokenAndIdNot(
            String token,
            Long id
    );
}
```

Estos métodos representan consultas como:

```text
findAllByUserId
    ↓
SELECT sesiones del usuario
```

y:

```text
findByIdAndUserId
    ↓
SELECT una sesión
si pertenece al usuario indicado
```

El segundo método evita consultar o modificar accidentalmente una sesión de otro usuario.

---

# 21. El problema del Lazy Loading

Configuramos:

```java
fetch = FetchType.LAZY
```

Esto significa:

> No cargar automáticamente todas las sesiones cada vez que se cargue un usuario.

Es conveniente para:

```http
GET /api/users
```

porque posiblemente solo queremos:

```text
id
name
email
```

Queremos cargar las sesiones únicamente cuando se soliciten:

```http
GET /api/users/{id}/detail-with-sessions
```

---

# 22. Cargar las sesiones al consultar un usuario

Actualizamos:

```text
repository/UserRepository.java
```

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(
            String email,
            Long id
    );

    @EntityGraph(attributePaths = {"profile"})
    Optional<User> findWithProfileById(Long id);

    @EntityGraph(attributePaths = {"sessions"})
    Optional<User> findWithSessionsById(Long id);
}
```

Ahora tenemos:

```text
findById()
     ↓
User


findWithProfileById()
     ↓
User + Profile


findWithSessionsById()
     ↓
User + List<Session>
```

---

# 23. ¿Por qué no poner `EAGER`?

Si utilizáramos:

```java
@OneToMany(fetch = FetchType.EAGER)
```

cada consulta de usuario cargaría todas sus sesiones.

Por ejemplo:

```text
GET /api/users
 ↓

User 1 + 8 sesiones
User 2 + 3 sesiones
User 3 + 15 sesiones
...
```

Aunque el listado solamente necesite:

```text
id
name
email
```

Por eso preferimos:

```java
LAZY
```

y cargamos la colección explícitamente cuando realmente se necesita.

---

# 24. Modificar `UserService`

Agregamos un método para consultar el usuario junto con sus sesiones:

```java
public UserSessionsResponseDTO findByIdAndSessions(Long id) {
    User user = userRepository
            .findWithSessionsById(id)
            .orElseThrow(
                    () -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Usuario no encontrado"
                    )
            );

    return userMapper.toSessionsResponseDTO(user);
}
```

El recorrido será:

```text
UserRepository
      ↓
User + sesiones
      ↓
UserMapper
      ↓
UserSessionsResponseDTO
```

---

# 25. Modificar `UserController`

Agregamos:

```java
@GetMapping("/{id}/detail-with-sessions")
public UserSessionsResponseDTO findByIdAndSessions(
        @PathVariable Long id) {

    return userService.findByIdAndSessions(id);
}
```

Así:

```http
GET /api/users/1/detail-with-sessions
```

devuelve el usuario junto con todas sus sesiones.

Mientras:

```http
GET /api/users
```

puede seguir devolviendo únicamente los datos básicos de cada usuario.

---

# 26. Crear `SessionService`

Creamos:

```text
service/SessionService.java
```

```java
package com.uc.ms_security.service;

import com.uc.ms_security.dto.SessionRequestDTO;
import com.uc.ms_security.dto.SessionResponseDTO;
import com.uc.ms_security.entity.Session;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.mapper.SessionMapper;
import com.uc.ms_security.repository.SessionRepository;
import com.uc.ms_security.repository.UserRepository;
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

        User user = findUser(userId);

        if (sessionRepository.existsByToken(dto.getToken())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El token ya está registrado"
            );
        }

        Session session = sessionMapper.toEntity(dto);
        session.setUser(user);

        Session savedSession = sessionRepository.save(session);

        return sessionMapper.toResponseDTO(savedSession);
    }

    public List<SessionResponseDTO> findAllByUserId(Long userId) {
        findUser(userId);

        List<Session> sessions =
                sessionRepository.findAllByUserId(userId);

        return sessionMapper.toResponseDTOList(sessions);
    }

    public SessionResponseDTO findById(
            Long userId,
            Long sessionId) {

        return sessionMapper.toResponseDTO(
                findSession(userId, sessionId)
        );
    }

    public SessionResponseDTO update(
            Long userId,
            Long sessionId,
            SessionRequestDTO dto) {

        Session session = findSession(userId, sessionId);

        if (sessionRepository.existsByTokenAndIdNot(
                dto.getToken(),
                sessionId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El token ya está registrado"
            );
        }

        sessionMapper.updateEntity(dto, session);

        Session updatedSession = sessionRepository.save(session);

        return sessionMapper.toResponseDTO(updatedSession);
    }

    public void delete(
            Long userId,
            Long sessionId) {

        sessionRepository.delete(
                findSession(userId, sessionId)
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        )
                );
    }

    private Session findSession(
            Long userId,
            Long sessionId) {

        return sessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Sesión no encontrada para este usuario"
                        )
                );
    }
}
```

---

# 27. ¿Por qué buscamos por `sessionId` y `userId`?

Podríamos buscar solamente:

```java
sessionRepository.findById(sessionId)
```

Pero nuestra ruta contiene ambos identificadores:

```http
/api/users/{userId}/sessions/{sessionId}
```

Por tanto, debemos comprobar que la sesión realmente pertenece al usuario indicado:

```java
findByIdAndUserId(sessionId, userId)
```

Esto evita una inconsistencia como:

```text
La Session 20 pertenece al User 3

Solicitud incorrecta:
/api/users/8/sessions/20
```

El servicio responderá:

```text
404 Sesión no encontrada para este usuario
```

---

# 28. Crear `SessionController`

Creamos:

```text
controller/SessionController.java
```

Como `Session` pertenece a `User`, utilizaremos rutas anidadas:

```text
/api/users/{userId}/sessions
```

Controller:

```java
package com.uc.ms_security.controller;

import com.uc.ms_security.dto.SessionRequestDTO;
import com.uc.ms_security.dto.SessionResponseDTO;
import com.uc.ms_security.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponseDTO create(
            @PathVariable Long userId,
            @Valid @RequestBody SessionRequestDTO dto) {

        return sessionService.create(userId, dto);
    }

    @GetMapping
    public List<SessionResponseDTO> findAll(
            @PathVariable Long userId) {

        return sessionService.findAllByUserId(userId);
    }

    @GetMapping("/{sessionId}")
    public SessionResponseDTO findById(
            @PathVariable Long userId,
            @PathVariable Long sessionId) {

        return sessionService.findById(userId, sessionId);
    }

    @PutMapping("/{sessionId}")
    public SessionResponseDTO update(
            @PathVariable Long userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionRequestDTO dto) {

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

        sessionService.delete(userId, sessionId);
    }
}
```

---

# 29. Endpoints resultantes

## Usuarios

```http
POST    /api/users
GET     /api/users
GET     /api/users/{id}
PUT     /api/users/{id}
DELETE  /api/users/{id}
```

## Perfil del usuario

```http
POST    /api/users/{userId}/profile
GET     /api/users/{userId}/profile
PUT     /api/users/{userId}/profile
DELETE  /api/users/{userId}/profile
```

## Sesiones del usuario

```http
POST    /api/users/{userId}/sessions
GET     /api/users/{userId}/sessions
GET     /api/users/{userId}/sessions/{sessionId}
PUT     /api/users/{userId}/sessions/{sessionId}
DELETE  /api/users/{userId}/sessions/{sessionId}
```

Esta estructura REST expresa que:

```text
Session
```

es un recurso dependiente de:

```text
User
```

---

# 30. Crear un usuario

Primero:

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

Respuesta:

```json
{
  "id": 1,
  "name": "Juan Pérez",
  "email": "juan@gmail.com"
}
```

---

# 31. Crear la primera sesión

```http
POST /api/users/1/sessions
```

```json
{
  "token": "token-iphone-14-pro",
  "expiration": "2026-08-29T18:30:00",
  "code2FA": "381924"
}
```

Respuesta:

```json
{
  "id": 1,
  "token": "token-iphone-14-pro",
  "expiration": "2026-08-29T18:30:00",
  "code2FA": "381924"
}
```

---

# 32. Crear una segunda sesión para el mismo usuario

```http
POST /api/users/1/sessions
```

```json
{
  "token": "token-computador-personal",
  "expiration": "2026-08-30T10:00:00",
  "code2FA": "794215"
}
```

Ahora el mismo usuario tiene:

```text
User 1
 ├── Session 1
 └── Session 2
```

Esto demuestra la relación:

```text
1 : N
```

---

# 33. Consultar todas las sesiones de un usuario

```http
GET /api/users/1/sessions
```

Respuesta:

```json
[
  {
    "id": 1,
    "token": "token-iphone-14-pro",
    "expiration": "2026-08-29T18:30:00",
    "code2FA": "381924"
  },
  {
    "id": 2,
    "token": "token-computador-personal",
    "expiration": "2026-08-30T10:00:00",
    "code2FA": "794215"
  }
]
```

---

# 34. Consultar una sesión específica

```http
GET /api/users/1/sessions/2
```

Respuesta:

```json
{
  "id": 2,
  "token": "token-computador-personal",
  "expiration": "2026-08-30T10:00:00",
  "code2FA": "794215"
}
```

---

# 35. Consultar el usuario con sus sesiones

La ruta del `SessionController`:

```http
GET /api/users/1/sessions
```

devuelve directamente la lista de sesiones. Si queremos devolver los datos del usuario y la colección dentro de un mismo objeto, usamos el método agregado al `UserController`:

```java
@GetMapping("/{id}/detail-with-sessions")
public UserSessionsResponseDTO findByIdAndSessions(
        @PathVariable Long id) {

    return userService.findByIdAndSessions(id);
}
```

La ruta será:

```http
GET /api/users/1/detail-with-sessions
```

Respuesta:

```json
{
  "id": 1,
  "name": "Juan Pérez",
  "email": "juan@gmail.com",
  "sessions": [
    {
      "id": 1,
      "token": "token-iphone-14-pro",
      "expiration": "2026-08-29T18:30:00",
      "code2FA": "381924"
    },
    {
      "id": 2,
      "token": "token-computador-personal",
      "expiration": "2026-08-30T10:00:00",
      "code2FA": "794215"
    }
  ]
}
```

Así evitamos definir la misma combinación de método HTTP y ruta en dos controladores con respuestas diferentes.

---

# 36. Verificar MySQL

Podemos ejecutar:

```sql
SELECT * FROM users;
```

Y:

```sql
SELECT * FROM sessions;
```

Obtendríamos conceptualmente:

```text
users

id | name        | email
────────────────────────────────
1  | Juan Pérez  | juan@gmail.com
```

Y:

```text
sessions

id | token                       | expiration          | code_2fa | user_id
────────────────────────────────────────────────────────────────────────────
1  | token-iphone-14-pro         | 2026-08-29 18:30:00 | 381924   | 1
2  | token-computador-personal   | 2026-08-30 10:00:00 | 794215   | 1
```

La columna:

```text
user_id
```

puede repetirse, porque representa el lado `N` de la relación.

---

# 37. ¿Qué pasa si el usuario no tiene sesiones?

Al ejecutar:

```http
GET /api/users/2/sessions
```

la respuesta puede ser:

```json
[]
```

Y al consultar el usuario con el DTO detallado:

```json
{
  "id": 2,
  "name": "Ana López",
  "email": "ana@gmail.com",
  "sessions": []
}
```

Esto es válido porque la relación real permite:

```text
User 1 ───── 0..N Session
```

---

# 38. Evitar recursividad

Las entidades tienen una relación bidireccional:

```text
User
 └── List<Session>
          └── User
               └── List<Session>
                        └── User
                             ...
```

Si devolviéramos directamente las entidades como JSON, podríamos producir serialización recursiva.

Los DTOs cortan el ciclo:

```text
UserSessionsResponseDTO
│
├── id
├── name
├── email
│
└── List<SessionResponseDTO>
     ├── id
     ├── token
     ├── expiration
     └── code2FA
```

`SessionResponseDTO` no contiene un `UserResponseDTO`, por lo que no existe un ciclo.

---

# 39. Arquitectura final

```text
                  GET usuario con sesiones
                           │
                           ▼
                    UserController
                           │
                           ▼
                      UserService
                           │
                           ▼
                    UserRepository
                           │
                    @EntityGraph
                           │
                           ▼
                        MySQL
                           │
                 ┌─────────┴─────────┐
                 ▼                   ▼
               User             List<Session>
                 │                   │
                 └─────────┬─────────┘
                           ▼
                       UserMapper
                           │
                     SessionMapper
                           │
                           ▼
                UserSessionsResponseDTO
                           │
                           ▼
                          JSON
```

---

# 40. Relación JPA final

## `User`

```java
@OneToMany(
    mappedBy = "user",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY
)
private List<Session> sessions = new ArrayList<>();
```

## `Session`

```java
@ManyToOne(
    fetch = FetchType.LAZY,
    optional = false
)
@JoinColumn(
    name = "user_id",
    nullable = false
)
private User user;
```

Conceptualmente:

```text
        User
        ────
        id PK
          │
          │ 1
          │
          │ 0..N
          ▼
       Session
       ───────
       id PK
       user_id FK
```

---

# 41. Qué debe aprender el estudiante

### ¿Qué representa `@OneToMany`?

```text
Una instancia de una entidad
puede relacionarse con muchas
instancias de la otra entidad.
```

### ¿Qué representa `@ManyToOne`?

```text
Muchas instancias de Session
pueden pertenecer
a un mismo User.
```

### ¿Dónde se almacena la llave foránea?

```text
En la tabla sessions,
mediante la columna user_id.
```

### ¿Qué significa `mappedBy = "user"`?

```text
Indica que Session.user
es el lado propietario
de la relación.
```

### ¿Por qué `user_id` no es único?

```text
Porque debe poder repetirse
para asociar muchas sesiones
con el mismo usuario.
```

### ¿Por qué usamos una lista?

```text
Porque User puede contener
cero, una o muchas Session.
```

### ¿Por qué usamos DTOs?

```text
Para controlar los datos
que entran y salen de la API
y evitar ciclos entre entidades.
```

### ¿Por qué usamos `LAZY`?

```text
Para cargar las sesiones
solamente cuando sean necesarias.
```

### ¿Cómo cargamos las sesiones con el usuario?

```java
@EntityGraph(
    attributePaths = {"sessions"}
)
```

### ¿Cómo verificamos que una sesión pertenece a un usuario?

```java
findByIdAndUserId(
    sessionId,
    userId
)
```

---

# 42. Evolución del proyecto

Ahora nuestro proyecto tiene:

```text
              Profile
                 ▲
                 │
                 │ 1 : 0..1
                 │
                User
                 │
                 │ 1 : 0..N
                 ▼
              Session
```

La siguiente evolución puede introducir una relación:

```text
User N ───────── N Role
```

De esta manera tendremos:

```text
              Profile
                 ▲
                 │ 1 : 0..1
                 │
                User ─────── Session
                 │            1 : 0..N
                 │
                 │ N : N
                 ▼
                Role
```

Posteriormente podremos incorporar autenticación, autorización, roles y permisos.
