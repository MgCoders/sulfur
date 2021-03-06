package coop.magnesium.sulfur.system;

import coop.magnesium.sulfur.db.dao.*;
import coop.magnesium.sulfur.db.entities.Notificacion;
import coop.magnesium.sulfur.db.entities.RecuperacionPassword;
import coop.magnesium.sulfur.db.entities.TipoConfiguracion;
import coop.magnesium.sulfur.db.entities.TipoNotificacion;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by rsperoni on 18/11/17.
 */
@Singleton
@Startup
public class StartupBean {


    @Inject
    Logger logger;
    @EJB
    HoraDao horaDao;
    @EJB
    ColaboradorDao colaboradorDao;
    @Inject
    ConfiguracionDao configuracionDao;
    @Inject
    String jbossNodeName;
    @Inject
    RecuperacionPasswordDao recuperacionPasswordDao;
    @Inject
    NotificacionDao notificacionDao;
    @Inject
    Event<MailEvent> mailEvent;
    @Inject
    Event<Notificacion> notificacionEvent;

    @PostConstruct
    public void init() {
        System.setProperty("user.timezone", "America/Montevideo");
        logger.warning("FECHA HORA DE JVM: " + LocalDateTime.now());

        /*try {
            if (colaboradorDao.findByEmail("info@magnesium.coop") == null) {
                colaboradorDao.save(new Colaborador("info@magnesium.coop", "root", null, PasswordUtils.digestPassword(UUID.randomUUID().toString()), "ADMIN"));
            }
        } catch (MagnesiumBdMultipleResultsException e) {
            logger.warning(e.getMessage());
        }*/
        configuraciones();
        setMyselfAsNodoMaster();

    }

    public void setMyselfAsNodoMaster() {
        configuracionDao.setNodoMaster(jbossNodeName);

    }

    public void configuraciones() {
        if (!configuracionDao.isEmailOn()) {
            configuracionDao.setMailOn(false);
        }
        if (configuracionDao.getPeriodicidadNotificaciones().equals(0L)) {
            configuracionDao.setPeriodicidadNotificaciones(48L);
        }
        if (configuracionDao.getDestinatariosNotificacionesAdmins().isEmpty()) {
            configuracionDao.addDestinatarioNotificacionesAdmins("info@magnesium.coop");
        }
        if (configuracionDao.getMailFrom() == null) {
            configuracionDao.setMailFrom("no-reply@mm.com");
        }
        if (configuracionDao.getMailHost() == null) {
            configuracionDao.setMailPort("1025");
        }
        if (configuracionDao.getMailPort() == null) {
            configuracionDao.setMailHost("ip-172-31-6-242");
        }
        if (configuracionDao.getProjectName() == null) {
            configuracionDao.setProjectName("MMMM");
        }
        if (configuracionDao.getProjectLogo() == null) {
            configuracionDao.setProjectLogo("https://fffff.com");
        }
        configuracionDao.ifNullSetStringProperty(TipoConfiguracion.FRONTEND_HOST, "https://fffff.com");
        configuracionDao.ifNullSetStringProperty(TipoConfiguracion.FRONTEND_PATH, "/#/extra/new-password?token=");
        configuracionDao.ifNullSetStringProperty(TipoConfiguracion.REST_BASE_PATH, "api");
    }

    public void putRecuperacionPassword(RecuperacionPassword recuperacionPassword) {
        recuperacionPasswordDao.save(recuperacionPassword);
    }

    public RecuperacionPassword getRecuperacionInfo(String token) {
        RecuperacionPassword recuperacionPassword = recuperacionPasswordDao.findById(token);
        if (recuperacionPassword != null && recuperacionPassword.getExpirationDate().isAfter(LocalDateTime.now())) {
            return recuperacionPassword;
        } else {
            recuperacionPasswordDao.delete(token);
            return null;
        }
    }

    @Schedule(hour = "*/24", info = "cleanRecuperacionContrasena", persistent = false)
    public void cleanRecuperacionContrasena() {
        //Solo si soy master
        if (configuracionDao.getNodoMaster().equals(jbossNodeName)) {
            logger.info("Master cleaning Recuperacion Contraseña");
            recuperacionPasswordDao.findAll().forEach(recuperacionPassword -> {
                if (recuperacionPassword.getExpirationDate().isBefore(LocalDateTime.now())) {
                    recuperacionPasswordDao.delete(recuperacionPassword);
                }
            });
        }
    }

    @Schedule(dayOfWeek = "Mon,Wed,Fri", hour = "9", info = "alertaHorasSinCargar", persistent = false)
    public void alertaHorasSinCargar() {
        //Solo si soy master
        if (configuracionDao.getNodoMaster().equals(jbossNodeName)) {
            logger.info("Master generando notificaciones");
            LocalDate hoy = LocalDate.now();
            //Solo días de semana
            if (!hoy.getDayOfWeek().equals(DayOfWeek.SATURDAY) && !hoy.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
                colaboradorDao.findAll().stream().filter(colaborador -> colaborador.getCargo() != null)
                        .forEach(colaborador -> {
                            //Si hace más de 3 días que no hay horas
                            if (horaDao.findAllByColaborador(colaborador, LocalDate.now().minusDays(3), LocalDate.now()).isEmpty()) {
                                notificacionEvent.fire(new Notificacion(TipoNotificacion.FALTAN_HORAS, colaborador, colaborador.getNombre() + " no cargó horas en más de 2 días."));
                            }
                        });
            }
        }

    }

    @Schedule(dayOfWeek = "Mon,Wed,Fri", hour = "9", minute = "30", info = "enviarMailsConNotificaciones", persistent = false)
    public void enviarMailsConNotificaciones() {
        //Solo si soy master
        if (configuracionDao.getNodoMaster().equals(jbossNodeName)) {
            logger.info("Master enviando mails");
            StringBuilder stringBuilder = new StringBuilder();
            notificacionDao.findAllNoEnviadas().stream()
                    .filter(notificacion -> notificacion.getTipo().equals(TipoNotificacion.FALTAN_HORAS))
                    .forEach(notificacion -> {
                stringBuilder.append("- ").append(notificacion.getTexto()).append("\n");
                notificacion.setEnviado(true);
            });

            String projectName = configuracionDao.getProjectName();
            List<String> mailsAdmins = configuracionDao.getDestinatariosNotificacionesAdmins();
            if (!stringBuilder.toString().isEmpty()) {
                mailEvent.fire(
                        new MailEvent(mailsAdmins,
                                stringBuilder.toString(),
                                projectName + ": Notificaciones"));
            }
        }

    }

    @Schedule(hour = "*/72", info = "limpiarNotificacionesAntiguas", persistent = false)
    public void limpiarNotificacionesAntiguas(){
        //Solo si soy master
        if (configuracionDao.getNodoMaster().equals(jbossNodeName)) {
            logger.info("Master limpiando notificaciones antiguas");
            notificacionDao.findAll(LocalDateTime.now().minusDays(100), LocalDateTime.now().minusDays(30)).forEach(notificacion -> notificacionDao.delete(notificacion));
        }
    }


}
