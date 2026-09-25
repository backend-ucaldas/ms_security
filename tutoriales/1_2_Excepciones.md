## Guía: manejo general de excepciones 

### 1. Problema de la primera versión

En la primera versión, el `Service` lanzaba directamente una excepción de Spring Web:

```java
private User findUser(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado"
            ));
}
```

Aquí el `Service` realiza dos tareas: busca al usuario y decide cómo responder mediante HTTP. Esto incumple el principio de responsabilidad única y acopla la lógica de la aplicación con la capa de presentación.

Para corregirlo, el `Service` lanzará una excepción propia de la aplicación. Un manejador global, ubicado en la capa web, convertirá esa excepción en la respuesta HTTP correspondiente.

### 2. Crear los paquetes nuevos

Dentro del paquete base del proyecto `com.uc.ms_security`, crea el paquete `exception`:

```text
src/main/java/com/uc/ms_security/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
└── exception/
```

En `exception` estarán las clases relacionadas con los errores de la aplicación y su manejo global.

### 3. Crear el enum de casos de error

Dentro de `exception`, crea el archivo `ErrorCase.java`:

```text
src/main/java/com/uc/ms_security/exception/ErrorCase.java
```

El enum contiene las categorías de error que se vayan identificando:

```java
package com.uc.ms_security.exception;

public enum ErrorCase {
    NOT_FOUND,
    ALREADY_EXISTS,
    INVALID_OPERATION
}
```

Los nombres representan tipos de problema, no entidades específicas. Por eso, `NOT_FOUND` puede utilizarse para usuarios, perfiles u otras entidades.

### 4. Crear una excepción reutilizable

En el mismo paquete, crea `ApplicationException.java`:

```text
src/main/java/com/uc/ms_security/exception/ApplicationException.java
```

```java
package com.uc.ms_security.exception;

public class ApplicationException extends RuntimeException {

    private final ErrorCase errorCase;

    public ApplicationException(ErrorCase errorCase, String message) {
        super(message);
        this.errorCase = errorCase;
    }

    public ErrorCase getErrorCase() {
        return errorCase;
    }
}
```

Esta excepción comunica el tipo de error y su descripción. No contiene `HttpStatus` ni depende de Spring Web.

### 5. Usar la excepción desde cualquier `Service`

El `UserService`, que ya se encuentra en el paquete `service`, importa las clases del paquete `exception`:

```java
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
```

Luego, puede lanzar la excepción cuando el usuario no exista:

```java
private User findUser(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ApplicationException(
                    ErrorCase.NOT_FOUND,
                    "Usuario no encontrado con id: " + id
            ));
}
```

Otro service puede utilizar la misma excepción para otra entidad:

```java
private Profile findProfile(Long id) {
    return profileRepository.findById(id)
            .orElseThrow(() -> new ApplicationException(
                    ErrorCase.NOT_FOUND,
                    "Perfil no encontrado con id: " + id
            ));
}
```

También se puede usar para un email duplicado:

```java
if (userRepository.existsByEmail(dto.getEmail())) {
    throw new ApplicationException(
            ErrorCase.ALREADY_EXISTS,
            "Ya existe un usuario con este email"
    );
}
```

Así, todos los services reutilizan la misma excepción y comunican el problema sin decidir cómo debe responder la API.

El `RoleService` utiliza los mismos casos para sus operaciones:

```java
private Role findEntityById(Long id) {
    return roleRepository.findById(id)
        .orElseThrow(() -> new ApplicationException(
            ErrorCase.NOT_FOUND,
            "Rol no encontrado con id: " + id
        ));
}
```

Para evitar nombres de rol duplicados:

```java
if (roleRepository.existsByNameIgnoreCase(dto.getName())) {
    throw new ApplicationException(
        ErrorCase.ALREADY_EXISTS,
        "Ya existe un rol con ese nombre"
    );
}
```

Si se intenta eliminar un rol asignado a un usuario, se utiliza `INVALID_OPERATION`:

```java
if (userRoleRepository.existsByRoleId(id)) {
    throw new ApplicationException(
        ErrorCase.INVALID_OPERATION,
        "No se puede eliminar un rol que está asignado"
    );
}
```

### 6. Crear el manejador global de excepciones

En el paquete `exception`, crea `GlobalExceptionHandler.java`:

```text
src/main/java/com/uc/ms_security/exception/GlobalExceptionHandler.java
```

```java
package com.uc.ms_security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Map<String, String>> handleApplicationException(
            ApplicationException exception) {

        HttpStatus status = switch (exception.getErrorCase()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case INVALID_OPERATION -> HttpStatus.BAD_REQUEST;
        };

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", exception.getErrorCase().name());
        error.put("message", exception.getMessage());

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException exception) {

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "HTTP_ERROR");
        error.put("message", exception.getReason());

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(
            Exception exception) {

        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorCase", "INTERNAL_ERROR");
        error.put("message", "Ocurrió un error interno en el servidor");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
```

La anotación `@RestControllerAdvice` permite que Spring detecte el manejador y aplique sus métodos a las excepciones que ocurran durante las solicitudes de la API. No es necesario agregar un `try-catch` en cada método del controlador.

Las excepciones de aplicación se devuelven como JSON. Por ejemplo, un usuario no encontrado produce:

```json
{
    "errorCase": "NOT_FOUND",
    "message": "Usuario no encontrado con id: 9"
}
```

Las excepciones inesperadas producen una respuesta `500` con este formato:

```json
{
    "errorCase": "INTERNAL_ERROR",
    "message": "Ocurrió un error interno en el servidor"
}
```

### 7. Estructura final del proyecto

```text
src/main/java/com/uc/ms_security/
├── controller/
│   └── UserController.java
├── service/
│   └── UserService.java
├── repository/
│   └── UserRepository.java
├── entity/
│   └── User.java
├── dto/
├── mapper/
└── exception/
    ├── ApplicationException.java
    ├── ErrorCase.java
    └── GlobalExceptionHandler.java
```

### 8. Agregar casos nuevos

Cuando se identifique una nueva categoría de error:

1. Si no encaja en las existentes, se agrega un valor a `ErrorCase`.
2. El service lanza `ApplicationException` con ese caso y un mensaje descriptivo.
3. Se añade la correspondencia entre el nuevo caso y el estado HTTP en el `switch` de `GlobalExceptionHandler`.

Por ejemplo, para un error de autenticación, se puede agregar `UNAUTHORIZED`:

```java
public enum ErrorCase {
    NOT_FOUND,
    ALREADY_EXISTS,
    INVALID_OPERATION,
    UNAUTHORIZED
}
```

Y en el `switch`:

```java
case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
```

Si el error nuevo ya corresponde a una categoría existente, se reutiliza esa categoría y se adapta el mensaje. De esta manera, la misma configuración puede atender excepciones de distintas entidades y situaciones.
