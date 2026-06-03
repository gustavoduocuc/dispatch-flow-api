# dispatch-flow-api

API para gestión de guías de despacho con generación automática de PDF, almacenamiento temporal en EFS y subida automática a AWS S3.

## Requisitos

- Java 21
- Maven 3.9+ (incluido vía `./mvnw`)
- Docker (para desarrollo local con LocalStack)

## Ejecutar en local (LocalStack + S3 emulado)

```bash
chmod +x run-local run-prod scripts/init-localstack.sh
./run-local
```

Este script levanta LocalStack, crea el bucket `dispatch-flow-local` y arranca la API con perfil `local`.

## Ejecutar en producción

Variables obligatorias:

| Variable | Descripción |
|----------|-------------|
| `AWS_REGION` | Región AWS (ej. `us-east-1`) |
| `S3_BUCKET_NAME` | Bucket S3 (default prod: `dispatch-flow-prod`) |

Credenciales vía variables de entorno AWS estándar o rol IAM. Si usas credenciales temporales, define también `AWS_SESSION_TOKEN`.

```bash
export AWS_REGION=us-east-1
export S3_BUCKET_NAME=dispatch-flow-prod
./run-prod
```

## Ejecutar tests

```bash
./mvnw test
```

Los tests E2E usan almacenamiento S3 en memoria (`dispatch.storage.s3.enabled=false`) para CI estable.

## Almacenamiento EFS (local / staging)

| Variable | Local (default) | Producción |
|----------|-----------------|------------|
| `EFS_BASE_PATH` | `./tmp/efs` | `/app/efs` |

Al **crear** o **actualizar** una guía, el sistema:

1. Genera un PDF con Apache PDFBox
2. Lo guarda en `{EFS_BASE_PATH}/guides/{fecha}/{transportista-slug}/guide-{id}.pdf`
3. Sube el mismo PDF a S3 con la misma clave relativa
4. Persiste `efsPath`, `s3Key` y el status `UPLOADED_TO_S3`

Si falla EFS o S3 durante POST/PUT, la operación completa falla.

## Almacenamiento S3

| Variable | Local (LocalStack) | Producción |
|----------|-------------------|------------|
| `DISPATCH_STORAGE_S3_ENABLED` | `true` | `true` |
| `S3_BUCKET_NAME` | `dispatch-flow-local` | `dispatch-flow-prod` |
| `AWS_S3_ENDPOINT` | `http://localhost:4566` | *(vacío — AWS real)* |
| `AWS_ACCESS_KEY_ID` | `test` | IAM / env |
| `AWS_SECRET_ACCESS_KEY` | `test` | IAM / env |
| `AWS_SESSION_TOKEN` | *(opcional)* | *(si aplica)* |
| `AWS_REGION` | `us-east-1` | región del bucket |

Estructura de claves S3 (igual que rutas EFS relativas):

```
guides/{fecha}/{transportista-slug}/guide-{id}.pdf
```

Verificar objetos en local:

```bash
awslocal s3 ls s3://dispatch-flow-local/guides/ --recursive
```

**Delete:** borra el objeto S3 (si existe) y marca la guía como `DELETED` en BD.

**Download:** lee desde S3 si hay `s3Key`; fallback a EFS para datos legacy.

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
| POST | `/api/guides` | Crear guía, PDF en EFS y S3 |
| GET | `/api/guides/{id}` | Obtener por ID |
| GET | `/api/guides/{id}/download` | Descargar PDF (S3 preferido) |
| GET | `/api/guides` | Listar guías activas |
| PUT | `/api/guides/{id}` | Actualizar guía y regenerar PDF + S3 |
| DELETE | `/api/guides/{id}` | Borrar objeto S3 + eliminación lógica |
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

Respuesta esperada: `201 Created` con `id`, `guideNumber`, `efsPath`, `s3Key` y `status: UPLOADED_TO_S3`.

Verificar EFS: `./tmp/efs/guides/2026-06-02/transportes-rapidos/`

Verificar S3: `awslocal s3 ls s3://dispatch-flow-local/guides/2026-06-02/transportes-rapidos/`

Colección Postman: [`postman/dispatch-flow-api.postman_collection.json`](postman/dispatch-flow-api.postman_collection.json)

## Ejemplo Postman: descargar PDF

**GET** `http://localhost:8080/api/guides/{id}/download`

Respuesta: `200 OK`, `Content-Type: application/pdf`, archivo adjunto `guide-{id}.pdf`.

## Ejemplo Postman: búsqueda

**GET** `http://localhost:8080/api/guides/search?carrierName=Transportes%20Rápidos&date=2026-06-02`

## Arquitectura

El proyecto sigue arquitectura hexagonal (inside-out):

- **Dominio**: entidades, value objects, `GuidePdfPathBuilder`, repositorio
- **Aplicación**: casos de uso, `GuidePdfEfsStorage`, `GuidePdfS3Storage`, puertos PDF/EFS/S3
- **Infraestructura**: JPA/H2, PDFBox, `LocalEfsStorageAdapter`, `S3ObjectStorageAdapter`, controladores REST

## Health check

```bash
curl http://localhost:8080/actuator/health
```
