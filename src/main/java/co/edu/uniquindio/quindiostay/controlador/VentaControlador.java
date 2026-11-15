package co.edu.uniquindio.quindiostay.controlador;

import co.edu.uniquindio.quindiostay.dao.VistaDAO;
import co.edu.uniquindio.quindiostay.modelo.*;
import co.edu.uniquindio.quindiostay.servicio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.*;

/**
 * Venta de boletas (operador y administrador en taquilla; el cliente en línea).
 * Paso 1: escoger la función. Paso 2: cantidades por zona y tipo de entrada, plan y cliente.
 * Combos: escoger el combo y, para cada atracción, la función y zona.
 */
@Controller
@RequestMapping("/venta")
public class VentaControlador {

    private final VentaServicio ventaServicio;
    private final VistaDAO vistaDAO;
    private final FuncionServicio funcionServicio;
    private final AtraccionServicio atraccionServicio;
    private final TipoEntradaServicio tipoEntradaServicio;
    private final ClienteServicio clienteServicio;
    private final ComboServicio comboServicio;

    public VentaControlador(VentaServicio ventaServicio, VistaDAO vistaDAO, FuncionServicio funcionServicio,
                            AtraccionServicio atraccionServicio, TipoEntradaServicio tipoEntradaServicio,
                            ClienteServicio clienteServicio, ComboServicio comboServicio) {
        this.ventaServicio = ventaServicio;
        this.vistaDAO = vistaDAO;
        this.funcionServicio = funcionServicio;
        this.atraccionServicio = atraccionServicio;
        this.tipoEntradaServicio = tipoEntradaServicio;
        this.clienteServicio = clienteServicio;
        this.comboServicio = comboServicio;
    }

    @GetMapping
    public String escogerFuncion(@RequestParam(required = false) Integer atraccion, Model model) throws SQLException {
        model.addAttribute("atracciones", atraccionServicio.listarOpciones());
        model.addAttribute("atraccion", atraccion);
        model.addAttribute("funciones", vistaDAO.funcionesParaVender(atraccion));
        return "venta/funciones";
    }

    @GetMapping("/funcion/{id}")
    public String formulario(@PathVariable int id, HttpSession session, Model model,
                             RedirectAttributes redirect) throws SQLException {
        Funcion f = funcionServicio.buscarPorId(id);
        try {
            ventaServicio.validarFuncionVendible(f);   // terminada, pasada o cancelada: no se abre el formulario
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/venta";
        }
        model.addAttribute("funcion", f);
        model.addAttribute("atraccion", atraccionServicio.buscarPorId(f.getIdAtraccion()));
        model.addAttribute("zonas", vistaDAO.zonasDeFuncion(id));
        model.addAttribute("tipos", tipoEntradaServicio.listarTodos());
        model.addAttribute("planes", vistaDAO.planesParaVender(f.getIdAtraccion(), f.getFecha()));
        cargarCliente(session, model);
        return "venta/formulario";
    }

    @PostMapping("/funcion/{id}")
    public String vender(@PathVariable int id, @RequestParam Map<String, String> datos, HttpSession session,
                         RedirectAttributes redirect) {
        SesionUsuario s = Sesiones.de(session);
        try {
            // Las cantidades llegan como cant_<idZona>_<idTipoEntrada>
            List<LineaVenta> lineas = new ArrayList<>();
            for (Map.Entry<String, String> e : datos.entrySet()) {
                if (e.getKey().startsWith("cant_") && !e.getValue().isBlank()) {
                    String[] p = e.getKey().split("_");
                    lineas.add(new LineaVenta(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(e.getValue())));
                }
            }
            int idCliente = s.isCliente() ? s.getIdCliente() : entero(datos.get("idCliente"), "Escoja el cliente.");
            String canal = s.isCliente() ? "web" : datos.getOrDefault("canal", "mostrador");
            int idPlan = entero(datos.get("idPlan"), "Escoja el plan.");
            int idReserva = ventaServicio.venderBoletas(idCliente, canal, id, idPlan, lineas);
            redirect.addFlashAttribute("exito", "Reserva #" + idReserva + " creada con sus boletas. Queda pendiente hasta que se pague.");
            return "redirect:" + (s.isCliente() ? "/mi-cuenta/reservas/" : "/reservas/") + idReserva;
        } catch (ReglaNegocioException | NumberFormatException e) {
            redirect.addFlashAttribute("error", e instanceof NumberFormatException ? "Las cantidades deben ser números enteros." : e.getMessage());
            return "redirect:/venta/funcion/" + id;
        }
    }

    // ---------------- Combos ----------------
    @GetMapping("/combo")
    public String escogerCombo(Model model) {
        model.addAttribute("combos", comboServicio.listarTodos().stream().filter(c -> "activo".equals(c.getEstado())).toList());
        return "venta/combos";
    }

    @GetMapping("/combo/{id}")
    public String formularioCombo(@PathVariable int id, HttpSession session, Model model) throws SQLException {
        model.addAttribute("combo", comboServicio.buscarPorId(id));
        Map<Atraccion, List<Map<String, Object>>> zonasPorAtraccion = new LinkedHashMap<>();
        for (Integer idAtraccion : comboServicio.atraccionesDelCombo(id)) {
            zonasPorAtraccion.put(atraccionServicio.buscarPorId(idAtraccion), vistaDAO.zonasFuturasDeAtraccion(idAtraccion));
        }
        model.addAttribute("zonasPorAtraccion", zonasPorAtraccion);
        model.addAttribute("tipos", tipoEntradaServicio.listarTodos());
        model.addAttribute("planes", vistaDAO.planesParaCombo(id, Reloj.hoy()));
        cargarCliente(session, model);
        return "venta/formulario-combo";
    }

    @PostMapping("/combo/{id}")
    public String venderCombo(@PathVariable int id, @RequestParam Map<String, String> datos, HttpSession session,
                              RedirectAttributes redirect) {
        SesionUsuario s = Sesiones.de(session);
        try {
            Map<Integer, Integer> zonaPorAtraccion = new HashMap<>();
            for (Map.Entry<String, String> e : datos.entrySet()) {
                if (e.getKey().startsWith("zona_") && e.getValue() != null && !e.getValue().isBlank()) {
                    zonaPorAtraccion.put(entero(e.getKey().substring(5), "Datos de zona inválidos."),
                            entero(e.getValue(), "Escoja una zona válida."));
                }
            }
            int idCliente = s.isCliente() ? s.getIdCliente() : entero(datos.get("idCliente"), "Escoja el cliente.");
            String canal = s.isCliente() ? "web" : datos.getOrDefault("canal", "mostrador");
            // Unidades por tipo de entrada (unidades_<idTipo>) para poder comprar en la
            // misma reserva, p. ej., 1 adulto mayor + 1 niño. Si llega el formato
            // anterior (unidades + idTipoEntrada) también se acepta.
            Map<Integer, Integer> unidadesPorTipo = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : datos.entrySet()) {
                if (e.getKey().startsWith("unidades_") && e.getValue() != null && !e.getValue().isBlank()) {
                    unidadesPorTipo.put(entero(e.getKey().substring(9), "Datos de tipo de entrada inválidos."),
                            entero(e.getValue(), "Escriba las unidades con números."));
                }
            }
            if (unidadesPorTipo.isEmpty()) {
                unidadesPorTipo.put(entero(datos.get("idTipoEntrada"), "Escoja el tipo de entrada."),
                        entero(datos.get("unidades"), "Escriba las unidades."));
            }
            int idReserva = ventaServicio.venderCombo(idCliente, canal, id, unidadesPorTipo,
                    entero(datos.get("idPlan"), "Escoja el plan."), zonaPorAtraccion);
            redirect.addFlashAttribute("exito", "Reserva #" + idReserva + " creada con las boletas del combo.");
            return "redirect:" + (s.isCliente() ? "/mi-cuenta/reservas/" : "/reservas/") + idReserva;
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/venta/combo/" + id;
        }
    }

    private void cargarCliente(HttpSession session, Model model) {
        if (!Sesiones.de(session).isCliente()) {
            model.addAttribute("opcCliente", clienteServicio.listarOpciones());
        }
    }

    private int entero(String valor, String mensajeSiFalta) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException(mensajeSiFalta);
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("«" + valor + "» no es un número válido.");
        }
    }
}
