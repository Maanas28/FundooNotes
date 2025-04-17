import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.UI.data.model.User
import com.example.fundoonotes.UI.data.repository.DataBridgeNotesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class AuthViewModel(context: Context) : ViewModel() {
    private val repository = DataBridgeNotesRepository(context)
    val authResult = MutableLiveData<Pair<Boolean, String?>>()

    fun registerWithGoogle(idToken: String, userInfo: User) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        repository.loginWithGoogleCredential(credential, userInfo) { success, message ->
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

    fun saveUserLocally(user: User) {
        viewModelScope.launch {
            repository.saveUserLocally(user)
        }
    }

    fun getLoggedInUser(
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        repository.getLoggedInUser(onSuccess, onFailure)
    }


}
