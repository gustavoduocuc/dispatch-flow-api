# dispatch-flow-api

API para gestión de guías de despacho. Esta versión incluye un CRUD básico con persistencia local en H2.

## Requisitos

- Java 21
- Maven 3.9+ (incluido vía `./mvnw`)

## Ejecutar la aplicación

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

## Ejecutar tests

```bash
./mvnw test
```

## Consola H2

Con la aplicación en ejecución:

| Campo | Valor |
|-------|-------|
| URL | http://localhost:8080/h2-console |
| JDBC URL | `jdbc:h2:mem:dispatchflow` |
| Usuario | `sa` |
| Contraseña | *(vacía)* |

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/guides` | Crear guía |
| GET | `/api/guides/{id}` | Obtener por ID |
| GET | `/api/guides` | Listar guías activas |
| PUT | `/api/guides/{id}` | Actualizar guía |
| DELETE | `/api/guides/{id}` | Eliminar lógicamente |
| GET | `/api/guides/search?carrierName=&date=` | Buscar por transportista y fecha |

El campo `guideNumber` lo genera el sistema. La eliminación es lógica (`status = DELETED`); las guías eliminadas no aparecen en listados ni búsquedas.

## Ejemplo Postman: crear guía

**POST** `http://localhost:8080/api/guides`

```json
{
  "carrierName": "Transportes Rápidos",
  "recipientName": "María González",
  "originAddress": "Av. Providencia 1234, Santiago",
  "destinationAddress": "Calle Huérfanos 567, Santiago",
  "description": "Electrónicos",
  "dispatchDate": "2026-06-02",
  "ownerEmail": "responsable@empresa.cl"
}
```

Respuesta esperada: `201 Created` con `id`, `guideNumber` (formato `GD-AAAA-NNNNNN`) y `status: CREATED`.

## Ejemplo Postman: búsqueda

**GET** `http://localhost:8080/api/guides/search?carrierName=Transportes%20Rápidos&date=2026-06-02`

## Arquitectura

El proyecto sigue arquitectura hexagonal (inside-out):

- **Dominio**: entidades, value objects, repositorio (interfaz + InMemory)
- **Aplicación**: casos de uso y DTOs
- **Infraestructura**: JPA/H2, controladores REST

### Integraciones futuras (preparadas, sin implementar)

Puertos definidos en `guides/application/ports/`:

- `ObjectStoragePort` — almacenamiento S3
- `EfsStoragePort` — almacenamiento EFS

Los campos `s3Key` y `efsPath` existen en el modelo pero permanecen en `null` en esta versión. La dependencia `ojdbc11` en el `pom.xml` permite migrar a Oracle cambiando el adapter de `GuideRepository` sin modificar el dominio.

## Health check

```bash
curl http://localhost:8080/actuator/health
```
