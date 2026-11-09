package com.example.sharedexpenses.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.sharedexpenses.R
import com.example.sharedexpenses.ui.AppViewModelProvider
import com.example.sharedexpenses.ui.activity.ActivityScreen
import com.example.sharedexpenses.ui.auth.LoginScreen
import com.example.sharedexpenses.ui.auth.RegisterScreen
import com.example.sharedexpenses.ui.auth.WelcomeScreen
import com.example.sharedexpenses.ui.expense.AddExpenseScreen
import com.example.sharedexpenses.ui.expense.AddExpenseViewModel
import com.example.sharedexpenses.ui.expense.AttachReceiptScreen
import com.example.sharedexpenses.ui.expense.ChoosePayerScreen
import com.example.sharedexpenses.ui.expense.ExpenseDetailsScreen
import com.example.sharedexpenses.ui.expense.ExpenseStatusScreen
import com.example.sharedexpenses.ui.expense.SplitScreen
import com.example.sharedexpenses.ui.group.BalancesScreen
import com.example.sharedexpenses.ui.group.CreateGroupScreen
import com.example.sharedexpenses.ui.group.EditGroupScreen
import com.example.sharedexpenses.ui.group.ExpensesScreen
import com.example.sharedexpenses.ui.group.GroupOverviewScreen
import com.example.sharedexpenses.ui.group.MembersScreen
import com.example.sharedexpenses.ui.group.ReminderScreen
import com.example.sharedexpenses.ui.home.HomeScreen
import com.example.sharedexpenses.ui.profile.EditProfileScreen
import com.example.sharedexpenses.ui.profile.NotificationSettingsScreen
import com.example.sharedexpenses.ui.profile.ProfileScreen
import com.example.sharedexpenses.ui.session.SessionViewModel

private val groupArg = listOf(navArgument(Routes.ARG_GROUP_ID) { type = NavType.StringType })
private val expenseArg = listOf(navArgument(Routes.ARG_EXPENSE_ID) { type = NavType.StringType })

/** Screens that show the bottom bar (Groups / Activity / Profile). */
private val bottomBarRoutes = setOf(
    Routes.HOME, Routes.NOTIFICATIONS, Routes.PROFILE, Routes.GROUP, Routes.EXPENSES, Routes.BALANCES,
)

/**
 * Lecture 9: NavHost is the root of the UI. Two nested graphs:
 * AUTH_FLOW (welcome, log in, register) and APP_FLOW (everything after login).
 * Screens only get callbacks - the NavController stays in here.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // Shared ViewModel for the logged-in user (Lecture 9 "Navigation - Keeping State")
    sessionViewModel: SessionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsStateWithLifecycle()
    // Wait until we know if someone is logged in (the "Logged in?" diamond in the navigation model).
    val loggedIn = isLoggedIn ?: return
    val startDestination = remember { if (loggedIn) Routes.APP_FLOW else Routes.AUTH_FLOW }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                BottomBar(currentRoute = currentRoute, onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            navigation(startDestination = Routes.WELCOME, route = Routes.AUTH_FLOW) {
                composable(Routes.WELCOME) {
                    WelcomeScreen(
                        onCreateAccount = { navController.navigate(Routes.REGISTER) },
                        onLogIn = { navController.navigate(Routes.LOGIN) },
                    )
                }
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onBack = { navController.popBackStack() },
                        onCreateAccount = { navController.navigate(Routes.REGISTER) },
                        onLoggedIn = { navController.enterApp() },
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onBack = { navController.popBackStack() },
                        onRegistered = { navController.enterApp() },
                    )
                }
            }

            navigation(startDestination = Routes.HOME, route = Routes.APP_FLOW) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onGroupClick = { id -> navController.navigate(Routes.group(id)) },
                        onCreateGroup = { navController.navigate(Routes.CREATE_GROUP) },
                    )
                }
                composable(Routes.CREATE_GROUP) {
                    CreateGroupScreen(
                        onBack = { navController.popBackStack() },
                        // Save -> Group overview, and remove the form from the back stack.
                        onCreated = { id ->
                            navController.navigate(Routes.group(id)) {
                                popUpTo(Routes.CREATE_GROUP) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.NOTIFICATIONS) {
                    ActivityScreen(
                        onNotificationSettings = { navController.navigate(Routes.NOTIFICATION_SETTINGS) },
                        onMyGroups = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                    )
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                        onNotificationSettings = { navController.navigate(Routes.NOTIFICATION_SETTINGS) },
                        onMyGroups = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                        onLogOut = {
                            sessionViewModel.logOut {
                                navController.navigate(Routes.AUTH_FLOW) {
                                    popUpTo(Routes.APP_FLOW) { inclusive = true }
                                }
                            }
                        },
                    )
                }
                composable(Routes.EDIT_PROFILE) {
                    EditProfileScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.NOTIFICATION_SETTINGS) {
                    NotificationSettingsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.GROUP, arguments = groupArg) { entry ->
                    val groupId = entry.groupId()
                    GroupOverviewScreen(
                        onBack = { navController.popBackStack() },
                        onExpenses = { navController.navigate(Routes.expenses(groupId)) },
                        onBalances = { navController.navigate(Routes.balances(groupId)) },
                        onMembers = { navController.navigate(Routes.members(groupId)) },
                        onEditGroup = { navController.navigate(Routes.editGroup(groupId)) },
                        onAddExpense = { navController.navigate(Routes.addExpense(groupId)) },
                        onExpenseClick = { id -> navController.navigate(Routes.expenseDetails(id)) },
                    )
                }
                composable(Routes.MEMBERS, arguments = groupArg) {
                    MembersScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.EDIT_GROUP, arguments = groupArg) {
                    EditGroupScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.EXPENSES, arguments = groupArg) { entry ->
                    val groupId = entry.groupId()
                    ExpensesScreen(
                        onBack = { navController.popBackStack() },
                        onAddExpense = { navController.navigate(Routes.addExpense(groupId)) },
                        onExpenseClick = { id -> navController.navigate(Routes.expenseDetails(id)) },
                    )
                }
                composable(Routes.EXPENSE_DETAILS, arguments = expenseArg) {
                    ExpenseDetailsScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.BALANCES, arguments = groupArg) { entry ->
                    val groupId = entry.groupId()
                    BalancesScreen(
                        onBack = { navController.popBackStack() },
                        onRemind = { userId -> navController.navigate(Routes.reminder(groupId, userId)) },
                    )
                }
                composable(
                    Routes.REMINDER,
                    arguments = groupArg + navArgument(Routes.ARG_USER_ID) { type = NavType.StringType },
                ) {
                    // Sent -> back to balances
                    ReminderScreen(onBack = { navController.popBackStack() })
                }

                // Add expense -> payer / split / receipt -> Save. One AddExpenseViewModel for the whole flow.
                navigation(startDestination = Routes.ADD_EXPENSE, route = Routes.ADD_EXPENSE_FLOW, arguments = groupArg) {
                    composable(Routes.ADD_EXPENSE) { entry ->
                        val flowViewModel = entry.addExpenseViewModel(navController)
                        AddExpenseScreen(
                            addExpenseViewModel = flowViewModel,
                            onBack = { navController.popBackStack() },
                            onChoosePayer = { navController.navigate(Routes.CHOOSE_PAYER) },
                            onSplit = { navController.navigate(Routes.SPLIT) },
                            onAttachReceipt = { navController.navigate(Routes.ATTACH_RECEIPT) },
                            // Save -> status screen; the form is removed so "back" does not reopen it.
                            onSaved = { expenseId ->
                                navController.navigate(Routes.expenseStatus(expenseId)) {
                                    popUpTo(Routes.ADD_EXPENSE_FLOW) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(Routes.CHOOSE_PAYER) { entry ->
                        ChoosePayerScreen(entry.addExpenseViewModel(navController), onBack = { navController.popBackStack() })
                    }
                    composable(Routes.SPLIT) { entry ->
                        SplitScreen(entry.addExpenseViewModel(navController), onBack = { navController.popBackStack() })
                    }
                    composable(Routes.ATTACH_RECEIPT) { entry ->
                        AttachReceiptScreen(entry.addExpenseViewModel(navController), onBack = { navController.popBackStack() })
                    }
                }

                composable(Routes.EXPENSE_STATUS, arguments = expenseArg) {
                    ExpenseStatusScreen(
                        // -> Expense list / transaction history (navigation model)
                        onContinue = { groupId ->
                            // Back stack becomes: groups -> group -> expenses
                            navController.navigate(Routes.expenses(groupId)) {
                                popUpTo(Routes.GROUP)
                            }
                        },
                    )
                }
            }
        }
    }
}

/** After login: go to the app and remove the login screens, so back does not return to them (Lecture 9). */
private fun NavHostController.enterApp() {
    navigate(Routes.APP_FLOW) {
        popUpTo(Routes.AUTH_FLOW) { inclusive = true }
    }
}

private fun NavBackStackEntry.groupId(): String = checkNotNull(arguments?.getString(Routes.ARG_GROUP_ID))

/** The add-expense ViewModel lives in the nested graph's back stack entry, so all its screens share it. */
@Composable
private fun NavBackStackEntry.addExpenseViewModel(navController: NavHostController): AddExpenseViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry(Routes.ADD_EXPENSE_FLOW) }
    return viewModel(viewModelStoreOwner = parentEntry, factory = AppViewModelProvider.Factory)
}

@Composable
private fun BottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val groupRoutes = setOf(Routes.HOME, Routes.GROUP, Routes.EXPENSES, Routes.BALANCES)
    NavigationBar {
        BottomItem(stringResource(R.string.tab_groups), Icons.Filled.Home, currentRoute in groupRoutes) { onNavigate(Routes.HOME) }
        BottomItem(stringResource(R.string.tab_activity), Icons.Filled.Notifications, currentRoute == Routes.NOTIFICATIONS) {
            onNavigate(Routes.NOTIFICATIONS)
        }
        BottomItem(stringResource(R.string.tab_profile), Icons.Filled.Person, currentRoute == Routes.PROFILE) { onNavigate(Routes.PROFILE) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomItem(
    label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
    )
}
