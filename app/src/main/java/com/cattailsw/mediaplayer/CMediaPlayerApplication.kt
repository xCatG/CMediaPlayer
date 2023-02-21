package com.cattailsw.mediaplayer

import android.app.Application

class CMediaPlayerApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        DIGraph.provide(this)
    }
}