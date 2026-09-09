package net.mehvahdjukaar.polytone.common.expressions;

import org.jetbrains.annotations.Nullable;
import org.mvel2.ParserContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//TODO: this is bad. we need to migrate to a new lib

// Pack strings go through this before MVEL sees them. MVEL is a full Java scripting language with no
// sandbox, so the only safe rule is an allowlist: known identifiers, member names our proxies declare,
// a fixed set of keywords and punctuation. Everything else is rejected with a reason.
public final class ExpressionValidator {

    public static final int MAX_LENGTH = 32768;
    public static final int MAX_NESTING = 64;

    // reflection and JVM gateways, refused on any receiver. A proxy declaring one is a bug and
    // collectMembers refuses to build
    private static final Set<String> NEVER = Set.of(
            "getClass", "class", "forName", "getClassLoader", "classLoader", "loadClass", "defineClass",
            "getMethod", "getMethods", "getDeclaredMethod", "getDeclaredMethods",
            "getField", "getFields", "getDeclaredField", "getDeclaredFields",
            "getConstructor", "getConstructors", "getDeclaredConstructor", "getDeclaredConstructors",
            "newInstance", "invoke", "setAccessible", "getResource", "getResourceAsStream",
            "getRuntime", "exec", "exit", "halt", "load", "loadLibrary", "getenv",
            "getProperty", "setProperty", "getProperties", "getSystemClassLoader",
            "getContextClassLoader", "setContextClassLoader", "lookup", "findVirtual", "findStatic",
            "findClass", "unreflect", "wait", "notify", "notifyAll", "finalize");
    private static final Set<String> WORDS = Set.of(
            "if", "else", "return", "var", "true", "false", "null", "nil", "empty", "and", "or", "isdef", "contains",
            "int", "long", "double", "float", "boolean");
    // plain String api, harmless on any value
    private static final Set<String> COMMON_MEMBERS = Set.of(
            "length", "isEmpty", "isBlank", "contains", "startsWith", "endsWith", "indexOf", "substring",
            "toLowerCase", "toUpperCase", "trim", "equals", "equalsIgnoreCase", "charAt", "matches", "replace",
            "toString", "hashCode");
    private static final String PUNCT = "(){}.,;:?!+-*/%<>=&|^~";

    private static final int IDENT = 0, NUMBER = 1, STRING = 2, SYMBOL = 3;

    private record Tok(int kind, String text, int start, int end) {
    }

    private final Set<String> identifiers;
    private final Set<String> members;

    public ExpressionValidator(Set<String> identifiers, Set<String> members) {
        this.identifiers = Set.copyOf(identifiers);
        this.members = Set.copyOf(members);
    }

    public static ExpressionValidator forContext(ParserContext ctx) {
        Map<String, Class> inputs = ctx.getInputs();
        if (inputs == null) inputs = Map.of();
        Set<String> ids = new HashSet<>(ctx.getImports().keySet());
        for (String n : inputs.keySet()) {
            if (!MvelLockdown.isShadow(n)) ids.add(n);
        }
        return new ExpressionValidator(ids, collectMembers(inputs.values()));
    }

    // every public instance method and field on the proxy classes and on whatever proxy they return,
    // plus the get/is/set spellings MVEL resolves properties through
    public static Set<String> collectMembers(Collection<? extends Class> roots) {
        Set<String> names = new HashSet<>(COMMON_MEMBERS);
        Deque<Class> queue = new ArrayDeque<>(roots);
        Set<Class<?>> seen = new HashSet<>();
        while (!queue.isEmpty()) {
            Class<?> c = queue.poll();
            if (!isProxy(c) || !seen.add(c)) continue;
            for (Method m : c.getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) || !isProxy(m.getDeclaringClass())) continue;
                addBeanForms(names, m.getName());
                queue.add(m.getReturnType());
            }
            for (Field f : c.getFields()) {
                if (Modifier.isStatic(f.getModifiers()) || !isProxy(f.getDeclaringClass())) continue;
                addBeanForms(names, f.getName());
                queue.add(f.getType());
            }
        }
        for (String n : names) {
            if (NEVER.contains(n)) throw new IllegalStateException("expression api exposes '" + n + "'");
        }
        return names;
    }

    private static boolean isProxy(Class<?> c) {
        return c.getName().startsWith("net.mehvahdjukaar.polytone.");
    }

    private static void addBeanForms(Set<String> names, String name) {
        names.add(name);
        for (String prefix : List.of("get", "is", "set")) {
            if (name.length() > prefix.length() && name.startsWith(prefix) && Character.isUpperCase(name.charAt(prefix.length()))) {
                names.add(Character.toLowerCase(name.charAt(prefix.length())) + name.substring(prefix.length() + 1));
                return;
            }
        }
        String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        names.add("get" + cap);
        names.add("is" + cap);
        names.add("set" + cap);
    }

    // null when the expression is fine, otherwise why it was rejected
    public @Nullable String validate(String expr, Collection<String> extraIdentifiers) {
        if (expr.length() > MAX_LENGTH) return "longer than " + MAX_LENGTH + " characters";
        List<Tok> toks = new ArrayList<>();
        String lexError = lex(expr, toks);
        if (lexError != null) return lexError;

        Set<String> locals = new HashSet<>();
        for (int i = 0; i + 1 < toks.size(); i++) {
            if (toks.get(i).kind == IDENT && !isMember(toks, i) && isAssignment(toks, i + 1)) locals.add(toks.get(i).text);
        }

        int depth = 0;
        for (int i = 0; i < toks.size(); i++) {
            Tok t = toks.get(i);
            if (t.kind == IDENT) {
                if (NEVER.contains(t.text)) return "'" + t.text + "' is not allowed";
                if (isMember(toks, i)) {
                    if (!members.contains(t.text)) return "unknown member '" + t.text + "'";
                } else if (!WORDS.contains(t.text) && !identifiers.contains(t.text) && !locals.contains(t.text) && !extraIdentifiers.contains(t.text)) {
                    return "unknown identifier '" + t.text + "'";
                }
            } else if (t.kind == SYMBOL) {
                char c = t.text.charAt(0);
                if (c == '(' || c == '{') {
                    if (++depth > MAX_NESTING) return "nested deeper than " + MAX_NESTING;
                } else if (c == ')' || c == '}') {
                    if (--depth < 0) return "unbalanced '" + c + "'";
                }
                boolean opensBlock = tokIs(toks, i - 1, ")") || (i > 0 && toks.get(i - 1).kind == IDENT && toks.get(i - 1).text.equals("else"));
                if (c == '{' && !opensBlock) return "'{' can only open an if/else block";
                if (c == '.') {
                    Tok prev = i > 0 ? toks.get(i - 1) : null;
                    boolean hasReceiver = prev != null && (prev.kind == IDENT || prev.kind == STRING || prev.text.equals(")"));
                    int next = tokIs(toks, i + 1, "?") ? i + 2 : i + 1;
                    boolean hasMember = next < toks.size() && toks.get(next).kind == IDENT;
                    if (!hasReceiver || !hasMember) return "'.' must sit between a value and a member name";
                }
            }
        }
        if (depth != 0) return "unbalanced brackets";
        return null;
    }

    // name after '.' or after the null safe '.?'
    private static boolean isMember(List<Tok> toks, int i) {
        return tokIs(toks, i - 1, ".") || (tokIs(toks, i - 1, "?") && tokIs(toks, i - 2, "."));
    }

    // '=' or one of += -= *= /= %= starting at j, never the first half of '=='
    private static boolean isAssignment(List<Tok> toks, int j) {
        if (tokIs(toks, j, "=")) return !adjacent(toks, j, "=");
        boolean compound = toks.get(j).kind == SYMBOL && "+-*/%".indexOf(toks.get(j).text.charAt(0)) >= 0;
        return compound && adjacent(toks, j, "=") && !adjacent(toks, j + 1, "=");
    }

    private static boolean adjacent(List<Tok> toks, int j, String next) {
        return tokIs(toks, j + 1, next) && toks.get(j + 1).start == toks.get(j).end;
    }

    private static boolean tokIs(List<Tok> toks, int i, String text) {
        return i >= 0 && i < toks.size() && toks.get(i).kind == SYMBOL && toks.get(i).text.equals(text);
    }

    private static @Nullable String lex(String s, List<Tok> out) {
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                i++;
                continue;
            }
            int start = i;
            Tok last = out.isEmpty() ? null : out.get(out.size() - 1);
            boolean afterValue = last != null && (last.kind != SYMBOL || last.text.equals(")"));
            if (c == '\'' || c == '"') {
                i++;
                while (i < n && s.charAt(i) != c) {
                    if (s.charAt(i) == '\\') i++;
                    i++;
                }
                if (i >= n) return "unterminated string";
                i++;
                out.add(new Tok(STRING, s.substring(start, i), start, i));
            } else if (isIdentStart(c)) {
                while (i < n && isIdentPart(s.charAt(i))) i++;
                out.add(new Tok(IDENT, s.substring(start, i), start, i));
            } else if (isDigit(c) || (c == '.' && !afterValue && i + 1 < n && isDigit(s.charAt(i + 1)))) {
                i = lexNumber(s, i);
                if (i < 0) return "malformed number";
                out.add(new Tok(NUMBER, s.substring(start, i), start, i));
            } else if (PUNCT.indexOf(c) >= 0) {
                i++;
                out.add(new Tok(SYMBOL, String.valueOf(c), start, i));
            } else {
                return "character '" + c + "' is not allowed";
            }
        }
        return null;
    }

    // end of the number starting at i, or -1 when it runs straight into letters or a second dot
    private static int lexNumber(String s, int i) {
        int n = s.length();
        if (s.charAt(i) == '0' && i + 1 < n && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) {
            i += 2;
            while (i < n && Character.digit(s.charAt(i), 16) >= 0) i++;
        } else {
            while (i < n && isDigit(s.charAt(i))) i++;
            if (i + 1 < n && s.charAt(i) == '.' && isDigit(s.charAt(i + 1))) {
                i++;
                while (i < n && isDigit(s.charAt(i))) i++;
            }
            if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                int j = i + 1;
                if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) j++;
                if (j < n && isDigit(s.charAt(j))) {
                    i = j;
                    while (i < n && isDigit(s.charAt(i))) i++;
                }
            }
        }
        if (i < n && "dDfFlLiIbB".indexOf(s.charAt(i)) >= 0) i++;
        if (i < n && (isIdentPart(s.charAt(i)) || s.charAt(i) == '.')) return -1;
        return i;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
    }

    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || isDigit(c);
    }
}
