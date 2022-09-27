package nextstep.study.di.stage4.annotations;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

/**
 * 스프링의 BeanFactory, ApplicationContext에 해당되는 클래스
 */
class DIContainer {

    private final Set<Object> beans;

    public DIContainer(final Set<Class<?>> classes)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.beans = new HashSet<>();
        for (final Class<?> aClass : classes) {
            final Constructor<?> declaredConstructor = aClass.getDeclaredConstructor();
            if (declaredConstructor.trySetAccessible()) {
                beans.add(declaredConstructor.newInstance());
            }
        }
        for (final Object bean : beans) {
            final Set<Field> fields = ReflectionUtils.getFields(bean.getClass(),
                    ReflectionUtils.withAnnotation(Inject.class));
            fields.stream()
                    .filter(AccessibleObject::trySetAccessible)
                    .forEach(it -> {
                        try {
                            it.set(bean, getBean(it.getType()));
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    public static DIContainer createContainerForPackage(final String rootPackageName)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final Reflections reflections = new Reflections(rootPackageName);
        final Set<Class<?>> classes = new HashSet<>();
        classes.addAll(reflections.getTypesAnnotatedWith(Service.class));
        classes.addAll(reflections.getTypesAnnotatedWith(Repository.class));
        return new DIContainer(classes);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(final Class<T> aClass) {
        return beans.stream()
                .filter(aClass::isInstance)
                .map(it -> (T) it)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("등록된 빈이 없습니다."));
    }
}
