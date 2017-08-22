package me.dnihze.reservationpicker.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import me.dnihze.reservationpicker.R
import me.dnihze.reservationpicker.model.Price
import me.dnihze.reservationpicker.utils.CalendarUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat

/**
 * Reservation picker slot view.
 *
 * @since 0.1.0
 * @author Artyom Dorosh (a.dorosh@seductive.com.ua)
 */
@Suppress("unused")
class ReservationPickerView : FrameLayout {

    private val timeData: MutableMap<LocalDate, List<LocalDateTime>> = mutableMapOf()
    private val priceData: MutableMap<LocalDate, Price> = mutableMapOf()

    private val rvDays: RecyclerView
    private val rvHours: RecyclerView
    private val layoutHours: View

    private var stateRestored = false
    private var lockScrollListener = false

    private var restoredScrollPosition = -1

    private val daysAdapter: DaysAdapter
    private val hoursAdapter: HourAdapter

    private var selectedDate: LocalDate? = null
    private var selectedLocalDateTime: LocalDateTime? = null

    private val dateSelectedListeners: MutableSet<OnDateSelectedListener> = mutableSetOf()
    private val timeSelectedListeners: MutableSet<OnDateTimeSelectedListener> = mutableSetOf()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        // Enable view save state
        isSaveEnabled = true
        View.inflate(context, R.layout.full_view, this)

        // Set all id's
        rvDays = findViewById(R.id.days_list)
        rvHours = findViewById(R.id.hours_list)
        layoutHours = findViewById(R.id.hours)

        // Init all adapters
        daysAdapter = DaysAdapter()
        hoursAdapter = HourAdapter()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        rvDays.adapter = daysAdapter
        rvHours.adapter = hoursAdapter

        if (!stateRestored) {
            // Create mock data
            timeData.put(LocalDate.now(), listOf(
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 30)),
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0)),
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 30)),
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0)),
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 30)),
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 0)),
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 30)),
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(13, 45)),
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(20, 15))))
            priceData.put(LocalDate.now(), Price(8.30, "$"))
            // Scroll to today
            val position = daysAdapter.getIndexOfDate(LocalDate.now())
            rvDays.scrollToPosition(position)
        } else {
            // Scroll to restored position
            if (restoredScrollPosition >= 0)
                rvDays.scrollToPosition(restoredScrollPosition)
        }

        rvDays.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            /**
             * Callback method to be invoked when the RecyclerView has been scrolled. This will be
             * called after the scroll has completed.
             *
             *
             * This callback will also be called if visible item range changes after a layout
             * calculation. In that case, dx and dy will be 0.

             * @param recyclerView The RecyclerView which scrolled.
             * *
             * @param dx The amount of horizontal scroll.
             * *
             * @param dy The amount of vertical scroll.
             */
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                (recyclerView.layoutManager as? LinearLayoutManager)?.let {
                    // There is no point to request another update if the list is currently updating
                    if (lockScrollListener)
                        return@let
                    // Find first and last visible positions for user right now
                    val first = it.findFirstVisibleItemPosition()
                    val last = it.findLastVisibleItemPosition()
                    // If there is less then 10 items to the start, add some more items
                    // and notify adapter about items insertion
                    //
                    // Otherwise, if there are less then 10 items at the end of the list,
                    // add some more items to the end.
                    if (first <= 10) {
                        // Lock updates requests
                        lockScrollListener = true
                        val firstDate = daysAdapter.getStartDate()
                        // Update all on recycler
                        recyclerView.post {
                            // Add some data to the start of list
                            daysAdapter.putItemsToStart(CalendarUtils.getRangeTo(firstDate))
                            // Unlock updates
                            lockScrollListener = false
                        }
                    } else if (last >= daysAdapter.itemCount - 10) {
                        // Lock updates
                        lockScrollListener = true
                        val lastDate = daysAdapter.getEndDate()
                        recyclerView.post {
                            // Add some data to the end of list
                            daysAdapter.putItemsToEnd(CalendarUtils.getRangeFrom(lastDate))
                            // Unlock updates
                            lockScrollListener = false
                        }
                    }
                }
            }
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        // If user already specified "height" as "wrap_content", set view height to 148dp.
        // According to design guidelines. Otherwise set the size requested by user.
        if (mode == MeasureSpec.AT_MOST) {
            val height = dpToPx(148)
            val hSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            super.onMeasure(widthMeasureSpec, hSpec)
        } else
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        // Get current scroll position
        val scrollPosition = (rvDays.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: -1
        // Add all needed stuff to parcelable entity
        val savedState = SavedState(timeData, priceData, selectedDate, selectedLocalDateTime,
                daysAdapter.getStartDate(), daysAdapter.getEndDate(), scrollPosition, superState)
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        // Set flag to `true`, which means that view is restored, not initially created on user screen.
        stateRestored = true
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            // Restore all data.
            selectedDate = state.selectedDate
            selectedLocalDateTime = state.selectedLocalDateTime
            setPriceData(state.priceData)
            // Set proper view data with specified methods
            setTimeAvailabilityData(state.timeData)
            daysAdapter.setRangeOnOwn(CalendarUtils.getDatesFromTo(state.start, state.end))
            // Set last known position
            restoredScrollPosition = state.scrollPosition
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    // View public methods

    /**
     * Add on date selected listener. Listener will be called whenever user selected
     * or unselected a date from dates list.
     *
     * @since 0.1.0
     *
     * @param listener listener object, that should be registered.
     */
    fun addOnDateSelectedListener(listener: OnDateSelectedListener) {
        dateSelectedListeners.add(listener)
    }

    /**
     * Add on time selected listener. Listener will be called whenever user selected
     * or unselected a time from times list.
     *
     * @since 0.1.0
     *
     * @param listener listener object, that should be registered.
     */
    fun addOnTimeSelectedListener(listener: OnDateTimeSelectedListener) {
        timeSelectedListeners.add(listener)
    }

    /**
     * Remove date listener from current view.
     *
     * @since 0.1.0
     *
     * @param listener listener object, that should be unregistered from view.
     */
    fun removeOnDateSelectedListener(listener: OnDateSelectedListener) {
        dateSelectedListeners.remove(listener)
    }

    /**
     * Remove time listener from current view.
     *
     * @since 0.1.0
     *
     * @param listener listener object, that should be unregistered from view.
     */
    fun removeOnTimeSelectedListener(listener: OnDateTimeSelectedListener) {
        timeSelectedListeners.remove(listener)
    }

    /**
     * Set currently selected date programaticaly.
     *
     * @since 0.1.0
     *
     * @param date date to be selected in picker.
     */
    fun setSelectedDate(date: LocalDate) {
        if (selectedDate != null && date.isEqual(selectedDate))
            return
        onDateSelected(date)
    }

    /**
     * Clear date selection from date picker.
     *
     * @since 0.1.0
     */
    fun clearSelectedDate() {
        if (selectedDate != null)
            onDateSelected(selectedDate as LocalDate)
    }

    /**
     * Set currently selected time slot programmatically.
     *
     * @since 0.1.0
     *
     * @param time time slot to be selected in picker.
     */
    fun setSelectedTime(time: LocalDateTime) {
        if (selectedLocalDateTime != null && time.isEqual(selectedLocalDateTime))
            return
        onTimeSelected(time)
    }

    /**
     * Clear date selection from date picker.
     *
     * @since 0.1.0
     */
    fun clearSelectedTime() {
        if (selectedLocalDateTime != null)
            onTimeSelected(selectedLocalDateTime as LocalDateTime)
    }

    /**
     * Make date selector to scroll to the specific date on calendar.
     *
     * @since 0.1.0
     *
     * @param date date, which should be scrolled to.
     */
    fun scrollToDate(date: LocalDate) {
        val index = daysAdapter.getIndexOfDate(date)
        if (index >= 0) {
            rvDays.scrollToPosition(index)
        } else {
            // If current date not found in picker, let add some more dates
            if (date.isBefore(daysAdapter.getStartDate())) {
                // Get filling date range for start
                val range = CalendarUtils.getDatesForLeftRange(daysAdapter.getStartDate(), date)
                // Add them to adapter and notify for some chagnes
                daysAdapter.putItemsToStart(range)
                // And then try to scroll one more time
                val newIndex = daysAdapter.getIndexOfDate(date)
                if (newIndex >= 0)
                    rvDays.scrollToPosition(newIndex)
            } else if (date.isAfter(daysAdapter.getEndDate())) {
                // Get filling date range for start
                val range = CalendarUtils.getDatesForRightRange(daysAdapter.getEndDate(), date)
                // Add them to adapter and notify for some chagnes
                daysAdapter.putItemsToEnd(range)
                // And then try to scroll one more time
                val newIndex = daysAdapter.getIndexOfDate(date)
                if (newIndex >= 0)
                    rvDays.scrollToPosition(newIndex)
            }
        }
    }

    /**
     * Get current time availability data, stored inside picker.
     *
     * @since 0.1.0
     *
     * @return locally stored time availability data for specific dates.
     */
    fun getTimeAvailabilityData() = timeData

    /**
     * Set data map with availability options for specific dates. This method resets
     * current collection of data and sets completely new.
     *
     * @since 0.1.0
     *
     * @param timeAvailability map with date availability options.
     */
    fun setTimeAvailabilityData(timeAvailability: Map<LocalDate, List<LocalDateTime>>) {
        timeData.clear()
        putTimeAvailabilityData(timeAvailability)
    }

    /**
     * Add data map with availability options for specific dates to existing one.
     *
     * @since 0.1.0
     *
     * @param timeAvailability map with date availability options.
     */
    fun putTimeAvailabilityData(timeAvailability: Map<LocalDate, List<LocalDateTime>>) {
        timeData.putAll(timeAvailability)
        if (selectedDate != null && timeData.containsKey(selectedDate ?: LocalDate.now())) {
            val list = timeData[selectedDate ?: LocalDate.now()] ?: listOf()
            if (selectedLocalDateTime != null
                    && !list.contains(selectedLocalDateTime ?: LocalDateTime.now()))
                selectedLocalDateTime = null
            hoursAdapter.setTimeSlots(list)
            layoutHours.visibility = View.VISIBLE
        } else {
            selectedLocalDateTime = null
            layoutHours.visibility = View.GONE
            hoursAdapter.clearData()
        }
    }

    /**
     * Clear all time availability options and related data.
     *
     * @since 0.1.0
     */
    fun clearTimeAvailabilityData() {
        timeData.clear()
        layoutHours.visibility = View.GONE
        hoursAdapter.clearData()
        selectedLocalDateTime = null
    }

    /**
     * Get locally stored price data.
     *
     * @since 0.1.0
     *
     * @return locally stored price data map for specific dates.
     */
    fun getPriceData() = priceData

    /**
     * Set data map with specific price options for specific dates. This method resets
     * current collection of data and sets completely new.
     *
     * @since 0.1.0
     *
     * @param priceMap map with min price option for specific dates.
     */
    fun setPriceData(priceMap: Map<LocalDate, Price>) {
        priceData.clear()
        putPriceData(priceMap)
    }

    /**
     * Add data map with specific price options for specific dates to existing one.
     *
     * @since 0.1.0
     *
     * @param priceMap map with min price option for specific dates.
     */
    fun putPriceData(priceMap: Map<LocalDate, Price>) {
        priceData.putAll(priceMap)
        daysAdapter.notifyDataSetChanged()
    }

    /**
     * Clear all price options and related data.
     *
     * @since 0.1.0
     */
    fun clearPriceData() {
        priceData.clear()
        daysAdapter.notifyDataSetChanged()
    }

    // View private implemetation

    /**
     * Called when specific date was selected by user.
     *
     * @since 0.1.0
     *
     * @param newDate new date to be selected.
     */
    private fun onDateSelected(newDate: LocalDate) {
        if (selectedDate == null || !newDate.isEqual(selectedDate)) {
            val oldPosition = daysAdapter.getIndexOfDate(selectedDate)
            val newPosition = daysAdapter.getIndexOfDate(newDate)
            selectedDate = newDate
            if (oldPosition >= 0)
                daysAdapter.notifyItemChanged(oldPosition)
            if (newPosition >= 0)
                daysAdapter.notifyItemChanged(newPosition)

            selectedLocalDateTime = null

            if (timeData.containsKey(newDate)) {
                val list = timeData[newDate] ?: listOf()
                layoutHours.visibility = View.VISIBLE
                hoursAdapter.setTimeSlots(list)
            } else {
                layoutHours.visibility = View.GONE
                hoursAdapter.clearData()
            }
            dateSelectedListeners.forEach {
                it.onSelected(selectedDate)
            }
        } else if (newDate.isEqual(selectedDate)) {
            val position = daysAdapter.getIndexOfDate(newDate)
            selectedDate = null
            daysAdapter.notifyItemChanged(position)

            selectedLocalDateTime = null
            layoutHours.visibility = View.GONE
            hoursAdapter.clearData()

            dateSelectedListeners.forEach {
                it.onSelected(selectedDate)
            }
        }
    }

    /**
     * Called when specific time slot was selected by user.
     *
     * @since 0.1.0
     *
     * @param newTime new time slot to be selected or unselected.
     */
    private fun onTimeSelected(newTime: LocalDateTime) {
        val date = newTime.toLocalDate()
        val allItems = timeData[date] ?: listOf()
        if (selectedLocalDateTime == null || !newTime.isEqual(selectedLocalDateTime)) {
            val oldPosition = if (selectedLocalDateTime != null)
                allItems.indexOf(selectedLocalDateTime as LocalDateTime)
            else
                -1
            val newPosition = allItems.indexOf(newTime)
            selectedLocalDateTime = newTime
            if (oldPosition >= 0)
                hoursAdapter.notifyItemChanged(oldPosition)
            if (newPosition >= 0)
                hoursAdapter.notifyItemChanged(newPosition)

            timeSelectedListeners.forEach {
                it.onSelected(selectedLocalDateTime)
            }
        } else if (newTime.isEqual(selectedLocalDateTime)) {
            val position = allItems.indexOf(newTime)
            selectedLocalDateTime = null
            if (position >= 0)
                hoursAdapter.notifyItemChanged(position)

            timeSelectedListeners.forEach {
                it.onSelected(selectedLocalDateTime)
            }
        }
    }

    /**
     * This method should be called to check if the requested date
     * included in range of dates: [today, ∞).
     *
     * @since 0.1.0
     *
     * @param date checked date.
     *
     * @return `true` if requested date is included in range of dates: [today, ∞).
     */
    private fun isDateValid(date: LocalDate): Boolean {
        val today = LocalDate.now()
        return today.isEqual(date) || today.isBefore(date)
    }

    /**
     * Converts dp value to pixel value.
     *
     * @since 0.1.0
     *
     * @param dp DP value to be converted to pixels.
     */
    private fun dpToPx(dp: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        return ((dp * displayMetrics.density) + 0.5).toInt()
    }

    // Adapters

    /**
     * Adapter for day selector.
     *
     * @since 0.1.0
     */
    private inner class DaysAdapter : RecyclerView.Adapter<DaysAdapter.Holder>() {

        private val dates = mutableListOf<LocalDate>()

        private val dayNameFormatter = DateTimeFormatter.ofPattern("EEE")
        private val dayNumberFormatter = DateTimeFormatter.ofPattern("dd")
        private val monthNameFormatter = DateTimeFormatter.ofPattern("MMM")
        private val currencyFormatter = DecimalFormat("#.##")

        init {
            // Set default calendar item range to adapter
            dates.addAll(CalendarUtils.getDefaultItemRange())
        }

        /**
         * Called when RecyclerView needs a new [RecyclerView.ViewHolder] of the given type to represent
         * an item.
         *
         *
         * This new ViewHolder should be constructed with a new View that can represent the items
         * of the given type. You can either create a new View manually or inflate it from an XML
         * layout file.
         *
         *
         * The new ViewHolder will be used to display items of the adapter using
         * [.onBindViewHolder]. Since it will be re-used to display
         * different items in the data set, it is a good idea to cache references to sub views of
         * the View to avoid unnecessary [View.findViewById] calls.

         * @param parent The ViewGroup into which the new View will be added after it is bound to
         * *               an adapter position.
         * *
         * @param viewType The view type of the new View.
         * *
         * *
         * @return A new ViewHolder that holds a View of the given view type.
         * *
         * @see .getItemViewType
         * @see .onBindViewHolder
         */
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): Holder {
            val view = LayoutInflater.from(parent?.context).inflate(R.layout.day_full, parent, false)
            return Holder(view)
        }

        /**
         * Return the stable ID for the item at `position`. If [.hasStableIds]
         * would return false this method should return [.NO_ID]. The default implementation
         * of this method returns [.NO_ID].

         * @param position Adapter position to query
         * *
         * @return the stable ID of the item at position
         */
        override fun getItemId(position: Int): Long {
            return dates[position].atStartOfDay().atZone(OffsetDateTime.now().offset)
                    .toInstant().toEpochMilli()
        }

        /**
         * Called by RecyclerView to display the data at the specified position. This method should
         * update the contents of the [RecyclerView.ViewHolder.itemView] to reflect the item at the given
         * position.
         *
         *
         * Note that unlike [android.widget.ListView], RecyclerView will not call this method
         * again if the position of the item changes in the data set unless the item itself is
         * invalidated or the new position cannot be determined. For this reason, you should only
         * use the `position` parameter while acquiring the related data item inside
         * this method and should not keep a copy of it. If you need the position of an item later
         * on (e.g. in a click listener), use [RecyclerView.ViewHolder.getAdapterPosition] which will
         * have the updated adapter position.

         * Override [.onBindViewHolder] instead if Adapter can
         * handle efficient partial bind.

         * @param holder The ViewHolder which should be updated to represent the contents of the
         * *        item at the given position in the data set.
         * *
         * @param position The position of the item within the adapter's data set.
         */
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val date = dates[position]
            holder.dayName.text = dayNameFormatter.format(date)
            holder.day.text = dayNumberFormatter.format(date)
            holder.month.text = monthNameFormatter.format(date)
            if (priceData.containsKey(date))
                holder.minPrice.text = "${currencyFormatter.format(priceData[date]?.value)} ${priceData[date]?.currency ?: "$"}"
            else
                holder.minPrice.text = "-"

            if (isDateValid(date)) {
                holder.dayName.alpha = 1F
                holder.day.alpha = 1F
                holder.month.alpha = 1F
                holder.minTag.alpha = 1F
                holder.minPrice.alpha = 1F
            } else {
                holder.dayName.alpha = 0.3F
                holder.day.alpha = 0.3F
                holder.month.alpha = 0.3F
                holder.minTag.alpha = 0.3F
                holder.minPrice.alpha = 0.3F
            }

            if (selectedDate != null && date.isEqual(selectedDate))
                holder.itemView.setBackgroundResource(R.drawable.selected_item)
            else
                holder.itemView.setBackgroundColor(Color.WHITE)
            holder.itemView.setOnClickListener {
                if (isDateValid(date))
                    onDateSelected(date)
            }
        }

        /**
         * Returns the total number of items in the data set held by the adapter.

         * @return The total number of items in this adapter.
         */
        override fun getItemCount() = dates.size

        /**
         * Get start date of current list content.
         *
         * @since 0.1.0
         *
         * @return first date in calendar list.
         */
        fun getStartDate(): LocalDate = dates.first()

        /**
         * Get ending date of current list content.
         *
         * @since 0.1.0
         *
         * @return last date in calendar list.
         */
        fun getEndDate(): LocalDate = dates.last()

        /**
         * Put dates to the start of list.
         *
         * @since 0.1.0
         *
         * @param items items to be add.
         */
        fun putItemsToStart(items: List<LocalDate>) {
            dates.addAll(0, items)
            notifyItemRangeInserted(0, items.size)
        }

        /**
         * Put dates to the start of list.
         *
         * @since 0.1.0
         *
         * @param items items to be add.
         */
        fun putItemsToEnd(items: List<LocalDate>) {
            val startIndex = dates.size
            dates.addAll(items)
            notifyItemRangeInserted(startIndex, items.count())
        }

        /**
         * Get index of specific date in date selection list.
         *
         * @since 0.1.0
         *
         * @param date date, position of which was requsted.
         *
         * @return position if list containing current date, otherwise -1.
         */
        fun getIndexOfDate(date: LocalDate?): Int {
            if (date is LocalDate)
                return dates.indexOf(date)
            return -1
        }

        /**
         * Reset list of dates and replace with the custom range. Needed for restoring of state.
         *
         * @since 0.1.0
         *
         * @param items items to replace current date list with.
         */
        fun setRangeOnOwn(items: List<LocalDate>) {
            dates.clear()
            dates.addAll(items)
            notifyDataSetChanged()
        }

        /**
         * View holder for date views.
         *
         * @since 0.1.0
         *
         * @param view inflated view.
         */
        private inner class Holder(view: View?) : RecyclerView.ViewHolder(view) {
            val dayName: TextView = itemView.findViewById(R.id.day_name)
            val day: TextView = itemView.findViewById(R.id.month_number)
            val month: TextView = itemView.findViewById(R.id.month_name)
            val minTag: TextView = itemView.findViewById(R.id.min_tag)
            val minPrice: TextView = itemView.findViewById(R.id.min_price)
        }
    }

    /**
     * Adapter for slot selection list.
     *
     * @since 0.1.0
     */
    private inner class HourAdapter : RecyclerView.Adapter<HourAdapter.Holder>() {

        private val times = mutableListOf<LocalDateTime>()

        private val hourFormatter = DateTimeFormatter.ofPattern("HH")
        private val minuteFormatter = DateTimeFormatter.ofPattern("mm")

        /**
         * Called by RecyclerView to display the data at the specified position. This method should
         * update the contents of the [RecyclerView.ViewHolder.itemView] to reflect the item at the given
         * position.
         *
         *
         * Note that unlike [android.widget.ListView], RecyclerView will not call this method
         * again if the position of the item changes in the data set unless the item itself is
         * invalidated or the new position cannot be determined. For this reason, you should only
         * use the `position` parameter while acquiring the related data item inside
         * this method and should not keep a copy of it. If you need the position of an item later
         * on (e.g. in a click listener), use [RecyclerView.ViewHolder.getAdapterPosition] which will
         * have the updated adapter position.

         * Override [.onBindViewHolder] instead if Adapter can
         * handle efficient partial bind.

         * @param holder The ViewHolder which should be updated to represent the contents of the
         * *        item at the given position in the data set.
         * *
         * @param position The position of the item within the adapter's data set.
         */
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val time = times[position]
            holder.hours.text = hourFormatter.format(time)
            holder.minutes.text = minuteFormatter.format(time)
            if (selectedLocalDateTime != null && time.isEqual(selectedLocalDateTime))
                holder.itemView.setBackgroundResource(R.drawable.selected_item)
            else
                holder.itemView.setBackgroundColor(Color.WHITE)

            holder.itemView.setOnClickListener {
                onTimeSelected(time)
            }
        }

        /**
         * Returns the total number of items in the data set held by the adapter.

         * @return The total number of items in this adapter.
         */
        override fun getItemCount() = times.size

        /**
         * Called when RecyclerView needs a new [RecyclerView.ViewHolder] of the given type to represent
         * an item.
         *
         *
         * This new ViewHolder should be constructed with a new View that can represent the items
         * of the given type. You can either create a new View manually or inflate it from an XML
         * layout file.
         *
         *
         * The new ViewHolder will be used to display items of the adapter using
         * [.onBindViewHolder]. Since it will be re-used to display
         * different items in the data set, it is a good idea to cache references to sub views of
         * the View to avoid unnecessary [View.findViewById] calls.

         * @param parent The ViewGroup into which the new View will be added after it is bound to
         * *               an adapter position.
         * *
         * @param viewType The view type of the new View.
         * *
         * *
         * @return A new ViewHolder that holds a View of the given view type.
         * *
         * @see .getItemViewType
         * @see .onBindViewHolder
         */
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): Holder {
            val view = LayoutInflater.from(parent?.context).inflate(R.layout.layout_time, parent, false)
            return Holder(view)
        }

        /**
         * Set list of time slots to selector.
         *
         * @since 0.1.0
         *
         * @param list slots data to be displayed in picker.
         */
        fun setTimeSlots(list: List<LocalDateTime>) {
            times.clear()
            times.addAll(list)
            notifyDataSetChanged()
        }

        /**
         * Clear picker contents.
         *
         * @since 0.1.0
         */
        fun clearData() {
            times.clear()
            notifyDataSetChanged()
        }

        /**
         * Holder for time slots view.
         *
         * @since 0.1.0
         *
         * @param view inflated view.
         */
        private inner class Holder(view: View?) : RecyclerView.ViewHolder(view) {
            val hours: TextView = itemView.findViewById(R.id.hours)
            val minutes: TextView = itemView.findViewById(R.id.minutes)
        }
    }

    /**
     * Parcelable of saved state for current view.
     *
     * @since 0.1.0
     *
     * @property timeData data of time slots available.
     * @property priceData min price data for specific dates.
     * @property selectedDate selected date by user.
     * @property selectedLocalDateTime selected time slot by user.
     * @property start start date of date picker content.
     * @property end end date of date picker content.
     * @property scrollPosition last known scroll position.
     */
    private class SavedState : BaseSavedState {

        val timeData: Map<LocalDate, List<LocalDateTime>>
        val priceData: Map<LocalDate, Price>
        val selectedDate: LocalDate?
        val selectedLocalDateTime: LocalDateTime?
        val start: LocalDate
        val end: LocalDate
        val scrollPosition: Int

        constructor(timeData: Map<LocalDate, List<LocalDateTime>>,
                    priceData: Map<LocalDate, Price>,
                    selectedDate: LocalDate?,
                    selectedLocalDateTime: LocalDateTime?,
                    start: LocalDate,
                    end: LocalDate,
                    scrollPosition: Int,
                    state: Parcelable) : super(state) {

            this.timeData = timeData
            this.priceData = priceData
            this.selectedDate = selectedDate
            this.selectedLocalDateTime = selectedLocalDateTime
            this.start = start
            this.end = end
            this.scrollPosition = scrollPosition
        }

        private constructor(parcel: Parcel) : super(parcel) {
            // Read all data from parcel to entity.
            val timeDataOpt = mutableMapOf<LocalDate, List<LocalDateTime>>()
            val timeDataSize = parcel.readInt()
            for (i in 0..(timeDataSize - 1)) {
                val key = parcel.readSerializable() as LocalDate
                val count = parcel.readInt()
                val list = mutableListOf<LocalDateTime>()
                for (j in 0..(count - 1)) {
                    list.add(parcel.readSerializable() as LocalDateTime)
                }
                timeDataOpt.put(key, list)
            }

            val priceDataOpt = mutableMapOf<LocalDate, Price>()
            val priceDataSize = parcel.readInt()
            for (i in 0..(priceDataSize - 1)) {
                val key = parcel.readSerializable() as LocalDate
                val price = parcel.readDouble()
                val currency = parcel.readString()
                priceDataOpt.put(key, Price(price, currency))
            }
            timeData = timeDataOpt.toMap()
            priceData = priceDataOpt.toMap()
            selectedDate = parcel.readSerializable() as? LocalDate
            selectedLocalDateTime = parcel.readSerializable() as? LocalDateTime
            start = parcel.readSerializable() as LocalDate
            end = parcel.readSerializable() as LocalDate
            scrollPosition = parcel.readInt()
        }

        override fun writeToParcel(parcel: Parcel, flag: Int) {
            super.writeToParcel(parcel, flag)
            // Write all data to parcel.
            parcel.writeInt(timeData.size)
            timeData.entries.forEach {
                parcel.writeSerializable(it.key)
                parcel.writeInt(it.value.size)
                it.value.forEach {
                    parcel.writeSerializable(it)
                }
            }
            parcel.writeInt(priceData.size)
            priceData.entries.forEach {
                parcel.writeSerializable(it.key)
                parcel.writeDouble(it.value.value)
                parcel.writeString(it.value.currency)
            }
            parcel.writeSerializable(selectedDate)
            parcel.writeSerializable(selectedLocalDateTime)
            parcel.writeSerializable(start)
            parcel.writeSerializable(end)
            parcel.writeInt(scrollPosition)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    // Public interfaces

    /**
     * Listener interface, called on user selects or deselects date.
     *
     * @since 0.1.0
     */
    interface OnDateSelectedListener {

        /**
         * Called when user select or deselect specific date.
         *
         * @since 0.1.0
         *
         * @param date last selected date. Equal to `null` if user unselected last known date.
         */
        fun onSelected(date: LocalDate?)
    }

    /**
     * Listener interface, called on user selects or deselects time slot.
     *
     * @since 0.1.0
     */
    interface OnDateTimeSelectedListener {

        /**
         * Called when user select or deselect specific time slot.
         *
         * @since 0.1.0
         *
         * @param time last selected time slot. Equal to `null` if user deselected last known time slot.
         */
        fun onSelected(time: LocalDateTime?)
    }
}