/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.colorconverter.ui.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.chillibits.colorconverter.model.Color
import com.chillibits.colorconverter.shared.Constants
import com.chillibits.colorconverter.tools.StorageTools
import com.chillibits.colorconverter.ui.adapter.ColorsAdapter
import com.mrgames13.jimdo.colorconverter.R
import kotlinx.android.synthetic.main.activity_color_selection.*
import kotlinx.android.synthetic.main.dialog_color_rename.view.*

class ColorSelectionActivity : AppCompatActivity(), ColorsAdapter.ColorSelectionListener {

    // Tools packages
    private val st = StorageTools(this)

    // Variables as objects
    private lateinit var colors: List<Color>
    private var selectedColor: Color? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_selection)

        applyWindowInsets()

        toolbar.layoutTransition = LayoutTransition()
        toolbar.setTitle(R.string.saved_colors)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Load colors
        colors = st.loadColors()

        savedColors.layoutManager = LinearLayoutManager(this)
        savedColors.adapter = ColorsAdapter(this, colors, this)
        noItems.visibility = if (colors.isNotEmpty()) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_color_selection, menu)
        val isColorSelected = selectedColor != null
        menu?.apply {
            findItem(R.id.action_import)?.isVisible = !isColorSelected
            findItem(R.id.action_export)?.isVisible = !isColorSelected
            findItem(R.id.action_edit)?.isVisible = isColorSelected
            findItem(R.id.action_delete)?.isVisible = isColorSelected
            findItem(R.id.action_done)?.isVisible = isColorSelected
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_import -> {}
            R.id.action_export -> {}
            R.id.action_edit -> showRenameColorDialog()
            R.id.action_delete -> showDeleteColorDialog()
            R.id.action_done -> done()
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun applyWindowInsets() = window.run {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                decorView.setOnApplyWindowInsetsListener { _, insets ->
                    val systemInsets = insets.getInsets(WindowInsets.Type.systemBars())
                    toolbar?.setPadding(0, systemInsets.top, 0, 0)
                    savedColors.setPadding(0, 0, 0, systemInsets.bottom)
                    insets
                }
                setDecorFitsSystemWindows(false)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                decorView.setOnApplyWindowInsetsListener { _, insets ->
                    toolbar?.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                    savedColors.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                    insets
                }
            }
            else -> {
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = ContextCompat.getColor(context, R.color.colorPrimaryDark)
            }
        }
    }

    private fun done() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(Constants.EXTRA_COLOR, selectedColor!!.color)
        })
        finish()
    }

    private fun showRenameColorDialog() {
        // Initialize views
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_rename, container, false)
        val newName = dialogView.dialogName
        newName.setText(selectedColor?.name)

        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(dialogView)
            .setPositiveButton(R.string.rename) { _, _ ->
                val name = newName.text.toString().trim()
                if (name.isNotEmpty()) st.updateColor(selectedColor!!.id, name)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

        // Prepare views
        newName.doAfterTextChanged {s ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = s.toString().isNotEmpty()
        }
        newName.selectAll()
        newName.requestFocus()
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun showDeleteColorDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(String.format(getString(R.string.delete_m), selectedColor?.name))
            .setIcon(R.drawable.delete_forever)
            .setPositiveButton(R.string.delete) { _, _ ->
                st.removeColor(selectedColor!!.id)
                // Refresh adapters
                colors = st.loadColors()
                savedColors.adapter = ColorsAdapter(this, colors, this)
                noItems.visibility = if (colors.isNotEmpty()) View.GONE else View.VISIBLE
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun animateAppAndStatusBar(toColor: Int) {
        val animator = ViewAnimationUtils.createCircularReveal(reveal, toolbar.width / 2, toolbar.height / 2, 0f, toolbar.width / 2.0f + 50)
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) { reveal.setBackgroundColor(toColor) }
            override fun onAnimationEnd(animation: Animator) { revealBackground.setBackgroundColor(toColor) }
        })

        animator.duration = 480
        animator.start()
        reveal.visibility = View.VISIBLE
    }

    override fun onColorSelected(color: Color) {
        selectedColor = color
        invalidateOptionsMenu()
        supportActionBar?.subtitle = "${getString(R.string.selected)}: ${color.name}"
        animateAppAndStatusBar(color.color)
    }
}