/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.nema.eduup.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.nema.eduup.R
import com.nema.eduup.newnote.NewNoteActivity
import com.nema.eduup.roomDatabase.Reminder
import com.nema.eduup.utils.AlarmScheduler
import com.nema.eduup.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.*

class ReminderDialog : DialogFragment() {

    private val TAG = ReminderDialog::class.qualifiedName
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val foregroundHandler = Handler(Looper.getMainLooper())
    private var toolbar: Toolbar? = null
    private var textInputName: TextInputEditText? = null
    private var checkBoxAdministered: CheckBox? = null
    private var radioGroupType: RadioGroup? = null
    private var radioButtonStudy: RadioButton? = null
    private var radioButtonShare: RadioButton? = null
    private var radioButtonOther: RadioButton? = null
    private var textInputDesc: TextInputEditText? = null
    private var buttonTime: Button? = null
    private var linearLayoutDays: LinearLayout? = null
    private var textInputNote: TextInputEditText? = null
    private var fabSaveReminder: FloatingActionButton? = null

    private var reminder: Reminder? = null
    private var date: Date? = null
    private var noteId: String? = null
    private var reminderId: Int? = null
    private var noteTitle: String = ""


    companion object {

        const val TAG = "ReminderDialog"

        fun newInstance(args: Bundle?): ReminderDialog {
            val reminderDialog = ReminderDialog()
            if (args != null) {
                reminderDialog.arguments = args
            }
            return reminderDialog
        }
    }

    private val viewModel by lazy { ViewModelProvider(requireActivity())[ReminderViewModel::class.java] }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handlerThread = HandlerThread("ReminderBackground")
        handlerThread!!.start()
        backgroundHandler = Handler(handlerThread!!.looper)

        val args = arguments

        if (args != null) {
            noteId = args.getString(AppConstants.KEY_DATA)
            noteTitle = args.getString(AppConstants.NOTE_TITLE).toString()

            if (noteId != null) {
                reminderId = hashCode(noteId)
                getReminder()
            }

        }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reminder_dialog, container, false)
        init(view)
        toolbar!!.setNavigationOnClickListener(navigationListener)
        fabSaveReminder!!.setOnClickListener(saveListener)
        buttonTime!!.setOnClickListener(timeListener)

        return view
    }


    private fun init(view: View) {
        toolbar = view.findViewById(R.id.toolbarReminder)
        textInputName = view.findViewById(R.id.textInputName)
        radioGroupType = view.findViewById(R.id.radioGroupType)
        radioButtonStudy = view.findViewById(R.id.radioButtonDog)
        radioButtonShare = view.findViewById(R.id.radioButtonCat)
        radioButtonOther = view.findViewById(R.id.radioButtonOther)
        radioGroupType = view.findViewById(R.id.radioGroupType)
        textInputDesc = view.findViewById(R.id.textInputDesc)
        buttonTime = view.findViewById(R.id.buttonTime)
        textInputNote = view.findViewById(R.id.textInputNote)
        fabSaveReminder = view.findViewById(R.id.fabSaveReminder)
        checkBoxAdministered = view.findViewById(R.id.checkBoxAdministered)
    }

    private fun hashCode(string: String?): Int {
        return if (string != null) string.hashCode() * 53 else 0
    }

    private fun getReminder() {
        if (reminderId != null)
            viewModel.getReminderById(reminderId!!) {
                reminder = it ?: Reminder()
                displayReminder(reminder!!)
            }
    }


    private fun displayReminder(reminder: Reminder) {
        toolbar!!.inflateMenu(R.menu.menu_reminder)
        toolbar!!.setOnMenuItemClickListener(menuItemListener)
        toolbar!!.title = getString(R.string.edit_reminder)
        if (reminder.name.isNotBlank()) {
            textInputName!!.setText(reminder.name)
        } else {
            textInputName!!.setText(noteTitle)
        }

        checkBoxAdministered!!.isChecked = reminder.opened
        textInputDesc!!.setText(reminder.desc)
        textInputNote!!.setText(reminder.note)
        setupTypeRadioGroup(reminder)
        if (reminder.time != 0L) {
            date = Date(reminder.time)
            setTimeButtonText(date!!)
        }

    }


    private fun setupTypeRadioGroup(reminderData: Reminder) {
        if (reminderData.type === Reminder.ReminderType.Study) {
            radioButtonStudy!!.isChecked = true
        } else if (reminderData.type === Reminder.ReminderType.Share) {
            radioButtonShare!!.isChecked = true
        } else {
            radioButtonOther!!.isChecked = true
        }
    }

    private val timeListener = View.OnClickListener {
        if (reminder != null) {
            val time = Calendar.getInstance()
            time.time = Date(reminder!!.time)
            val hour = time.get(Calendar.HOUR_OF_DAY)
            val minute = time.get(Calendar.MINUTE)
            pickDateTime(hour, minute)
        } else {
            val date = Calendar.getInstance()
            val hour = date.get(Calendar.HOUR_OF_DAY)
            val minute = date.get(Calendar.MINUTE)
            pickDateTime(hour, minute)
        }
    }

    private fun pickDateTime(hour: Int, minute: Int) {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(
            requireContext(),
            DatePickerDialog.OnDateSetListener { _, year, month, day ->
                TimePickerDialog(
                    requireContext(),
                    TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                        val pickedDateTime = Calendar.getInstance()
                        pickedDateTime.set(year, month, day, hour, minute)
                        date = pickedDateTime.time
                        //setTimeButtonText(hour, minute)
                        setTimeButtonText(date!!)
                    },
                    hour,
                    minute,
                    false
                ).show()
            },
            startYear,
            startMonth,
            startDay
        ).show()
    }


    private fun setTimeButtonText(date: Date) {
        val dateFormat = SimpleDateFormat("h:mm a E dd/MMM/yyyy ", Locale.getDefault())
        buttonTime!!.text = dateFormat.format(date)
    }


    private val navigationListener = View.OnClickListener { dismiss() }

    private val menuItemListener = Toolbar.OnMenuItemClickListener { menuItem ->
        if (menuItem.itemId == R.id.action_delete_reminder) {
            backgroundHandler!!.post {
                reminder?.let {
                    viewModel.deleteReminder(it)
                    AlarmScheduler.removeAlarmsForReminder(requireContext(), it)
                }
                Toast.makeText(requireContext(), "Reminder removed", Toast.LENGTH_LONG).show()
                dismiss()
            }

            return@OnMenuItemClickListener true
        }
        false
    }

    private fun displayError(errorCode: Int) {
        foregroundHandler.post {
            val message: String
            when (errorCode) {
                AppConstants.ERROR_NO_NAME -> message = getString(R.string.name_required)
                AppConstants.ERROR_NO_DESC -> message = getString(R.string.desc_required)
                AppConstants.ERROR_NO_TIME -> message = getString(R.string.time_required)
                AppConstants.ERROR_NO_DAYS -> message = getString(R.string.days_required)
                AppConstants.ERROR_SAVE_FAILED -> message = getString(R.string.save_failed)
                AppConstants.ERROR_UPDATE_FAILED -> message = getString(R.string.update_failed)
                AppConstants.ERROR_DELETE_FAILED -> message = getString(R.string.delete_failed)
                else -> message = getString(R.string.unknown_error)
            }

            if (view != null) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkFields(name: String, desc: String, date: Date?): Boolean {
        if (name.trim().isEmpty()) {
            displayError(AppConstants.ERROR_NO_NAME)
            return false
        } else if (desc.trim().isEmpty()) {
            displayError(AppConstants.ERROR_NO_DESC)
            return false
        } else if (date == null) {
            displayError(AppConstants.ERROR_NO_DAYS)
            return false
        }
        return true
    }

    private fun scheduleAlarm(reminderData: Reminder) {
        foregroundHandler.post {
            if (reminder == Reminder()) {
                AlarmScheduler.scheduleAlarmsForReminder(requireContext(), reminderData)
            } else {
                AlarmScheduler.updateAlarmsForReminder(requireContext(), reminderData)
            }
            val formattedDate = reminderData.time.let { it1 ->
                DateUtils.getRelativeTimeSpanString(
                    it1,
                    Calendar.getInstance().timeInMillis,
                    DateUtils.MINUTE_IN_MILLIS
                )
            }
            Toast.makeText(
                context,
                "Reminder set you will get notification $formattedDate",
                Toast.LENGTH_LONG
            ).show()
            dismiss()
        }
    }


    private val saveListener = View.OnClickListener {
        // Gather all the fields
        val name = textInputName!!.text!!.toString()
        val checkedId = radioGroupType!!.checkedRadioButtonId
        val reminderType: Reminder.ReminderType
        if (checkedId == R.id.radioButtonDog) {
            reminderType = Reminder.ReminderType.Study
        } else if (checkedId == R.id.radioButtonCat) {
            reminderType = Reminder.ReminderType.Share
        } else {
            reminderType = Reminder.ReminderType.Other
        }

        val desc = textInputDesc!!.text!!.toString()
        val note = textInputNote!!.text!!.toString()

        val opened = checkBoxAdministered!!.isChecked

        if (checkFields(name, desc, date)) {
            val newReminder = date?.let { it1 ->
                reminderId?.let { it2 ->
                    Reminder(
                        it2,
                        name,
                        reminderType,
                        desc,
                        note,
                        it1.time,
                        opened
                    )
                }
            }

            backgroundHandler!!.post {
                newReminder?.let { it1 ->
                    if (reminder != Reminder()) {
                        viewModel.updateReminder(it1)
                    } else {
                        viewModel.insertReminder(it1)
                    }
                }
            }
            if (newReminder != null) {
                scheduleAlarm(newReminder)
            }
        }

    }
}