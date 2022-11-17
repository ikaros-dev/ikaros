package run.ikaros.server.unittest;

import java.lang.reflect.Field;

public class MemberMatcher {

    public static Field field(Class<?> declaringClass, String fieldName)
        throws NoSuchFieldException {
        Field declaredField = declaringClass.getDeclaredField(fieldName);
        declaredField.setAccessible(true);
        return declaredField;
    }

}
