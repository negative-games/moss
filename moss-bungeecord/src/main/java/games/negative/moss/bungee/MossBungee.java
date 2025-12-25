package games.negative.moss.bungee;

import games.negative.moss.spring.Disableable;
import games.negative.moss.spring.Enableable;
import games.negative.moss.spring.Loadable;
import games.negative.moss.spring.Reloadable;
import net.md_5.bungee.api.plugin.Plugin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class MossBungee extends Plugin {

    private Logger logger;
    protected AnnotationConfigApplicationContext context;

    @Override
    public void onLoad() {
        logger = getLogger();

        context = new AnnotationConfigApplicationContext();

        context.setClassLoader(getClass().getClassLoader());

        loadInitialComponents(context);

        context.scan(basePackage());

        context.refresh();

        invokeBeans(Loadable.class, loadable -> loadable.onLoad(context), (loadable, e) -> {
            logger.severe("Failed to load " + loadable.getClass().getSimpleName());
            logger.severe(e.getMessage());
        });
    }

    public void loadInitialComponents(AnnotationConfigApplicationContext context) {
        context.registerBean(Plugin.class, () -> this);
    }

    @Override
    public void onEnable() {
        enableComponents();

        // Initial reload
        reload();
    }

    private void enableComponents() {
        // Register enableables
        invokeBeans(Enableable.class, Enableable::onEnable, (enableable, e) -> {
            logger.severe("Failed to enable " + enableable.getClass().getSimpleName());
            logger.severe(e.getMessage());
        });
    }

    @Override
    public void onDisable() {
        disableComponents();

        if (context != null) {
            context.close();
            context = null;
        }
    }

    public void disableComponents() {
        invokeBeans(Disableable.class, Disableable::onDisable, (disableable, e) -> {
            logger.severe("Failed to disable " + disableable.getClass().getSimpleName());
            logger.severe(e.getMessage());
        });
    }


    public void reload() {
        invokeBeans(Reloadable.class, Reloadable::onReload, (reloadable, e) -> {
            logger.severe("Failed to reload " + reloadable.getClass().getSimpleName());
            logger.severe(e.getMessage());
        });
    }

    /**
     * Invoke all beans of a certain class type with a consumer.
     * @param clazz the class type of the beans to invoke
     * @param consumer the consumer to invoke on each bean
     * @param onFailure the failure consumer to invoke if an exception occurs
     * @param <T> the type of the beans
     */
    public <T> void invokeBeans(Class<T> clazz, Consumer<T> consumer, BiConsumer<T, Exception> onFailure) {
        Collection<T> beans = context.getBeansOfType(clazz).values();
        for (T bean : beans) {
            try {
                consumer.accept(bean);
            } catch (Exception e) {
                if (onFailure == null) return;

                onFailure.accept(bean, e);
            }
        }
    }

    /**
     * Invoke all beans of a certain class type with a consumer.
     * @param clazz the class type of the beans to invoke
     * @param consumer the consumer to invoke on each bean
     * @param <T> the type of the beans
     */
    public <T> void invokeBeans(Class<T> clazz, Consumer<T> consumer) {
        invokeBeans(clazz, consumer, null);
    }

    private String basePackage() {
        return this.getClass().getPackageName();
    }
}
