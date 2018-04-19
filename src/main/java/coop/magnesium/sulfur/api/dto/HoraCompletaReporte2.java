package coop.magnesium.sulfur.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by rsperoni on 17/12/17.
 */
public class HoraCompletaReporte2 {

    public Long colaborador_id;
    public Long cargo_id;
    public Long duracion;
    public LocalDate dia;
    public BigDecimal costo;


    public HoraCompletaReporte2() {
    }

    public HoraCompletaReporte2(Long colaborador_id, Long cargo_id, Long duracion, LocalDate dia) {
        this.colaborador_id = colaborador_id;
        this.cargo_id = cargo_id;
        this.duracion = duracion;
        this.dia = dia;
    }

    @Override
    public String toString() {
        return "HoraCompletaReporte1{" +
                ", colaborador_id=" + colaborador_id +
                ", cargo_id=" + cargo_id +
                ", duracion=" + duracion +
                ", dia=" + dia +
                ", costo=" + costo +
                '}';
    }
}
