package coop.magnesium.sulfur.api.utils;

import coop.magnesium.sulfur.db.dao.ConfiguracionDao;
import coop.magnesium.sulfur.db.entities.TipoConfiguracion;
import coop.magnesium.sulfur.utils.PropertiesFromFile;
import io.swagger.jaxrs.config.BeanConfig;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Properties;

/**
 * Configures a JAX-RS endpoint. Delete this class, if you are not exposing
 * JAX-RS resources in your application.
 *
 * @author airhacks.com
 */
@ApplicationPath("/api")
public class JAXRSConfiguration extends Application {

    @Inject
    @PropertiesFromFile
    Properties endpointsProperties;

    @Inject
    ConfiguracionDao configuracionDao;

    /**
     * Add swagger configuraction
     */
    @PostConstruct
    public void init() {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion(endpointsProperties.getProperty("project.version"));
        beanConfig.setSchemes(new String[]{"https"});
        beanConfig.setHost(configuracionDao.getStringProperty(TipoConfiguracion.FRONTEND_HOST));
        beanConfig.setBasePath(configuracionDao.getStringProperty(TipoConfiguracion.FRONTEND_PATH));
        beanConfig.setResourcePackage("coop.magnesium.sulfur.api");
        beanConfig.setDescription(getDescription());
        beanConfig.setTitle(configuracionDao.getStringProperty(TipoConfiguracion.PROJECT_NAME));
        beanConfig.setContact("info@magnesium.coop");
        beanConfig.setScan(true);
        beanConfig.setPrettyPrint(true);
    }

    private String getDescription() {
        return "El login exitoso de UserService devuelve un Colaborador con un campo \"token\".\n" +
                "Dicho token se debe incluir como header de las llamadas del resto de las operaciones de cada servicio.";
    }



}