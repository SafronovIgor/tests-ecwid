import java.lang.reflect.Constructor;
import java.util.*;

public final class CopyUtils {
    public static void main(String[] args) throws Exception {
        System.out.println("*** Test: 1 ***");
        ArrayList<String> books = new ArrayList<>();
        books.add("book 1");
        var obj = new Man("Alex", 45, books);
        var copy = CopyUtils.deepClone(obj);
        System.out.println("Original: " + obj);
        System.out.println("Cloned: " + copy);
    }

    private static final IdentityHashMap<Object, Object> cloneMap = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T deepClone(T original) throws Exception {
        if (cloneMap.containsKey(original)) {
            return (T) cloneMap.get(original);
        }

        var copy = (T) createInstanceWithDefaults(original.getClass());
        cloneMap.put(original, copy);

        deepCopyFields(copy, original);

        return copy;
    }

    public static Object createInstanceWithDefaults(Class<?> clazz) throws Exception {
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
                .map(type -> {
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
                })
                .toArray();
    }

    public static <T> void deepCopyFields(T copy, T original) {
        var clazz = original.getClass();

        Arrays.stream(clazz.getDeclaredFields())
                .peek(field -> field.setAccessible(true))
                .forEach(field -> {
                    try {
                        Object value = field.get(original);
                        if (value instanceof Collection) {
                            field.set(copy, copyCollection((Collection<?>) value));
                        } else if (value instanceof Map) {
                            field.set(copy, copyMap((Map<?, ?>) value));
                        } else {
                            if (!(value instanceof String) && !value.getClass().isPrimitive() && isImmutable(value)) {
                                field.set(copy, deepClone(value));
                            } else {
                                field.set(copy, value);
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to copy field: " + field.getName(), e);
                    }
                });

    }

    @SuppressWarnings("unchecked")
    public static <E> Collection<E> copyCollection(Collection<? extends E> original) {
        if (isImmutableCollection(original)) {
            return (Collection<E>) original;
        }
        try {
            var collectionType = (Class<? extends Collection<E>>) original.getClass();
            var constructor = collectionType.getDeclaredConstructor();
            constructor.setAccessible(true);
            var copy = constructor.newInstance();

            for (E element : original) {
                if (element != null && isImmutable(element)) {
                    element = deepClone(element);
                }
                copy.add(element);
            }

            return copy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy collection", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> copyMap(Map<? extends K, ? extends V> original) {
        if (isImmutableMap(original)) {
            return (Map<K, V>) original;
        }
        try {
            var mapType = (Class<? extends Map<K, V>>) original.getClass();
            var constructor = mapType.getDeclaredConstructor();
            constructor.setAccessible(true);
            var copy = constructor.newInstance();

            for (var entry : original.entrySet()) {
                K key = entry.getKey();
                V value = entry.getValue();

                if (key != null && isImmutable(key)) {
                    key = deepClone(key);
                }
                if (value != null && isImmutable(value)) {
                    value = deepClone(value);
                }

                copy.put(key, value);
            }

            return copy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy map", e);
        }
    }

    private static boolean isImmutable(Object obj) {
        return !(obj instanceof String) &&
                !(obj instanceof Number) &&
                !(obj instanceof Boolean) &&
                !(obj instanceof Character);
    }

    private static boolean isImmutableCollection(Collection<?> collection) {
        return collection.getClass().getName().startsWith("java.util.ImmutableCollections$");
    }

    private static boolean isImmutableMap(Map<?, ?> map) {
        return map.getClass().getName().startsWith("java.util.ImmutableCollections$");
    }
}