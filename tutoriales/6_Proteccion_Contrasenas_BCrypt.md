# Guía práctica: Protección de contraseñas con BCrypt en Spring Boot

## 1. Objetivo

Continuaremos el CRUD de `User` para dejar de almacenar contraseñas en texto plano.

Actualmente podríamos tener:

```text
password
────────────
12345678
```

Después de aplicar BCrypt almacenaremos un hash parecido a:

```text
$2a$12$9QwB8oY4R6N7mS5M2x3C2u...
```

En esta guía implementaremos:

* Instalación de BCrypt con Maven.
* Configuración de `PasswordEncoder`.
* Hash de la contraseña al crear un usuario.
* Hash de la contraseña al actualizarla.
* Conservación del hash anterior cuando no se envía una contraseña nueva.
* Validación de contraseñas mediante `matches()`.
* DTOs, mapper, service y controller.
* Pruebas mediante peticiones HTTP y MySQL.


---

# 2. ¿Qué hace BCrypt?

BCrypt es una función de hash diseñada para proteger contraseñas.

```text
Contraseña original
        ↓
      BCrypt
        ↓
Hash almacenado
```

No existe una operación para descifrar el hash y recuperar la contraseña.

Por eso no debemos decir:

> La contraseña se cifra con BCrypt.

Es más preciso decir:

> La contraseña se protege mediante un hash BCrypt.

---

# 3. Diferencia entre texto plano y hash

## Forma incorrecta

```text
users
────────────────────────────
id | email        | password
1  | ana@mail.com | 12345678
```

Si alguien obtiene acceso a la base de datos, puede leer inmediatamente la contraseña.

## Forma correcta

```text
users
──────────────────────────────────────────────────────────
id | email        | password
1  | ana@mail.com | $2a$12$9QwB8oY4R6N7mS5M2x3C2u...
```

La base de datos conserva únicamente el hash.

---

# 4. Flujo que construiremos

```text
POST /api/users
       │
       ▼
CreateUserDTO
       │
       ▼
UserService
       │
       ├── valida el correo
       │
       ├── passwordEncoder.encode(password)
       │
       ▼
UserMapper
       │
       ▼
User con contraseña hasheada
       │
       ▼
UserRepository
       │
       ▼
MySQL
```

La respuesta nunca incluirá la contraseña:

```text
User
 │
 ▼
UserResponseDTO
────────────────
id
name
email
```

---

# 5. Instalar Spring Security con Maven

Aunque en esta guía solo utilizaremos BCrypt, posteriormente construiremos la
autenticación y la autorización. Por eso instalaremos desde ahora el starter
completo de Spring Security:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Esta dependencia ya incluye las clases necesarias para utilizar:

```text
PasswordEncoder
BCryptPasswordEncoder
SecurityFilterChain
Authentication
AuthorizationManager
```

Si el proyecto utiliza la gestión de dependencias de Spring Boot, no necesitamos
indicar manualmente la versión.

La sección puede quedar:

```xml
<dependencies>

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
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
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

</dependencies>
```

Después de modificar el archivo podemos recargar Maven desde el IDE o ejecutar:

```bash
mvn clean compile
```

---

# 6. ¿Qué ocurre al instalar el starter?

Si instalamos `spring-boot-starter-security` y no creamos una configuración,
Spring Boot protege automáticamente las peticiones.

Al intentar consultar:

```http
GET /api/users
```

podríamos recibir:

```http
401 Unauthorized
```

También puede aparecer en la consola una contraseña generada para un usuario
temporal llamado `user`.

Todavía no queremos autenticar usuarios. En este incremento únicamente
protegeremos las contraseñas con BCrypt.

Por ello crearemos una configuración transitoria:

```text
Todos los endpoints → permitAll()
```

Esta configuración permite continuar probando los CRUD mientras dejamos
instalada la infraestructura que utilizaremos después.

---

# 7. Crear la configuración inicial de seguridad

Agregaremos un nuevo paquete:

```text
config
```

La estructura será:

```text
src/main/java/com/uc/ms_security
│
├── config
│   └── SecurityConfig.java
│
├── controller
├── dto
├── entity
├── mapper
├── repository
└── service
```

Creamos:

```text
config/SecurityConfig.java
```

```java
package com.uc.ms_security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
                .authorizeHttpRequests(auth -> auth
                        .anyRequest()
                        .permitAll()
                );

        return http.build();
    }
}
```

Esta clase tiene dos responsabilidades temporales:

```text
PasswordEncoder
    ↓
Generar hashes BCrypt

SecurityFilterChain
    ↓
Permitir los endpoints mientras aún no existe autenticación
```

> `permitAll()` es transitorio. Debe reemplazarse cuando implementemos el
> login, JWT y la autorización.

Deshabilitamos CSRF porque estamos construyendo una API REST que posteriormente
utilizará un token enviado en encabezados, no autenticación basada en formularios
y cookies. Esta decisión deberá revisarse si la aplicación usa cookies para
autenticarse.

---

# 8. Entender el factor de costo

Utilizamos:

```java
new BCryptPasswordEncoder(12)
```

El valor `12` es el factor de costo.

```text
Costo mayor
    ↓
Más trabajo para generar y comprobar el hash
    ↓
Mayor dificultad para ataques masivos
```

También aumenta el tiempo consumido por el servidor.

Para una práctica podemos utilizar:

```text
10 o 12
```

En esta guía utilizaremos `12`.

---

# 9. ¿Por qué creamos un Bean?

La anotación:

```java
@Bean
```

registra el codificador en el contenedor de Spring.

Así podemos inyectarlo:

```java
private final PasswordEncoder passwordEncoder;
```

Gracias a Lombok:

```java
@RequiredArgsConstructor
```

Spring lo recibe mediante el constructor.

Además, dependemos de la interfaz:

```java
PasswordEncoder
```

en lugar de depender directamente de:

```java
BCryptPasswordEncoder
```

Esto aplica inversión de dependencias y permite cambiar la implementación en el futuro.

---

# 10. Verificar la columna `password`

La entidad `User` puede conservar:

```java
@Column(
        nullable = false,
        length = 255
)
private String password;
```

Una longitud de `255` es suficiente para almacenar el hash.

No debemos utilizar una longitud como:

```java
length = 20
```

porque el hash sería truncado o rechazado por la base de datos.

---

# 11. Mantener los DTO existentes

Conservaremos la herencia:

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
       obligatoria       opcional
```

El cliente continúa enviando la contraseña original mediante el DTO.

El hash se genera internamente y nunca lo debe producir el cliente.

---

# 12. Revisar `CreateUserDTO`

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserDTO extends BaseUserDTO {

    @NotBlank(
            message = "La contraseña es obligatoria"
    )
    @Size(
            min = 8,
            max = 72,
            message = "La contraseña debe tener entre 8 y 72 caracteres"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])\\S{8,72}$",
            message = "La contraseña debe contener al menos una mayúscula, "
                    + "una minúscula, un número y un carácter especial, "
                    + "sin espacios"
    )
    private String password;
}
```

En esta práctica limitamos la contraseña a 72 caracteres por la restricción de entrada de BCrypt.

> Para caracteres Unicode, la restricción real depende de los bytes codificados, no únicamente del número de caracteres.

La contraseña debe cumplir:

| Regla | Ejemplo |
| --- | --- |
| Entre 8 y 72 caracteres | `Clave123!` |
| Al menos una mayúscula | `C` |
| Al menos una minúscula | `lave` |
| Al menos un número | `123` |
| Al menos un carácter especial | `!` |
| No contener espacios | `Clave 123!` no es válida |

La expresión regular significa:

```text
(?=.*[a-z])       al menos una minúscula
(?=.*[A-Z])       al menos una mayúscula
(?=.*\d)          al menos un número
(?=.*[@$!%*?&])   al menos uno de estos caracteres especiales
\S{8,72}          entre 8 y 72 caracteres, sin espacios
```

---

# 13. Revisar `UpdateUserDTO`

La contraseña seguirá siendo opcional:

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDTO extends BaseUserDTO {

    @Size(
            min = 8,
            max = 72,
            message = "La contraseña debe tener entre 8 y 72 caracteres"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])\\S{8,72}$",
            message = "La contraseña debe contener al menos una mayúscula, "
                    + "una minúscula, un número y un carácter especial, "
                    + "sin espacios"
    )
    private String password;
}
```

No agregamos:

```java
@NotBlank
```

porque se debe poder actualizar el nombre o el correo sin cambiar la contraseña.

Las anotaciones `@Size` y `@Pattern` no rechazan un valor `null`. Por eso la
contraseña continúa siendo opcional en la actualización, pero si se envía debe
cumplir todas las reglas.

---

# 14. Mantener `UserResponseDTO`

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

No agregamos:

```text
password
```

ni tampoco:

```text
passwordHash
```

El hash tampoco debe exponerse en las respuestas.

---

# 15. ¿Dónde debe aplicarse BCrypt?

Tenemos dos responsabilidades diferentes:

```text
Mapper
    ↓
Transformar DTO ↔ Entity

Service
    ↓
Aplicar reglas del negocio y seguridad
```

El mapper no debería decidir:

```text
qué algoritmo utilizar
qué costo utilizar
cuándo cambiar una contraseña
```

Por eso el `Service` ejecutará:

```java
passwordEncoder.encode(dto.getPassword())
```

y enviará el resultado al mapper.

---

# 16. Modificar `UserMapper`

Actualizamos:

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

    public User toEntity(
            CreateUserDTO dto,
            String encodedPassword) {

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(encodedPassword);

        return user;
    }

    public void updateBasicData(
            UpdateUserDTO dto,
            User user) {

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
    }

    public UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public List<UserResponseDTO> toResponseDTOList(
            List<User> users) {

        return users.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
```

---

# 17. ¿Por qué el mapper recibe `encodedPassword`?

El mapper recibe:

```java
String encodedPassword
```

pero no conoce cómo fue generado.

```text
UserService
    │
    ├── BCrypt
    │
    ▼
encodedPassword
    │
    ▼
UserMapper
```

Así:

* El servicio controla la seguridad.
* El mapper se limita a asignar valores.
* La entidad nunca se guarda con la contraseña original.

---

# 18. Modificar `UserService`

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO create(CreateUserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya está registrado"
            );
        }

        String encodedPassword = passwordEncoder.encode(
                dto.getPassword()
        );

        User user = userMapper.toEntity(
                dto,
                encodedPassword
        );

        User savedUser = userRepository.save(user);

        return userMapper.toResponseDTO(savedUser);
    }

    public List<UserResponseDTO> findAll() {
        return userMapper.toResponseDTOList(
                userRepository.findAll()
        );
    }

    public UserResponseDTO findById(Long id) {
        return userMapper.toResponseDTO(
                findEntityById(id)
        );
    }

    public UserResponseDTO update(
            Long id,
            UpdateUserDTO dto) {

        User user = findEntityById(id);

        if (userRepository.existsByEmailAndIdNot(
                dto.getEmail(),
                id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya está registrado"
            );
        }

        userMapper.updateBasicData(dto, user);

        if (dto.getPassword() != null
                && !dto.getPassword().isBlank()) {

            String encodedPassword = passwordEncoder.encode(
                    dto.getPassword()
            );

            user.setPassword(encodedPassword);
        }

        User updatedUser = userRepository.save(user);

        return userMapper.toResponseDTO(updatedUser);
    }

    public void delete(Long id) {
        User user = findEntityById(id);
        userRepository.delete(user);
    }

    private User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        )
                );
    }
}
```

---

# 19. Crear un usuario de forma segura

La parte principal es:

```java
String encodedPassword = passwordEncoder.encode(
        dto.getPassword()
);
```

Después:

```java
User user = userMapper.toEntity(
        dto,
        encodedPassword
);
```

El recorrido es:

```text
"12345678"
      │
      ▼
PasswordEncoder.encode()
      │
      ▼
"$2a$12$..."
      │
      ▼
User.password
      │
      ▼
MySQL
```

---

# 20. Actualizar sin cambiar la contraseña

Petición:

```http
PUT /api/users/1
Content-Type: application/json
```

```json
{
  "name": "Ana Gómez",
  "email": "ana.gomez@mail.com"
}
```

Como `password` no fue enviado:

```java
if (dto.getPassword() != null
        && !dto.getPassword().isBlank()) {
    // no entra
}
```

El hash anterior permanece sin cambios.

---

# 21. Actualizar cambiando la contraseña

```http
PUT /api/users/1
Content-Type: application/json
```

```json
{
  "name": "Ana Gómez",
  "email": "ana.gomez@mail.com",
  "password": "NuevaClave123!"
}
```

El servicio vuelve a ejecutar:

```java
passwordEncoder.encode(dto.getPassword())
```

y reemplaza el hash anterior.

Nunca debemos guardar directamente:

```java
user.setPassword(dto.getPassword());
```

---

# 22. El Controller no necesita aplicar BCrypt

El controlador puede conservarse:

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
    public UserResponseDTO create(
            @Valid @RequestBody CreateUserDTO dto) {

        return userService.create(dto);
    }

    @GetMapping
    public List<UserResponseDTO> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponseDTO findById(
            @PathVariable Long id) {

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

El controlador:

* Recibe la petición.
* Valida el DTO con `@Valid`.
* Delega al servicio.

No debe generar el hash.

---

# 23. Probar la creación

```http
POST /api/users
Content-Type: application/json
```

```json
{
  "name": "Ana Gómez",
  "email": "ana@mail.com",
  "password": "ClaveSegura123!"
}
```

Respuesta:

```json
{
  "id": 1,
  "name": "Ana Gómez",
  "email": "ana@mail.com"
}
```

La respuesta no devuelve:

```text
password
passwordHash
```

---

# 24. Probar la validación de la contraseña

Ejemplos:

| Contraseña | ¿Válida? | Motivo |
| --- | ---: | --- |
| `Clave123!` | Sí | Cumple todas las reglas |
| `clave123!` | No | No tiene mayúscula |
| `CLAVE123!` | No | No tiene minúscula |
| `ClaveSegura!` | No | No tiene número |
| `Clave1234` | No | No tiene carácter especial |
| `Cla1!` | No | Tiene menos de 8 caracteres |
| `Clave 123!` | No | Contiene un espacio |

Si la contraseña no cumple, Jakarta Validation responderá con:

```http
400 Bad Request
```

El controlador no llegará a ejecutar el servicio porque el DTO se valida primero
mediante `@Valid`.

---

# 25. Verificar MySQL

Ejecutamos:

```sql
SELECT id, name, email, password
FROM users;
```

Resultado esperado:

```text
id | name      | email        | password
──────────────────────────────────────────────────────────────
1  | Ana Gómez | ana@mail.com | $2a$12$...
```

No debe aparecer:

```text
ClaveSegura123!
```

---

# 26. La misma contraseña genera hashes diferentes

Si dos usuarios utilizan:

```text
ClaveSegura123!
```

podemos obtener:

```text
Usuario 1 → $2a$12$abc...
Usuario 2 → $2a$12$xyz...
```

Esto ocurre porque BCrypt genera un `salt` aleatorio para cada hash.

Los hashes distintos pueden representar la misma contraseña.

Por eso nunca debemos comparar así:

```java
passwordEncoder.encode(rawPassword)
        .equals(user.getPassword());
```

---

# 27. ¿Cómo se comprueba una contraseña?

BCrypt no descifra el valor almacenado.

Utiliza:

```java
passwordEncoder.matches(
        rawPassword,
        encodedPassword
);
```

Ejemplo conceptual:

```java
boolean matches = passwordEncoder.matches(
        "ClaveSegura123!",
        user.getPassword()
);
```

Resultado:

```text
true
```

Con una contraseña incorrecta:

```java
passwordEncoder.matches(
        "ClaveIncorrecta",
        user.getPassword()
);
```

Resultado:

```text
false
```

Esta operación se utilizará posteriormente durante el login.

---

# 28. No crear un endpoint para comprobar contraseñas

Aunque `matches()` permite comprobar una contraseña, no agregaremos algo como:

```http
POST /api/users/{id}/check-password
```

Un endpoint así puede facilitar intentos masivos y revelar información innecesaria.

La comprobación se integrará más adelante dentro del proceso formal de autenticación.

---

# 29. ¿Qué pasa con los usuarios existentes?

Si la tabla ya contiene contraseñas en texto plano:

```text
12345678
```

agregar BCrypt no las transforma automáticamente.

Tenemos tres opciones:

1. Eliminar los usuarios de prueba y crearlos nuevamente.
2. Forzar un restablecimiento de contraseña.
3. Crear una migración controlada si conocemos legítimamente las contraseñas originales.

No debemos aplicar BCrypt sobre un hash BCrypt existente:

```text
contraseña
   ↓ BCrypt
hash
   ↓ BCrypt otra vez
hash del hash  ← incorrecto
```

---

# 30. Error frecuente: hashear en el Controller

No recomendado:

```java
@PostMapping
public UserResponseDTO create(CreateUserDTO dto) {
    dto.setPassword(
            passwordEncoder.encode(dto.getPassword())
    );

    return userService.create(dto);
}
```

El controlador no debería implementar seguridad ni modificar el DTO.

La responsabilidad corresponde al servicio.

---

# 31. Error frecuente: devolver la entidad

No recomendado:

```java
public User create(CreateUserDTO dto) {
    return userRepository.save(user);
}
```

Esto podría exponer:

```json
{
  "id": 1,
  "name": "Ana Gómez",
  "email": "ana@mail.com",
  "password": "$2a$12$..."
}
```

Aunque sea un hash, no debe enviarse al cliente.

Debemos devolver:

```java
UserResponseDTO
```

---

# 32. Error frecuente: comparar hashes directamente

Incorrecto:

```java
String newHash = passwordEncoder.encode(rawPassword);

if (newHash.equals(user.getPassword())) {
    // ...
}
```

La comparación normalmente fallará porque cada llamada a `encode()` genera un `salt` distinto.

Correcto:

```java
passwordEncoder.matches(
        rawPassword,
        user.getPassword()
);
```

---

# 33. Responsabilidad de cada componente

| Componente | Responsabilidad |
| --- | --- |
| `CreateUserDTO` | Recibir y validar la contraseña original |
| `UpdateUserDTO` | Recibir opcionalmente una contraseña nueva |
| `SecurityConfig` | Registrar BCrypt y configurar temporalmente `permitAll()` |
| `UserMapper` | Convertir DTO y hash en una entidad |
| `UserService` | Decidir cuándo generar o reemplazar el hash |
| `UserRepository` | Guardar el usuario |
| `UserResponseDTO` | Excluir completamente la contraseña |
| `UserController` | Recibir la petición y delegar |

---

# 34. Arquitectura final

```text
CreateUserDTO
      │
      ▼
UserController
      │
      ▼
UserService
      │
      ├── PasswordEncoder.encode()
      │
      ▼
UserMapper
      │
      ▼
UserRepository
      │
      ▼
MySQL
```

Para la respuesta:

```text
User guardado
      │
      ▼
UserMapper
      │
      ▼
UserResponseDTO
      │
      ▼
JSON sin contraseña
```

---

# 35. Qué debe aprender el estudiante

### ¿BCrypt cifra la contraseña?

No. Genera un hash no reversible.

### ¿Qué debe cumplir la contraseña?

Debe tener entre 8 y 72 caracteres e incluir mayúscula, minúscula, número y
carácter especial, sin espacios.

### ¿Dónde se genera el hash?

En el servicio mediante `PasswordEncoder`.

### ¿Por qué no se genera en el mapper?

Porque elegir y aplicar el mecanismo de protección es una decisión de seguridad, no una simple transformación.

### ¿Por qué no se devuelve el hash?

Porque es información sensible y no aporta nada al cliente.

### ¿Cómo se verifica una contraseña?

Mediante:

```java
passwordEncoder.matches(rawPassword, encodedPassword)
```

### ¿Qué pasa si no envío contraseña al actualizar?

Se conserva el hash almacenado.

### ¿Qué pasa si envío una contraseña nueva?

Se genera un hash nuevo antes de guardar.

### ¿La misma contraseña produce el mismo hash?

No necesariamente. BCrypt utiliza un `salt` aleatorio.

---

# 36. Evolución del proyecto

Antes:

```text
CreateUserDTO
      ↓
UserMapper
      ↓
Contraseña en texto plano
      ↓
MySQL
```

Ahora:

```text
CreateUserDTO
      ↓
UserService
      ↓
BCrypt
      ↓
UserMapper
      ↓
Hash
      ↓
MySQL
```

El siguiente incremento podrá implementar el login:

```text
email + contraseña
        ↓
buscar usuario
        ↓
passwordEncoder.matches()
        ↓
credenciales válidas o inválidas
```

Todavía no generaremos JWT. Los endpoints permanecerán temporalmente públicos
mediante `permitAll()`.

---

# 37. Cómo evolucionará esta configuración

## Incremento actual: protección de contraseñas

```text
SecurityConfig
├── PasswordEncoder con BCrypt
└── Todos los endpoints con permitAll()
```

En esta etapa:

```text
La contraseña se almacena de forma segura.
Los endpoints todavía no exigen autenticación.
```

## Próximo incremento: autenticación

La configuración evolucionará para permitir públicamente solo:

```text
POST /api/auth/login
POST /api/auth/register
```

y exigir autenticación en las demás rutas:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .anyRequest().authenticated()
)
```

También se incorporarán:

```text
CustomUserDetailsService
AuthenticationManager
PasswordEncoder.matches()
JWT
JwtAuthenticationFilter
```

## Incremento posterior: autorización

Después del filtro JWT se ejecutará el sistema dinámico de permisos:

```text
Petición
    ↓
JwtAuthenticationFilter
    ↓
Usuario autenticado
    ↓
DynamicAuthorizationManager
    ↓
EndpointPermission
    ↓
User → Role → Permission
    ↓
Controller
```

La regla global evolucionará conceptualmente a:

```java
.anyRequest()
.access(dynamicAuthorizationManager)
```

Por tanto, instalar el starter desde esta guía no desperdicia el trabajo actual:
BCrypt, el `PasswordEncoder` y la cadena de seguridad se reutilizarán en los
siguientes incrementos.

---

# 38. Referencias oficiales

* Spring Security: Password Storage.
* Spring Security API: `BCryptPasswordEncoder`.
* Spring Security API: `PasswordEncoder`.
