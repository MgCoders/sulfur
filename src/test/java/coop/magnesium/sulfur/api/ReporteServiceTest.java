package coop.magnesium.sulfur.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import coop.magnesium.sulfur.api.dto.HoraCompletaReporte1;
import coop.magnesium.sulfur.api.dto.HorasProyectoXCargo;
import coop.magnesium.sulfur.api.dto.ReporteHoras1;
import coop.magnesium.sulfur.api.dto.ReporteHoras2;
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
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Created by rsperoni on 22/11/17.
 */
@RunWith(Arquillian.class)
public class ReporteServiceTest {

    /**
     * El mapper de jackson para LocalTime y LocalDate se registra via CDI.
     * Acá en arquillian hay que hacerlo a mano.
     */
    final ObjectMapper objectMapper = new ObjectMapper();

    final Proyecto proyecto1 = new Proyecto("P1", "P1");
    final Proyecto proyecto2 = new Proyecto("P2", "P2");
    final TipoTarea tipoTarea1 = new TipoTarea("T1", "T1");
    final TipoTarea tipoTarea2 = new TipoTarea("T2", "T2");
    final Cargo cargo1 = new Cargo("C1", "C1", new BigDecimal(40));
    final Cargo cargo2 = new Cargo("C2", "C2", new BigDecimal(33.2));
    final Cargo cargo3 = new Cargo("C3", "C3", new BigDecimal(50));

    final Colaborador colaborador_admin = new Colaborador("em", "nom", cargo1, "pwd", "ADMIN");
    final Colaborador colaborador_user = new Colaborador("em1", "nom", cargo2, "pwd", "USER");

    @Inject
    CargoDao cargoDao;
    @Inject
    ProyectoDao proyectoDao;
    @Inject
    ColaboradorDao colaboradorDao;
    @Inject
    TipoTareaDao tipoTareaDao;
    @Inject
    EstimacionDao estimacionDao;
    @Inject
    HoraDao horaDao;
    @Inject
    ReportesDao reporteDao;
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
                .addClass(ReportesService.class)
                .addClass(EstimacionTipoTarea.class)
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
    }

    @Test
    @InSequence(1)
    public void inicializarBd() {
        Proyecto proyecto1 = proyectoDao.save(this.proyecto1);
        Proyecto proyecto2 = proyectoDao.save(this.proyecto2);
        TipoTarea tipoTarea1 = tipoTareaDao.save(this.tipoTarea1);
        TipoTarea tipoTarea2 = tipoTareaDao.save(this.tipoTarea2);
        Cargo cargo1 = cargoDao.save(this.cargo1);
        Cargo cargo2 = cargoDao.save(this.cargo2);
        Cargo cargo3 = cargoDao.save(this.cargo3);
        this.colaborador_admin.setCargo(cargo1);
        this.colaborador_user.setCargo(cargo2);
        Colaborador colaborador1 = colaboradorDao.save(this.colaborador_admin);
        Colaborador colaborador2 = colaboradorDao.save(this.colaborador_user);

        EstimacionCargo estimacionCargo = new EstimacionCargo(cargo1, new BigDecimal(150));
        Estimacion estimacion = new Estimacion(proyecto1, null, LocalDate.now());
        estimacion.getEstimacionCargos().add(estimacionCargo);
        estimacionCargo.getEstimacionTipoTareas().add(new EstimacionTipoTarea(tipoTarea1, Duration.ofHours(3)));
        estimacionCargo.getEstimacionTipoTareas().add(new EstimacionTipoTarea(tipoTarea2, Duration.ofHours(6)));
        estimacionDao.save(estimacion);

        EstimacionCargo estimacionCargo2 = new EstimacionCargo(cargo1, new BigDecimal(150));
        Estimacion estimacion2 = new Estimacion(proyecto1, null, LocalDate.now().plusDays(1));
        estimacion2.getEstimacionCargos().add(estimacionCargo2);
        estimacionCargo2.getEstimacionTipoTareas().add(new EstimacionTipoTarea(tipoTarea1, Duration.ofHours(1).plusMinutes(30)));
        estimacionCargo2.getEstimacionTipoTareas().add(new EstimacionTipoTarea(tipoTarea2, Duration.ofHours(1).plusMinutes(30)));
        estimacionDao.save(estimacion2);

        Hora hora = new Hora(LocalDate.now(), LocalTime.MIN, LocalTime.MAX, colaborador1);
        hora.getHoraDetalleList().add(new HoraDetalle(proyecto1, tipoTarea1, Duration.ofHours(20), colaborador1.getCargo()));
        horaDao.save(hora);

        Hora hora2 = new Hora(LocalDate.now(), LocalTime.MIN, LocalTime.MAX, colaborador2);
        hora2.getHoraDetalleList().add(new HoraDetalle(proyecto1, tipoTarea1, Duration.ofHours(15), colaborador2.getCargo()));
        horaDao.save(hora2);

        Hora hora3 = new Hora(LocalDate.now().plusDays(1), LocalTime.MIN, LocalTime.MAX, colaborador2);
        hora3.getHoraDetalleList().add(new HoraDetalle(proyecto1, tipoTarea1, Duration.ofHours(10), colaborador2.getCargo()));
        horaDao.save(hora3);

        Hora hora4 = new Hora(LocalDate.now().plusDays(1), LocalTime.MIN, LocalTime.MAX, colaborador1);
        hora4.getHoraDetalleList().add(new HoraDetalle(proyecto1, tipoTarea1, Duration.ofHours(5), colaborador1.getCargo()));
        horaDao.save(hora4);

        Hora hora5 = new Hora(LocalDate.now().plusDays(1), LocalTime.MIN, LocalTime.MAX, colaborador1);
        hora5.getHoraDetalleList().add(new HoraDetalle(proyecto1, tipoTarea2, Duration.ofHours(20), colaborador1.getCargo()));
        horaDao.save(hora5);

        Hora hora6 = new Hora(LocalDate.now().plusDays(1), LocalTime.MIN, LocalTime.MAX, colaborador2);
        hora6.getHoraDetalleList().add(new HoraDetalle(proyecto1, tipoTarea2, Duration.ofHours(10).plusMinutes(10), colaborador2.getCargo()));
        horaDao.save(hora6);

        Hora hora7 = new Hora(LocalDate.now().plusDays(1), LocalTime.MIN, LocalTime.MAX, colaborador1);
        hora7.getHoraDetalleList().add(new HoraDetalle(proyecto2, tipoTarea2, Duration.ofHours(20), colaborador1.getCargo()));
        horaDao.save(hora7);

        Hora hora8 = new Hora(LocalDate.now().plusDays(1), LocalTime.MIN, LocalTime.MAX, colaborador2);
        hora8.getHoraDetalleList().add(new HoraDetalle(proyecto2, tipoTarea2, Duration.ofHours(10).plusMinutes(10), colaborador2.getCargo()));
        horaDao.save(hora8);

        //Cargo 1, P1, T1, 40h
        //Cargo 2, P1, T1, 20h

        //Cargo 1, P1, T2, 20h
        //Cargo 2, P1, T2, 10h

        //Cargo 1 45h
        //Cargo 2 45:10h

    }


    @Test
    @InSequence(7)
    public void horasCompletas() {
        List<HoraCompletaReporte1> horaCompletaReporte1s = horaDao.findHorasByFechasXCargo(LocalDate.of(2018, 01, 01), LocalDate.of(2019, 01, 01));
        horaCompletaReporte1s.forEach(horaCompletaReporte1 -> logger.info(horaCompletaReporte1.toString()));


    }


    @Test
    @InSequence(7)
    @RunAsClient
    public void getReporte1V1(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/reportes/horas/proyecto/1/tarea/1")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:2")
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ReporteHoras1> horaList = response.readEntity(new GenericType<List<ReporteHoras1>>() {
        });
        assertEquals(4, horaList.size());
        horaList.forEach(reporteHoras1 -> {
            //Fila total
            if (reporteHoras1.cargo == null) {
                assertEquals(new BigDecimal(50).setScale(2, RoundingMode.HALF_DOWN), reporteHoras1.cantidadHoras);

            }
        });

    }

    @Test
    @InSequence(8)
    @RunAsClient
    public void getReporte1V2(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/reportes/horas/proyecto/1/tarea/2")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:2")
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ReporteHoras1> horaList = response.readEntity(new GenericType<List<ReporteHoras1>>() {
        });
        assertEquals(4, horaList.size());
        horaList.forEach(reporteHoras1 -> {
            //Fila total
            if (reporteHoras1.cargo == null) {
                assertEquals(new BigDecimal(30.17).setScale(2, RoundingMode.HALF_DOWN), reporteHoras1.cantidadHoras);

            }
        });

    }

    @Test
    @InSequence(9)
    @RunAsClient
    public void getReporte1Totales(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/reportes/horas/proyecto/1")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:2")
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ReporteHoras1> horaList = response.readEntity(new GenericType<List<ReporteHoras1>>() {
        });
        assertEquals(4, horaList.size());
        horaList.forEach(reporteHoras1 -> {
            //Fila total
            if (reporteHoras1.cargo == null) {
                assertEquals(new BigDecimal(80.17).setScale(2, RoundingMode.HALF_DOWN), reporteHoras1.cantidadHoras);

            }
        });

    }


    @Test
    @InSequence(10)
    @RunAsClient
    public void getReporteFechas(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/reportes/horas/fechas/01-01-2018/01-01-2019/")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:2")
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ReporteHoras1> horaList = response.readEntity(new GenericType<List<ReporteHoras1>>() {
        });
        assertEquals(4, horaList.size());
        horaList.forEach(reporteHoras1 -> {
            //Fila total
            if (reporteHoras1.cargo == null) {
                assertEquals(new BigDecimal(110.34).setScale(2, RoundingMode.HALF_DOWN), reporteHoras1.cantidadHoras);

            }
        });

    }

    @Test
    @InSequence(11)
    @RunAsClient
    public void getReporteFechasProyecto(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/reportes/horas/fechas/01-01-2018/01-01-2019/proyecto/1")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:2")
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ReporteHoras1> horaList = response.readEntity(new GenericType<List<ReporteHoras1>>() {
        });
        assertEquals(4, horaList.size());
        horaList.forEach(reporteHoras1 -> {
            //Fila total
            if (reporteHoras1.cargo == null) {
                assertEquals(new BigDecimal(80.17).setScale(2, RoundingMode.HALF_DOWN), reporteHoras1.cantidadHoras);

            }
        });

    }

    @Test
    @InSequence(12)
    @RunAsClient
    public void getReporteFechasXColaborador(@ArquillianResteasyResource final WebTarget webTarget) {
        final Response response = webTarget
                .path("/reportes/horas/colaboradores/fechas/01-01-2018/01-01-2019")
                .request(MediaType.APPLICATION_JSON)
                .header("AUTHORIZATION", "ADMIN:2")
                .get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<ReporteHoras2> horaList = response.readEntity(new GenericType<List<ReporteHoras2>>() {
        });
        assertEquals(3, horaList.size());
        horaList.forEach(reporteHoras1 -> {
            //Fila total
            if (reporteHoras1.colaborador == null) {
                assertEquals(new BigDecimal(110.33).setScale(2, RoundingMode.HALF_DOWN), reporteHoras1.cantidadHoras);

            }
        });

    }



}
