import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//Tipos de token
enum TokenType {
    UNARY_OPERATION,
    BINARY_OPERATION,
    FUNCTION,
    LITERAL,
    PARENTHESIS,
    UNKNOWN
}


// Clase para guardar un token reconocido
class Token {
    private final TokenType type;
    private final String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return type + "(" + value + ")";
    }
}


// Clase para la logica de parceo y evaluar expreciones matematicas
class Calculator {
    public double parcer(String expression) {
        List<Token> tokens = tokenize(expression);
        System.out.println("Evaluando expresion: " + expression);
        /* Para debuguear tokenizacion y orden de evaluacion
        System.out.println("Tokens:");
        for (Token token : evalOrder) {
            System.out.println("   " + token);
        }*/
        List<Token> evalOrder = toEvaluationOrder(tokens);
        /* Para debuguear el orden de evaluacion de los tokens
        System.out.println("Tokens:");
        for (Token token : evalOrder) {
            System.out.println("   " + token);
        }*/
        return evaluate(evalOrder);
    }

    private List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        TokenType currentType = null;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    addToken(tokens, current, currentType);
                    current.setLength(0);
                    currentType = null;
                }
                continue;
            }

            if (Character.isLetter(c)) {
                if (currentType != TokenType.FUNCTION && current.length() > 0) {
                    addToken(tokens, current, currentType);
                    current.setLength(0);
                }
                current.append(c);
                currentType = TokenType.FUNCTION;
            } else if (Character.isDigit(c) || c == '.') {
                if (currentType != TokenType.LITERAL && current.length() > 0) {
                    addToken(tokens, current, currentType);
                    current.setLength(0);
                }
                current.append(c);
                currentType = TokenType.LITERAL;
            } else {
                if (current.length() > 0) {
                    addToken(tokens, current, currentType);
                    current.setLength(0);
                    currentType = null;
                }
                TokenType tokenType;
                if (c == '+' || c == '-') {
                    boolean unary = tokens.isEmpty() || tokens.get(tokens.size() - 1).getType() == TokenType.BINARY_OPERATION
                            || (tokens.get(tokens.size() - 1).getType() == TokenType.PARENTHESIS
                                    && tokens.get(tokens.size() - 1).getValue().equals("("));
                    tokenType = unary ? TokenType.UNARY_OPERATION : TokenType.BINARY_OPERATION;
                } else if (c == '*' || c == '/' || c == '^') {
                    tokenType = TokenType.BINARY_OPERATION;
                } else if (c == '(' || c == ')') {
                    tokenType = TokenType.PARENTHESIS;
                } else {
                    tokenType = TokenType.UNKNOWN;
                }
                tokens.add(new Token(tokenType, String.valueOf(c)));
            }
        }

        if (current.length() > 0) {
            addToken(tokens, current, currentType);
        }

        return tokens;
    }

    private void addToken(List<Token> tokens, StringBuilder current, TokenType currentType) {
        String value = current.toString();
        if (currentType == TokenType.FUNCTION) {
            String lower = value.toLowerCase();
            if ("pi".equals(lower)) {
                tokens.add(new Token(TokenType.LITERAL, String.valueOf(Math.PI)));
                return;
            }
            if ("e".equals(lower) || "euler".equals(lower)) {
                tokens.add(new Token(TokenType.LITERAL, String.valueOf(Math.E)));
                return;
            }
            tokens.add(new Token(TokenType.FUNCTION, lower));
        } else {
            tokens.add(new Token(currentType == null ? TokenType.UNKNOWN : currentType, value));
        }
    }

    private List<Token> toEvaluationOrder(List<Token> tokens) {
        List<Token> output = new ArrayList<>();
        List<Token> operators = new ArrayList<>();

        for (Token token : tokens) {
            switch (token.getType()) {
                case LITERAL:
                    output.add(token);
                    break;
                case FUNCTION:
                    operators.add(token);
                    break;
                case UNARY_OPERATION:
                case BINARY_OPERATION:
                    while (!operators.isEmpty() && isOperator(operators.get(operators.size() - 1))
                            && ((isLeftAssociative(token) && precedence(token) <= precedence(operators.get(operators.size() - 1)))
                                    || (!isLeftAssociative(token)
                                            && precedence(token) < precedence(operators.get(operators.size() - 1))))) {
                        output.add(operators.remove(operators.size() - 1));
                    }
                    operators.add(token);
                    break;
                case PARENTHESIS:
                    if ("(".equals(token.getValue())) {
                        operators.add(token);
                    } else {
                        while (!operators.isEmpty() && !"(".equals(operators.get(operators.size() - 1).getValue())) {
                            output.add(operators.remove(operators.size() - 1));
                        }
                        if (!operators.isEmpty() && "(".equals(operators.get(operators.size() - 1).getValue())) {
                            operators.remove(operators.size() - 1);
                        }
                        if (!operators.isEmpty() && operators.get(operators.size() - 1).getType() == TokenType.FUNCTION) {
                            output.add(operators.remove(operators.size() - 1));
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        while (!operators.isEmpty()) {
            output.add(operators.remove(operators.size() - 1));
        }

        return output;
    }

    private boolean isOperator(Token token) {
        return token.getType() == TokenType.BINARY_OPERATION || token.getType() == TokenType.UNARY_OPERATION;
    }

    private int precedence(Token token) {
        if (token.getType() == TokenType.UNARY_OPERATION) {
            return 4;
        }
        if (token.getType() == TokenType.BINARY_OPERATION) {
            switch (token.getValue()) {
                case "^":
                    return 3;
                case "*":
                case "/":
                    return 2;
                case "+":
                case "-":
                    return 1;
            }
        }
        return 0;
    }

    private boolean isLeftAssociative(Token token) {
        if (token.getType() == TokenType.BINARY_OPERATION) {
            return !"^".equals(token.getValue());
        }
        if (token.getType() == TokenType.UNARY_OPERATION) {
            return false;
        }
        return true;
    }

    private double evaluate(List<Token> tokens) {
        List<Double> stack = new ArrayList<>();

        for (Token token : tokens) {
            switch (token.getType()) {
                case LITERAL:
                    stack.add(Double.parseDouble(token.getValue()));
                    break;
                case UNARY_OPERATION:
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("Invalid expression");
                    }
                    double value = stack.remove(stack.size() - 1);
                    if ("-".equals(token.getValue())) {
                        stack.add(-value);
                    } else {
                        stack.add(value);
                    }
                    break;
                case BINARY_OPERATION:
                    if (stack.size() < 2) {
                        throw new IllegalArgumentException("Invalid expression");
                    }
                    double right = stack.remove(stack.size() - 1);
                    double left = stack.remove(stack.size() - 1);
                    switch (token.getValue()) {
                        case "+":
                            stack.add(left + right);
                            break;
                        case "-":
                            stack.add(left - right);
                            break;
                        case "*":
                            stack.add(left * right);
                            break;
                        case "/":
                            stack.add(left / right);
                            break;
                        case "^":
                            stack.add(Math.pow(left, right));
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown operator " + token.getValue());
                    }
                    break;
                case FUNCTION:
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("Invalid expression");
                    }
                    double arg = stack.remove(stack.size() - 1);
                    stack.add(applyFunction(token.getValue(), arg));
                    break;
                default:
                    break;
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression");
        }

        return stack.get(0);
    }

    private double applyFunction(String name, double arg) {
        switch (name) {
            case "sin":
                return Math.sin((arg));
            case "cos":
                return Math.cos((arg));
            case "tan":
                return Math.tan((arg));
            case "arcsin":
                return (Math.asin(arg));
            case "arccos":
                return (Math.acos(arg));
            case "arctan":
                return (Math.atan(arg));
            case "sinh":
                return Math.sinh((arg));
            case "cosh":
                return Math.cosh((arg));
            case "tanh":
                return Math.tanh((arg));
            default:
                throw new IllegalArgumentException("Unknown function " + name);
        }
    }
}

//clase main para manejo de consola
public class main {
    public static void main(String[] args) {
        Calculator calc = new Calculator();
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;
        while (!exit) {
            System.out.print("Ingrese una expresion matematica: (angulo en radianes, 'exit' para salir) ");
            String expression = scanner.nextLine();
            if ("exit".equals(expression)) {
                exit = true;
                continue;
            }
            double result = calc.parcer(expression);
            System.out.println("Resultado: " + result);
        }
    }
}

