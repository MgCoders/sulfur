package coop.magnesium.sulfur.db.dao;

import coop.magnesium.sulfur.api.dto.EstimacionProyectoTipoTareaXCargo;
import coop.magnesium.sulfur.api.dto.ReporteHoras1;
import coop.magnesium.sulfur.db.entities.Cargo;
import coop.magnesium.sulfur.db.entities.Proyecto;
import coop.magnesium.sulfur.db.entities.TipoTarea;
import coop.magnesium.sulfur.utils.TimeUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by rsperoni on 28/10/17.
 */
@Stateless
public class ReportesDao {

    @EJB
    private EstimacionDao estimacionDao;
    @EJB
    private HoraDao horaDao;
    @Inject
    private Logger logger;


    /**
     * Reporte de horas por proyecto y tipo de tarea agrupada por cargo.
     *
     * @param proyecto
     * @param tipoTarea
     * @return
     */
    public List<ReporteHoras1> reporteHoras1(Proyecto proyecto, TipoTarea tipoTarea) {

        //Busco las estimaciones
        Map<Cargo, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = estimacionDao.findEstimacionProyectoTipoTareaXCargo(proyecto, tipoTarea);


        //Aca va el resultado
        Map<Cargo, Set<ReporteHoras1>> reporteXCargo = new HashMap<>();
        //Junto todas las horas Detalle separadas por cargo.
        horaDao.findAll().forEach(hora -> {
            Cargo cargo = hora.getColaborador().getCargo();
            reporteXCargo.computeIfAbsent(cargo, k -> new HashSet<>());
            hora.getHoraDetalleList().forEach(horaDetalle -> {
                if (horaDetalle.getProyecto().equals(proyecto) && horaDetalle.getTipoTarea().equals(tipoTarea)) {
                    BigDecimal costoXHora = cargo.getPrecioHora(hora.getDia()).get().getPrecioHora();
                    BigDecimal cantHoras = TimeUtils.durationToBigDecimal(horaDetalle.getDuracion());
                    BigDecimal costoHoras = costoXHora.multiply(cantHoras);
                    BigDecimal estimacionHoras = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).cantidadHoras : BigDecimal.ZERO;
                    BigDecimal estimacionPrecio = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).precioTotal : BigDecimal.ZERO;
                    reporteXCargo.get(cargo).add(new ReporteHoras1(cantHoras, estimacionHoras, estimacionPrecio, costoHoras, horaDetalle.getProyecto(), horaDetalle.getTipoTarea(), cargo));
                }
            });
        });

        //Resultados
        List<ReporteHoras1> result = new ArrayList<>();
        reporteXCargo.keySet().forEach(cargo ->
                reporteXCargo.get(cargo).stream().reduce((r1, r2) -> new ReporteHoras1(
                        r1.cantidadHoras.add(r2.cantidadHoras),
                        r1.cantidadHorasEstimadas.add(r2.cantidadHorasEstimadas),
                        r1.precioEstimado.add(r2.precioEstimado),
                        r1.precioTotal.add(r2.precioTotal),
                        r1.proyecto, r1.tipoTarea, r1.cargo)).ifPresent(result::add));


        ReporteHoras1 filaTotal = new ReporteHoras1(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, proyecto, tipoTarea, null);
        result.stream().reduce((r1, r2) -> new ReporteHoras1(
                r1.cantidadHoras.add(r2.cantidadHoras),
                r1.cantidadHorasEstimadas.add(r2.cantidadHorasEstimadas),
                r1.precioEstimado.add(r2.precioEstimado),
                r1.precioTotal.add(r2.precioTotal),
                r1.proyecto, r1.tipoTarea, r1.cargo)).ifPresent(reporteHoras1 -> {
            filaTotal.precioTotal = reporteHoras1.precioTotal;
            filaTotal.cantidadHorasEstimadas = reporteHoras1.cantidadHorasEstimadas;
            filaTotal.precioEstimado = reporteHoras1.precioEstimado;
            filaTotal.cantidadHoras = reporteHoras1.cantidadHoras;
        });

        result.add(filaTotal);
        return result;
    }

    /**
     * Reporte de horas por proyecto agrupado por cargo.
     *
     * @param proyecto
     * @return
     */
    public List<ReporteHoras1> reporteHoras1Totales(Proyecto proyecto) {
//Busco las estimaciones
        Map<Cargo, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = estimacionDao.findEstimacionProyectoXCargo(proyecto);


        //Aca va el resultado
        Map<Cargo, Set<ReporteHoras1>> reporteXCargo = new HashMap<>();
        //Junto todas las horas Detalle separadas por cargo.
        horaDao.findAll().forEach(hora -> {
            Cargo cargo = hora.getColaborador().getCargo();
            reporteXCargo.computeIfAbsent(cargo, k -> new HashSet<>());
            hora.getHoraDetalleList().forEach(horaDetalle -> {
                if (horaDetalle.getProyecto().equals(proyecto)) {
                    BigDecimal costoXHora = cargo.getPrecioHora(hora.getDia()).get().getPrecioHora();
                    BigDecimal cantHoras = TimeUtils.durationToBigDecimal(horaDetalle.getDuracion());
                    BigDecimal costoHoras = costoXHora.multiply(cantHoras);
                    BigDecimal estimacionHoras = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).cantidadHoras : BigDecimal.ZERO;
                    BigDecimal estimacionPrecio = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).precioTotal : BigDecimal.ZERO;
                    reporteXCargo.get(cargo).add(new ReporteHoras1(cantHoras, estimacionHoras, estimacionPrecio, costoHoras, horaDetalle.getProyecto(), horaDetalle.getTipoTarea(), cargo));
                }
            });
        });

        //Resultados
        List<ReporteHoras1> result = new ArrayList<>();
        reporteXCargo.keySet().forEach(cargo ->
                reporteXCargo.get(cargo).stream().reduce((r1, r2) -> new ReporteHoras1(
                        r1.cantidadHoras.add(r2.cantidadHoras),
                        r1.cantidadHorasEstimadas.add(r2.cantidadHorasEstimadas),
                        r1.precioEstimado.add(r2.precioEstimado),
                        r1.precioTotal.add(r2.precioTotal),
                        r1.proyecto, null, r1.cargo)).ifPresent(result::add));


        ReporteHoras1 filaTotal = new ReporteHoras1(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, proyecto, null, null);
        result.stream().reduce((r1, r2) -> new ReporteHoras1(
                r1.cantidadHoras.add(r2.cantidadHoras),
                r1.cantidadHorasEstimadas.add(r2.cantidadHorasEstimadas),
                r1.precioEstimado.add(r2.precioEstimado),
                r1.precioTotal.add(r2.precioTotal),
                r1.proyecto, r1.tipoTarea, r1.cargo)).ifPresent(reporteHoras1 -> {
            filaTotal.precioTotal = reporteHoras1.precioTotal;
            filaTotal.cantidadHorasEstimadas = reporteHoras1.cantidadHorasEstimadas;
            filaTotal.precioEstimado = reporteHoras1.precioEstimado;
            filaTotal.cantidadHoras = reporteHoras1.cantidadHoras;
        });

        result.add(filaTotal);
        return result;
    }


}