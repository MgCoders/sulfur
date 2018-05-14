package coop.magnesium.sulfur.api;

import coop.magnesium.sulfur.api.utils.MagnesiumStatus;
import coop.magnesium.sulfur.db.dao.ConfiguracionDao;
import coop.magnesium.sulfur.db.entities.TipoConfiguracion;
import coop.magnesium.sulfur.utils.PropertiesFromFile;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Properties;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by rsperoni on 14/09/17.
 */
@Api(description = "System Status", tags = "status")
@Path("/status")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class Status {

    @Inject
    ConfiguracionDao configuracionDao;

    @Inject
    @PropertiesFromFile
    Properties endpointsProperties;

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    String jbossNodeName;

    //@Inject
    //StartupBean startupBean;


    @GET
    @ApiOperation(value = "Get system status", response = MagnesiumStatus.class)
    public MagnesiumStatus status() {
        return new MagnesiumStatus(configuracionDao.getStringProperty(TipoConfiguracion.PROJECT_NAME), endpointsProperties.getProperty("project.version"), jbossNodeName, configuracionDao.getStringProperty(TipoConfiguracion.FRONTEND_HOST) + "/swagger.json", "https://mgcoders.github.io/sulfur/");
    }

    /*@GET
    @Path("alertas")
    public Response runalertas() {
        startupBean.alertaHorasSinCargar();
        return Response.ok().build();
    }

    @GET
    @Path("mails")
    public Response mails() {
        startupBean.enviarMailsConNotificaciones();
        return Response.ok().build();
    }
    */


}
