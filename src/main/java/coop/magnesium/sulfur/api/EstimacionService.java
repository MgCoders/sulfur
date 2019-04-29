package coop.magnesium.sulfur.api;


import coop.magnesium.sulfur.api.utils.JWTTokenNeeded;
import coop.magnesium.sulfur.api.utils.RoleNeeded;
import coop.magnesium.sulfur.db.dao.CargoDao;
import coop.magnesium.sulfur.db.dao.EstimacionDao;
import coop.magnesium.sulfur.db.dao.ProyectoDao;
import coop.magnesium.sulfur.db.dao.TipoTareaDao;
import coop.magnesium.sulfur.db.entities.Estimacion;
import coop.magnesium.sulfur.db.entities.Proyecto;
import coop.magnesium.sulfur.db.entities.Role;
import coop.magnesium.sulfur.utils.Logged;
import coop.magnesium.sulfur.utils.ex.MagnesiumBdAlredyExistsException;
import coop.magnesium.sulfur.utils.ex.MagnesiumNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by rsperoni on 05/05/17.
 */
@Path("/estimaciones")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Transactional
@Api(description = "Estimacion service", tags = "estimacion")
public class EstimacionService {

    @Inject
    private Logger logger;
    @EJB
    private EstimacionDao estimacionDao;
    @EJB
    private ProyectoDao proyectoDao;
    @EJB
    private CargoDao cargoDao;
    @EJB
    private TipoTareaDao tipoTareaDao;

    @POST
    @Logged
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @ApiOperation(value = "Create estimacion", response = Estimacion.class)
    @ApiResponses(value = {
            @ApiResponse(code = 409, message = "Código o Id ya existe"),
            @ApiResponse(code = 400, message = "Objeto inválido"),
            @ApiResponse(code = 500, message = "Error interno"),
            @ApiResponse(code = 401, message = "No Autorizado")})
    public Response create(@Valid Estimacion estimacion) {
        try {
            Estimacion estimacionExists = estimacion.getId() != null ? estimacionDao.findById(estimacion.getId()) : null;
            if (estimacionExists != null) throw new MagnesiumBdAlredyExistsException("Estimacion ya existe");


            estimacion = estimacionDao.save(estimacion);
            return Response.status(Response.Status.CREATED).entity(estimacion).build();
        } catch (MagnesiumBdAlredyExistsException exists) {
            exists.printStackTrace();
            logger.warning(exists.getMessage());
            return Response.status(Response.Status.CONFLICT).entity(exists.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        }
    }


    @GET
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @ApiOperation(value = "Get estimaciones", response = Estimacion.class, responseContainer = "List")
    public Response findAll() {
        List<Estimacion> estimacionList = estimacionDao.findAll();
        return Response.ok(estimacionList).build();
    }

    @GET
    @Path("proyecto/{id}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @ApiOperation(value = "Get estimaciones por proyecto", response = Estimacion.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Objeto inválido"),
            @ApiResponse(code = 401, message = "No Autorizado")})
    public Response findAllByProyecto(@PathParam("id") Long id) {
        try {
            Proyecto proyecto = proyectoDao.findById(id);
            if (proyecto == null) throw new MagnesiumNotFoundException("Proyecto no encontrado");

            List<Estimacion> estimacionList = estimacionDao.findAllByProyecto(proyecto);
            return Response.ok(estimacionList).build();
        } catch (Exception e) {
            logger.warning(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{id}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @ApiOperation(value = "Get hora", response = Estimacion.class)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Id no encontrado")})
    public Response find(@PathParam("id") Long id) {
        Estimacion estimacion = estimacionDao.findById(id);
        if (estimacion == null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(estimacion).build();
    }

    @PUT
    @Path("{id}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @ApiOperation(value = "Edit estimacion", response = Estimacion.class)
    @ApiResponses(value = {
            @ApiResponse(code = 304, message = "Error: objeto no modificado")})
    public Response edit(@PathParam("id") Long id, @Valid Estimacion estimacion) {
        try {

            if (estimacionDao.findById(id) == null) throw new MagnesiumNotFoundException("Estimacion no encontrada");
            estimacion.setId(id);

            Proyecto proyecto = proyectoDao.findById(estimacion.getProyecto().getId());
            if (proyecto == null) throw new MagnesiumNotFoundException("Proyecto no encontrado");
            estimacion.setProyecto(proyecto);


            estimacion = estimacionDao.save(estimacion);
            return Response.ok(estimacion).build();
        } catch (Exception e) {
            return Response.notModified().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("delete/{id}")
    @JWTTokenNeeded
    @RoleNeeded({Role.ADMIN})
    @ApiOperation(value = "Delete estimacion", response = Estimacion.class)
    @ApiResponses(value = {
            @ApiResponse(code = 304, message = "Error: objeto no modificado")})
    public Response delete(@PathParam("id") Long id) {
        try {
            Estimacion est = this.estimacionDao.findById(id);
            if(est == null)
                throw new Exception("No existe ninguna estimación con id " + id);

            this.estimacionDao.delete(id);

            return Response.ok(est).build();
        } catch (Exception e) {
            return Response.notModified().entity(e.getMessage()).build();
        }
    }
}
