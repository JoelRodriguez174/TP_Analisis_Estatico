public class ReturnException extends RuntimeException {
    public int valor;
    public ReturnException(int valor) {
        this.valor = valor;
    }
}
