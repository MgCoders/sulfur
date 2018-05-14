package coop.magnesium.sulfur.db.dao;

import coop.magnesium.sulfur.db.entities.Configuracion;
import coop.magnesium.sulfur.db.entities.TipoConfiguracion;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by rsperoni on 28/10/17.
 */
@Stateless
public class ConfiguracionDao extends AbstractDao<Configuracion, Long> {

    @PersistenceContext
    EntityManager em;

    @Inject
    Logger logger;

    @Inject
    ConfiguracionDao configuracionDao;

    @Override
    public Class<Configuracion> getEntityClass() {
        return Configuracion.class;
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }


    public List<Configuracion> findAllByClave(TipoConfiguracion clave) {
        CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        Root entity = criteriaQuery.from(Configuracion.class);
        criteriaQuery.select(entity);
        Predicate claveIsOk = criteriaBuilder.equal(entity.get("clave"), criteriaBuilder.parameter(TipoConfiguracion.class, "c"));
        criteriaQuery.where(claveIsOk);
        Query query = this.getEntityManager().createQuery(criteriaQuery);
        query.setParameter("c", clave);
        return (List<Configuracion>) query.getResultList();
    }

    public boolean isEmailOn() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.NOTIFICACION_MAIL_ACTIVADO);
        return !configuracionList.isEmpty() && (Boolean.valueOf(configuracionList.get(0).getValor()));
    }

    public void setMailOn(boolean on) {
        List<Configuracion> emailOn = configuracionDao.findAllByClave(TipoConfiguracion.NOTIFICACION_MAIL_ACTIVADO);
        if (emailOn.isEmpty()) {
            configuracionDao.save(new Configuracion(TipoConfiguracion.NOTIFICACION_MAIL_ACTIVADO, String.valueOf(on)));
        } else {
            emailOn.get(0).setValor(String.valueOf(on));
        }

    }

    public List<String> getDestinatariosNotificacionesAdmins() {
        return findAllByClave(TipoConfiguracion.NOTIFICACION_ADMIN_DESTINATARIO).stream().map(Configuracion::getValor).collect(Collectors.toList());
    }

    public void addDestinatarioNotificacionesAdmins(String email) {
        configuracionDao.save(new Configuracion(TipoConfiguracion.NOTIFICACION_ADMIN_DESTINATARIO, email));
    }

    public void deleteDestinatarioNotificacionesAdmins(String email) {
        findAllByClave(TipoConfiguracion.NOTIFICACION_ADMIN_DESTINATARIO).forEach(configuracion -> {
            if (configuracion.getValor().equals(email)) {
                configuracionDao.delete(configuracion);
            }
        });
    }

    public Long getPeriodicidadNotificaciones() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.NOTIFICACION_PERIODICIDAD);
        if (!configuracionList.isEmpty()) {
            //logger.info(configuracionList.get(0).getValor());
            return new Long(configuracionList.get(0).getValor());
        } else {
            return 0L;
        }
    }

    public void setPeriodicidadNotificaciones(Long horas) {
        List<Configuracion> periodicidad = configuracionDao.findAllByClave(TipoConfiguracion.NOTIFICACION_PERIODICIDAD);
        if (periodicidad.isEmpty()) {
            configuracionDao.save(new Configuracion(TipoConfiguracion.NOTIFICACION_PERIODICIDAD, String.valueOf(horas)));
        } else {
            periodicidad.get(0).setValor(String.valueOf(horas));
        }

    }

    public String getMailHost() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.MAIL_HOST);
        if (!configuracionList.isEmpty()) {
            return configuracionList.get(0).getValor();
        } else {
            return null;
        }
    }

    public String getProjectName() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.PROJECT_NAME);
        if (!configuracionList.isEmpty()) {
            return configuracionList.get(0).getValor();
        } else {
            return null;
        }
    }

    public void setProjectName(String proyectName) {
        Optional<Configuracion> configuracion = findAllByClave(TipoConfiguracion.PROJECT_NAME).stream().findFirst();
        if (configuracion.isPresent()) {
            configuracion.get().setValor(proyectName);
        } else {
            save(new Configuracion(TipoConfiguracion.PROJECT_NAME, proyectName));
        }
    }

    public String getFrotendHost() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.FRONTEND_HOST);
        if (!configuracionList.isEmpty()) {
            return configuracionList.get(0).getValor();
        } else {
            return null;
        }
    }

    public void setStringProperty(String property, TipoConfiguracion tipoConfiguracion) {
        Optional<Configuracion> configuracion = findAllByClave(tipoConfiguracion).stream().findFirst();
        if (configuracion.isPresent()) {
            configuracion.get().setValor(property);
        } else {
            save(new Configuracion(tipoConfiguracion, property));
        }
    }

    public String getStringProperty(TipoConfiguracion tipoConfiguracion) {
        List<Configuracion> configuracionList = findAllByClave(tipoConfiguracion);
        if (!configuracionList.isEmpty()) {
            return configuracionList.get(0).getValor();
        } else {
            return null;
        }
    }

    public void ifNullSetStringProperty(TipoConfiguracion tipoConfiguracion, String value) {
        if (configuracionDao.getStringProperty(tipoConfiguracion) == null) {
            configuracionDao.setStringProperty(value, tipoConfiguracion);
        }
    }

    public void setFrontendHost(String frontendHost) {
        Optional<Configuracion> configuracion = findAllByClave(TipoConfiguracion.FRONTEND_HOST).stream().findFirst();
        if (configuracion.isPresent()) {
            configuracion.get().setValor(frontendHost);
        } else {
            save(new Configuracion(TipoConfiguracion.FRONTEND_HOST, frontendHost));
        }
    }

    public String getProjectLogo() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.LOGO_URL);
        if (!configuracionList.isEmpty()) {
            return configuracionList.get(0).getValor();
        } else {
            return null;
        }
    }

    public void setProjectLogo(String proyectName) {
        Optional<Configuracion> configuracion = findAllByClave(TipoConfiguracion.LOGO_URL).stream().findFirst();
        if (configuracion.isPresent()) {
            configuracion.get().setValor(proyectName);
        } else {
            save(new Configuracion(TipoConfiguracion.LOGO_URL, proyectName));
        }
    }

    public void setMailHost(String mailHost) {
        Optional<Configuracion> configuracion = findAllByClave(TipoConfiguracion.MAIL_HOST).stream().findFirst();
        if (configuracion.isPresent()) {
            configuracion.get().setValor(mailHost);
        } else {
            save(new Configuracion(TipoConfiguracion.MAIL_HOST, mailHost));
        }
    }

    public String getMailPort() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.MAIL_PORT);
        if (!configuracionList.isEmpty()) {
            return configuracionList.get(0).getValor();
        } else {
            return null;
        }
    }

    public void setMailPort(String mailPort) {
        Optional<Configuracion> configuracion = findAllByClave(TipoConfiguracion.MAIL_PORT).stream().findFirst();
        if (configuracion.isPresent()) {
            configuracion.get().setValor(mailPort);
        } else {
            save(new Configuracion(TipoConfiguracion.MAIL_PORT, mailPort));
        }
    }

    public String getMailFrom() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.MAIL_FROM);
        if (!configuracionList.isEmpty()) {
            return configuracionList.get(0).getValor();
        } else {
            return null;
        }
    }

    public void setMailFrom(String mailFrom) {
        Optional<Configuracion> configuracion = findAllByClave(TipoConfiguracion.MAIL_FROM).stream().findFirst();
        if (configuracion.isPresent()) {
            configuracion.get().setValor(mailFrom);
        } else {
            save(new Configuracion(TipoConfiguracion.MAIL_FROM, mailFrom));
        }
    }

    public String getMailPass() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.MAIL_PASS);
        if (!configuracionList.isEmpty()) {
            return configuracionList.get(0).getValor();
        } else {
            return null;
        }
    }

    public void setMailPass(String mailPass) {
        Optional<Configuracion> configuracion = findAllByClave(TipoConfiguracion.MAIL_PASS).stream().findFirst();
        if (configuracion.isPresent()) {
            configuracion.get().setValor(mailPass);
        } else {
            save(new Configuracion(TipoConfiguracion.MAIL_PASS, mailPass));
        }
    }

    public String getNodoMaster() {
        List<Configuracion> configuracionList = findAllByClave(TipoConfiguracion.NODO_MASTER);
        if (!configuracionList.isEmpty()) {
            return configuracionList.get(0).getValor();
        } else {
            return null;
        }
    }

    public void setNodoMaster(String nodoMaster) {
        Optional<Configuracion> configuracion = findAllByClave(TipoConfiguracion.NODO_MASTER).stream().findFirst();
        if (configuracion.isPresent()) {
            configuracion.get().setValor(nodoMaster);
        } else {
            save(new Configuracion(TipoConfiguracion.NODO_MASTER, nodoMaster));
        }
    }
}
