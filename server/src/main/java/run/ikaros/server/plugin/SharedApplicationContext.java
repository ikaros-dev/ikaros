package run.ikaros.server.plugin;

import org.springframework.context.support.GenericApplicationContext;

/**
 * all plugin application context parent, can contain some bean that can be accessed by plugin.
 *
 * @author li-guohao
 */
public class SharedApplicationContext extends GenericApplicationContext {
}
