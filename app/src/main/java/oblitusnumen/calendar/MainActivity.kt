package oblitusnumen.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import oblitusnumen.calendar.ui.BottomNavItem
import oblitusnumen.calendar.ui.state.Calendar
import oblitusnumen.calendar.ui.state.CalendarTab
import oblitusnumen.calendar.ui.theme.CalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            NavHostContainer(
//                NavHostController(),
//                padding =
//            )
            CalendarTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CalendarTab()
//                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
//    val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//    val navController = navHostFragment.navController
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CalendarTheme {
        Greeting("Android")
    }
}


@Composable
fun NavHostContainer(
    navController: NavHostController,
    padding: PaddingValues
) {

    NavHost(
        navController = navController,

        // set the start destination as home
        startDestination = "home",

        // Set the padding provided by scaffold
        modifier = Modifier.padding(paddingValues = padding),

        builder = {

            // route : Home
            composable("home") {
                HomeScreen()
            }

            // route : search
            composable("search") {
                SearchScreen()
            }

            // route : profile
            composable("profile") {
                ProfileScreen()
            }
        })

}

@Composable
fun HomeScreen() {

    // Column Composable,
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        // Parameters set to place the items in center
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Composable
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "home",
            tint = Color(0xFF0F9D58)
        )
        // Text to Display the current Screen
        Text(text = "Home", color = Color.Black)
    }
}




@Composable
fun SearchScreen() {
    // Column Composable,
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        // parameters set to place the items in center
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Composable
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "search",
            tint = Color(0xFF0F9D58)
        )
        // Text to Display the current Screen
        Text(text = "Search", color = Color.Black)
    }
}


object Constants {
    val BottomNavItems = listOf(
        BottomNavItem(
            label = "Home",
            icon = Icons.Filled.Home,
            route = "home"
        ),
        BottomNavItem(
            label = "Search",
            icon = Icons.Filled.Search,
            route = "search"
        ),
        BottomNavItem(
            label = "Profile",
            icon = Icons.Filled.Person,
            route = "profile"
        )
    )
}

@Composable
fun ProfileScreen() {
    // Column Composable,
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        // parameters set to place the items in center
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Composable
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            tint = Color(0xFF0F9D58)
        )
        // Text to Display the current Screen
        Text(text = "Profile", color = Color.Black)
    }
}






//@Serializable
//object Home
//@Serializable
//object Settings
//@Composable
//fun HomeScreen(onNavigateToSettings: () -> Unit){
//    Column {
//        Text("Home")
//        Button(onClick = onNavigateToSettings){
//            Text("Open settings")
//        }
//    }
//}
//
//// This screen will be displayed as a dialog
//@Composable
//fun SettingsScreen(){
//    Text("Settings")
//    // ...
//}
//
//@Composable
//fun MyApp() {
//    val navController = rememberNavController()
//    NavHost(navController, startDestination = Home) {
//        composable<Home> { HomeScreen(onNavigateToSettings = { navController.navigate(route = Settings) }) }
//        dialog<Settings> { SettingsScreen() }
//    }
//}