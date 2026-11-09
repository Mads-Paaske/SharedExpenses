package com.example.sharedexpenses.ui.profile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharedexpenses.R
import com.example.sharedexpenses.data.local.UserEntity
import com.example.sharedexpenses.data.prefs.NotificationLevel
import com.example.sharedexpenses.data.repository.UserRepository
import com.example.sharedexpenses.ui.AppViewModelProvider
import com.example.sharedexpenses.ui.components.AppScreen
import com.example.sharedexpenses.ui.components.InfoCard
import com.example.sharedexpenses.ui.components.NavRow
import com.example.sharedexpenses.ui.components.PrimaryButton
import com.example.sharedexpenses.ui.components.SecondaryButton
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** F02 + F03. */
class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {
    val user: StateFlow<UserEntity?> = userRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notificationLevel: StateFlow<NotificationLevel> = userRepository.notificationLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationLevel.ALL)

    fun updateProfile(name: String, email: String, phone: String, onDone: () -> Unit) {
        viewModelScope.launch {
            userRepository.updateProfile(name, email, phone)
            onDone()
        }
    }

    fun setNotificationLevel(level: NotificationLevel) {
        viewModelScope.launch { userRepository.setNotificationLevel(level) }
    }
}

/** Lo-fi 15 - My profile. */
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onNotificationSettings: () -> Unit,
    onMyGroups: () -> Unit,
    onLogOut: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val user by profileViewModel.user.collectAsStateWithLifecycle()
    AppScreen(title = stringResource(R.string.my_profile)) {
        user?.let {
            InfoCard(title = it.name) {
                Text(it.email)
                Text(it.phone)
            }
        }
        NavRow(stringResource(R.string.edit_profile), onEditProfile)
        NavRow(stringResource(R.string.notification_settings), onNotificationSettings)
        NavRow(stringResource(R.string.my_groups), onMyGroups)
        SecondaryButton(stringResource(R.string.log_out), onLogOut)
    }
}

/** Lo-fi 16 - Edit profile (F02). */
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val loaded by profileViewModel.user.collectAsStateWithLifecycle()
    val user = loaded ?: return
    var name by rememberSaveable { mutableStateOf(user.name) }
    var email by rememberSaveable { mutableStateOf(user.email) }
    var phone by rememberSaveable { mutableStateOf(user.phone) }

    AppScreen(
        title = stringResource(R.string.edit_profile),
        onBack = onBack,
        bottomButton = {
            PrimaryButton(
                stringResource(R.string.save_changes),
                { profileViewModel.updateProfile(name, email, phone, onBack) },
                enabled = name.isNotBlank() && email.isNotBlank(),
            )
        },
    ) {
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            email, { email = it }, label = { Text(stringResource(R.string.field_email)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            phone, { phone = it }, label = { Text(stringResource(R.string.field_phone)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Lo-fi 17 / 30 / 31 - Notification settings (F03). */
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val level by profileViewModel.notificationLevel.collectAsStateWithLifecycle()
    AppScreen(
        title = stringResource(R.string.notification_settings),
        onBack = onBack,
        bottomButton = { PrimaryButton(stringResource(R.string.done), onBack) },
    ) {
        Text(stringResource(R.string.choose_activity))
        NotificationLevel.entries.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = level == option, onClick = { profileViewModel.setNotificationLevel(option) })
                Text(
                    when (option) {
                        NotificationLevel.ALL -> stringResource(R.string.notif_all)
                        NotificationLevel.MENTIONS -> stringResource(R.string.notif_mentions)
                        NotificationLevel.OFF -> stringResource(R.string.notif_off)
                    }
                )
            }
        }
        Text(stringResource(R.string.notif_settings_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
