package com.example.sharedexpenses.ui.navigation

/** Lecture 9: routes defined in one place instead of raw strings, to avoid typos. */
object Routes {
    // Nested graph: log in / register
    const val AUTH_FLOW = "auth_flow"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Nested graph: the app after login
    const val APP_FLOW = "app_flow"
    const val HOME = "home"
    const val CREATE_GROUP = "create_group"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val NOTIFICATION_SETTINGS = "notification_settings"

    // Group screens - take the group id as an argument
    const val ARG_GROUP_ID = "groupId"
    const val ARG_EXPENSE_ID = "expenseId"
    const val ARG_USER_ID = "userId"

    const val GROUP = "group/{groupId}"
    const val MEMBERS = "group/{groupId}/members"
    const val EDIT_GROUP = "group/{groupId}/edit"
    const val EXPENSES = "group/{groupId}/expenses"
    const val EXPENSE_DETAILS = "expense/{expenseId}"
    const val EXPENSE_STATUS = "expense/{expenseId}/status"
    const val BALANCES = "group/{groupId}/balances"
    const val REMINDER = "group/{groupId}/remind/{userId}"

    // Nested graph: add expense (Add expense -> payer / split / receipt), shares one ViewModel
    const val ADD_EXPENSE_FLOW = "group/{groupId}/add"
    const val ADD_EXPENSE = "add_expense"
    const val CHOOSE_PAYER = "add_expense_payer"
    const val SPLIT = "add_expense_split"
    const val ATTACH_RECEIPT = "add_expense_receipt"

    fun group(groupId: String) = "group/$groupId"
    fun members(groupId: String) = "group/$groupId/members"
    fun editGroup(groupId: String) = "group/$groupId/edit"
    fun expenses(groupId: String) = "group/$groupId/expenses"
    fun expenseDetails(expenseId: String) = "expense/$expenseId"
    fun expenseStatus(expenseId: String) = "expense/$expenseId/status"
    fun balances(groupId: String) = "group/$groupId/balances"
    fun reminder(groupId: String, userId: String) = "group/$groupId/remind/$userId"
    fun addExpense(groupId: String) = "group/$groupId/add"
}
