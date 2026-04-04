/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.nikhil.yt.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.nikhil.yt.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "search"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.library_outlined,
        iconIdActive = R.drawable.library_filled,
        route = "library"
    )

    object Stats : Screens(
        titleId = R.string.stats,
        iconIdInactive = R.drawable.trending_up,
        iconIdActive = R.drawable.trending_up,
        route = "stats"
    )

    object History : Screens(
        titleId = R.string.history,
        iconIdInactive = R.drawable.history,
        iconIdActive = R.drawable.history,
        route = "history"
    )


    companion object {
        val MainScreens = listOf(Home, Stats, History, Library)
    }
}
