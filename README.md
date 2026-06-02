#**Proyecto – Compiladores y Análisis Estático**

## **Descripción General**
El sistema procesa archivos de código fuente, genera una representación intermedia (AST), realiza chequeos de tipos, permite la interpretación del programa y genera representaciones gráficas de las estructuras de control y dependencia. Además, produce un archivo de pseudo-assembly.

## **Métodos Utilizados**

1.  **Análisis Léxico:** Se utiliza **JFlex** para definir los tokens del lenguaje (identificadores, palabras reservadas, operadores, etc.).
2.  **Análisis Sintáctico:** Se emplea **CUP** para definir la gramática y generar un analizador LALR que construye el **Árbol de Sintaxis Abstracta (AST)**.
3.  **Análisis Semántico (Chequeo de Tipos):** Se realiza un recorrido sobre el AST para validar que las operaciones sean consistentes con los tipos de datos (int, bool, void) y que las variables estén declaradas.
4.  **Representación Intermedia:** El AST sirve como base para todas las etapas posteriores, implementado mediante una estructura de nodos jerárquica.
5.  **Análisis de Flujo de Control (CFG):** Se construye un Grafo de Flujo de Control que modela todas las posibles rutas de ejecución del programa.
6.  **Análisis de Postdominadores:**
    *   Cálculo de postdominadores para cada nodo del CFG.
    *   Generación del **Árbol de Postdominadores (PDT)**.
7.  **Análisis de Dependencia de Control (CDG):** Basándose en el CFG y el PDT, se identifican las condiciones que controlan la ejecución de cada sentencia.
8.  **Generación de Código:** Se traduce el AST a una representación de **Pseudo-Assembly** de bajo nivel.
9.  **Interpretación:** Un componente **Evaluador** permite ejecutar el programa directamente desde el AST, gestionando una tabla de símbolos para el estado de las variables.
10. **Visualización:** Integración con **Graphviz** para generar imágenes (.png) de:
    *   AST (Árbol de Sintaxis Abstracta)
    *   CFG (Grafo de Flujo de Control)
    *   PDT (Árbol de Postdominadores)
    *   CDG (Grafo de Dependencia de Control)

## **Arquitectura y Clases Principales**

El proyecto está organizado en varias clases Java que se encargan de las distintas etapas del proceso:

### **1. Clase `Main`**
Es el punto de entrada del programa. Coordina la ejecución secuencial de todas las fases:
*   Inicializa el **Lexer** y el **Parser**.
*   Genera el **AST**.
*   Invoca los motores de análisis estático (**CFG**, **PDT**, **CDG**).
*   Ejecuta el **Chequeo de Tipos** y el **Evaluador**.
*   Produce la salida en **Pseudo-Assembly**.

### **2. Clase `Nodo` (Estructura del AST)**
Define la unidad básica del Árbol de Sintaxis Abstracta.
*   `agregarHijo(Nodo hijo)`: Construye la jerarquía del árbol.
*   `toString()`: Genera una representación textual indentada del árbol.
*   `toDot()`: Genera el código compatible con Graphviz para visualizar el AST.

### **3. Clase `CFG` (Análisis de Flujo)**
Construye y analiza el Grafo de Flujo de Control.
*   `procesarSentencias(Nodo, NodoCFG)`: Transforma las sentencias del AST en nodos del grafo.
*   `calcularPostdominadores()`: Implementa el algoritmo iterativo para hallar los postdominadores de cada nodo.
*   `calcularArbolPostdominadores()`: Identifica postdominadores inmediatos para construir el **PDT**.
*   `calcularCDG()`: Calcula las dependencias de control basándose en las aristas del CFG y el PDT.
*   `exportarDOT()` / `exportarCDG_DOT()`: Generan visualizaciones de los grafos resultantes.

### **4. Clase `ChequeoTipos` (Análisis Semántico)**
Asegura la validez lógica del programa.
*   `chequear(Nodo nodo)`: Recorre el AST validando que las variables estén declaradas y que los tipos en asignaciones y operaciones aritméticas sean compatibles (ej. no sumar un `bool` con un `int`).

### **5. Clase `Evaluador` (Interpretación)**
Un intérprete que ejecuta el código directamente desde el AST.
*   `evaluar(Nodo ast)`: Ejecuta recursivamente las operaciones, actualizando el estado del programa.
*   `mostrarTabla()`: Imprime los valores finales de todas las variables tras la ejecución.

### **6. Clase `GeneradorAssembly` (Generación de Código)**
Traduce la lógica de alto nivel a bajo nivel.
*   `generar(Nodo raiz)`: Produce una cadena de texto con instrucciones de pseudo-assembly (`MOV`, `ADD`, `MUL`, `RET`).
*   `nuevoTemporal()`: Gestiona la creación de registros temporales (`T1`, `T2`, etc.) para operaciones complejas.

### **7. Clase `TablaSimbolos` (Gestión de Memoria)**
Maneja el almacenamiento de variables durante la compilación y ejecución.
*   `declarar()`, `asignar()`, `obtener()`: Métodos para gestionar el ciclo de vida de las variables y detectar errores como variables no declaradas o duplicadas.

**Integrantes**

- Rodríguez, Joel

# **Ejecución del programa:**
*   Simplemente ejecutar el archivo `.bat` incluido en el proyecto(Elimina archivos "basura" y vuelve a generar todo devuelta).

---
Ingeniería en Sistemas de Información – UnViMe – 2025
