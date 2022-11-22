package me.zort.commons.resolverframework.lifecycle;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import lombok.Getter;
import me.zort.commons.resolverframework.ResolverService;
import me.zort.commons.resolverframework.exceptions.ConfigurerException;
import me.zort.commons.resolverframework.exceptions.ServiceException;
import me.zort.commons.resolverframework.type.Construct;
import me.zort.commons.resolverframework.type.Value;
import me.zort.commons.resolverframework.type.configuration.Path;
import me.zort.commons.resolverframework.type.configuration.SourceAccessMoment;
import me.zort.commons.resolverframework.type.configuration.SourceType;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResolverConfigurer_Dirs extends ResolverConfigurer {

    @Getter
    private final ResolverClass resolverClass;

    /**
     *  Generika
     */
    @SuppressWarnings("unchecked")
    public ResolverConfigurer_Dirs(ResolverService service, Class<?> clazz) throws ConfigurerException {
        super(service, clazz);
        List<Class<? extends Annotation>> addonChecker = Lists.newArrayList(Path.class);
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
    }

    @Override
    public boolean configure() {
        Map<Class<? extends Annotation>, List<ResolverMethod>> methods = resolverClass.getResolverMethods();
        List<ResolverMethod> dataMethods = methods.getOrDefault(Path.class, Collections.emptyList());
        AtomicBoolean res = new AtomicBoolean(true);
        dataMethods.forEach(resolverMethod -> {
            if(res.get()) {
                Path pathAnnot = resolverMethod.getMethod().getDeclaredAnnotation(Path.class);
                String path = pathAnnot.value();
                String pluginLinked = getService().getBukkitPluginLinked();
                File file = new File((pathAnnot.bukkit() && pluginLinked != null && org.bukkit.Bukkit.getPluginManager().getPlugin(pluginLinked) != null
                        ? org.bukkit.Bukkit.getPluginManager().getPlugin(pluginLinked).getDataFolder().getAbsolutePath()
                        : System.getProperty("user.dir")
                ) + path);
                boolean created = false;
                try {
                    if(!file.exists()) {
                        Files.createParentDirs(file);
                        if(pathAnnot.type().equals(SourceType.DIRECTORY)) {
                            file.mkdir();
                        } else if(pathAnnot.type().equals(SourceType.FILE)) {
                            file.createNewFile();
                        }
                        created = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    res.set(false);
                }
                if(pathAnnot.moment().equals(SourceAccessMoment.EVERY) || (pathAnnot.moment().equals(SourceAccessMoment.CREATION) && created) || (pathAnnot.moment().equals(SourceAccessMoment.ACCESS) && !created)) {
                    SourceAccessMoment sourceAccessMoment = created ? SourceAccessMoment.CREATION : SourceAccessMoment.ACCESS;
                    resolverMethod.invokeCustom(file, sourceAccessMoment);
                }
            }
        });
        return res.get();
    }

    @Override
    public boolean postConfigure() {
        resolverClass.invokeFields(Value.class);
        return true;
    }

    @Override
    public void deconfigure() {}

}
