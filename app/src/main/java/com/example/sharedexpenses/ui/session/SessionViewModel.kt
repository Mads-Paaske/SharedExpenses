package com.example.sharedexpenses.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharedexpenses.data.local.UserEntity
import com.example.sharedexpenses.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lecture 9 "Navigation - Keeping State": shared ViewModel created at the top of the nav graph
 * and passed to the screens that need the logged-in user.
 */
class SessionViewModel(private val userRepository: UserRepository) : ViewModel() {

    /** null = still reading from storage, then true/false. Used for the "Logged in?" check at launch. */
    val isLoggedIn: StateFlow<Boolean?> = userRepository.currentUserId
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentUser: StateFlow<UserEntity?> = userRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun logOut(onDone: () -> Unit) {
        viewModelScope.launch {
            userRepository.logOut()
            onDone()
        }
    }
}
