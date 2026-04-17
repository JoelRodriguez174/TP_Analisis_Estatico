import java.util.*;

public class NodoCFG {
    public static int contadorGlobal = 0;
    public int id;
    public String etiqueta;
    public NodoCFG ipdom;
    public List<NodoCFG> sucesores;
    public Set<NodoCFG> postdominadores;
    public List<NodoCFG> dependenciasControl;

    public NodoCFG(String etiqueta) {
        this.id = ++contadorGlobal;
        this.etiqueta = etiqueta;
        this.sucesores = new ArrayList<>();
        this.postdominadores = new HashSet<>();
        this.dependenciasControl = new ArrayList<>();
    }

    public void agregarSucesor(NodoCFG nodo) {
        if (nodo != null && !sucesores.contains(nodo)) {
            sucesores.add(nodo);
        }
    }
}