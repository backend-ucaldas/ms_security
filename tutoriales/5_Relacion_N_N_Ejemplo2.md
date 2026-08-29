# Guía práctica: Relación N:N entre `Role` y `Permission` mediante `RolePermission`

## 1. Objetivo

Siguiendo el mismo esquema usado para `User`-`Role`, agregaremos una nueva relación N:N, esta vez entre `Role` y una nueva entidad `Permission`:

```text
Permission
──────────────────
id     : Long
url    : String
method : String
```

Un rol podrá tener muchos permisos y un permiso podrá pertenecer a muchos roles:

```text
Role N ───────── N Permission
```

La relación se implementará mediante:

```text
RolePermission
──────────────────
id         : Long
role       : Role
permission : Permission
```

Visualmente:

```text
Role 1 ───── N RolePermission N ───── 1 Permission
```

La base de datos tendrá:

```text
roles                 role_permissions           permissions
────────────          ────────────────────       ───────────────
id              ┌──── role_id       FK             id
name            │     permission_id FK ─────────── url
description     │                                method
id ─────────────┘
```

Los objetivos serán:

* Crear, consultar, actualizar y eliminar permisos.
* Asignar un permiso a un rol.
* Consultar los permisos de un rol.
* Consultar los roles que tienen un permiso.
* Retirar un permiso de un rol.
* Evitar asignaciones duplicadas.

---

# 2. Comprender la cardinalidad

```text
Un Role puede tener cero, uno o muchos Permission.
Un Permission puede pertenecer a cero, uno o muchos Role.
```

Ejemplo:

```text
Role ADMIN
 ├── GET /api/users
 └── DELETE /api/users/{id}

Role EDITOR
 ├── GET /api/users
 └── PUT /api/users/{id}
```

El rol `ADMIN` tiene varios permisos y `GET /api/users` pertenece a varios roles.

En la base de datos, la relación N:N se transforma en dos relaciones 1:N:

```text
Role 1 ───── N RolePermission
Permission 1 ───── N RolePermission
```

---

# 3. ¿Por qué crear la entidad `RolePermission`?

Igual que hicimos con `UserRole`, representamos explícitamente la tabla intermedia en lugar de usar `@ManyToMany` directo:

```text
RolePermission
├── id
├── role
└── permission
```

Esto permite:

* Administrar cada asignación como un recurso.
* Identificarla mediante un `id`.
* Crear endpoints específicos para asignar y retirar permisos.
* Agregar atributos a la relación en el futuro (por ejemplo, quién la otorgó).

---

# 4. Estructura del proyecto

```text
src/main/java/com/uc/ms_security
│
├── controller
│   ├── PermissionController.java
│   └── RolePermissionController.java
│
├── service
│   ├── PermissionService.java
│   └── RolePermissionService.java
│
├── repository
│   ├── PermissionRepository.java
│   └── RolePermissionRepository.java
│
├── entity
│   ├── Role.java
│   ├── Permission.java
│   └── RolePermission.java
│
├── dto
│   ├── PermissionRequestDTO.java
│   ├── PermissionResponseDTO.java
│   ├── AssignPermissionRequestDTO.java
│   ├── RolePermissionResponseDTO.java
│   ├── PermissionRoleResponseDTO.java
│   └── RolePermissionsResponseDTO.java
│
└── mapper
    ├── PermissionMapper.java
    └── RolePermissionMapper.java
```

---

# 5. Crear Entity `Permission`

```text
entity/Permission.java
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
    private List<RolePermission> rolePermissions = new ArrayList<>();
}
```

A diferencia de `Role`, no exigimos que `url` sea único por sí sola, porque la misma url puede combinarse con distintos métodos HTTP (`GET`, `POST`, etc.).

---

# 6. Crear Entity `RolePermission`

```text
entity/RolePermission.java
```

```java
package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_role_permission",
                        columnNames = {"role_id", "permission_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RolePermission {

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
            name = "role_id",
            nullable = false
    )
    private Role role;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "permission_id",
            nullable = false
    )
    private Permission permission;
}
```

`RolePermission` es dueño de ambas relaciones porque contiene las llaves foráneas.

La restricción:

```java
@UniqueConstraint(
    columnNames = {"role_id", "permission_id"}
)
```

garantiza que un rol no tenga dos veces el mismo permiso.

---

# 7. ¿Por qué `RolePermission` tiene un `id`?

Igual que en `UserRole`, la combinación:

```text
role_id + permission_id
```

identifica funcionalmente la asignación, pero usamos un `id` propio para simplificar:

* Las consultas.
* Las eliminaciones.
* Los endpoints.
* El uso de `JpaRepository<RolePermission, Long>`.

---

# 8. Modificar Entity `Role`

Agregamos la lista de asignaciones hacia `Permission`, conservando la relación existente con `UserRole`:

```java
@OneToMany(
        mappedBy = "role",
        fetch = FetchType.LAZY
)
private List<UserRole> userRoles = new ArrayList<>();

@OneToMany(
        mappedBy = "role",
        fetch = FetchType.LAZY
)
private List<RolePermission> rolePermissions = new ArrayList<>();
```

La entidad completa queda:

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

    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY
    )
    private List<RolePermission> rolePermissions = new ArrayList<>();
}
```

---

# 9. Entender `mappedBy`

En `Role`:

```java
mappedBy = "role"
```

corresponde al atributo:

```java
private Role role;
```

de `RolePermission`.

En `Permission`:

```java
mappedBy = "permission"
```

corresponde a:

```java
private Permission permission;
```

`mappedBy` contiene el nombre del atributo Java, no el nombre de la columna MySQL.

---

# 10. Entender la eliminación

En `RolePermission` no usamos `CascadeType.REMOVE` desde ninguno de sus dos extremos: eliminar un rol o un permiso no debe arrastrar automáticamente las asignaciones.

El servicio impedirá:

* Eliminar un permiso mientras tenga roles asignados.
* (El rol ya impedía su eliminación si tenía usuarios asignados; ahora también podríamos extenderlo para permisos, pero mantenemos el criterio original de `UserRole` para no romper el comportamiento existente.)

---

# 11. Crear `PermissionRequestDTO`

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionRequestDTO {

    @NotBlank(message = "La url es obligatoria")
    @Size(
            max = 255,
            message = "La url no puede superar 255 caracteres"
    )
    private String url;

    @NotBlank(message = "El método es obligatorio")
    @Size(
            max = 10,
            message = "El método no puede superar 10 caracteres"
    )
    private String method;
}
```

---

# 12. Crear `PermissionResponseDTO`

```java
package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class PermissionResponseDTO {

    Long id;

    String url;

    String method;
}
```

No incluimos roles dentro de este DTO para evitar ciclos.

---

# 13. Crear `AssignPermissionRequestDTO`

Como `RolePermission` representa la asociación, el cuerpo debe identificar sus dos extremos:

```http
POST /api/role-permissions
```

```java
package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignPermissionRequestDTO {

    @NotNull(message = "El identificador del rol es obligatorio")
    private Long roleId;

    @NotNull(message = "El identificador del permiso es obligatorio")
    private Long permissionId;
}
```

---

# 14. Crear `RolePermissionResponseDTO` y `PermissionRoleResponseDTO`

```java
package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class RolePermissionResponseDTO {

    Long id;

    Long roleId;

    PermissionResponseDTO permission;
}
```

`id` identifica la asignación y `permission.id` identifica el permiso.

De forma simétrica, para representar los roles que tienen un permiso sin reutilizar el DTO anterior:

```java
package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class PermissionRoleResponseDTO {

    Long id;

    Long permissionId;

    RoleResponseDTO role;
}
```

`id` identifica la asignación, `permissionId` el permiso y `role` contiene los datos básicos del rol (`id`, `name`, `description`).

---

# 15. Crear `RolePermissionsResponseDTO`

```java
package com.uc.ms_security.dto;

import lombok.Value;

import java.util.List;

@Value
public class RolePermissionsResponseDTO {

    Long id;

    String name;

    String description;

    List<RolePermissionResponseDTO> permissions;
}
```

---

# 16. Crear `PermissionMapper`

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.PermissionRequestDTO;
import com.uc.ms_security.dto.PermissionResponseDTO;
import com.uc.ms_security.entity.Permission;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PermissionMapper {

    public Permission toEntity(PermissionRequestDTO dto) {
        Permission permission = new Permission();
        permission.setUrl(dto.getUrl());
        permission.setMethod(dto.getMethod());
        return permission;
    }

    public void updateEntity(
            PermissionRequestDTO dto,
            Permission permission) {

        permission.setUrl(dto.getUrl());
        permission.setMethod(dto.getMethod());
    }

    public PermissionResponseDTO toResponseDTO(Permission permission) {
        return new PermissionResponseDTO(
                permission.getId(),
                permission.getUrl(),
                permission.getMethod()
        );
    }

    public List<PermissionResponseDTO> toResponseDTOList(
            List<Permission> permissions) {

        return permissions.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
```

---

# 17. Crear `RolePermissionMapper`

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.PermissionRoleResponseDTO;
import com.uc.ms_security.dto.RolePermissionResponseDTO;
import com.uc.ms_security.dto.RoleResponseDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.RolePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RolePermissionMapper {

    private final PermissionMapper permissionMapper;

    public RolePermissionResponseDTO toResponseDTO(
            RolePermission rolePermission) {

        return new RolePermissionResponseDTO(
                rolePermission.getId(),
                rolePermission.getRole().getId(),
                permissionMapper.toResponseDTO(
                        rolePermission.getPermission()
                )
        );
    }

    public List<RolePermissionResponseDTO> toResponseDTOList(
            List<RolePermission> rolePermissions) {

        return rolePermissions.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public PermissionRoleResponseDTO toPermissionRoleResponseDTO(
            RolePermission rolePermission) {

        Role role = rolePermission.getRole();

        return new PermissionRoleResponseDTO(
                rolePermission.getId(),
                rolePermission.getPermission().getId(),
                new RoleResponseDTO(
                        role.getId(),
                        role.getName(),
                        role.getDescription()
                )
        );
    }

    public List<PermissionRoleResponseDTO> toPermissionRoleResponseDTOList(
            List<RolePermission> rolePermissions) {

        return rolePermissions.stream()
                .map(this::toPermissionRoleResponseDTO)
                .toList();
    }
}
```

`toPermissionRoleResponseDTO` construye el `RoleResponseDTO` directamente con los datos de `Role` para evitar una dependencia circular con `RoleMapper` (que depende de `RolePermissionMapper` para construir `RolePermissionsResponseDTO`).

---

# 18. Modificar `RoleMapper`

Inyectamos `RolePermissionMapper` y agregamos el método para construir el detalle de un rol con sus permisos:

```java
package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.RolePermissionsResponseDTO;
import com.uc.ms_security.dto.RoleRequestDTO;
import com.uc.ms_security.dto.RoleResponseDTO;
import com.uc.ms_security.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleMapper {

    private final RolePermissionMapper rolePermissionMapper;

    // ...métodos existentes: toEntity, updateEntity, toResponseDTO, toResponseDTOList...

    public RolePermissionsResponseDTO toPermissionsResponseDTO(Role role) {
        return new RolePermissionsResponseDTO(
                role.getId(),
                role.getName(),
                role.getDescription(),
                rolePermissionMapper.toResponseDTOList(
                        role.getRolePermissions()
                )
        );
    }
}
```

---

# 19. Crear `PermissionRepository`

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByUrlAndMethod(
            String url,
            String method
    );

    boolean existsByUrlAndMethodAndIdNot(
            String url,
            String method,
            Long id
    );
}
```

---

# 20. Crear `RolePermissionRepository`

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.RolePermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    boolean existsByRoleIdAndPermissionId(
            Long roleId,
            Long permissionId
    );

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermission> findAllByRoleId(Long roleId);

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermission> findAllByPermissionId(Long permissionId);

    @Override
    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermission> findAll();

    @EntityGraph(attributePaths = {"role", "permission"})
    Optional<RolePermission> findByRoleIdAndPermissionId(
            Long roleId,
            Long permissionId
    );

    boolean existsByPermissionId(Long permissionId);
}
```

`findAllByRoleId` permite listar los permisos de un rol y `findAllByPermissionId` los roles que tienen un permiso. `findByRoleIdAndPermissionId` ubica la asignación exacta que se debe eliminar.

---

# 21. Modificar `RoleRepository`

Agregamos la consulta para traer un rol junto con sus permisos:

```java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );

    @EntityGraph(
            attributePaths = {
                    "rolePermissions",
                    "rolePermissions.permission"
            }
    )
    Optional<Role> findWithPermissionsById(Long id);
}
```

---

# 22. Crear `PermissionService`

```java
package com.uc.ms_security.service;

import com.uc.ms_security.dto.PermissionRequestDTO;
import com.uc.ms_security.dto.PermissionResponseDTO;
import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.mapper.PermissionMapper;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionResponseDTO create(PermissionRequestDTO dto) {
        if (permissionRepository.existsByUrlAndMethod(
                dto.getUrl(), dto.getMethod())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un permiso con esa url y método"
            );
        }

        Permission permission = permissionMapper.toEntity(dto);

        return permissionMapper.toResponseDTO(
                permissionRepository.save(permission)
        );
    }

    public List<PermissionResponseDTO> findAll() {
        return permissionMapper.toResponseDTOList(
                permissionRepository.findAll()
        );
    }

    public PermissionResponseDTO findById(Long id) {
        return permissionMapper.toResponseDTO(
                findEntityById(id)
        );
    }

    public PermissionResponseDTO update(
            Long id,
            PermissionRequestDTO dto) {

        Permission permission = findEntityById(id);

        if (permissionRepository.existsByUrlAndMethodAndIdNot(
                dto.getUrl(), dto.getMethod(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un permiso con esa url y método"
            );
        }

        permissionMapper.updateEntity(dto, permission);

        return permissionMapper.toResponseDTO(
                permissionRepository.save(permission)
        );
    }

    public void delete(Long id) {
        Permission permission = findEntityById(id);

        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar un permiso que está asignado"
            );
        }

        permissionRepository.delete(permission);
    }

    private Permission findEntityById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Permiso no encontrado"
                        )
                );
    }
}
```

---

# 23. Crear `PermissionController`

```java
package com.uc.ms_security.controller;

import com.uc.ms_security.dto.PermissionRequestDTO;
import com.uc.ms_security.dto.PermissionResponseDTO;
import com.uc.ms_security.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponseDTO create(
            @Valid @RequestBody PermissionRequestDTO dto) {
        return permissionService.create(dto);
    }

    @GetMapping
    public List<PermissionResponseDTO> findAll() {
        return permissionService.findAll();
    }

    @GetMapping("/{id}")
    public PermissionResponseDTO findById(
            @PathVariable Long id) {
        return permissionService.findById(id);
    }

    @PutMapping("/{id}")
    public PermissionResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody PermissionRequestDTO dto) {
        return permissionService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        permissionService.delete(id);
    }
}
```

---

# 24. Crear `RolePermissionService`

```java
package com.uc.ms_security.service;

import com.uc.ms_security.dto.AssignPermissionRequestDTO;
import com.uc.ms_security.dto.PermissionRoleResponseDTO;
import com.uc.ms_security.dto.RolePermissionResponseDTO;
import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.RolePermission;
import com.uc.ms_security.mapper.RolePermissionMapper;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import com.uc.ms_security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionMapper rolePermissionMapper;

    @Transactional
    public RolePermissionResponseDTO assign(
            AssignPermissionRequestDTO dto) {

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Rol no encontrado"
                        )
                );

        Permission permission = permissionRepository.findById(dto.getPermissionId())
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Permiso no encontrado"
                        )
                );

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(
                dto.getRoleId(),
                dto.getPermissionId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El rol ya tiene asignado ese permiso"
            );
        }

        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);

        return rolePermissionMapper.toResponseDTO(
                rolePermissionRepository.save(rolePermission)
        );
    }

    @Transactional(readOnly = true)
    public List<RolePermissionResponseDTO> findAll() {
        return rolePermissionMapper.toResponseDTOList(
                rolePermissionRepository.findAll()
        );
    }

    @Transactional(readOnly = true)
    public List<RolePermissionResponseDTO> findByRoleId(Long roleId) {
        return rolePermissionMapper.toResponseDTOList(
                rolePermissionRepository.findAllByRoleId(roleId)
        );
    }

    @Transactional(readOnly = true)
    public List<PermissionRoleResponseDTO> findByPermissionId(Long permissionId) {
        return rolePermissionMapper.toPermissionRoleResponseDTOList(
                rolePermissionRepository.findAllByPermissionId(permissionId)
        );
    }

    @Transactional
    public void delete(Long roleId, Long permissionId) {
        RolePermission rolePermission = findAssignment(roleId, permissionId);

        rolePermissionRepository.delete(rolePermission);
    }

    private RolePermission findAssignment(Long roleId, Long permissionId) {
        return rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Asignación de permiso no encontrada"
                        )
                );
    }
}
```

---

# 25. Crear `RolePermissionController`

```java
package com.uc.ms_security.controller;

import com.uc.ms_security.dto.AssignPermissionRequestDTO;
import com.uc.ms_security.dto.PermissionRoleResponseDTO;
import com.uc.ms_security.dto.RolePermissionResponseDTO;
import com.uc.ms_security.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolePermissionResponseDTO assign(
            @Valid @RequestBody AssignPermissionRequestDTO dto) {

        return rolePermissionService.assign(dto);
    }

    @GetMapping
    public List<RolePermissionResponseDTO> findAll() {
        return rolePermissionService.findAll();
    }

    @GetMapping("/roles/{roleId}")
    public List<RolePermissionResponseDTO> findByRoleId(
            @PathVariable Long roleId) {

        return rolePermissionService.findByRoleId(roleId);
    }

    @GetMapping("/permissions/{permissionId}")
    public List<PermissionRoleResponseDTO> findByPermissionId(
            @PathVariable Long permissionId) {

        return rolePermissionService.findByPermissionId(permissionId);
    }

    @DeleteMapping("/{roleId}/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {

        rolePermissionService.delete(roleId, permissionId);
    }
}
```

`GET /api/role-permissions/roles/{roleId}` devuelve los permisos de un rol y `GET /api/role-permissions/permissions/{permissionId}` devuelve los roles que tienen ese permiso, usando `PermissionRoleResponseDTO` para mostrar el detalle del rol en vez del permiso.

No necesitamos `PUT` para `RolePermission`: para cambiar el permiso, retiramos la asignación anterior y creamos otra.

---

# 26. Endpoints resultantes

## Permisos

```http
POST    /api/permissions
GET     /api/permissions
GET     /api/permissions/{permissionId}
PUT     /api/permissions/{permissionId}
DELETE  /api/permissions/{permissionId}
```

## Asignaciones

```http
POST    /api/role-permissions
GET     /api/role-permissions
GET     /api/role-permissions/roles/{roleId}
GET     /api/role-permissions/permissions/{permissionId}
DELETE  /api/role-permissions/{roleId}/{permissionId}
```

## Detalle del rol

```http
GET     /api/roles/{roleId}/detail-with-permissions
```

---

# 27. Crear un permiso

```http
POST /api/permissions
Content-Type: application/json
```

```json
{
  "url": "/api/users",
  "method": "GET"
}
```

Respuesta:

```json
{
  "id": 1,
  "url": "/api/users",
  "method": "GET"
}
```

---

# 28. Asignar un permiso

Supongamos:

```text
roleId = 1
permissionId = 1
```

```http
POST /api/role-permissions
Content-Type: application/json
```

```json
{
  "roleId": 1,
  "permissionId": 1
}
```

Respuesta:

```json
{
  "id": 1,
  "roleId": 1,
  "permission": {
    "id": 1,
    "url": "/api/users",
    "method": "GET"
  }
}
```

---

# 29. Consultar las asociaciones

```http
GET /api/role-permissions
```

Este endpoint devuelve todas las asociaciones e incluye el `roleId` de cada una.

```json
[
  {
    "id": 1,
    "roleId": 1,
    "permission": {
      "id": 1,
      "url": "/api/users",
      "method": "GET"
    }
  },
  {
    "id": 2,
    "roleId": 1,
    "permission": {
      "id": 2,
      "url": "/api/users/{id}",
      "method": "DELETE"
    }
  }
]
```

## Consultar los permisos de un rol

```http
GET /api/role-permissions/roles/1
```

```json
[
  {
    "id": 1,
    "roleId": 1,
    "permission": {
      "id": 1,
      "url": "/api/users",
      "method": "GET"
    }
  }
]
```

## Consultar los roles que tienen un permiso

```http
GET /api/role-permissions/permissions/1
```

Este endpoint usa `PermissionRoleResponseDTO`, con el detalle del rol en lugar del permiso:

```json
[
  {
    "id": 1,
    "permissionId": 1,
    "role": {
      "id": 1,
      "name": "ADMIN",
      "description": "Administra usuarios, roles y permisos"
    }
  }
]
```

---

# 30. Retirar un permiso

```http
DELETE /api/role-permissions/1/1
```

Los identificadores corresponden a `roleId` y `permissionId` respectivamente.

La operación elimina:

```text
RolePermission
```

pero conserva:

```text
Role
Permission
```

---

# 31. Consultar el rol con sus permisos

Agregamos en `RoleRepository`:

```java
@EntityGraph(
        attributePaths = {
                "rolePermissions",
                "rolePermissions.permission"
        }
)
Optional<Role> findWithPermissionsById(Long id);
```

En `RoleMapper` inyectamos:

```java
private final RolePermissionMapper rolePermissionMapper;
```

y agregamos:

```java
public RolePermissionsResponseDTO toPermissionsResponseDTO(
        Role role) {

    return new RolePermissionsResponseDTO(
            role.getId(),
            role.getName(),
            role.getDescription(),
            rolePermissionMapper.toResponseDTOList(
                    role.getRolePermissions()
            )
    );
}
```

En `RoleService`:

```java
public RolePermissionsResponseDTO findByIdAndPermissions(Long id) {
    Role role = roleRepository
            .findWithPermissionsById(id)
            .orElseThrow(
                    () -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Rol no encontrado"
                    )
            );

    return roleMapper.toPermissionsResponseDTO(role);
}
```

En `RoleController`:

```java
@GetMapping("/{id}/detail-with-permissions")
public RolePermissionsResponseDTO findByIdAndPermissions(
        @PathVariable Long id) {

    return roleService.findByIdAndPermissions(id);
}
```

---

# 32. Evitar recursividad

Las entidades forman:

```text
Role → RolePermission → Permission → RolePermission → Role...
```

Los DTO evitan el ciclo:

```text
RolePermissionsResponseDTO
 ↓
RolePermissionResponseDTO
 ↓
PermissionResponseDTO
```

Y en la dirección inversa:

```text
PermissionRoleResponseDTO
 ↓
RoleResponseDTO
```

---

# 33. Verificar MySQL

```sql
SELECT * FROM permissions;
```

```text
id | url                | method
──────────────────────────────────
1  | /api/users         | GET
2  | /api/users/{id}    | DELETE
```

```sql
SELECT * FROM role_permissions;
```

```text
id | role_id | permission_id
─────────────────────────────
1  | 1       | 1
2  | 1       | 2
3  | 2       | 1
```

Consulta completa:

```sql
SELECT
    r.id AS role_id,
    r.name AS role_name,
    p.id AS permission_id,
    p.url AS permission_url,
    p.method AS permission_method
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id;
```

---

# 34. Casos que debemos probar

| Caso | Resultado |
| --- | --- |
| Rol y permiso existentes | `201 Created` |
| Rol inexistente | `404 Not Found` |
| Permiso inexistente | `404 Not Found` |
| Permiso duplicado para el mismo rol | `409 Conflict` |
| Eliminar un permiso todavía asignado | `409 Conflict` |
| Rol sin permisos | `[]` |

---

# 35. Relación JPA final

## `Role`

```java
@OneToMany(mappedBy = "role")
private List<RolePermission> rolePermissions;
```

## `RolePermission`

```java
@ManyToOne
@JoinColumn(name = "role_id")
private Role role;

@ManyToOne
@JoinColumn(name = "permission_id")
private Permission permission;
```

## `Permission`

```java
@OneToMany(mappedBy = "permission")
private List<RolePermission> rolePermissions;
```

```text
Role 1 ───── N RolePermission N ───── 1 Permission
```

---

# 36. Qué debe aprender el estudiante

### ¿Qué representa una relación N:N?

Muchos roles pueden tener muchos permisos.

### ¿Cómo se representa en la base de datos?

Mediante la tabla intermedia `role_permissions`.

### ¿Por qué usamos `RolePermission`?

Para administrar explícitamente cada asignación, igual que hicimos con `UserRole`.

### ¿Quién es dueño de las relaciones?

`RolePermission`, porque contiene `role_id` y `permission_id`.

### ¿Por qué la combinación rol-permiso es única?

Para impedir asignaciones duplicadas.

### ¿Qué ocurre al retirar un permiso?

Se elimina `RolePermission`, pero permanecen `Role` y `Permission`.

### ¿Por qué usamos DTOs distintos según la dirección de la consulta?

`RolePermissionResponseDTO` muestra el permiso dentro de un rol, mientras que `PermissionRoleResponseDTO` muestra el rol dentro de un permiso. Reutilizar un único DTO obligaría a exponer información irrelevante o a duplicar consultas.

---

# 37. Evolución del proyecto

```text
User 1 ───── 0..1 Profile

User 1 ───── 0..N Session

User 1 ───── 0..N UserRole N..0 ───── 1 Role

Role 1 ───── 0..N RolePermission N..0 ───── 1 Permission
```

Conceptualmente:

```text
User N ───────── N Role N ───────── N Permission
```

Este diseño permite verificar si un usuario, a través de sus roles, posee el permiso necesario (url + method) antes de autorizar una operación.
