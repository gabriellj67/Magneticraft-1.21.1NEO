package committee.nova.mods.magneticraft.content.computer.runtime;

import committee.nova.mods.magneticraft.content.computer.runtime.ComputerDeviceBus.DeviceCommand;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bounded Lisp 2.0 evaluator for the released documented language surface. */
final class LispEngine implements ScriptEngine {
    static final int MAX_NODES = 512;
    static final int MAX_DEPTH = 32;
    static final int MAX_GLOBALS = 64;
    static final int MAX_LIST_SIZE = 128;

    private static final String PC_TAG = "program_counter";
    private static final String RUNNING_TAG = "running";
    private static final String FAULT_TAG = "fault";
    private static final String LAST_RESULT_TAG = "last_result";
    private static final String OUTPUT_TAG = "output";
    private static final String GLOBALS_TAG = "globals";
    private static final String NAME_TAG = "name";
    private static final String VALUE_TAG = "value";
    private static final String TYPE_TAG = "type";
    private static final String NUMBER_TAG = "number";
    private static final String TEXT_TAG = "text";
    private static final String VALUES_TAG = "values";
    private static final String HELP = "quote progn if eval define set! defun first second third fourth fifth atom rest "
            + "cons length append reverse null list numberp symbolp stringp free env help print println clear "
            + "mine move scan quarry front back left right down inventory energy redstone";

    private final List<Node> forms;
    private final Map<String, UserFunction> functions = new LinkedHashMap<>();
    private final Map<String, Value> globals = new LinkedHashMap<>();
    private final ScriptOutput output = new ScriptOutput();
    private int programCounter;
    private boolean running;
    private VmFault fault = VmFault.NONE;
    private int lastResult;

    LispEngine(String source) {
        forms = new Parser(source).parseAll();
        collectFunctions(forms);
        validateDevicePlacement(forms);
        running = !forms.isEmpty();
    }

    @Override
    public int executeTick(ComputerDeviceBus bus, int instructionBudget) {
        if (!running || fault != VmFault.NONE) {
            return 0;
        }
        if (programCounter < 0 || programCounter >= forms.size()) {
            running = false;
            return 0;
        }
        Budget budget = new Budget(Math.max(1, Math.min(ScriptRuntime.MAX_INSTRUCTIONS_PER_TICK, instructionBudget)));
        try {
            Value result = evaluate(forms.get(programCounter), Map.of(), bus, budget, 0);
            lastResult = result.numericResult();
            programCounter++;
            if (programCounter >= forms.size()) {
                running = false;
            }
        } catch (DeviceWait ignored) {
            // The value-only request is retried next tick without advancing the form.
        } catch (EvaluationFailure failure) {
            latch(failure.fault);
        }
        return Math.max(1, budget.used);
    }

    private Value evaluate(
            Node node,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth
    ) {
        budget.step();
        if (depth > MAX_DEPTH) {
            throw new EvaluationFailure(VmFault.RETURN_STACK_OVERFLOW);
        }
        if (node instanceof NumberNode number) {
            return Value.number(number.value());
        }
        if (node instanceof StringNode string) {
            return Value.string(string.value());
        }
        if (node instanceof QuoteNode quote) {
            return quoted(quote.value(), 0);
        }
        if (node instanceof SymbolNode symbol) {
            String name = normalized(symbol.name());
            Value local = locals.get(name);
            if (local != null) {
                return local;
            }
            Value global = globals.get(name);
            if (global != null) {
                return global;
            }
            return switch (name) {
                case "nil", "false" -> Value.nil();
                case "true" -> Value.bool(true);
                case "front" -> Value.number(0);
                case "back" -> Value.number(1);
                case "left" -> Value.number(2);
                case "right" -> Value.number(3);
                case "down" -> Value.number(5);
                default -> throw new EvaluationFailure(VmFault.INVALID_SOURCE);
            };
        }
        List<Node> values = ((ListNode) node).values();
        if (values.isEmpty()) {
            return Value.nil();
        }
        if (!(values.get(0) instanceof SymbolNode head)) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
        String name = normalized(head.name());
        List<Node> arguments = values.subList(1, values.size());
        return switch (name) {
            case "quote" -> {
                requireArgumentCount(name, arguments, 1);
                yield quoted(arguments.get(0), 0);
            }
            case "progn" -> evaluateSequence(arguments, locals, bus, budget, depth + 1);
            case "if" -> evaluateIf(arguments, locals, bus, budget, depth + 1);
            case "define" -> define(arguments, locals, bus, budget, depth + 1, false);
            case "set!" -> define(arguments, locals, bus, budget, depth + 1, true);
            case "defun" -> Value.symbol(functionName(arguments));
            case "+" -> arithmetic(arguments, locals, bus, budget, depth + 1, Arithmetic.ADD);
            case "-" -> arithmetic(arguments, locals, bus, budget, depth + 1, Arithmetic.SUBTRACT);
            case "*" -> arithmetic(arguments, locals, bus, budget, depth + 1, Arithmetic.MULTIPLY);
            case "/" -> arithmetic(arguments, locals, bus, budget, depth + 1, Arithmetic.DIVIDE);
            case "mod" -> arithmetic(arguments, locals, bus, budget, depth + 1, Arithmetic.MODULO);
            case "list" -> list(evaluateArguments(arguments, locals, bus, budget, depth + 1));
            case "list*" -> listStar(evaluateArguments(arguments, locals, bus, budget, depth + 1));
            case "cons" -> cons(evaluateArguments(arguments, locals, bus, budget, depth + 1));
            case "first" -> positional(arguments, locals, bus, budget, depth + 1, 0);
            case "second" -> positional(arguments, locals, bus, budget, depth + 1, 1);
            case "third" -> positional(arguments, locals, bus, budget, depth + 1, 2);
            case "fourth" -> positional(arguments, locals, bus, budget, depth + 1, 3);
            case "fifth" -> positional(arguments, locals, bus, budget, depth + 1, 4);
            case "rest" -> rest(arguments, locals, bus, budget, depth + 1);
            case "length" -> Value.number(single(arguments, locals, bus, budget, depth + 1).length());
            case "append" -> append(evaluateArguments(arguments, locals, bus, budget, depth + 1));
            case "reverse" -> reverse(single(arguments, locals, bus, budget, depth + 1));
            case "null" -> Value.bool(single(arguments, locals, bus, budget, depth + 1).isNil());
            case "atom" -> Value.bool(single(arguments, locals, bus, budget, depth + 1).kind() != Kind.LIST);
            case "numberp" -> Value.bool(single(arguments, locals, bus, budget, depth + 1).kind() == Kind.NUMBER);
            case "symbolp" -> Value.bool(single(arguments, locals, bus, budget, depth + 1).kind() == Kind.SYMBOL);
            case "stringp" -> Value.bool(single(arguments, locals, bus, budget, depth + 1).kind() == Kind.STRING);
            case "print", "println" -> print(name, arguments, locals, bus, budget, depth + 1);
            case "clear" -> {
                requireArgumentCount(name, arguments, 0);
                output.clear();
                yield Value.nil();
            }
            case "free" -> {
                requireArgumentCount(name, arguments, 0);
                yield Value.number(MAX_GLOBALS - globals.size());
            }
            case "env", "help" -> {
                requireArgumentCount(name, arguments, 0);
                output.appendLine(HELP);
                yield Value.symbol("ok");
            }
            case "eval" -> {
                Value value = single(arguments, locals, bus, budget, depth + 1);
                yield evaluate(value.toNode(), locals, bus, budget, depth + 1);
            }
            case "mine" -> device(name, DeviceCommand.MINE_FRONT, arguments, locals, bus, budget, depth + 1, 0);
            case "front" -> device(name, DeviceCommand.MOVE_FRONT, arguments, locals, bus, budget, depth + 1, 0);
            case "back" -> device(name, DeviceCommand.MOVE_BACK, arguments, locals, bus, budget, depth + 1, 0);
            case "left" -> device(name, DeviceCommand.ROTATE_LEFT, arguments, locals, bus, budget, depth + 1, 0);
            case "right" -> device(name, DeviceCommand.ROTATE_RIGHT, arguments, locals, bus, budget, depth + 1, 0);
            case "up" -> device(name, DeviceCommand.ROTATE_UP, arguments, locals, bus, budget, depth + 1, 0);
            case "down" -> device(name, DeviceCommand.ROTATE_DOWN, arguments, locals, bus, budget, depth + 1, 0);
            case "move" -> device(name, DeviceCommand.MOVE, arguments, locals, bus, budget, depth + 1, 0);
            case "scan" -> device(name, DeviceCommand.SCAN_FRONT, arguments, locals, bus, budget, depth + 1, 0);
            case "quarry" -> device(name, DeviceCommand.QUARRY, arguments, locals, bus, budget, depth + 1, 1);
            case "redstone" -> device(name, DeviceCommand.SET_REDSTONE, arguments, locals, bus, budget, depth + 1, 1);
            case "inventory" -> device(name, DeviceCommand.INVENTORY_COUNT, arguments, locals, bus, budget, depth + 1, 0);
            case "energy" -> device(name, DeviceCommand.ENERGY_STORED, arguments, locals, bus, budget, depth + 1, 0);
            default -> callFunction(name, arguments, locals, bus, budget, depth + 1);
        };
    }

    private Value evaluateSequence(
            List<Node> nodes,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth
    ) {
        Value result = Value.nil();
        for (Node node : nodes) {
            result = evaluate(node, locals, bus, budget, depth);
        }
        return result;
    }

    private Value evaluateIf(
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth
    ) {
        if (arguments.size() < 2 || arguments.size() > 3) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
        Value condition = evaluate(arguments.get(0), locals, bus, budget, depth);
        if (condition.truthy()) {
            return evaluate(arguments.get(1), locals, bus, budget, depth);
        }
        return arguments.size() == 3
                ? evaluate(arguments.get(2), locals, bus, budget, depth)
                : Value.nil();
    }

    private Value define(
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth,
            boolean requireExisting
    ) {
        requireArgumentCount(requireExisting ? "set!" : "define", arguments, 2);
        if (!(arguments.get(0) instanceof SymbolNode symbol)) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
        String name = normalized(symbol.name());
        if (requireExisting && !globals.containsKey(name)) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
        if (!globals.containsKey(name) && globals.size() >= MAX_GLOBALS) {
            throw new EvaluationFailure(VmFault.MEMORY_EXHAUSTED);
        }
        Value value = evaluate(arguments.get(1), locals, bus, budget, depth);
        globals.put(name, value);
        return value;
    }

    private Value arithmetic(
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth,
            Arithmetic operation
    ) {
        if (arguments.isEmpty()) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
        List<Value> values = evaluateArguments(arguments, locals, bus, budget, depth);
        int result = values.get(0).asNumber();
        if (values.size() == 1 && operation == Arithmetic.SUBTRACT) {
            return Value.number(-result);
        }
        for (int index = 1; index < values.size(); index++) {
            int operand = values.get(index).asNumber();
            if ((operation == Arithmetic.DIVIDE || operation == Arithmetic.MODULO) && operand == 0) {
                throw new EvaluationFailure(VmFault.DIVISION_BY_ZERO);
            }
            result = switch (operation) {
                case ADD -> result + operand;
                case SUBTRACT -> result - operand;
                case MULTIPLY -> result * operand;
                case DIVIDE -> result / operand;
                case MODULO -> result % operand;
            };
        }
        return Value.number(result);
    }

    private Value print(
            String name,
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth
    ) {
        requireArgumentCount(name, arguments, 1);
        Value value = evaluate(arguments.get(0), locals, bus, budget, depth);
        if (name.equals("println")) {
            output.appendLine(value.display());
        } else {
            output.append(value.display());
        }
        return value;
    }

    private Value device(
            String name,
            DeviceCommand command,
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth,
            int requiredArguments
    ) {
        if (arguments.size() > 1 || arguments.size() < requiredArguments) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
        int argument = arguments.isEmpty() ? 0 : evaluate(arguments.get(0), locals, bus, budget, depth).asNumber();
        ComputerDeviceBus.DeviceResult result = bus.execute(command, argument);
        return switch (result.status()) {
            case WAIT -> throw new DeviceWait();
            case FAULT -> throw new EvaluationFailure(result.fault());
            case COMPLETE -> Value.number(result.value());
        };
    }

    private Value callFunction(
            String name,
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth
    ) {
        UserFunction function = functions.get(name);
        if (function == null || function.parameters().size() != arguments.size()) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
        List<Value> values = evaluateArguments(arguments, locals, bus, budget, depth);
        Map<String, Value> functionLocals = new LinkedHashMap<>();
        for (int index = 0; index < values.size(); index++) {
            functionLocals.put(function.parameters().get(index), values.get(index));
        }
        return evaluateSequence(function.body(), functionLocals, bus, budget, depth);
    }

    private List<Value> evaluateArguments(
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth
    ) {
        List<Value> values = new ArrayList<>(arguments.size());
        for (Node argument : arguments) {
            values.add(evaluate(argument, locals, bus, budget, depth));
        }
        return values;
    }

    private Value single(
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth
    ) {
        requireArgumentCount("function", arguments, 1);
        return evaluate(arguments.get(0), locals, bus, budget, depth);
    }

    private static Value list(List<Value> values) {
        if (values.size() > MAX_LIST_SIZE) {
            throw new EvaluationFailure(VmFault.MEMORY_EXHAUSTED);
        }
        return Value.list(values);
    }

    private static Value listStar(List<Value> values) {
        if (values.isEmpty()) {
            return Value.nil();
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        List<Value> result = new ArrayList<>(values.subList(0, values.size() - 1));
        Value tail = values.get(values.size() - 1);
        result.addAll(tail.list());
        return list(result);
    }

    private static Value cons(List<Value> values) {
        if (values.size() != 2) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
        List<Value> result = new ArrayList<>();
        result.add(values.get(0));
        result.addAll(values.get(1).list());
        return list(result);
    }

    private Value positional(
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth,
            int index
    ) {
        List<Value> values = single(arguments, locals, bus, budget, depth).list();
        return index < values.size() ? values.get(index) : Value.nil();
    }

    private Value rest(
            List<Node> arguments,
            Map<String, Value> locals,
            ComputerDeviceBus bus,
            Budget budget,
            int depth
    ) {
        List<Value> values = single(arguments, locals, bus, budget, depth).list();
        return values.isEmpty() ? Value.nil() : Value.list(values.subList(1, values.size()));
    }

    private static Value append(List<Value> values) {
        List<Value> result = new ArrayList<>();
        for (Value value : values) {
            result.addAll(value.list());
            if (result.size() > MAX_LIST_SIZE) {
                throw new EvaluationFailure(VmFault.MEMORY_EXHAUSTED);
            }
        }
        return Value.list(result);
    }

    private static Value reverse(Value value) {
        List<Value> result = new ArrayList<>(value.list());
        java.util.Collections.reverse(result);
        return Value.list(result);
    }

    private static void requireArgumentCount(String name, List<Node> arguments, int count) {
        if (arguments.size() != count) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
    }

    private static String functionName(List<Node> arguments) {
        if (arguments.size() < 3 || !(arguments.get(0) instanceof SymbolNode symbol)) {
            throw new EvaluationFailure(VmFault.INVALID_SOURCE);
        }
        return normalized(symbol.name());
    }

    private void collectFunctions(List<Node> parsedForms) {
        for (Node form : parsedForms) {
            if (!(form instanceof ListNode list)
                    || list.values().size() < 4
                    || !(list.values().get(0) instanceof SymbolNode head)
                    || !normalized(head.name()).equals("defun")
                    || !(list.values().get(1) instanceof SymbolNode name)
                    || !(list.values().get(2) instanceof ListNode parameters)) {
                continue;
            }
            List<String> names = new ArrayList<>();
            for (Node parameter : parameters.values()) {
                if (!(parameter instanceof SymbolNode symbol)) {
                    throw invalid("Function parameters must be symbols");
                }
                names.add(normalized(symbol.name()));
            }
            if (names.size() > 16 || functions.putIfAbsent(
                    normalized(name.name()),
                    new UserFunction(List.copyOf(names), List.copyOf(list.values().subList(3, list.values().size())))
            ) != null) {
                throw invalid("Duplicate or oversized function " + name.name());
            }
        }
    }

    private static void validateDevicePlacement(List<Node> parsedForms) {
        for (Node form : parsedForms) {
            if (!containsDevice(form)) {
                continue;
            }
            if (!(form instanceof ListNode list)
                    || list.values().isEmpty()
                    || !(list.values().get(0) instanceof SymbolNode head)
                    || !isDeviceName(normalized(head.name()))) {
                throw invalid("Device calls must be top-level forms");
            }
            for (Node argument : list.values().subList(1, list.values().size())) {
                if (containsDevice(argument)) {
                    throw invalid("Nested device calls are not allowed");
                }
            }
        }
    }

    private static boolean containsDevice(Node node) {
        if (!(node instanceof ListNode list) || list.values().isEmpty()) {
            return false;
        }
        if (list.values().get(0) instanceof SymbolNode head && isDeviceName(normalized(head.name()))) {
            return true;
        }
        return list.values().stream().anyMatch(LispEngine::containsDevice);
    }

    private static boolean isDeviceName(String name) {
        return switch (name) {
            case "mine", "front", "back", "left", "right", "up", "down", "move", "scan", "quarry",
                    "redstone", "inventory", "energy" -> true;
            default -> false;
        };
    }

    private static Value quoted(Node node, int depth) {
        if (depth > MAX_DEPTH) {
            throw new EvaluationFailure(VmFault.MEMORY_EXHAUSTED);
        }
        if (node instanceof NumberNode number) {
            return Value.number(number.value());
        }
        if (node instanceof StringNode string) {
            return Value.string(string.value());
        }
        if (node instanceof SymbolNode symbol) {
            return Value.symbol(symbol.name());
        }
        if (node instanceof QuoteNode quote) {
            return quoted(quote.value(), depth + 1);
        }
        List<Value> values = new ArrayList<>();
        for (Node child : ((ListNode) node).values()) {
            values.add(quoted(child, depth + 1));
        }
        return Value.list(values);
    }

    private void latch(VmFault newFault) {
        fault = newFault;
        running = false;
    }

    @Override
    public boolean running() {
        return running;
    }

    @Override
    public int programCounter() {
        return programCounter;
    }

    @Override
    public int lastResult() {
        return lastResult;
    }

    @Override
    public VmFault fault() {
        return fault;
    }

    @Override
    public String output() {
        return output.value();
    }

    @Override
    public CompoundTag saveState() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(PC_TAG, programCounter);
        tag.putBoolean(RUNNING_TAG, running);
        tag.putString(FAULT_TAG, fault.name());
        tag.putInt(LAST_RESULT_TAG, lastResult);
        tag.putString(OUTPUT_TAG, output.value());
        ListTag encodedGlobals = new ListTag();
        globals.forEach((name, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString(NAME_TAG, name);
            entry.put(VALUE_TAG, value.save(0));
            encodedGlobals.add(entry);
        });
        tag.put(GLOBALS_TAG, encodedGlobals);
        return tag;
    }

    @Override
    public boolean restoreState(CompoundTag tag) {
        if (!tag.contains(PC_TAG, Tag.TAG_INT)
                || !tag.contains(RUNNING_TAG, Tag.TAG_BYTE)
                || !tag.contains(FAULT_TAG, Tag.TAG_STRING)
                || !tag.contains(LAST_RESULT_TAG, Tag.TAG_INT)
                || !tag.contains(OUTPUT_TAG, Tag.TAG_STRING)
                || !tag.contains(GLOBALS_TAG, Tag.TAG_LIST)) {
            return false;
        }
        int restoredPc = tag.getInt(PC_TAG);
        VmFault restoredFault = VmFault.fromPersistentName(tag.getString(FAULT_TAG));
        ListTag encodedGlobals = tag.getList(GLOBALS_TAG, Tag.TAG_COMPOUND);
        if (restoredPc < 0 || restoredPc > forms.size()
                || restoredFault == VmFault.INVALID_SNAPSHOT
                || encodedGlobals.size() > MAX_GLOBALS) {
            return false;
        }
        Map<String, Value> restoredGlobals = new LinkedHashMap<>();
        try {
            for (Tag element : encodedGlobals) {
                CompoundTag entry = (CompoundTag) element;
                if (!entry.contains(NAME_TAG, Tag.TAG_STRING) || !entry.contains(VALUE_TAG, Tag.TAG_COMPOUND)) {
                    return false;
                }
                String name = normalized(entry.getString(NAME_TAG));
                if (name.isBlank() || restoredGlobals.put(name, Value.load(entry.getCompound(VALUE_TAG), 0)) != null) {
                    return false;
                }
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
        programCounter = restoredPc;
        running = tag.getBoolean(RUNNING_TAG);
        fault = restoredFault;
        lastResult = tag.getInt(LAST_RESULT_TAG);
        output.restore(tag.getString(OUTPUT_TAG));
        globals.clear();
        globals.putAll(restoredGlobals);
        return (!running || programCounter < forms.size()) && (!running || fault == VmFault.NONE);
    }

    private static String normalized(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid Lisp source: " + message);
    }

    private enum Arithmetic {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        MODULO
    }

    private enum Kind {
        NUMBER,
        STRING,
        SYMBOL,
        LIST,
        BOOLEAN,
        NIL
    }

    private record UserFunction(List<String> parameters, List<Node> body) {
    }

    private record Value(Kind kind, int number, String text, List<Value> values) {
        private static Value number(int value) {
            return new Value(Kind.NUMBER, value, "", List.of());
        }

        private static Value string(String value) {
            return new Value(Kind.STRING, 0, value, List.of());
        }

        private static Value symbol(String value) {
            return new Value(Kind.SYMBOL, 0, value, List.of());
        }

        private static Value list(List<Value> values) {
            if (values.isEmpty()) {
                return nil();
            }
            if (values.size() > MAX_LIST_SIZE) {
                throw new EvaluationFailure(VmFault.MEMORY_EXHAUSTED);
            }
            return new Value(Kind.LIST, 0, "", List.copyOf(values));
        }

        private static Value bool(boolean value) {
            return value ? new Value(Kind.BOOLEAN, 1, "", List.of()) : nil();
        }

        private static Value nil() {
            return new Value(Kind.NIL, 0, "", List.of());
        }

        private int asNumber() {
            if (kind != Kind.NUMBER && kind != Kind.BOOLEAN) {
                throw new EvaluationFailure(VmFault.INVALID_SOURCE);
            }
            return number;
        }

        private List<Value> list() {
            if (kind == Kind.NIL) {
                return List.of();
            }
            if (kind != Kind.LIST) {
                throw new EvaluationFailure(VmFault.INVALID_SOURCE);
            }
            return values;
        }

        private int length() {
            return switch (kind) {
                case LIST -> values.size();
                case NIL -> 0;
                case STRING, SYMBOL -> text.length();
                default -> 1;
            };
        }

        private boolean isNil() {
            return kind == Kind.NIL;
        }

        private boolean truthy() {
            return kind != Kind.NIL && (kind != Kind.BOOLEAN || number != 0);
        }

        private int numericResult() {
            return switch (kind) {
                case NUMBER, BOOLEAN -> number;
                case NIL -> 0;
                default -> 1;
            };
        }

        private String display() {
            return switch (kind) {
                case NUMBER -> Integer.toString(number);
                case STRING, SYMBOL -> text;
                case BOOLEAN -> number == 0 ? "nil" : "true";
                case NIL -> "nil";
                case LIST -> "(" + values.stream().map(Value::display).collect(java.util.stream.Collectors.joining(" ")) + ")";
            };
        }

        private Node toNode() {
            return switch (kind) {
                case NUMBER -> new NumberNode(number);
                case STRING -> new StringNode(text);
                case SYMBOL -> new SymbolNode(text);
                case BOOLEAN -> new SymbolNode(number == 0 ? "nil" : "true");
                case NIL -> new ListNode(List.of());
                case LIST -> new ListNode(values.stream().map(Value::toNode).toList());
            };
        }

        private CompoundTag save(int depth) {
            if (depth > MAX_DEPTH) {
                throw new IllegalArgumentException("Lisp value nesting exceeds limit");
            }
            CompoundTag tag = new CompoundTag();
            tag.putString(TYPE_TAG, kind.name());
            switch (kind) {
                case NUMBER, BOOLEAN -> tag.putInt(NUMBER_TAG, number);
                case STRING, SYMBOL -> tag.putString(TEXT_TAG, text);
                case LIST -> {
                    ListTag encoded = new ListTag();
                    for (Value value : values) {
                        encoded.add(value.save(depth + 1));
                    }
                    tag.put(VALUES_TAG, encoded);
                }
                case NIL -> {
                }
            }
            return tag;
        }

        private static Value load(CompoundTag tag, int depth) {
            if (depth > MAX_DEPTH || !tag.contains(TYPE_TAG, Tag.TAG_STRING)) {
                throw new IllegalArgumentException("Invalid Lisp value");
            }
            Kind kind;
            try {
                kind = Kind.valueOf(tag.getString(TYPE_TAG));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown Lisp value type", exception);
            }
            return switch (kind) {
                case NUMBER -> {
                    requireTag(tag, NUMBER_TAG, Tag.TAG_INT);
                    yield number(tag.getInt(NUMBER_TAG));
                }
                case BOOLEAN -> {
                    requireTag(tag, NUMBER_TAG, Tag.TAG_INT);
                    yield bool(tag.getInt(NUMBER_TAG) != 0);
                }
                case STRING -> {
                    requireTag(tag, TEXT_TAG, Tag.TAG_STRING);
                    yield string(tag.getString(TEXT_TAG));
                }
                case SYMBOL -> {
                    requireTag(tag, TEXT_TAG, Tag.TAG_STRING);
                    yield symbol(tag.getString(TEXT_TAG));
                }
                case NIL -> nil();
                case LIST -> {
                    requireTag(tag, VALUES_TAG, Tag.TAG_LIST);
                    ListTag encoded = tag.getList(VALUES_TAG, Tag.TAG_COMPOUND);
                    if (encoded.size() > MAX_LIST_SIZE) {
                        throw new IllegalArgumentException("Lisp list exceeds limit");
                    }
                    List<Value> values = new ArrayList<>();
                    for (Tag element : encoded) {
                        values.add(load((CompoundTag) element, depth + 1));
                    }
                    yield list(values);
                }
            };
        }

        private static void requireTag(CompoundTag tag, String key, int type) {
            if (!tag.contains(key, type)) {
                throw new IllegalArgumentException("Missing Lisp value field " + key);
            }
        }
    }

    private sealed interface Node permits NumberNode, StringNode, SymbolNode, ListNode, QuoteNode {
    }

    private record NumberNode(int value) implements Node {
    }

    private record StringNode(String value) implements Node {
    }

    private record SymbolNode(String name) implements Node {
    }

    private record ListNode(List<Node> values) implements Node {
    }

    private record QuoteNode(Node value) implements Node {
    }

    private static final class Budget {
        private final int limit;
        private int used;

        private Budget(int limit) {
            this.limit = limit;
        }

        private void step() {
            if (++used > limit) {
                throw new EvaluationFailure(VmFault.EXECUTION_LIMIT);
            }
        }
    }

    private static final class EvaluationFailure extends RuntimeException {
        private final VmFault fault;

        private EvaluationFailure(VmFault fault) {
            super(null, null, false, false);
            this.fault = fault;
        }
    }

    private static final class DeviceWait extends RuntimeException {
        private DeviceWait() {
            super(null, null, false, false);
        }
    }

    private static final class Parser {
        private final List<Token> tokens;
        private int index;
        private int nodes;

        private Parser(String source) {
            tokens = lex(source);
        }

        private List<Node> parseAll() {
            List<Node> result = new ArrayList<>();
            while (index < tokens.size()) {
                result.add(parse(0));
            }
            return List.copyOf(result);
        }

        private Node parse(int depth) {
            if (depth > MAX_DEPTH || ++nodes > MAX_NODES || index >= tokens.size()) {
                throw invalid("Expression exceeds parser limits");
            }
            Token token = tokens.get(index++);
            return switch (token.type()) {
                case LEFT -> {
                    List<Node> values = new ArrayList<>();
                    while (index < tokens.size() && tokens.get(index).type() != TokenType.RIGHT) {
                        values.add(parse(depth + 1));
                    }
                    if (index >= tokens.size()) {
                        throw invalid("Missing ')'");
                    }
                    index++;
                    yield new ListNode(List.copyOf(values));
                }
                case RIGHT -> throw invalid("Unexpected ')'");
                case QUOTE -> new QuoteNode(parse(depth + 1));
                case STRING -> new StringNode(token.text());
                case ATOM -> atom(token.text());
            };
        }

        private static Node atom(String text) {
            try {
                return new NumberNode(Integer.decode(text));
            } catch (NumberFormatException ignored) {
                return new SymbolNode(text);
            }
        }

        private static List<Token> lex(String source) {
            List<Token> result = new ArrayList<>();
            for (int index = 0; index < source.length();) {
                char current = source.charAt(index);
                if (Character.isWhitespace(current)) {
                    index++;
                } else if (current == ';') {
                    while (index < source.length() && source.charAt(index) != '\n') {
                        index++;
                    }
                } else if (current == '(') {
                    result.add(new Token(TokenType.LEFT, "("));
                    index++;
                } else if (current == ')') {
                    result.add(new Token(TokenType.RIGHT, ")"));
                    index++;
                } else if (current == '\'') {
                    result.add(new Token(TokenType.QUOTE, "'"));
                    index++;
                } else if (current == '"') {
                    StringBuilder value = new StringBuilder();
                    index++;
                    boolean closed = false;
                    while (index < source.length()) {
                        char character = source.charAt(index++);
                        if (character == '"') {
                            closed = true;
                            break;
                        }
                        if (character == '\\' && index < source.length()) {
                            char escaped = source.charAt(index++);
                            value.append(escaped == 'n' ? '\n' : escaped);
                        } else {
                            value.append(character);
                        }
                    }
                    if (!closed) {
                        throw invalid("Unclosed string");
                    }
                    result.add(new Token(TokenType.STRING, value.toString()));
                } else {
                    int start = index;
                    while (index < source.length()) {
                        char character = source.charAt(index);
                        if (Character.isWhitespace(character) || character == '(' || character == ')' || character == '\'') {
                            break;
                        }
                        index++;
                    }
                    result.add(new Token(TokenType.ATOM, source.substring(start, index)));
                }
                if (result.size() > MAX_NODES * 2) {
                    throw invalid("Too many tokens");
                }
            }
            return List.copyOf(result);
        }
    }

    private enum TokenType {
        LEFT,
        RIGHT,
        QUOTE,
        STRING,
        ATOM
    }

    private record Token(TokenType type, String text) {
    }
}
