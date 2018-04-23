package coop.magnesium.sulfur.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by rsperoni on 17/12/17.
 */
public class HoraCompletaReporte3 {

    public Long proyecto_id;
    public Long cargo_id;
    public Long duracion;
    public LocalDate dia;
    public BigDecimal costo;


    public HoraCompletaReporte3() {
    }

    public HoraCompletaReporte3(Long proyecto_id, Long cargo_id, Long duracion, LocalDate dia) {
        this.proyecto_id = proyecto_id;
        this.cargo_id = cargo_id;
        this.duracion = duracion;
        this.dia = dia;
    }

    @Override
    public String toString() {
        return "HoraCompletaReporte3{" +
                "proyecto_id=" + proyecto_id +
                ", cargo_id=" + cargo_id +
                ", duracion=" + duracion +
                ", dia=" + dia +
                ", costo=" + costo +
                '}';
    }
}
