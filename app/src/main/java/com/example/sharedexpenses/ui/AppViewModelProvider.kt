package com.example.sharedexpenses.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sharedexpenses.SharedExpensesApplication
import com.example.sharedexpenses.ui.activity.ActivityViewModel
import com.example.sharedexpenses.ui.auth.AuthViewModel
import com.example.sharedexpenses.ui.expense.AddExpenseViewModel
import com.example.sharedexpenses.ui.expense.ExpenseViewModel
import com.example.sharedexpenses.ui.group.CreateGroupViewModel
import com.example.sharedexpenses.ui.group.GroupViewModel
import com.example.sharedexpenses.ui.group.ReminderViewModel
import com.example.sharedexpenses.ui.home.HomeViewModel
import com.example.sharedexpenses.ui.profile.ProfileViewModel
import com.example.sharedexpenses.ui.session.SessionViewModel

/**
 * Our ViewModels need repositories, so `viewModel()` gets this factory:
 * `viewModel(factory = AppViewModelProvider.Factory)`.
 * createSavedStateHandle() gives access to the navigation arguments (e.g. groupId).
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { SessionViewModel(app().container.userRepository) }
        initializer { AuthViewModel(app().container.userRepository) }
        initializer {
            HomeViewModel(app().container.groupRepository, app().container.userRepository, app().container.syncRepository)
        }
        initializer { CreateGroupViewModel(app().container.groupRepository, app().container.userRepository) }
        initializer {
            GroupViewModel(
                createSavedStateHandle(), app().container.groupRepository,
                app().container.userRepository, app().container.syncRepository,
            )
        }
        initializer {
            AddExpenseViewModel(createSavedStateHandle(), app().container.groupRepository, app().container.userRepository)
        }
        initializer {
            ExpenseViewModel(createSavedStateHandle(), app().container.groupRepository, app().container.syncRepository)
        }
        initializer {
            ReminderViewModel(
                createSavedStateHandle(), app().container.groupRepository,
                app().container.userRepository, app().container.syncRepository,
            )
        }
        initializer { ActivityViewModel(app().container.userRepository, app().container.syncRepository) }
        initializer { ProfileViewModel(app().container.userRepository) }
    }
}

private fun CreationExtras.app(): SharedExpensesApplication = this[APPLICATION_KEY] as SharedExpensesApplication
