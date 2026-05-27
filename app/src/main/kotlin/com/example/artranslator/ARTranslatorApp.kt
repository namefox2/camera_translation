package com.example.artranslator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class ARTranslatorApp : Application()
