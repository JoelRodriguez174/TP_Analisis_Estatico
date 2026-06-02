import java.util.*;
import java.io.*;

public class CFG {
    public NodoCFG start;
    public NodoCFG exit;
    public List<NodoCFG> nodos;
    public Nodo astRaiz;

    public CFG(Nodo astRaiz) {
        this.astRaiz = astRaiz;
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
                String expr = getExpressionString(sentencia.hijos.get(1));
                NodoCFG n = new NodoCFG("Asignacion: " + id + " = " + expr, sentencia.linea, sentencia.columna);
                n.variableDefinida = id;
                extraerVariablesUsadas(sentencia.hijos.get(1), n.variablesUsadas);
                n.agregarSucesor(siguiente);
                nodos.add(n);
                return n;
            }
            case "Return": {
                String expr = getExpressionString(sentencia);
                NodoCFG n = new NodoCFG("Return " + expr, sentencia.linea, sentencia.columna);
                if (!sentencia.hijos.isEmpty()) {
                    extraerVariablesUsadas(sentencia.hijos.get(0), n.variablesUsadas);
                }
                n.agregarSucesor(exit); // Los retornos SIEMPRE apuntan a EXIT
                nodos.add(n);
                return n;
            }
            case "If": {
                String cond = getExpressionString(sentencia.hijos.get(0));
                NodoCFG nCond = new NodoCFG("If (" + cond + ")", sentencia.linea, sentencia.columna);
                extraerVariablesUsadas(sentencia.hijos.get(0), nCond.variablesUsadas);
                nodos.add(nCond);
                
                NodoCFG nTrue = procesarSentencias(sentencia.hijos.get(1), siguiente);
                NodoCFG nFalse = procesarSentencias(sentencia.hijos.get(2), siguiente);
                
                nCond.agregarSucesor(nTrue != null ? nTrue : siguiente);
                nCond.agregarSucesor(nFalse != null ? nFalse : siguiente);
                return nCond;
            }
            case "While": {
                String cond = getExpressionString(sentencia.hijos.get(0));
                NodoCFG nCond = new NodoCFG("While (" + cond + ")", sentencia.linea, sentencia.columna);
                extraerVariablesUsadas(sentencia.hijos.get(0), nCond.variablesUsadas);
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

    private void extraerVariablesUsadas(Nodo n, Set<String> usadas) {
        if (n == null) return;
        if ("Identificador".equals(n.nombre)) {
            usadas.add(n.valor);
        }
        for (Nodo hijo : n.hijos) {
            extraerVariablesUsadas(hijo, usadas);
        }
    }

    public void calcularFDG() {
        // 1. Construir el mapa de predecesores
        Map<NodoCFG, List<NodoCFG>> predecesores = new HashMap<>();
        for (NodoCFG n : nodos) {
            predecesores.put(n, new ArrayList<>());
        }
        for (NodoCFG n : nodos) {
            for (NodoCFG suc : n.sucesores) {
                predecesores.get(suc).add(n);
            }
        }

        // 2. Inicializar conjuntos IN y OUT para Definiciones Alcanzables (Reaching Definitions)
        Map<NodoCFG, Set<NodoCFG>> inDefs = new HashMap<>();
        Map<NodoCFG, Set<NodoCFG>> outDefs = new HashMap<>();
        
        for (NodoCFG n : nodos) {
            inDefs.put(n, new HashSet<>());
            Set<NodoCFG> outSet = new HashSet<>();
            if (n.variableDefinida != null) {
                outSet.add(n); // GEN[n]
            }
            outDefs.put(n, outSet);
        }

        // 3. Algoritmo iterativo de punto fijo
        boolean huboCambios = true;
        while (huboCambios) {
            huboCambios = false;
            for (NodoCFG n : nodos) {
                // IN[n] = U_{p \in preds(n)} OUT[p]
                Set<NodoCFG> nuevoIn = new HashSet<>();
                for (NodoCFG p : predecesores.get(n)) {
                    nuevoIn.addAll(outDefs.get(p));
                }
                inDefs.put(n, nuevoIn);

                // OUT[n] = GEN[n] U (IN[n] - KILL[n])
                Set<NodoCFG> nuevoOut = new HashSet<>();
                if (n.variableDefinida != null) {
                    nuevoOut.add(n);
                }
                for (NodoCFG defNode : nuevoIn) {
                    if (n.variableDefinida == null || !defNode.variableDefinida.equals(n.variableDefinida)) {
                        nuevoOut.add(defNode);
                    }
                }

                if (!outDefs.get(n).equals(nuevoOut)) {
                    outDefs.put(n, nuevoOut);
                    huboCambios = true;
                }
            }
        }

        // 4. Calcular dependencias de flujo (dependenciasDatos)
        for (NodoCFG n : nodos) {
            for (String var : n.variablesUsadas) {
                for (NodoCFG reachingDef : inDefs.get(n)) {
                    if (var.equals(reachingDef.variableDefinida)) {
                        n.dependenciasDatos.add(reachingDef);
                    }
                }
            }
        }

        // 5. Conectar nodos del flujo principal (control-dependientes de START) al nodo START en el FDG
        for (NodoCFG n : nodos) {
            if (n == start || n == exit) continue;
            if (n.dependenciasControl.contains(start)) {
                n.dependenciasDatos.add(start);
            }
        }
    }

    private String getExpressionString(Nodo n) {
        if (n == null) return "";
        switch (n.nombre) {
            case "Identificador":
            case "Numero":
            case "Bool":
                return n.valor;
            case "Suma":
                return getExpressionString(n.hijos.get(0)) + " + " + getExpressionString(n.hijos.get(1));
            case "Multiplicacion":
                return getExpressionString(n.hijos.get(0)) + " * " + getExpressionString(n.hijos.get(1));
            case "Menor":
                return getExpressionString(n.hijos.get(0)) + " < " + getExpressionString(n.hijos.get(1));
            case "Return":
                if (n.valor != null && n.valor.equals("void")) return "";
                return !n.hijos.isEmpty() ? getExpressionString(n.hijos.get(0)) : "";
            default:
                if (!n.hijos.isEmpty()) return getExpressionString(n.hijos.get(0));
                return "";
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
                    // Subimos desde v hasta el LCA en el PDT (excluyendo el LCA)
                    while (temp != lca && temp != null) {
                        temp.dependenciasControl.add(u);
                        temp = temp.ipdom;
                    }
                    
                    // CASO ESPECIAL WHILE: Si u es una condición de ciclo, 
                    // debe depender de sí mismo. En un while, la arista true v 
                    // no post-domina a u. Si u no es el LCA, ya se agregó arriba.
                    // Pero si u es el LCA (común en ciclos), forzamos la dependencia.
                    if (u.etiqueta.contains("While") && (lca == u || lca == u.ipdom)) {
                        if (!u.dependenciasControl.contains(u)) {
                            u.dependenciasControl.add(u);
                        }
                    }
                }
            }
        }

        // Si un nodo no tiene dependencias externas, su jefe es el START
        for (NodoCFG n : nodos) {
            if (n == start || n == exit) continue;
            
            boolean tieneDependenciaExterna = false;
            for (NodoCFG dep : n.dependenciasControl) {
                if (dep != n) {
                    tieneDependenciaExterna = true;
                    break;
                }
            }
            
            if (!tieneDependenciaExterna) {
                if (!n.dependenciasControl.contains(start)) {
                    n.dependenciasControl.add(start);
                }
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
        // Ordenamos los nodos por su posición en el código fuente
        List<NodoCFG> nodosOrdenados = new ArrayList<>(nodos);
        Collections.sort(nodosOrdenados, (a, b) -> {
            if (a == start) return -1;
            if (b == start) return 1;
            if (a == exit) return 1;
            if (b == exit) return -1;
            if (a.linea != b.linea) return Integer.compare(a.linea, b.linea);
            return Integer.compare(a.columna, b.columna);
        });

        StringBuilder sb = new StringBuilder("digraph CDG {\n  node [shape=note, style=filled, color=orange];\n");
        
        // 1. Primero definimos los nodos en el orden correcto
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            sb.append("  n").append(n.id).append(" [label=\"").append(n.id).append(": ").append(n.etiqueta).append("\"];\n");
        }
        
        // 2. Luego definimos las aristas de dependencia
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            for (NodoCFG dep : n.dependenciasControl) {
                if (dep == exit) continue; 
                sb.append("  n").append(dep.id).append(" -> n").append(n.id).append(";\n");
            }
        }
        return sb.append("}\n").toString();
    }

    public String exportarFDG_DOT() {
        // Ordenamos los nodos por su posición en el código fuente
        List<NodoCFG> nodosOrdenados = new ArrayList<>(nodos);
        Collections.sort(nodosOrdenados, (a, b) -> {
            if (a == start) return -1;
            if (b == start) return 1;
            if (a == exit) return 1;
            if (b == exit) return -1;
            if (a.linea != b.linea) return Integer.compare(a.linea, b.linea);
            return Integer.compare(a.columna, b.columna);
        });

        StringBuilder sb = new StringBuilder();
        sb.append("digraph FDG {\n");
        sb.append("  node [shape=box, style=\"filled,rounded\", fillcolor=\"#E6F4EA\", color=\"#137333\", fontname=\"Segoe UI\", penwidth=1.5];\n");
        sb.append("  edge [color=\"#137333\", fontname=\"Segoe UI\", penwidth=1.5];\n");

        // 1. Definir los nodos en el orden correcto
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            sb.append("  n").append(n.id).append(" [label=\"").append(n.id).append(": ").append(n.etiqueta).append("\"];\n");
        }

        // 2. Definir las aristas de dependencia de datos (de definidores a usuarios)
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            for (NodoCFG dep : n.dependenciasDatos) {
                if (dep == exit) continue;
                sb.append("  n").append(dep.id).append(" -> n").append(n.id).append(";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    public String exportarPDG_DOT() {
        // Ordenamos los nodos por su posición en el código fuente
        List<NodoCFG> nodosOrdenados = new ArrayList<>(nodos);
        Collections.sort(nodosOrdenados, (a, b) -> {
            if (a == start) return -1;
            if (b == start) return 1;
            if (a == exit) return 1;
            if (b == exit) return -1;
            if (a.linea != b.linea) return Integer.compare(a.linea, b.linea);
            return Integer.compare(a.columna, b.columna);
        });

        StringBuilder sb = new StringBuilder();
        sb.append("digraph PDG {\n");
        sb.append("  node [shape=box, style=\"filled,rounded\", fillcolor=\"#F1F3F4\", color=\"#5F6368\", fontname=\"Segoe UI\", penwidth=1.5];\n");

        // 1. Definir los nodos en el orden correcto
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            sb.append("  n").append(n.id).append(" [label=\"").append(n.id).append(": ").append(n.etiqueta).append("\"];\n");
        }

        // 2. Definir las aristas de dependencia de control (azul)
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            for (NodoCFG dep : n.dependenciasControl) {
                if (dep == exit) continue;
                sb.append("  n").append(dep.id).append(" -> n").append(n.id)
                  .append(" [color=\"#1A73E8\", label=\"CD\", fontcolor=\"#1A73E8\", penwidth=1.5];\n");
            }
        }

        // 3. Definir las aristas de dependencia de datos (verde)
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            for (NodoCFG dep : n.dependenciasDatos) {
                if (dep == exit) continue;
                sb.append("  n").append(dep.id).append(" -> n").append(n.id)
                  .append(" [color=\"#137333\", label=\"DD\", fontcolor=\"#137333\", penwidth=1.5];\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    // Detecta nodos que no son alcanzables desde START
    public List<NodoCFG> obtenerCodigoMuerto() {
        Set<NodoCFG> alcanzables = new HashSet<>();
        Queue<NodoCFG> cola = new LinkedList<>();

        cola.add(start);
        alcanzables.add(start);

        while (!cola.isEmpty()) {
            NodoCFG actual = cola.poll();
            for (NodoCFG sucesor : actual.sucesores) {
                if (!alcanzables.contains(sucesor)) {
                    alcanzables.add(sucesor);
                    cola.add(sucesor);
                }
            }
        }

        List<NodoCFG> muertos = new ArrayList<>();
        for (NodoCFG n : nodos) {
            if (!alcanzables.contains(n)) {
                muertos.add(n);
            }
        }
        return muertos;
    }

    public String obtenerTablaDefUse() {
        // Ordenamos los nodos por su posición en el código fuente
        List<NodoCFG> nodosOrdenados = new ArrayList<>(nodos);
        Collections.sort(nodosOrdenados, (a, b) -> {
            if (a == start) return -1;
            if (b == start) return 1;
            if (a == exit) return 1;
            if (b == exit) return -1;
            if (a.linea != b.linea) return Integer.compare(a.linea, b.linea);
            return Integer.compare(a.columna, b.columna);
        });

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================================\n");
        sb.append("                                TABLA DE ASIGNACIÓN Y USO (DEF-USE PAIRS)\n");
        sb.append("========================================================================================================\n");
        sb.append(String.format("%-10s | %-45s | %s\n", "Nodo ID", "Sentencia / Etiqueta", "Pares Definición-Uso (Def-Use)"));
        sb.append("--------------------------------------------------------------------------------------------------------\n");

        for (NodoCFG n : nodosOrdenados) {
            String label = n.etiqueta;
            String nodeIdStr;
            if (n == start) {
                nodeIdStr = "START";
            } else if (n == exit) {
                nodeIdStr = "EXIT";
            } else {
                nodeIdStr = String.valueOf(n.id);
            }

            List<String> pairsStrList = new ArrayList<>();
            // Para cada definición que alcanza a n y es usada por n
            for (NodoCFG dep : n.dependenciasDatos) {
                String varName = dep.variableDefinida;
                if (varName != null && n.variablesUsadas.contains(varName)) {
                    String depIdStr = (dep == start) ? "START" : String.valueOf(dep.id);
                    String useIdStr = (n == start) ? "START" : ((n == exit) ? "EXIT" : String.valueOf(n.id));
                    pairsStrList.add(String.format("[%s:%s, %s:%s]", depIdStr, varName, useIdStr, varName));
                }
            }

            // Ordenamos la lista de pares para que sea consistente
            Collections.sort(pairsStrList);

            String pairsStr = pairsStrList.isEmpty() ? "(none)" : String.join(", ", pairsStrList);
            sb.append(String.format("%-10s | %-45s | %s\n", nodeIdStr, label.length() > 45 ? label.substring(0, 42) + "..." : label, pairsStr));
        }
        sb.append("========================================================================================================\n");
        return sb.toString();
    }

    // Algoritmo de Backward Slicing
    public Set<NodoCFG> calcularBackwardSlice(int criterioId) {
        NodoCFG criterio = null;
        for (NodoCFG n : nodos) {
            if (n.id == criterioId) {
                criterio = n;
                break;
            }
        }
        if (criterio == null) {
            System.err.println("Error: No se encontró el nodo con ID " + criterioId);
            return new HashSet<>();
        }

        Set<NodoCFG> slice = new HashSet<>();
        Queue<NodoCFG> cola = new LinkedList<>();

        cola.add(criterio);
        slice.add(criterio);

        while (!cola.isEmpty()) {
            NodoCFG actual = cola.poll();
            
            // 1. Visitar dependencias de control (nodos de los que 'actual' depende)
            for (NodoCFG dep : actual.dependenciasControl) {
                if (!slice.contains(dep)) {
                    slice.add(dep);
                    cola.add(dep);
                }
            }

            // 2. Visitar dependencias de datos (nodos de los que 'actual' depende)
            for (NodoCFG dep : actual.dependenciasDatos) {
                if (!slice.contains(dep)) {
                    slice.add(dep);
                    cola.add(dep);
                }
            }
        }
        return slice;
    }

    // Exportación del PDG resaltando el slice
    public String exportarPDGConSliceDOT(Set<NodoCFG> slice, NodoCFG criterio) {
        List<NodoCFG> nodosOrdenados = new ArrayList<>(nodos);
        Collections.sort(nodosOrdenados, (a, b) -> {
            if (a == start) return -1;
            if (b == start) return 1;
            if (a == exit) return 1;
            if (b == exit) return -1;
            if (a.linea != b.linea) return Integer.compare(a.linea, b.linea);
            return Integer.compare(a.columna, b.columna);
        });

        StringBuilder sb = new StringBuilder();
        sb.append("digraph PDG_Slice {\n");
        sb.append("  node [fontname=\"Segoe UI\", penwidth=1.5];\n");
        
        // 1. Definir los nodos
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            
            String shape = "box";
            String style = "filled,rounded";
            String fillcolor;
            String color;
            String fontcolor = "#202124";
            double penwidth = 1.5;
            
            if (n == criterio) {
                // Criterio del slice (Destacado en Coral/Rojo)
                fillcolor = "#FF8A65";
                color = "#D84315";
                style = "filled,bold,rounded";
                penwidth = 3.0;
            } else if (slice.contains(n)) {
                // Nodos en el slice (Destacado en Dorado/Amarillo)
                fillcolor = "#FFE082";
                color = "#F57C00";
                penwidth = 2.0;
            } else {
                // Nodos fuera del slice (Gris atenuado)
                fillcolor = "#F1F3F4";
                color = "#D2D6DC";
                fontcolor = "#9AA0A6";
            }
            
            sb.append("  n").append(n.id)
              .append(" [shape=").append(shape)
              .append(", style=\"").append(style).append("\"")
              .append(", fillcolor=\"").append(fillcolor).append("\"")
              .append(", color=\"").append(color).append("\"")
              .append(", fontcolor=\"").append(fontcolor).append("\"")
              .append(", penwidth=").append(penwidth)
              .append(", label=\"").append(n.id).append(": ").append(n.etiqueta).append("\"];\n");
        }

        // 2. Definir las aristas de dependencia de control
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            for (NodoCFG dep : n.dependenciasControl) {
                if (dep == exit) continue;
                
                boolean inSlice = slice.contains(n) && slice.contains(dep);
                String color = inSlice ? "#1A73E8" : "#DADCE0";
                String fontcolor = inSlice ? "#1A73E8" : "#BCC1C6";
                double penwidth = inSlice ? 3.0 : 0.8;
                
                sb.append("  n").append(dep.id).append(" -> n").append(n.id)
                  .append(" [color=\"").append(color).append("\"")
                  .append(", label=\"CD\"")
                  .append(", fontcolor=\"").append(fontcolor).append("\"")
                  .append(", penwidth=").append(penwidth).append("];\n");
            }
        }

        // 3. Definir las aristas de dependencia de datos
        for (NodoCFG n : nodosOrdenados) {
            if (n == exit) continue;
            for (NodoCFG dep : n.dependenciasDatos) {
                if (dep == exit) continue;
                
                boolean inSlice = slice.contains(n) && slice.contains(dep);
                String color = inSlice ? "#137333" : "#DADCE0";
                String fontcolor = inSlice ? "#137333" : "#BCC1C6";
                double penwidth = inSlice ? 3.0 : 0.8;
                
                sb.append("  n").append(dep.id).append(" -> n").append(n.id)
                  .append(" [color=\"").append(color).append("\"")
                  .append(", label=\"DD\"")
                  .append(", fontcolor=\"").append(fontcolor).append("\"")
                  .append(", penwidth=").append(penwidth).append("];\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    // Muestra el código fuente original con el resaltado del slice
    public void mostrarCodigoConSlice(Set<NodoCFG> slice, String archivoOriginal) {
        Set<String> variablesEnSlice = new HashSet<>();
        Set<Integer> lineasSlice = new HashSet<>();

        for (NodoCFG n : slice) {
            if (n.variableDefinida != null) {
                variablesEnSlice.add(n.variableDefinida);
            }
            if (n.variablesUsadas != null) {
                variablesEnSlice.addAll(n.variablesUsadas);
            }
            if (n.linea >= 0) {
                lineasSlice.add(n.linea + 1);
            }
        }

        // Buscar líneas de declaración de variables usadas en el slice recursivamente en el AST
        buscarLineasDeclaracion(this.astRaiz, variablesEnSlice, lineasSlice);

        System.out.println("\n=====================================================================");
        System.out.println("                 CÓDIGO FUENTE CON BACKWARD SLICE");
        System.out.println("=====================================================================");
        try (BufferedReader br = new BufferedReader(new FileReader(archivoOriginal))) {
            String linea;
            int nroLinea = 1;
            while ((linea = br.readLine()) != null) {
                if (lineasSlice.contains(nroLinea)) {
                    System.out.printf(" => %3d: %s\n", nroLinea, linea);
                } else {
                    System.out.printf("    %3d: %s\n", nroLinea, linea);
                }
                nroLinea++;
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo original para mostrar el slice: " + e.getMessage());
        }
        System.out.println("=====================================================================\n");
    }

    private void buscarLineasDeclaracion(Nodo nodo, Set<String> variablesEnSlice, Set<Integer> lineasSlice) {
        if (nodo == null) return;
        if ("Declaracion".equals(nodo.nombre)) {
            if (nodo.hijos.size() >= 2) {
                Nodo idNodo = nodo.hijos.get(1);
                if ("Identificador".equals(idNodo.nombre) && variablesEnSlice.contains(idNodo.valor)) {
                    lineasSlice.add(nodo.linea + 1);
                }
            }
        }
        for (Nodo hijo : nodo.hijos) {
            buscarLineasDeclaracion(hijo, variablesEnSlice, lineasSlice);
        }
    }
}