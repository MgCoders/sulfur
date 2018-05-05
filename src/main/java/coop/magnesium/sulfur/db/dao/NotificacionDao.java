package coop.magnesium.sulfur.db.dao;

import coop.magnesium.sulfur.db.entities.Colaborador;
import coop.magnesium.sulfur.db.entities.Notificacion;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by rsperoni on 22/01/18.
 */
@Stateless
public class NotificacionDao extends AbstractDao<Notificacion, Long> {

    @PersistenceContext
    EntityManager em;

    @Override
    public Class<Notificacion> getEntityClass() {
        return Notificacion.class;
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    public List<Notificacion> findAllByColaborador(Colaborador colaborador, LocalDateTime ini, LocalDateTime fin) {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Notificacion.class);
        criteriaQuery.select(entity);
        Predicate colaboradorIsOk = criteriaBuilder.equal(entity.get("colaborador"), criteriaBuilder.parameter(Colaborador.class, "c"));
        Predicate diaEntreFechas = criteriaBuilder.between(entity.get("fechaHora"), criteriaBuilder.parameter(LocalDateTime.class, "ini"), criteriaBuilder.parameter(LocalDateTime.class, "fin"));
        criteriaQuery.where(criteriaBuilder.and(colaboradorIsOk, diaEntreFechas));
        criteriaQuery.orderBy(criteriaBuilder.desc(entity.get("fechaHora")));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("c", colaborador);
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        return (List<Notificacion>) query.getResultList();
    }

    public List<Notificacion> findAllNoEnviadas() {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Notificacion.class);
        criteriaQuery.select(entity);
        Predicate noEnviadaIsOk = criteriaBuilder.equal(entity.get("enviado"), false);
        criteriaQuery.where(noEnviadaIsOk);
        criteriaQuery.orderBy(criteriaBuilder.desc(entity.get("fechaHora")));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        return (List<Notificacion>) query.getResultList();
    }

    public List<Notificacion> findAll(LocalDateTime ini, LocalDateTime fin) {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Notificacion.class);
        criteriaQuery.select(entity);
        Predicate diaEntreFechas = criteriaBuilder.between(entity.get("fechaHora"), criteriaBuilder.parameter(LocalDateTime.class, "ini"), criteriaBuilder.parameter(LocalDateTime.class, "fin"));
        criteriaQuery.where(diaEntreFechas);
        criteriaQuery.orderBy(criteriaBuilder.desc(entity.get("fechaHora")));
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("ini", ini);
        query.setParameter("fin", fin);
        return (List<Notificacion>) query.getResultList();
    }
}
