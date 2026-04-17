import java.util.ArrayList;
import java.util.List;


public class Nodo {
    // Contador para asignar IDs únicos a cada nodo
    private static int contadorGlobal = 0;

    public int id;             // ID único de este nodo
    public String nombre;      // Nombre o tipo del nodo 
    public String valor;       
    public List<Nodo> hijos;   // Lista de nodos hijos (estructura recursiva)

    public int linea;          // Línea en el código fuente (-1 si no está disponible)
    public int columna;        // Columna en el código fuente (-1 si no está disponible)

  
    public Nodo(String nombre) {
        this(nombre, null, -1, -1);
    }

    //Constructor que crea un nodo con nombre y valor pero sin ubicación.
    
    public Nodo(String nombre, String valor) {
        this(nombre, valor, -1, -1);
    }

    // Constructor que crea un nodo con nombre, valor y posición en el código.
    
    public Nodo(String nombre, String valor, int linea, int columna) {
        this.id = ++contadorGlobal; // asigna ID único automáticamente
        this.nombre = nombre;
        this.valor = valor;
        this.linea = linea;
        this.columna = columna;
        this.hijos = new ArrayList<>();
    }

    // Agrega un nodo hijo a la lista de hijos de este nodo.
   
    public void agregarHijo(Nodo hijo) {
        hijos.add(hijo);
    }

    /**
     * Devuelve una representación en texto del árbol a partir de este nodo.
     * Muestra indentación para visualizar la jerarquía y línea si está disponible.
     */
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(" ".repeat(indent));
        sb.append("[").append(id).append("] ").append(nombre);
        if (valor != null) sb.append(" : ").append(valor);
        if (linea >= 0) sb.append(" (línea ").append(linea + 1).append(")");
        sb.append("\n");
        // Recorre hijos recursivamente
        for (Nodo h : hijos) {
            sb.append(h.toString(indent + 2));
        }
        return sb.toString();
    }

    // Sobrescribe el toString para imprimir el árbol completo desde la raíz.
  
    @Override
    public String toString() {
        return toString(0);
    }

    //Genera representación en formato DOT para Graphviz.
   
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph AST {\n");
        sb.append("  node [shape=box, style=filled, color=lightblue, fontname=\"Consolas\"];\n");
        generarDot(sb);
        sb.append("}\n");
        return sb.toString();
    }

    // Método recursivo para recorrer el árbol y generar nodos/aristas en DOT
    private void generarDot(StringBuilder sb) {
        String etiqueta = nombre;
        if (valor != null) etiqueta += "\\n" + valor;  
        sb.append("  n").append(id)
          .append(" [label=\"").append(etiqueta).append("\"];\n");

        for (Nodo h : hijos) {
            sb.append("  n").append(id).append(" -> n").append(h.id).append(";\n");
            h.generarDot(sb);
        }
    }
}

