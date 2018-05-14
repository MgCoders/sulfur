package coop.magnesium.sulfur.system;

import coop.magnesium.sulfur.db.dao.ConfiguracionDao;
import coop.magnesium.sulfur.db.entities.Notificacion;
import coop.magnesium.sulfur.utils.Logged;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by rsperoni on 17/12/17.
 */
@Singleton
public class MailService {


    @Inject
    Logger logger;
    @Resource(mappedName = "java:/zohoMail")
    private Session mailSession;
    @Inject
    ConfiguracionDao configuracionDao;

    public static String generarEmailRecuperacionClave(String token, String frontendHost, String frontendPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hacé click en el siguiente enlace para recuperar tu contraseña: ").append("\n\n");
        String url = frontendHost + frontendPath + token;
        sb.append(url).append("\n\n");
        return sb.toString();

    }

    public static String generarEmailNuevoUsuario(String frontendHost, String projectName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Se ha creado un nuevo usuario en " + projectName + ": Registro de Horas.").append("\n");
        sb.append("Hacé click en el siguiente enlace para obtener una contraseña.").append("\n\n");
        String url = frontendHost + "/#/extra/forgot-password";
        sb.append(url).append("\n\n");
        return sb.toString();

    }

    public static String generarEmailAviso(Notificacion notificacion) {
        StringBuilder sb = new StringBuilder();
        switch (notificacion.getTipo()) {
            case NUEVA_HORA:
                sb.append(notificacion.getColaborador().getNombre() + " cargó horas de la fecha " + notificacion.getHora().getDia()).append("\n");
                break;
            case FALTAN_HORAS:
                sb.append(notificacion.getColaborador().getNombre() + " no cargó horas en más de 2 días.").append("\n");
                break;
        }
        sb.append("\n\n");
        return sb.toString();

    }

    @PostConstruct
    public void init() {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", configuracionDao.getMailHost());
        props.put("mail.smtp.port", configuracionDao.getMailPort());
        props.setProperty("mail.smtp.startssl.enable", "true");
        props.setProperty("mail.smtps.auth", "true");
        mailSession = Session.getInstance(props);
    }

    @Logged
    @Asynchronous
    @Lock(LockType.READ)
    public void sendMail(@Observes(during = TransactionPhase.AFTER_SUCCESS) MailEvent event) {
        if (configuracionDao.isEmailOn()) {
            try {
                MimeMessage m = new MimeMessage(mailSession);
                m.setFrom(new InternetAddress(configuracionDao.getMailFrom()));
                Address[] to = event.getTo().stream().map(this::toAddress).toArray(InternetAddress[]::new);
                m.setRecipients(Message.RecipientType.TO, to);
                m.setSubject(event.getSubject(), "UTF-8");
                m.setSentDate(new java.util.Date());
                m.setText(event.getMessage(), "UTF-8");
                Transport transport = mailSession.getTransport("smtps");
                transport.connect(configuracionDao.getMailHost(), configuracionDao.getMailFrom(), configuracionDao.getMailPass());
                transport.sendMessage(m, m.getAllRecipients());
                transport.close();
            } catch (MessagingException e) {
                logger.severe(e.getMessage());
            }
        } else {
            logger.info("MAIL OFF");
        }
    }

    private InternetAddress toAddress(String s) {
        try {
            return new InternetAddress(s);
        } catch (AddressException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
