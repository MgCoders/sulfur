package coop.magnesium.sulfur.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import coop.magnesium.sulfur.db.entities.Cargo;
import coop.magnesium.sulfur.db.entities.Colaborador;
import io.swagger.annotations.ApiModel;

import java.math.BigDecimal;

/**
 * Created by rsperoni on 19/12/17.
 * Para extraer horas por proyecto y tipoTarea agrupadas por Cargo.
 * Con fila total con cargo en null.
 */
@JsonAutoDetect
@ApiModel
public class ReporteHoras2 {

    public BigDecimal cantidadHoras;
    public BigDecimal precioTotal;
    public Colaborador colaborador;
    public Cargo cargo;

    public ReporteHoras2() {
    }

    public ReporteHoras2(BigDecimal cantidadHoras, BigDecimal precioTotal, Cargo cargo, Colaborador colaborador) {
        this.cantidadHoras = cantidadHoras;
        this.precioTotal = precioTotal;
        this.cargo = cargo;
        this.colaborador = colaborador;
    }

    public BigDecimal getCantidadHoras() {
        return cantidadHoras;
    }

    public void setCantidadHoras(BigDecimal cantidadHoras) {
        this.cantidadHoras = cantidadHoras;
    }


    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(BigDecimal precioTotal) {
        this.precioTotal = precioTotal;
    }


    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    @Override
    public String toString() {
        return "ReporteHoras1{" +
                "cantidadHoras=" + cantidadHoras +
                ", precioTotal=" + precioTotal +
                ", cargo=" + ((cargo != null) ? cargo.getCodigo() : "-") +
                '}';
    }
}
