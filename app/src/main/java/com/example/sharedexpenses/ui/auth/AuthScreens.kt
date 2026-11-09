package com.example.sharedexpenses.ui.auth

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharedexpenses.R
import com.example.sharedexpenses.SeedData
import com.example.sharedexpenses.ui.AppViewModelProvider
import com.example.sharedexpenses.ui.components.AppScreen
import com.example.sharedexpenses.ui.components.PrimaryButton
import com.example.sharedexpenses.ui.components.SecondaryButton
import com.example.sharedexpenses.ui.theme.SharedExpensesTheme

/** Lo-fi 01. */
@Composable
fun WelcomeScreen(onCreateAccount: () -> Unit, onLogIn: () -> Unit) {
    AppScreen(
        title = stringResource(R.string.welcome_title),
        bottomButton = { PrimaryButton(stringResource(R.string.log_in), onLogIn) },
    ) {
        Text(stringResource(R.string.welcome_headline), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.welcome_body))
        SecondaryButton(stringResource(R.string.create_account), onCreateAccount)
        Text(stringResource(R.string.demo_mock_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Lo-fi 02. Form values use rememberSaveable so they survive rotation (Lecture 8). */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onLoggedIn: () -> Unit,
    authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    // Pre-filled with the demo account so the app is quick to demo (NF06).
    var email by rememberSaveable { mutableStateOf(SeedData.alex.email) }
    var password by rememberSaveable { mutableStateOf(SeedData.DEMO_PASSWORD) }
    val error by authViewModel.error.collectAsStateWithLifecycle()

    AppScreen(
        title = stringResource(R.string.log_in),
        onBack = onBack,
        bottomButton = {
            PrimaryButton(stringResource(R.string.log_in), onClick = { authViewModel.logIn(email, password, onLoggedIn) })
        },
    ) {
        OutlinedTextField(
            value = email, onValueChange = { email = it; authViewModel.clearError() },
            label = { Text(stringResource(R.string.field_email)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it; authViewModel.clearError() },
            label = { Text(stringResource(R.string.field_password)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) Text(authErrorText(error!!), color = MaterialTheme.colorScheme.error)
        SecondaryButton(stringResource(R.string.create_account), onCreateAccount)
        Text(stringResource(R.string.demo_auth_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Lo-fi 03 (F01: name and contact details). */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val error by authViewModel.error.collectAsStateWithLifecycle()

    AppScreen(
        title = stringResource(R.string.create_profile),
        onBack = onBack,
        bottomButton = {
            PrimaryButton(
                stringResource(R.string.create_profile),
                onClick = { authViewModel.register(name, email, phone, password, onRegistered) },
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
        OutlinedTextField(
            password, { password = it }, label = { Text(stringResource(R.string.field_password)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) Text(authErrorText(error!!), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun authErrorText(error: AuthError) = when (error) {
    AuthError.WRONG_LOGIN -> stringResource(R.string.error_wrong_login)
    AuthError.MISSING_FIELDS -> stringResource(R.string.error_missing_fields)
    AuthError.EMAIL_TAKEN -> stringResource(R.string.error_email_taken)
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    SharedExpensesTheme { WelcomeScreen({}, {}) }
}
