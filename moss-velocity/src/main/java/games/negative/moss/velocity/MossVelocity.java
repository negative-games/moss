package games.negative.moss.velocity;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import games.negative.moss.spring.Disableable;
import games.negative.moss.spring.Enableable;
import games.negative.moss.spring.Loadable;
import games.negative.moss.velocity.spring.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public abstract class MossVelocity {

    public static AnnotationConfigApplicationContext CONTEXT;

    private final ProxyServer server;

    public MossVelocity(ProxyServer server) {
        this.server = server;
        load();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        enable();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        disable();
    }

    /**
     * Reload the plugin by disabling, loading, and enabling it again.
     */
    public void reload() {
        disable();
        load();
        enable();
    }

    /**
     * Disable the plugin, unregistering listeners and cancelling scheduled tasks.
     */
    private void disable() {
        disableComponents();

        if (CONTEXT != null) {
            CONTEXT.close();
            CONTEXT = null;
        }
    }

    /**
     * Enable the plugin, registering listeners and enabling components.
     */
    private void enable() {
        enableComponents();

        // Register listeners
        EventManager events = server.getEventManager();
        invokeBeans(Listener.class, listener -> events.register(this, listener), (listener, e) -> {
            log.error("Failed to register listener {}", listener.getClass().getSimpleName(), e);
        });
    }

    /**
     * Load the Spring application context and initialize components.
     */
    private void load() {
        CONTEXT = new AnnotationConfigApplicationContext();

        CONTEXT.setClassLoader(getClass().getClassLoader());

        loadInitialComponents(CONTEXT);

        CONTEXT.scan(basePackage());

        CONTEXT.refresh();

        invokeBeans(Loadable.class, loadable -> loadable.onLoad(CONTEXT), (loadable, e) -> {
            log.error("Failed to load {}", loadable.getClass().getSimpleName(), e);
        });
    }

    /**
     * Load initial components into the Spring application context.
     * @param context the Spring application context
     */
    public void loadInitialComponents(AnnotationConfigApplicationContext context) {
        context.registerBean(MossVelocity.class, () -> this);
        context.registerBean(ProxyServer.class, () -> this.server);
    }

    /**
     * Enable all components that implement the Enableable interface.
     */
    public void enableComponents() {
        // Register enableables
        invokeBeans(Enableable.class, Enableable::onEnable, (enableable, e) -> {
            log.error("An error occurred while enabling {}", enableable.getClass().getSimpleName(), e);
        });
    }

    /**
     * Disable all components that implement the Disableable interface, unregister listeners, and cancel scheduled tasks.
     */
    public void disableComponents() {
        invokeBeans(Disableable.class, Disableable::onDisable, (disableable, e) -> {
            log.error("An error occurred while disabling {}", disableable.getClass().getSimpleName(), e);
        });

        // Disable listeners
        server.getEventManager().unregisterListeners(this);

        // Cancel scheduled tasks
        Collection<ScheduledTask> tasks = server.getScheduler().tasksByPlugin(this);
        tasks.forEach(ScheduledTask::cancel);
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

    public String basePackage() {
        return this.getClass().getPackageName();
    }
}
