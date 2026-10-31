package io.github.ziomekmateusz.iso100

import android.app.Application

class Iso100Application : Application() {
    val container by lazy { AppContainer(this) }
}
