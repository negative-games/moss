package games.negative.moss.paper;

import games.negative.moss.spring.Disableable;
import games.negative.moss.spring.Enableable;
import games.negative.moss.spring.Loadable;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public abstract class MossPaper extends JavaPlugin {

    public static AnnotationConfigApplicationContext CONTEXT;

    @Override
    public void onLoad() {

        CONTEXT = new AnnotationConfigApplicationContext();

        CONTEXT.setClassLoader(getClass().getClassLoader());

        loadInitialComponents(CONTEXT);

        CONTEXT.scan(basePackage());

        CONTEXT.refresh();
        CONTEXT.start();

        invokeBeans(Loadable.class, loadable -> loadable.onLoad(CONTEXT), (loadable, e) -> {
            log.error("Failed to load {}", loadable.getClass().getSimpleName(), e);
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
            log.error("Failed to enable {}", enableable.getClass().getSimpleName(), e);
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
        // Invoke disableables
        invokeBeans(Disableable.class, Disableable::onDisable, (disableable, e) -> {
            log.error("Failed to disable {}", disableable.getClass().getSimpleName(), e);
        });

        // Unregister listeners
        HandlerList.unregisterAll(this);

        // Cancel scheduled tasks
        Bukkit.getScheduler().cancelTasks(this);
    }

    /**
     * Reload the plugin by disabling, loading, and enabling it again.
     */
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
