package com.nema.eduup.repository

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.nema.eduup.discussions.*
import com.nema.eduup.quiz.Question
import com.nema.eduup.quiz.Quiz
import com.nema.eduup.utils.AppConstants
import java.util.*
import kotlin.collections.ArrayList

object QuizRepository {


    private val TAG = QuizRepository::class.qualifiedName
    private val userRepository = UserRepository
    private val currentUserId = userRepository.getCurrentUserID()
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun deleteDocument(documentReference: DocumentReference) {
        documentReference
            .delete()

    }

    fun addQuizToFirestore(quiz: Quiz, collection: CollectionReference, onComplete: (String) -> Unit) {
        collection
            .add(quiz)
            .addOnSuccessListener {
                onComplete(it.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding quiz", e)
                return@addOnFailureListener
            }
    }

    fun addQuestionToFirestore(question: Question, collection: CollectionReference, onComplete: () -> Unit) {
        collection
            .add(question)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding question", e)
                return@addOnFailureListener
            }
    }

    fun updateQuiz(quiz: Quiz, documentReference: DocumentReference, onComplete: () -> Unit) {
        documentReference
            .set(quiz)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating quiz", e)
                return@addOnFailureListener
            }
    }

    private fun seedQuizzes(onComplete: (ArrayList<Quiz>) -> Unit) {
        val quizList = java.util.ArrayList<Quiz>()
        val collection = firestoreInstance.collection(AppConstants.QUIZZES).document(AppConstants.COLLEGE).collection(AppConstants.SUBJECT)
        var quiz =
            Quiz(
                UUID.randomUUID().toString(),
                "Quiz 1",
                "Sample Quiz",
                AppConstants.ALL_SUBJECTS,
                "College",
                10,
                15
            )
        addQuizToFirestore(quiz, collection){
            quizList.add(quiz)
        }

        quiz = Quiz(
            UUID.randomUUID().toString(),
            "General Knowledge Quiz 1",
            "Sample Quiz",
            "General Knowledge",
            "College",
            10,
            20,
        )
        addQuizToFirestore(quiz, collection){
            quizList.add(quiz)
        }

        quiz = Quiz(
            UUID.randomUUID().toString(),
            "History",
            "Sample Quiz",
            "history",
            "College",
            20,
            30,
        )
        addQuizToFirestore(quiz, collection){
            quizList.add(quiz)
        }

        quiz =
            Quiz(
                UUID.randomUUID().toString(),
                "Computers Quiz 2",
                "Sample Quiz",
                "Computers",
                "College",
                25,
                25
            )
        addQuizToFirestore(quiz, collection){
            quizList.add(quiz)
        }

        quiz = Quiz(
            UUID.randomUUID().toString(),
            "General Knowledge Quiz 2",
            "Sample Quiz",
            "General Knowledge",
            "College",
            30,
            45,
        )

        addQuizToFirestore(quiz, collection){
            quizList.add(quiz)
            onComplete(quizList)
        }
    }

    fun seedQuestions(onComplete: (ArrayList<Question>) -> Unit) {
        val questionsList = java.util.ArrayList<Question>()
        val collection = firestoreInstance.collection(AppConstants.QUIZZES).document(AppConstants.COLLEGE).collection(AppConstants.SUBJECT).document("Bn97BlJSMgR63KiYriZl").collection(AppConstants.QUESTIONS)
        var question = Question(
            UUID.randomUUID().toString(),
            "Which internet company began life as an online bookstore called 'Cadabra' ?",
            "ebay",
            "Shopify",
            "Amazon",
            "Overstock",
            3
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
        }


        question = Question(
            UUID.randomUUID().toString(),
            "Which of the following languages is used as a scripting language in the Unity 3D game engine?",
            "Java",
            "C#",
            "C++",
            "Objective-C",
            2
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
        }

        question = Question(
            UUID.randomUUID().toString(),
            "Which of these people was NOT a founder of Apple Inc?",
            "Jonathan Ive",
            "Steve Jobs",
            "Ronald Wayne",
            "Steve Wozniak",
            1
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
        }

        question = Question(
            UUID.randomUUID().toString(),
            "What does the term GPU stand for?",
            "Graphite Producing Unit",
            "Gaming Processor Unit",
            "Graphical Proprietary Unit",
            "Graphics Processing Unit",
            4
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
        }

        question = Question(
            UUID.randomUUID().toString(),
            "Moore's law originally stated that the number of transistors on a microprocessor chip would double every...",
            "Year",
            "Four Years",
            "Two Years",
            "Eight Years",
            1
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
        }

        question = Question(
            UUID.randomUUID().toString(),
            "What five letter word is the motto of the IBM Computer company?",
            "Click",
            "Logic",
            "Pixel",
            "Think",
            4
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
        }

        question = Question(
            UUID.randomUUID().toString(),
            "In programming, the ternary operator is mostly defined with what symbol(s)?",
            "??",
            "if then",
            "?:",
            "?",
            3
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
        }

        question = Question(
            UUID.randomUUID().toString(),
            "On which computer hardware device is the BIOS chip located?",
            "Motherboard",
            "Hard Disk Drive",
            "Central Processing Unit",
            "Graphics Processing Unit",
            1
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
        }

        question = Question(
            UUID.randomUUID().toString(),
            "What did the name of the Tor Anonymity Network orignially stand for?",
            "The Only Router",
            "The Orange Router",
            "The Ominous Router",
            "The Onion Router",
            4
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
        }

        question = Question(
            UUID.randomUUID().toString(),
            "What was the first Android version specifically optimized for tablets?",
            "Eclair",
            "Honeycomb",
            "Marshmellow",
            "Froyo",
            2
        )
        addQuestionToFirestore(question,collection) {
            questionsList.add(question)
            onComplete(questionsList)
        }
    }

    fun getQuestions(documentReference: DocumentReference,onComplete: (ArrayList<Question>) -> Unit) {
        documentReference.collection(AppConstants.QUESTIONS)
            .get()
            .addOnSuccessListener { result ->
                val questions = ArrayList<Question>()
                for (document in result) {
                    val question = document.toObject(Question::class.java)
                    questions.add(question)
                }
                onComplete(questions)

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting Questions", e)
                return@addOnFailureListener
            }

    }

    fun getQuizzesByIds(quizCollection: CollectionReference, quizIds: ArrayList<String>, onComplete: (ArrayList<Quiz>) -> Unit) {
        val quizTasks = quizIds.map { quizCollection.document(it).get() }
        Tasks.whenAllSuccess<DocumentSnapshot>(quizTasks)
            .addOnSuccessListener { documentList ->
                val quizList = java.util.ArrayList<Quiz>()
                for (document in documentList) {
                    quizList.add(parseQuizDocument(document))
                }
                onComplete(quizList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting notes", e)
                return@addOnFailureListener
            }
    }


    fun loadQuiz(documentReference: DocumentReference, onComplete: (Quiz?) -> Unit) {
        documentReference.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val quiz = document.toObject(Quiz::class.java)!!
                    onComplete (quiz)
                }else {
                    onComplete (null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, e.message.toString())
            }
    }


    fun addQuizzesListener(collection: CollectionReference, onListen: (MutableList<Quiz>) -> Unit): ListenerRegistration{
        val quizzes = mutableListOf<Quiz>()
        return collection
            .orderBy(AppConstants.DATE, Query.Direction.ASCENDING)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e(TAG, "Quizzes listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                if (querySnapshot!!.isEmpty) {
                    seedQuizzes {

                    }
                }
                for (document in querySnapshot.documentChanges) {
                    when (document.type) {
                        DocumentChange.Type.ADDED -> {
                            quizzes.add(0, parseQuizDocument(document.document))
                        }
                        DocumentChange.Type.REMOVED -> {
                            quizzes.remove(parseQuizDocument(document.document))
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val quiz = parseQuizDocument(document.document)
                            val quizPosition = quizzes.indexOf(quizzes.find { it.id == quiz.id })
                            quizzes[quizPosition] = quiz
                        }
                    }
                }
                onListen(quizzes)
            }
    }

    fun addLevelQuizzesListener(level: String, onListen: (MutableList<Quiz>) -> Unit): ListenerRegistration{
        val quizzes = mutableListOf<Quiz>()
        return firestoreInstance.collectionGroup("${level.lowercase()}${AppConstants.PUBLIC_QUIZZES}")
            .orderBy(AppConstants.DATE, Query.Direction.ASCENDING)
            .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.e(TAG, "Notes listener error.", firebaseFirestoreException)
                    return@addSnapshotListener
                }
                for (document in querySnapshot!!.documentChanges) {
                    when (document.type) {
                        DocumentChange.Type.ADDED -> {
                            quizzes.add(0, parseQuizDocument(document.document))
                        }
                        DocumentChange.Type.REMOVED -> {
                            quizzes.remove(parseQuizDocument(document.document))
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val quiz = parseQuizDocument(document.document)
                            val notePosition = quizzes.indexOf(quizzes.find { it.id == quiz.id })
                            quizzes[notePosition] = quiz
                        }
                    }
                }
                onListen(quizzes)

            }
    }

    fun removeListener(registration: ListenerRegistration) = registration.remove()

    private fun parseQuizDocument(document: DocumentSnapshot): Quiz {
        return Quiz(
            document.id,
            document.getString(AppConstants.TITLE).toString(),
            document.getString(AppConstants.DESCRIPTION).toString(),
            document.getString(AppConstants.SUBJECT).toString(),
            document.getString(AppConstants.LEVEL).toString(),
            document.getLong(AppConstants.TOTAL_QUESTIONS)!!.toInt(),
            document.getLong(AppConstants.DURATION)!!.toInt(),
            document.getLong(AppConstants.DATE)!!,
        )
    }
}