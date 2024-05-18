package deep.clone;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;

public final class CopyUtils {
    public static <T> T deepClone(T original) throws Exception {
        var clazz = original.getClass();
        @SuppressWarnings("unchecked")
        var copy = (T) createInstanceWithDefaults(clazz);

        return copy;
    }

    public static Object createInstanceWithDefaults(Class<?> clazz) throws Exception {
        Constructor<?> constructor = getConstructorForCopy(clazz);
        constructor.setAccessible(true);
        Object[] parameters = getDefaultParameters(constructor);
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

}
