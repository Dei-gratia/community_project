package com.nema.eduup.quizzes

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.repository.QuizRepository
import kotlinx.coroutines.launch

class QuizzesFragmentViewModel(app: Application): AndroidViewModel(app),
    DefaultLifecycleObserver {
    private val TAG = QuizzesFragmentViewModel::class.qualifiedName
    private lateinit var quizzesListenerRegistration: ListenerRegistration
    private var quizzes : MutableLiveData<List<Quiz>> = MutableLiveData()
    private var quiz = Quiz()
    private var quizRepository = QuizRepository



    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun getQuiz(documentReference: DocumentReference, onComplete: (Quiz?) -> Unit): Quiz {
        viewModelScope.launch {
            quizRepository.loadQuiz(documentReference){
                onComplete(it)
            }
        }
        return quiz
    }

    fun getQuizzes(collection: CollectionReference): LiveData<List<Quiz>> {
        viewModelScope.launch {
            quizzesListenerRegistration = quizRepository.addQuizzesListener(collection) {
                quizzes.value = it
            }
        }
        return quizzes
    }

    fun getLevelQuizzes(level: String): LiveData<List<Quiz>> {
        viewModelScope.launch {
            quizzesListenerRegistration = quizRepository.addLevelQuizzesListener(level) {
                quizzes.value = it
            }
        }
        return quizzes
    }

    fun getQuestions(documentReference: DocumentReference, onComplete: (ArrayList<Question>) -> Unit) {
        viewModelScope.launch {
            quizRepository.getQuestions(documentReference) {
                onComplete(it)
            }
        }
    }

    fun addQuizToFirestore(quiz: Quiz, collection: CollectionReference, onComplete: (String) -> Unit) {
        quizRepository.addQuizToFirestore(quiz,collection) {
            onComplete(it)
        }
    }

    fun deleteQuiz(documentReference: DocumentReference) {
        viewModelScope.launch {
            quizRepository.deleteDocument(documentReference)
        }
    }

    fun addQuestionToFirestore(question: Question, collection: CollectionReference, onComplete: () -> Unit) {
        viewModelScope.launch {
            quizRepository.addQuestionToFirestore(question, collection){
                onComplete()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (::quizzesListenerRegistration.isInitialized) {
            quizRepository.removeListener(quizzesListenerRegistration)
        }
    }

}