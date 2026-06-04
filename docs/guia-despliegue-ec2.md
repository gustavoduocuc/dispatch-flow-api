# Guía de despliegue: GitHub Actions → Docker Hub → EC2

Despliegue mediante [`.github/workflows/docker-deploy.yml`](../.github/workflows/docker-deploy.yml).

## Requisitos previos

- Aplicación operativa en local con `./run-prod` o Docker. (Recomendado)
- Wallet Oracle en `Wallet_DISPATCHFLOWDB/` (generado desde `Wallet_DISPATCHFLOWDB.zip`).
- Credenciales Oracle y AWS en `.env` (mismos valores que se usarán en los secrets de GitHub).

La wallet y el `.env` **no** se copian manualmente al servidor EC2. La wallet se incluye en la imagen durante el build en CI; usuario, contraseña Oracle y credenciales S3 se inyectan en el contenedor desde secrets de GitHub.

En EC2, **AWS EFS debe montarse en el host Linux** antes del primer deploy; el contenedor recibe ese directorio en `/app/efs` vía volumen Docker.

→ **[Configuración manual de EFS en EC2](configuracion-efs-ec2.md)** (pasos por DNS NFS4, sin clonar el repo; ajusta tu `fs-...` y región).

---

## 1. Secrets en GitHub

Configurar en **Settings → Secrets and variables → Actions**:

| Secret | Valor |
| ------ | ----- |
| `ORACLE_WALLET_BASE64` | Archivo zip de `Wallet_DISPATCHFLOWDB` codificado en base64 |
| `SPRING_DATASOURCE_USERNAME` | Usuario Oracle (equivalente a `.env` local) |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña Oracle (equivalente a `.env` local) |
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub |
| `DOCKERHUB_TOKEN` | Access token de Docker Hub (no la contraseña de la cuenta) |
| `EC2_HOST` | IP pública de la instancia EC2 |
| `USER_SERVER` | Usuario SSH: `ubuntu` (Ubuntu) o `ec2-user` (Amazon Linux) |
| `EC2_SSH_KEY` | Contenido del archivo `.pem` (líneas `BEGIN` a `END` inclusive) |
| `AWS_ACCESS_KEY_ID` | Access key IAM con permisos S3 sobre el bucket |
| `AWS_SECRET_ACCESS_KEY` | Secret key IAM |
| `AWS_SESSION_TOKEN` | Credenciales STS temporales; omitir si no aplica |
| `AWS_REGION` | Región del bucket S3 (ej. `us-east-1`) |
| `S3_BUCKET_NAME` | Bucket prod (`dispatch-flow-prod`) |
| `SPRING_DATASOURCE_URL` | Opcional. Por defecto: `jdbc:oracle:thin:@dispatchflowdb_high` |

### S3 en producción

1. Crear el bucket en la misma región que `AWS_REGION`.
2. Asignar al IAM de las credenciales permisos `s3:PutObject`, `GetObject`, `DeleteObject` y `ListBucket` sobre ese bucket.
3. Configurar los secrets `S3_BUCKET_NAME` y `AWS_REGION` en GitHub Actions.

No definir `AWS_S3_ENDPOINT` en producción; el SDK usa el endpoint regional de AWS.

### Generar `ORACLE_WALLET_BASE64`

```bash
cd /ruta/a/dispatch-flow-api
zip -r wallet.zip Wallet_DISPATCHFLOWDB
base64 -i wallet.zip | pbcopy
```

Asignar el resultado al secret `ORACLE_WALLET_BASE64`.

Alternativa si solo tienes el zip original:

```bash
base64 -i Wallet_DISPATCHFLOWDB.zip | pbcopy
```

### Determinar `USER_SERVER`

Consola AWS → EC2 → **Connect**, o verificación por SSH:

```bash
ssh -i ruta/a/tu-key.pem ubuntu@TU_IP_EC2
# Alternativa (Amazon Linux):
ssh -i ruta/a/tu-key.pem ec2-user@TU_IP_EC2
```

El usuario válido en el comando SSH es el valor de `USER_SERVER`.

### Configurar `EC2_SSH_KEY`

```bash
cat ruta/a/tu-key.pem
```

Copiar la salida completa al secret. No versionar el archivo `.pem` en el repositorio.

---

## 2. EFS y volumen Docker

La app escribe PDFs en el host **antes** de subirlos a S3. Configura y monta tu EFS siguiendo **[configuracion-efs-ec2.md](configuracion-efs-ec2.md)** (una vez por instancia EC2, antes del primer push a `main`).

El pipeline enlaza automáticamente:

```text
-v /mnt/dispatch-flow-efs:/app/efs
-e EFS_BASE_PATH=/app/efs
```

---

## 3. Configuración de EC2

- Docker instalado.
- **EFS montado** en `/mnt/dispatch-flow-efs` ([configuracion-efs-ec2.md](configuracion-efs-ec2.md)).
- Security group: regla de entrada TCP en el puerto **8080**.
- Acceso SSH con la llave asociada a `EC2_SSH_KEY`.
- No desplegar `Wallet_DISPATCHFLOWDB/` ni `.env` en el servidor.

---

## 4. Ejecutar el despliegue

1. Publicar el código en GitHub (incluye el workflow).
2. Push o merge a la rama `main`.

| Evento | Pipeline |
| ------ | -------- |
| Pull request → `main` | Solo `./mvnw test` |
| Push → `main` | Tests, build, push a Docker Hub, deploy por SSH |

Secuencia del job `build-and-deploy`:

1. `./mvnw test`
2. Decodifica `ORACLE_WALLET_BASE64` → carpeta `wallet/` en contexto Docker
3. Build de imagen Docker
4. Push a `{DOCKERHUB_USERNAME}/dispatch-flow-api:latest`
5. SSH a EC2: `docker pull`, recreación del contenedor con volumen EFS, variables Oracle y S3

---

## 5. Verificación

Sustituir `<IP_EC2>` por la IP pública de la instancia:

```text
http://<IP_EC2>:8080/actuator/health
http://<IP_EC2>:8080/api/guides
```

Crear una guía de prueba:

```bash
curl -X POST "http://<IP_EC2>:8080/api/guides" \
  -H "Content-Type: application/json" \
  -d '{
    "carrierName": "Transportes Rápidos",
    "recipientName": "María González",
    "originAddress": "Av. Providencia 1234, Santiago",
    "destinationAddress": "Calle Huérfanos 567, Santiago",
    "description": "Electrónicos",
    "dispatchDate": "2026-06-02",
    "ownerEmail": "responsable@empresa.cl"
  }'
```

Respuesta esperada: `201 Created` con `status: UPLOADED_TO_S3` y `s3Key` poblado.

Comprobar PDF en EFS (en el EC2):

```bash
ls -R /mnt/dispatch-flow-efs/guides/
docker exec dispatch-flow-api ls -R /app/efs/guides/
```

Endpoint `/actuator/health`: respuesta con `"status":"UP"`.

Errores de despliegue: revisar el job `build-and-deploy` en **Actions** del repositorio.

---

## Resumen

| Entorno | Wallet | Credenciales Oracle | S3 | EFS | Base de datos |
| ------- | ------ | --------------------- | --- | --- | ------------- |
| Local (`./run-local`) | No aplica | No aplica | LocalStack | `./tmp/efs` | H2 in-memory |
| Local prod (`./run-prod`) | `Wallet_DISPATCHFLOWDB/` | `.env` | `.env` | `./tmp/efs` o `/app/efs` | Oracle ATP |
| GitHub | `ORACLE_WALLET_BASE64` | `SPRING_DATASOURCE_*` | secrets AWS | — | Oracle ATP |
| EC2 | Imagen Docker (`/app/wallet`) | Variables en `docker run` | Mismas variables S3 | Host `/mnt/dispatch-flow-efs` → contenedor `/app/efs` | Oracle ATP |

---

## Docker en local (referencia)

Atajo recomendado:

```bash
cp .env.example .env
# Completar usuario, contraseña Oracle y credenciales AWS

./run-docker
curl http://localhost:8080/actuator/health
```

Equivalente manual (misma imagen `dispatch-flow-api:local`):

```bash
./scripts/setup-oracle-wallet.sh
mkdir -p wallet
cp -R Wallet_DISPATCHFLOWDB/. wallet/

docker build -t dispatch-flow-api:local .
mkdir -p ./tmp/efs-docker
docker run -d --name dispatch-flow-api -p 8080:8080 --env-file .env \
  -v "$(pwd)/tmp/efs-docker:/app/efs" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e TNS_ADMIN=/app/wallet \
  -e EFS_BASE_PATH=/app/efs \
  dispatch-flow-api:local

curl http://localhost:8080/actuator/health
```

Ver también [README.md](../README.md) (secciones «Oracle en producción» y «Ejecutar con Docker»).
