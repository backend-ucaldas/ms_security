# Guía práctica: consultas con Spring Data JPA en los repositorios

## 1. Propósito

Hasta ahora hemos utilizado los métodos básicos heredados de
`JpaRepository`:

~~~java
findAll();
findById(id);
save(entity);
deleteById(id);
existsById(id);
~~~

Sin embargo, una aplicación necesita responder preguntas más específicas:

~~~text
¿Existe un usuario con este email?
¿Qué usuarios contienen cierto texto en el nombre?
¿Qué roles tiene un usuario?
¿Qué permisos tiene cada rol?
¿Cuántos usuarios están registrados?
¿Cuál es el precio máximo, mínimo y promedio?
¿Cuántos usuarios tiene cada rol?
~~~

Spring Data JPA ofrece varias formas de construir estas consultas. Las
estudiaremos desde la más sencilla hasta la más flexible:

~~~text
1. Métodos heredados de JpaRepository
2. Consultas derivadas del nombre del método
3. Ordenamiento y paginación
4. Consultas JPQL con @Query
5. JOIN entre entidades
6. Proyecciones y DTO de reportes
7. COUNT, MAX, MIN, SUM y AVG
8. GROUP BY y HAVING
9. Consultas nativas SQL
10. Consultas dinámicas con Specification
~~~

---

## 2. Tres lenguajes que no debemos confundir

En los repositorios podemos encontrar tres formas diferentes de expresar una
consulta.

### 2.1 Método derivado

~~~java
Optional<User> findByEmail(String email);
~~~

Spring interpreta el nombre del método y construye la consulta.

### 2.2 JPQL

~~~java
@Query("""
        SELECT u
        FROM User u
        WHERE u.email = :email
        """)
Optional<User> searchByEmail(
        @Param("email") String email
);
~~~

JPQL utiliza:

* El nombre de la entidad: `User`.
* Los atributos Java: `email`.
* Las relaciones entre entidades.

### 2.3 SQL nativo

~~~java
@Query(
        value = """
                SELECT *
                FROM users
                WHERE email = :email
                """,
        nativeQuery = true
)
Optional<User> searchNativeByEmail(
        @Param("email") String email
);
~~~

SQL nativo utiliza:

* La tabla: `users`.
* Las columnas de la base de datos.
* La sintaxis particular de MySQL.

| Tipo | Trabaja con | Ejemplo |
| --- | --- | --- |
| Método derivado | Atributos Java | `findByEmail` |
| JPQL | Entidades y atributos | `FROM User u` |
| SQL nativo | Tablas y columnas | `FROM users` |

> La mayoría de las consultas debe comenzar con métodos derivados o JPQL. SQL
> nativo se reserva para casos que realmente necesitan características de la
> base de datos.

---

## 3. Repositorio básico

Un repositorio puede definirse así:

~~~java
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository
        extends JpaRepository<User, Long> {
}
~~~

`JpaRepository<User, Long>` indica:

~~~text
User → entidad administrada
Long → tipo de la llave primaria
~~~

Ya proporciona operaciones comunes:

| Método | Propósito |
| --- | --- |
| `findAll()` | Consultar todos los registros. |
| `findById(id)` | Buscar por llave primaria. |
| `existsById(id)` | Comprobar si existe. |
| `count()` | Contar todos los registros. |
| `save(entity)` | Crear o actualizar. |
| `deleteById(id)` | Eliminar por ID. |

Ejemplo:

~~~java
long totalUsers = userRepository.count();
~~~

---

## 3.1 Cómo construir una consulta a partir del nombre

Spring Data JPA analiza el nombre del método como si fuera una pequeña oración.
Una consulta derivada puede seguir esta estructura:

~~~text
[operación] [modificador] By [atributo] [operador]
                         [conector]
                         [atributo] [operador]
                         [ordenamiento]
~~~

Ejemplo:

~~~java
List<User> findTop5ByNameContainingIgnoreCaseOrderByEmailAsc(
        String text
);
~~~

Separación:

~~~text
find       → buscar
Top5       → limitar a cinco resultados
By         → comienza la condición
Name       → atributo User.name
Containing → contiene el texto
IgnoreCase → ignorar mayúsculas y minúsculas
OrderBy    → ordenar
Email      → atributo User.email
Asc        → ascendente
~~~

Interpretación conceptual en MySQL:

~~~sql
SELECT *
FROM users
WHERE LOWER(name) LIKE LOWER(CONCAT('%', ?, '%'))
ORDER BY email ASC
LIMIT 5;
~~~

> Hibernate puede generar un SQL con alias, parámetros y funciones ligeramente
> diferentes. El SQL mostrado en esta guía representa la intención de la
> consulta, no necesariamente el texto exacto enviado por Hibernate.

---

## 3.2 Regla fundamental: se escriben atributos Java

Para construir el método utilizamos el nombre del atributo de la entidad, no
el nombre de la columna en MySQL.

Entidad:

~~~java
@Column(name = "email_address")
private String email;
~~~

Método correcto:

~~~java
Optional<User> findByEmail(String email);
~~~

Método incorrecto:

~~~java
Optional<User> findByEmailAddress(String email);
~~~

La traducción es:

~~~text
Método derivado     → atributo Java: email
JPA/Hibernate       → conoce @Column(name = "email_address")
MySQL               → columna email_address
~~~

SQL conceptual:

~~~sql
SELECT *
FROM users
WHERE email_address = ?;
~~~

---

## 3.3 Palabras que indican la operación

Estas palabras aparecen antes de `By`:

| Palabra en el método | Interpretación | MySQL conceptual |
| --- | --- | --- |
| `find…By` | Buscar registros. | `SELECT ... WHERE` |
| `read…By` | Buscar registros. | `SELECT ... WHERE` |
| `get…By` | Buscar registros. | `SELECT ... WHERE` |
| `query…By` | Buscar registros. | `SELECT ... WHERE` |
| `search…By` | Buscar registros. | `SELECT ... WHERE` |
| `exists…By` | Comprobar existencia. | `SELECT ... EXISTS` o conteo optimizado |
| `count…By` | Contar coincidencias. | `SELECT COUNT(...)` |
| `delete…By` | Eliminar coincidencias. | `DELETE ... WHERE` |
| `remove…By` | Eliminar coincidencias. | `DELETE ... WHERE` |

Para mantener consistencia en el proyecto utilizaremos principalmente:

~~~text
findBy
existsBy
countBy
deleteBy
~~~

Aunque `findBy`, `readBy` y `getBy` pueden producir búsquedas semejantes,
no conviene mezclar nombres sin una razón.

---

## 3.4 Palabras para igualdad y diferencia

| Palabra JPA | Ejemplo | MySQL conceptual |
| --- | --- | --- |
| Sin palabra | `findByEmail` | `email = ?` |
| `Is` | `findByEmailIs` | `email = ?` |
| `Equals` | `findByEmailEquals` | `email = ?` |
| `Not` | `findByEmailNot` | `email <> ?` |
| `IsNot` | `findByEmailIsNot` | `email <> ?` |

Estas tres consultas expresan igualdad:

~~~java
findByEmail(String email);
findByEmailIs(String email);
findByEmailEquals(String email);
~~~

La forma más corta suele ser preferible:

~~~java
findByEmail(String email);
~~~

---

## 3.5 Palabras para búsquedas de texto

| Palabra JPA | Interpretación | MySQL conceptual |
| --- | --- | --- |
| `Containing` / `Contains` | Contiene el texto. | `LIKE '%texto%'` |
| `StartingWith` / `StartsWith` | Comienza con el texto. | `LIKE 'texto%'` |
| `EndingWith` / `EndsWith` | Termina con el texto. | `LIKE '%texto'` |
| `Like` | Usa el patrón recibido. | `LIKE ?` |
| `NotLike` | No coincide con el patrón. | `NOT LIKE ?` |
| `IgnoreCase` | Ignora mayúsculas/minúsculas. | Comparación con `LOWER` según proveedor |

Ejemplo:

~~~java
List<User> findByNameContainingIgnoreCase(
        String text
);
~~~

Construcción:

~~~text
find + By + Name + Containing + IgnoreCase
            │
            └── atributo User.name
~~~

MySQL conceptual:

~~~sql
SELECT *
FROM users
WHERE LOWER(name) LIKE LOWER(CONCAT('%', ?, '%'));
~~~

---

## 3.6 Palabras para comparaciones y rangos

| Palabra JPA | MySQL conceptual |
| --- | --- |
| `GreaterThan` | `> ?` |
| `GreaterThanEqual` | `>= ?` |
| `LessThan` | `< ?` |
| `LessThanEqual` | `<= ?` |
| `Between` | `BETWEEN ? AND ?` |
| `After` | `> ?` para fechas |
| `Before` | `< ?` para fechas |
| `In` | `IN (?, ?, ...)` |
| `NotIn` | `NOT IN (?, ?, ...)` |

Ejemplo con fechas:

~~~java
List<Session> findByExpirationBetween(
        Instant start,
        Instant end
);
~~~

Construcción:

~~~text
find + By + Expiration + Between
            │              │
            │              └── requiere dos parámetros
            └── atributo Session.expiration
~~~

MySQL conceptual:

~~~sql
SELECT *
FROM sessions
WHERE expiration BETWEEN ? AND ?;
~~~

Ejemplo con una colección:

~~~java
List<User> findByIdIn(
        Collection<Long> ids
);
~~~

MySQL conceptual:

~~~sql
SELECT *
FROM users
WHERE id IN (?, ?, ?);
~~~

---

## 3.7 Nulos, valores booleanos y colecciones

| Palabra JPA | Interpretación | MySQL conceptual |
| --- | --- | --- |
| `IsNull` / `Null` | El atributo es nulo. | `IS NULL` |
| `IsNotNull` / `NotNull` | El atributo no es nulo. | `IS NOT NULL` |
| `True` / `IsTrue` | El atributo es verdadero. | `= TRUE` |
| `False` / `IsFalse` | El atributo es falso. | `= FALSE` |
| `IsEmpty` / `Empty` | La colección está vacía. | `NOT EXISTS` según traducción |
| `IsNotEmpty` / `NotEmpty` | La colección tiene elementos. | `EXISTS` según traducción |

Si en el futuro `User` tiene:

~~~java
private Boolean enabled;
~~~

podríamos escribir:

~~~java
List<User> findByEnabledTrue();
~~~

MySQL conceptual:

~~~sql
SELECT *
FROM users
WHERE enabled = TRUE;
~~~

Ejemplo con nulos:

~~~java
List<Profile> findByPhoneIsNull();
~~~

MySQL conceptual:

~~~sql
SELECT *
FROM profiles
WHERE phone IS NULL;
~~~

---

## 3.8 Conectores lógicos

| Palabra JPA | MySQL |
| --- | --- |
| `And` | `AND` |
| `Or` | `OR` |

Ejemplo:

~~~java
Optional<User> findByNameAndEmail(
        String name,
        String email
);
~~~

Construcción:

~~~text
find + By + Name + And + Email
            │           │
            │           └── segundo atributo
            └── primer atributo
~~~

MySQL conceptual:

~~~sql
SELECT *
FROM users
WHERE name = ?
  AND email = ?;
~~~

Ejemplo con `Or`:

~~~java
List<User> findByNameContainingOrEmailContaining(
        String name,
        String email
);
~~~

MySQL conceptual:

~~~sql
SELECT *
FROM users
WHERE name LIKE CONCAT('%', ?, '%')
   OR email LIKE CONCAT('%', ?, '%');
~~~

Cuando una consulta mezcla varios `And` y `Or`, el nombre puede ser difícil
de interpretar. En ese caso es mejor escribir JPQL con paréntesis explícitos.

---

## 3.9 Orden, límite y resultados diferentes

| Palabra JPA | Interpretación | MySQL conceptual |
| --- | --- | --- |
| `OrderBy` | Comienza el ordenamiento. | `ORDER BY` |
| `Asc` | Orden ascendente. | `ASC` |
| `Desc` | Orden descendente. | `DESC` |
| `First` / `Top` | Primer resultado. | `LIMIT 1` |
| `First5` / `Top5` | Primeros cinco. | `LIMIT 5` |
| `Distinct` | Evita repetidos. | `SELECT DISTINCT` |

Ejemplo:

~~~java
List<User> findTop5ByOrderByNameAsc();
~~~

Construcción:

~~~text
find + Top5 + By + OrderBy + Name + Asc
       │                    │      │
       │                    │      └── dirección
       │                    └── atributo de orden
       └── límite
~~~

MySQL conceptual:

~~~sql
SELECT *
FROM users
ORDER BY name ASC
LIMIT 5;
~~~

Ejemplo con `Distinct`:

~~~java
List<User> findDistinctByNameContaining(
        String text
);
~~~

MySQL conceptual:

~~~sql
SELECT DISTINCT *
FROM users
WHERE name LIKE CONCAT('%', ?, '%');
~~~

---

## 3.10 Navegar atributos relacionados

Para una relación:

~~~text
User
  └── profile
         └── phone
~~~

podemos construir:

~~~java
List<User> findByProfilePhoneContaining(
        String phone
);
~~~

Spring interpreta:

~~~text
Profile → atributo User.profile
Phone   → atributo Profile.phone
~~~

MySQL conceptual:

~~~sql
SELECT u.*
FROM users u
INNER JOIN profiles p
        ON p.user_id = u.id
WHERE p.phone LIKE CONCAT('%', ?, '%');
~~~

También podemos separar visualmente la navegación con guion bajo:

~~~java
List<User> findByProfile_PhoneContaining(
        String phone
);
~~~

El guion bajo ayuda a señalar el cambio de entidad:

~~~text
Profile_Phone
   │      │
   │      └── atributo de Profile
   └── relación de User
~~~

Para:

~~~text
User 1 ─── N Session
Session.expiration
~~~

podemos escribir:

~~~java
List<User> findDistinctBySessions_ExpirationAfter(
        Instant date
);
~~~

MySQL conceptual:

~~~sql
SELECT DISTINCT u.*
FROM users u
INNER JOIN sessions s
        ON s.user_id = u.id
WHERE s.expiration > ?;
~~~

---

## 3.11 Catálogo: método derivado e interpretación MySQL

| Método de repositorio | Interpretación MySQL |
| --- | --- |
| `findByEmail(email)` | `WHERE email = ?` |
| `findByEmailNot(email)` | `WHERE email <> ?` |
| `findByNameContaining(text)` | `WHERE name LIKE '%texto%'` |
| `findByNameStartingWith(text)` | `WHERE name LIKE 'texto%'` |
| `findByEmailEndingWith(text)` | `WHERE email LIKE '%texto'` |
| `findByExpirationAfter(date)` | `WHERE expiration > ?` |
| `findByExpirationBefore(date)` | `WHERE expiration < ?` |
| `findByExpirationBetween(a,b)` | `WHERE expiration BETWEEN ? AND ?` |
| `findByIdIn(ids)` | `WHERE id IN (...)` |
| `findByPhoneIsNull()` | `WHERE phone IS NULL` |
| `findByNameAndEmail(a,b)` | `WHERE name = ? AND email = ?` |
| `findByNameOrEmail(a,b)` | `WHERE name = ? OR email = ?` |
| `existsByEmail(email)` | Comprobar si existe `WHERE email = ?` |
| `countByEmailEndingWith(d)` | `SELECT COUNT(*) ... WHERE email LIKE ?` |
| `findTop5ByOrderByNameAsc()` | `ORDER BY name ASC LIMIT 5` |
| `findDistinctBySessions_ExpirationAfter(d)` | `SELECT DISTINCT` con `JOIN sessions` |

---

## 3.12 Método paso a paso para diseñar una consulta

Antes de escribir el nombre, responda estas preguntas:

~~~text
1. ¿Qué entidad quiero obtener?
2. ¿Espero uno, varios, una existencia o un conteo?
3. ¿Sobre qué atributo se aplica la condición?
4. ¿Qué operador necesito?
5. ¿Existen más condiciones?
6. ¿Necesito ordenar o limitar?
~~~

### Ejemplo A: buscar usuarios por dominio de correo

Necesidad:

> Obtener todos los usuarios cuyo email termine en `@ucaldas.edu.co`.

Paso 1. Entidad:

~~~text
User
~~~

Paso 2. Resultado:

~~~text
pueden ser varios → List<User>
~~~

Paso 3. Operación:

~~~text
buscar → find
~~~

Paso 4. Atributo:

~~~text
User.email → Email
~~~

Paso 5. Operador:

~~~text
termina en → EndingWith
~~~

Construcción:

~~~text
find + By + Email + EndingWith
~~~

Método final:

~~~java
List<User> findByEmailEndingWith(
        String domain
);
~~~

Invocación:

~~~java
findByEmailEndingWith("@ucaldas.edu.co");
~~~

MySQL conceptual:

~~~sql
SELECT *
FROM users
WHERE email LIKE '%@ucaldas.edu.co';
~~~

### Ejemplo B: contar sesiones que vencen después de una fecha

Necesidad:

> Conocer cuántas sesiones expiran después del momento actual.

Descomposición:

~~~text
contar          → count
comienzo filtro → By
atributo        → Expiration
comparación     → After
~~~

Construcción:

~~~text
count + By + Expiration + After
~~~

Método final:

~~~java
long countByExpirationAfter(
        Instant date
);
~~~

MySQL conceptual:

~~~sql
SELECT COUNT(*)
FROM sessions
WHERE expiration > ?;
~~~

### Ejemplo C: validar un email durante la actualización

Necesidad:

> Comprobar si existe otro usuario con el mismo email, excluyendo al usuario
> que se está actualizando.

Descomposición:

~~~text
comprobar existencia → exists
comienzo filtro      → By
primer atributo      → Email
conector             → And
segundo atributo     → Id
operador             → Not
~~~

Construcción:

~~~text
exists + By + Email + And + Id + Not
~~~

Método final:

~~~java
boolean existsByEmailAndIdNot(
        String email,
        Long id
);
~~~

MySQL conceptual:

~~~sql
SELECT EXISTS (
    SELECT 1
    FROM users
    WHERE email = ?
      AND id <> ?
);
~~~

### Ejemplo D: buscar usuarios con sesiones futuras y ordenar por nombre

Necesidad:

> Obtener sin duplicados los usuarios que tengan alguna sesión vigente,
> ordenados alfabéticamente.

Descomposición:

~~~text
buscar              → find
evitar repetidos    → Distinct
comienzo filtro     → By
relación            → Sessions
atributo relacionado→ Expiration
operador fecha      → After
ordenar             → OrderBy
atributo de orden   → Name
dirección           → Asc
~~~

Método final:

~~~java
List<User>
        findDistinctBySessions_ExpirationAfterOrderByNameAsc(
                Instant date
        );
~~~

MySQL conceptual:

~~~sql
SELECT DISTINCT u.*
FROM users u
INNER JOIN sessions s
        ON s.user_id = u.id
WHERE s.expiration > ?
ORDER BY u.name ASC;
~~~

---

## 4. Primera consulta derivada: igualdad

Supongamos que `User` tiene:

~~~text
id
name
email
password
~~~

Para buscar por email escribimos:

~~~java
Optional<User> findByEmail(String email);
~~~

Spring separa el nombre:

~~~text
find  By  Email
│         │
│         └── atributo de User
└── operación de búsqueda
~~~

Conceptualmente genera:

~~~sql
SELECT *
FROM users
WHERE email = ?;
~~~

### ¿Por qué devuelve `Optional<User>`?

Porque puede ocurrir:

~~~text
Usuario encontrado     → Optional con User
Usuario no encontrado  → Optional.empty()
~~~

Uso:

~~~java
User user = userRepository
        .findByEmail(email)
        .orElseThrow(
                () -> new ResourceNotFoundException(
                        "Usuario no encontrado"
                )
        );
~~~

---

## 5. Elegir el tipo de retorno

| Retorno | Cuándo utilizarlo |
| --- | --- |
| `Optional<User>` | Esperamos cero o un resultado. |
| `User` | Esperamos exactamente uno, pero puede devolver `null`. |
| `List<User>` | Pueden existir cero, uno o muchos. |
| `boolean` | Solo interesa saber si existe. |
| `long` | Solo interesa la cantidad. |
| `Page<User>` | Necesitamos resultados y total de páginas. |
| `Slice<User>` | Necesitamos saber si hay una página siguiente, no el total. |

Ejemplos:

~~~java
Optional<User> findByEmail(String email);

List<User> findByName(String name);

boolean existsByEmail(String email);

long countByName(String name);
~~~

Si un método retorna `Optional<User>` pero la consulta encuentra más de un
registro, se produce un error. Por eso el email debe tener una restricción
única.

---

## 6. Consultas con texto

### Coincidencia exacta

~~~java
List<User> findByName(String name);
~~~

### Contiene

~~~java
List<User> findByNameContaining(String text);
~~~

Conceptualmente:

~~~sql
WHERE name LIKE '%texto%'
~~~

### Comienza por

~~~java
List<User> findByNameStartingWith(String prefix);
~~~

### Termina en

~~~java
List<User> findByEmailEndingWith(String domain);
~~~

Uso:

~~~java
List<User> users =
        userRepository.findByEmailEndingWith(
                "@ucaldas.edu.co"
        );
~~~

### Ignorar mayúsculas y minúsculas

~~~java
Optional<User> findByEmailIgnoreCase(String email);

List<User> findByNameContainingIgnoreCase(
        String text
);
~~~

---

## 7. Operadores de comparación

Los métodos derivados soportan palabras clave que representan operadores.

| Palabra | Significado |
| --- | --- |
| `GreaterThan` | Mayor que. |
| `GreaterThanEqual` | Mayor o igual. |
| `LessThan` | Menor que. |
| `LessThanEqual` | Menor o igual. |
| `Between` | Dentro de un intervalo. |
| `After` | Fecha posterior. |
| `Before` | Fecha anterior. |
| `In` | Está dentro de una colección. |
| `NotIn` | No está dentro de una colección. |
| `IsNull` | Es nulo. |
| `IsNotNull` | No es nulo. |

Ejemplos con `Session.expiration`:

~~~java
List<Session> findByExpirationAfter(
        Instant date
);

List<Session> findByExpirationBefore(
        Instant date
);

List<Session> findByExpirationBetween(
        Instant start,
        Instant end
);
~~~

Ejemplo con varios IDs:

~~~java
List<User> findByIdIn(
        Collection<Long> ids
);
~~~

---

## 8. Combinar condiciones

### Usar `And`

~~~java
Optional<User> findByNameAndEmail(
        String name,
        String email
);
~~~

Conceptualmente:

~~~sql
WHERE name = ?
  AND email = ?
~~~

### Usar `Or`

~~~java
List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String name,
        String email
);
~~~

Conceptualmente:

~~~sql
WHERE LOWER(name) LIKE LOWER(?)
   OR LOWER(email) LIKE LOWER(?)
~~~

Los nombres pueden volverse difíciles de leer. Cuando un método resulta
demasiado largo, conviene cambiar a `@Query` o a una consulta dinámica.

---

## 9. Existencia y conteo

### Comprobar existencia

~~~java
boolean existsByEmail(String email);
~~~

Se utiliza, por ejemplo, antes de registrar un usuario:

~~~java
if (userRepository.existsByEmail(dto.getEmail())) {
    throw new DuplicateResourceException(
            "El email ya está registrado"
    );
}
~~~

Para actualización:

~~~java
boolean existsByEmailAndIdNot(
        String email,
        Long id
);
~~~

Esto pregunta:

~~~text
¿Existe otro usuario con este email
y con un ID diferente al usuario actualizado?
~~~

### Contar por condición

~~~java
long countByName(String name);

long countByEmailEndingWith(String domain);
~~~

---

## 10. Ordenamiento, primeros resultados y valores distintos

### Ordenamiento incluido en el nombre

~~~java
List<User> findAllByOrderByNameAsc();

List<User> findAllByOrderByNameDesc();

List<User> findByNameContainingIgnoreCaseOrderByEmailAsc(
        String text
);
~~~

### Primer resultado

~~~java
Optional<User> findFirstByOrderByIdAsc();

Optional<User> findTopByOrderByIdDesc();
~~~

### Primeros cinco

~~~java
List<User> findTop5ByOrderByNameAsc();
~~~

### Evitar duplicados

~~~java
List<User> findDistinctByNameContainingIgnoreCase(
        String text
);
~~~

---

## 11. Ordenamiento dinámico con `Sort`

En lugar de crear un método por cada orden, podemos recibir `Sort`:

~~~java
List<User> findByNameContainingIgnoreCase(
        String text,
        Sort sort
);
~~~

Uso:

~~~java
List<User> users =
        userRepository
                .findByNameContainingIgnoreCase(
                        "ana",
                        Sort.by(
                                Sort.Direction.ASC,
                                "email"
                        )
                );
~~~

El nombre `email` es un atributo Java de `User`.

> Si el campo de ordenamiento viene del cliente, debe validarse mediante una
> lista permitida. No se deben aceptar nombres arbitrarios sin control.

---

## 12. Paginación

Para evitar devolver miles de registros:

~~~java
Page<User> findByNameContainingIgnoreCase(
        String text,
        Pageable pageable
);
~~~

Uso en el Service:

~~~java
Pageable pageable = PageRequest.of(
        0,
        10,
        Sort.by("name").ascending()
);

Page<User> page =
        userRepository
                .findByNameContainingIgnoreCase(
                        "ana",
                        pageable
                );
~~~

`Page<User>` permite conocer:

~~~java
page.getContent();
page.getTotalElements();
page.getTotalPages();
page.getNumber();
page.getSize();
page.hasNext();
page.hasPrevious();
~~~

### `Page` frente a `Slice`

| Tipo | Ejecuta conteo total | Uso |
| --- | --- | --- |
| `Page` | Sí | Interfaces con número total de páginas. |
| `Slice` | No | Botón “cargar más” o desplazamiento continuo. |

~~~java
Slice<User> findByEmailEndingWith(
        String domain,
        Pageable pageable
);
~~~

---

## 13. Navegar relaciones mediante el nombre

Spring también puede interpretar atributos relacionados.

Supongamos:

~~~text
User 1 ─── 1 Profile
Profile.phone
~~~

Podemos escribir:

~~~java
List<User> findByProfilePhoneContaining(
        String phone
);
~~~

Spring interpreta:

~~~text
User.profile.phone
~~~

Para una relación 1–N:

~~~text
User 1 ─── N Session
Session.expiration
~~~

~~~java
List<User> findDistinctBySessionsExpirationAfter(
        Instant date
);
~~~

`Distinct` evita que un mismo usuario aparezca varias veces si tiene varias
sesiones que cumplen la condición.

Cuando se atraviesan muchas relaciones, el nombre puede volverse inmanejable.
En ese momento conviene utilizar JPQL.

---

## 14. Primera consulta con `@Query`

La consulta derivada:

~~~java
Optional<User> findByEmail(String email);
~~~

puede expresarse en JPQL:

~~~java
@Query("""
        SELECT u
        FROM User u
        WHERE u.email = :email
        """)
Optional<User> searchByEmail(
        @Param("email") String email
);
~~~

Elementos:

~~~text
User   → nombre de la entidad
u      → alias
email  → atributo Java
:email → parámetro nombrado
~~~

No escribimos:

~~~sql
SELECT *
FROM users
~~~

porque eso sería SQL, no JPQL.

---

## 15. Búsqueda con varios filtros en JPQL

~~~java
@Query("""
        SELECT u
        FROM User u
        WHERE LOWER(u.name)
              LIKE LOWER(CONCAT('%', :text, '%'))
           OR LOWER(u.email)
              LIKE LOWER(CONCAT('%', :text, '%'))
        ORDER BY u.name ASC
        """)
List<User> searchByNameOrEmail(
        @Param("text") String text
);
~~~

Este método es más legible que:

~~~java
findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(...)
~~~

---

## 16. Filtros opcionales

Podemos permitir que un parámetro sea opcional:

~~~java
@Query("""
        SELECT u
        FROM User u
        WHERE (:name IS NULL
               OR LOWER(u.name)
                  LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:email IS NULL
               OR LOWER(u.email)
                  LIKE LOWER(CONCAT('%', :email, '%')))
        """)
Page<User> search(
        @Param("name") String name,
        @Param("email") String email,
        Pageable pageable
);
~~~

Si `name` es `null`, ese filtro no se aplica. Esta estrategia sirve para
pocos filtros. Cuando existen muchos, `Specification` suele ser más clara.

---

## 17. `JOIN` entre entidades

Supongamos las relaciones:

~~~text
User
  ↓ userRoles
UserRole
  ↓ role
Role
~~~

Para consultar los roles de un usuario:

~~~java
@Query("""
        SELECT DISTINCT r
        FROM UserRole ur
        JOIN ur.user u
        JOIN ur.role r
        WHERE u.id = :userId
        ORDER BY r.name ASC
        """)
List<Role> findRolesByUserId(
        @Param("userId") Long userId
);
~~~

No se utilizan nombres de tablas ni llaves foráneas. JPQL navega las relaciones:

~~~text
ur.user
ur.role
~~~

---

## 18. Consultar usuarios por rol

~~~java
@Query("""
        SELECT DISTINCT u
        FROM UserRole ur
        JOIN ur.user u
        JOIN ur.role r
        WHERE LOWER(r.name) = LOWER(:roleName)
        ORDER BY u.name ASC
        """)
List<User> findUsersByRoleName(
        @Param("roleName") String roleName
);
~~~

Uso:

~~~java
List<User> admins =
        userRepository
                .findUsersByRoleName("ADMIN");
~~~

---

## 19. Consultar permisos de un usuario

Con el modelo:

~~~text
User → UserRole → Role → RolePermission → Permission
~~~

la consulta puede ser:

~~~java
@Query("""
        SELECT DISTINCT p
        FROM Permission p
        JOIN p.rolePermissions rp
        JOIN rp.role r
        JOIN r.userRoles ur
        WHERE ur.user.id = :userId
        ORDER BY p.method, p.url
        """)
List<Permission> findPermissionsByUserId(
        @Param("userId") Long userId
);
~~~

> Los nombres usados después del punto son atributos Java. Si en tus entidades
> se llaman de otra manera, la consulta debe ajustarse.

---

## 20. `INNER JOIN` y `LEFT JOIN`

### `INNER JOIN`

Solo devuelve entidades que tienen relación:

~~~java
@Query("""
        SELECT DISTINCT r
        FROM Role r
        JOIN r.rolePermissions rp
        """)
List<Role> findRolesWithPermissions();
~~~

### `LEFT JOIN`

También incluye entidades sin relación:

~~~java
@Query("""
        SELECT DISTINCT r
        FROM Role r
        LEFT JOIN r.rolePermissions rp
        """)
List<Role> findAllRolesIncludingEmpty();
~~~

| JOIN | Incluye roles sin permisos |
| --- |:---:|
| `JOIN` / `INNER JOIN` | No |
| `LEFT JOIN` | Sí |

---

## 21. `JOIN FETCH` y el problema N+1

Supongamos que consultamos usuarios y después recorremos sus roles:

~~~java
List<User> users = userRepository.findAll();

for (User user : users) {
    user.getUserRoles().size();
}
~~~

Puede ocurrir:

~~~text
1 consulta para usuarios
N consultas adicionales para sus roles
~~~

Esto se conoce como problema N+1.

Podemos cargar la relación en una consulta:

~~~java
@Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.userRoles ur
        LEFT JOIN FETCH ur.role
        """)
List<User> findAllWithRoles();
~~~

`JOIN FETCH` no es necesario en todos los casos. Debe utilizarse cuando
realmente necesitaremos la relación en esa operación.

> Se debe tener cuidado al combinar colecciones con `JOIN FETCH` y
> paginación, porque puede duplicar filas y afectar el conteo.

---

## 22. Proyecciones: devolver solo lo necesario

No siempre necesitamos retornar la entidad completa.

### Proyección mediante interfaz

~~~java
public interface UserSummaryProjection {

    Long getId();

    String getName();

    String getEmail();
}
~~~

Repositorio:

~~~java
List<UserSummaryProjection>
        findByNameContainingIgnoreCase(String text);
~~~

La contraseña no forma parte de la proyección.

### DTO mediante constructor JPQL

~~~java
package com.uc.ms_security.dto.report;

public record UserSummaryDTO(
        Long id,
        String name,
        String email) {
}
~~~

Consulta:

~~~java
@Query("""
        SELECT new com.uc.ms_security.dto.report.UserSummaryDTO(
                u.id,
                u.name,
                u.email
        )
        FROM User u
        ORDER BY u.name
        """)
List<UserSummaryDTO> findUserSummaries();
~~~

En una expresión `new` de JPQL se escribe el nombre completo del DTO.

---

## 23. Funciones de agregación

Las funciones principales son:

| Función | Resultado |
| --- | --- |
| `COUNT` | Cantidad de registros. |
| `MAX` | Valor máximo. |
| `MIN` | Valor mínimo. |
| `SUM` | Suma. |
| `AVG` | Promedio. |

No todas tienen sentido sobre cualquier atributo. Por ejemplo:

~~~text
COUNT usuarios          → tiene sentido
MAX fecha de expiración → tiene sentido
AVG precio              → tiene sentido
AVG ID de usuario       → técnicamente posible, pero sin valor de negocio
~~~

---

## 24. Conteos en el sistema de seguridad

### Total de usuarios

Podemos usar el método heredado:

~~~java
long total = userRepository.count();
~~~

O JPQL:

~~~java
@Query("""
        SELECT COUNT(u)
        FROM User u
        """)
long countAllUsers();
~~~

### Cantidad de usuarios por rol

~~~java
@Query("""
        SELECT COUNT(DISTINCT ur.user.id)
        FROM UserRole ur
        WHERE ur.role.id = :roleId
        """)
long countUsersByRoleId(
        @Param("roleId") Long roleId
);
~~~

### Cantidad de permisos por rol

~~~java
@Query("""
        SELECT COUNT(DISTINCT rp.permission.id)
        FROM RolePermission rp
        WHERE rp.role.id = :roleId
        """)
long countPermissionsByRoleId(
        @Param("roleId") Long roleId
);
~~~

---

## 25. Mínimos y máximos con fechas

Con `Session.expiration` podemos consultar:

### Expiración más cercana

~~~java
@Query("""
        SELECT MIN(s.expiration)
        FROM Session s
        """)
Instant findEarliestExpiration();
~~~

### Expiración más lejana

~~~java
@Query("""
        SELECT MAX(s.expiration)
        FROM Session s
        """)
Instant findLatestExpiration();
~~~

Si no existen sesiones, la función puede devolver `null`. Podemos declarar:

~~~java
Optional<Instant> findEarliestExpiration();
~~~

---

## 26. Ejemplo numérico para `SUM`, `AVG`, `MIN` y `MAX`

Las entidades actuales de seguridad no tienen un campo numérico sobre el que
sea útil calcular precio promedio o suma. Para aprender estas funciones,
utilizaremos una entidad auxiliar:

~~~java
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    private String name;

    private BigDecimal price;

    private Integer stock;
}
~~~

`ProductRepository`:

~~~java
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    @Query("SELECT MIN(p.price) FROM Product p")
    BigDecimal findMinimumPrice();

    @Query("SELECT MAX(p.price) FROM Product p")
    BigDecimal findMaximumPrice();

    @Query("SELECT AVG(p.price) FROM Product p")
    Double findAveragePrice();

    @Query("SELECT SUM(p.stock) FROM Product p")
    Long findTotalStock();
}
~~~

> El tipo exacto devuelto por una agregación puede depender del tipo del
> atributo y del proveedor JPA. Para dinero, conviene revisar el tipo generado
> y evitar cálculos financieros posteriores con `double`.

---

## 27. Crear un DTO de reporte

En lugar de realizar cuatro consultas, podemos construir un reporte:

~~~java
package com.uc.ms_security.dto.report;

import java.math.BigDecimal;

public record ProductStatisticsDTO(
        Long totalProducts,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        Double averagePrice,
        Long totalStock) {
}
~~~

Utilizamos tipos envoltorio porque `MIN`, `MAX`, `AVG` y `SUM` pueden
devolver `null` cuando la tabla no contiene registros.

Consulta:

~~~java
@Query("""
        SELECT new com.uc.ms_security.dto.report.ProductStatisticsDTO(
                COUNT(p),
                MIN(p.price),
                MAX(p.price),
                AVG(p.price),
                SUM(p.stock)
        )
        FROM Product p
        """)
ProductStatisticsDTO getStatistics();
~~~

Resultado conceptual:

~~~json
{
  "totalProducts": 25,
  "minimumPrice": 12000.00,
  "maximumPrice": 950000.00,
  "averagePrice": 182500.50,
  "totalStock": 430
}
~~~

---

## 28. `GROUP BY`: agrupar resultados

Queremos responder:

> ¿Cuántos usuarios tiene cada rol?

DTO:

~~~java
package com.uc.ms_security.dto.report;

public record RoleUserCountDTO(
        Long roleId,
        String roleName,
        long userCount) {
}
~~~

Consulta:

~~~java
@Query("""
        SELECT new com.uc.ms_security.dto.report.RoleUserCountDTO(
                r.id,
                r.name,
                COUNT(DISTINCT ur.user.id)
        )
        FROM Role r
        LEFT JOIN r.userRoles ur
        GROUP BY r.id, r.name
        ORDER BY COUNT(DISTINCT ur.user.id) DESC
        """)
List<RoleUserCountDTO> countUsersByRole();
~~~

`LEFT JOIN` permite incluir roles con cero usuarios.

Resultado:

| Rol | Usuarios |
| --- | ---: |
| ADMIN | 12 |
| USER | 87 |
| AUDITOR | 0 |

---

## 29. Reporte de permisos por rol

DTO:

~~~java
public record RolePermissionCountDTO(
        Long roleId,
        String roleName,
        long permissionCount) {
}
~~~

Consulta:

~~~java
@Query("""
        SELECT new com.uc.ms_security.dto.report.RolePermissionCountDTO(
                r.id,
                r.name,
                COUNT(DISTINCT rp.permission.id)
        )
        FROM Role r
        LEFT JOIN r.rolePermissions rp
        GROUP BY r.id, r.name
        ORDER BY r.name
        """)
List<RolePermissionCountDTO>
        countPermissionsByRole();
~~~

---

## 30. `HAVING`: filtrar grupos

`WHERE` filtra registros antes de agrupar. `HAVING` filtra los grupos
después de calcular la agregación.

Ejemplo: roles con al menos cinco usuarios.

~~~java
@Query("""
        SELECT new com.uc.ms_security.dto.report.RoleUserCountDTO(
                r.id,
                r.name,
                COUNT(DISTINCT ur.user.id)
        )
        FROM Role r
        JOIN r.userRoles ur
        GROUP BY r.id, r.name
        HAVING COUNT(DISTINCT ur.user.id) >= :minimum
        ORDER BY COUNT(DISTINCT ur.user.id) DESC
        """)
List<RoleUserCountDTO> findRolesWithMinimumUsers(
        @Param("minimum") long minimum
);
~~~

| Cláusula | Momento |
| --- | --- |
| `WHERE` | Antes de agrupar. |
| `GROUP BY` | Forma los grupos. |
| `HAVING` | Después de agrupar. |

---

## 31. Subconsultas

Una subconsulta es una consulta dentro de otra.

Ejemplo: usuarios que tienen asignado al menos un rol.

~~~java
@Query("""
        SELECT u
        FROM User u
        WHERE EXISTS (
                SELECT ur.id
                FROM UserRole ur
                WHERE ur.user = u
        )
        """)
List<User> findUsersWithRoles();
~~~

Usuarios sin roles:

~~~java
@Query("""
        SELECT u
        FROM User u
        WHERE NOT EXISTS (
                SELECT ur.id
                FROM UserRole ur
                WHERE ur.user = u
        )
        """)
List<User> findUsersWithoutRoles();
~~~

---

## 32. Consultas nativas

Una consulta nativa utiliza SQL real:

~~~java
@Query(
        value = """
                SELECT u.*
                FROM users u
                INNER JOIN user_roles ur
                    ON ur.user_id = u.id
                WHERE ur.role_id = :roleId
                """,
        nativeQuery = true
)
List<User> findUsersByRoleNative(
        @Param("roleId") Long roleId
);
~~~

Ventajas:

* Permite usar funciones específicas de MySQL.
* Ofrece control directo del SQL.
* Facilita consultas muy particulares o heredadas.

Desventajas:

* Depende de nombres de tablas y columnas.
* Es menos portable a otra base de datos.
* Los cambios del modelo pueden romper la consulta.
* El mapeo de resultados puede requerir más trabajo.

No debemos usar `nativeQuery = true` solo porque JPQL parezca nuevo. Primero
debemos evaluar si un método derivado o JPQL resuelve el problema.

---

## 33. Actualizaciones y eliminaciones con `@Modifying`

`@Query` también puede ejecutar cambios:

~~~java
@Modifying
@Transactional
@Query("""
        UPDATE User u
        SET u.name = :name
        WHERE u.id = :id
        """)
int updateName(
        @Param("id") Long id,
        @Param("name") String name
);
~~~

El retorno `int` indica cuántas filas fueron afectadas.

Eliminar sesiones vencidas:

~~~java
@Modifying
@Transactional
@Query("""
        DELETE FROM Session s
        WHERE s.expiration < :now
        """)
int deleteExpiredSessions(
        @Param("now") Instant now
);
~~~

Para `UPDATE` y `DELETE` declarados necesitamos:

~~~java
@Modifying
@Transactional
~~~

Para operaciones CRUD normales suele ser preferible mantener la transacción en
el Service.

---

## 34. Consultas dinámicas con `Specification`

Supongamos un buscador cuyos filtros son opcionales:

~~~text
name
email
role
~~~

Crear un método diferente para cada combinación no escala bien:

~~~text
findByName
findByEmail
findByNameAndEmail
findByNameAndRole
findByEmailAndRole
findByNameAndEmailAndRole
~~~

Podemos extender:

~~~java
public interface UserRepository
        extends JpaRepository<User, Long>,
                JpaSpecificationExecutor<User> {
}
~~~

Especificación para nombre:

~~~java
public static Specification<User> nameContains(
        String name) {

    return (root, query, builder) -> {
        if (name == null || name.isBlank()) {
            return builder.conjunction();
        }

        return builder.like(
                builder.lower(root.get("name")),
                "%" + name.toLowerCase() + "%"
        );
    };
}
~~~

Especificación para email:

~~~java
public static Specification<User> emailContains(
        String email) {

    return (root, query, builder) -> {
        if (email == null || email.isBlank()) {
            return builder.conjunction();
        }

        return builder.like(
                builder.lower(root.get("email")),
                "%" + email.toLowerCase() + "%"
        );
    };
}
~~~

Composición:

~~~java
Specification<User> specification =
        Specification
                .where(nameContains(name))
                .and(emailContains(email));

Page<User> result =
        userRepository.findAll(
                specification,
                pageable
        );
~~~

`Specification` es apropiada cuando la consulta cambia según filtros
opcionales.

---

## 35. ¿Qué técnica debemos escoger?

| Situación | Técnica recomendada |
| --- | --- |
| CRUD básico | Métodos heredados. |
| Búsqueda sencilla por atributos | Método derivado. |
| Existencia o conteo sencillo | `existsBy`, `countBy`. |
| Orden y paginación | `Sort`, `Pageable`. |
| Nombre derivado demasiado largo | JPQL con `@Query`. |
| Relación entre entidades | JPQL con `JOIN`. |
| Evitar N+1 en un caso concreto | `JOIN FETCH` o `EntityGraph`. |
| Reporte parcial | Proyección o DTO. |
| Conteos y estadísticas | Agregaciones y `GROUP BY`. |
| Muchos filtros opcionales | `Specification`. |
| Función exclusiva de MySQL | SQL nativo. |
| Actualización masiva | `@Modifying` y transacción. |

Regla pedagógica:

~~~text
Empiece por la solución más sencilla
        ↓
si deja de ser clara o suficiente
        ↓
avance al siguiente nivel
~~~

---

## 36. Errores frecuentes

### Usar nombres de columnas en JPQL

Incorrecto:

~~~java
FROM users u
WHERE u.user_name = :name
~~~

Correcto:

~~~java
FROM User u
WHERE u.name = :name
~~~

### Esperar un único resultado cuando pueden existir varios

~~~java
Optional<User> findByName(String name);
~~~

Si varios usuarios pueden tener el mismo nombre, debe ser:

~~~java
List<User> findByName(String name);
~~~

### Crear nombres de métodos imposibles de leer

Si el método ocupa varias líneas y mezcla muchos `And` y `Or`, utilice
`@Query` o `Specification`.

### Retornar entidades completas para un reporte

Un reporte debe usar proyecciones o DTO cuando solo necesita algunos campos.

### Promediar datos sin significado

`AVG(id)` puede ser válido técnicamente, pero no responde una pregunta útil
del negocio.

### Usar `JOIN FETCH` en todo

Puede cargar demasiados datos, duplicar filas y complicar la paginación.

### Exponer ordenamiento arbitrario

Los atributos recibidos desde el cliente deben validarse contra una lista de
campos permitidos.

---

## 37. Ejercicios

### Nivel 1: métodos derivados

1. Buscar usuario por email.
2. Buscar usuarios cuyo nombre contenga un texto.
3. Buscar emails que terminen en un dominio.
4. Comprobar si un email existe.
5. Contar usuarios de un dominio.
6. Obtener los cinco primeros usuarios ordenados por nombre.

### Nivel 2: fechas, orden y paginación

7. Buscar sesiones que expiren después de una fecha.
8. Buscar sesiones dentro de un intervalo.
9. Paginar usuarios de diez en diez.
10. Ordenar usuarios dinámicamente por nombre o email.

### Nivel 3: JPQL y relaciones

11. Consultar los roles de un usuario.
12. Consultar usuarios de un rol.
13. Consultar permisos de un usuario.
14. Consultar usuarios que no tienen roles.
15. Consultar roles que todavía no tienen permisos.

### Nivel 4: reportes

16. Contar usuarios por rol.
17. Contar permisos por rol.
18. Mostrar roles con al menos tres usuarios.
19. Obtener la expiración más cercana y más lejana.
20. Crear un DTO que resuma cada rol y sus cantidades.

### Nivel 5: consultas dinámicas

21. Crear un buscador de usuarios con nombre y email opcionales.
22. Agregar rol como tercer filtro.
23. Combinar la búsqueda con paginación.

---

## 38. Pruebas recomendadas

Utilice `@DataJpaTest` para verificar los repositorios:

~~~java
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        Optional<User> result =
                userRepository.findByEmail(
                        "ana@mail.com"
                );

        assertThat(result).isPresent();
    }
}
~~~

Cada consulta importante debería probar:

* Caso con resultados.
* Caso sin resultados.
* Varios resultados.
* Diferencias entre mayúsculas y minúsculas.
* Relaciones vacías.
* Paginación.
* Agregaciones con y sin datos.

---

## 39. Resumen

~~~text
CONSULTA SENCILLA
findByEmail(...)
        ↓
Spring deriva la consulta


CONSULTA PERSONALIZADA
@Query con JPQL
        ↓
entidades, atributos y JOIN


REPORTE
COUNT, MAX, MIN, SUM, AVG
        ↓
GROUP BY y DTO


FILTROS DINÁMICOS
Specification
        ↓
composición de condiciones


CASO ESPECÍFICO DE MYSQL
nativeQuery = true
        ↓
SQL nativo
~~~

La consulta más avanzada no siempre es la mejor. Debemos escoger la alternativa
más sencilla que exprese correctamente la necesidad y siga siendo legible.

---

## 40. Referencias oficiales

* [Métodos de consulta de Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
* [Palabras clave de consultas derivadas](https://docs.spring.io/spring-data/jpa/reference/repositories/query-keywords-reference.html)
* [Tipos de retorno de repositorios](https://docs.spring.io/spring-data/jpa/reference/repositories/query-return-types-reference.html)
* [Proyecciones](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html)
* [Specifications](https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html)
