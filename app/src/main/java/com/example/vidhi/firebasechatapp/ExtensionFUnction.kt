package com.example.vidhi.firebasechatapp

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import org.jetbrains.anko.sdk25.coroutines.__TextWatcher

/**
 * Created by vidhi on 6/30/2017.
 */
fun EditText.addTextWatcher(afterTextChanged: (s: Editable?) -> Unit = { _ -> } ,
                            beforeTextChanged: (s: CharSequence? , start: Int , count: Int , after: Int) -> Unit = { _ , _ , _ , _ -> } ,
                            onTextChanged: (s: CharSequence? , start: Int , before: Int , count: Int) -> Unit = { _ , _ , _ , _ -> }): TextWatcher {

    val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChanged(s)
        }

        override fun beforeTextChanged(s: CharSequence? , start: Int , count: Int , after: Int) {
            beforeTextChanged(s , start , count , after)
        }

        override fun onTextChanged(s: CharSequence? , start: Int , before: Int , count: Int) {
            onTextChanged(s , start , before , count)
        }
    }

    addTextChangedListener(textWatcher)
    return textWatcher
}