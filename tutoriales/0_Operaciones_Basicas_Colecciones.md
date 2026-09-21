Un `Stream` en Java permite procesar los elementos de una colección mediante un flujo de operaciones. Para entenderlo, comparemos primero la forma tradicional con `for each`.

Supongamos esta lista:

```java
List<String> nombres = List.of("Ana", "Carlos", "Beatriz");
```

### 1. Recorrer los elementos

Forma tradicional:

```java
for (String nombre : nombres) {
    System.out.println(nombre);
}
```

Con `Stream`:

```java
nombres.stream()
       .forEach(nombre -> System.out.println(nombre));
```

El `Stream` recorre la lista y ejecuta una acción sobre cada elemento. La expresión:

```java
nombre -> System.out.println(nombre)
```

es una función lambda.

Sin embargo, para un recorrido simple, el `for each` suele ser más claro.

---

### 2. Filtrar elementos

Supongamos que queremos obtener los nombres que empiezan por `"A"`.

Forma tradicional:

```java
List<String> resultado = new ArrayList<>();

for (String nombre : nombres) {
    if (nombre.startsWith("A")) {
        resultado.add(nombre);
    }
}
```

Con `Stream`:

```java
List<String> resultado = nombres.stream()
        .filter(nombre -> nombre.startsWith("A"))
        .toList();
```

El método `filter()` conserva únicamente los elementos que cumplen la condición.

En este caso:

```java
nombre -> nombre.startsWith("A")
```

significa: “recibe un nombre y verifica si comienza con A”.

---

### 3. Transformar elementos

Supongamos que queremos convertir todos los nombres a mayúsculas.

Forma tradicional:

```java
List<String> resultado = new ArrayList<>();

for (String nombre : nombres) {
    resultado.add(nombre.toUpperCase());
}
```

Con `Stream`:

```java
List<String> resultado = nombres.stream()
        .map(nombre -> nombre.toUpperCase())
        .toList();
```

El método `map()` transforma cada elemento.

Por ejemplo:

```text
Ana      → ANA
Carlos   → CARLOS
Beatriz  → BEATRIZ
```

---

### 4. Filtrar y transformar

Ahora queremos los nombres que empiezan por `"A"` y convertirlos a mayúsculas.

Forma tradicional:

```java
List<String> resultado = new ArrayList<>();

for (String nombre : nombres) {
    if (nombre.startsWith("A")) {
        resultado.add(nombre.toUpperCase());
    }
}
```

Con `Stream`:

```java
List<String> resultado = nombres.stream()
        .filter(nombre -> nombre.startsWith("A"))
        .map(nombre -> nombre.toUpperCase())
        .toList();
```

El flujo funciona así:

```text
Lista
  ↓
filter()
  ↓
map()
  ↓
toList()
```

Cada operación realiza una tarea específica.

---

### 5. Contar elementos

Supongamos que queremos contar cuántos nombres empiezan por `"A"`.

Forma tradicional:

```java
int contador = 0;

for (String nombre : nombres) {
    if (nombre.startsWith("A")) {
        contador++;
    }
}
```

Con `Stream`:

```java
long contador = nombres.stream()
        .filter(nombre -> nombre.startsWith("A"))
        .count();
```

El método `count()` devuelve la cantidad de elementos que quedaron después del filtro.

---

### 6. Verificar condiciones

Supongamos que queremos saber si existe algún nombre que empiece por `"A"`.

Forma tradicional:

```java
boolean existe = false;

for (String nombre : nombres) {
    if (nombre.startsWith("A")) {
        existe = true;
        break;
    }
}
```

Con `Stream`:

```java
boolean existe = nombres.stream()
        .anyMatch(nombre -> nombre.startsWith("A"));
```

Algunos métodos similares son:

```java
anyMatch();   // Al menos uno cumple
allMatch();   // Todos cumplen
noneMatch();  // Ninguno cumple
```

Ejemplo:

```java
boolean todosTienenNombre = nombres.stream()
        .allMatch(nombre -> !nombre.isEmpty());
```

---

### 7. Buscar un elemento

Forma tradicional:

```java
String encontrado = null;

for (String nombre : nombres) {
    if (nombre.startsWith("A")) {
        encontrado = nombre;
        break;
    }
}
```

Con `Stream`:

```java
Optional<String> encontrado = nombres.stream()
        .filter(nombre -> nombre.startsWith("A"))
        .findFirst();
```

El resultado es un `Optional` porque puede existir o no un elemento que cumpla la condición.

Para obtenerlo de forma segura:

```java
encontrado.ifPresent(nombre -> System.out.println(nombre));
```

---

### 8. Ordenar elementos

Forma tradicional:

```java
List<String> resultado = new ArrayList<>(nombres);
resultado.sort(String::compareTo);
```

Con `Stream`:

```java
List<String> resultado = nombres.stream()
        .sorted()
        .toList();
```

El método `sorted()` ordena los elementos y produce un nuevo resultado.

---

### 9. Eliminar elementos repetidos

Supongamos:

```java
List<Integer> numeros = List.of(1, 2, 2, 3, 3, 4);
```

Forma tradicional:

```java
Set<Integer> resultado = new HashSet<>();

for (Integer numero : numeros) {
    resultado.add(numero);
}
```

Con `Stream`:

```java
List<Integer> resultado = numeros.stream()
        .distinct()
        .toList();
```

`distinct()` elimina los elementos repetidos.

---

### 10. Combinar todos los elementos

Supongamos que queremos sumar números.

Forma tradicional:

```java
int suma = 0;

for (Integer numero : numeros) {
    suma += numero;
}
```

Con `Stream`:

```java
int suma = numeros.stream()
        .reduce(0, (total, numero) -> total + numero);
```

El método `reduce()` combina todos los elementos para obtener un único resultado.

También puede escribirse:

```java
int suma = numeros.stream()
        .mapToInt(numero -> numero)
        .sum();
```

---

### 11. Ejemplo más completo

Lista de usuarios:

```java
List<User> users = userRepository.findAll();
```

Forma tradicional:

```java
List<String> nombres = new ArrayList<>();

for (User user : users) {
    if (user.isActive()) {
        nombres.add(user.getName().toUpperCase());
    }
}

nombres.sort(String::compareTo);
```

Con `Stream`:

```java
List<String> nombres = users.stream()
        .filter(User::isActive)
        .map(user -> user.getName().toUpperCase())
        .sorted()
        .toList();
```

Este flujo:

1. Obtiene todos los usuarios.
2. Conserva únicamente los activos.
3. Extrae sus nombres.
4. Convierte los nombres a mayúsculas.
5. Los ordena.
6. Crea una nueva lista.

---

### Idea principal

El `for each` indica paso a paso cómo recorrer y procesar los datos.

El `Stream` indica qué operaciones se desean realizar sobre los datos:

```java
users.stream()
     .filter(...)
     .map(...)
     .sorted()
     .toList();
```

Por eso, un `Stream` permite expresar el procesamiento de forma más declarativa y encadenada. La lógica sigue siendo equivalente a utilizar ciclos, condiciones y acumuladores, pero se organiza como un flujo de operaciones.
