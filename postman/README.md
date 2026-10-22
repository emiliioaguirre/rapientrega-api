<p align="center">
  <img src="../docs/assets/rapientrega.svg" alt="RapiEntrega" width="112">
</p>

<h1 align="center">RapiEntrega en Postman</h1>

Me apoyé en IA para preparar esta colección de peticiones y organizar las llamadas a la API.

Importar [RapiEntrega.postman_collection.json](RapiEntrega.postman_collection.json) desde **Import > Files**. Iniciar la API y ejecutar las 43 peticiones en orden.

Iniciar también la pasarela simulada indicada en el README principal. La colección prepara el catálogo y el flujo de un pedido, incluyendo asignación, seguimiento, entrega y calificación. Para reproducir el recorrido completo, usar una base nueva sin repartidores previamente registrados.

Los tokens `tok_demo_aprobado` y `tok_demo_rechazado` representan los dos resultados de cobro. `tok_demo_reembolso_pendiente` permite comprobar el reintento de reembolso cuando la pasarela no lo confirma a la primera.

La consulta de vencimiento espera los cinco minutos reales desde el pago. Las fechas y estados del servidor no se modifican para adelantar la cancelación.

Los ejemplos se incluyen con fines prácticos para comprender el proceso y las reglas del servicio.
