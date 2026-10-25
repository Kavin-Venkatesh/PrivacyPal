package com.privacypal.app.ui.watchdog

import androidx.lifecycle.ViewModel
import com.privacypal.app.data.watchdog.UsageAccessChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class UsageAccessViewModel @Inject constructor(private val checker: UsageAccessChecker) : ViewModel() {
    private val _granted = MutableStateFlow(checker.isGranted())
    val granted: StateFlow<Boolean> = _granted.asStateFlow()

    /** Usage access is granted in system Settings, so check again whenever the screen resumes. */
    fun refresh() {
        _granted.value = checker.isGranted()
    }
}
