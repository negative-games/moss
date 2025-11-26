package games.negative.moss.spring;

import org.springframework.context.support.GenericApplicationContext;

public interface Loadable {

    /**
     * Called when the application context is being loaded.
     * @param context The application context.
     */
    void onLoad(GenericApplicationContext context);

}
