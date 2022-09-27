package nextstep.study.di.stage3.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * 스프링의 BeanFactory, ApplicationContext 에 해당되는 클래스
 */
class DIContainer {

    private final Set<Object> beans;

    public DIContainer(final Set<Class<?>> classes)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        beans = new HashSet<>();
        for (final Class<?> aClass : classes) {
            addInstance(aClass, classes);
        }
    }

    private void addInstance(final Class<?> aClass, final Set<Class<?>> classes)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<?>[] constructors = aClass.getConstructors();
        for (final Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == 0) {
                beans.add(constructor.newInstance());
                return;
            }
            for (final Class<?> parameterType : constructor.getParameterTypes()) {
                final Optional<Class<?>> target = classes.stream().filter(parameterType::isAssignableFrom).findFirst();
                if (target.isPresent()) {
                    addInstance(target.get(), classes);
                    addInstance(constructor, parameterType);
                }
            }
        }
    }

    private void addInstance(final Constructor<?> constructor, final Class<?> parameterType)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        beans.add(constructor.newInstance(beans.stream()
                .filter(parameterType::isInstance)
                .findFirst()
                .get()
        ));
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
