package coop.magnesium.sulfur.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import coop.magnesium.sulfur.api.dto.HorasProyectoXCargo;
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
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by rsperoni on 22/11/17.
 */
@RunWith(Arquillian.class)
public class HoraServiceTest {

    /**
     * El mapper de jackson para LocalTime y LocalDate se registra via CDI.
     * Acá en arquillian hay que hacerlo a mano.
     */
    final ObjectMapper objectMapper = new ObjectMapper();

    final Proyecto proyecto = new Proyecto("PP", "PP");
    final TipoTarea tipoTarea = new TipoTarea("TT", "TT");
    final Cargo cargo = new Cargo("CC", "CC", new BigDecimal(32.2));
    final Colaborador colaborador_admin = new Colaborador("em", "nom", cargo, "pwd", "ADMIN");
    final Colaborador colaborador_user = new Colaborador("em1", "nom", cargo, "pwd", "USER");

    @Inject
    CargoDao cargoDao;
    @Inject
    ProyectoDao proyectoDao;
    @Inject
    ColaboradorDao colaboradorDao;
    @Inject
    TipoTareaDao tipoTareaDao;
    @Inject
    Logger logger;

    @Deployment(testable = true)


    public static WebArchive createDeployment() {
        File[] libs = Maven.resolver()
                .loadPomFromFile("pom.xml").resolve("com.fasterxml.jackson.datatype:jackson-datatype-jsr310").withTransitivity().asFile();
        return ShrinkWrap.create(WebArchive.class)
                .addPackages(true, Filters.exclude(".*Test.*"),
                        Hora.class.getPackage(),
                        HoraDao.class.getPackage(),
                        Logged.class.getPackage(),
                        HorasProyectoXCargo.class.getPackage())
                .addClass(JAXRSConfiguration.class)
                .addClass(JWTTokenNeeded.class)
                .addClass(RoleNeeded.class)
                .addClass(JWTTokenNeededFilterMock.class)
                .addClass(RoleNeededFilterMock.class)
                .addClass(HoraService.class)
                .addClass(UserServiceMock.class)
                .addAsResource("META-INF/persistence.xml")
                .addAsResource("endpoints.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsLibraries(libs);
    }

    @Before
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.proyecto.setId(1L);
        this.tipoTarea.setId(1L);
        this.colaborador_admin.setId(1L);
        this.cargo.setId(1L);
        this.colaborador_user.setId(2L);
    }

    @Test
    @InSequence(1)
    public void inicializarBd() {
        logger.info(proyectoDao.save(this.proyecto).toString());
        logger.info(tipoTareaDao.save(this.tipoTarea).toString());
        Cargo cargo = cargoDao.save(this.cargo);
        logger.info(cargo.toString());
        this.colaborador_admin.setCargo(cargo);
        this.colaborador_user.setCargo(cargo);
        logger.info(colaboradorDao.save(this.colaborador_admin).toString());
        logger.info(colaboradorDao.save(this.colaborador_user).toString());
    }


    @Test
    @InSequence(2)
    @RunAsClient
    public void createHoraAdmin(@ArquillianResteasyResource final WebTarget webTarget) throws IOException {
        Hora hora = new Hora(LocalDate.of(2017, 12, 24), LocalTime.MIN, LocalTime.MAX, this.colaborador_admin);
        hora.getHoraDetalleList().add(new HoraDetalle(this.proyecto, this.tipoTarea, Duration.ofHours(23), null));

        System.out.println(objectMapper.writeValueAsString(hora));

        final Response response = webTarget
                .path("/horas")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:1")
                .post(Entity.json(objectMapper.writeValueAsString(hora)));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        String horaCreadaString = response.readEntity(String.class);
        Hora horaCreada = objectMapper.readValue(horaCreadaString, Hora.class);
        assertEquals(1, horaCreada.getId().longValue());
        assertEquals(Duration.parse("PT23H59M"), horaCreada.getSubtotal());
    }

    @Test
    @InSequence(3)
    @RunAsClient
    public void createHoraUserBien(@ArquillianResteasyResource final WebTarget webTarget) throws IOException {
        Hora hora = new Hora(LocalDate.of(2017, 12, 24), LocalTime.MIN, LocalTime.MAX, this.colaborador_user);
        hora.getHoraDetalleList().add(new HoraDetalle(this.proyecto, this.tipoTarea, Duration.ofHours(23), null));

        System.out.println(objectMapper.writeValueAsString(hora) + " " + hora.getHoraDetalleList().size());

        final Response response = webTarget
                .path("/horas")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:1")
                .post(Entity.json(objectMapper.writeValueAsString(hora)));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        String horaCreadaString = response.readEntity(String.class);
        Hora horaCreada = objectMapper.readValue(horaCreadaString, Hora.class);
        assertEquals(2, horaCreada.getId().longValue());
        assertEquals(Duration.parse("PT23H59M"), horaCreada.getSubtotal());
        assertEquals(Duration.parse("PT23H"), horaCreada.getSubtotalDetalles());
        assertEquals(false, horaCreada.isCompleta());
    }

    @Test
    @InSequence(4)
    @RunAsClient
    public void createHoraUserMal(@ArquillianResteasyResource final WebTarget webTarget) throws IOException {
        Hora hora = new Hora(LocalDate.now(), LocalTime.MIN, LocalTime.MAX, this.colaborador_user);
        hora.getHoraDetalleList().add(new HoraDetalle(this.proyecto, this.tipoTarea, Duration.ofHours(23), null));

        System.out.println(objectMapper.writeValueAsString(hora));

        final Response response = webTarget
                .path("/horas")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "USER:5")
                .post(Entity.json(objectMapper.writeValueAsString(hora)));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(5)
    @RunAsClient
    public void createHoraUserMal2(@ArquillianResteasyResource final WebTarget webTarget) throws IOException {
        Hora hora = new Hora(LocalDate.now(), LocalTime.MIN, LocalTime.MAX, this.colaborador_user);
        hora.getHoraDetalleList().add(new HoraDetalle(new Proyecto("EE", "No existe"), this.tipoTarea, Duration.ofHours(23), null));

        System.out.println(objectMapper.writeValueAsString(hora));

        final Response response = webTarget
                .path("/horas")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "USER:2")
                .post(Entity.json(objectMapper.writeValueAsString(hora)));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }


    @Test
    @InSequence(6)
    @RunAsClient
    public void getHorasAdminBien(@ArquillianResteasyResource final WebTarget webTarget) {


        final Response response = webTarget
                .path("/horas/01-01-2011/01-01-2018")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:1")
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<Hora> horaList = response.readEntity(new GenericType<List<Hora>>() {
        });
        assertEquals(2, horaList.size());
    }

    @Test
    @InSequence(7)
    @RunAsClient
    public void getHorasUserMal(@ArquillianResteasyResource final WebTarget webTarget) {


        final Response response = webTarget
                .path("/horas/01-01-2011/01-01-2018")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "USER:1")
                .get();

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(8)
    @RunAsClient
    public void getHorasUserMal2(@ArquillianResteasyResource final WebTarget webTarget) {


        final Response response = webTarget
                .path("/horas/user/2/01-01-2011/01-01-2025")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "USER:1")
                .get();

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    @InSequence(9)
    @RunAsClient
    public void getHorasUserBien(@ArquillianResteasyResource final WebTarget webTarget) {


        final Response response = webTarget
                .path("/horas/user/2/01-01-2011/01-01-2018")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "USER:2")
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<Hora> horaList = response.readEntity(new GenericType<List<Hora>>() {
        });
        assertEquals(1, horaList.size());
    }

    @Test
    @InSequence(10)
    @RunAsClient
    public void getHorasUserBien2(@ArquillianResteasyResource final WebTarget webTarget) {


        final Response response = webTarget
                .path("/horas/user/2/01-01-2011/01-01-2018")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:1")
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<Hora> horaList = response.readEntity(new GenericType<List<Hora>>() {
        });
        assertEquals(1, horaList.size());
    }

    @Test
    @InSequence(11)
    @RunAsClient
    public void editHoraUser(@ArquillianResteasyResource final WebTarget webTarget) throws IOException {
        Hora hora = new Hora(LocalDate.of(2017, 12, 24), LocalTime.MIN, LocalTime.MAX, this.colaborador_user);
        hora.getHoraDetalleList().add(new HoraDetalle(this.proyecto, this.tipoTarea, Duration.ofHours(20), null));
        hora.getHoraDetalleList().add(new HoraDetalle(this.proyecto, this.tipoTarea, Duration.ofHours(3), null));
        hora.getHoraDetalleList().add(new HoraDetalle(this.proyecto, this.tipoTarea, Duration.ofMinutes(59), null));



        System.out.println(objectMapper.writeValueAsString(hora));

        final Response response = webTarget
                .path("/horas/1")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:1")
                .put(Entity.json(objectMapper.writeValueAsString(hora)));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String horaCreadaString = response.readEntity(String.class);
        System.out.println(horaCreadaString);
        Hora horaCreada = objectMapper.readValue(horaCreadaString, Hora.class);
        assertEquals(1, horaCreada.getId().longValue());
        assertEquals(23, horaCreada.getSubtotal().toHours());
        assertEquals(horaCreada.getSubtotalDetalles(), horaCreada.getSubtotal());
        assertEquals(true, horaCreada.isCompleta());
    }

    @Test
    @InSequence(12)
    @RunAsClient
    public void editHoraUserIncompleta(@ArquillianResteasyResource final WebTarget webTarget) throws IOException {
        Hora hora = new Hora(LocalDate.of(2017, 12, 24), LocalTime.MIN, LocalTime.MAX, this.colaborador_user);
        hora.getHoraDetalleList().add(new HoraDetalle(this.proyecto, this.tipoTarea, Duration.ofHours(20), null));
        hora.getHoraDetalleList().add(new HoraDetalle(this.proyecto, this.tipoTarea, Duration.ofHours(2), null));


        System.out.println(objectMapper.writeValueAsString(hora));

        final Response response = webTarget
                .path("/horas/1")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:2")
                .put(Entity.json(objectMapper.writeValueAsString(hora)));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String horaCreadaString = response.readEntity(String.class);
        System.out.println(horaCreadaString);
        Hora horaCreada = objectMapper.readValue(horaCreadaString, Hora.class);
        assertEquals(1, horaCreada.getId().longValue());
        assertEquals(2, horaCreada.getHoraDetalleList().size());
        assertNotEquals(horaCreada.getSubtotalDetalles(), horaCreada.getSubtotal());
        assertEquals(false, horaCreada.isCompleta());
    }

    @Test
    @InSequence(13)
    @RunAsClient
    public void borrarHoras(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/horas/01-01-2011/01-01-2018")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:1")
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<Hora> horaList = response.readEntity(new GenericType<List<Hora>>() {
        });
        assertEquals(2, horaList.size());

        horaList.forEach(hora -> {
            final Response res = webTarget
                    .path("/horas/" + hora.getId())
                    .request(MediaType.APPLICATION_JSON)
                    .header("AUTHORIZATION", "ADMIN:1")
                    .delete();
        });

        final Response finalResponse = webTarget
                .path("/horas/01-01-2011/01-01-2018")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:1")
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<Hora> horaListFinal = finalResponse.readEntity(new GenericType<List<Hora>>() {
        });
        assertEquals(0, horaListFinal.size());

    }

}
