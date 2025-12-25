package games.negative.moss.velocity;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import games.negative.moss.spring.Disableable;
import games.negative.moss.spring.Enableable;
import games.negative.moss.spring.Loadable;
import games.negative.moss.spring.Reloadable;
import games.negative.moss.velocity.spring.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public abstract class MossVelocity {

    protected AnnotationConfigApplicationContext context;

    private final ProxyServer server;

    public MossVelocity(ProxyServer server) {
        this.server = server;

        context = new AnnotationConfigApplicationContext();

        context.setClassLoader(getClass().getClassLoader());

        loadInitialComponents(context);

        context.scan(basePackage());

        context.refresh();

        invokeBeans(Loadable.class, loadable -> loadable.onLoad(context), (loadable, e) -> {
            log.error("Failed to load {}", loadable.getClass().getSimpleName(), e);
        });
    }

    public void loadInitialComponents(AnnotationConfigApplicationContext context) {
        context.registerBean(MossVelocity.class, () -> this);
        context.registerBean(ProxyServer.class, () -> this.server);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        enableComponents();

        reload();

        // Register listeners
        EventManager events = server.getEventManager();
        invokeBeans(Listener.class, listener -> events.register(this, listener), (listener, e) -> {
            log.error("Failed to register listener {}", listener.getClass().getSimpleName(), e);
        });

    }

    public void enableComponents() {
        // Register enableables
        invokeBeans(Enableable.class, Enableable::onEnable, (enableable, e) -> {
            log.error("An error occurred while enabling {}", enableable.getClass().getSimpleName(), e);
        });
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        disableComponents();

        if (context != null) {
            context.close();
            context = null;
        }
    }

    public void disableComponents() {
        invokeBeans(Disableable.class, Disableable::onDisable, (disableable, e) -> {
            log.error("An error occurred while disabling {}", disableable.getClass().getSimpleName(), e);
        });
    }

    public void reload() {
        invokeBeans(Reloadable.class, Reloadable::onReload, (reloadable, e) -> {
            log.error("Failed to reload {}", reloadable.getClass().getSimpleName(), e);
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

    public String basePackage() {
        return this.getClass().getPackageName();
    }
}
