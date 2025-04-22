import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.common.data.model.User
import com.example.fundoonotes.common.database.repository.databridge.DataBridgeAuthRepository
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    // Expose repository
    val repository = DataBridgeAuthRepository(application.applicationContext)

    val authResult = MutableLiveData<Pair<Boolean, String?>>()

    // Expose this for flow-based login check in MainActivity
    val accountDetails: StateFlow<User?> = repository.accountDetails
    fun registerWithGoogle(idToken: String, userInfo: User) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        repository.registerWithGoogleCredential(credential, userInfo) { success, message ->
            authResult.postValue(Pair(success, message))
        }
    }

    fun loginWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        repository.loginWithGoogleCredential(credential, null) { success, message ->
            authResult.postValue(Pair(success, message))
        }
    }

    fun register(user: User, password: String) {
        repository.register(user, password) { success, message ->
            authResult.postValue(Pair(success, message))
        }
    }

    fun login(email: String, password: String) {
        repository.login(email, password) { success, message ->
            authResult.postValue(Pair(success, message))
        }
    }

    fun saveUserLocally(user: User, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveUserLocally(user)
            onComplete()
        }
    }

    fun getLoggedInUser(
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        repository.getLoggedInUser(onSuccess, onFailure)
    }


}
