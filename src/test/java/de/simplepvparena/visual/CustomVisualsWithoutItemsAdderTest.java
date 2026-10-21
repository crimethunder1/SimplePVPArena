package de.simplepvparena.visual;

import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomVisualsWithoutItemsAdderTest {

    private static final String ITEMS_ADDER_PACKAGE = "dev.lone.";
    private static final String PLUGIN_PACKAGE = "de.simplepvparena.";
    private static final String ITEMS_ADDER_VISUALS = "de.simplepvparena.visual.ItemsAdderVisuals";

    @Test
    void theItemsAdderClassesAreReallyOnTheTestClassPathSoTheHidingBelowIsMeaningful() throws Exception {
        Class.forName("dev.lone.itemsadder.api.CustomStack", false, getClass().getClassLoader());
    }

    @Test
    void thePluginClassLoadsAndVerifiesWithoutAnyClassOfItemsAdder() throws Exception {
        ItemsAdderHidingClassLoader classLoader = new ItemsAdderHidingClassLoader(getClass().getClassLoader());

        Class<?> pluginClass = Class.forName("de.simplepvparena.SimplePVPArenaPlugin", true, classLoader);
        pluginClass.getDeclaredMethods();

        assertSame(classLoader, pluginClass.getClassLoader());
        assertEquals(List.of(), classLoader.hiddenRequests());
        assertFalse(classLoader.requested(ITEMS_ADDER_VISUALS), "ItemsAdderVisuals was loaded");
    }

    @Test
    void theSelectionWithoutItemsAdderRunsWithoutAnyClassOfItemsAdder() throws Exception {
        ItemsAdderHidingClassLoader classLoader = new ItemsAdderHidingClassLoader(getClass().getClassLoader());
        Class<?> selectionClass = Class.forName("de.simplepvparena.visual.CustomVisualsSelection", true, classLoader);
        Method select = selectionClass.getMethod("select", PluginManager.class, boolean.class, Supplier.class,
                Consumer.class);
        List<String> infoLines = new ArrayList<>();
        Supplier<Object> refusingFactory = () -> {
            throw new AssertionError("the ItemsAdder factory must not run without ItemsAdder");
        };

        Object withoutItemsAdder = select.invoke(null, pluginManager(false), true, refusingFactory,
                (Consumer<String>) infoLines::add);
        Object switchedOff = select.invoke(null, pluginManager(true), false, refusingFactory,
                (Consumer<String>) infoLines::add);

        assertEquals("de.simplepvparena.visual.VanillaVisuals", withoutItemsAdder.getClass().getName());
        assertEquals("de.simplepvparena.visual.VanillaVisuals", switchedOff.getClass().getName());
        assertSame(classLoader, withoutItemsAdder.getClass().getClassLoader());
        assertEquals(List.of(), classLoader.hiddenRequests());
        assertFalse(classLoader.requested(ITEMS_ADDER_VISUALS), "ItemsAdderVisuals was loaded");
    }

    @Test
    void creatingTheItemsAdderVisualsWithoutItemsAdderFailsWhichShowsTheClassesAreHidden() throws Exception {
        ItemsAdderHidingClassLoader classLoader = new ItemsAdderHidingClassLoader(getClass().getClassLoader());

        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> Class.forName(ITEMS_ADDER_VISUALS, true, classLoader).getConstructor().newInstance());

        assertInstanceOf(NoClassDefFoundError.class, failure.getCause());
        assertTrue(classLoader.hiddenRequests().stream().anyMatch(name -> name.startsWith(ITEMS_ADDER_PACKAGE)),
                classLoader.hiddenRequests().toString());
    }

    private static PluginManager pluginManager(boolean itemsAdderEnabled) {
        return SharedBukkitServer.proxy(PluginManager.class, (method, arguments) -> switch (method.getName()) {
            case "isPluginEnabled" -> itemsAdderEnabled;
            default -> throw new UnsupportedOperationException("PluginManager." + method.getName());
        });
    }

    private static final class ItemsAdderHidingClassLoader extends ClassLoader {

        private final List<String> hiddenRequests = new ArrayList<>();
        private final List<String> definedClasses = new ArrayList<>();

        private ItemsAdderHidingClassLoader(ClassLoader parent) {
            super(parent);
        }

        List<String> hiddenRequests() {
            return List.copyOf(hiddenRequests);
        }

        boolean requested(String className) {
            return definedClasses.contains(className);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                if (name.startsWith(ITEMS_ADDER_PACKAGE)) {
                    hiddenRequests.add(name);
                    throw new ClassNotFoundException(name);
                }
                if (!name.startsWith(PLUGIN_PACKAGE)) {
                    return super.loadClass(name, resolve);
                }
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = definePluginClass(name);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private Class<?> definePluginClass(String name) throws ClassNotFoundException {
            String resourceName = name.replace('.', '/') + ".class";
            try (InputStream classFile = getParent().getResourceAsStream(resourceName)) {
                if (classFile == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = classFile.readAllBytes();
                definedClasses.add(name);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException failure) {
                throw new ClassNotFoundException(name, failure);
            }
        }
    }
}
