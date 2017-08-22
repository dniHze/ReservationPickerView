package me.dnihze.reservationpicker.utils

import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit

/**
 * Utils for operations with date offsets.
 *
 * @since 0.1.0
 * @author Artyom Dorosh (a.dorosh@seductive.com.ua)
 */
internal object CalendarUtils {

    /**
     * Get default item range of dates.
     *
     * @since 0.1.0
     *
     * @return default item range of 91 date for date picker.
     */
    fun getDefaultItemRange(): List<LocalDate> {
        val startFrom = LocalDate.now().minus(45, ChronoUnit.DAYS)
        return (0..90).map { startFrom.plus(it.toLong(), ChronoUnit.DAYS) }
    }

    /**
     * Get date range which has to be added to the to the end of the date selector.
     *
     * @since 0.1.0
     *
     * @param from end date in current date selector.
     *
     * @return item range of dates to be add to end of current date picker list.
     */
    fun getRangeFrom(from: LocalDate): List<LocalDate> {
        val startFrom = from.plus(1, ChronoUnit.DAYS)
        return (0..90).map { startFrom.plus(it.toLong(), ChronoUnit.DAYS) }
    }

    /**
     * Get date range which has to be added to the to the start of the date selector.
     *
     * @since 0.1.0
     *
     * @param to start date in current date selector.
     *
     * @return item range of dates to be add to start of current date picker list.
     */
    fun getRangeTo(to: LocalDate): List<LocalDate> {
        val startFrom = to.minus(91, ChronoUnit.DAYS)
        return (0..90).map { startFrom.plus(it.toLong(), ChronoUnit.DAYS) }
    }

    /**
     * Get all the dates between specific dates.
     *
     * @since 0.1.0
     *
     * @param from start date of range.
     * @param to end date of range.
     *
     * @return list of dates in specified range.
     */
    fun getDatesFromTo(from: LocalDate, to: LocalDate): List<LocalDate> {
        var newDate = from
        val list = mutableListOf<LocalDate>()
        while (!newDate.isAfter(to)) {
            list.add(newDate)
            newDate = newDate.plus(1, ChronoUnit.DAYS)
        }
        return list
    }

    /**
     * As the date picker contains only limited amount of dates, additional dates added
     * to the picker when the user dragging it, there is a big possibility of case, when user can
     * request date picker to show date, which is out of picker internal date range.
     * If the requested date is out of range and after the end of the date range, view calls this
     * method to request missing dates range updates picker with it.
     *
     * Also notice, that requested range contains extra 20 dates as a `display buffer` for picker.
     *
     * @since 0.1.0
     *
     * @param endOfList end date of current display list.
     * @param requestedDate requested date to scroll to.
     *
     * @return requested missing range of dates in picker.
     */
    fun getDatesForRightRange(endOfList: LocalDate, requestedDate: LocalDate): List<LocalDate> {
        val last = requestedDate.plus(20, ChronoUnit.DAYS)
        var newDate = endOfList.plus(1, ChronoUnit.DAYS)
        val list = mutableListOf<LocalDate>()
        while (!newDate.isAfter(last)) {
            list.add(newDate)
            newDate = newDate.plus(1, ChronoUnit.DAYS)
        }
        return list
    }

    /**
     * As the date picker contains only limited amount of dates, additional dates are added
     * to the picker when the user dragging it, there is a big possibility of case, when user can
     * request date picker to show date, which is out of picker internal date range.
     * If the requested date is out of range and before the start of the date range, view calls this
     * method to request missing dates range of dates and updates picker with it.
     *
     * Also notice, that requested range contains extra 20 dates as a `display buffer` for picker.
     *
     * @since 0.1.0
     *
     * @param startOfList start date of current display list.
     * @param requestedDate requested date to scroll to.
     *
     * @return requested missing range of dates in picker.
     */
    fun getDatesForLeftRange(startOfList: LocalDate, requestedDate: LocalDate): List<LocalDate> {
        val last = startOfList.minus(1, ChronoUnit.DAYS)
        var newDate = requestedDate.minus(20, ChronoUnit.DAYS)
        val list = mutableListOf<LocalDate>()
        while (!newDate.isAfter(last)) {
            list.add(newDate)
            newDate = newDate.plus(1, ChronoUnit.DAYS)
        }
        return list
    }
}