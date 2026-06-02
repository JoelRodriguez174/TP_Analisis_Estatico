/**
 * Recorre el AST y lo interpreta (ejecuta).
 * Usa una Tabla de Símbolos para guardar las variables declaradas,
 * sus valores y validar el uso correcto de las mismas.
 */
public class Evaluador {
    // Tabla de símbolos (variables declaradas con sus valores)
    private TablaSimbolos ts = new TablaSimbolos();

    // Evalúa recursivamente un nodo del AST.
    public int evaluar(Nodo ast) {
        try {
            return evaluarInterno(ast);
        } catch (ReturnException e) {
            return e.valor;
        }
    }

    private int evaluarInterno(Nodo ast) {
        if (ast == null) return 0; // nodo nulo

        switch (ast.nombre) {
            // Nodos estructurales, se evalúan sus hijos
            case "Programa":
            case "Bloque":
            case "Declaraciones":
            case "Sentencias":
                return evaluarHijos(ast);

            // Declaración de variables 
            case "Declaracion": {
                String tipo = ast.hijos.get(0).valor;      // hijo 0 = tipo ("int" o "bool")
                String var  = ast.hijos.get(1).valor;      // hijo 1 = nombre de variable
                ts.declarar(var, tipo, ast.linea);         // registrar variable en tabla
                return 0;
            }

            // Asignación de variables 
            case "Asignacion": {
                String nombreVar = ast.hijos.get(0).valor; // hijo 0 = nombre de la variable
                int valor = evaluarInterno(ast.hijos.get(1));     // hijo 1 = expresión a evaluar
                ts.asignar(nombreVar, valor, ast.linea);   // actualizar valor en tabla
                return valor;
            }

            // Sentencia return 
            case "Return": {
                int val = ast.hijos.isEmpty() ? 0 : evaluarInterno(ast.hijos.get(0));
                throw new ReturnException(val);
            }

            case "Numero":
                return Integer.parseInt(ast.valor);
            
            case "Bool":
                return ast.valor.equals("true") ? 1 : 0;

            // Identificadores 
            case "Identificador":
                return ts.obtener(ast.valor, ast.linea);

            // Estructuras de control
            case "If": {
                int condicion = evaluarInterno(ast.hijos.get(0));
                if (condicion != 0) {
                    return evaluarInterno(ast.hijos.get(1)); // rama true (Sentencias)
                } else {
                    return evaluarInterno(ast.hijos.get(2)); // rama false (Sentencias)
                }
            }

            case "While": {
                int ultimoValor = 0;
                while (evaluarInterno(ast.hijos.get(0)) != 0) {
                    ultimoValor = evaluarInterno(ast.hijos.get(1)); // cuerpo (Sentencias)
                }
                return ultimoValor;
            }

            // Expresiones aritméticas
            case "Suma":
                return evaluarInterno(ast.hijos.get(0)) + evaluarInterno(ast.hijos.get(1));

            case "Multiplicacion":
                return evaluarInterno(ast.hijos.get(0)) * evaluarInterno(ast.hijos.get(1));

            case "Menor":
                return evaluarInterno(ast.hijos.get(0)) < evaluarInterno(ast.hijos.get(1)) ? 1 : 0;

            // Cualquier otro nodo → evalúa sus hijos
            default:
                return evaluarHijos(ast);
        }
    }

    // Evalúa recursivamente todos los hijos de un nodo, devuelve el resultado del último hijo.
    private int evaluarHijos(Nodo n) {
        int result = 0;
        for (Nodo h : n.hijos) {
            result = evaluarInterno(h);
        }
        return result;
    }

    // Muestra en consola el estado actual de la tabla de símbolos.
    public void mostrarTabla() {
        System.out.println("Tabla de símbolos: " + ts);
    }
}
