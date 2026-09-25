# Guía práctica: CRUD de User con Spring Boot + Lombok + MySQL

## 1. Objetivo

Construir una API REST para gestionar usuarios utilizando:

* Spring Boot
* Spring Web
* Spring Data JPA
* MySQL
* Jakarta Validation
* Lombok
* Entity
* Repository
* Service
* Controller
* DTOs con herencia
* Mapper

La entidad será:

```text
User
────────────────
id       : Long
name     : String
email    : String
password : String
```

La API tendrá:

```http
POST    /api/users
GET     /api/users
GET     /api/users/{id}
PUT     /api/users/{id}
DELETE  /api/users/{id}
```

---

# 2. Arquitectura

```text
                    REQUEST
                       │
                       ▼
              ┌─────────────────┐
              │   Controller    │
              └────────┬────────┘
                       │
                       ▼
                    DTOs
                       │
                       ▼
              ┌─────────────────┐
              │     Service     │
              └────────┬────────┘
                       │
                 ┌─────┴─────┐
                 ▼           ▼
              Mapper      Repository
                 │           │
                 ▼           │
              Entity ◄───────┘
                 │
                 ▼
                MySQL
```

Responsabilidades:

| Componente             | Responsabilidad                   |
| ---------------------- | --------------------------------- |
| **Entity**             | Representar los datos almacenados |
| **DTO**                | Definir datos de entrada y salida |
| **Mapper**             | Convertir DTO ↔ Entity            |
| **Repository**         | Acceder a la base de datos        |
| **Service**            | Implementar lógica de negocio     |
| **Controller**         | Exponer endpoints HTTP            |
| **Jakarta Validation** | Validar datos recibidos           |

---

# 3. Dependencias

Desde Spring Initializr seleccionamos:

```text
Spring Web
Spring Data JPA
Validation
MySQL Driver
Lombok
```

En Maven tendremos dependencias equivalentes a:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

# 4. Estructura del proyecto

```text
src/main/java/com/example/users
│
├── controller
│   └── UserController.java
│
├── service
│   └── UserService.java
│
├── repository
│   └── UserRepository.java
│
├── entity
│   └── User.java
│
├── dto
│   ├── BaseUserDTO.java
│   ├── CreateUserDTO.java
│   ├── UpdateUserDTO.java
│   └── UserResponseDTO.java
│
├── mapper
│   └── UserMapper.java
│
└── UsersApplication.java
```

---

# 5. Crear la base de datos MySQL

```sql
CREATE DATABASE security_ms_db;
```

Podemos verificar:

```sql
SHOW DATABASES;
```

No necesitamos crear manualmente la tabla `users` durante esta práctica.

---

# 6. Configurar MySQL

En:

```text
src/main/resources/application.properties
```

agregamos:

```properties
spring.application.name=security_ms
server.port=8181
spring.datasource.url=jdbc:mysql://localhost:3306/security_ms_db
spring.datasource.username=my_user
spring.datasource.password=my_password

spring.jpa.hibernate.ddl-auto=update

spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Para desarrollo utilizaremos:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Más adelante, en proyectos reales, puede sustituirse por migraciones con Flyway o Liquibase.

---

# 7. Entity `User`

Creamos:

```text
entity/User.java
```

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
}
```

Gracias a Lombok no necesitamos escribir:

```java
getId()
setId()

getName()
setName()

getEmail()
setEmail()

getPassword()
setPassword()
```

Lombok los genera durante la compilación.

Utilizamos:

```java
@Getter
@Setter
@NoArgsConstructor
```

La tabla será aproximadamente:

```text
users
─────────────────────────────
id          BIGINT      PK AI
name        VARCHAR
email       VARCHAR     UNIQUE
password    VARCHAR
```

---

# 8. Diseño de DTOs

Queremos utilizar herencia:

```text
                  BaseUserDTO
                  ───────────
                  name
                  email
                      ▲
               ┌──────┴───────┐
               │              │
       CreateUserDTO     UpdateUserDTO
       ─────────────     ─────────────
       password          password
       obligatorio       opcional
```

Además tendremos:

```text
UserResponseDTO
───────────────
id
name
email
```

---

# 9. `BaseUserDTO`

```text
dto/BaseUserDTO.java
```

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseUserDTO {

    @NotBlank(
            message = "El nombre es obligatorio"
    )
    @Size(
            min = 2,
            max = 100,
            message = "El nombre debe tener entre 2 y 100 caracteres"
    )
    private String name;

    @NotBlank(
            message = "El email es obligatorio"
    )
    @Email(
            message = "El email no tiene un formato válido"
    )
    private String email;
}

```

Aquí centralizamos:

```text
name
├── @NotBlank
└── @Size

email
├── @NotBlank
└── @Email
```

---

# 10. `CreateUserDTO`

```text
dto/CreateUserDTO.java
```

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserDTO
        extends BaseUserDTO {

    @NotBlank(
            message = "La contraseña es obligatoria"
    )
    @Size(
            min = 8,
            message = "La contraseña debe tener mínimo 8 caracteres"
    )
    private String password;
}
```

Entonces:

```text
CreateUserDTO
│
├── name          heredado
├── email         heredado
│
└── password
    ├── @NotBlank
    └── @Size(min = 8)
```

---

# 11. `UpdateUserDTO`

En actualización la contraseña será opcional.

```text
dto/UpdateUserDTO.java
```

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDTO extends BaseUserDTO {

    @Size(  min = 8,
            message = "La contraseña debe tener mínimo 8 caracteres"
    )
    private String password;
}
```

No usamos:

```java
@NotBlank
```

Por lo tanto, podemos actualizar:

```json
{
  "name": "Juan Pérez",
  "email": "juan@gmail.com"
}
```

sin enviar contraseña.

---

# 12. `UserResponseDTO`

El DTO de respuesta nunca debe devolver la contraseña.

Podemos aprovechar Lombok con `@Value`:

```java
package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class UserResponseDTO {
    Long id;
    String name;
    String email;
}
```

`@Value` es útil para DTOs de respuesta porque genera un objeto inmutable.

Conceptualmente:

```text
REQUEST

name
email
password

      ↓

RESPONSE

id
name
email
```

Nunca:

```text
password
```

---

# 13. Crear el Mapper

Ahora agregamos una capa específica para transformar objetos.

```text
mapper/UserMapper.java
```

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.CreateUserDTO;
import com.uc.ms_security.dto.UpdateUserDTO;
import com.uc.ms_security.dto.UserResponseDTO;
import com.uc.ms_security.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

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

    public List<UserResponseDTO> toResponseDTOList(List<User> users) {
        return users.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
```

El Mapper tendrá tres responsabilidades:

```text
CreateUserDTO
      │
      ▼
     User


UpdateUserDTO + User
      │
      ▼
User actualizado


User
 │
 ▼
UserResponseDTO
```

---

# 14. ¿Por qué utilizar Mapper?

Sin Mapper, el Service tendría que hacer:

```java
User user = new User();

user.setName(dto.getName());
user.setEmail(dto.getEmail());
user.setPassword(dto.getPassword());
```

Además de encargarse de:

```text
validar email
buscar usuario
guardar usuario
verificar reglas
```

Con Mapper:

```java
User user =
    userMapper.toEntity(dto);
```

Por tanto:

```text
Service
    ↓
Lógica de negocio

Mapper
    ↓
Transformación de objetos
```

Tenemos una mejor separación de responsabilidades.

---

# 15. Repository

```text
repository/UserRepository.java
```

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}

```

Al extender:

```java
JpaRepository<User, Long>
```

obtenemos operaciones como:

```text
save()
findAll()
findById()
delete()
existsById()
```

---

# 16. Crear el Service

```text
service/UserService.java
```

```java
package com.uc.ms_security.service;

import com.uc.ms_security.dto.CreateUserDTO;
import com.uc.ms_security.dto.UpdateUserDTO;
import com.uc.ms_security.dto.UserResponseDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.mapper.UserMapper;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    public UserResponseDTO create(CreateUserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario con este email"
            );
        }
        User user = userMapper.toEntity(dto);
        User savedUser = userRepository.save(user);
        return userMapper.toResponseDTO(savedUser);
    }
    public List<UserResponseDTO> findAll() {
        List<User> users =userRepository.findAll();
        return userMapper.toResponseDTOList(users);
    }
    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));
    }

    public UserResponseDTO findById(Long id) {
        User user = findUser(id);
        return userMapper.toResponseDTO(user);
    }

    public UserResponseDTO update(Long id, UpdateUserDTO dto) {
        User user = findUser(id);
        if (userRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El email pertenece a otro usuario"
            );
        }
        userMapper.updateEntity(dto, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
    }
    public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
    }
}
```

Aquí también aprovechamos Lombok:

```java
@RequiredArgsConstructor
```

Esto genera automáticamente el constructor necesario para:

```java
private final UserRepository userRepository;

private final UserMapper userMapper;
```

Por tanto ya no necesitamos escribir:

```java
public UserService(
        UserRepository userRepository,
        UserMapper userMapper) {

    this.userRepository = userRepository;
    this.userMapper = userMapper;
}
```

---

# 17. Crear usuario

```java
public UserResponseDTO create(CreateUserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario con este email"
            );
        }
        User user = userMapper.toEntity(dto);
        User savedUser = userRepository.save(user);
        return userMapper.toResponseDTO(savedUser);
}
```

Ahora el Service está mucho más limpio.

```text
DTO
 ↓
Mapper
 ↓
Entity
 ↓
Repository
 ↓
MySQL
```

---

# 18. Listar usuarios

```java
public List<UserResponseDTO> findAll() {
        List<User> users =userRepository.findAll();
        return userMapper.toResponseDTOList(users);
}
```
---
# 19. Buscar internamente un usuario

Crearemos un método reutilizable:

```java
private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));
    }
```
---

# 20. Consultar usuario

```java
public UserResponseDTO findById(Long id) {
        User user = findUser(id);
        return userMapper.toResponseDTO(user);
}
```

---

# 21. Actualizar usuario

```java
public UserResponseDTO update(Long id, UpdateUserDTO dto) {
        User user = findUser(id);
        if (userRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El email pertenece a otro usuario"
            );
        }
        userMapper.updateEntity(dto, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
}
```

El Mapper se encarga internamente de:

```text
name
    ↓
actualizar

email
    ↓
actualizar

password
    ↓
¿vino?
   /   \
 sí     no
 │       │
 ▼       ▼
cambiar conservar
```

---

# 22. Eliminar usuario

```java
public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
    }
```

---

# 23. Controller

También podemos aprovechar Lombok para la inyección de dependencias.

```java
package com.uc.ms_security.controller;

import com.uc.ms_security.dto.CreateUserDTO;
import com.uc.ms_security.dto.UpdateUserDTO;
import com.uc.ms_security.dto.UserResponseDTO;
import com.uc.ms_security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO create(@Valid @RequestBody CreateUserDTO dto) {
        return userService.create(dto);
    }

    @GetMapping
    public List<UserResponseDTO> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponseDTO findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}")
    public UserResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDTO dto) {
        return userService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

---

# 24. Probar CREATE

```http
POST /api/users
Content-Type: application/json
```

```json
{
  "name": "Juan Pérez",
  "email": "juan@gmail.com",
  "password": "12345678"
}
```

Recorrido:

```text
JSON
 │
 ▼
CreateUserDTO
 │
 │ @Valid
 ▼
Controller
 │
 ▼
Service
 │
 ▼
Mapper
 │
 ▼
User Entity
 │
 ▼
Repository
 │
 ▼
MySQL
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

# 25. Probar UPDATE sin contraseña

```http
PUT /api/users/1
```

```json
{
  "name": "Juan David Pérez",
  "email": "juan@gmail.com"
}
```

El Mapper ejecutará:

```java
if (dto.getPassword() != null) {
    user.setPassword(
        dto.getPassword()
    );
}
```

Como no llegó `password`, conserva el existente.

---

# 26. Probar UPDATE con contraseña

```json
{
  "name": "Juan David Pérez",
  "email": "juan@gmail.com",
  "password": "87654321"
}
```

En este caso sí se modifica.



---

# 27. Responsabilidad de cada capa

Al terminar la práctica, la regla mental debería ser:

```text
Controller
──────────
Recibe HTTP.


DTO
──────────
Define qué entra
y qué sale.


Validation
──────────
Comprueba que
los datos sean válidos.


Service
──────────
Toma decisiones
de negocio.


Mapper
──────────
Convierte objetos.


Repository
──────────
Consulta y guarda.


Entity
──────────
Representa los datos.


MySQL
──────────
Persiste los datos.
```