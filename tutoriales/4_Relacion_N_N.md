# Guía práctica: Relación N:N entre `User` y `Role` mediante `UserRole`

## 1. Objetivo

Continuaremos el proyecto anterior agregando:

```text
Role
──────────────────
id          : Long
name        : String
description : String
```

Un usuario podrá tener muchos roles y un rol podrá pertenecer a muchos usuarios:

```text
User N ───────── N Role
```

La relación se implementará mediante:

```text
UserRole
──────────────────
id   : Long
user : User
role : Role
```

Visualmente:

```text
User 1 ───── N UserRole N ───── 1 Role
```

La base de datos tendrá:

```text
users                 user_roles              roles
────────────          ────────────────        ───────────────
id              ┌──── user_id  FK             id
name            │     role_id  FK ─────────── name
email           │                           description
password        │
id ─────────────┘
```

Los objetivos serán:

* Crear, consultar, actualizar y eliminar roles.
* Asignar un rol a un usuario.
* Consultar los roles de un usuario.
* Retirar un rol de un usuario.
* Evitar asignaciones duplicadas.

---

# 2. Comprender la cardinalidad

```text
Un User puede tener cero, uno o muchos Role.
Un Role puede pertenecer a cero, uno o muchos User.
```

Ejemplo:

```text
User 1
 ├── ADMIN
 └── EDITOR

User 2
 ├── EDITOR
 └── AUDITOR
```

El usuario 1 tiene varios roles y `EDITOR` pertenece a varios usuarios.

En la base de datos, la relación N:N se transforma en dos relaciones 1:N:

```text
User 1 ───── N UserRole
Role 1 ───── N UserRole
```

---

# 3. ¿Por qué crear la entidad `UserRole`?

JPA permite una relación directa:

```java
@ManyToMany
private Set<Role> roles;
```

En esta guía representaremos explícitamente la tabla intermedia:

```text
UserRole
├── id
├── user
└── role
```

Esto permite:

* Administrar cada asignación como un recurso.
* Identificarla mediante un `id`.
* Crear endpoints específicos para asignar y retirar roles.
* Agregar atributos a la relación en el futuro.

---

# 4. Estructura del proyecto

```text
src/main/java/com/example/users
│
├── controller
│   ├── RoleController.java
│   └── UserRoleController.java
│
├── service
│   ├── RoleService.java
│   └── UserRoleService.java
│
├── repository
│   ├── RoleRepository.java
│   └── UserRoleRepository.java
│
├── entity
│   ├── User.java
│   ├── Role.java
│   └── UserRole.java
│
├── dto
│   ├── RoleRequestDTO.java
│   ├── RoleResponseDTO.java
│   ├── AssignRoleRequestDTO.java
│   ├── UserRoleResponseDTO.java
│   └── UserRolesResponseDTO.java
│
└── mapper
    ├── RoleMapper.java
    └── UserRoleMapper.java
```

---

# 5. Crear Entity `Role`

```text
entity/Role.java
```

```java
package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 50
    )
    private String name;

    @Column(
            nullable = false,
            length = 255
    )
    private String description;

    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY
    )
    private List<UserRole> userRoles = new ArrayList<>();
}
```

El nombre será único para impedir:

```text
ADMIN
ADMIN  ← no permitido
```

---

# 6. Crear Entity `UserRole`

```text
entity/UserRole.java
```

```java
package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_role",
                        columnNames = {"user_id", "role_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class UserRole {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "role_id",
            nullable = false
    )
    private Role role;
}
```

`UserRole` es dueño de ambas relaciones porque contiene las llaves foráneas.

La restricción:

```java
@UniqueConstraint(
    columnNames = {"user_id", "role_id"}
)
```

garantiza que un usuario no tenga dos veces el mismo rol.

---

# 7. ¿Por qué `UserRole` tiene un `id`?

La combinación:

```text
user_id + role_id
```

identifica funcionalmente la asignación. También utilizamos:

```java
private Long id;
```

Esto simplifica:

* Las consultas.
* Las eliminaciones.
* Los endpoints.
* El uso de `JpaRepository<UserRole, Long>`.

---

# 8. Modificar Entity `User`

Agregamos:

```java
@OneToMany(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
)
private List<UserRole> userRoles = new ArrayList<>();
```

La entidad, conservando las relaciones anteriores, quedará:

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
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

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<UserRole> userRoles = new ArrayList<>();
}
```

---

# 9. Entender `mappedBy`

En `User`:

```java
mappedBy = "user"
```

corresponde al atributo:

```java
private User user;
```

de `UserRole`.

En `Role`:

```java
mappedBy = "role"
```

corresponde a:

```java
private Role role;
```

`mappedBy` contiene el nombre del atributo Java, no el nombre de la columna MySQL.

---

# 10. Entender la eliminación

En `User` usamos:

```java
cascade = CascadeType.ALL
orphanRemoval = true
```

Por tanto:

```text
Eliminar User
    ↓
Eliminar sus UserRole
    ↓
Los Role permanecen
```

En `Role` no usamos `CascadeType.REMOVE`, porque eliminar un rol no debe eliminar usuarios.

El servicio impedirá eliminar un rol mientras tenga asignaciones.

---

# 11. Crear `RoleRequestDTO`

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequestDTO {

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(
            max = 50,
            message = "El nombre no puede superar 50 caracteres"
    )
    private String name;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(
            max = 255,
            message = "La descripción no puede superar 255 caracteres"
    )
    private String description;
}
```

---

# 12. Crear `RoleResponseDTO`

```java
package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class RoleResponseDTO {

    Long id;

    String name;

    String description;
}
```

No incluimos usuarios dentro de este DTO para evitar ciclos.

---

# 13. Crear `AssignRoleRequestDTO`

Como `UserRole` representa la asociación, el cuerpo debe identificar sus dos extremos:

```http
POST /api/user-roles
```

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequestDTO {

    @NotNull(message = "El identificador del usuario es obligatorio")
    private Long userId;

    @NotNull(message = "El identificador del rol es obligatorio")
    private Long roleId;
}
```

---

# 14. Crear `UserRoleResponseDTO`

```java
package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class UserRoleResponseDTO {

    Long id;

    Long userId;

    RoleResponseDTO role;
}
```

`id` identifica la asignación y `role.id` identifica el rol.

También creamos `RoleUserResponseDTO`, simétrico al anterior, para representar
el usuario dentro de un rol sin reutilizar el DTO orientado a "roles de un
usuario":

```java
package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class RoleUserResponseDTO {

    Long id;

    Long roleId;

    UserResponseDTO user;
}
```

`id` identifica la asignación, `roleId` el rol y `user` contiene los datos
básicos del usuario (`id`, `name`, `email`).

---

# 15. Crear `UserRolesResponseDTO`

```java
package com.uc.ms_security.dto;

import lombok.Value;

import java.util.List;

@Value
public class UserRolesResponseDTO {

    Long id;

    String name;

    String email;

    List<UserRoleResponseDTO> roles;
}
```

---

# 16. Crear `RoleMapper`

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.RoleRequestDTO;
import com.uc.ms_security.dto.RoleResponseDTO;
import com.uc.ms_security.entity.Role;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleMapper {

    public Role toEntity(RoleRequestDTO dto) {
        Role role = new Role();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        return role;
    }

    public void updateEntity(
            RoleRequestDTO dto,
            Role role) {

        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
    }

    public RoleResponseDTO toResponseDTO(Role role) {
        return new RoleResponseDTO(
                role.getId(),
                role.getName(),
                role.getDescription()
        );
    }

    public List<RoleResponseDTO> toResponseDTOList(
            List<Role> roles) {

        return roles.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
```

---

# 17. Crear `UserRoleMapper`

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.RoleUserResponseDTO;
import com.uc.ms_security.dto.UserResponseDTO;
import com.uc.ms_security.dto.UserRoleResponseDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserRoleMapper {

    private final RoleMapper roleMapper;

    public UserRoleResponseDTO toResponseDTO(
            UserRole userRole) {

        return new UserRoleResponseDTO(
                userRole.getId(),
                userRole.getUser().getId(),
                roleMapper.toResponseDTO(
                        userRole.getRole()
                )
        );
    }

    public List<UserRoleResponseDTO> toResponseDTOList(
            List<UserRole> userRoles) {

        return userRoles.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public RoleUserResponseDTO toRoleUserResponseDTO(
            UserRole userRole) {

        User user = userRole.getUser();

        return new RoleUserResponseDTO(
                userRole.getId(),
                userRole.getRole().getId(),
                new UserResponseDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail()
                )
        );
    }

    public List<RoleUserResponseDTO> toRoleUserResponseDTOList(
            List<UserRole> userRoles) {

        return userRoles.stream()
                .map(this::toRoleUserResponseDTO)
                .toList();
    }
}
```

`toRoleUserResponseDTO` construye el `UserResponseDTO` directamente con los
datos de `User` para evitar una dependencia circular con `UserMapper` (que ya
depende de `UserRoleMapper`).

---

# 18. Crear `RoleRepository`

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository
        extends JpaRepository<Role, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );
}
```

---

# 19. Crear `UserRoleRepository`

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository
        extends JpaRepository<UserRole, Long> {

    boolean existsByUserIdAndRoleId(
            Long userId,
            Long roleId
    );

    @EntityGraph(attributePaths = {"user", "role"})
    List<UserRole> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "role"})
    List<UserRole> findAllByRoleId(Long roleId);

    @Override
    @EntityGraph(attributePaths = {"user", "role"})
    List<UserRole> findAll();

    @EntityGraph(attributePaths = {"user", "role"})
    Optional<UserRole> findByUserIdAndRoleId(
            Long userId,
            Long roleId
    );

    boolean existsByRoleId(Long roleId);
}
```

`findAllByUserId` permite listar los roles de un usuario y `findAllByRoleId`
los usuarios que tienen un rol. `findByUserIdAndRoleId` ubica la asignación
exacta que se debe eliminar.

---

# 20. Crear `RoleService`

```java
package com.uc.ms_security.service;

import com.uc.ms_security.dto.RoleRequestDTO;
import com.uc.ms_security.dto.RoleResponseDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.mapper.RoleMapper;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMapper roleMapper;

    public RoleResponseDTO create(RoleRequestDTO dto) {
        if (roleRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un rol con ese nombre"
            );
        }

        Role role = roleMapper.toEntity(dto);

        return roleMapper.toResponseDTO(
                roleRepository.save(role)
        );
    }

    public List<RoleResponseDTO> findAll() {
        return roleMapper.toResponseDTOList(
                roleRepository.findAll()
        );
    }

    public RoleResponseDTO findById(Long id) {
        return roleMapper.toResponseDTO(
                findEntityById(id)
        );
    }

    public RoleResponseDTO update(
            Long id,
            RoleRequestDTO dto) {

        Role role = findEntityById(id);

        if (roleRepository.existsByNameIgnoreCaseAndIdNot(
                dto.getName(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un rol con ese nombre"
            );
        }

        roleMapper.updateEntity(dto, role);

        return roleMapper.toResponseDTO(
                roleRepository.save(role)
        );
    }

    public void delete(Long id) {
        Role role = findEntityById(id);

        if (userRoleRepository.existsByRoleId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar un rol que está asignado"
            );
        }

        roleRepository.delete(role);
    }

    private Role findEntityById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Rol no encontrado"
                        )
                );
    }
}
```

---

# 21. Crear `RoleController`

```java
package com.uc.ms_security.controller;

import com.uc.ms_security.dto.RoleRequestDTO;
import com.uc.ms_security.dto.RoleResponseDTO;
import com.uc.ms_security.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponseDTO create(
            @Valid @RequestBody RoleRequestDTO dto) {
        return roleService.create(dto);
    }

    @GetMapping
    public List<RoleResponseDTO> findAll() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    public RoleResponseDTO findById(
            @PathVariable Long id) {
        return roleService.findById(id);
    }

    @PutMapping("/{id}")
    public RoleResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequestDTO dto) {
        return roleService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
```

---

# 22. Crear `UserRoleService`

```java
package com.uc.ms_security.service;

import com.uc.ms_security.dto.AssignRoleRequestDTO;
import com.uc.ms_security.dto.RoleUserResponseDTO;
import com.uc.ms_security.dto.UserRoleResponseDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.mapper.UserRoleMapper;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleMapper userRoleMapper;

    @Transactional
    public UserRoleResponseDTO assign(
            AssignRoleRequestDTO dto) {

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        )
                );

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Rol no encontrado"
                        )
                );

        if (userRoleRepository.existsByUserIdAndRoleId(
                dto.getUserId(),
                dto.getRoleId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El usuario ya tiene asignado ese rol"
            );
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        return userRoleMapper.toResponseDTO(
                userRoleRepository.save(userRole)
        );
    }

    @Transactional(readOnly = true)
    public List<UserRoleResponseDTO> findAll() {
        return userRoleMapper.toResponseDTOList(
                userRoleRepository.findAll()
        );
    }

    @Transactional(readOnly = true)
    public List<UserRoleResponseDTO> findByUserId(Long userId) {
        return userRoleMapper.toResponseDTOList(
                userRoleRepository.findAllByUserId(userId)
        );
    }

    @Transactional(readOnly = true)
    public List<RoleUserResponseDTO> findByRoleId(Long roleId) {
        return userRoleMapper.toRoleUserResponseDTOList(
                userRoleRepository.findAllByRoleId(roleId)
        );
    }

    @Transactional
    public void delete(Long userId, Long roleId) {
        UserRole userRole = findAssignment(userId, roleId);

        userRoleRepository.delete(userRole);
    }

    private UserRole findAssignment(Long userId, Long roleId) {
        return userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Asignación de rol no encontrada"
                        )
                );
    }
}
```

---

# 23. Crear `UserRoleController`

```java
package com.uc.ms_security.controller;

import com.uc.ms_security.dto.AssignRoleRequestDTO;
import com.uc.ms_security.dto.RoleUserResponseDTO;
import com.uc.ms_security.dto.UserRoleResponseDTO;
import com.uc.ms_security.service.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserRoleResponseDTO assign(
            @Valid @RequestBody AssignRoleRequestDTO dto) {

        return userRoleService.assign(dto);
    }

    @GetMapping
    public List<UserRoleResponseDTO> findAll() {
        return userRoleService.findAll();
    }

    @GetMapping("/users/{userId}")
    public List<UserRoleResponseDTO> findByUserId(
            @PathVariable Long userId) {

        return userRoleService.findByUserId(userId);
    }

    @GetMapping("/roles/{roleId}")
    public List<RoleUserResponseDTO> findByRoleId(
            @PathVariable Long roleId) {

        return userRoleService.findByRoleId(roleId);
    }

    @DeleteMapping("/{userId}/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        userRoleService.delete(userId, roleId);
    }
}
```

`GET /api/user-roles/users/{userId}` devuelve los roles de un usuario y
`GET /api/user-roles/roles/{roleId}` devuelve los usuarios que tienen ese rol.
No necesitamos `PUT` para `UserRole`: para cambiar el rol, retiramos la asignación anterior y creamos otra.

---

# 24. Endpoints resultantes

## Roles

```http
POST    /api/roles
GET     /api/roles
GET     /api/roles/{roleId}
PUT     /api/roles/{roleId}
DELETE  /api/roles/{roleId}
```

## Asignaciones

```http
POST    /api/user-roles
GET     /api/user-roles
GET     /api/user-roles/users/{userId}
GET     /api/user-roles/roles/{roleId}
DELETE  /api/user-roles/{userId}/{roleId}
```

---

# 25. Crear un rol

```http
POST /api/roles
Content-Type: application/json
```

```json
{
  "name": "ADMIN",
  "description": "Administra usuarios, roles y permisos"
}
```

Respuesta:

```json
{
  "id": 1,
  "name": "ADMIN",
  "description": "Administra usuarios, roles y permisos"
}
```

---

# 26. Asignar un rol

Supongamos:

```text
userId = 1
roleId = 2
```

```http
POST /api/user-roles
Content-Type: application/json
```

```json
{
  "userId": 1,
  "roleId": 2
}
```

Respuesta:

```json
{
  "id": 1,
  "userId": 1,
  "role": {
    "id": 2,
    "name": "EDITOR",
    "description": "Puede editar contenido"
  }
}
```

---

# 27. Consultar las asociaciones

```http
GET /api/user-roles
```

Este endpoint devuelve todas las asociaciones e incluye el `userId` de cada una.

```json
[
  {
    "id": 1,
    "userId": 1,
    "role": {
      "id": 2,
      "name": "EDITOR",
      "description": "Puede editar contenido"
    }
  },
  {
    "id": 2,
    "userId": 1,
    "role": {
      "id": 1,
      "name": "ADMIN",
      "description": "Administra usuarios, roles y permisos"
    }
  }
]
```

## Consultar los roles de un usuario

```http
GET /api/user-roles/users/1
```

```json
[
  {
    "id": 1,
    "userId": 1,
    "role": {
      "id": 2,
      "name": "EDITOR",
      "description": "Puede editar contenido"
    }
  }
]
```

## Consultar los usuarios que tienen un rol

```http
GET /api/user-roles/roles/2
```

Este endpoint usa `RoleUserResponseDTO`, con el detalle del usuario en lugar
del rol:

```json
[
  {
    "id": 1,
    "roleId": 2,
    "user": {
      "id": 1,
      "name": "Ana Gómez",
      "email": "ana@example.com"
    }
  }
]
```

---

# 28. Retirar un rol

```http
DELETE /api/user-roles/1/2
```

Los identificadores corresponden a `userId` y `roleId` respectivamente.

La operación elimina:

```text
UserRole
```

pero conserva:

```text
User
Role
```

---

# 29. Consultar el usuario con sus roles

Agregamos en `UserRepository`:

```java
@EntityGraph(
        attributePaths = {
                "userRoles",
                "userRoles.role"
        }
)
Optional<User> findWithRolesById(Long id);
```

En `UserMapper` inyectamos:

```java
private final UserRoleMapper userRoleMapper;
```

y agregamos:

```java
public UserRolesResponseDTO toRolesResponseDTO(
        User user) {

    return new UserRolesResponseDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            userRoleMapper.toResponseDTOList(
                    user.getUserRoles()
            )
    );
}
```

En `UserService`:

```java
public UserRolesResponseDTO findByIdAndRoles(Long id) {
    User user = userRepository
            .findWithRolesById(id)
            .orElseThrow(
                    () -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Usuario no encontrado"
                    )
            );

    return userMapper.toRolesResponseDTO(user);
}
```

En `UserController`:

```java
@GetMapping("/{id}/detail-with-roles")
public UserRolesResponseDTO findByIdAndRoles(
        @PathVariable Long id) {

    return userService.findByIdAndRoles(id);
}
```

---

# 30. Evitar recursividad

Las entidades forman:

```text
User → UserRole → Role → UserRole → User...
```

Los DTO evitan el ciclo:

```text
UserRolesResponseDTO
 ↓
UserRoleResponseDTO
 ↓
RoleResponseDTO
```

---

# 31. Verificar MySQL

```sql
SELECT * FROM roles;
```

```text
id | name   | description
──────────────────────────────────────────
1  | ADMIN  | Administra usuarios y roles
2  | EDITOR | Puede editar contenido
```

```sql
SELECT * FROM user_roles;
```

```text
id | user_id | role_id
──────────────────────
1  | 1       | 2
2  | 1       | 1
3  | 2       | 2
```

Consulta completa:

```sql
SELECT
    u.id AS user_id,
    u.name AS user_name,
    r.id AS role_id,
    r.name AS role_name
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
JOIN roles r ON r.id = ur.role_id;
```

---

# 32. Casos que debemos probar

| Caso | Resultado |
| --- | --- |
| Usuario y rol existentes | `201 Created` |
| Usuario inexistente | `404 Not Found` |
| Rol inexistente | `404 Not Found` |
| Rol duplicado para el mismo usuario | `409 Conflict` |
| Eliminar un rol todavía asignado | `409 Conflict` |
| Usuario sin roles | `[]` |

---

# 33. Relación JPA final

## `User`

```java
@OneToMany(mappedBy = "user")
private List<UserRole> userRoles;
```

## `UserRole`

```java
@ManyToOne
@JoinColumn(name = "user_id")
private User user;

@ManyToOne
@JoinColumn(name = "role_id")
private Role role;
```

## `Role`

```java
@OneToMany(mappedBy = "role")
private List<UserRole> userRoles;
```

```text
User 1 ───── N UserRole N ───── 1 Role
```

---

# 34. Qué debe aprender el estudiante

### ¿Qué representa una relación N:N?

Muchos usuarios pueden tener muchos roles.

### ¿Cómo se representa en la base de datos?

Mediante la tabla intermedia `user_roles`.

### ¿Por qué usamos `UserRole`?

Para administrar explícitamente cada asignación.

### ¿Quién es dueño de las relaciones?

`UserRole`, porque contiene `user_id` y `role_id`.

### ¿Por qué la combinación usuario-rol es única?

Para impedir asignaciones duplicadas.

### ¿Qué ocurre al retirar un rol?

Se elimina `UserRole`, pero permanecen `User` y `Role`.

### ¿Por qué usamos DTOs?

Para controlar las respuestas y evitar recursividad.

---

# 35. Evolución del proyecto

```text
User 1 ───── 0..1 Profile

User 1 ───── 0..N Session

User 1 ───── 0..N UserRole N..0 ───── 1 Role
```

Conceptualmente:

```text
User N ───────── N Role
```

Este diseño permitirá posteriormente comprobar si un usuario posee un rol antes de autorizar una operación.
