package coop.magnesium.sulfur.utils;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by rsperoni on 27/09/17.
 */
@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LoggedInterceptor implements Serializable {

    @Inject
    private Logger logger;

    public LoggedInterceptor() {
    }

    @AroundInvoke
    public Object logMethodEntry(InvocationContext invocationContext) throws Exception {
        logger.log(Level.INFO, "{0} called with {1} parameters",
                new Object[]{
                        invocationContext.getMethod().getName(),
                        invocationContext.getParameters().length
                }
        );
        return invocationContext.proceed();
    }
}