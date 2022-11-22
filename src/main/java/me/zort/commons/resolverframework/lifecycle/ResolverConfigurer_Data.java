package me.zort.commons.resolverframework.lifecycle;

import com.google.common.base.Defaults;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import me.zort.commons.common.Pair;
import me.zort.commons.resolverframework.Resolver;
import me.zort.commons.resolverframework.ResolverService;
import me.zort.commons.resolverframework.exceptions.ConfigurerException;
import me.zort.commons.resolverframework.exceptions.ServiceException;
import me.zort.commons.resolverframework.type.Construct;
import me.zort.commons.resolverframework.type.Value;
import me.zort.commons.resolverframework.type.configuration.DataInit;
import org.apache.commons.lang.RandomStringUtils;

import java.lang.annotation.Annotation;
import java.util.*;

public class ResolverConfigurer_Data extends ResolverConfigurer {

    @Getter
    private final ResolverClass resolverClass;
    @Getter
    private final Map<String, Object> data;

    /**
     *  Generika
     */
    @SuppressWarnings("unchecked")
    public ResolverConfigurer_Data(ResolverService service, Class<?> clazz) throws ConfigurerException {
        super(service, clazz);
        List<Class<? extends Annotation>> addonChecker = Lists.newArrayList(DataInit.class);
        ResolverClass resolverClass;
        try {
            resolverClass = new ResolverClass(service, clazz, addonChecker, true);
        } catch (ServiceException e) {
            /**
             *  Nikdy se nevyvola, jelikoz konstruktor ignoruje zakladovou anotaci.
             */
            resolverClass = null;
        }
        this.resolverClass = resolverClass;
        this.resolverClass.prepare();
        this.resolverClass.instantinate();
        this.resolverClass.invokeMethods(Construct.class);
        this.data = Maps.newConcurrentMap();
    }

    @Override
    public boolean configure() {
        Map<Class<? extends Annotation>, List<ResolverMethod>> methods = resolverClass.getResolverMethods();
        List<ResolverMethod> dataMethods = methods.getOrDefault(DataInit.class, Collections.emptyList());
        Random random = Resolver.RANDOM;
        dataMethods.forEach(method -> {
            DataInit dataInitAnnot = method.getMethod().getDeclaredAnnotation(DataInit.class);
            int chanceValue = random.nextInt(100);
            if(chanceValue < dataInitAnnot.chance()) {
                Optional<Object> objOptional = method.invoke();
                if(objOptional.isPresent()) {
                    Object obj = objOptional.get();
                    String key = obj instanceof Pair && ((Pair<?, ?>) obj).getKey() instanceof String
                            ? (String) ((Pair) obj).getKey()
                            : RandomStringUtils.randomAlphabetic(8);
                    Object value = obj instanceof Pair
                            ? ((Pair<?, ?>) obj).getValue()
                            : Defaults.defaultValue(obj.getClass());
                    data.put(key, value);
                }
            }
        });
        return true;
    }

    @Override
    public boolean postConfigure() {
        resolverClass.invokeFields(Value.class);
        return true;
    }

    @Override
    public void deconfigure() {
        data.clear();
    }

}
