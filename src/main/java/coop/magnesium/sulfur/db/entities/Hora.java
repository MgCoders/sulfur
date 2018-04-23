package coop.magnesium.sulfur.db.entities;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import coop.magnesium.sulfur.api.dto.HoraCompletaReporte1;
import coop.magnesium.sulfur.api.dto.HoraCompletaReporte2;
import coop.magnesium.sulfur.api.dto.HoraCompletaReporte3;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsperoni on 17/11/17.
 */
@Entity
@JsonAutoDetect
@ApiModel
@SqlResultSetMappings({
        @SqlResultSetMapping(name = "HoraCompletaReporte1", classes = {
                @ConstructorResult(targetClass = HoraCompletaReporte1.class,
                        columns = {
                                @ColumnResult(name = "proyecto_id", type = Long.class),
                                @ColumnResult(name = "tipotarea_id", type = Long.class),
                                @ColumnResult(name = "colaborador_id", type = Long.class),
                                @ColumnResult(name = "cargo_id", type = Long.class),
                                @ColumnResult(name = "duracion", type = Long.class),
                                @ColumnResult(name = "dia", type = LocalDate.class)
                        })

        }),
        @SqlResultSetMapping(name = "HoraCompletaReporte2", classes = {
                @ConstructorResult(targetClass = HoraCompletaReporte2.class,
                        columns = {
                                @ColumnResult(name = "colaborador_id", type = Long.class),
                                @ColumnResult(name = "cargo_id", type = Long.class),
                                @ColumnResult(name = "duracion", type = Long.class),
                                @ColumnResult(name = "dia", type = LocalDate.class)
                        })

        }),
        @SqlResultSetMapping(name = "HoraCompletaReporte3", classes = {
                @ConstructorResult(targetClass = HoraCompletaReporte3.class,
                        columns = {
                                @ColumnResult(name = "proyecto_id", type = Long.class),
                                @ColumnResult(name = "cargo_id", type = Long.class),
                                @ColumnResult(name = "duracion", type = Long.class),
                                @ColumnResult(name = "dia", type = LocalDate.class)
                        })

        })
})
public class Hora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @ApiModelProperty(dataType = "date", example = "23-01-2017")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate dia;
    @NotNull
    @ApiModelProperty(dataType = "dateTime", example = "08:15")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime horaIn;
    @NotNull
    @ApiModelProperty(dataType = "dateTime", example = "17:34")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime horaOut;
    private Duration subtotal;
    @NotNull
    @ManyToOne
    private Colaborador colaborador;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "hora_id")
    private Set<HoraDetalle> horaDetalleList = new HashSet<>();
    private boolean completa = false;
    private Duration subtotalDetalles;

    public Hora() {
    }

    public Hora(LocalDate dia, LocalTime horaIn, LocalTime horaOut, Colaborador colaborador) {
        this.dia = dia;
        this.horaIn = horaIn;
        this.horaOut = horaOut;
        this.colaborador = colaborador;
        this.completa = false;
    }

    @PrePersist
    @PreUpdate
    public void calcularSubtotal() {
        this.subtotal = Duration.between(this.horaIn, this.horaOut);
        this.completa = (this.subtotal != null && this.subtotalDetalles != null) && (this.subtotal.compareTo(this.subtotalDetalles) == 0);
    }

    /**
     * Campos calculados
     * Debe ejecutarse antes de guardar o editar.
     */
    public void cacularSubtotalDetalle() {
        this.subtotalDetalles = Duration.ofMillis(this.getHoraDetalleList().stream().map(HoraDetalle::getDuracion).mapToLong(Duration::toMillis).sum());
    }


    @JsonProperty
    public boolean isCompleta() {
        return completa;
    }

    @JsonIgnore
    public void setCompleta(boolean completa) {
        this.completa = completa;
    }

    public Set<HoraDetalle> getHoraDetalleList() {
        return horaDetalleList;
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDia() {
        return dia;
    }

    public void setDia(LocalDate dia) {
        this.dia = dia;
    }

    public LocalTime getHoraIn() {
        return horaIn;
    }

    public void setHoraIn(LocalTime horaIn) {
        this.horaIn = horaIn;
    }

    public LocalTime getHoraOut() {
        return horaOut;
    }

    public void setHoraOut(LocalTime horaOut) {
        this.horaOut = horaOut;
    }

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "'PT'HH'H'MM'M'")
    @ApiModelProperty(example = "PT23H59M", dataType = "dateTime")
    @JsonDeserialize(using = DurationDeserializer.class)
    @JsonSerialize(using = DurationSerializer.class)
    public Duration getSubtotal() {
        return subtotal;
    }

    @JsonIgnore
    public void setSubtotal(Duration subtotal) {
        this.subtotal = subtotal;
    }

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "'PT'HH'H'MM'M'")
    @ApiModelProperty(example = "PT23H59M", dataType = "dateTime")
    @JsonDeserialize(using = DurationDeserializer.class)
    @JsonSerialize(using = DurationSerializer.class)
    public Duration getSubtotalDetalles() {
        return subtotalDetalles;
    }

    @JsonIgnore
    public void setSubtotalDetalles(Duration subtotalDetalles) {
        this.subtotalDetalles = subtotalDetalles;
    }

    @Override
    public String toString() {
        return "Hora{" +
                "id=" + id +
                ", dia=" + dia +
                ", horaIn=" + horaIn +
                ", horaOut=" + horaOut +
                ", subtotal=" + subtotal +
                ", colaborador=" + colaborador +
                ", horaDetalleList=" + horaDetalleList +
                ", completa=" + completa +
                ", subtotalDetalles=" + subtotalDetalles +
                '}';
    }
}
