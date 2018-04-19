package coop.magnesium.sulfur.api;


import coop.magnesium.sulfur.api.dto.EstimacionProyectoTipoTareaXCargo;
import coop.magnesium.sulfur.api.dto.ReporteHoras1;
import coop.magnesium.sulfur.api.dto.ReporteHoras2;
import coop.magnesium.sulfur.api.utils.JWTTokenNeeded;
import coop.magnesium.sulfur.api.utils.RoleNeeded;
import coop.magnesium.sulfur.db.dao.*;
import coop.magnesium.sulfur.db.entities.*;
import coop.magnesium.sulfur.utils.Logged;
import coop.magnesium.sulfur.utils.ex.MagnesiumNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by rsperoni on 05/05/17.
 */
@Path("/reportes")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Transactional
@Api(description = "Reportes service", tags = "reportes")
public class ReportesService {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Inject
    private Logger logger;
    @EJB
    private EstimacionDao estimacionDao;
    @EJB
    private HoraDao horaDao;
    @EJB
    private ReportesDao reportesDao;
    @EJB
    private ProyectoDao proyectoDao;
    @EJB
    private TipoTareaDao tipoTareaDao;
    @EJB
    private CargoDao cargoDao;
    @EJB
    private ColaboradorDao colaboradorDao;


    @GET
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @ApiOperation(value = "Get estimaciones", response = Estimacion.class, responseContainer = "List")
    public Response findAll() {
        List<Estimacion> estimacionList = estimacionDao.findAll();
        return Response.ok(estimacionList).build();
    }

    @GET
    @Path("horas/proyecto/{proyecto_id}/tarea/{tarea_id}/cargo/{cargo_id}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @ApiOperation(value = "Horas de Proyecto, TipoTarea y Cargo agrupadas por Colaborador", response = ReporteHoras1.class, responseContainer = "List")
    public Response findHorasProyectoTipoTareaCargoXColaborador(@PathParam("proyecto_id") Long proyecto_id, @PathParam("tarea_id") Long tarea_id, @PathParam("cargo_id") Long cargo_id) {
        try {
            Proyecto proyecto = proyectoDao.findById(proyecto_id);
            if (proyecto == null)
                throw new MagnesiumNotFoundException("Proyecto no encontrado");
            TipoTarea tipoTarea = tipoTareaDao.findById(tarea_id);
            if (tipoTarea == null)
                throw new MagnesiumNotFoundException("Tarea no encontrada");
            Cargo cargo = cargoDao.findById(cargo_id);
            if (cargo == null)
                throw new MagnesiumNotFoundException("Cargo no encontrado");
            return Response.ok(null).build();
        } catch (MagnesiumNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("horas/proyecto/{proyecto_id}/tarea/{tarea_id}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @Logged
    @ApiOperation(value = "Horas de Proyecto y TipoTarea agrupadas por Cargo", response = ReporteHoras1.class, responseContainer = "List")
    public Response reporte1(@PathParam("proyecto_id") Long proyecto_id, @PathParam("tarea_id") Long tarea_id) {
        try {
            Proyecto proyecto = proyectoDao.findById(proyecto_id);
            if (proyecto == null)
                throw new MagnesiumNotFoundException("Proyecto no encontrado");
            TipoTarea tipoTarea = tipoTareaDao.findById(tarea_id);
            if (tipoTarea == null)
                throw new MagnesiumNotFoundException("Tarea no encontrada");

            List<ReporteHoras1> reporteHoras1List = reportesDao.reporteHoras1(proyecto, tipoTarea);
            return Response.ok(reporteHoras1List).build();

        } catch (MagnesiumNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("horas/fechas/{fecha_ini}/{fecha_fin}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @Logged
    @ApiOperation(value = "Reporte de horas cargadas por fechas", response = ReporteHoras1.class, responseContainer = "List")
    public Response reporteFechas(@PathParam("fecha_ini") String fechaIniString,
                                  @PathParam("fecha_fin") String fechaFinString) {
        try {

            LocalDate fechaIni = LocalDate.parse(fechaIniString, formatter);
            LocalDate fechaFin = LocalDate.parse(fechaFinString, formatter);

            List<ReporteHoras1> reporteHoras2List = reportesDao.reporteHoras2Fechas(fechaIni, fechaFin);
            return Response.ok(reporteHoras2List).build();


        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("horas/colaboradores/fechas/{fecha_ini}/{fecha_fin}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @Logged
    @ApiOperation(value = "Reporte de horas cargadas por fechas agrupada por colaborador", response = ReporteHoras2.class, responseContainer = "List")
    public Response reporteFechasPorColaboradores(@PathParam("fecha_ini") String fechaIniString,
                                                  @PathParam("fecha_fin") String fechaFinString) {
        try {

            LocalDate fechaIni = LocalDate.parse(fechaIniString, formatter);
            LocalDate fechaFin = LocalDate.parse(fechaFinString, formatter);

            List<ReporteHoras2> reporteHoras2List = reportesDao.reporteHoras2FechaXColaborador(fechaIni, fechaFin);
            return Response.ok(reporteHoras2List).build();


        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("horas/fechas/{fecha_ini}/{fecha_fin}/proyecto/{proyecto_id}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @Logged
    @ApiOperation(value = "Reporte de horas cargadas por fechas y proyecto", response = ReporteHoras1.class, responseContainer = "List")
    public Response reporteFechaProyecto(@PathParam("fecha_ini") String fechaIniString,
                                         @PathParam("fecha_fin") String fechaFinString,
                                         @PathParam("proyecto_id") Long proyecto_id) {
        try {

            LocalDate fechaIni = LocalDate.parse(fechaIniString, formatter);
            LocalDate fechaFin = LocalDate.parse(fechaFinString, formatter);
            Proyecto proyecto = proyectoDao.findById(proyecto_id);
            if (proyecto == null)
                throw new MagnesiumNotFoundException("Proyecto no encontrado");

            List<ReporteHoras1> reporteHoras2List = reportesDao.reporteHoras2FechasProyecto(fechaIni, fechaFin, proyecto);
            return Response.ok(reporteHoras2List).build();


        } catch (MagnesiumNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }


    @GET
    @Path("horas/proyecto/{proyecto_id}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @Logged
    @ApiOperation(value = "Horas de Proyecto agrupadas por Cargo", response = ReporteHoras1.class, responseContainer = "List")
    public Response reporte1Totales(@PathParam("proyecto_id") Long proyecto_id) {
        try {
            Proyecto proyecto = proyectoDao.findById(proyecto_id);
            if (proyecto == null)
                throw new MagnesiumNotFoundException("Proyecto no encontrado");

            List<ReporteHoras1> reporteHoras1List = reportesDao.reporteHoras1Totales(proyecto);
            //reporteHoras1List.forEach(reporteHoras1 -> logger.info(reporteHoras1.toString()));
            return Response.ok(reporteHoras1List).build();

        } catch (MagnesiumNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("estimaciones/proyecto/{proyecto_id}/tarea/{tarea_id}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @ApiOperation(value = "Estimaciones de Proyecto y TipoTarea agrupadas por Cargo", response = EstimacionProyectoTipoTareaXCargo.class, responseContainer = "List")
    public Response findEstimacionesProyectoTipoTareaXCargo(@PathParam("proyecto_id") Long proyecto_id, @PathParam("tarea_id") Long tarea_id) {
        try {
            Proyecto proyecto = proyectoDao.findById(proyecto_id);
            if (proyecto == null)
                throw new MagnesiumNotFoundException("Proyecto no encontrado");
            TipoTarea tipoTarea = tipoTareaDao.findById(tarea_id);
            if (tipoTarea == null)
                throw new MagnesiumNotFoundException("Tarea no encontrada");


            return Response.ok(null).build();
        } catch (MagnesiumNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }


}
