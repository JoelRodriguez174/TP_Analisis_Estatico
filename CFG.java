import java.util.*;

public class CFG {
    public NodoCFG start;
    public NodoCFG exit;
    public List<NodoCFG> nodos;

    public CFG(Nodo astRaiz) {
        NodoCFG.contadorGlobal = 0;
        nodos = new ArrayList<>();
        start = new NodoCFG("START");
        exit = new NodoCFG("EXIT");
        nodos.add(start);
        nodos.add(exit);
        
        // Buscar el nodo "Sentencias" dentro de "Programa" -> "Bloque"
        Nodo sentencias = null;
        if (astRaiz != null && astRaiz.hijos.size() >= 3) {
            Nodo bloque = astRaiz.hijos.get(2); // El Bloque
            if (bloque.hijos.size() >= 2) {
                sentencias = bloque.hijos.get(1); // Las Sentencias
            }
        }
        
        // Empezamos a armar el grafo recursivamente desde atrás hacia adelante
        NodoCFG primerNodo = procesarSentencias(sentencias, exit);
        start.agregarSucesor(primerNodo != null ? primerNodo : exit);
    }

    // Procesa la lista enlazada de sentencias del AST
    private NodoCFG procesarSentencias(Nodo nodoSentencias, NodoCFG siguiente) {
        if (nodoSentencias == null || nodoSentencias.hijos.isEmpty()) {
            return siguiente;
        }
        
        Nodo sentencia = nodoSentencias.hijos.get(0);
        Nodo restoSentencias = nodoSentencias.hijos.size() > 1 ? nodoSentencias.hijos.get(1) : null;
        
        // Recursión: construimos primero lo que va DESPUÉS, para saber a dónde apuntar
        NodoCFG nodoSiguienteReal = procesarSentencias(restoSentencias, siguiente);
        return procesarSentencia(sentencia, nodoSiguienteReal);
    }

    // Procesa una sentencia individual y genera sus nodos CFG
    private NodoCFG procesarSentencia(Nodo sentencia, NodoCFG siguiente) {
        if (sentencia == null) return siguiente;
        
        switch (sentencia.nombre) {
            case "Asignacion": {
                String id = sentencia.hijos.get(0).valor;
                NodoCFG n = new NodoCFG("Asignacion: " + id);
                n.agregarSucesor(siguiente);
                nodos.add(n);
                return n;
            }
            case "Return": {
                NodoCFG n = new NodoCFG("Return");
                n.agregarSucesor(exit); // Los retornos SIEMPRE apuntan a EXIT
                nodos.add(n);
                return n;
            }
            case "If": {
                NodoCFG nCond = new NodoCFG("If (Condicion)");
                nodos.add(nCond);
                
                NodoCFG nTrue = procesarSentencias(sentencia.hijos.get(1), siguiente);
                NodoCFG nFalse = procesarSentencias(sentencia.hijos.get(2), siguiente);
                
                nCond.agregarSucesor(nTrue != null ? nTrue : siguiente);
                nCond.agregarSucesor(nFalse != null ? nFalse : siguiente);
                return nCond;
            }
            case "While": {
                NodoCFG nCond = new NodoCFG("While (Condicion)");
                nodos.add(nCond);
                
                // El cuerpo del while apunta de nuevo a la condición
                NodoCFG nCuerpo = procesarSentencias(sentencia.hijos.get(1), nCond); 
                
                nCond.agregarSucesor(nCuerpo != null ? nCuerpo : nCond); // Camino True
                nCond.agregarSucesor(siguiente);                         // Camino False
                return nCond;
            }
            default:
                return siguiente;
        }
    }

    // Algoritmo iterativo para el Punto 2 (Computar Postdominadores)
    public void calcularPostdominadores() {
        // Inicialización
        for (NodoCFG n : nodos) {
            if (n == exit) {
                n.postdominadores.add(exit);
            } else {
                n.postdominadores.addAll(nodos); // Inicialmente todos post-dominan a todos
            }
        }
        
        boolean huboCambios = true;
        while (huboCambios) {
            huboCambios = false;
            for (NodoCFG n : nodos) {
                if (n == exit) continue; // El EXIT no cambia
                
                Set<NodoCFG> nuevaInterseccion = new HashSet<>();
                boolean primeraVez = true;
                
                // Intersección de los PDOM de los sucesores
                for (NodoCFG sucesor : n.sucesores) {
                    if (primeraVez) {
                        nuevaInterseccion.addAll(sucesor.postdominadores);
                        primeraVez = false;
                    } else {
                        nuevaInterseccion.retainAll(sucesor.postdominadores);
                    }
                }
                
                nuevaInterseccion.add(n); // Un nodo siempre se post-domina a sí mismo
                
                if (!n.postdominadores.equals(nuevaInterseccion)) {
                    n.postdominadores = nuevaInterseccion;
                    huboCambios = true;
                }
            }
        }
    }

    // Exporta el grafo a DOT para visualizarlo
    public String exportarDOT() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph CFG {\n");
        sb.append("  node [shape=box, fontname=\"Consolas\"];\n");
        
        for (NodoCFG n : nodos) {
            StringBuilder pdoms = new StringBuilder();
            for(NodoCFG p : n.postdominadores) pdoms.append(p.id).append(",");
            String pdomStr = pdoms.length() > 0 ? pdoms.substring(0, pdoms.length()-1) : "";
            
            sb.append("  n").append(n.id)
              .append(" [label=\"").append(n.id).append(": ").append(n.etiqueta)
              .append("\\nPDOM: {").append(pdomStr).append("}\"];\n");
        }
        
        for (NodoCFG n : nodos) {
            for (NodoCFG s : n.sucesores) {
                sb.append("  n").append(n.id).append(" -> n").append(s.id).append(";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

     // Punto 3, Arbol de Postdominadores
    public void calcularArbolPostdominadores() {
        // Para cada nodo, buscamos su postdominador inmediato
        for (NodoCFG n : nodos) {
            if (n == exit) continue; // El EXIT es la raíz del árbol, no tiene padre
            for (NodoCFG d : n.postdominadores) {
                if (d != n && d.postdominadores.size() == n.postdominadores.size() - 1) {
                    n.ipdom = d;
                    break;
                }
            }
        }
    }

    // Punto 4, Construcción de CDG
    public void calcularCDG() {
        for (NodoCFG u : nodos) {
            for (NodoCFG v : u.sucesores) {
                // Para cada arista (u, v) en el CFG, si v no post-domina a u:
                if (!u.postdominadores.contains(v)) {
                    NodoCFG lca = encontrarLCA(u, v);
                    NodoCFG temp = v;
                    // Subimos desde v hasta el LCA en el PDT
                    while (temp != lca && temp != null) {
                        temp.dependenciasControl.add(u);
                        temp = temp.ipdom;
                    }
                }
            }
        }

        // Si un nodo no tiene dependencias, su jefe es el START
        for (NodoCFG n : nodos) {
            if (n != start && n.dependenciasControl.isEmpty()) {
                n.dependenciasControl.add(start);
            }
        }
    }

    private NodoCFG encontrarLCA(NodoCFG u, NodoCFG v) {
        // Al ser un árbol de postdominadores, el LCA siempre existe (EXIT es la raíz común)
        Set<NodoCFG> ancestrosU = new HashSet<>();
        NodoCFG curr = u;
        while (curr != null) {
            ancestrosU.add(curr);
            curr = curr.ipdom;
        }
        curr = v;
        while (curr != null) {
            if (ancestrosU.contains(curr)) return curr;
            curr = curr.ipdom;
        }
        return exit;
    }
    
    public String exportarArbolPostdominadoresDOT() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph PDT {\n");
        sb.append("  node [shape=ellipse, fontname=\"Consolas\", style=filled, color=lightgreen];\n");
        
        for (NodoCFG n : nodos) {
            sb.append("  n").append(n.id)
              .append(" [label=\"").append(n.id).append(": ").append(n.etiqueta).append("\"];\n");
        }
        
        // Hacemos que la flecha vaya del ipdom (padre) hacia el nodo (hijo).
        for (NodoCFG n : nodos) {
            if (n.ipdom != null) {
                sb.append("  n").append(n.ipdom.id).append(" -> n").append(n.id).append(";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    public String exportarCDG_DOT() {
        StringBuilder sb = new StringBuilder("digraph CDG {\n  node [shape=note, style=filled, color=orange];\n");
        for (NodoCFG n : nodos) {
            for (NodoCFG dep : n.dependenciasControl) {
                sb.append("  n").append(dep.id).append(" -> n").append(n.id).append(";\n");
            }
            sb.append("  n").append(n.id).append(" [label=\"").append(n.id).append(": ").append(n.etiqueta).append("\"];\n");
        }
        return sb.append("}\n").toString();
    }
}