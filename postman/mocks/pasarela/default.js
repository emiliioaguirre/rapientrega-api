const http = require('http');

http.createServer(async (req, res) => {
    function responder(codigo, datos) {
        res.writeHead(codigo, {'Content-Type': 'application/json'});
        res.end(JSON.stringify(datos));
    }

    const ruta = new URL(req.url, 'http://localhost').pathname;
    if (req.method !== 'POST' || !['/cobros', '/reembolsos'].includes(ruta)) {
        return responder(501, {mensaje: 'Operación no disponible en esta simulación'});
    }
    const datos = req.body || {};
    const clave = req.headers['idempotency-key'];
    if (!clave || !Number.isFinite(datos.monto) || datos.monto <= 0) {
        return responder(400, {mensaje: 'Se requieren clave y monto positivo'});
    }
    const llave = 'pasarela.resultado.' + clave;
    const anterior = await pm.state.get(llave);
    if (anterior) {
        return responder(200, anterior);
    }
    if (ruta === '/cobros') {
        if (!Number.isInteger(datos.pedidoId) || datos.pedidoId <= 0 || !datos.token) {
            return responder(400, {mensaje: 'Se requieren pedido y token de demostración'});
        }
        if (datos.token === 'tok_demo_error') {
            return responder(503, {mensaje: 'Pasarela temporalmente no disponible'});
        }
        const resultado = {
            referencia: 'COBRO-' + datos.pedidoId,
            estado: ['tok_demo_aprobado', 'tok_demo_reembolso_pendiente'].includes(datos.token) ? 'APROBADO' : 'RECHAZADO'
        };
        await pm.state.set(llave, resultado);
        await pm.state.set('pasarela.cobro.' + resultado.referencia, {
            monto: datos.monto,
            estado: resultado.estado,
            falloReembolso: datos.token === 'tok_demo_reembolso_pendiente'
        });
        return responder(200, resultado);
    }
    const cobro = await pm.state.get('pasarela.cobro.' + datos.referencia);
    if (!cobro || cobro.estado !== 'APROBADO') {
        return responder(404, {mensaje: 'No se encuentra un cobro aprobado'});
    }
    if (cobro.monto !== datos.monto) {
        return responder(400, {mensaje: 'El monto no corresponde al cobro'});
    }
    const intentado = 'pasarela.intento.' + clave;
    if (cobro.falloReembolso && !await pm.state.get(intentado)) {
        await pm.state.set(intentado, true);
        return responder(503, {mensaje: 'Reembolso temporalmente no disponible'});
    }
    const resultado = {referencia: 'REEMBOLSO-' + datos.referencia, estado: 'REEMBOLSADO'};
    await pm.state.set(llave, resultado);
    return responder(200, resultado);
}).listen(process.env.PORT || 4900);
