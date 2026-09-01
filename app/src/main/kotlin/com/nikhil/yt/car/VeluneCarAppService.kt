package com.nikhil.yt.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class VeluneCarAppService : CarAppService() {
    override fun onCreateSession(): Session = VeluneSession()

    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
}
