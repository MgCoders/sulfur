package coop.magnesium.sulfur.db.dao;

import coop.magnesium.sulfur.api.dto.EstimacionProyectoTipoTareaXCargo;
import coop.magnesium.sulfur.api.dto.ReporteHoras1;
import coop.magnesium.sulfur.api.dto.ReporteHoras2;
import coop.magnesium.sulfur.db.entities.Cargo;
import coop.magnesium.sulfur.db.entities.Proyecto;
import coop.magnesium.sulfur.db.entities.TipoTarea;
import coop.magnesium.sulfur.utils.TimeUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by rsperoni on 28/10/17.
 */
@Stateless
public class ReportesDao {

    @Inject
    CargoDao cargoDao;
    @Inject
    ProyectoDao proyectoDao;
    @Inject
    TipoTareaDao tipoTareaDao;
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
        //Aca voy a buscar el precio hora, no pude con sql. Pueden quedar filas de distintos dias mismo proyecto y tipo de tarea para sumar.
        Map<Cargo, Set<ReporteHoras1>> reporteXCargo = new HashMap<>();
        horaDao.findHorasProyectoTipoTareaXCargo(proyecto, tipoTarea).forEach(horaCompleta -> {
            Cargo cargo = cargoDao.findById(horaCompleta.cargo_id);
            BigDecimal costoXHora = horaDao.findPrecioHoraCargo(cargo, horaCompleta.dia);
            BigDecimal cantHoras = TimeUtils.durationToBigDecimal(Duration.ofNanos(horaCompleta.duracion));
            BigDecimal costoHoras = costoXHora.multiply(cantHoras);
            BigDecimal estimacionHoras = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).cantidadHoras : BigDecimal.ZERO;
            BigDecimal estimacionPrecio = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).precioTotal : BigDecimal.ZERO;
            reporteXCargo.computeIfAbsent(cargo, k -> new HashSet<>());
            reporteXCargo.get(cargo).add(new ReporteHoras1(cantHoras, estimacionHoras, estimacionPrecio, costoHoras, proyecto, tipoTarea, cargo));

        });


        //Resultados
        //Aca tengo que sumar las de arriba
        List<ReporteHoras1> result = new ArrayList<>();
        reporteXCargo.keySet().forEach(cargo ->
                reporteXCargo.get(cargo).stream().reduce((r1, r2) -> new ReporteHoras1(
                        r1.cantidadHoras.add(r2.cantidadHoras),
                        r1.cantidadHorasEstimadas.add(r2.cantidadHorasEstimadas),
                        r1.precioEstimado.add(r2.precioEstimado),
                        r1.precioTotal.add(r2.precioTotal),
                        r1.proyecto, r1.tipoTarea, r1.cargo)).ifPresent(result::add));

        //result.sort(Comparator.comparing(reporteHoras1 -> reporteHoras1.cargo.getPrecioHora(LocalDate.now()).get().getPrecioHora()));


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
        //Aca voy a buscar el precio hora, no pude con sql. Pueden quedar filas de distintos dias mismo proyecto y tipo de tarea para sumar.
        Map<Cargo, Set<ReporteHoras1>> reporteXCargo = new HashMap<>();
        horaDao.findHorasProyectoXCargo(proyecto).forEach(horaCompleta -> {
            Cargo cargo = cargoDao.findById(horaCompleta.cargo_id);
            TipoTarea tipoTarea = tipoTareaDao.findById(horaCompleta.tipoTarea_id);
            BigDecimal costoXHora = horaDao.findPrecioHoraCargo(cargo, horaCompleta.dia);
            BigDecimal cantHoras = TimeUtils.durationToBigDecimal(Duration.ofNanos(horaCompleta.duracion));
            BigDecimal costoHoras = costoXHora.multiply(cantHoras);
            BigDecimal estimacionHoras = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).cantidadHoras : BigDecimal.ZERO;
            BigDecimal estimacionPrecio = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).precioTotal : BigDecimal.ZERO;
            reporteXCargo.computeIfAbsent(cargo, k -> new HashSet<>());
            reporteXCargo.get(cargo).add(new ReporteHoras1(cantHoras, estimacionHoras, estimacionPrecio, costoHoras, proyecto, tipoTarea, cargo));

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

    /**
     * Reporte de horas por fechas
     */
    public List<ReporteHoras2> reporteHoras2Fechas(LocalDate ini, LocalDate fin) {
        //Busco las estimaciones
        Map<Cargo, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = estimacionDao.findEstimacionFechasTipoTareaXCargo(ini, fin);


        //Aca va el resultado
        Map<Cargo, Set<ReporteHoras2>> reporteXCargo = new HashMap<>();
        //Junto todas las horas Detalle separadas por cargo.
        horaDao.findAllByFechas(ini, fin).forEach(hora -> {
            hora.getHoraDetalleList().forEach(horaDetalle -> {
                Cargo cargo = horaDetalle.getCargo();
                BigDecimal costoXHora = horaDao.findPrecioHoraCargo(cargo, hora.getDia());
                BigDecimal cantHoras = TimeUtils.durationToBigDecimal(horaDetalle.getDuracion());
                BigDecimal costoHoras = costoXHora.multiply(cantHoras);
                BigDecimal estimacionHoras = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).cantidadHoras : BigDecimal.ZERO;
                BigDecimal estimacionPrecio = estimacionesXCargo.get(cargo) != null ? estimacionesXCargo.get(cargo).precioTotal : BigDecimal.ZERO;
                reporteXCargo.computeIfAbsent(cargo, k -> new HashSet<>());
                reporteXCargo.get(cargo).add(new ReporteHoras2(cantHoras, estimacionHoras, estimacionPrecio, costoHoras, horaDetalle.getProyecto(), horaDetalle.getTipoTarea(), cargo));
            });
        });

        //Resultados
        List<ReporteHoras2> result = new ArrayList<>();
        reporteXCargo.keySet().forEach(cargo ->
                reporteXCargo.get(cargo).stream().reduce((r1, r2) -> new ReporteHoras2(
                        r1.cantidadHoras.add(r2.cantidadHoras),
                        r1.cantidadHorasEstimadas.add(r2.cantidadHorasEstimadas),
                        r1.precioEstimado.add(r2.precioEstimado),
                        r1.precioTotal.add(r2.precioTotal),
                        r1.proyecto, r1.tipoTarea, r1.cargo)).ifPresent(result::add));

        //result.sort(Comparator.comparing(reporteHoras1 -> reporteHoras1.cargo.getPrecioHora(LocalDate.now()).get().getPrecioHora()));


        ReporteHoras2 filaTotal = new ReporteHoras2(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);
        result.stream().reduce((r1, r2) -> new ReporteHoras2(
                r1.cantidadHoras.add(r2.cantidadHoras),
                r1.cantidadHorasEstimadas.add(r2.cantidadHorasEstimadas),
                r1.precioEstimado.add(r2.precioEstimado),
                r1.precioTotal.add(r2.precioTotal),
                null, null, null)).ifPresent(reporteHoras1 -> {
            filaTotal.precioTotal = reporteHoras1.precioTotal;
            filaTotal.cantidadHorasEstimadas = reporteHoras1.cantidadHorasEstimadas;
            filaTotal.precioEstimado = reporteHoras1.precioEstimado;
            filaTotal.cantidadHoras = reporteHoras1.cantidadHoras;
        });

        result.add(filaTotal);
        return result;
    }


}
