import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
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

            System.out.println("\n=== COMPROBACIÓN DE TIPOS ===");
            try {
                ChequeoTipos checker = new ChequeoTipos();
                checker.chequear(ast);
                System.out.println("Chequeo de tipos correcto ✅");
            } catch (RuntimeException e) {
                System.err.println("Error de tipos: " + e.getMessage());
                return; // corta ejecución si falla el chequeo de tipos
            }

            // ====== NUEVO CÓDIGO: PUNTOS 1 AL 4 DEL TP ======
            System.out.println("\n=== ANÁLISIS DE FLUJO Y DEPENDENCIAS ===");
            CFG cfg = new CFG(ast);             // Punto 1: Grafo de Flujo
            cfg.calcularPostdominadores();      // Punto 2: Postdominadores
            cfg.calcularArbolPostdominadores(); // Punto 3: Árbol de Postdominadores (PDT)
            cfg.calcularCDG();                  // Punto 4: Grafo de Dependencia (CDG)
            cfg.calcularFDG();                  // Punto 5: Grafo de Dependencia de Datos (FDG)

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

            // Exportar FDG (Grafo de Dependencia de Datos / Flujo)
            try (FileWriter fw = new FileWriter("fdg.dot")) {
                fw.write(cfg.exportarFDG_DOT());
                System.out.println("Archivo 'fdg.dot' generado correctamente.");
            }

            // Exportar PDG (Grafo de Dependencia del Programa)
            try (FileWriter fw = new FileWriter("pdg.dot")) {
                fw.write(cfg.exportarPDG_DOT());
                System.out.println("Archivo 'pdg.dot' generado correctamente.");
            }

            // ====== ANÁLISIS DE SLICING ======
            int criterioId = -1;
            if (argv.length >= 2) {
                try {
                    criterioId = Integer.parseInt(argv[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Advertencia: El criterio de slicing debe ser un número entero (ID del nodo).");
                }
            }
            
            if (criterioId == -1) {
                // Buscar un nodo por defecto (preferentemente Return o el último antes de EXIT)
                for (NodoCFG n : cfg.nodos) {
                    if (n.etiqueta.startsWith("Return")) {
                        criterioId = n.id;
                        break;
                    }
                }
                // Si no hay return, buscar la última asignación o el nodo con id más alto que no sea EXIT/START
                if (criterioId == -1) {
                    int maxId = -1;
                    for (NodoCFG n : cfg.nodos) {
                        if (n != cfg.start && n != cfg.exit && n.id > maxId) {
                            maxId = n.id;
                        }
                    }
                    criterioId = maxId;
                }
            }

            if (criterioId != -1) {
                System.out.println("\n=== REALIZANDO PROGRAM SLICING (BACKWARD) ===");
                NodoCFG criterioNodo = null;
                for (NodoCFG n : cfg.nodos) {
                    if (n.id == criterioId) {
                        criterioNodo = n;
                        break;
                    }
                }
                if (criterioNodo != null) {
                    System.out.println("Criterio seleccionado: Nodo [" + criterioNodo.id + "] \"" + criterioNodo.etiqueta + "\"");
                    java.util.Set<NodoCFG> slice = cfg.calcularBackwardSlice(criterioId);
                    
                    System.out.print("Nodos en el slice: ");
                    java.util.List<Integer> idsSlice = new java.util.ArrayList<>();
                    for (NodoCFG n : slice) {
                        idsSlice.add(n.id);
                    }
                    java.util.Collections.sort(idsSlice);
                    System.out.println(idsSlice);

                    // Escribir el archivo DOT para el slice
                    try (FileWriter fw = new FileWriter("pdg_slice.dot")) {
                        fw.write(cfg.exportarPDGConSliceDOT(slice, criterioNodo));
                        System.out.println("Archivo 'pdg_slice.dot' generado correctamente.");
                    } catch (IOException e) {
                        System.err.println("Error al escribir el archivo pdg_slice.dot: " + e.getMessage());
                    }

                    // Generar imagen con Graphviz si está disponible
                    try {
                        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", "pdg_slice.dot", "-o", "pdg_slice.png");
                        Process p_dot = pb.start();
                        p_dot.waitFor();
                        System.out.println("Imagen 'pdg_slice.png' generada con éxito.");
                    } catch (Exception e) {
                        System.out.println("Advertencia: No se pudo generar la imagen pdg_slice.png automáticamente (¿está Graphviz en el PATH?).");
                    }

                    // Mostrar el código original resaltando las líneas del slice
                    cfg.mostrarCodigoConSlice(slice, argv[0]);
                } else {
                    System.err.println("Error: No se encontró el nodo con ID " + criterioId + " para realizar el slice.");
                }
            }

            // --- TABLA DE PARES DEFINICIÓN-USO (DEF-USE) ---
            String tablaDefUse = cfg.obtenerTablaDefUse();
            System.out.println("\n" + tablaDefUse);
            try (FileWriter fw = new FileWriter("def_use.txt")) {
                fw.write(tablaDefUse);
                System.out.println("Archivo 'def_use.txt' generado correctamente con la tabla Def-Use.");
            } catch (IOException e) {
                System.err.println("Error al escribir el archivo def_use.txt: " + e.getMessage());
            }

            // --- DETECCIÓN DE CÓDIGO MUERTO ---
            List<NodoCFG> muertos = cfg.obtenerCodigoMuerto();
            if (!muertos.isEmpty()) {
                System.out.println("\n⚠️ ADVERTENCIA: Se ha detectado código muerto (nodos inalcanzables):");
                for (NodoCFG m : muertos) {
                    System.out.println("  - [" + m.id + "] " + m.etiqueta);
                }
            } else {
                System.out.println("\nNo se ha detectado código muerto ✅");
            }
            // =================================================

            // 5) Interpretar el programa a partir del AST
            System.out.println("\n=== EJECUCIÓN DEL PROGRAMA ===");
            Evaluador eval = new Evaluador();
            int resultado = eval.evaluar(ast);
            eval.mostrarTabla();
            System.out.println("Valor de retorno: " + resultado);

            GeneradorAssembly generador = new GeneradorAssembly();
            String asm = generador.generar(ast);
            System.out.println("\n=== Pseudo-Assembly ===");
            System.out.println(asm);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}