package com.stripe.android.view

import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.text.Editable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.stripe.android.R

/**
 * Extension of [TextInputEditText] that listens for users pressing the delete key when
 * there is no text present. Google has actually made this
 * [somewhat difficult](https://code.google.com/p/android/issues/detail?id=42904),
 * but we listen here for hardware key presses, older Android soft keyboard delete presses,
 * and modern Google Keyboard delete key presses.
 */
open class StripeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {

    protected var isLastKeyDelete: Boolean = false

    private var afterTextChangedListener: AfterTextChangedListener? = null
    private var deleteEmptyListener: DeleteEmptyListener? = null
    var cachedColorStateList: ColorStateList? = null
        private set

    /**
     * Gets whether or not the text should be displayed in error mode.
     *
     * Sets whether or not the text should be put into "error mode," which displays
     * the text in an error color determined by the original text color.
     */
    var shouldShowError: Boolean = false
        set(shouldShowError) = if (errorMessage != null) {
            val errorMessage = errorMessage.takeIf { shouldShowError }
            errorMessageListener?.displayErrorMessage(errorMessage)
            field = shouldShowError
        } else {
            field = shouldShowError
            if (this.shouldShowError) {
                setTextColor(errorColor ?: defaultErrorColor)
            } else {
                setTextColor(cachedColorStateList)
            }

            refreshDrawableState()
        }

    internal var errorMessage: String? = null

    protected val fieldText: String
        get() {
            return text?.toString().orEmpty()
        }

    @ColorInt
    private var defaultErrorColor: Int = 0
    @ColorInt
    private var errorColor: Int? = null

    private val hintHandler: Handler = Handler()
    private var errorMessageListener: ErrorMessageListener? = null

    /**
     * @return the color used for error text.
     */
    // It's possible that we need to verify this value again
    // in case the user programmatically changes the text color.
    val defaultErrorColorInt: Int
        @ColorInt
        get() {
            determineDefaultErrorColor()
            return defaultErrorColor
        }

    init {
        listenForTextChanges()
        listenForDeleteEmpty()
        determineDefaultErrorColor()
        cachedColorStateList = textColors
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(outAttrs)
        return inputConnection?.let {
            SoftDeleteInputConnection(it, true, deleteEmptyListener)
        }
    }

    /**
     * Sets a listener that can react to changes in text, but only by reflecting the new
     * text in the field.
     *
     * @param afterTextChangedListener the [AfterTextChangedListener] to attach to this view
     */
    fun setAfterTextChangedListener(afterTextChangedListener: AfterTextChangedListener?) {
        this.afterTextChangedListener = afterTextChangedListener
    }

    /**
     * Sets a listener that can react to the user attempting to delete the empty string.
     *
     * @param deleteEmptyListener the [DeleteEmptyListener] to attach to this view
     */
    fun setDeleteEmptyListener(deleteEmptyListener: DeleteEmptyListener?) {
        this.deleteEmptyListener = deleteEmptyListener
    }

    fun setErrorMessageListener(errorMessageListener: ErrorMessageListener?) {
        this.errorMessageListener = errorMessageListener
    }

    fun setErrorMessage(errorMessage: String?) {
        this.errorMessage = errorMessage
    }

    /**
     * Sets the error text color on this [StripeEditText].
     *
     * @param errorColor a [ColorInt]
     */
    fun setErrorColor(@ColorInt errorColor: Int) {
        this.errorColor = errorColor
    }

    /**
     * Change the hint value of this control after a delay.
     *
     * @param hintResource the string resource for the hint text
     * @param delayMilliseconds a delay period, measured in milliseconds
     */
    fun setHintDelayed(@StringRes hintResource: Int, delayMilliseconds: Long) {
        setHintDelayed(resources.getText(hintResource), delayMilliseconds)
    }

    /**
     * Change the hint value of this control after a delay.
     *
     * @param hint the hint text
     * @param delayMilliseconds a delay period, measured in milliseconds
     */
    fun setHintDelayed(hint: CharSequence, delayMilliseconds: Long) {
        hintHandler.postDelayed({
            setHintSafely(hint)
        }, delayMilliseconds)
    }

    /**
     * Call setHint() and guard against NPE. This is a workaround for a
     * [known issue on Samsung devices](https://issuetracker.google.com/issues/37127697).
     */
    private fun setHintSafely(hint: CharSequence) {
        try {
            setHint(hint)
        } catch (e: NullPointerException) {
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.isContentInvalid = shouldShowError
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Passing a null token removes all callbacks and messages to the handler.
        hintHandler.removeCallbacksAndMessages(null)
    }

    private fun determineDefaultErrorColor() {
        cachedColorStateList = textColors
        defaultErrorColor = ContextCompat.getColor(
            context,
            if (StripeColorUtils.isColorDark(textColors.defaultColor)) {
                // Note: if the _text_ color is dark, then this is a
                // light theme, and vice-versa.
                R.color.stripe_error_text_light_theme
            } else {
                R.color.stripe_error_text_dark_theme
            }
        )
    }

    private fun listenForTextChanges() {
        addTextChangedListener(object : StripeTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                afterTextChangedListener?.onTextChanged(s?.toString().orEmpty())
            }
        })
    }

    private fun listenForDeleteEmpty() {
        // This method works for hard keyboards and older phones.
        setOnKeyListener { _, keyCode, event ->
            isLastKeyDelete = isDeleteKey(keyCode, event)
            if (isLastKeyDelete && length() == 0) {
                deleteEmptyListener?.onDeleteEmpty()
            }
            false
        }
    }

    private fun isDeleteKey(keyCode: Int, event: KeyEvent): Boolean {
        return keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN
    }

    interface DeleteEmptyListener {
        fun onDeleteEmpty()
    }

    interface AfterTextChangedListener {
        fun onTextChanged(text: String)
    }

    interface ErrorMessageListener {
        fun displayErrorMessage(message: String?)
    }

    private class SoftDeleteInputConnection constructor(
        target: InputConnection,
        mutable: Boolean,
        private val deleteEmptyListener: DeleteEmptyListener?
    ) : InputConnectionWrapper(target, mutable) {
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            // This method works on modern versions of Android with soft keyboard delete.
            if (getTextBeforeCursor(1, 0).isEmpty()) {
                deleteEmptyListener?.onDeleteEmpty()
            }
            return super.deleteSurroundingText(beforeLength, afterLength)
        }
    }
}
