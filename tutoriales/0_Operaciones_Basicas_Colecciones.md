# Guía: De `for each` a `Stream` en Java usando la clase `User`

## Clase de ejemplo

```java
public class User {
    private Long id;
    private String email;
    private int age;

    public User(Long id, String email, int age) {
        this.id = id;
        this.email = email;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public int getAge() {
        return age;
    }
}
```

Lista de usuarios:

```java
List<User> users = List.of(
        new User(1L, "ana@gmail.com", 22),
        new User(2L, "carlos@gmail.com", 17),
        new User(3L, "beatriz@gmail.com", 30)
);
```

---

## 1. Recorrer usuarios

Objetivo: mostrar el correo de cada usuario.

### Forma tradicional con `for each`

```java
for (User user : users) {
    System.out.println(user.getEmail());
}
```

### Forma con `Stream`

```java
users.stream()
     .forEach(user -> System.out.println(user.getEmail()));
```

El `Stream` recorre cada usuario y ejecuta una acción.

---

## 2. Filtrar usuarios

Objetivo: obtener los usuarios mayores de edad.

### Forma tradicional

```java
List<User> adults = new ArrayList<>();

for (User user : users) {
    if (user.getAge() >= 18) {
        adults.add(user);
    }
}
```

### Forma con `Stream`

```java
List<User> adults = users.stream()
        .filter(user -> user.getAge() >= 18)
        .toList();
```

`filter()` conserva únicamente los usuarios que cumplen la condición.

---

## 3. Obtener un atributo

Objetivo: obtener solamente los correos de los usuarios.

### Forma tradicional

```java
List<String> emails = new ArrayList<>();

for (User user : users) {
    emails.add(user.getEmail());
}
```

### Forma con `Stream`

```java
List<String> emails = users.stream()
        .map(user -> user.getEmail())
        .toList();
```

`map()` transforma cada objeto `User` en su correo electrónico.

```text
User → email
```

---

## 4. Filtrar y obtener atributos

Objetivo: obtener los correos de los usuarios mayores de edad.

### Forma tradicional

```java
List<String> emails = new ArrayList<>();

for (User user : users) {
    if (user.getAge() >= 18) {
        emails.add(user.getEmail());
    }
}
```

### Forma con `Stream`

```java
List<String> emails = users.stream()
        .filter(user -> user.getAge() >= 18)
        .map(user -> user.getEmail())
        .toList();
```

El flujo realiza:

```text
Usuarios → Filtrar mayores de edad → Obtener correos → Crear lista
```

---

## 5. Contar usuarios

Objetivo: contar cuántos usuarios son mayores de edad.

### Forma tradicional

```java
int counter = 0;

for (User user : users) {
    if (user.getAge() >= 18) {
        counter++;
    }
}
```

### Forma con `Stream`

```java
long counter = users.stream()
        .filter(user -> user.getAge() >= 18)
        .count();
```

`count()` cuenta los usuarios que cumplen la condición.

---

## 6. Verificar si existe un usuario

Objetivo: verificar si existe algún usuario menor de edad.

### Forma tradicional

```java
boolean exists = false;

for (User user : users) {
    if (user.getAge() < 18) {
        exists = true;
        break;
    }
}
```

### Forma con `Stream`

```java
boolean exists = users.stream()
        .anyMatch(user -> user.getAge() < 18);
```

`anyMatch()` devuelve `true` si al menos un usuario cumple la condición.

---

## 7. Verificar si todos cumplen

Objetivo: verificar si todos los usuarios son mayores de edad.

### Forma tradicional

```java
boolean allAdults = true;

for (User user : users) {
    if (user.getAge() < 18) {
        allAdults = false;
        break;
    }
}
```

### Forma con `Stream`

```java
boolean allAdults = users.stream()
        .allMatch(user -> user.getAge() >= 18);
```

`allMatch()` devuelve `true` solamente si todos cumplen la condición.

---

## 8. Buscar un usuario

Objetivo: encontrar el primer usuario cuyo correo sea de Gmail.

### Forma tradicional

```java
User result = null;

for (User user : users) {
    if (user.getEmail().endsWith("@gmail.com")) {
        result = user;
        break;
    }
}
```

### Forma con `Stream`

```java
Optional<User> result = users.stream()
        .filter(user -> user.getEmail().endsWith("@gmail.com"))
        .findFirst();
```

`findFirst()` obtiene el primer usuario que cumple la condición.

---

## 9. Ordenar usuarios

Objetivo: ordenar los usuarios de menor a mayor edad.

### Forma tradicional

```java
List<User> orderedUsers = new ArrayList<>(users);

orderedUsers.sort(
        Comparator.comparing(User::getAge)
);
```

### Forma con `Stream`

```java
List<User> orderedUsers = users.stream()
        .sorted(Comparator.comparing(User::getAge))
        .toList();
```

`sorted()` ordena los elementos del flujo.

---

## 10. Obtener la edad promedio

Objetivo: calcular la edad promedio de los usuarios.

### Forma tradicional

```java
int totalAge = 0;

for (User user : users) {
    totalAge += user.getAge();
}

double averageAge = (double) totalAge / users.size();
```

### Forma con `Stream`

```java
double averageAge = users.stream()
        .mapToInt(User::getAge)
        .average()
        .orElse(0);
```

`mapToInt()` obtiene las edades y `average()` calcula el promedio.

---

## 11. Ejemplo completo

Objetivo: obtener los correos de los usuarios mayores de edad, ordenados alfabéticamente.

### Forma tradicional

```java
List<String> emails = new ArrayList<>();

for (User user : users) {
    if (user.getAge() >= 18) {
        emails.add(user.getEmail());
    }
}

emails.sort(String::compareTo);
```

### Forma con `Stream`

```java
List<String> emails = users.stream()
        .filter(user -> user.getAge() >= 18)
        .map(User::getEmail)
        .sorted()
        .toList();
```

El flujo funciona así:

```text
List<User>
   ↓
filter()   → conserva adultos
   ↓
map()      → obtiene sus correos
   ↓
sorted()   → ordena los correos
   ↓
toList()   → crea la lista final
```

La diferencia es que el `for each` describe cada paso manualmente, mientras que el `Stream` expresa el proceso como una cadena de operaciones.
