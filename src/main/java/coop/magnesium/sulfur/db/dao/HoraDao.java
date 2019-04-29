package coop.magnesium.sulfur.db.dao;

import coop.magnesium.sulfur.api.dto.HoraCompletaReporte1;
import coop.magnesium.sulfur.api.dto.HoraCompletaReporte2;
import coop.magnesium.sulfur.api.dto.HoraCompletaReporte3;
import coop.magnesium.sulfur.db.entities.*;
import coop.magnesium.sulfur.utils.Logged;
import coop.magnesium.sulfur.utils.ex.MagnesiumBdMultipleResultsException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by rsperoni on 28/10/17.
 */
@Stateless
public class HoraDao extends AbstractDao<Hora, Long> {

    @PersistenceContext
    EntityManager em;

    @Override
    public Class<Hora> getEntityClass() {
        return Hora.class;
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    @Inject
    Logger logger;

    /**
     * Devuelva horas de colaborador entre fechas ini y fin.
     *
     * @param colaborador
     * @param ini
     * @param fin
     * @return
     */
    public List<Hora> findAllByColaborador(Colaborador colaborador, LocalDate ini, LocalDate fin) {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Hora.class);
        criteriaQuery.select(entity);
        Predicate colaboradorIsOk = criteriaBuilder.equal(entity.get("colaborador"), criteriaBuilder.parameter(Colaborador.class, "c"));
        Predicate diaEntreFechas = criteriaBuilder.between(entity.get("dia"), criteriaBuilder.parameter(LocalDate.class, "ini"), criteriaBuilder.parameter(LocalDate.class, "fin"));
        criteriaQuery.where(criteriaBuilder.and(colaboradorIsOk, diaEntreFechas));
        criteriaQuery.orderBy(criteriaBuilder.asc(entity.get("dia")));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("c", colaborador);
        query.setParameter("ini", ini);
        query.setParameter("fin",fin);
        return (List<Hora>) query.getResultList();
    }

    public List<Hora> findAllByFechas(LocalDate ini, LocalDate fin) {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Hora.class);
        criteriaQuery.select(entity);
        Predicate diaEntreFechas = criteriaBuilder.between(entity.get("dia"), criteriaBuilder.parameter(LocalDate.class, "ini"), criteriaBuilder.parameter(LocalDate.class, "fin"));
        criteriaQuery.where(diaEntreFechas);
        criteriaQuery.orderBy(criteriaBuilder.asc(entity.get("dia")));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        return (List<Hora>) query.getResultList();
    }

    public Hora findByColaboradorFecha(Colaborador colaborador, LocalDate dia) throws MagnesiumBdMultipleResultsException {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Hora.class);
        criteriaQuery.select(entity);
        Predicate colaboradorIsOk = criteriaBuilder.equal(entity.get("colaborador"), criteriaBuilder.parameter(Colaborador.class, "c"));
        Predicate fechaIsOk = criteriaBuilder.equal(entity.get("dia"), criteriaBuilder.parameter(LocalDate.class, "dia"));
        criteriaQuery.where(criteriaBuilder.and(colaboradorIsOk, fechaIsOk));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("c", colaborador);
        query.setParameter("dia", dia);
        List<Hora> result = query.getResultList();
        if (result.size() == 0) return null;
        if (result.size() > 1)
            throw new MagnesiumBdMultipleResultsException(Hora.class.getSimpleName() + "multiples resultados encontrados");
        return result.get(0);
    }

    public boolean existsByColaboradorIncompleta(Colaborador colaborador) {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Hora.class);
        criteriaQuery.select(entity);
        Predicate colaboradorIsOk = criteriaBuilder.equal(entity.get("colaborador"), criteriaBuilder.parameter(Colaborador.class, "c"));
        Predicate completaOk = criteriaBuilder.equal(entity.get("completa"), false);
        criteriaQuery.where(criteriaBuilder.and(colaboradorIsOk, completaOk));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("c", colaborador);
        List<Hora> result = query.getResultList();
        return result.size() > 0;
    }

    public List<HoraCompletaReporte1> findHorasByFechasXCargo(LocalDate ini, LocalDate fin) {
        Query query = em.createNativeQuery(
                "SELECT\n" +
                        "  p.id             proyecto_id,\n" +
                        "  ta.id            tipotarea_id,\n" +
                        "  co.id            colaborador_id,\n" +
                        "  ca.id            cargo_id,\n" +
                        "  sum(hd.duracion) duracion,\n" +
                        "  h.dia            dia\n" +
                        "FROM horaDetalle hd,\n" +
                        "  Colaborador co,\n" +
                        "  Proyecto p,\n" +
                        "  TipoTarea ta,\n" +
                        "  hora h,\n" +
                        "  Cargo ca\n" +
                        "WHERE hd.hora_id = h.id\n" +
                        "      --AND p.id = :proyecto\n" +
                        "      AND co.id = h.colaborador_id\n" +
                        "      AND hd.tipoTarea_id = ta.id\n" +
                        "      AND hd.proyecto_id = p.id\n" +
                        "      AND ca.id = hd.cargo_id\n" +
                        "      AND h.dia >= :ini AND h.dia <= :fin\n" +
                        "GROUP BY\n" +
                        "  p.id,\n" +
                        "  ta.id,\n" +
                        "  co.id,\n" +
                        "  ca.id,\n" +
                        "  h.dia\n" +
                        "ORDER BY co.cargo_id;", "HoraCompletaReporte1");
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        return query.getResultList();
    }

    public List<HoraCompletaReporte3> findHorasByFechasXCargoProyecto(LocalDate ini, LocalDate fin) {
        Query query = em.createNativeQuery(
                "SELECT\n" +
                        "  p.id             proyecto_id,\n" +
                        "  ca.id            cargo_id,\n" +
                        "  sum(hd.duracion) duracion,\n" +
                        "  h.dia            dia\n" +
                        "FROM horaDetalle hd,\n" +
                        "  Colaborador co,\n" +
                        "  Proyecto p,\n" +
                        "  TipoTarea ta,\n" +
                        "  hora h,\n" +
                        "  Cargo ca\n" +
                        "WHERE hd.hora_id = h.id\n" +
                        "      AND co.id = h.colaborador_id\n" +
                        "      AND hd.tipoTarea_id = ta.id\n" +
                        "      AND hd.proyecto_id = p.id\n" +
                        "      AND ca.id = hd.cargo_id\n" +
                        "      AND h.dia >= :ini AND h.dia <= :fin\n" +
                        "GROUP BY\n" +
                        "  p.id,\n" +
                        "  ca.id,\n" +
                        "  h.dia\n" +
                        "ORDER BY ca.id;", "HoraCompletaReporte3");
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        return query.getResultList();
    }

    public List<HoraCompletaReporte2> findHorasByFechasXColaborador(LocalDate ini, LocalDate fin) {
        Query query = em.createNativeQuery(
                "SELECT\n" +
                        "  co.id            colaborador_id,\n" +
                        "  ca.id            cargo_id,\n" +
                        "  sum(hd.duracion) duracion,\n" +
                        "  h.dia            dia\n" +
                        "FROM horaDetalle hd,\n" +
                        "  Colaborador co,\n" +
                        "  hora h,\n" +
                        "  Cargo ca\n" +
                        "WHERE hd.hora_id = h.id\n" +
                        "      AND co.id = h.colaborador_id\n" +
                        "      AND ca.id = hd.cargo_id\n" +
                        "      AND h.dia >= :ini AND h.dia <= :fin\n" +
                        "GROUP BY\n" +
                        "  co.id,\n" +
                        "  ca.id,\n" +
                        "  h.dia\n" +
                        "ORDER BY co.cargo_id;", "HoraCompletaReporte2");
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        return query.getResultList();
    }

    public List<HoraCompletaReporte1> findHorasByFechasProyectoXCargo(LocalDate ini, LocalDate fin, Proyecto proyecto) {
        Query query = em.createNativeQuery(
                "SELECT\n" +
                        "  p.id             proyecto_id,\n" +
                        "  ta.id            tipotarea_id,\n" +
                        "  co.id            colaborador_id,\n" +
                        "  ca.id            cargo_id,\n" +
                        "  sum(hd.duracion) duracion,\n" +
                        "  h.dia            dia\n" +
                        "FROM horaDetalle hd,\n" +
                        "  Colaborador co,\n" +
                        "  Proyecto p,\n" +
                        "  TipoTarea ta,\n" +
                        "  hora h,\n" +
                        "  Cargo ca\n" +
                        "WHERE hd.hora_id = h.id\n" +
                        "      --AND p.id = :proyecto\n" +
                        "      AND co.id = h.colaborador_id\n" +
                        "      AND hd.tipoTarea_id = ta.id\n" +
                        "      AND hd.proyecto_id = p.id\n" +
                        "      AND ca.id = hd.cargo_id\n" +
                        "      AND h.dia >= :ini AND h.dia <= :fin AND p.id = :proyecto\n" +
                        "GROUP BY\n" +
                        "  p.id,\n" +
                        "  ta.id,\n" +
                        "  co.id,\n" +
                        "  ca.id,\n" +
                        "  h.dia\n" +
                        "ORDER BY co.cargo_id;", "HoraCompletaReporte1");
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        query.setParameter("proyecto", proyecto.getId());
        return query.getResultList();
    }

    public List<HoraCompletaReporte1> findHorasProyectoTipoTareaXCargo(Proyecto proyecto, TipoTarea tipoTarea, LocalDate ini, LocalDate fin) {
        Query query = em.createNativeQuery(
                "SELECT\n" +
                        "  p.id             proyecto_id,\n" +
                        "  ta.id            tipotarea_id,\n" +
                        "  co.id            colaborador_id,\n" +
                        "  ca.id            cargo_id,\n" +
                        "  sum(hd.duracion) duracion,\n" +
                        "  h.dia            dia\n" +
                        "FROM horaDetalle hd,\n" +
                        "  Colaborador co,\n" +
                        "  Proyecto p,\n" +
                        "  TipoTarea ta,\n" +
                        "  hora h,\n" +
                        "  Cargo ca\n" +
                        "WHERE hd.hora_id = h.id\n" +
                        "      AND ta.id = :tipo_tarea\n" +
                        "      AND p.id = :proyecto\n" +
                        "      AND co.id = h.colaborador_id\n" +
                        "      AND hd.tipoTarea_id = ta.id\n" +
                        "      AND hd.proyecto_id = p.id\n" +
                        "      AND ca.id = hd.cargo_id\n" +
                        "      AND h.dia >= :ini AND h.dia <= :fin " +
                        "GROUP BY\n" +
                        "  p.id,\n" +
                        "  ta.id,\n" +
                        "  co.id,\n" +
                        "  ca.id,\n" +
                        "  h.dia\n" +
                        "ORDER BY co.cargo_id;", "HoraCompletaReporte1");
        query.setParameter("proyecto", proyecto.getId());
        query.setParameter("tipo_tarea", tipoTarea.getId());
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        return query.getResultList();
    }

    public List<HoraCompletaReporte1> findHorasProyectoXCargo(Proyecto proyecto, LocalDate ini, LocalDate fin) {
        Query query = em.createNativeQuery(
                "SELECT\n" +
                        "  p.id             proyecto_id,\n" +
                        "  ta.id            tipotarea_id,\n" +
                        "  co.id            colaborador_id,\n" +
                        "  ca.id            cargo_id,\n" +
                        "  sum(hd.duracion) duracion,\n" +
                        "  h.dia            dia\n" +
                        "FROM horaDetalle hd,\n" +
                        "  Colaborador co,\n" +
                        "  Proyecto p,\n" +
                        "  TipoTarea ta,\n" +
                        "  hora h,\n" +
                        "  Cargo ca\n" +
                        "WHERE hd.hora_id = h.id\n" +
                        "      AND p.id = :proyecto\n" +
                        "      AND co.id = h.colaborador_id\n" +
                        "      AND hd.tipoTarea_id = ta.id\n" +
                        "      AND hd.proyecto_id = p.id\n" +
                        "      AND ca.id = hd.cargo_id\n" +
                        "      AND h.dia >= :ini AND h.dia <= :fin " +
                        "GROUP BY\n" +
                        "  p.id,\n" +
                        "  ta.id,\n" +
                        "  co.id,\n" +
                        "  ca.id,\n" +
                        "  h.dia\n" +
                        "ORDER BY co.cargo_id;", "HoraCompletaReporte1");
        query.setParameter("proyecto", proyecto.getId());
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        return query.getResultList();
    }

    public BigDecimal findPrecioHoraCargo(Cargo cargo, LocalDate dia) {
        Query query = em.createNativeQuery(
                "SELECT\n" +
                        "  ph.preciohora\n" +
                        "FROM preciohora ph\n" +
                        "  JOIN (SELECT\n" +
                        "          cargo_id,\n" +
                        "          max(vigenciadesde) max_vigenciadesde\n" +
                        "        FROM preciohora\n" +
                        "        WHERE vigenciadesde <= :dia AND cargo_id = :cargo\n" +
                        "        GROUP BY cargo_id\n" +
                        "       ) ph2\n" +
                        "    ON (ph.vigenciadesde = ph2.max_vigenciadesde and ph.cargo_id = ph2.cargo_id);");
        query.setParameter("dia", dia);
        query.setParameter("cargo", cargo.getId());
        Number singleResult = ((Number) query.getSingleResult());
        return new BigDecimal(singleResult.toString());
    }


    @Logged
    public void deleteCascade(Hora hora) {
        int not = em.createQuery("DELETE FROM Notificacion WHERE hora_id = :id").setParameter("id", hora.getId()).executeUpdate();
        int horaDet = em.createQuery("DELETE FROM HoraDetalle WHERE hora_id = :id").setParameter("id", hora.getId()).executeUpdate();
        int horaD = em.createQuery("DELETE FROM Hora WHERE id = :id").setParameter("id", hora.getId()).executeUpdate();
        logger.info("Hora borrada: " + horaD + " con " + horaDet + " hora detalle y " + not + " notificacion");
    }
}
