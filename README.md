<p align="center">
  <img src="docs/assets/rapientrega.svg" alt="Icono de RapiEntrega" width="112">
</p>

<h1 align="center">API de RapiEntrega</h1>

<p align="center">Pedidos de comida, pagos y seguimiento de entregas.</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=flat" alt="Spring Boot 4.1.1">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat" alt="MySQL">
  <img src="https://img.shields.io/badge/Postman-FF6C37?style=flat" alt="Postman">
</p>

## Resumen

La API gestiona pedidos de comida, pagos, repartidores y seguimiento de entregas. Organiza los servicios de catálogo, pedidos, pagos y logística.

1. El cliente elige restaurante y productos.
2. Se registra el pedido y se calcula el total con los precios del catálogo.
3. Se procesa el pago con la pasarela simulada.
4. El restaurante acepta o rechaza el pedido dentro de cinco minutos.
5. Se asigna el repartidor disponible más cercano y se sigue la entrega.
6. Se confirma la entrega, se registra el comprobante y se califica el servicio.

Si el restaurante rechaza o no acepta a tiempo, el pedido se cancela y se solicita el reembolso. La pasarela y el sistema de facturación de 2012 están simulados. Las notificaciones se registran mediante logger.

## Estructura del proyecto

```text
src/main/java/com/universidad/rapientrega/
├── RapientregaApplication.java        # Arranque de la aplicación
├── controllers/
│   ├── ClienteController.java         # Rutas de cliente
│   ├── PedidoController.java          # Rutas de pedido
│   ├── RepartidorController.java      # Rutas de repartidor
│   └── RestauranteController.java     # Rutas de restaurante
├── models/
│   ├── Calificacion.java              # Datos de calificación
│   ├── CambioPedido.java              # Evento de cambio de pedido
│   ├── Cliente.java                   # Datos de cliente
│   ├── DetallePedido.java             # Productos y cantidades del pedido
│   ├── Pago.java                      # Datos de pago
│   ├── Pedido.java                    # Datos de pedido
│   ├── Producto.java                  # Datos de producto
│   ├── Repartidor.java                # Datos de repartidor
│   └── Restaurante.java               # Datos de restaurante
├── repositories/
│   ├── CalificacionRepository.java    # Consultas de calificaciones
│   ├── ClienteRepository.java         # Consultas de cliente
│   ├── DetallePedidoRepository.java   # Consultas de detalles del pedido
│   ├── PagoRepository.java            # Consultas de pago
│   ├── PedidoRepository.java          # Consultas de pedido
│   ├── ProductoRepository.java        # Consultas de producto
│   ├── RepartidorRepository.java      # Consultas de repartidor
│   └── RestauranteRepository.java     # Consultas de restaurante
└── services/
    ├── ClienteService.java            # Operaciones de cliente
    ├── EntregaService.java            # Asignación y entrega
    ├── FacturacionService.java        # Comprobante del sistema heredado
    ├── NotificacionService.java       # Avisos por consola
    ├── PagoService.java               # Operaciones de pago
    ├── PasarelaService.java           # Cobro y reembolso por HTTP
    ├── PedidoScheduler.java           # Revisión de plazos y reembolsos
    ├── PedidoService.java             # Operaciones de pedido
    ├── RepartidorService.java         # Operaciones de repartidor
    ├── RestauranteService.java        # Operaciones de restaurante
    └── SeguimientoService.java        # Estado y eventos de seguimiento
```

Los controladores reciben las peticiones, los servicios aplican las reglas de negocio y los repositorios acceden a MySQL. Los modelos representan los datos y eventos de cada proceso.

## Instalación y ejecución

Requiere JDK 21 y MySQL en ejecución.

Crear la base desde MySQL Workbench y configurar las credenciales en un archivo `.env`, tomando como referencia `.env.example`.

```sql
CREATE DATABASE IF NOT EXISTS rapientrega CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS rapientrega_pruebas CHARACTER SET utf8mb4;
```

| Variable | Uso |
|---|---|
| `DB_URL` | Dirección de la base de datos. |
| `DB_USERNAME` | Usuario de MySQL. |
| `DB_PASSWORD` | Contraseña local de MySQL. |
| `PASARELA_URL` | Dirección del proveedor simulado. |
| `PASARELA_SESION` | Sesión que separa los datos del proveedor de demostración. |
| `TEST_DB_URL` | Base separada para las verificaciones de integración. |

Ejecutar `RapientregaApplication.java` con **Run Java**.

La pasarela requiere Postman CLI 1.70 o superior. Antes de procesar pagos, iniciarla desde la raíz del proyecto:

```bash
postman simulation run postman/simulations/pasarela.sim.yaml --port 4900 --no-history
```

La aplicación usa `http://127.0.0.1:4900/pasarela`. Los tokens de ejemplo representan resultados de prueba.

**URL base:** `http://localhost:8082/api/v1`.

## Endpoints

Las rutas de la tabla parten de `/api/v1`. Los registros reciben JSON.

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/clientes` | Listar clientes | 200 OK |
| POST | `/clientes` | Registrar cliente | 201 Created |
| GET | `/pedidos` | Listar pedidos | 200 OK |
| GET | `/pedidos/{id}` | Consultar pedido | 200 OK |
| POST | `/pedidos` | Registrar pedido | 201 Created |
| POST | `/pedidos/{id}/pagar` | Procesar pago | 200 OK |
| POST | `/pedidos/{id}/aceptar` | Aceptar pedido | 200 OK |
| POST | `/pedidos/{id}/rechazar` | Rechazar pedido | 200 OK |
| POST | `/pedidos/{id}/asignar` | Asignar repartidor | 200 OK |
| POST | `/pedidos/{id}/iniciar` | Iniciar entrega | 200 OK |
| POST | `/pedidos/{id}/entregar` | Entregar pedido | 200 OK |
| POST | `/pedidos/{id}/calificar` | Calificar servicio | 201 Created |
| GET | `/pedidos/{id}/seguimiento` | Consultar seguimiento | 200 OK |
| GET | `/pedidos/{id}/eventos` | Recibir eventos de seguimiento | 200 OK |
| GET | `/repartidores` | Listar repartidores | 200 OK |
| POST | `/repartidores` | Registrar repartidor | 201 Created |
| PUT | `/repartidores/{id}/ubicacion` | Actualizar ubicación | 200 OK |
| GET | `/restaurantes` | Listar restaurantes | 200 OK |
| GET | `/restaurantes/{id}` | Consultar restaurante | 200 OK |
| POST | `/restaurantes` | Registrar restaurante | 201 Created |
| GET | `/restaurantes/{id}/productos` | Listar productos | 200 OK |
| POST | `/restaurantes/{id}/productos` | Registrar producto | 201 Created |

## Postman

Importar la [colección de Postman](postman/RapiEntrega.postman_collection.json) y ejecutar las peticiones en orden. Las [instrucciones de uso](postman/README.md) explican cómo realizar las pruebas en Postman.

### Ejemplos de peticiones

Usar los IDs obtenidos en las respuestas.

**POST `/clientes`**

```json
{
  "nombre": "Emilio Alejandro Aguirre Tronco",
  "correo": "202360079@ucc.mx",
  "direccion": "Dirección de demostración en Veracruz"
}
```

**POST `/restaurantes`**

```json
{
  "nombre": "Cocina de prueba",
  "direccion": "Centro de Veracruz",
  "latitud": 19.17,
  "longitud": -96.12
}
```

**POST `/restaurantes/1/productos`**

```json
{
  "nombre": "Hamburguesa",
  "precio": 75,
  "disponible": true
}
```

**POST `/pedidos`**

```json
{
  "cliente": {
    "id": 1
  },
  "restaurante": {
    "id": 1
  },
  "detalles": [
    {
      "producto": {
        "id": 1
      },
      "cantidad": 2
    }
  ]
}
```

**POST `/pedidos/1/pagar`**

```json
{
  "token": "tok_demo_aprobado"
}
```

**POST `/repartidores`**

```json
{
  "nombre": "Repartidor de prueba",
  "latitud": 19.1701,
  "longitud": -96.1201,
  "disponible": true
}
```

**PUT `/repartidores/1/ubicacion`**

```json
{
  "latitud": 19.175,
  "longitud": -96.125
}
```

**POST `/pedidos/1/calificar`**

```json
{
  "puntaje": 5,
  "comentario": "Entrega recibida correctamente"
}
```

### Pago y seguimiento

- El precio del pedido se calcula con el catálogo, sin aceptar importes enviados por el cliente.
- Al completar el pago, el restaurante tiene cinco minutos para aceptar; la tarea revisa vencimientos cada cinco segundos.
- Un reembolso no confirmado queda como `REEMBOLSO_PENDIENTE` y se vuelve a intentar.

Consultar el estado y la ubicación con `GET /pedidos/{id}/seguimiento`. La ruta `/pedidos/{id}/eventos` entrega las actualizaciones mediante SSE. Cuando el pedido termina, el flujo se cierra.

La asignación puede esperar si no hay repartidores disponibles. El endpoint de asignar permite volver a intentarlo; un pedido ya asignado devuelve conflicto. Cada pedido registra un resultado de pago: repetir la petición conserva ese resultado.

## Manejo de errores

Los servicios validan los datos y el estado antes de guardar. Los rechazos incluyen el código HTTP y el motivo en `message`.

| Código HTTP | Motivo |
|---|---|
| `400 Bad Request` | Datos obligatorios ausentes o valores inválidos. |
| `404 Not Found` | No se encuentra el registro consultado. |
| `409 Conflict` | Operación incompatible con el estado del pedido o calificación duplicada. |
| `502 Bad Gateway` | La pasarela no está disponible o devuelve una respuesta inválida. |
