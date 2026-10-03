package com.burton.weather

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burton.weather.report.ShakeToReport
import com.burton.weather.ui.cities.CitiesScreen
import com.burton.weather.ui.detail.DetailScreen
import com.burton.weather.ui.navigation.Routes
import com.burton.weather.ui.radar.RadarScreen
import com.burton.weather.ui.theme.BurtonBlack
import com.burton.weather.ui.theme.BurtonIvory
import com.burton.weather.ui.theme.BurtonMute
import com.burton.weather.ui.theme.BurtonWeatherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var pendingLocation: (() -> Unit)? = null

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) pendingLocation?.invoke()
        pendingLocation = null
    }
    private val shakeToReport by lazy { ShakeToReport(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BurtonWeatherTheme {
                BurtonApp(onNeedLocation = ::requestLocation)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeToReport.start()
    }

    override fun onPause() {
        shakeToReport.stop()
        super.onPause()
    }

    private fun requestLocation(onGranted: () -> Unit) {
        pendingLocation = onGranted
        locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}

@Composable
private fun BurtonApp(
    onNeedLocation: (onGranted: () -> Unit) -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onDetail = route?.startsWith("city/") == true
    val selectedTab = if (route?.startsWith("radar") == true) Routes.RADAR else Routes.CITIES
    Scaffold(
        containerColor = BurtonBlack,
        bottomBar = {
            if (!onDetail) {
                NavigationBar(containerColor = BurtonBlack, contentColor = BurtonIvory) {
                    NavigationBarItem(
                        selected = selectedTab == Routes.CITIES,
                        onClick = { navController.goTab(Routes.CITIES) },
                        icon = { Icon(Icons.Rounded.LocationCity, contentDescription = "Cities") },
                        label = { Text("Cities") },
                        colors = navColors(selectedTab == Routes.CITIES),
                    )
                    NavigationBarItem(
                        selected = selectedTab == Routes.RADAR,
                        onClick = { navController.goTab(Routes.radar()) },
                        icon = { Icon(Icons.Rounded.Radar, contentDescription = "Radar") },
                        label = { Text("Radar") },
                        colors = navColors(selectedTab == Routes.RADAR),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CITIES,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.CITIES) {
                CitiesScreen(
                    onOpenCity = { navController.navigate(Routes.city(it)) },
                    onNeedLocation = onNeedLocation,
                )
            }
            composable(
                Routes.RADAR,
                arguments = listOf(navArgument("cityId") { type = NavType.StringType; defaultValue = "" }),
            ) {
                RadarScreen()
            }
            composable(
                Routes.CITY,
                arguments = listOf(navArgument("cityId") { type = NavType.StringType }),
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    onRadar = {
                        navController.navigate(Routes.cityRadar(it)) { launchSingleTop = true }
                    },
                )
            }
            composable(
                Routes.CITY_RADAR,
                arguments = listOf(navArgument("cityId") { type = NavType.StringType }),
            ) {
                RadarScreen(
                    locked = true,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.goTab(route: String, restore: Boolean = true) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = restore
    }
}

@Composable
private fun navColors(selected: Boolean) = NavigationBarItemDefaults.colors(
    selectedIconColor = BurtonIvory,
    selectedTextColor = BurtonIvory,
    unselectedIconColor = BurtonMute,
    unselectedTextColor = BurtonMute,
    indicatorColor = Color(0xFF222222),
)
