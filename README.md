# dispatch-flow-api

API para gestión de guías de despacho con generación automática de PDF y almacenamiento temporal en EFS (carpeta local en desarrollo).

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

## Almacenamiento EFS (local)

Variable de configuración:

| Variable | Local (default) | Producción |
|----------|-----------------|------------|
| `EFS_BASE_PATH` | `./tmp/efs` | `/app/efs` |

Ejemplo con variable explícita:

```bash
EFS_BASE_PATH=./tmp/efs ./mvnw spring-boot:run
```

Al **crear** o **actualizar** una guía, el sistema:

1. Genera un PDF con Apache PDFBox
2. Lo guarda en `{EFS_BASE_PATH}/guides/{fecha}/{transportista-slug}/guide-{id}.pdf`
3. Persiste la ruta absoluta en `efsPath` y el status `PDF_GENERATED`

Los archivos generados en local quedan en `./tmp/efs/` (ignorado por git).

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
| POST | `/api/guides` | Crear guía y generar PDF en EFS |
| GET | `/api/guides/{id}` | Obtener por ID |
| GET | `/api/guides/{id}/download` | Descargar PDF de la guía |
| GET | `/api/guides` | Listar guías activas |
| PUT | `/api/guides/{id}` | Actualizar guía y regenerar PDF |
| DELETE | `/api/guides/{id}` | Eliminar lógicamente |
| GET | `/api/guides/search?carrierName=&date=` | Buscar por transportista y fecha |

El campo `guideNumber` lo genera el sistema. La eliminación es lógica (`status = DELETED`); las guías eliminadas no aparecen en listados ni búsquedas.

Si falla la generación o escritura del PDF durante POST/PUT, la operación completa falla (no se persiste una guía sin archivo).

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

Respuesta esperada: `201 Created` con `id`, `guideNumber`, `efsPath` y `status: PDF_GENERATED`.

Verificar el archivo en `./tmp/efs/guides/2026-06-02/transportes-rapidos/`.

## Ejemplo Postman: descargar PDF

**GET** `http://localhost:8080/api/guides/{id}/download`

Respuesta: `200 OK`, `Content-Type: application/pdf`, archivo adjunto `guide-{id}.pdf`.

## Ejemplo Postman: búsqueda

**GET** `http://localhost:8080/api/guides/search?carrierName=Transportes%20Rápidos&date=2026-06-02`

## Arquitectura

El proyecto sigue arquitectura hexagonal (inside-out):

- **Dominio**: entidades, value objects, `GuidePdfPathBuilder`, repositorio
- **Aplicación**: casos de uso, `GuidePdfEfsStorage`, puertos PDF/EFS
- **Infraestructura**: JPA/H2, PDFBox, `LocalEfsStorageAdapter`, controladores REST

### Integraciones futuras (preparadas, sin implementar)

- `ObjectStoragePort` — subida a AWS S3 (endpoint dedicado en iteración futura)
- Campo `s3Key` en el modelo, sin uso actual

## Health check

```bash
curl http://localhost:8080/actuator/health
```
