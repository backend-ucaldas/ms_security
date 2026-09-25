# Guía práctica: Relación 1:1 entre `User` y `Profile`

## 1. Objetivo

Continuaremos el proyecto anterior agregando la entidad:

```text
Profile
──────────────────
id        : Long
phone     : String
birthDate : Date
```

Cada usuario tendrá como máximo un perfil y cada perfil pertenecerá a un solo usuario:

```text
User                   Profile
────                   ───────
id                     id
name                   phone
email                  birthDate
password               user_id
   │                       │
   └──────── 1 : 1 ────────┘
```

La base de datos tendrá:

```text
users
────────────────
id
name
email
password


profiles
────────────────
id
phone
birth_date
user_id  ← UNIQUE FK
```

El requisito principal será:

> Cuando consultemos un usuario individualmente mediante `GET /api/users/{id}`, también se debe devolver su perfil.

---

# 2. ¿Quién será el dueño de la relación?

Haremos que `Profile` sea el dueño de la relación porque allí estará la llave foránea:

```text
profiles.user_id
```

Por tanto:

```java
Profile
@OneToOne
@JoinColumn(...)
```

Y en `User` tendremos:

```java
@OneToOne(mappedBy = "user")
```

Visualmente:

```text
User
│
│ mappedBy = "user"
│
│ 1
│
│ 1
▼
Profile
│
└── user_id FK UNIQUE
```

---

# 3. Estructura del proyecto

Agregaremos:

```text
src/main/java/com/example/users
│
├── controller
│   ├── UserController.java
│   └── ProfileController.java
│
├── service
│   ├── UserService.java
│   └── ProfileService.java
│
├── repository
│   ├── UserRepository.java
│   └── ProfileRepository.java
│
├── entity
│   ├── User.java
│   └── Profile.java
│
├── dto
│   ├── BaseUserDTO.java
│   ├── CreateUserDTO.java
│   ├── UpdateUserDTO.java
│   ├── UserResponseDTO.java
│   ├── UserDetailResponseDTO.java
│   │
│   ├── ProfileRequestDTO.java
│   └── ProfileResponseDTO.java
│
├── mapper
│   ├── UserMapper.java
│   └── ProfileMapper.java
│
└── UsersApplication.java
```

---

# 4. Tipo de dato para `birthDate`

Aunque conceptualmente tenemos:

```text
birthDate : Date
```

en Java moderno recomiendo utilizar:

```java
LocalDate
```

en lugar de:

```java
java.util.Date
```

porque una fecha de nacimiento:

```text
1998-05-21
```

no necesita hora ni zona horaria.

Importaremos:

```java
java.time.LocalDate;
```

Y MySQL la almacenará naturalmente como:

```text
DATE
```

---

# 5. Crear Entity `Profile`

Creamos:

```text
entity/Profile.java
```

```java
package com.uc.ms_security.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            length = 30
    )
    private String phone;

    @Column(
            name = "birth_date",
            nullable = false
    )
    private LocalDate birthDate;

    @OneToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;
}
```

La parte importante es:

```java
@OneToOne
@JoinColumn(
    name = "user_id",
    unique = true
)
private User user;
```

---

# 6. ¿Por qué `unique = true`?

Porque queremos garantizar realmente:

```text
User 1 ───── 1 Profile
```

Sin:

```java
unique = true
```

la base de datos podría permitir:

```text
Profile 1 ── User 5
Profile 2 ── User 5
Profile 3 ── User 5
```

lo cual terminaría siendo:

```text
User 1 : N Profile
```

Con:

```java
unique = true
```

MySQL garantiza que un `user_id` aparezca como máximo una vez en `profiles`.

---

# 7. Modificar Entity `User`

Nuestro `User` actualmente tiene:

```java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    ...
}
```

Agregaremos:

```java
@OneToOne(
    mappedBy = "user",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY
)
private Profile profile;
```

La entidad completa quedaría:

```java
package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
```

---

# 8. Entender `mappedBy`

Tenemos:

```java
mappedBy = "user"
```

Ese `"user"` corresponde exactamente al atributo de `Profile`:

```java
private User user;
```

Es decir:

```text
User.java

private Profile profile;
        │
        │ mappedBy = "user"
        ▼

Profile.java

private User user;
             ▲
             │
        dueño relación
```

---

# 9. Entender `cascade`

Utilizamos:

```java
cascade = CascadeType.ALL
```

porque conceptualmente el perfil depende del usuario.

Por ejemplo:

```text
Eliminar User
    ↓
Eliminar Profile
```

Y:

```java
orphanRemoval = true
```

permite eliminar el perfil si deja de estar asociado al usuario.

Esto tiene sentido si consideramos que:

> Un `Profile` no existe independientemente de un `User`.

---

# 10. DTO del Profile

Para esta práctica podemos utilizar un solo DTO de entrada:

```text
dto/ProfileRequestDTO.java
```

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileRequestDTO {

    @NotBlank(
            message = "El teléfono es obligatorio"
    )
    private String phone;

    @NotNull(
            message = "La fecha de nacimiento es obligatoria"
    )
    @Past(
            message = "La fecha de nacimiento debe estar en el pasado"
    )
    private LocalDate birthDate;
}

```

Aquí introducimos:

```java
@Past
```

para impedir fechas futuras.

Por ejemplo:

```json
{
  "phone": "3001234567",
  "birthDate": "2030-01-01"
}
```

sería inválido.

---

# 11. DTO de respuesta del Profile

Creamos:

```text
dto/ProfileResponseDTO.java
```

```java
package com.uc.ms_security.dto;

import lombok.Value;

import java.time.LocalDate;

@Value
public class ProfileResponseDTO {

    Long id;

    String phone;

    LocalDate birthDate;
}
```

Observa que no agregamos:

```text
user
```

dentro del DTO del perfil.

Eso es importante.

Evitaríamos generar algo como:

```text
User
 ↓
Profile
 ↓
User
 ↓
Profile
 ↓
User
 ...
```

---

# 12. Crear un DTO especial para ver un usuario

Nuestro DTO actual:

```java
UserResponseDTO
```

puede continuar siendo:

```text
id
name
email
```

Lo utilizaremos para los listados.

Creamos otro:

```text
UserDetailResponseDTO
```

para consultar un usuario individual.

```java
package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class UserDetailResponseDTO {

    Long id;

    String name;

    String email;

    ProfileResponseDTO profile;
}
```

Así tenemos:

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
GET /api/users/1

UserDetailResponseDTO
─────────────────────
id
name
email
profile
   ├── id
   ├── phone
   └── birthDate
```

---

# 13. Ejemplo de respuesta

```json
{
  "id": 1,
  "name": "Juan Pérez",
  "email": "juan@gmail.com",
  "profile": {
    "id": 3,
    "phone": "3001234567",
    "birthDate": "1995-08-21"
  }
}
```

Esto cumple nuestro requisito:

> Consultar un solo usuario también carga su información de perfil.

---

# 14. Crear `ProfileMapper`

Creamos:

```text
mapper/ProfileMapper.java
```

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.ProfileRequestDTO;
import com.uc.ms_security.dto.ProfileResponseDTO;
import com.uc.ms_security.entity.Profile;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public Profile toEntity(
            ProfileRequestDTO dto) {

        Profile profile =
                new Profile();

        profile.setPhone(
                dto.getPhone()
        );

        profile.setBirthDate(
                dto.getBirthDate()
        );

        return profile;
    }

    public void updateEntity(
            ProfileRequestDTO dto,
            Profile profile) {

        profile.setPhone(
                dto.getPhone()
        );

        profile.setBirthDate(
                dto.getBirthDate()
        );
    }

    public ProfileResponseDTO toResponseDTO(
            Profile profile) {

        if (profile == null) {
            return null;
        }

        return new ProfileResponseDTO(
                profile.getId(),
                profile.getPhone(),
                profile.getBirthDate()
        );
    }
}
```

El:

```java
if (profile == null)
```

es importante porque inicialmente un usuario puede no tener perfil.

---

# 15. Modificar `UserMapper`

Nuestro mapper anteriormente convertía:

```text
User
 ↓
UserResponseDTO
```

Ahora también debe poder convertir:

```text
User
 ↓
UserDetailResponseDTO
```

Agregamos `ProfileMapper`.

```java
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ProfileMapper profileMapper;

    ...
}
```

Una versión completa quedaría:

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.CreateUserDTO;
import com.uc.ms_security.dto.UpdateUserDTO;
import com.uc.ms_security.dto.UserDetailResponseDTO;
import com.uc.ms_security.dto.UserResponseDTO;
import com.uc.ms_security.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ProfileMapper profileMapper;

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

    public List<UserResponseDTO> toResponseDTOList(List<User> users) {
        return users.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
```

---

# 16. Crear `ProfileRepository`

Creamos:

```text
repository/ProfileRepository.java
```

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
```

Esto nos permitirá buscar:

```text
Profile
WHERE user.id = ?
```

simplemente utilizando:

```java
findByUserId(...)
```

---

# 17. El problema del Lazy Loading

Configuramos la relación:

```java
fetch = FetchType.LAZY
```

Esto significa:

> No cargar automáticamente el perfil cada vez que se cargue un usuario.

Esto es conveniente para:

```http
GET /api/users
```

porque probablemente no queremos cargar todos los perfiles cuando solamente estamos haciendo un listado.

Queremos algo así:

```text
GET /api/users
         ↓
users
solamente
```

Pero:

```text
GET /api/users/{id}
         ↓
User + Profile
```

---

# 18. Cargar el perfil al consultar un usuario

Para garantizarlo podemos crear un método especial en:

```text
UserRepository
```

Utilizaremos:

```java
@EntityGraph
```

Actualizamos:

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = {"profile"})
    Optional<User> findWithProfileById(
            Long id
    );
}

```

Ahora tenemos dos maneras de buscar:

```java
findById(id)
```

y:

```java
findWithProfileById(id)
```

Conceptualmente:

```text
findById()
     ↓
User


findWithProfileById()
     ↓
User
 │
 └── Profile
```

---

# 19. ¿Por qué no poner `EAGER`?

Podríamos hacer:

```java
@OneToOne(
    fetch = FetchType.EAGER
)
```

Pero eso implicaría cargar el perfil siempre.

Por ejemplo:

```text
GET /api/users
 ↓

User 1 + Profile 1
User 2 + Profile 2
User 3 + Profile 3
User 4 + Profile 4
...
```

Aunque solo queramos mostrar:

```text
id
name
email
```

Por eso preferimos:

```java
LAZY
```

y cargar explícitamente el perfil cuando realmente lo necesitamos.

---

# 20. Modificar el Service para consultar usuario

Anteriormente teníamos:

```java
public UserResponseDTO findById(
        Long id) {

    return userMapper
        .toResponseDTO(
            findUser(id)
        );
}
```

Ahora agregaremos el tipo de retorno:

```java
public UserDetailResponseDTO findByIdAndProfile(Long id) {
        User user =userRepository
                        .findWithProfileById(id)
                        .orElseThrow(
                                () -> new ApplicationException(
                                    ErrorCase.NOT_FOUND,
                                    "Usuario no encontrado con id: " + id
                                )
                        );

        return userMapper.toDetailResponseDTO(user);
}
```

Ahora:

```text
Repository
   ↓
User + Profile
   ↓
Mapper
   ↓
UserDetailResponseDTO
```

---

# 21. Modificar `UserController`

Nuestro endpoint:

```java
@GetMapping("/{id}")
```

ahora agregaremos :

```java
@GetMapping("/{id}/profile")
    public UserDetailResponseDTO findByIdAndProfile(
            @PathVariable Long id) {

        return userService.findByIdAndProfile(id);
}
```

Así:

```http
GET /api/users/1/profile
```

devuelve:

```json
{
  "id": 1,
  "name": "Juan Pérez",
  "email": "juan@gmail.com",
  "profile": {
    "id": 1,
    "phone": "3001234567",
    "birthDate": "1995-05-20"
  }
}
```

Mientras:

```http
GET /api/users
```

puede seguir devolviendo:

```json
[
  {
    "id": 1,
    "name": "Juan Pérez",
    "email": "juan@gmail.com"
  },
  {
    "id": 2,
    "name": "Ana López",
    "email": "ana@gmail.com"
  }
]
```

---

# 22. Crear `ProfileService`

Ahora necesitamos poder crear el perfil para un usuario.

Creamos:

```text
service/ProfileService.java
```

```java
package com.uc.ms_security.service;

import com.uc.ms_security.dto.ProfileRequestDTO;
import com.uc.ms_security.dto.ProfileResponseDTO;
import com.uc.ms_security.entity.Profile;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.ProfileMapper;
import com.uc.ms_security.repository.ProfileRepository;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;

    public ProfileResponseDTO create(Long userId, ProfileRequestDTO dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApplicationException(
                ErrorCase.NOT_FOUND,
                "Usuario no encontrado con id: " + userId
                ));

        if (profileRepository.existsByUserId(userId)) {
            throw new ApplicationException(
                ErrorCase.ALREADY_EXISTS,
                    "El usuario ya tiene un perfil"
            );
        }

        Profile profile = profileMapper.toEntity(dto);
        profile.setUser(user);

        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toResponseDTO(savedProfile);
    }

    public ProfileResponseDTO update(Long userId, ProfileRequestDTO dto) {
        Profile profile = findProfile(userId);

        profileMapper.updateEntity(dto, profile);
        Profile updatedProfile = profileRepository.save(profile);

        return profileMapper.toResponseDTO(updatedProfile);
    }

    public ProfileResponseDTO findByUserId(Long userId) {
        return profileMapper.toResponseDTO(findProfile(userId));
    }

    public void delete(Long userId) {
        profileRepository.delete(findProfile(userId));
    }

    private Profile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> new ApplicationException(
                ErrorCase.NOT_FOUND,
                "Perfil no encontrado para el usuario con id: " + userId
                ));
    }
}
```

---

# 23. Crear `ProfileController`

Creamos:

```text
controller/ProfileController.java
```

Como el Profile pertenece al User, utilizaremos rutas anidadas:

```text
/api/users/{userId}/profile
```

Controller:

```java
package com.uc.ms_security.controller;

import com.uc.ms_security.dto.ProfileRequestDTO;
import com.uc.ms_security.dto.ProfileResponseDTO;
import com.uc.ms_security.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponseDTO create(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileRequestDTO dto) {
        return profileService.create(userId, dto);
    }
       

    @PutMapping
    public ProfileResponseDTO update(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileRequestDTO dto) {
        return profileService.update(userId, dto);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        profileService.delete(userId);
    }
}

```

---

# 30. Endpoints resultantes

Ahora tenemos:

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

Esta estructura REST representa muy bien que:

```text
Profile
```

es un recurso dependiente de:

```text
User
```

---

# 31. Crear un usuario

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

# 32. Crear su Profile

Ahora:

```http
POST /api/users/1/profile
```

```json
{
  "phone": "3001234567",
  "birthDate": "1995-05-20"
}
```

Respuesta:

```json
{
  "id": 1,
  "phone": "3001234567",
  "birthDate": "1995-05-20"
}
```

---

# 33. Verificar MySQL

Podemos ejecutar:

```sql
SELECT * FROM users;
```

Y:

```sql
SELECT * FROM profiles;
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
profiles

id | phone      | birth_date | user_id
────────────────────────────────────────
1  | 3001234567 | 1995-05-20 | 1
```

---

# 34. Consultar un usuario con su perfil

Ahora:

```http
GET /api/users/1
```

El recorrido será:

```text
UserController
      ↓
UserService
      ↓
findWithProfileById()
      ↓
UserRepository
      ↓
MySQL
      ↓
User + Profile
      ↓
UserMapper
      ↓
UserDetailResponseDTO
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
  }
}
```

---

# 35. ¿Qué pasa si el usuario todavía no tiene Profile?

Por ejemplo:

```http
GET /api/users/2
```

puede responder:

```json
{
  "id": 2,
  "name": "Ana López",
  "email": "ana@gmail.com",
  "profile": null
}
```

Esto es perfectamente válido si hemos decidido que:

```text
User
puede existir
sin Profile
```

Matemáticamente nuestra relación sería más exactamente:

```text
User 1 ───── 0..1 Profile
```

Es decir:

> Un User puede tener cero o un Profile.

Mientras que:

> Todo Profile debe pertenecer exactamente a un User.

---

# 36. Evitar recursividad

Hay una razón importante por la cual estamos utilizando DTOs.

Las entidades tienen:

```text
User
 └── Profile
      └── User
           └── Profile
                └── User
```

Si devolviéramos directamente las Entities como JSON podríamos tener problemas de serialización recursiva.

Pero nuestros DTOs cortan la relación:

```text
UserDetailResponseDTO
│
├── id
├── name
├── email
│
└── ProfileResponseDTO
     ├── id
     ├── phone
     └── birthDate
```

No existe:

```text
ProfileResponseDTO
└── UserResponseDTO
```

Por eso no tenemos un ciclo.

---

# 37. Arquitectura final

```text
                        GET /api/users/1
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
                   User               Profile
                     │                   │
                     └─────────┬─────────┘
                               ▼
                          UserMapper
                               │
                        ProfileMapper
                               │
                               ▼
                   UserDetailResponseDTO
                               │
                               ▼
                              JSON
```

---

# 38. Relación JPA final

La relación queda:

## `User`

```java
@OneToOne(
    mappedBy = "user",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY
)
private Profile profile;
```

## `Profile`

```java
@OneToOne(
    fetch = FetchType.LAZY
)
@JoinColumn(
    name = "user_id",
    nullable = false,
    unique = true
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
          │ 0..1
          ▼
       Profile
       ───────
       id PK
       user_id FK UNIQUE
```

---

# 39. Qué debe aprender el estudiante

Al terminar esta práctica debe poder responder:

### ¿Qué representa `@OneToOne`?

```text
Una entidad se relaciona
con máximo una instancia
de la otra entidad.
```

### ¿Qué hace `@JoinColumn`?

```text
Define la columna
que contiene la FK.
```

### ¿Qué significa `mappedBy`?

```text
Indica que la otra entidad
es la propietaria de la relación.
```

### ¿Por qué `unique = true`?

```text
Garantiza la cardinalidad 1:1
en la base de datos.
```

### ¿Por qué usamos DTO?

```text
Para controlar qué datos
entran y salen de la API
y evitar ciclos entre entidades.
```

### ¿Por qué usamos Mapper?

```text
Para mantener separada
la conversión DTO ↔ Entity.
```

### ¿Por qué usamos `LAZY`?

```text
Para cargar la relación
solamente cuando sea necesaria.
```

### ¿Cómo cargamos Profile al consultar un usuario?

```text
@EntityGraph
attributePaths = {"profile"}
```

---

# 40. Evolución del proyecto

Ahora nuestro proyecto ya tiene:

```text
User
 │
 │ 1 : 0..1
 ▼
Profile
```

La siguiente evolución puede introducir una relación:

```text
User
 │
 │ N : N
 ▼
Role
```

De esta manera tendremos:

```text
              Profile
                 ▲
                 │
                 │ 1 : 1
                 │
                User
                 │
                 │ N : N
                 ▼
                Role
```

Y posteriormente:

```text
Role
 │
 │ N : N
 ▼
Permission
```

Esto permitirá que el mismo proyecto evolucione progresivamente desde relaciones JPA hasta autenticación y autorización.
