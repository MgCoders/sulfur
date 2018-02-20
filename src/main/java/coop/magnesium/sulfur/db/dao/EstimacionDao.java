package coop.magnesium.sulfur.db.dao;

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
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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

    public List<Estimacion> findAllByFechas(LocalDate ini, LocalDate fin) {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Estimacion.class);
        criteriaQuery.select(entity).distinct(true);
        Predicate diaEntreFechas = criteriaBuilder.between(entity.get("fecha"), criteriaBuilder.parameter(LocalDate.class, "ini"), criteriaBuilder.parameter(LocalDate.class, "fin"));
        criteriaQuery.where(diaEntreFechas);
        criteriaQuery.orderBy(criteriaBuilder.asc(entity.get("dia")));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        return (List<Estimacion>) query.getResultList();
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

    @Logged
    public Map<Cargo, EstimacionProyectoTipoTareaXCargo> findEstimacionProyectoTipoTareaXCargo(Proyecto proyecto, TipoTarea tipoTarea) {
        Map<Cargo, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = new HashMap<>();
        estimacionDao.findAllByProyecto(proyecto)
                .forEach(estimacion -> estimacion.getEstimacionCargos().forEach(estimacionCargo ->
                        estimacionCargo.getEstimacionTipoTareas().forEach(estimacionTipoTarea -> {
                            if (estimacionTipoTarea.getTipoTarea().getId().equals(tipoTarea.getId())) {
                                estimacionesXCargo.computeIfPresent(estimacionCargo.getCargo(), (cargo, estimacionProyectoTipoTareaXCargo) -> {
                                    estimacionProyectoTipoTareaXCargo.cantidadHoras = estimacionProyectoTipoTareaXCargo.cantidadHoras.add(TimeUtils.durationToBigDecimal(estimacionTipoTarea.getDuracion()));
                            return estimacionProyectoTipoTareaXCargo;
                        });
                                estimacionesXCargo.computeIfAbsent(estimacionCargo.getCargo(), cargo -> new EstimacionProyectoTipoTareaXCargo(estimacion.getProyecto(), estimacionTipoTarea.getTipoTarea(), estimacionCargo.getCargo(), estimacionCargo.getPrecioTotal(), TimeUtils.durationToBigDecimal(estimacionTipoTarea.getDuracion())));
                    }
                        })));
        return estimacionesXCargo;
    }

    @Logged
    public Map<Cargo, EstimacionProyectoTipoTareaXCargo> findEstimacionFechasTipoTareaXCargo(LocalDate ini, LocalDate fin) {
        Map<Cargo, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = new HashMap<>();
        estimacionDao.findAllByFechas(ini, fin)
                .forEach(estimacion -> estimacion.getEstimacionCargos().forEach(estimacionCargo ->
                        estimacionCargo.getEstimacionTipoTareas().forEach(estimacionTipoTarea -> {
                            estimacionesXCargo.computeIfPresent(estimacionCargo.getCargo(), (cargo, estimacionProyectoTipoTareaXCargo) -> {
                                estimacionProyectoTipoTareaXCargo.cantidadHoras = estimacionProyectoTipoTareaXCargo.cantidadHoras.add(TimeUtils.durationToBigDecimal(estimacionTipoTarea.getDuracion()));
                                return estimacionProyectoTipoTareaXCargo;
                            });
                            estimacionesXCargo.computeIfAbsent(estimacionCargo.getCargo(), cargo -> new EstimacionProyectoTipoTareaXCargo(estimacion.getProyecto(), estimacionTipoTarea.getTipoTarea(), estimacionCargo.getCargo(), estimacionCargo.getPrecioTotal(), TimeUtils.durationToBigDecimal(estimacionTipoTarea.getDuracion())));
                        })));
        return estimacionesXCargo;
    }

    public Map<Cargo, EstimacionProyectoTipoTareaXCargo> findEstimacionProyectoXCargo(Proyecto proyecto) {
        Map<Cargo, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = new HashMap<>();
        estimacionDao.findAllByProyecto(proyecto)
                .forEach(estimacion -> estimacion.getEstimacionCargos().forEach(estimacionCargo ->
                        estimacionCargo.getEstimacionTipoTareas().forEach(estimacionTipoTarea -> {
                            estimacionesXCargo.computeIfPresent(estimacionCargo.getCargo(), (cargo, estimacionProyectoTipoTareaXCargo) -> {
                                estimacionProyectoTipoTareaXCargo.cantidadHoras = estimacionProyectoTipoTareaXCargo.cantidadHoras.add(TimeUtils.durationToBigDecimal(estimacionTipoTarea.getDuracion()));
                                return estimacionProyectoTipoTareaXCargo;
                            });
                            estimacionesXCargo.computeIfAbsent(estimacionCargo.getCargo(), cargo -> new EstimacionProyectoTipoTareaXCargo(estimacion.getProyecto(), estimacionTipoTarea.getTipoTarea(), estimacionCargo.getCargo(), estimacionCargo.getPrecioTotal(), TimeUtils.durationToBigDecimal(estimacionTipoTarea.getDuracion())));
                        })));
        return estimacionesXCargo;
    }


}
