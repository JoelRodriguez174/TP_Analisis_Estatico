@echo off
echo [1/6] Limpiando archivos de clases y generados...
del /q *.class
del /q Parser.java
del /q sym.java
del /q AnalizadorLexico.java
del /q AnalizadorLexicoCUP.java

echo [2/6] Generando el Parser y la interfaz de simbolos con CUP...
java -jar lib/java-cup-11b.jar -interface -parser Parser gramatica.cup

echo [3/6] Generando los Analizadores Lexicos con JFlex...
java -jar lib/jflex-full-1.9.1.jar AnalizadorLexico.jflex
java -jar lib/jflex-full-1.9.1.jar AnalizadorLexicoCUP.jflex

echo [4/6] Compilando todos los archivos Java...
javac -cp ".;lib/java-cup-11b-runtime.jar" *.java

echo [5/6] Ejecutando el programa principal con test.txt...
echo.
java -cp ".;lib/java-cup-11b-runtime.jar" Main test.txt

echo.
echo [6/6] Convirtiendo archivos DOT a imagenes PNG...
dot -Tpng ast.dot -o ast.png
dot -Tpng cfg.dot -o cfg.png
dot -Tpng pdt.dot -o pdt.png
dot -Tpng cdg.dot -o cdg.png
echo.
echo ¡Imagenes PNG generadas con exito!

echo.
echo Proceso completado. Presiona cualquier tecla para cerrar.
pause