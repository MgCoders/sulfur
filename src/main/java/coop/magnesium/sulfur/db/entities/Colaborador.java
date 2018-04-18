package coop.magnesium.sulfur.db.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsperoni on 16/11/17.
 */
@Entity
@JsonAutoDetect
public class Colaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "El campo email no puede estar vacío")
    @Column(unique = true)
    private String email;
    @NotNull(message = "El campo nombre no puede estar vacío")
    private String nombre;
    @Valid
    @NotNull(message = "El cargo no puede ser vacío")
    @ManyToOne
    private Cargo cargo;
    private String password;
    @NotNull(message = "El campo rol no puede estar vacío")
    private String role = Role.USER.name();
    @Transient
    private String token;
    private Boolean enabled = true;

    public Colaborador() {
    }

    public Colaborador(String email, String nombre, Cargo cargo, String password, String role) {
        this.email = email;
        this.nombre = nombre;
        this.cargo = cargo;
        this.password = password;
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @JsonProperty
    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Colaborador{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nombre='" + nombre + '\'' +
                ", cargo=" + cargo +
                ", role='" + role + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
