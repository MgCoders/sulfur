package coop.magnesium.sulfur.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import coop.magnesium.sulfur.db.entities.Cargo;
import coop.magnesium.sulfur.db.entities.Proyecto;
import io.swagger.annotations.ApiModel;

import java.math.BigDecimal;

/**
 * Created by rsperoni on 19/12/17.
 * Para extraer horas por proyecto y tipoTarea agrupadas por Cargo.
 * Con fila total con cargo en null.
 */
@JsonAutoDetect
@ApiModel
public class ReporteHoras3 {

    public BigDecimal cantidadHoras;
    public BigDecimal precioTotal;
    public Proyecto proyecto;
    public Cargo cargo;

    public ReporteHoras3() {
    }

    public ReporteHoras3(BigDecimal cantidadHoras, BigDecimal precioTotal, Cargo cargo, Proyecto proyecto) {
        this.cantidadHoras = cantidadHoras;
        this.precioTotal = precioTotal;
        this.cargo = cargo;
        this.proyecto = proyecto;
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
        return "ReporteHoras3{" +
                "cantidadHoras=" + cantidadHoras +
                ", precioTotal=" + precioTotal +
                ", proyecto=" + proyecto +
                ", cargo=" + cargo +
                '}';
    }
}
