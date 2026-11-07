package co.edu.uniquindio.quindiostay.modelo;

/** Una línea del formulario de venta: cuántas boletas de un tipo en una zona. */
public class LineaVenta {

    private final int idZona;
    private final int idTipoEntrada;
    private final int cantidad;

    public LineaVenta(int idZona, int idTipoEntrada, int cantidad) {
        this.idZona = idZona;
        this.idTipoEntrada = idTipoEntrada;
        this.cantidad = cantidad;
    }

    public int getIdZona() { return idZona; }
    public int getIdTipoEntrada() { return idTipoEntrada; }
    public int getCantidad() { return cantidad; }
}
