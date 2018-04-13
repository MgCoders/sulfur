package coop.magnesium.sulfur.system;

import coop.magnesium.sulfur.db.dao.CargoDao;
import coop.magnesium.sulfur.db.dao.ConfiguracionDao;
import coop.magnesium.sulfur.db.dao.NotificacionDao;
import coop.magnesium.sulfur.db.entities.Notificacion;
import coop.magnesium.sulfur.utils.Logged;

import javax.ejb.*;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by rsperoni on 22/01/18.
 */
@Stateless
public class NotificationesService {

    @Inject
    Logger logger;
    @EJB
    NotificacionDao notificacionDao;

    @Asynchronous
    @Lock(LockType.READ)
    public void nuevaNotificacionHoras(@Observes(during = TransactionPhase.AFTER_SUCCESS) Notificacion notificacion) {
        notificacionDao.save(notificacion);
        logger.info("# NUEVA NOTIFICACION: "+notificacion.getTipo()+" #");
    }
}
