package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.modelo.SesionUsuario;
import co.edu.uniquindio.quindiostay.servicio.PdfServicio;
import co.edu.uniquindio.quindiostay.servicio.ReglaNegocioException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.sql.SQLException;
import java.util.Map;

/**
 * Plantillas de documentos en PDF (RF16): comprobante de reserva, comprobante de pago,
 * factura, boleta y autorización de datos personales, con los datos reales.
 * El cliente solo descarga documentos de SUS reservas, pagos y datos.
 */
@Controller
@RequestMapping("/plantillas")
public class PlantillaControlador {

    private final PdfServicio pdf;
    private final VistaDAO vistaDAO;

    public PlantillaControlador(PdfServicio pdf, VistaDAO vistaDAO) {
        this.pdf = pdf;
        this.vistaDAO = vistaDAO;
    }

    @GetMapping("/reserva/{id}")
    public ResponseEntity<byte[]> reserva(@PathVariable int id, HttpSession s) {
        duenoReserva(id, s);
        return respuesta(pdf.comprobanteReserva(id), "comprobante-reserva-" + id);
    }

    @GetMapping("/pago/{id}")
    public ResponseEntity<byte[]> pago(@PathVariable int id, HttpSession s) throws SQLException {
        var p = vistaDAO.consultar("SELECT id_reserva FROM Pago WHERE id_pago = ?", id);
        if (p.isEmpty()) throw new ReglaNegocioException("No existe el pago " + id + ".");
        duenoReserva(((Number) p.get(0).get("id_reserva")).intValue(), s);
        return respuesta(pdf.comprobantePago(id), "comprobante-pago-" + id);
    }

    @GetMapping("/factura/{id}")
    public ResponseEntity<byte[]> factura(@PathVariable int id, HttpSession s) {
        duenoReserva(id, s);
        return respuesta(pdf.factura(id), "factura-" + id);
    }

    @GetMapping("/boletas/{id}")
    public ResponseEntity<byte[]> boletas(@PathVariable int id, HttpSession s) {
        duenoReserva(id, s);
        return respuesta(pdf.boletas(id, true), "boletas-reserva-" + id);
    }

    @GetMapping("/autorizacion/{idCliente}")
    public ResponseEntity<byte[]> autorizacion(@PathVariable int idCliente, HttpSession s) {
        SesionUsuario u = Sesiones.de(s);
        if (u.isCliente() && !u.getIdCliente().equals(idCliente)) {
            throw new ReglaNegocioException("Ese documento no es suyo.");
        }
        return respuesta(pdf.autorizacionDatos(idCliente), "autorizacion-datos-" + idCliente);
    }

    private void duenoReserva(int idReserva, HttpSession s) {
        SesionUsuario u = Sesiones.de(s);
        try {
            Map<String, Object> d = vistaDAO.reservaDetalle(idReserva);
            if (d == null) throw new ReglaNegocioException("No existe la reserva " + idReserva + ".");
            if (u.isCliente() && !u.getIdCliente().equals(d.get("id_cliente"))) {
                throw new ReglaNegocioException("Ese documento no es suyo.");
            }
            if (u.isEncargado() && !u.getIdPrestador().equals(d.get("id_prestador"))) {
                throw new ReglaNegocioException("Esa reserva no pertenece a su prestador.");
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException("Error de base de datos: " + e.getMessage());
        }
    }

    private ResponseEntity<byte[]> respuesta(byte[] bytes, String nombre) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + nombre + ".pdf").body(bytes);
    }
}
