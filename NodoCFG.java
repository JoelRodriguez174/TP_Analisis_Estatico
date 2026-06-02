import java.util.*;

public class NodoCFG {
    public static int contadorGlobal = 0;
    public int id;
    public String etiqueta;
    public int linea;
    public int columna;
    public NodoCFG ipdom;
    public List<NodoCFG> sucesores;
    public Set<NodoCFG> postdominadores;
    public List<NodoCFG> dependenciasControl;
    
    // Campos para dependencias de datos
    public String variableDefinida;
    public Set<String> variablesUsadas;
    public Set<NodoCFG> dependenciasDatos;

    public NodoCFG(String etiqueta) {
        this(etiqueta, -1, -1);
    }

    public NodoCFG(String etiqueta, int linea, int columna) {
        this.id = ++contadorGlobal;
        this.etiqueta = etiqueta;
        this.linea = linea;
        this.columna = columna;
        this.sucesores = new ArrayList<>();
        this.postdominadores = new HashSet<>();
        this.dependenciasControl = new ArrayList<>();
        this.variablesUsadas = new HashSet<>();
        this.dependenciasDatos = new HashSet<>();
        this.variableDefinida = null;
    }

    public void agregarSucesor(NodoCFG nodo) {
        if (nodo != null && !sucesores.contains(nodo)) {
            sucesores.add(nodo);
        }
    }
}