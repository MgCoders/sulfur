package coop.magnesium.sulfur.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import coop.magnesium.sulfur.api.dto.*;
import coop.magnesium.sulfur.api.utils.JWTTokenNeeded;
import coop.magnesium.sulfur.api.utils.RoleNeeded;
import coop.magnesium.sulfur.db.dao.*;
import coop.magnesium.sulfur.db.entities.*;
import coop.magnesium.sulfur.utils.Logged;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Created by rsperoni on 22/11/17.
 */
@RunWith(Arquillian.class)
public class NotificacionServiceTest {

    /**
     * El mapper de jackson para LocalTime y LocalDate se registra via CDI.
     * Acá en arquillian hay que hacerlo a mano.
     */
    final ObjectMapper objectMapper = new ObjectMapper();

    final Cargo cargo1 = new Cargo("C1", "C1", new BigDecimal(40));
    final Cargo cargo2 = new Cargo("C2", "C2", new BigDecimal(33.2));

    final Colaborador colaborador_admin = new Colaborador("em", "nom", cargo1, "pwd", "ADMIN");
    final Colaborador colaborador_user = new Colaborador("em1", "nom", cargo2, "pwd", "USER");

    @Inject
    CargoDao cargoDao;
    @Inject
    ColaboradorDao colaboradorDao;
    @Inject
    NotificacionDao notificacionDao;
    @Inject
    Logger logger;

    @Deployment(testable = true)


    public static WebArchive createDeployment() {
        File[] libs = Maven.resolver()
                .loadPomFromFile("pom.xml").resolve("com.fasterxml.jackson.datatype:jackson-datatype-jsr310").withTransitivity().asFile();
        return ShrinkWrap.create(WebArchive.class)
                .addPackages(true, Filters.exclude(".*Test.*"),
                        Notificacion.class.getPackage(),
                        NotificacionDao.class.getPackage(),
                        ReporteHoras1.class.getPackage(),
                        Logged.class.getPackage())
                .addClass(JAXRSConfiguration.class)
                .addClass(JWTTokenNeeded.class)
                .addClass(RoleNeeded.class)
                .addClass(JWTTokenNeededFilterMock.class)
                .addClass(RoleNeededFilterMock.class)
                .addClass(NotificacionService.class)
                .addClass(UserServiceMock.class)
                .addClass(TipoNotificacion.class)
                .addAsResource("META-INF/persistence.xml")
                .addAsResource("endpoints.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsLibraries(libs);
    }

    @Before
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    @InSequence(1)
    public void inicializarBd() {
        Cargo cargo1 = cargoDao.save(this.cargo1);
        Cargo cargo2 = cargoDao.save(this.cargo2);
        this.colaborador_admin.setCargo(cargo1);
        this.colaborador_user.setCargo(cargo2);
        Colaborador colaborador1 = colaboradorDao.save(this.colaborador_admin);
        Colaborador colaborador2 = colaboradorDao.save(this.colaborador_user);

        Notificacion notificacion1 = new Notificacion();
        notificacion1.setColaborador(colaborador1);
        notificacion1.setFechaHora(LocalDateTime.now());
        notificacion1.setTexto("hola");
        notificacion1.setTipo(TipoNotificacion.LOGIN);

        Notificacion notificacion2 = new Notificacion();
        notificacion2.setColaborador(colaborador2);
        notificacion2.setFechaHora(LocalDateTime.now());
        notificacion2.setTexto("chau");
        notificacion2.setTipo(TipoNotificacion.LOGIN);

        notificacion1 = notificacionDao.save(notificacion1);
        notificacion2 = notificacionDao.save(notificacion2);


    }










    @Test
    @InSequence(2)
    @RunAsClient
    public void getNotificacionUser1(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/notificaciones/colaborador/1/01-01-2018/01-01-2019/")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "USER:1")
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<Notificacion> notificacions = response.readEntity(new GenericType<List<Notificacion>>() {
        });
        assertEquals(1, notificacions.size());
    }

    @Test
    @InSequence(3)
    @RunAsClient
    public void getNotificacionUser2(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/notificaciones/colaborador/2/01-01-2018/01-01-2019/")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "USER:2")
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<Notificacion> notificacions = response.readEntity(new GenericType<List<Notificacion>>() {
        });
        assertEquals(1, notificacions.size());
    }

    @Test
    @InSequence(4)
    @RunAsClient
    public void getAll(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/notificaciones/")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:2")
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<Notificacion> notificacions = response.readEntity(new GenericType<List<Notificacion>>() {
        });
        assertEquals(2, notificacions.size());
    }





}
