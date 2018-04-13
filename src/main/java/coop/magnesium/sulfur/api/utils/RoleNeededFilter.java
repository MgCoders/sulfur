package coop.magnesium.sulfur.api.utils;

import coop.magnesium.sulfur.db.entities.Role;
import coop.magnesium.sulfur.db.entities.SulfurUser;
import coop.magnesium.sulfur.utils.KeyGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.security.Key;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by rsperoni on 05/05/17.
 */
@Provider
@JWTTokenNeeded
@Priority(Priorities.AUTHORIZATION)
public class RoleNeededFilter implements ContainerRequestFilter {

    @Inject
    private KeyGenerator keyGenerator;

    @Inject
    private Logger logger;

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {

        // Get the resource class which matches with the requested URL
        // Extract the roles declared by it
        Class<?> resourceClass = resourceInfo.getResourceClass();
        List<Role> classRoles = extractRoles(resourceClass);

        // Get the resource method which matches with the requested URL
        // Extract the roles declared by it
        Method resourceMethod = resourceInfo.getResourceMethod();
        List<Role> methodRoles = extractRoles(resourceMethod);

        //Lista de roles permitidos
        List<Role> rolesPermitidos = methodRoles;
        if (rolesPermitidos.isEmpty()) {
            rolesPermitidos = classRoles;
        }

        //Saco del token
        String authorizationHeader = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        String token = authorizationHeader.substring("Bearer".length()).trim();


        try {
            //Verifico roles que tiene el usuario
            Key key = keyGenerator.generateKey();
            Jws<Claims> claimsJws = Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token);

            Role rolUsuario = Role.valueOf((String) claimsJws.getBody().get("role"));
            Long idColaborador = Long.valueOf((String) claimsJws.getBody().get("id"));
            logger.info("##### ROL: " + rolUsuario.name() + " #####");

            if (!rolesPermitidos.contains(rolUsuario)) {
                throw new Exception("Rol incorrecto");
            }

            logger.info("#### AUTORIZADO: "+idColaborador+"####");
            containerRequestContext.setSecurityContext(new Authorizer(idColaborador, rolUsuario.name()));

        } catch (Exception e) {
            logger.severe("#### NO AUTORIZADO ####");
            containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build());
        }

    }

    // Extract the roles from the annotated element
    private List<Role> extractRoles(AnnotatedElement annotatedElement) {
        if (annotatedElement == null) {
            return new ArrayList<Role>();
        } else {
            RoleNeeded secured = annotatedElement.getAnnotation(RoleNeeded.class);
            if (secured == null) {
                return new ArrayList<Role>();
            } else {
                Role[] allowedRoles = secured.value();
                return Arrays.asList(allowedRoles);
            }
        }
    }

    public static class Authorizer implements SecurityContext {

        private SulfurUser sulfurUser;

        public Authorizer(Long colaboradorId, String role) {
            this.sulfurUser = new SulfurUser(colaboradorId, role);
        }

        @Override
        public Principal getUserPrincipal() {
            return this.sulfurUser;
        }

        @Override
        public boolean isUserInRole(String s) {
            return sulfurUser.getRole().equals(s);
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getAuthenticationScheme() {
            return null;
        }
    }
}
