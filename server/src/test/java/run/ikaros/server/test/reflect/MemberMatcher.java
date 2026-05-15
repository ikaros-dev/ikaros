package run.ikaros.server.test.reflect;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.springframework.util.Assert;

public class MemberMatcher {

    /**
     * get class field.
     *
     * @param clazz class
     * @param name  field name
     * @return a Field instance
     * @throws NoSuchFieldException no found field in appoint class
     */
    public static Field field(Class<?> clazz, String name) throws NoSuchFieldException {
        Assert.notNull(clazz, "'clazz' must not null");
        Assert.hasText(name, "'name' must has text");
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    /**
     * get class final field.
     *
     * @param clazz class
     * @param name  field name
     * @param value new field value
     * @throws NoSuchFieldException no found field in appoint class
     */
    public static void finalField(Class<?> clazz, String name, Object value)
        throws NoSuchFieldException, IllegalAccessException {
        Assert.notNull(clazz, "'clazz' must not null");
        Assert.hasText(name, "'name' must has text");
        Field field = field(clazz, name);
        Field modifiersField = getModifiersField();
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }

    private static Field getModifiersField() throws NoSuchFieldException {
        try {
            return Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException e) {
            try {
                Method
                    getDeclaredFields0 =
                    Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                getDeclaredFields0.setAccessible(true);
                Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
                for (Field field : fields) {
                    if ("modifiers".equals(field.getName())) {
                        return field;
                    }
                }
            } catch (ReflectiveOperationException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

}
