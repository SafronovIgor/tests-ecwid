import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;

public final class CopyUtils {

    public static void main(String[] args) throws Exception {
        System.out.println("*** Test: 1 ***");
        var mans = new ArrayList<>();
        mans.add(new Man("Alex", 34, List.of("book 1")));
        mans.add(new Man("Igor", 24, new ArrayList<>(List.of("book 2"))));
        mans.add(new Man("Sergei", 32, new LinkedList<>(List.of("book 3"))));

        var clone = CopyUtils.deepClone(mans);
        System.out.println(clone);
    }

    @SuppressWarnings("unchecked")
    public static <T> T deepClone(T original) throws Exception {
        Map<Object, Object> cloneMap = new IdentityHashMap<>();
        return (T) deepCloneInternal(original, cloneMap);
    }

    private static Object deepCloneInternal(Object original, Map<Object, Object> cloneMap) throws Exception {
        if (original == null || isImmutable(original)) {
            return original;
        }

        if (cloneMap.containsKey(original)) {
            return cloneMap.get(original);
        }

        var copy = createInstanceWithDefaults(original.getClass());
        cloneMap.put(original, copy);

        if (original instanceof Collection) {
            copy = copyCollection((Collection<?>) original, cloneMap);
        } else if (original instanceof Map) {
            copy = copyMap((Map<?, ?>) original, cloneMap);
        } else {
            deepCopyFields(copy, original, cloneMap);
        }

        return copy;
    }

    private static Object createInstanceWithDefaults(Class<?> clazz) throws Exception {
        var constructor = getConstructorForCopy(clazz);
        constructor.setAccessible(true);
        var parameters = getDefaultParameters(constructor);
        return constructor.newInstance(parameters);
    }

    private static Constructor<?> getConstructorForCopy(Class<?> clazz) throws NoSuchMethodException {
        var constructors = clazz.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new NoSuchMethodException("No constructors found for class: " + clazz.getName());
        }

        return Arrays.stream(constructors)
                .min(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() -> new NoSuchMethodException("No suitable constructor found for class: " + clazz.getName()));
    }

    private static Object[] getDefaultParameters(Constructor<?> constructor) {
        return Arrays.stream(constructor.getParameterTypes())
                .map(CopyUtils::getDefaultValue)
                .toArray();
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            return switch (type.getName()) {
                case "boolean" -> false;
                case "byte" -> (byte) 0;
                case "char" -> '\0';
                case "short" -> (short) 0;
                case "int" -> 0;
                case "long" -> 0L;
                case "float" -> 0.0f;
                case "double" -> 0.0;
                default -> null;
            };
        }
        return null;
    }

    private static <T> void deepCopyFields(T copy, T original, Map<Object, Object> cloneMap) {
        var clazz = original.getClass();
        Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.isSynthetic() && !Modifier.isFinal(field.getModifiers()) && !Modifier.isTransient(field.getModifiers()))
                .forEach(field -> {
                    field.setAccessible(true);
                    try {
                        var value = field.get(original);
                        if (value instanceof Collection) {
                            field.set(copy, copyCollection((Collection<?>) value, cloneMap));
                        } else if (value instanceof Map) {
                            field.set(copy, copyMap((Map<?, ?>) value, cloneMap));
                        } else if (!isImmutable(value)) {
                            field.set(copy, deepCloneInternal(value, cloneMap));
                        } else {
                            field.set(copy, value);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to copy field: " + field.getName(), e);
                    }
                });
    }


    @SuppressWarnings("unchecked")
    private static <E> Collection<E> copyCollection(Collection<? extends E> original, Map<Object, Object> cloneMap) throws Exception {
        if (isImmutable(original)) {
            return (Collection<E>) original;
        }
        var collectionType = (Class<? extends Collection<E>>) original.getClass();
        var constructor = collectionType.getDeclaredConstructor();
        constructor.setAccessible(true);
        var copy = constructor.newInstance();

        for (E element : original) {
            copy.add((E) deepCloneInternal(element, cloneMap));
        }

        return copy;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> copyMap(Map<? extends K, ? extends V> original, Map<Object, Object> cloneMap) throws Exception {
        if (isImmutable(original)) {
            return (Map<K, V>) original;
        }
        var mapType = (Class<? extends Map<K, V>>) original.getClass();
        var constructor = mapType.getDeclaredConstructor();
        constructor.setAccessible(true);
        var copy = constructor.newInstance();

        for (var entry : original.entrySet()) {
            K key = (K) deepCloneInternal(entry.getKey(), cloneMap);
            V value = (V) deepCloneInternal(entry.getValue(), cloneMap);
            copy.put(key, value);
        }

        return copy;
    }

    private static boolean isImmutable(Object obj) {
        if (obj == null) return true;
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj instanceof Character) {
            return true;
        }
        if (obj instanceof Collection || obj instanceof Map) {
            var className = obj.getClass().getName();
            return className.contains("Immutable") || className.contains("Unmodifiable");
        }
        return false;
    }
}
