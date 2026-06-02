import java.util.*;

public class GeneradorAssembly {
    private int tempCount = 0;
    private int labelCount = 0;
    private StringBuilder codigo = new StringBuilder();

    // Genera un nuevo temporal T1, T2, ...
    private String nuevoTemporal() {
        tempCount++;
        return "T" + tempCount;
    }

    private String nuevaEtiqueta() {
        labelCount++;
        return "L" + labelCount;
    }

    // Método público para generar assembly a partir de la raíz del AST
    public String generar(Nodo raiz) {
        codigo.setLength(0); // limpiar
        recorrer(raiz);
        return codigo.toString();
    }

    // Recorrido recursivo del AST
    private String recorrer(Nodo nodo) {
        if (nodo == null) return "";

        String tipo = nodo.nombre;

        switch (tipo) {
            case "Programa":
            case "Main":
            case "Bloque":
            case "Declaraciones":
            case "Sentencias":
                // Procesar hijos, no generan código directamente
                for (Nodo hijo : nodo.hijos) {
                    recorrer(hijo);
                }
                return "";

            case "Declaracion":
                // Podrías reservar espacio si quisieras (ej: DECL var), pero no es obligatorio
                return "";

            case "Asignacion": {
                String var = recorrer(nodo.hijos.get(0));   // Identificador
                String valor = recorrer(nodo.hijos.get(1)); // Expresión/Numero
                codigo.append("MOV ").append(var).append(", ").append(valor).append("\n");
                return var;
            }

            case "Return": {
                String valor = recorrer(nodo.hijos.get(0));
                String t = nuevoTemporal();
                codigo.append("MOV ").append(t).append(", ").append(valor).append("\n");
                codigo.append("RET ").append(t).append("\n");
                return valor;
            }

            case "Identificador":
                return nodo.valor; // ej. "x"

            case "Numero":
                return nodo.valor; // ej. "5"

            case "Bool":
                return nodo.valor.equals("true") ? "1" : "0";

            case "If": {
                String cond = recorrer(nodo.hijos.get(0));
                String labelElse = nuevaEtiqueta();
                String labelEnd = nuevaEtiqueta();

                codigo.append("CMP ").append(cond).append(", 0\n");
                codigo.append("JE ").append(labelElse).append("\n");

                recorrer(nodo.hijos.get(1)); // rama true
                codigo.append("JMP ").append(labelEnd).append("\n");

                codigo.append(labelElse).append(":\n");
                recorrer(nodo.hijos.get(2)); // rama false

                codigo.append(labelEnd).append(":\n");
                return "";
            }

            case "While": {
                String labelStart = nuevaEtiqueta();
                String labelEnd = nuevaEtiqueta();

                codigo.append(labelStart).append(":\n");
                String cond = recorrer(nodo.hijos.get(0));

                codigo.append("CMP ").append(cond).append(", 0\n");
                codigo.append("JE ").append(labelEnd).append("\n");

                recorrer(nodo.hijos.get(1)); // cuerpo
                codigo.append("JMP ").append(labelStart).append("\n");

                codigo.append(labelEnd).append(":\n");
                return "";
            }

            case "Suma": {
                String izq = recorrer(nodo.hijos.get(0));
                String der = recorrer(nodo.hijos.get(1));
                String t = nuevoTemporal();
                codigo.append("ADD ").append(t).append(", ").append(izq).append(", ").append(der).append("\n");
                return t;
            }

            case "Multiplicacion": {
                String izq = recorrer(nodo.hijos.get(0));
                String der = recorrer(nodo.hijos.get(1));
                String t = nuevoTemporal();
                codigo.append("MUL ").append(t).append(", ").append(izq).append(", ").append(der).append("\n");
                return t;
            }

            case "Menor": {
                String izq = recorrer(nodo.hijos.get(0));
                String der = recorrer(nodo.hijos.get(1));
                String t = nuevoTemporal();
                codigo.append("CMP ").append(izq).append(", ").append(der).append("\n");
                codigo.append("SETL ").append(t).append("\n");
                return t;
            }

            default:
                // Si es algo desconocido, solo recorremos hijos
                for (Nodo hijo : nodo.hijos) {
                    recorrer(hijo);
                }
                return "";
        }
    }
}
