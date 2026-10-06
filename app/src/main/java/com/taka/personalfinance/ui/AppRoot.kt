@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taka.personalfinance.PFApp
import com.taka.personalfinance.data.TxType

@Composable
fun AppRoot(vm: FinanceViewModel, app: PFApp, activity: FragmentActivity) {
    val locked by app.appLock.locked.collectAsStateWithLifecycle()
    val bn = LocalBn.current
    val bnNow by rememberUpdatedState(bn)
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.events.collect { msg ->
            snackbar.showSnackbar(if (bnNow) msg.bn else msg.en)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (locked) {
            // While locked nothing from the app (including open dialogs) is composed,
            // so no financial information can be seen.
            LockScreen(app.appLock, activity)
        } else {
            AppNav(vm, app, activity)
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp),
        )
    }
}

@Composable
private fun AppNav(vm: FinanceViewModel, app: PFApp, activity: FragmentActivity) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "main") {
        composable("main") { MainShell(vm, app, activity, nav) }
        composable(
            "txn?id={id}&date={date}&type={type}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("date") { type = NavType.LongType; defaultValue = -1L },
                navArgument("type") { type = NavType.StringType; defaultValue = TxType.EXPENSE },
            ),
        ) { entry ->
            TransactionEditScreen(
                vm = vm,
                id = entry.arguments?.getLong("id") ?: -1L,
                presetDay = entry.arguments?.getLong("date") ?: -1L,
                presetType = entry.arguments?.getString("type") ?: TxType.EXPENSE,
                onClose = { nav.popBackStack() },
            )
        }
        composable(
            "loan?id={id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            LoanEditScreen(
                vm = vm,
                id = entry.arguments?.getLong("id") ?: -1L,
                onClose = { nav.popBackStack() },
                onDeleted = { nav.popBackStack("main", false) },
            )
        }
        composable(
            "loandetail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            LoanDetailScreen(
                vm = vm,
                id = entry.arguments?.getLong("id") ?: -1L,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("loan?id=$it") },
            )
        }
        composable("categories") {
            CategoriesScreen(vm = vm, onClose = { nav.popBackStack() })
        }
    }
}

@Composable
private fun MainShell(vm: FinanceViewModel, app: PFApp, activity: FragmentActivity, nav: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(0) }
    BackHandler(enabled = tab != 0) { tab = 0 }

    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val labels = listOf(
        t("Home", "হোম"),
        t("History", "লেনদেন"),
        t("Reports", "রিপোর্ট"),
        t("Loans", "ঋণ"),
        t("Settings", "সেটিংস"),
    )

    val content: @Composable () -> Unit = {
        when (tab) {
            0 -> HomeScreen(
                vm = vm,
                onAdd = { type -> nav.navigate("txn?id=-1&date=-1&type=$type") },
                onEdit = { id -> nav.navigate("txn?id=$id&date=-1&type=${TxType.EXPENSE}") },
                onSeeAll = { tab = 1 },
                onOpenLoans = { tab = 3 },
            )
            1 -> TransactionsScreen(
                vm = vm,
                onAdd = { day -> nav.navigate("txn?id=-1&date=$day&type=${TxType.EXPENSE}") },
                onEdit = { id -> nav.navigate("txn?id=$id&date=-1&type=${TxType.EXPENSE}") },
            )
            2 -> ReportsScreen(vm)
            3 -> LoansScreen(
                vm = vm,
                onAdd = { nav.navigate("loan?id=-1") },
                onOpen = { id -> nav.navigate("loandetail/$id") },
            )
            else -> SettingsScreen(vm, app, activity, onOpenCategories = { nav.navigate("categories") })
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                labels.forEachIndexed { i, label ->
                    NavigationRailItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { TabIcon(i) },
                        label = { Text(label, maxLines = 1) },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxSize()) { content() }
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    labels.forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { TabIcon(i) },
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
            },
        ) { pad ->
            val bottom = pad.calculateBottomPadding()
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = bottom)
                    .consumeWindowInsets(PaddingValues(bottom = bottom)),
            ) { content() }
        }
    }
}

@Composable
private fun TabIcon(index: Int) {
    when (index) {
        0 -> Icon(Icons.Filled.Home, contentDescription = null)
        1 -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
        2 -> BarsIcon()
        3 -> Icon(Icons.Filled.Person, contentDescription = null)
        else -> Icon(Icons.Filled.Settings, contentDescription = null)
    }
}

/** Small bar-chart icon (there is no chart icon in the core icon set). */
@Composable
private fun BarsIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val barW = w * 0.18f
        val r = CornerRadius(barW / 3f)
        drawRoundRect(color, Offset(w * 0.14f, h * 0.50f), Size(barW, h * 0.36f), r)
        drawRoundRect(color, Offset(w * 0.41f, h * 0.22f), Size(barW, h * 0.64f), r)
        drawRoundRect(color, Offset(w * 0.68f, h * 0.36f), Size(barW, h * 0.50f), r)
    }
}
