import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java_cup.runtime.Symbol;

public class Main {
    public static void main(String[] argv) {
        // Verifica que le pases el archivo txt por consola
        if (argv.length < 1) {
            System.err.println("Uso: java Main <archivo_fuente>");
            return;
        }

        try {
            // 1) Crear el parser usando el analizador léxico
            Parser p = new Parser(new AnalizadorLexicoCUP(new FileReader(argv[0])));

            // 2) Ejecutar el parser y recuperar el AST 
            // AQUÍ ES DONDE SE CREA LA VARIABLE 'ast' QUE TE DABA ERROR
            Symbol s = p.parse();
            Nodo ast = (Nodo) s.value;

            // 3) Mostrar el AST en consola
            System.out.println("=== AST generado ===");
            System.out.println(ast);

            // 4) Generar archivo DOT para Graphviz del AST
            try (FileWriter fw = new FileWriter("ast.dot")) {
                fw.write(ast.toDot());
                System.out.println("\nArchivo 'ast.dot' generado correctamente.");
                System.out.println("Para convertirlo en imagen ejecuta:");
                System.out.println("dot -Tpng ast.dot -o ast.png");
            } catch (IOException e) {
                System.err.println("Error al escribir el archivo DOT: " + e.getMessage());
            }

            // ====== NUEVO CÓDIGO: PUNTOS 1 AL 4 DEL TP ======
            System.out.println("\n=== ANÁLISIS DE FLUJO Y DEPENDENCIAS ===");
            // Ahora le pasamos la variable 'ast' que creamos más arriba
            CFG cfg = new CFG(ast);             // Punto 1: Grafo de Flujo
            cfg.calcularPostdominadores();      // Punto 2: Postdominadores
            cfg.calcularArbolPostdominadores(); // Punto 3: Árbol de Postdominadores (PDT)
            cfg.calcularCDG();                  // Punto 4: Grafo de Dependencia (CDG)

            // Exportar CFG (Grafo de Flujo de Control)
            try (FileWriter fw = new FileWriter("cfg.dot")) {
                fw.write(cfg.exportarDOT());
                System.out.println("Archivo 'cfg.dot' generado correctamente.");
            }

            // Exportar PDT (Árbol de Postdominadores)
            try (FileWriter fw = new FileWriter("pdt.dot")) {
                fw.write(cfg.exportarArbolPostdominadoresDOT());
                System.out.println("Archivo 'pdt.dot' generado correctamente.");
            }

            // Exportar CDG (Grafo de Dependencia de Control)
            try (FileWriter fw = new FileWriter("cdg.dot")) {
                fw.write(cfg.exportarCDG_DOT());
                System.out.println("Archivo 'cdg.dot' generado correctamente.");
            }
            // =================================================

            // 5) Interpretar el programa a partir del AST
            System.out.println("\n=== EJECUCIÓN DEL PROGRAMA ===");
            Evaluador eval = new Evaluador();
            int resultado = eval.evaluar(ast);
            eval.mostrarTabla();
            System.out.println("Valor de retorno: " + resultado);

            System.out.println("\n=== COMPROBACIÓN DE TIPOS ===");
            try {
                ChequeoTipos checker = new ChequeoTipos();
                checker.chequear(ast);
                System.out.println("Chequeo de tipos correcto ✅");
            } catch (RuntimeException e) {
                System.err.println("Error de tipos: " + e.getMessage());
                return; // corta ejecución
            }

            GeneradorAssembly generador = new GeneradorAssembly();
            String asm = generador.generar(ast);
            System.out.println("=== Pseudo-Assembly ===");
            System.out.println(asm);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}