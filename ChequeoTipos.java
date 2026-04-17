public class ChequeoTipos {
    private TablaSimbolos ts = new TablaSimbolos();

    public String chequear(Nodo nodo) {
        if (nodo == null) return "void";

        switch (nodo.nombre) {
            case "Programa":
            case "Bloque":
            case "Declaraciones":
            case "Sentencias":
                for (Nodo h : nodo.hijos) chequear(h);
                return "void";

            case "Declaracion": {
                String tipo = nodo.hijos.get(0).valor;     // hijo[0] = Tipo ("int"/"bool")
                String nombreVar = nodo.hijos.get(1).valor; // hijo[1] = Identificador
                ts.declarar(nombreVar, tipo, nodo.linea);
                return tipo;
            }

            case "Asignacion": {
                String nombreVar = nodo.hijos.get(0).valor;
                String tipoVar = ts.obtenerTipo(nombreVar, nodo.linea);
                String tipoExpr = chequear(nodo.hijos.get(1));
                if (!tipoVar.equals(tipoExpr)) {
                    throw new RuntimeException("Error de tipos en línea " + (nodo.linea+1) +
                        ": no se puede asignar " + tipoExpr + " a " + tipoVar);
                }
                return tipoVar;
            }

            case "Return":
                return nodo.hijos.isEmpty() ? "void" : chequear(nodo.hijos.get(0));

            case "Identificador":
                return ts.obtenerTipo(nodo.valor, nodo.linea);

            case "Numero":
                return "int";

            case "Bool":
                return "bool";

            case "Suma": {
                String t1 = chequear(nodo.hijos.get(0));
                String t2 = chequear(nodo.hijos.get(1));
                if (!(t1.equals("int") && t2.equals("int"))) {
                    throw new RuntimeException("Error de tipos en suma (línea " + (nodo.linea+1) + ")");
                }
                return "int";
            }

            case "Multiplicacion": {
                String t1 = chequear(nodo.hijos.get(0));
                String t2 = chequear(nodo.hijos.get(1));
                if (!(t1.equals("int") && t2.equals("int"))) {
                    throw new RuntimeException("Error de tipos en multiplicación (línea " + (nodo.linea+1) + ")");
                }
                return "int";
            }

            default:
                for (Nodo h : nodo.hijos) chequear(h);
                return "void";
        }
    }
}
