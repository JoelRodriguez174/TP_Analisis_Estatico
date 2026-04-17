import java.util.Map;
import java.util.HashMap;


public class TablaSimbolos {

    private static class Entrada {
        String tipo; // "int" o "bool"
        int valor;   
        int linea;  

        Entrada(String tipo, int valor, int linea) {
            this.tipo = tipo;
            this.valor = valor;
            this.linea = linea;
        }
    }

    private Map<String, Entrada> tabla = new HashMap<>();

    public void declarar(String nombre, String tipo, int linea) {
        if (tabla.containsKey(nombre)) {
            throw new RuntimeException("Error: variable '" + nombre + "' ya declarada (primera en línea " 
                + tabla.get(nombre).linea + ")");
        }
        tabla.put(nombre, new Entrada(tipo, 0, linea));
    }

    public void asignar(String nombre, int valor, int linea) {
        Entrada e = tabla.get(nombre);
        if (e == null) {
            throw new RuntimeException("Error en línea " + (linea+1) + ": variable '" + nombre + "' no declarada");
        }
        e.valor = valor;
    }

    public int obtener(String nombre, int linea) {
        Entrada e = tabla.get(nombre);
        if (e == null) {
            throw new RuntimeException("Error en línea " + (linea+1) + ": variable '" + nombre + "' no declarada");
        }
        return e.valor;
    }

    public String obtenerTipo(String nombre, int linea) {
        Entrada e = tabla.get(nombre);
        if (e == null) {
            throw new RuntimeException("Error en línea " + (linea+1) + ": variable '" + nombre + "' no declarada");
        }
        return e.tipo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{ ");
        for (var entry : tabla.entrySet()) {
            sb.append(entry.getKey())
              .append(" :")
              .append(entry.getValue().tipo)
              .append("=")
              .append(entry.getValue().valor)
              .append(" ");
        }
        sb.append("}");
        return sb.toString();
    }
}
