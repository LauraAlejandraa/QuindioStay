package co.edu.uniquindio.quindiostay.modelo;

import java.util.List;

/**
 * Definición de un reporte: su pregunta de negocio, su nivel, la consulta SQL
 * (probada con los datos reales) y, si lleva gráfico, qué columnas se grafican.
 * Los filtros son parámetros opcionales: la consulta usa (? IS NULL OR campo = ?).
 */
public class Reporte {

    public static class Filtro {
        public final String nombre, etiqueta, tipo;   // tipo: texto | fecha | lista
        public final List<String> opciones;
        public Filtro(String nombre, String etiqueta, String tipo, List<String> opciones) {
            this.nombre = nombre; this.etiqueta = etiqueta; this.tipo = tipo; this.opciones = opciones;
        }
        public String getNombre() { return nombre; }
        public String getEtiqueta() { return etiqueta; }
        public String getTipo() { return tipo; }
        public List<String> getOpciones() { return opciones; }
    }

    private final int numero;
    private final String titulo, nivel, pregunta, sql, columnaEtiqueta, columnaValor;
    private final List<Filtro> filtros;
    /** Cuántas veces se repite cada filtro dentro del SQL (los ? van en el orden de los filtros). */
    private final boolean grafico;

    public Reporte(int numero, String titulo, String nivel, String pregunta, String sql,
                   List<Filtro> filtros, String columnaEtiqueta, String columnaValor) {
        this.numero = numero; this.titulo = titulo; this.nivel = nivel; this.pregunta = pregunta;
        this.sql = sql; this.filtros = filtros; this.columnaEtiqueta = columnaEtiqueta;
        this.columnaValor = columnaValor; this.grafico = columnaEtiqueta != null;
    }

    public int getNumero() { return numero; }
    public String getTitulo() { return titulo; }
    public String getNivel() { return nivel; }
    public String getPregunta() { return pregunta; }
    public String getSql() { return sql; }
    public List<Filtro> getFiltros() { return filtros; }
    public String getColumnaEtiqueta() { return columnaEtiqueta; }
    public String getColumnaValor() { return columnaValor; }
    public boolean isGrafico() { return grafico; }
}
