package me.dnihze.reservationpicker.model

/**
 * Price entity for specific dates.
 *
 * @since 0.1.0
 *
 * @property value price value.
 * @property currency currency string identifier.
 */
data class Price(
        var value: Double,
        var currency: String
)