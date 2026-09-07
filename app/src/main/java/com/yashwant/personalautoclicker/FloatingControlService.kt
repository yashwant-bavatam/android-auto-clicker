package com.yashwant.personalautoclicker

import android.app.AlertDialog
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class FloatingControlService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var toolbar: LinearLayout
    private lateinit var toolbarParams: WindowManager.LayoutParams

    private val actions = mutableListOf<AutomationAction>()
    private val handler = Handler(Looper.getMainLooper())

    private var currentActionIndex = 0
    private var completedCycles = 0
    private var isRunning = false

    private val preferences by lazy {
        getSharedPreferences(
            "auto_clicker_profiles",
            MODE_PRIVATE
        )
    }

    sealed class AutomationAction {

        data class Tap(
            val view: TextView,
            val params: WindowManager.LayoutParams,
            var delayMs: Long
        ) : AutomationAction()

        data class Swipe(
            val startView: TextView,
            val startParams: WindowManager.LayoutParams,
            val endView: TextView,
            val endParams: WindowManager.LayoutParams,
            var durationMs: Long,
            var delayAfterMs: Long
        ) : AutomationAction()
    }

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        createToolbar()
    }

    // --------------------------------------------------
    // TOOLBAR
    // --------------------------------------------------

    private fun createToolbar() {

        toolbar = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                8,
                8,
                8,
                8
            )

            background =
                GradientDrawable().apply {

                    cornerRadius = 22f

                    setColor(
                        android.graphics.Color.argb(
                            220,
                            35,
                            35,
                            35
                        )
                    )
                }
        }

        val dragHandle =
            createSmallButton(
                "☰",
                null
            )

        val startButton =
            createSmallButton(
                "▶"
            ) {
                startSequence()
            }

        val addTapButton =
            createSmallButton(
                "+"
            ) {
                addTapTarget()
            }

        val addSwipeButton =
            createSmallButton(
                "↔"
            ) {
                addSwipe()
            }

        val removeButton =
            createSmallButton(
                "−"
            ) {
                showDeleteActionDialog()
            }

        val stopButton =
            createSmallButton(
                "■"
            ) {
                stopSequence()
            }

        val saveButton =
            createSmallButton(
                "💾"
            ) {
                showSaveProfileDialog()
            }

        val profileButton =
            createSmallButton(
                "📂"
            ) {
                showProfileManager()
            }

        val closeButton =
            createSmallButton(
                "×"
            ) {
                stopSequence()
                stopSelf()
            }

        toolbar.addView(dragHandle)
        toolbar.addView(startButton)
        toolbar.addView(addTapButton)
        toolbar.addView(addSwipeButton)
        toolbar.addView(removeButton)
        toolbar.addView(stopButton)
        toolbar.addView(saveButton)
        toolbar.addView(profileButton)
        toolbar.addView(closeButton)

        toolbarParams =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

        toolbarParams.gravity =
            Gravity.TOP or Gravity.START

        toolbarParams.x = 20
        toolbarParams.y = 250

        windowManager.addView(
            toolbar,
            toolbarParams
        )

        makeDragHandleDraggable(
            dragHandle
        )
    }

    private fun createSmallButton(
        textValue: String,
        action: (() -> Unit)?
    ): TextView {

        return TextView(this).apply {

            text = textValue
            textSize = 20f
            gravity = Gravity.CENTER

            setTextColor(
                android.graphics.Color.WHITE
            )

            setPadding(
                10,
                7,
                10,
                7
            )

            minWidth = 68
            minHeight = 68

            background =
                GradientDrawable().apply {

                    cornerRadius = 18f

                    setColor(
                        android.graphics.Color.rgb(
                            70,
                            70,
                            70
                        )
                    )

                    setStroke(
                        2,
                        android.graphics.Color.LTGRAY
                    )
                }

            if (action != null) {

                setOnClickListener {
                    action()
                }
            }
        }
    }

    private fun makeDragHandleDraggable(
        dragHandle: View
    ) {

        dragHandle.setOnTouchListener(
            object : View.OnTouchListener {

                private var initialX = 0
                private var initialY = 0

                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(
                    view: View?,
                    event: MotionEvent
                ): Boolean {

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            initialX =
                                toolbarParams.x

                            initialY =
                                toolbarParams.y

                            initialTouchX =
                                event.rawX

                            initialTouchY =
                                event.rawY

                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {

                            toolbarParams.x =
                                initialX +
                                        (
                                                event.rawX -
                                                        initialTouchX
                                                ).toInt()

                            toolbarParams.y =
                                initialY +
                                        (
                                                event.rawY -
                                                        initialTouchY
                                                ).toInt()

                            windowManager.updateViewLayout(
                                toolbar,
                                toolbarParams
                            )

                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            return true
                        }
                    }

                    return false
                }
            }
        )
    }

    // --------------------------------------------------
    // TAP TARGET
    // --------------------------------------------------

    private fun addTapTarget() {

        if (isRunning) {
            return
        }

        val actionNumber =
            actions.size + 1

        createTapTarget(
            actionNumber = actionNumber,
            x = 250 + (actions.size * 100),
            y = 500,
            delayMs =
                AutoClickerConfig.intervalMs
                    .coerceAtLeast(100L)
        )
    }

    private fun createTapTarget(
        actionNumber: Int,
        x: Int,
        y: Int,
        delayMs: Long
    ) {

        val targetView =
            TextView(this).apply {

                text =
                    actionNumber.toString()

                textSize = 18f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    android.graphics.Color.WHITE
                )

                background =
                    GradientDrawable().apply {

                        shape =
                            GradientDrawable.OVAL

                        setColor(
                            android.graphics.Color.rgb(
                                40,
                                100,
                                220
                            )
                        )

                        setStroke(
                            4,
                            android.graphics.Color.WHITE
                        )
                    }
            }

        val size = 105

        val params =
            createOverlayParams(
                size,
                size,
                x,
                y
            )

        windowManager.addView(
            targetView,
            params
        )

        val action =
            AutomationAction.Tap(
                view = targetView,
                params = params,
                delayMs =
                    delayMs.coerceAtLeast(100L)
            )

        actions.add(
            action
        )

        makeTapDraggable(
            action
        )
    }

    private fun makeTapDraggable(
        target: AutomationAction.Tap
    ) {

        target.view.setOnTouchListener(
            object : View.OnTouchListener {

                private var initialX = 0
                private var initialY = 0

                private var initialTouchX = 0f
                private var initialTouchY = 0f

                private var downTime = 0L
                private var moved = false

                override fun onTouch(
                    view: View?,
                    event: MotionEvent
                ): Boolean {

                    if (isRunning) {
                        return true
                    }

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            initialX =
                                target.params.x

                            initialY =
                                target.params.y

                            initialTouchX =
                                event.rawX

                            initialTouchY =
                                event.rawY

                            downTime =
                                System.currentTimeMillis()

                            moved = false

                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {

                            val dx =
                                event.rawX -
                                        initialTouchX

                            val dy =
                                event.rawY -
                                        initialTouchY

                            if (
                                abs(dx) > 10 ||
                                abs(dy) > 10
                            ) {
                                moved = true
                            }

                            target.params.x =
                                initialX +
                                        dx.toInt()

                            target.params.y =
                                initialY +
                                        dy.toInt()

                            windowManager.updateViewLayout(
                                target.view,
                                target.params
                            )

                            return true
                        }

                        MotionEvent.ACTION_UP -> {

                            val duration =
                                System.currentTimeMillis() -
                                        downTime

                            if (
                                !moved &&
                                duration >= 600L
                            ) {

                                showTapDelayDialog(
                                    target
                                )
                            }

                            return true
                        }
                    }

                    return false
                }
            }
        )
    }

    private fun showTapDelayDialog(
        target: AutomationAction.Tap
    ) {

        val input =
            EditText(this).apply {

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                setText(
                    target.delayMs.toString()
                )

                selectAll()
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Action ${target.view.text} - Tap Delay"
                )
                .setMessage(
                    "Delay after this tap.\n1000 ms = 1 second"
                )
                .setView(
                    input
                )
                .setPositiveButton(
                    "SAVE"
                ) { _, _ ->

                    val value =
                        input.text
                            .toString()
                            .toLongOrNull()
                            ?.coerceAtLeast(100L)

                    if (value != null) {

                        target.delayMs =
                            value
                    }
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    // --------------------------------------------------
    // SWIPE
    // --------------------------------------------------

    private fun addSwipe() {

        if (isRunning) {
            return
        }

        val actionNumber =
            actions.size + 1

        createSwipe(
            actionNumber = actionNumber,
            startX = 400,
            startY = 800,
            endX = 400,
            endY = 400,
            durationMs = 500L,
            delayAfterMs = 500L
        )
    }

    private fun createSwipe(
        actionNumber: Int,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMs: Long,
        delayAfterMs: Long
    ) {

        val startView =
            createSwipeMarker(
                "${actionNumber}S",
                android.graphics.Color.rgb(
                    0,
                    170,
                    90
                )
            )

        val endView =
            createSwipeMarker(
                "${actionNumber}E",
                android.graphics.Color.rgb(
                    220,
                    80,
                    60
                )
            )

        val size = 105

        val startParams =
            createOverlayParams(
                size,
                size,
                startX,
                startY
            )

        val endParams =
            createOverlayParams(
                size,
                size,
                endX,
                endY
            )

        windowManager.addView(
            startView,
            startParams
        )

        windowManager.addView(
            endView,
            endParams
        )

        val action =
            AutomationAction.Swipe(
                startView = startView,
                startParams = startParams,
                endView = endView,
                endParams = endParams,
                durationMs =
                    durationMs.coerceAtLeast(100L),
                delayAfterMs =
                    delayAfterMs.coerceAtLeast(100L)
            )

        actions.add(
            action
        )

        makeSwipeMarkerDraggable(
            startView,
            startParams,
            action
        )

        makeSwipeMarkerDraggable(
            endView,
            endParams,
            action
        )
    }

    private fun createSwipeMarker(
        textValue: String,
        color: Int
    ): TextView {

        return TextView(this).apply {

            text = textValue
            textSize = 16f
            gravity = Gravity.CENTER

            setTextColor(
                android.graphics.Color.WHITE
            )

            background =
                GradientDrawable().apply {

                    shape =
                        GradientDrawable.OVAL

                    setColor(
                        color
                    )

                    setStroke(
                        4,
                        android.graphics.Color.WHITE
                    )
                }
        }
    }

    private fun makeSwipeMarkerDraggable(
        view: TextView,
        params: WindowManager.LayoutParams,
        swipe: AutomationAction.Swipe
    ) {

        view.setOnTouchListener(
            object : View.OnTouchListener {

                private var initialX = 0
                private var initialY = 0

                private var initialTouchX = 0f
                private var initialTouchY = 0f

                private var downTime = 0L
                private var moved = false

                override fun onTouch(
                    v: View?,
                    event: MotionEvent
                ): Boolean {

                    if (isRunning) {
                        return true
                    }

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {

                            initialX =
                                params.x

                            initialY =
                                params.y

                            initialTouchX =
                                event.rawX

                            initialTouchY =
                                event.rawY

                            downTime =
                                System.currentTimeMillis()

                            moved = false

                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {

                            val dx =
                                event.rawX -
                                        initialTouchX

                            val dy =
                                event.rawY -
                                        initialTouchY

                            if (
                                abs(dx) > 10 ||
                                abs(dy) > 10
                            ) {
                                moved = true
                            }

                            params.x =
                                initialX +
                                        dx.toInt()

                            params.y =
                                initialY +
                                        dy.toInt()

                            windowManager.updateViewLayout(
                                view,
                                params
                            )

                            return true
                        }

                        MotionEvent.ACTION_UP -> {

                            val duration =
                                System.currentTimeMillis() -
                                        downTime

                            if (
                                !moved &&
                                duration >= 600L
                            ) {

                                showSwipeSettingsDialog(
                                    swipe
                                )
                            }

                            return true
                        }
                    }

                    return false
                }
            }
        )
    }

    private fun showSwipeSettingsDialog(
        swipe: AutomationAction.Swipe
    ) {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    30,
                    10,
                    30,
                    10
                )
            }

        val durationInput =
            EditText(this).apply {

                hint =
                    "Swipe duration ms"

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                setText(
                    swipe.durationMs.toString()
                )
            }

        val delayInput =
            EditText(this).apply {

                hint =
                    "Delay after swipe ms"

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                setText(
                    swipe.delayAfterMs.toString()
                )
            }

        container.addView(
            durationInput
        )

        container.addView(
            delayInput
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Swipe Settings"
                )
                .setMessage(
                    "${swipe.startView.text} = start\n${swipe.endView.text} = end"
                )
                .setView(
                    container
                )
                .setPositiveButton(
                    "SAVE"
                ) { _, _ ->

                    swipe.durationMs =
                        durationInput.text
                            .toString()
                            .toLongOrNull()
                            ?.coerceAtLeast(100L)
                            ?: 500L

                    swipe.delayAfterMs =
                        delayInput.text
                            .toString()
                            .toLongOrNull()
                            ?.coerceAtLeast(100L)
                            ?: 500L
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    // --------------------------------------------------
    // REMOVE + RENUMBER
    // --------------------------------------------------

    private fun showDeleteActionDialog() {

        if (isRunning) {

            toast(
                "Stop the auto clicker before deleting an action."
            )

            return
        }

        if (actions.isEmpty()) {

            toast(
                "No actions to delete."
            )

            return
        }

        val actionNames =
            actions.mapIndexed { index, action ->

                val number =
                    index + 1

                when (action) {

                    is AutomationAction.Tap ->
                        "Action $number - Tap"

                    is AutomationAction.Swipe ->
                        "Action $number - Swipe"
                }

            }.toTypedArray()

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Delete Action"
                )
                .setItems(
                    actionNames
                ) { _, which ->

                    showDeleteActionConfirmation(
                        which
                    )
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    private fun showDeleteActionConfirmation(
        index: Int
    ) {

        if (
            index < 0 ||
            index >= actions.size
        ) {
            return
        }

        val action =
            actions[index]

        val actionName =
            when (action) {

                is AutomationAction.Tap ->
                    "Tap"

                is AutomationAction.Swipe ->
                    "Swipe"
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Delete Action ${index + 1}?"
                )
                .setMessage(
                    "Delete this $actionName action?"
                )
                .setPositiveButton(
                    "DELETE"
                ) { _, _ ->

                    deleteActionAt(
                        index
                    )
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    private fun deleteActionAt(
        index: Int
    ) {

        if (
            index < 0 ||
            index >= actions.size
        ) {
            return
        }

        val action =
            actions.removeAt(
                index
            )

        removeActionViews(
            action
        )

        renumberActions()

        toast(
            "Action deleted."
        )
    }

    private fun renumberActions() {

        actions.forEachIndexed {
                index,
                action ->

            val number =
                index + 1

            when (action) {

                is AutomationAction.Tap -> {

                    action.view.text =
                        number.toString()
                }

                is AutomationAction.Swipe -> {

                    action.startView.text =
                        "${number}S"

                    action.endView.text =
                        "${number}E"
                }
            }
        }
    }

    private fun removeActionViews(
        action: AutomationAction
    ) {

        try {

            when (action) {

                is AutomationAction.Tap -> {

                    windowManager.removeView(
                        action.view
                    )
                }

                is AutomationAction.Swipe -> {

                    windowManager.removeView(
                        action.startView
                    )

                    windowManager.removeView(
                        action.endView
                    )
                }
            }

        } catch (_: Exception) {
        }
    }

    // --------------------------------------------------
    // SAVE PROFILE
    // --------------------------------------------------

    private fun showSaveProfileDialog() {

        if (isRunning) {

            toast(
                "Stop the auto clicker before saving."
            )

            return
        }

        if (actions.isEmpty()) {

            toast(
                "Add at least one action."
            )

            return
        }

        val input =
            EditText(this).apply {

                hint =
                    "Profile name"

                inputType =
                    InputType.TYPE_CLASS_TEXT
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Save Profile"
                )
                .setView(
                    input
                )
                .setPositiveButton(
                    "SAVE"
                ) { _, _ ->

                    val name =
                        input.text
                            .toString()
                            .trim()

                    if (
                        name.isEmpty()
                    ) {
                        return@setPositiveButton
                    }

                    if (
                        profileExists(
                            name
                        )
                    ) {

                        showOverwriteConfirmation(
                            name
                        )

                    } else {

                        saveProfile(
                            name
                        )
                    }
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    private fun saveProfile(
        profileName: String
    ) {

        try {

            val profile =
                JSONObject()

            profile.put(
                "version",
                3
            )

            profile.put(
                "intervalMs",
                AutoClickerConfig.intervalMs
            )

            profile.put(
                "continuousMode",
                AutoClickerConfig.continuousMode
            )

            profile.put(
                "repeatCount",
                AutoClickerConfig.repeatCount
            )

            val actionArray =
                JSONArray()

            actions.forEach { action ->

                when (action) {

                    is AutomationAction.Tap -> {

                        val item =
                            JSONObject()

                        item.put(
                            "type",
                            "tap"
                        )

                        item.put(
                            "x",
                            action.params.x
                        )

                        item.put(
                            "y",
                            action.params.y
                        )

                        item.put(
                            "delayMs",
                            action.delayMs
                        )

                        actionArray.put(
                            item
                        )
                    }

                    is AutomationAction.Swipe -> {

                        val item =
                            JSONObject()

                        item.put(
                            "type",
                            "swipe"
                        )

                        item.put(
                            "startX",
                            action.startParams.x
                        )

                        item.put(
                            "startY",
                            action.startParams.y
                        )

                        item.put(
                            "endX",
                            action.endParams.x
                        )

                        item.put(
                            "endY",
                            action.endParams.y
                        )

                        item.put(
                            "durationMs",
                            action.durationMs
                        )

                        item.put(
                            "delayAfterMs",
                            action.delayAfterMs
                        )

                        actionArray.put(
                            item
                        )
                    }
                }
            }

            profile.put(
                "actions",
                actionArray
            )

            preferences.edit()
                .putString(
                    "profile_$profileName",
                    profile.toString()
                )
                .apply()

            val names =
                getProfileNames()
                    .toMutableSet()

            names.add(
                profileName
            )

            preferences.edit()
                .putStringSet(
                    "profile_names",
                    names
                )
                .apply()

            toast(
                "Profile \"$profileName\" saved."
            )

        } catch (_: Exception) {

            toast(
                "Could not save profile."
            )
        }
    }

    private fun showOverwriteConfirmation(
        profileName: String
    ) {

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Overwrite Profile?"
                )
                .setMessage(
                    "\"$profileName\" already exists."
                )
                .setPositiveButton(
                    "OVERWRITE"
                ) { _, _ ->

                    saveProfile(
                        profileName
                    )
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    // --------------------------------------------------
    // PROFILE MANAGER
    // --------------------------------------------------

    private fun showProfileManager() {

        if (isRunning) {

            toast(
                "Stop the auto clicker first."
            )

            return
        }

        val names =
            getProfileNames()

        if (names.isEmpty()) {

            toast(
                "No saved profiles."
            )

            return
        }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Saved Profiles"
                )
                .setItems(
                    names.toTypedArray()
                ) { _, which ->

                    showProfileActions(
                        names[which]
                    )
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    private fun showProfileActions(
        profileName: String
    ) {

        val items =
            arrayOf(
                "Load",
                "Rename",
                "Delete"
            )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    profileName
                )
                .setItems(
                    items
                ) { _, which ->

                    when (which) {

                        0 ->
                            loadProfile(
                                profileName
                            )

                        1 ->
                            showRenameDialog(
                                profileName
                            )

                        2 ->
                            showDeleteConfirmation(
                                profileName
                            )
                    }
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    private fun loadProfile(
        profileName: String
    ) {

        try {

            val raw =
                preferences.getString(
                    "profile_$profileName",
                    null
                ) ?: return

            val profile =
                JSONObject(
                    raw
                )

            clearAllActions()

            AutoClickerConfig.intervalMs =
                profile.optLong(
                    "intervalMs",
                    1000L
                )

            AutoClickerConfig.continuousMode =
                profile.optBoolean(
                    "continuousMode",
                    true
                )

            AutoClickerConfig.repeatCount =
                profile.optInt(
                    "repeatCount",
                    1
                )

            if (
                profile.has(
                    "actions"
                )
            ) {

                val savedActions =
                    profile.getJSONArray(
                        "actions"
                    )

                for (
                index in 0 until
                        savedActions.length()
                ) {

                    val item =
                        savedActions.getJSONObject(
                            index
                        )

                    val actionNumber =
                        index + 1

                    when (
                        item.getString(
                            "type"
                        )
                    ) {

                        "tap" -> {

                            createTapTarget(
                                actionNumber =
                                    actionNumber,

                                x =
                                    item.getInt(
                                        "x"
                                    ),

                                y =
                                    item.getInt(
                                        "y"
                                    ),

                                delayMs =
                                    item.getLong(
                                        "delayMs"
                                    )
                            )
                        }

                        "swipe" -> {

                            createSwipe(
                                actionNumber =
                                    actionNumber,

                                startX =
                                    item.getInt(
                                        "startX"
                                    ),

                                startY =
                                    item.getInt(
                                        "startY"
                                    ),

                                endX =
                                    item.getInt(
                                        "endX"
                                    ),

                                endY =
                                    item.getInt(
                                        "endY"
                                    ),

                                durationMs =
                                    item.getLong(
                                        "durationMs"
                                    ),

                                delayAfterMs =
                                    item.getLong(
                                        "delayAfterMs"
                                    )
                            )
                        }
                    }
                }

            } else if (
                profile.has(
                    "targets"
                )
            ) {

                val oldTargets =
                    profile.getJSONArray(
                        "targets"
                    )

                for (
                index in 0 until
                        oldTargets.length()
                ) {

                    val item =
                        oldTargets.getJSONObject(
                            index
                        )

                    createTapTarget(
                        actionNumber =
                            index + 1,

                        x =
                            item.getInt(
                                "x"
                            ),

                        y =
                            item.getInt(
                                "y"
                            ),

                        delayMs =
                            item.getLong(
                                "delayMs"
                            )
                    )
                }
            }

            renumberActions()

            toast(
                "Profile \"$profileName\" loaded."
            )

        } catch (_: Exception) {

            toast(
                "Could not load profile."
            )
        }
    }

    private fun showRenameDialog(
        oldName: String
    ) {

        val input =
            EditText(this).apply {

                inputType =
                    InputType.TYPE_CLASS_TEXT

                setText(
                    oldName
                )

                selectAll()
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Rename Profile"
                )
                .setView(
                    input
                )
                .setPositiveButton(
                    "RENAME"
                ) { _, _ ->

                    val newName =
                        input.text
                            .toString()
                            .trim()

                    if (
                        newName.isNotEmpty() &&
                        newName != oldName
                    ) {

                        if (
                            profileExists(
                                newName
                            )
                        ) {

                            toast(
                                "That profile name already exists."
                            )

                        } else {

                            renameProfile(
                                oldName,
                                newName
                            )
                        }
                    }
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    private fun renameProfile(
        oldName: String,
        newName: String
    ) {

        val oldKey =
            "profile_$oldName"

        val raw =
            preferences.getString(
                oldKey,
                null
            ) ?: return

        val names =
            getProfileNames()
                .toMutableSet()

        names.remove(
            oldName
        )

        names.add(
            newName
        )

        preferences.edit()
            .putString(
                "profile_$newName",
                raw
            )
            .remove(
                oldKey
            )
            .putStringSet(
                "profile_names",
                names
            )
            .apply()

        toast(
            "\"$oldName\" renamed to \"$newName\"."
        )
    }

    private fun showDeleteConfirmation(
        profileName: String
    ) {

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Delete Profile?"
                )
                .setMessage(
                    "Delete \"$profileName\" permanently?"
                )
                .setPositiveButton(
                    "DELETE"
                ) { _, _ ->

                    deleteProfile(
                        profileName
                    )
                }
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    private fun deleteProfile(
        profileName: String
    ) {

        val names =
            getProfileNames()
                .toMutableSet()

        names.remove(
            profileName
        )

        preferences.edit()
            .remove(
                "profile_$profileName"
            )
            .putStringSet(
                "profile_names",
                names
            )
            .apply()

        toast(
            "Profile \"$profileName\" deleted."
        )
    }

    private fun profileExists(
        profileName: String
    ): Boolean {

        return preferences.contains(
            "profile_$profileName"
        )
    }

    private fun getProfileNames():
            List<String> {

        return preferences
            .getStringSet(
                "profile_names",
                emptySet()
            )
            ?.toList()
            ?.sorted()
            ?: emptyList()
    }

    // --------------------------------------------------
    // SEQUENCE ENGINE
    // --------------------------------------------------

    private fun startSequence() {

        if (
            actions.isEmpty() ||
            AutoClickerService.instance == null
        ) {
            return
        }

        stopSequence()

        isRunning = true

        currentActionIndex = 0

        completedCycles = 0

        makeActionsPassThrough()

        handler.postDelayed(
            {
                runCurrentAction()
            },
            200L
        )
    }

    private fun runCurrentAction() {

        if (
            !isRunning ||
            actions.isEmpty()
        ) {
            return
        }

        val action =
            actions[
                currentActionIndex
            ]

        when (action) {

            is AutomationAction.Tap -> {

                val location =
                    IntArray(2)

                action.view.getLocationOnScreen(
                    location
                )

                val x =
                    location[0] +
                            action.view.width /
                            2f

                val y =
                    location[1] +
                            action.view.height /
                            2f

                AutoClickerService.instance
                    ?.performTap(
                        x,
                        y
                    ) {

                        moveToNextAction(
                            action.delayMs
                        )
                    }
            }

            is AutomationAction.Swipe -> {

                val startLocation =
                    IntArray(2)

                val endLocation =
                    IntArray(2)

                action.startView.getLocationOnScreen(
                    startLocation
                )

                action.endView.getLocationOnScreen(
                    endLocation
                )

                val startX =
                    startLocation[0] +
                            action.startView.width /
                            2f

                val startY =
                    startLocation[1] +
                            action.startView.height /
                            2f

                val endX =
                    endLocation[0] +
                            action.endView.width /
                            2f

                val endY =
                    endLocation[1] +
                            action.endView.height /
                            2f

                AutoClickerService.instance
                    ?.performSwipe(
                        startX,
                        startY,
                        endX,
                        endY,
                        action.durationMs
                    ) {

                        moveToNextAction(
                            action.delayAfterMs
                        )
                    }
            }
        }
    }

    private fun moveToNextAction(
        delayMs: Long
    ) {

        if (!isRunning) {
            return
        }

        currentActionIndex++

        if (
            currentActionIndex >=
            actions.size
        ) {

            currentActionIndex = 0

            completedCycles++

            if (
                !AutoClickerConfig.continuousMode &&
                completedCycles >=
                AutoClickerConfig.repeatCount
            ) {

                stopSequence()

                return
            }
        }

        handler.postDelayed(
            {
                runCurrentAction()
            },
            delayMs.coerceAtLeast(100L)
        )
    }

    private fun stopSequence() {

        isRunning = false

        handler.removeCallbacksAndMessages(
            null
        )

        makeActionsTouchable()
    }

    // --------------------------------------------------
    // OVERLAY HELPERS
    // --------------------------------------------------

    private fun makeActionsPassThrough() {

        actions.forEach { action ->

            when (action) {

                is AutomationAction.Tap -> {

                    action.params.flags =
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

                    windowManager.updateViewLayout(
                        action.view,
                        action.params
                    )
                }

                is AutomationAction.Swipe -> {

                    action.startParams.flags =
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

                    action.endParams.flags =
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

                    windowManager.updateViewLayout(
                        action.startView,
                        action.startParams
                    )

                    windowManager.updateViewLayout(
                        action.endView,
                        action.endParams
                    )
                }
            }
        }
    }

    private fun makeActionsTouchable() {

        actions.forEach { action ->

            when (action) {

                is AutomationAction.Tap -> {

                    action.params.flags =
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

                    windowManager.updateViewLayout(
                        action.view,
                        action.params
                    )
                }

                is AutomationAction.Swipe -> {

                    action.startParams.flags =
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

                    action.endParams.flags =
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

                    windowManager.updateViewLayout(
                        action.startView,
                        action.startParams
                    )

                    windowManager.updateViewLayout(
                        action.endView,
                        action.endParams
                    )
                }
            }
        }
    }

    private fun createOverlayParams(
        width: Int,
        height: Int,
        x: Int,
        y: Int
    ): WindowManager.LayoutParams {

        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {

            gravity =
                Gravity.TOP or Gravity.START

            this.x = x
            this.y = y
        }
    }

    private fun clearAllActions() {

        stopSequence()

        actions.forEach { action ->

            removeActionViews(
                action
            )
        }

        actions.clear()
    }

    private fun toast(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {

        stopSequence()

        actions.forEach { action ->

            removeActionViews(
                action
            )
        }

        actions.clear()

        if (
            ::toolbar.isInitialized
        ) {

            try {

                windowManager.removeView(
                    toolbar
                )

            } catch (_: Exception) {
            }
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}
