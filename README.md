# EcoAPI — Servicio REST de EcoRecicla

Backend del **Examen Parcial U2 – Versión B (caso EcoRecicla, pesajes en ruta)**.
Custodia rutas, puntos de acopio y pesajes, autentica con **JWT** y autoriza por **privilegio**.

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 (Web, Data JPA, Security, Validation) |
| JWT | jjwt 0.12.6 (HMAC-SHA256) |
| Base de datos | PostgreSQL local (perfil por defecto) · H2 en memoria (perfil `h2`) |
| Build | Gradle 8.14.3 (wrapper incluido) |

---

## 1. Ejecución (un solo comando)

### Requisitos
- JDK 21 (`JAVA_HOME` apuntando a él).
- PostgreSQL local corriendo en `localhost:5432` **sin Docker**, con una base vacía llamada `ecorecicla`:

```bash
psql -U postgres -c "CREATE DATABASE ecorecicla;"
```

### Levantar el servicio

```bash
./gradlew bootRun
```

En Windows: `gradlew.bat bootRun`. El servicio queda en `http://localhost:8080/api/v1`
(escucha en `0.0.0.0`, así que el emulador Android lo alcanza en `http://10.0.2.2:8080/api/v1`).

Las credenciales de PostgreSQL se configuran con variables de entorno (valores por defecto entre paréntesis):

| Variable | Uso |
|---|---|
| `ECO_DB_URL` | URL JDBC (`jdbc:postgresql://localhost:5432/ecorecicla`) |
| `ECO_DB_USUARIO` | Usuario (`postgres`) |
| `ECO_DB_CLAVE` | Contraseña (`postgres`) |
| `ECO_JWT_SECRETO` | Clave HMAC en Base64 de 256 bits (hay una de desarrollo por defecto) |

Ejemplo en PowerShell:

```powershell
$env:ECO_DB_CLAVE="mi_clave"; .\gradlew.bat bootRun
```

Alternativa sin PostgreSQL (base en memoria, se recrea en cada arranque):

```bash
./gradlew bootRun --args='--spring.profiles.active=h2'
```

Al iniciar se cargan los **datos semilla** del anexo (sección 10). La carga es idempotente: no duplica datos y, si cambió el día, crea la ruta del día.

### Pruebas

```bash
./gradlew test
```

10 pruebas de integración (`EcoApiIntegracionTest`) con MockMvc sobre H2: login, 401 (sin token, alterado y vencido), 403, paginación, 201 con neto calculado, 400 por campo, 409 por ruta cerrada, RN-02 y RN-04.

---

## 2. Estructura de paquetes

```
src/main/java/pe/upeu/ecorecicla/
├── config/        SecurityConfig, JwtAuthenticationFilter, JwtService,
│                  ManejadorDeErrores (formato de error + 401/403), DatosSemilla
├── seguridad/     Usuario, Rol, Privilegio, UsuarioRepository, RolRepository,
│                  PrivilegioRepository, UsuarioAutenticado, AuthController
├── dominio/       PuntoAcopio, Ruta, RutaPunto, Pesaje (entidades del caso)
├── repositorio/   PuntoAcopioRepository, RutaRepository, PesajeRepository
├── servicio/      RutaService, PesajeService, PuntoAcopioService (RN-01..RN-04
│                  y transacciones), excepciones de negocio, Paginacion
├── web/           RutaController, PuntoAcopioController, PesajeController
│   └── dto/       Pagina (envoltorio), ErrorApi (error común), peticiones y respuestas
└── Aplicacion.java
```

> El enunciado muestra la estructura con archivos `.kt`; este servicio se implementó en **Java** con Spring Boot (framework del curso) respetando exactamente los mismos paquetes y nombres de clase.

---

## 3. Contrato del API (`/api/v1`)

Todas las rutas, salvo el inicio de sesión, exigen `Authorization: Bearer <token>`.

| # | Operación | Verbo y ruta | Privilegio | Éxito | Errores |
|---|---|---|---|---|---|
| RF-B01 | Inicio de sesión | `POST /auth/login` | Público | 200 | 400, 401 |
| RF-B07 | Usuario actual | `GET /auth/me` | Autenticado | 200 | 401 |
| RF-B02 | Ruta del día del usuario | `GET /rutas/hoy?fecha=YYYY-MM-DD` (fecha opcional, hoy por defecto) | `PUNTO_LEER` | 200 | 401, 403, 404 |
| RF-B03 | Listado de puntos de acopio | `GET /puntos?zona=&pagina=0&tamanio=20` | `PUNTO_LEER` | 200 (paginado) | 400, 401, 403 |
| — | Detalle de un punto | `GET /puntos/{id}` | `PUNTO_LEER` | 200 | 404 |
| — | Crear punto de acopio | `POST /puntos` | `PUNTO_GESTIONAR` | 201 | 400, 403 |
| — | Planificar ruta | `POST /rutas` | `PUNTO_GESTIONAR` | 201 | 400, 403, 404 |
| — | Pesajes de un punto | `GET /puntos/{id}/pesajes?pagina=0&tamanio=20` (más reciente primero) | `PESAJE_LEER` | 200 (paginado) | 404 |
| — | Pesajes de una ruta (sincronización) | `GET /rutas/{id}/pesajes?pagina=0&tamanio=50` | `PESAJE_LEER` | 200 (paginado) | 404 |
| RF-B04 | Registrar pesaje | `POST /puntos/{id}/pesajes` | `PESAJE_CREAR` | **201** (+`Location`); 200 si la `claveCliente` ya se había recibido | 400 (RN-03), 403, 404, 409 (RN-01) |
| — | Consultar pesaje | `GET /pesajes/{id}` | `PESAJE_LEER` | 200 | 404 |
| RF-B05 | Corregir pesaje | `PUT /pesajes/{id}` | `PESAJE_EDITAR` | 200 | 400, 403 (RN-02), 404, 409 (RN-01) |
| RF-B05 | Anular pesaje | `DELETE /pesajes/{id}?motivo=...` | `PESAJE_ANULAR` | **204** (marca `ANULADO`, no borra) | 400, 403, 404, 409 |
| — | Omitir un punto | `POST /rutas/{rutaId}/puntos/{puntoId}/omision` | `PESAJE_CREAR` | 200 | 400, 403, 404, 409 |
| RF-B06 | Cerrar ruta | `POST /rutas/{id}/cierre` | `RUTA_CERRAR` | 200 | 403, 404, 409 (RN-04) |

### Ejemplos

**Login**

```http
POST /api/v1/auth/login
{ "username": "operario1", "password": "Eco2026*" }
```
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "expiraEn": 3600, "id": 1,
  "usuario": "operario1", "nombre": "Carlos Quispe Mamani", "rol": "OPERARIO",
  "privilegios": ["PESAJE_CREAR", "PESAJE_EDITAR", "PESAJE_LEER", "PUNTO_LEER"] }
```

**Registrar pesaje** (el cliente **nunca** envía el neto)

```http
POST /api/v1/puntos/4/pesajes
{ "rutaId": 2, "material": "Vidrio", "pesoBruto": 50, "tara": 5,
  "observacion": "Botellas", "claveCliente": "6f1c...-uuid" }
```
```json
{ "id": 16, "puntoId": 4, "rutaId": 2, "fechaHora": "2026-10-28T10:15:02", "material": "Vidrio",
  "pesoBruto": 50.0, "tara": 5.0, "pesoNeto": 45.0, "operario": "operario1",
  "estado": "REGISTRADO", "claveCliente": "6f1c...-uuid" }
```

**Envoltorio de paginación** (todos los listados)

```json
{ "contenido": [ ... ], "pagina": 0, "tamanio": 20, "totalElementos": 37, "totalPaginas": 2, "ultima": false }
```

**Formato de error** (todo el servicio, incluidos los 401/403 del filtro)

```json
{ "timestamp": "2026-10-27T09:14:22", "status": 400, "error": "Bad Request", "message": "Datos invalidos",
  "path": "/api/v1/puntos/1/pesajes",
  "validationErrors": { "tara": "La tara debe ser menor que el peso bruto: el peso neto debe ser mayor que cero" } }
```

### Códigos de estado

| Código | Cuándo |
|---|---|
| 200 | Consulta o actualización correcta |
| 201 | Creación correcta (pesaje, punto, ruta) |
| 204 | Anulación correcta, sin cuerpo |
| 400 | Validación fallida, con `validationErrors` por campo |
| 401 | Token ausente, alterado o vencido; credenciales incorrectas |
| 403 | Autenticado sin el privilegio, o RN-02 |
| 404 | El recurso no existe |
| 409 | Regla de negocio incumplida (RN-01, RN-04, pesaje ya anulado) |
| 500 | Fallo no controlado del servidor |

La colección de Postman con todas las peticiones (incluidos los ejemplos de 401, 403 y 400) está en [`postman/EcoAPI.postman_collection.json`](postman/EcoAPI.postman_collection.json).

---

## 4. Roles y privilegios

La autorización **siempre** se verifica contra el privilegio (autoridad de Spring Security), nunca contra el nombre del rol. La asignación rol → privilegios vive en la **base de datos** (tablas `rol`, `privilegio`, `rol_privilegio`, sembradas por `DatosSemilla`), y la exigencia de cada operación está centralizada en `SecurityConfig` con `hasAuthority(...)`.

| Privilegio | Permite | OPERARIO | SUPERVISOR | ADMINISTRADOR |
|---|---|:-:|:-:|:-:|
| `PUNTO_LEER` | Consultar la ruta y los puntos | Sí | Sí | Sí |
| `PESAJE_LEER` | Consultar los pesajes | Sí | Sí | Sí |
| `PESAJE_CREAR` | Registrar un pesaje | Sí | Sí | No |
| `PESAJE_EDITAR` | Corregir un pesaje | Solo propios del día | Sí | No |
| `PESAJE_EDITAR_TODOS` | Amplía la corrección a todos los pesajes | No | Sí | No |
| `PESAJE_ANULAR` | Anular un pesaje | No | Sí | No |
| `RUTA_CERRAR` | Cerrar la ruta del día | No | Sí | No |
| `PUNTO_GESTIONAR` | Crear puntos y asignar rutas | No | No | Sí |

> **Decisión:** la matriz del enunciado dice "PESAJE_EDITAR: solo los propios / todos". Para que esa diferencia no dependa de un `if (rol == "SUPERVISOR")`, se modeló como un privilegio adicional `PESAJE_EDITAR_TODOS`. `PesajeService` consulta `usuario.tiene(PESAJE_EDITAR_TODOS)`.

**Dónde se verifica:**
1. `JwtAuthenticationFilter` valida la **firma** y el **vencimiento** del token → si falla, 401.
2. `SecurityConfig` exige el privilegio de cada ruta → si falta, 403 (`ManejadorDeErrores.handle`).
3. `PesajeService` aplica RN-02 (propiedad y mismo día) → 403.

Ocultar el botón en la aplicación es solo experiencia de usuario: el servicio rechaza la operación aunque se invoque directamente.

### Agregar un rol nuevo (p. ej. AUDITOR)
Solo se agrega una entrada en la matriz `DatosSemilla.MATRIZ` (o se inserta en la base el rol y sus filas en `rol_privilegio`) y el usuario correspondiente. Ni el filtro ni los controladores cambian, porque nunca preguntan por el nombre del rol.

---

## 5. Reglas de negocio (capa `servicio/`)

| Regla | Dónde | Respuesta |
|---|---|---|
| **RN-01** No se registra, corrige ni anula en una ruta cerrada | `RutaService.exigirRutaAbierta`, usada por `PesajeService` | 409 |
| **RN-02** El operario solo corrige sus propios pesajes y del mismo día | `PesajeService.exigirPuedeCorregir` | 403 |
| **RN-03** Neto = bruto − tara, calculado por el servicio y > 0 | `Pesaje.calcularNeto` + `PesajeService.exigirNetoPositivo` | 400 con `validationErrors.tara` |
| **RN-04** La ruta no se cierra si hay puntos sin pesaje vigente ni motivo de omisión | `RutaService.cerrar` | 409 "faltan N puntos" |

---

## 6. Decisiones de arquitectura

- **JWT sin estado** (HS256, 1 hora, sin renovación). Transporta `sub` (id), `usr`, `nombre`, `rol` y `privilegios`. El servicio no guarda sesiones.
- **Contraseñas con BCrypt**; nunca se guardan en claro.
- **Un único punto de traducción de errores** (`ManejadorDeErrores`): es `@RestControllerAdvice`, `AuthenticationEntryPoint` y `AccessDeniedHandler` a la vez, así que todos los errores —incluidos los del filtro— salen con el mismo formato.
- **Anulación lógica:** `DELETE` marca `anulado=true` y guarda el motivo; la fila no se elimina.
- **Idempotencia para la sincronización offline:** la app envía una `claveCliente` (UUID) con cada pesaje; si la sincronización se reintenta después de un corte, el servicio devuelve el pesaje ya creado (200) en lugar de duplicarlo.
- **Identificadores positivos:** las claves se generan con `IDENTITY` (1, 2, 3…), por eso la app puede usar ids **negativos** para sus filas provisionales sin colisionar.
- **DTO separados de las entidades** y conversión dentro de la transacción del servicio.

---

## 7. Datos semilla (anexo, sección 10)

| Usuario | Contraseña | Rol |
|---|---|---|
| `operario1` | `Eco2026*` | OPERARIO |
| `supervisor1` | `Eco2026*` | SUPERVISOR |
| `admin` | `Eco2026*` | ADMINISTRADOR |

- 12 puntos de acopio en 3 zonas (Norte, Centro, Sur).
- 2 rutas de `operario1` supervisadas por `supervisor1`: la **del día, abierta, con 6 puntos** (3 pesados y 3 sin pesar → RN-04) y la **del día anterior, cerrada** (→ RN-01).
- 15 pesajes: 8 en la ruta cerrada y 7 en la del día, con **uno anulado** y **uno registrado por otro usuario** (`supervisor1`, → RN-02).

---

## 8. Solicitudes de cambio (Parte II)

| Rama | Solicitud | Cambio en el servicio | Prueba |
|---|---|---|---|
| `sc-b-plasencia` | Rol AUDITOR | Una fila en `DatosSemilla.MATRIZ` (`PUNTO_LEER`, `PESAJE_LEER`) y el usuario `auditor1`; `SecurityConfig` y los controladores no cambian | `AuditorTest` (lectura 200, escritura 403) |
| `sc-c-plasencia` | Humedad estimada | Campo en `Pesaje`, validación 0–100 % en las peticiones, `PesajeDto` y semilla | `HumedadEstimadaTest` |

Las solicitudes SC-A y SC-D solo tocan la aplicación.

## 9. Flujo de Git

Ramas: `main` (solo fusiones desde `develop`), `develop` (integración), `feature/*`, `fix/*`, `docs/*` y, en el examen, `sc-<letra>-<apellido>`. Todas las fusiones se hicieron con `--no-ff`. La versión evaluada se etiqueta `v1.0-unidad2`.

```bash
git log --graph --oneline --all
git log --date=short --pretty="%h %ad %s"
```
