package coop.magnesium.sulfur.db.dao;

import coop.magnesium.sulfur.api.dto.EstimacionProyecto;
import coop.magnesium.sulfur.api.dto.EstimacionProyectoTipoTareaXCargo;
import coop.magnesium.sulfur.db.entities.Cargo;
import coop.magnesium.sulfur.db.entities.Estimacion;
import coop.magnesium.sulfur.db.entities.Proyecto;
import coop.magnesium.sulfur.db.entities.TipoTarea;
import coop.magnesium.sulfur.utils.Logged;
import coop.magnesium.sulfur.utils.TimeUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by rsperoni on 28/10/17.
 */
@Stateless
public class EstimacionDao extends AbstractDao<Estimacion, Long> {

    @Inject
    EstimacionDao estimacionDao;

    @Inject
    CargoDao cargoDao;

    @Inject
    TipoTareaDao tipoTareaDao;

    @Inject
    ProyectoDao proyectoDao;

    @Inject
    Logger logger;

    @PersistenceContext
    EntityManager em;

    @Override
    public Class<Estimacion> getEntityClass() {
        return Estimacion.class;
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    public Estimacion findById(Long id) {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Estimacion.class);
        criteriaQuery.select(entity).distinct(true);
        criteriaQuery.where(criteriaBuilder.equal(entity.get("id"), criteriaBuilder.parameter(Long.class, "id")));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("id", id);
        return (Estimacion) query.getSingleResult();
    }

    public List<EstimacionProyecto> findAllByFechas(LocalDate ini, LocalDate fin) {
        Query query = em.createNativeQuery("SELECT\n" +
                "  c.proyecto_id,\n" +
                "  su.tipotarea_id,\n" +
                "  su.cargo_id,\n" +
                "  su.preciototal,\n" +
                "  sum(su.duracion)    duracion\n" +
                "FROM Estimacion c, (\n" +
                "                     SELECT\n" +
                "                       ec.estimacion_id,\n" +
                "                       ett.tipotarea_id,\n" +
                "                       ec.cargo_id,\n" +
                "                       ec.preciototal,\n" +
                "                       sum(ett.duracion) duracion\n" +
                "                     FROM estimacioncargo ec, estimaciontipotarea ett\n" +
                "                     WHERE ett.estimacion_cargo_id = ec.id\n" +
                "                     GROUP BY\n" +
                "                       ec.estimacion_id,\n" +
                "                       ett.tipotarea_id,\n" +
                "                       ec.cargo_id,\n" +
                "                       ec.preciototal\n" +
                "                   ) su\n" +
                "WHERE c.id = su.estimacion_id AND c.fecha >= :fecha_ini AND c.fecha <= :fecha_fin\n" +
                "GROUP BY\n" +
                "  c.proyecto_id,\n" +
                "  su.preciototal,\n" +
                "  su.tipotarea_id,\n" +
                "  su.cargo_id;", "EstimacionProyecto");
        query.setParameter("fecha_ini", ini);
        query.setParameter("fecha_fin", fin);
        @SuppressWarnings("unchecked")
        List<EstimacionProyecto> resultList = query.getResultList();
        return resultList;
    }

    public List<EstimacionProyecto> findAllByFechasProyecto(LocalDate ini, LocalDate fin, Proyecto proyecto) {
        Query query = em.createNativeQuery("SELECT\n" +
                "  c.proyecto_id,\n" +
                "  su.tipotarea_id,\n" +
                "  su.cargo_id,\n" +
                "  su.preciototal,\n" +
                "  sum(su.duracion)    duracion\n" +
                "FROM Estimacion c, (\n" +
                "                     SELECT\n" +
                "                       ec.estimacion_id,\n" +
                "                       ett.tipotarea_id,\n" +
                "                       ec.cargo_id,\n" +
                "                       ec.preciototal,\n" +
                "                       sum(ett.duracion) duracion\n" +
                "                     FROM estimacioncargo ec, estimaciontipotarea ett\n" +
                "                     WHERE ett.estimacion_cargo_id = ec.id\n" +
                "                     GROUP BY\n" +
                "                       ec.estimacion_id,\n" +
                "                       ett.tipotarea_id,\n" +
                "                       ec.cargo_id,\n" +
                "                       ec.preciototal\n" +
                "                   ) su\n" +
                "WHERE c.id = su.estimacion_id AND c.fecha >= :fecha_ini AND c.fecha <= :fecha_fin AND c.proyecto_id = :proyecto\n" +
                "GROUP BY\n" +
                "  c.proyecto_id,\n" +
                "  su.tipotarea_id,\n" +
                "  su.preciototal,\n" +
                "  su.cargo_id;", "EstimacionProyecto");
        query.setParameter("fecha_ini", ini);
        query.setParameter("fecha_fin", fin);
        query.setParameter("proyecto", proyecto.getId());
        @SuppressWarnings("unchecked")
        List<EstimacionProyecto> resultList = query.getResultList();
        return resultList;
    }

    public List<Estimacion> findAllByProyecto(Proyecto proyecto) {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Estimacion.class);
        criteriaQuery.select(entity).distinct(true);
        criteriaQuery.where(criteriaBuilder.equal(entity.get("proyecto"), criteriaBuilder.parameter(Proyecto.class, "p")));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("p", proyecto);
        return (List<Estimacion>) query.getResultList();
    }

    public List<EstimacionProyecto> findEstimacionProyectoTipoTarea(Proyecto proyecto, TipoTarea tipoTarea) {
        Query query = em.createNativeQuery("SELECT\n" +
                "  c.proyecto_id,\n" +
                "  su.tipotarea_id,\n" +
                "  su.cargo_id,\n" +
                "  su.preciototal,\n" +
                "  sum(su.duracion)    duracion\n" +
                "FROM Estimacion c, (\n" +
                "                     SELECT\n" +
                "                       ec.estimacion_id,\n" +
                "                       ett.tipotarea_id,\n" +
                "                       ec.cargo_id,\n" +
                "                       ec.preciototal,\n" +
                "                       sum(ett.duracion) duracion\n" +
                "                     FROM estimacioncargo ec, estimaciontipotarea ett\n" +
                "                     WHERE ett.tipotarea_id = :tipotarea\n" +
                "                           AND ett.estimacion_cargo_id = ec.id\n" +
                "                     GROUP BY\n" +
                "                       ec.estimacion_id,\n" +
                "                       ett.tipotarea_id,\n" +
                "                       ec.cargo_id,\n" +
                "                       ec.preciototal\n" +
                "                   ) su\n" +
                "WHERE c.id = su.estimacion_id AND c.proyecto_id = :proyecto\n" +
                "GROUP BY\n" +
                "  c.proyecto_id,\n" +
                "  su.tipotarea_id,\n" +
                "  su.preciototal,\n" +
                "  su.cargo_id;", "EstimacionProyecto");
        query.setParameter("proyecto", proyecto.getId());
        query.setParameter("tipotarea", tipoTarea.getId());
        @SuppressWarnings("unchecked")
        List<EstimacionProyecto> resultList = query.getResultList();
        return resultList;
    }

    public List<EstimacionProyecto> findEstimacionProyecto(Proyecto proyecto) {
        Query query = em.createNativeQuery("SELECT\n" +
                "  c.proyecto_id,\n" +
                "  su.tipotarea_id,\n" +
                "  su.cargo_id,\n" +
                "  su.preciototal,\n" +
                "  sum(su.duracion)    duracion\n" +
                "FROM Estimacion c, (\n" +
                "                     SELECT\n" +
                "                       ec.estimacion_id,\n" +
                "                       ett.tipotarea_id,\n" +
                "                       ec.cargo_id,\n" +
                "                       ec.preciototal,\n" +
                "                       sum(ett.duracion) duracion\n" +
                "                     FROM estimacioncargo ec, estimaciontipotarea ett\n" +
                "                     WHERE ett.estimacion_cargo_id = ec.id\n" +
                "                     GROUP BY\n" +
                "                       ec.estimacion_id,\n" +
                "                       ett.tipotarea_id,\n" +
                "                       ec.cargo_id,\n" +
                "                       ec.preciototal\n" +
                "                   ) su\n" +
                "WHERE c.id = su.estimacion_id AND c.proyecto_id = :proyecto\n" +
                "GROUP BY\n" +
                "  c.proyecto_id,\n" +
                "  su.tipotarea_id,\n" +
                "  su.preciototal,\n" +
                "  su.cargo_id;", "EstimacionProyecto");
        query.setParameter("proyecto", proyecto.getId());
        @SuppressWarnings("unchecked")
        List<EstimacionProyecto> resultList = query.getResultList();
        return resultList;
    }

    @Logged
    public Map<Long, EstimacionProyectoTipoTareaXCargo> findEstimacionProyectoTipoTareaXCargo(Proyecto proyecto, TipoTarea tipoTarea) {
        Map<Long, EstimacionProyectoTipoTareaXCargo> estimacionesXCargoNative = new HashMap<>();
        estimacionDao.findEstimacionProyectoTipoTarea(proyecto, tipoTarea).forEach(estimacionProyecto -> {
            Cargo cargo = cargoDao.findById(estimacionProyecto.cargo_id);
            BigDecimal precioXHoraNuevo = estimacionProyecto.precioTotal;
            BigDecimal cantHorasNueva = TimeUtils.durationToBigDecimal(Duration.ofNanos(estimacionProyecto.duracion));
            BigDecimal precioTotalNuevo = precioXHoraNuevo.multiply(cantHorasNueva);
            BigDecimal cantHorasAnterior = estimacionesXCargoNative.get(cargo.getId()) != null ? estimacionesXCargoNative.get(cargo.getId()).cantidadHoras : BigDecimal.ZERO;
            BigDecimal precioTotalAnterior = estimacionesXCargoNative.get(cargo.getId()) != null ? estimacionesXCargoNative.get(cargo.getId()).precioTotal : BigDecimal.ZERO;
            estimacionesXCargoNative.put(cargo.getId(), new EstimacionProyectoTipoTareaXCargo(proyecto, tipoTarea, cargo, precioTotalAnterior.add(precioTotalNuevo), cantHorasAnterior.add(cantHorasNueva)));
        });
        return estimacionesXCargoNative;
    }

    @Logged
    public Map<Long, EstimacionProyectoTipoTareaXCargo> findEstimacionFechasXCargo(LocalDate ini, LocalDate fin) {
        Map<Long, EstimacionProyectoTipoTareaXCargo> estimacionesXCargoNative = new HashMap<>();
        estimacionDao.findAllByFechas(ini, fin).forEach(estimacionProyecto -> {
            Cargo cargo = cargoDao.findById(estimacionProyecto.cargo_id);
            TipoTarea tipoTarea = tipoTareaDao.findById(estimacionProyecto.tipoTarea_id);
            Proyecto proyecto = proyectoDao.findById(estimacionProyecto.proyecto_id);
            BigDecimal precioXHoraNuevo = estimacionProyecto.precioTotal;
            BigDecimal cantHorasNueva = TimeUtils.durationToBigDecimal(Duration.ofNanos(estimacionProyecto.duracion));
            BigDecimal precioTotalNuevo = precioXHoraNuevo.multiply(cantHorasNueva);
            BigDecimal cantHorasAnterior = estimacionesXCargoNative.get(cargo.getId()) != null ? estimacionesXCargoNative.get(cargo.getId()).cantidadHoras : BigDecimal.ZERO;
            BigDecimal precioTotalAnterior = estimacionesXCargoNative.get(cargo.getId()) != null ? estimacionesXCargoNative.get(cargo.getId()).precioTotal : BigDecimal.ZERO;
            estimacionesXCargoNative.put(cargo.getId(), new EstimacionProyectoTipoTareaXCargo(proyecto, tipoTarea, cargo, precioTotalAnterior.add(precioTotalNuevo), cantHorasAnterior.add(cantHorasNueva)));
        });
        return estimacionesXCargoNative;
    }

    @Logged
    public Map<Long, EstimacionProyectoTipoTareaXCargo> findEstimacionFechasProyectoXCargo(LocalDate ini, LocalDate fin, Proyecto proyecto) {
        Map<Long, EstimacionProyectoTipoTareaXCargo> estimacionesXCargoNative = new HashMap<>();
        estimacionDao.findAllByFechas(ini, fin).forEach(estimacionProyecto -> {
            Cargo cargo = cargoDao.findById(estimacionProyecto.cargo_id);
            TipoTarea tipoTarea = tipoTareaDao.findById(estimacionProyecto.tipoTarea_id);
            BigDecimal precioXHoraNuevo = estimacionProyecto.precioTotal;
            BigDecimal cantHorasNueva = TimeUtils.durationToBigDecimal(Duration.ofNanos(estimacionProyecto.duracion));
            BigDecimal precioTotalNuevo = precioXHoraNuevo.multiply(cantHorasNueva);
            BigDecimal cantHorasAnterior = estimacionesXCargoNative.get(cargo.getId()) != null ? estimacionesXCargoNative.get(cargo.getId()).cantidadHoras : BigDecimal.ZERO;
            BigDecimal precioTotalAnterior = estimacionesXCargoNative.get(cargo.getId()) != null ? estimacionesXCargoNative.get(cargo.getId()).precioTotal : BigDecimal.ZERO;
            estimacionesXCargoNative.put(cargo.getId(), new EstimacionProyectoTipoTareaXCargo(proyecto, tipoTarea, cargo, precioTotalAnterior.add(precioTotalNuevo), cantHorasAnterior.add(cantHorasNueva)));
        });
        return estimacionesXCargoNative;
    }

    @Logged
    public Map<Long, EstimacionProyectoTipoTareaXCargo> findEstimacionProyectoXCargo(Proyecto proyecto) {
        Map<Long, EstimacionProyectoTipoTareaXCargo> estimacionesXCargoNative = new HashMap<>();
        estimacionDao.findEstimacionProyecto(proyecto).forEach(estimacionProyecto -> {
            Cargo cargo = cargoDao.findById(estimacionProyecto.cargo_id);
            TipoTarea tipoTarea = tipoTareaDao.findById(estimacionProyecto.tipoTarea_id);
            BigDecimal precioXHoraNuevo = estimacionProyecto.precioTotal;
            BigDecimal cantHorasNueva = TimeUtils.durationToBigDecimal(Duration.ofNanos(estimacionProyecto.duracion));
            BigDecimal precioTotalNuevo = precioXHoraNuevo.multiply(cantHorasNueva);
            BigDecimal cantHorasAnterior = estimacionesXCargoNative.get(cargo.getId()) != null ? estimacionesXCargoNative.get(cargo.getId()).cantidadHoras : BigDecimal.ZERO;
            BigDecimal precioTotalAnterior = estimacionesXCargoNative.get(cargo.getId()) != null ? estimacionesXCargoNative.get(cargo.getId()).precioTotal : BigDecimal.ZERO;
            estimacionesXCargoNative.put(cargo.getId(), new EstimacionProyectoTipoTareaXCargo(proyecto, tipoTarea, cargo, precioTotalAnterior.add(precioTotalNuevo), cantHorasAnterior.add(cantHorasNueva)));
        });
        return estimacionesXCargoNative;
    }


}
