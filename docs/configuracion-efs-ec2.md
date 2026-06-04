# Configuración manual de AWS EFS en EC2 (Linux)

Guía para preparar el host **antes** del primer deploy con Docker Hub. El pipeline ([`docker-deploy.yml`](../.github/workflows/docker-deploy.yml)) solo asume que EFS ya está montado en `/mnt/dispatch-flow-efs` y enlaza ese directorio al contenedor en `/app/efs`.

No es necesario clonar este repositorio en el EC2.

---

## Valores que debes tener

Sustituye según tu entorno:

| Parámetro | Ejemplo | Uso |
| --------- | ------- | --- |
| File system ID | `fs-020d791d0e1ad4b39` | ID del EFS en AWS |
| Región | `us-east-1` | Misma región que EC2 y EFS |
| DNS EFS | `fs-020d791d0e1ad4b39.efs.us-east-1.amazonaws.com` | `{FILE_SYSTEM_ID}.efs.{REGION}.amazonaws.com` |
| Punto de montaje en el host | `/mnt/dispatch-flow-efs` | Debe coincidir con el workflow (`EFS_HOST_MOUNT`) |

---

## 1. Crear EFS en AWS

1. Consola AWS → **EFS** → **Create file system**.
2. Misma **VPC** y **región** que la instancia EC2.
3. Anotar el **File system ID** (`fs-xxxxxxxx`).
4. Crear **mount targets** en la subnet y zona de disponibilidad del EC2.

---

## 2. Security groups

| Origen | Destino | Puerto | Protocolo |
| ------ | ------- | ------ | --------- |
| Security group del EC2 | Security group del EFS | 2049 | NFS (TCP) |

El security group del EFS debe permitir NFS desde el security group del EC2.

---

## 3. Instalar cliente NFS en el EC2

Conéctate por SSH al EC2.

**Ubuntu / Debian:**

```bash
sudo apt-get update
sudo apt-get install -y nfs-common
```

**Amazon Linux:**

```bash
sudo dnf install -y nfs-utils
# o en versiones antiguas:
# sudo yum install -y nfs-utils
```

---

## 4. Montar EFS por DNS (NFS4)

Crea el directorio de montaje (debe ser `/mnt/dispatch-flow-efs` para alinear con el pipeline):

```bash
sudo mkdir -p /mnt/dispatch-flow-efs
```

Monta reemplazando los valores de ejemplo por los tuyos:

| En el comando | Reemplazar por |
| ------------- | -------------- |
| `fs-020d791d0e1ad4b39` | Tu **File system ID** (consola EFS, ej. `fs-0abc123...`) |
| `us-east-1` | Tu **región AWS** (la misma del EC2 y del EFS) |
| `/mnt/dispatch-flow-efs` | Dejar así (debe coincidir con el pipeline) |

DNS completo: `{FILE_SYSTEM_ID}.efs.{REGION}.amazonaws.com`

```bash
sudo mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport \
  <TU_FILE_SYSTEM_ID>.efs.<TU_REGION>.amazonaws.com:/ /mnt/dispatch-flow-efs
```

Ejemplo (no copiar si tus IDs son otros):

```bash
sudo mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport \
  fs-020d791d0e1ad4b39.efs.us-east-1.amazonaws.com:/ /mnt/dispatch-flow-efs
```

Verificar:

```bash
mount | grep dispatch-flow-efs
ls -la /mnt/dispatch-flow-efs
```

---

## 5. Persistir tras reinicio (`/etc/fstab`)

Añade una línea sustituyendo el DNS de ejemplo por el tuyo (`<TU_FILE_SYSTEM_ID>.efs.<TU_REGION>.amazonaws.com`):

```bash
echo '<TU_FILE_SYSTEM_ID>.efs.<TU_REGION>.amazonaws.com:/ /mnt/dispatch-flow-efs nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport,_netdev 0 0' | sudo tee -a /etc/fstab
```

Ejemplo:

```bash
echo 'fs-020d791d0e1ad4b39.efs.us-east-1.amazonaws.com:/ /mnt/dispatch-flow-efs nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport,_netdev 0 0' | sudo tee -a /etc/fstab
```

Comprobar que el punto de montaje no esté duplicado en `/etc/fstab` antes de añadir.

---

## 6. Permisos de escritura

El contenedor Docker debe poder escribir en el mount:

```bash
sudo chmod 1777 /mnt/dispatch-flow-efs
```

---

## 7. Relación con Docker Hub

Tras completar estos pasos, el deploy por GitHub Actions ejecuta en el EC2:

```text
docker run ... -v /mnt/dispatch-flow-efs:/app/efs -e EFS_BASE_PATH=/app/efs ...
```

La aplicación corre **solo** desde la imagen de Docker Hub; EFS queda en el sistema operativo del host.

---

## 8. Verificación después del deploy

```bash
curl http://<IP_EC2>:8080/actuator/health

# Tras crear una guía por API:
ls -R /mnt/dispatch-flow-efs/guides/
docker exec dispatch-flow-api ls -R /app/efs/guides/
```

---

## Referencia

Volver a la guía general de despliegue: [guia-despliegue-ec2.md](guia-despliegue-ec2.md).
