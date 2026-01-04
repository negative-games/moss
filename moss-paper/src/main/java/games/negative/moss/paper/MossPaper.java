package games.negative.moss.paper;

import games.negative.moss.spring.Disableable;
import games.negative.moss.spring.Enableable;
import games.negative.moss.spring.Loadable;
import games.negative.moss.spring.Reloadable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class MossPaper extends JavaPlugin {

    private Logger logger;
    protected static AnnotationConfigApplicationContext CONTEXT;

    @Override
    public void onLoad() {
        logger = getLogger();

        CONTEXT = new AnnotationConfigApplicationContext();

        CONTEXT.setClassLoader(getClass().getClassLoader());

        loadInitialComponents(CONTEXT);

        CONTEXT.scan(basePackage());

        CONTEXT.refresh();

        invokeBeans(Loadable.class, loadable -> loadable.onLoad(CONTEXT), (loadable, e) -> {
            logger.severe("Failed to load " + loadable.getClass().getSimpleName());
            logger.severe(e.getMessage());
        });
    }

    public void loadInitialComponents(AnnotationConfigApplicationContext context) {
        context.registerBean(JavaPlugin.class, () -> this);
    }

    @Override
    public void onEnable() {
        enableComponents();
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

        if (CONTEXT != null) {
            CONTEXT.close();
            CONTEXT = null;
        }
    }

    public void disableComponents() {
        invokeBeans(Disableable.class, Disableable::onDisable, (disableable, e) -> {
            logger.severe("Failed to disable " + disableable.getClass().getSimpleName());
            logger.severe(e.getMessage());
        });
    }


    public void reload() {
        onDisable();
        onLoad();
        onEnable();
    }

    /**
     * Invoke all beans of a certain class type with a consumer.
     * @param clazz the class type of the beans to invoke
     * @param consumer the consumer to invoke on each bean
     * @param onFailure the failure consumer to invoke if an exception occurs
     * @param <T> the type of the beans
     */
    public <T> void invokeBeans(Class<T> clazz, Consumer<T> consumer, BiConsumer<T, Exception> onFailure) {
        Collection<T> beans = CONTEXT.getBeansOfType(clazz).values();
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
