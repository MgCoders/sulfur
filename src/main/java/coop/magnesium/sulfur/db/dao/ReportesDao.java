package coop.magnesium.sulfur.db.dao;

import coop.magnesium.sulfur.api.dto.EstimacionProyectoTipoTareaXCargo;
import coop.magnesium.sulfur.api.dto.ReporteHoras1;
import coop.magnesium.sulfur.api.dto.ReporteHoras2;
import coop.magnesium.sulfur.api.dto.ReporteHoras3;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

/**
 * Created by rsperoni on 28/10/17.
 */
@Stateless
public class ReportesDao {

    @Inject
    CargoDao cargoDao;
    @Inject
    ColaboradorDao colaboradorDao;
    @Inject
    ProyectoDao proyectoDao;
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
    public List<ReporteHoras1> reporteHoras1(Proyecto proyecto, TipoTarea tipoTarea, LocalDate ini, LocalDate fin) {

        //Busco las estimaciones
        Map<Long, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = estimacionDao.findEstimacionProyectoTipoTareaXCargo(proyecto, tipoTarea);


        //Aca va el resultado
        Map<Cargo, ReporteHoras1> reporteXCargo = new HashMap<>();
        //En principio cada reporte/cargo con Zero.
        cargoDao.findAll().forEach(cargo -> reporteXCargo.put(cargo, new ReporteHoras1(BigDecimal.ZERO, estimacionesXCargo.get(cargo.getId()) != null ? estimacionesXCargo.get(cargo.getId()).cantidadHoras : BigDecimal.ZERO, estimacionesXCargo.get(cargo.getId()) != null ? estimacionesXCargo.get(cargo.getId()).precioTotal : BigDecimal.ZERO, BigDecimal.ZERO, proyecto, tipoTarea, cargo)));

        //Aca voy a buscar el precio hora e ir consolidando las diferentes filas con mismo cargo.
        horaDao.findHorasProyectoTipoTareaXCargo(proyecto, tipoTarea, ini, fin).forEach(horaCompleta -> {
            //logger.info(horaCompleta.toString());
            Cargo cargo = cargoDao.findById(horaCompleta.cargo_id);
            BigDecimal costoXHora = horaDao.findPrecioHoraCargo(cargo, horaCompleta.dia);
            BigDecimal cantHoras = TimeUtils.durationToBigDecimal(Duration.ofNanos(horaCompleta.duracion));
            BigDecimal costoHoras = costoXHora.multiply(cantHoras);
            reporteXCargo.get(cargo).cantidadHoras = reporteXCargo.get(cargo).cantidadHoras.add(cantHoras);
            reporteXCargo.get(cargo).precioTotal = reporteXCargo.get(cargo).precioTotal.add(costoHoras);
        });

        //reporteXCargo.entrySet().forEach(cargoReporteHoras1Entry -> logger.info(cargoReporteHoras1Entry.toString()));

        //Resultados en lista
        List<ReporteHoras1> result = new ArrayList<>();
        reporteXCargo.keySet().forEach(cargo ->
                result.add(reporteXCargo.get(cargo)));

        //Ordeno lista por precio hora de cargo al dia de hoy
        result.sort(comparing(reporteHoras1 -> horaDao.findPrecioHoraCargo(reporteHoras1.cargo, LocalDate.now()), reverseOrder()));


        //Armo la fila de totales
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
        //result.forEach(reporteHoras1 -> logger.info(reporteHoras1.toString()));
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
        Map<Long, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = estimacionDao.findEstimacionProyectoXCargo(proyecto);


        //Aca va el resultado
        Map<Cargo, ReporteHoras1> reporteXCargo = new HashMap<>();
        //En principio cada reporte/cargo con Zero.
        cargoDao.findAll().forEach(cargo -> reporteXCargo.put(cargo, new ReporteHoras1(BigDecimal.ZERO, estimacionesXCargo.get(cargo.getId()) != null ? estimacionesXCargo.get(cargo.getId()).cantidadHoras : BigDecimal.ZERO, estimacionesXCargo.get(cargo.getId()) != null ? estimacionesXCargo.get(cargo.getId()).precioTotal : BigDecimal.ZERO, BigDecimal.ZERO, proyecto, null, cargo)));

        //Aca voy a buscar el precio hora e ir consolidando las diferentes filas con mismo cargo.
        horaDao.findHorasProyectoXCargo(proyecto).forEach(horaCompleta -> {
            //logger.info(horaCompleta.toString());
            Cargo cargo = cargoDao.findById(horaCompleta.cargo_id);
            BigDecimal costoXHora = horaDao.findPrecioHoraCargo(cargo, horaCompleta.dia);
            BigDecimal cantHoras = TimeUtils.durationToBigDecimal(Duration.ofNanos(horaCompleta.duracion));
            BigDecimal costoHoras = costoXHora.multiply(cantHoras);
            reporteXCargo.get(cargo).cantidadHoras = reporteXCargo.get(cargo).cantidadHoras.add(cantHoras);
            reporteXCargo.get(cargo).precioTotal = reporteXCargo.get(cargo).precioTotal.add(costoHoras);
        });

        //reporteXCargo.entrySet().forEach(cargoReporteHoras1Entry -> logger.info(cargoReporteHoras1Entry.toString()));

        //Resultados en lista
        List<ReporteHoras1> result = new ArrayList<>();
        reporteXCargo.keySet().forEach(cargo ->
                result.add(reporteXCargo.get(cargo)));

        //Ordeno lista por precio hora de cargo al dia de hoy
        result.sort(comparing(reporteHoras1 -> horaDao.findPrecioHoraCargo(reporteHoras1.cargo, LocalDate.now()), reverseOrder()));


        //Armo la fila de totales
        ReporteHoras1 filaTotal = new ReporteHoras1(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, proyecto, null, null);
        result.stream().reduce((r1, r2) -> new ReporteHoras1(
                r1.cantidadHoras.add(r2.cantidadHoras),
                r1.cantidadHorasEstimadas.add(r2.cantidadHorasEstimadas),
                r1.precioEstimado.add(r2.precioEstimado),
                r1.precioTotal.add(r2.precioTotal),
                r1.proyecto, null, r1.cargo)).ifPresent(reporteHoras1 -> {
            filaTotal.precioTotal = reporteHoras1.precioTotal;
            filaTotal.cantidadHorasEstimadas = reporteHoras1.cantidadHorasEstimadas;
            filaTotal.precioEstimado = reporteHoras1.precioEstimado;
            filaTotal.cantidadHoras = reporteHoras1.cantidadHoras;
        });

        result.add(filaTotal);
        //result.forEach(reporteHoras1 -> logger.info(reporteHoras1.toString()));
        return result;
    }

    /**
     * Reporte de horas por fechas
     */
    public List<ReporteHoras1> reporteHoras2Fechas(LocalDate ini, LocalDate fin) {
        //Busco las estimaciones
        Map<Long, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = estimacionDao.findEstimacionFechasXCargo(ini, fin);


        //Aca va el resultado
        Map<Cargo, ReporteHoras1> reporteXCargo = new HashMap<>();
        cargoDao.findAll().forEach(cargo -> reporteXCargo.put(cargo, new ReporteHoras1(BigDecimal.ZERO, estimacionesXCargo.get(cargo.getId()) != null ? estimacionesXCargo.get(cargo.getId()).cantidadHoras : BigDecimal.ZERO, estimacionesXCargo.get(cargo.getId()) != null ? estimacionesXCargo.get(cargo.getId()).precioTotal : BigDecimal.ZERO, BigDecimal.ZERO, null, null, cargo)));

        //Aca voy a buscar el precio hora e ir consolidando las diferentes filas con mismo cargo.
        horaDao.findHorasByFechasXCargo(ini, fin).forEach(horaCompleta -> {
            //logger.info(horaCompleta.toString());
            Cargo cargo = cargoDao.findById(horaCompleta.cargo_id);
            BigDecimal costoXHora = horaDao.findPrecioHoraCargo(cargo, horaCompleta.dia);
            BigDecimal cantHoras = TimeUtils.durationToBigDecimal(Duration.ofNanos(horaCompleta.duracion));
            BigDecimal costoHoras = costoXHora.multiply(cantHoras);
            reporteXCargo.get(cargo).cantidadHoras = reporteXCargo.get(cargo).cantidadHoras.add(cantHoras);
            reporteXCargo.get(cargo).precioTotal = reporteXCargo.get(cargo).precioTotal.add(costoHoras);
        });


        //Resultados en lista
        List<ReporteHoras1> result = new ArrayList<>();
        reporteXCargo.keySet().forEach(cargo ->
                result.add(reporteXCargo.get(cargo)));

        //Ordeno lista por precio hora de cargo al dia de hoy
        result.sort(comparing(reporteHoras1 -> horaDao.findPrecioHoraCargo(reporteHoras1.cargo, LocalDate.now()), reverseOrder()));


        ReporteHoras1 filaTotal = new ReporteHoras1(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);
        result.stream().reduce((r1, r2) -> new ReporteHoras1(
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
        //result.forEach(reporteHoras2 -> logger.info(reporteHoras2.toString()));
        return result;
    }

    /**
     * Reporte de horas por fechas
     */
    public List<ReporteHoras1> reporteHoras2FechasProyecto(LocalDate ini, LocalDate fin, Proyecto proyecto) {
        //Busco las estimaciones
        Map<Long, EstimacionProyectoTipoTareaXCargo> estimacionesXCargo = estimacionDao.findEstimacionFechasProyectoXCargo(ini, fin, proyecto);


        //Aca va el resultado
        Map<Cargo, ReporteHoras1> reporteXCargo = new HashMap<>();
        cargoDao.findAll().forEach(cargo -> reporteXCargo.put(cargo, new ReporteHoras1(BigDecimal.ZERO, estimacionesXCargo.get(cargo.getId()) != null ? estimacionesXCargo.get(cargo.getId()).cantidadHoras : BigDecimal.ZERO, estimacionesXCargo.get(cargo.getId()) != null ? estimacionesXCargo.get(cargo.getId()).precioTotal : BigDecimal.ZERO, BigDecimal.ZERO, proyecto, null, cargo)));

        //Aca voy a buscar el precio hora e ir consolidando las diferentes filas con mismo cargo.
        horaDao.findHorasByFechasProyectoXCargo(ini, fin, proyecto).forEach(horaCompleta -> {
            //logger.info(horaCompleta.toString());
            Cargo cargo = cargoDao.findById(horaCompleta.cargo_id);
            BigDecimal costoXHora = horaDao.findPrecioHoraCargo(cargo, horaCompleta.dia);
            BigDecimal cantHoras = TimeUtils.durationToBigDecimal(Duration.ofNanos(horaCompleta.duracion));
            BigDecimal costoHoras = costoXHora.multiply(cantHoras);
            reporteXCargo.get(cargo).cantidadHoras = reporteXCargo.get(cargo).cantidadHoras.add(cantHoras);
            reporteXCargo.get(cargo).precioTotal = reporteXCargo.get(cargo).precioTotal.add(costoHoras);
        });


        //Resultados en lista
        List<ReporteHoras1> result = new ArrayList<>();
        reporteXCargo.keySet().forEach(cargo ->
                result.add(reporteXCargo.get(cargo)));

        //Ordeno lista por precio hora de cargo al dia de hoy
        result.sort(comparing(reporteHoras1 -> horaDao.findPrecioHoraCargo(reporteHoras1.cargo, LocalDate.now()), reverseOrder()));


        ReporteHoras1 filaTotal = new ReporteHoras1(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, proyecto, null, null);
        result.stream().reduce((r1, r2) -> new ReporteHoras1(
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
        //result.forEach(reporteHoras2 -> logger.info(reporteHoras2.toString()));
        return result;
    }

    /*
     * Reporte de horas por fechas por Colaborador
     */
    public List<ReporteHoras2> reporteHoras2FechaXColaborador(LocalDate ini, LocalDate fin) {

        //Aca va el resultado
        Map<Long, ReporteHoras2> reporteXColaborador = new HashMap<>();
        colaboradorDao.findAll().stream().filter(colaborador -> colaborador.getCargo() != null).forEach(colaborador -> {
            reporteXColaborador.put(colaborador.getId(), new ReporteHoras2(BigDecimal.ZERO, BigDecimal.ZERO, colaborador.getCargo(), colaborador));
        });

        //Aca voy a buscar el precio hora e ir consolidando las diferentes filas con mismo colaborador.
        horaDao.findHorasByFechasXColaborador(ini, fin).forEach(horaCompleta -> {
            //logger.info(horaCompleta.toString());
            Cargo cargo = reporteXColaborador.get(horaCompleta.colaborador_id).cargo;
            BigDecimal costoXHora = horaDao.findPrecioHoraCargo(cargo, horaCompleta.dia);
            BigDecimal cantHoras = TimeUtils.durationToBigDecimal(Duration.ofNanos(horaCompleta.duracion));
            BigDecimal costoHoras = costoXHora.multiply(cantHoras);
            reporteXColaborador.get(horaCompleta.colaborador_id).cantidadHoras = reporteXColaborador.get(horaCompleta.colaborador_id).cantidadHoras.add(cantHoras);
            reporteXColaborador.get(horaCompleta.colaborador_id).precioTotal = reporteXColaborador.get(horaCompleta.colaborador_id).precioTotal.add(costoHoras);
        });


        //Resultados en lista
        List<ReporteHoras2> result = new ArrayList<>();
        result.addAll(reporteXColaborador.values());

        //Ordeno lista por precio hora de cargo al dia de hoy
        result.sort(comparing(reporteHoras2 -> horaDao.findPrecioHoraCargo(reporteHoras2.cargo, LocalDate.now()), reverseOrder()));


        ReporteHoras2 filaTotal = new ReporteHoras2(BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        result.stream().reduce((r1, r2) -> new ReporteHoras2(
                r1.cantidadHoras.add(r2.cantidadHoras),
                r1.precioTotal.add(r2.precioTotal),
                null, null)).ifPresent(reporteHoras2 -> {
            filaTotal.precioTotal = reporteHoras2.precioTotal;
            filaTotal.cantidadHoras = reporteHoras2.cantidadHoras;
        });

        result.add(filaTotal);
        //result.forEach(reporteHoras2 -> logger.info(reporteHoras2.toString()));
        return result;
    }

    /*
     * Reporte de horas por fechas por Cargo y Proyecto
     */
    public List<ReporteHoras3> reporteHoras3FechaXCargoProyecto(LocalDate ini, LocalDate fin) {

        //Aca va el resultado
        Map<Long, Map<Long, ReporteHoras3>> reporteXCargo = new HashMap<>(); //Clave cargo
        cargoDao.findAll().stream().forEach(cargo -> {
            reporteXCargo.put(cargo.getId(), new HashMap<>()); //Clave proyecto
        });

        //Aca voy a buscar el precio hora e ir consolidando las diferentes filas con mismo proyecto.
        horaDao.findHorasByFechasXCargoProyecto(ini, fin).forEach(horaCompleta -> {
            //logger.info(horaCompleta.toString());
            Cargo cargo = cargoDao.findById(horaCompleta.cargo_id);
            Proyecto proyecto = proyectoDao.findById(horaCompleta.proyecto_id);

            Map<Long, ReporteHoras3> reportesXProyecto = reporteXCargo.get(horaCompleta.cargo_id);
            reportesXProyecto.computeIfAbsent(horaCompleta.proyecto_id, proyecto_id -> reportesXProyecto.put(proyecto_id, new ReporteHoras3(BigDecimal.ZERO, BigDecimal.ZERO, cargo, proyecto)));

            BigDecimal costoXHora = horaDao.findPrecioHoraCargo(cargo, horaCompleta.dia);
            BigDecimal cantHoras = TimeUtils.durationToBigDecimal(Duration.ofNanos(horaCompleta.duracion));
            BigDecimal costoHoras = costoXHora.multiply(cantHoras);
            reportesXProyecto.get(horaCompleta.proyecto_id).cantidadHoras = reportesXProyecto.get(horaCompleta.proyecto_id).cantidadHoras.add(cantHoras);
            reportesXProyecto.get(horaCompleta.proyecto_id).precioTotal = reportesXProyecto.get(horaCompleta.proyecto_id).precioTotal.add(costoHoras);
        });


        //Resultados en lista
        List<ReporteHoras3> result = new ArrayList<>();
        reporteXCargo.entrySet().forEach(entrySet -> {
            Cargo cargo = cargoDao.findById(entrySet.getKey());
            Map<Long, ReporteHoras3> reporteXProyecto = entrySet.getValue();
            //Agrego todos los proyectos para este cargo
            result.addAll(reporteXProyecto.values());
            //Agrego una fila de totales parciales
            if (!reporteXProyecto.values().isEmpty()) {
                ReporteHoras3 filaTotalParcial = new ReporteHoras3(BigDecimal.ZERO, BigDecimal.ZERO, cargo, null);
                reporteXProyecto.values().stream().reduce((r1, r2) -> new ReporteHoras3(
                        r1.cantidadHoras.add(r2.cantidadHoras),
                        r1.precioTotal.add(r2.precioTotal),
                        null, null)).ifPresent(reporteHoras2 -> {
                    filaTotalParcial.precioTotal = reporteHoras2.precioTotal;
                    filaTotalParcial.cantidadHoras = reporteHoras2.cantidadHoras;
                });
                result.add(filaTotalParcial);
            }
        });

        //Ordeno lista por precio hora de cargo al dia de hoy
        result.sort(comparing(reporteHoras2 -> horaDao.findPrecioHoraCargo(reporteHoras2.cargo, LocalDate.now()), reverseOrder()));

        //Filas total
        ReporteHoras3 filaTotal = new ReporteHoras3(BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        result.stream().filter(reporteHoras3 -> reporteHoras3.proyecto == null).reduce((r1, r2) -> new ReporteHoras3(
                r1.cantidadHoras.add(r2.cantidadHoras),
                r1.precioTotal.add(r2.precioTotal),
                null, null)).ifPresent(reporteHoras2 -> {
            filaTotal.precioTotal = reporteHoras2.precioTotal;
            filaTotal.cantidadHoras = reporteHoras2.cantidadHoras;
        });

        result.add(filaTotal);
        return result;
    }


}
