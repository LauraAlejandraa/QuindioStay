package co.edu.uniquindio.quindiostay.servicio;

import co.edu.uniquindio.quindiostay.dao.AtraccionDAO;
import co.edu.uniquindio.quindiostay.dao.ConexionBD;
import co.edu.uniquindio.quindiostay.dao.PrestadorAtraccionDAO;
import co.edu.uniquindio.quindiostay.modelo.PrestadorAtraccion;
import java.sql.Connection;
import java.time.LocalDate;
import co.edu.uniquindio.quindiostay.modelo.Atraccion;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de Atraccion. El controlador nunca llama al DAO
 * directamente: pasa por aquí, donde van las reglas que un CHECK no puede revisar.
 * Si MySQL rechaza algo (FK, CHECK, UNIQUE) se traduce a un mensaje entendible.
 */
@Service
public class AtraccionServicio {

    private final AtraccionDAO dao;
    private final PrestadorAtraccionDAO afiliacionDAO;
    private final ConexionBD conexionBD;


    public AtraccionServicio(AtraccionDAO dao, PrestadorAtraccionDAO afiliacionDAO, ConexionBD conexionBD) {
        this.dao = dao;
        this.afiliacionDAO = afiliacionDAO;
        this.conexionBD = conexionBD;
    }

    public List<Atraccion> listarTodos() {
        try {
            return dao.listarTodos();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Atraccion buscarPorId(int id) {
        try {
            Atraccion x = dao.buscarPorId(id);
            if (x == null) {
                throw new ReglaNegocioException("No existe atracción con id " + id + ".");
            }
            return x;
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Crea si no tiene id, o actualiza si ya lo tiene. */
    public void guardar(Atraccion x) {
        try {
            validar(x);
            if (x.getIdAtraccion() == null) {
                dao.crear(x);
            } else {
                dao.actualizar(x);
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * Eliminar: borra la fila (Tabla 39 de la Entrega 2). Solo se puede si la atracción
     * no tiene funciones (ni, por lo tanto, boletas o reservas) y no está en ningún combo.
     * Sus afiliaciones a prestadores se borran en la misma transacción, porque sin
     * funciones la afiliación no tiene historial de ventas.
     *
     * @param idPrestadorEncargado prestador del encargado que elimina; null si es el administrador.
     *        El encargado no puede eliminar una atracción que también está afiliada a otro prestador.
     * @return cuántas afiliaciones se quitaron junto con la atracción.
     */
    public int eliminar(int id, Integer idPrestadorEncargado) {
        Atraccion a = buscarPorId(id);
        try {
            List<String> motivos = new java.util.ArrayList<>();
            int funciones = dao.contarFunciones(id);
            if (funciones > 0) {
                int boletas = dao.contarBoletas(id);
                motivos.add("tiene " + funciones + (funciones == 1 ? " función registrada" : " funciones registradas")
                        + (boletas > 0 ? " con " + boletas + (boletas == 1 ? " boleta vendida" : " boletas vendidas") : "")
                        + " (las funciones son historial y no se borran; si ya no la va a ofrecer, cancele sus funciones programadas)");
            }
            List<String> combos = dao.combosDe(id);
            if (!combos.isEmpty()) {
                motivos.add("forma parte " + (combos.size() == 1 ? "del combo «" : "de los combos «")
                        + String.join("», «", combos) + "» (quítela primero del combo)");
            }
            if (!motivos.isEmpty()) {
                throw new ReglaNegocioException("No se puede eliminar la atracción «" + a.getNombre() + "» porque "
                        + String.join(" y ", motivos) + ".");
            }
            Map<Integer, String> afiliados = dao.prestadoresAfiliados(id);
            if (idPrestadorEncargado != null) {
                List<String> otros = afiliados.entrySet().stream()
                        .filter(en -> !en.getKey().equals(idPrestadorEncargado)).map(Map.Entry::getValue).toList();
                if (!otros.isEmpty()) {
                    throw new ReglaNegocioException("La atracción también está afiliada a otro prestador ("
                            + String.join(", ", otros) + "): no la puede eliminar. Si su prestador ya no la ofrece, "
                            + "pida al administrador que quite solo su afiliación.");
                }
            }
            try (Connection con = conexionBD.getConexion()) {
                con.setAutoCommit(false);
                try {
                    afiliacionDAO.eliminarPorAtraccion(con, id);
                    dao.eliminar(con, id);
                    con.commit();
                } catch (SQLException e) {
                    con.rollback();
                    throw e;
                }
            }
            return afiliados.size();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public Map<Integer, String> listarOpciones() {
        try {
            return dao.listarOpciones();
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Lo que ve el encargado: solo las atracciones afiliadas a su prestador. */
    public List<Atraccion> listarPorPrestador(int idPrestador) {
        try {
            return dao.listarPorPrestador(idPrestador);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public List<Atraccion> buscarPorNombre(String texto) {
        try {
            return dao.buscarPorNombre(texto);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    public boolean perteneceAPrestador(int idAtraccion, int idPrestador) {
        try {
            return afiliacionDAO.estaAfiliado(idPrestador, idAtraccion);
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /**
     * El encargado crea una atracción: se guarda la atracción y su afiliación a su
     * prestador en una sola transacción (toda atracción tiene al menos un prestador).
     */
    public void crearParaPrestador(Atraccion x, int idPrestador) {
        try (Connection con = conexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                int id = dao.crear(con, x);
                PrestadorAtraccion pa = new PrestadorAtraccion();
                pa.setIdPrestador(idPrestador);
                pa.setIdAtraccion(id);
                pa.setFechaAfiliacion(Reloj.hoy());
                afiliacionDAO.crear(con, pa);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ReglaNegocioException(MensajesError.traducir(e));
        }
    }

    /** Reglas que miran otras tablas u otras filas (no caben en un CHECK). */
    private void validar(Atraccion x) throws SQLException {
        if (x.getNombre() == null || x.getNombre().isBlank()) {
            throw new ReglaNegocioException("El nombre de la atracción no puede quedar vacío.");
        }
        if (x.getDescripcion() == null || x.getDescripcion().isBlank()) {
            throw new ReglaNegocioException("La descripción de la atracción no puede quedar vacía.");
        }
        if (x.getUbicacion() == null || x.getUbicacion().isBlank()) {
            throw new ReglaNegocioException("La ubicación de la atracción no puede quedar vacía.");
        }
    }
}
