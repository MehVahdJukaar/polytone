package net.mehvahdjukaar.polytone.common.expressions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionValidatorTest {

    // stand-ins for the real proxies, same package prefix so collectMembers walks them
    public static class Block {
        public String block() { return ""; }
        public Block up() { return this; }
        public boolean hasFluid() { return false; }
        public Object blockStateValue(String key) { return null; }
    }

    public static class Entity extends Block {
        public double age() { return 0; }
        public String name() { return ""; }
        public Entity owner() { return this; }
        public boolean inWater() { return false; }
        public String armor(String slot) { return ""; }
    }

    public static class Particle extends Block {
        public double roll() { return 0; }
        public void roll(double roll) {}
        public void blue(double b) {}
        public void removed() {}
        public double custom() { return 0; }
        public void custom(double v) {}
    }

    public static class Global {
        public double time() { return 0; }
        public Entity lastInteractedEntity() { return null; }
    }

    public static class Bad {
        public Object getRuntime() { return null; }
    }

    static final ExpressionValidator V = new ExpressionValidator(
            Set.of("p", "player", "g", "global", "o", "object", "r", "random", "v",
                    "sin", "cos", "clamp", "color", "config", "colormap", "PI"),
            ExpressionValidator.collectMembers(List.of(Entity.class, Particle.class, Global.class)));

    static String check(String expr) {
        return V.validate(expr, Set.of("my_pack_calc"));
    }

    static void ok(String expr) {
        assertNull(check(expr), expr);
    }

    static void bad(String expr) {
        assertNotNull(check(expr), expr);
    }

    @Test
    void wikiStyleScript() {
        ok("""
                t = global.time();
                r = 0.5; g = 0.5; b = 0.5;
                if (player.age() > 0.3) {
                    r = 1.0;
                } else if (player.age > 0.1) {
                    r = 0.7;
                }
                if (object.hasFluid()) {
                    g = clamp(object.up.block == 'x' ? 1 : 0, 0, 1);
                } else {
                    g = 0.2;
                }
                if (t % 2 < 1) {
                    b = 0.5 + 0.5 * sin(t);
                } else {
                    b = 0.3 + 0.3 * cos(global.time());
                }
                if (p.inWater) {
                    g += 0.2;
                }
                color(r, g, b, 1.0);
                """);
    }

    @Test
    void propertyAndMethodSpellings() {
        ok("p.age");
        ok("p.getAge()");
        ok("player.age()");
        ok("p.isInWater()");
        ok("player.armor('head') == 'minecraft:iron_helmet' ? 1 : 0");
        ok("p.owner.?age");
        ok("g.lastInteractedEntity.?name");
        ok("o.up.up.block");
        ok("p.owner().owner().name()");
        ok("p.blockStateValue('facing') == 'north'");
    }

    @Test
    void particleTicker() {
        ok("if(o.custom>4){ o.setRemoved(); }; o.roll = 2 + o.roll; o.setBlue(255); o.custom = 3;");
    }

    @Test
    void stringsGlobalsNumbersAndLocals() {
        ok("'abc'.length() > 2");
        ok("p.name().toLowerCase() == 'x'");
        ok("my_pack_calc + 1");
        ok("isdef my_pack_calc ? 1 : 0");
        ok("PI * 2");
        ok("sin(p.age) + config('x')");
        ok("return p.age;");
        ok("var q = 3; q * 2");
        ok("x = 1; x++; x");
        ok("x = 1; x *= 2; x -= 1");
        ok("1.5e-3 + 0x1F + .5 + 10L + 2.0f");
        ok("-p.age");
        ok("(int) p.age");
        ok("(p.age) != 0 && !((g.time) != 0)");
        ok("p.name() ~= '.*'");
    }

    @Test
    void reflectionAndClassAccess() {
        bad("Runtime.getRuntime().exec('x')");
        bad("java.lang.Runtime.getRuntime()");
        bad("System.exit(0)");
        bad("Thread.sleep(1)");
        bad("p.getClass()");
        bad("p.getClass");
        bad("p.class");
        bad("p.name().class");
        bad("p.owner().getClass()");
        bad("p.owner().forName('x')");
        bad("'x'.getBytes()");
        bad("'x'.getClass().forName('java.lang.Runtime')");
        bad("x = 1; x.getRuntime()");
        bad("Math.getClass()");
        bad("getClass");
        bad("1.getClass()");
        bad("this");
        bad("unknownVar + 1");
    }

    @Test
    void languageConstructs() {
        bad("new java.io.File('x')");
        bad("import java.io.File; 1");
        bad("with (p) { age = 1 }");
        bad(".{ age = 1 }");
        bad("p.{ age = 1 }");
        bad("p['getClass']");
        bad("[1, 2]");
        bad("{1, 2}");
        bad("@foo 1");
        bad("while (true) {}");
        bad("foreach (x : p) {}");
        bad("def f() { 1 }; f()");
        bad("function f() { 1 }; f()");
        bad("assert false");
        bad("p.age instanceof double");
        bad("p.age is double");
    }

    @Test
    void lexicalGarbage() {
        bad("p.näme");
        bad("p.name()\0");
        bad("p.name\\u0028)");
        bad("'abc");
        bad("(p.age");
        bad("p.age)");
        bad("5.x");
        bad("1abc");
        bad("p.");
        bad(".age");
        bad("p..age");
        bad("(".repeat(65) + "1" + ")".repeat(65));
        bad("1+".repeat(ExpressionValidator.MAX_LENGTH / 2) + "1");
    }

    @Test
    void apiExposingAGatewayRefusesToBuild() {
        assertThrows(IllegalStateException.class, () -> ExpressionValidator.collectMembers(List.of(Bad.class)));
    }

    @Test
    void rejectionNamesTheToken() {
        assertEquals("unknown member 'getBytes'", check("'x'.getBytes()"));
        assertEquals("'getClass' is not allowed", check("p.getClass()"));
        assertEquals("unknown identifier 'Runtime'", check("Runtime.getRuntime()"));
    }
}
