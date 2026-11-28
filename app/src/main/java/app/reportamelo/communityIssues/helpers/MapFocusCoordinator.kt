package app.reportamelo.communityIssues.helpers

import app.reportamelo.models.Coordinate
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Petición de enfoque del mapa interno sobre un reporte concreto.
 *
 * `coordinate` y `cityId` viajan solo como atajo: si quien emite ya los tiene en memoria,
 * el mapa puede animar la cámara sin esperar a la red.
 */
data class MapFocusRequest(
    val reportId: String,
    val coordinate: Coordinate? = null,
    val cityId: String? = null
)

/**
 * Canal compartido para enfocar un reporte en el tab Issues desde otro tab.
 *
 * Es un evento de un solo consumo (`replay = 0`): una rotación de pantalla recompone
 * `MapExplorerView` pero no vuelve a recibir la petición, así que la cámara no se mueve otra vez.
 * Su alcance es el proceso / la Activity, no una pantalla, igual que `DeepLinkRouter`.
 */
class MapFocusCoordinator private constructor() {
    companion object {
        val shared = MapFocusCoordinator()
    }

    private val _requests = MutableSharedFlow<MapFocusRequest>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val requests: SharedFlow<MapFocusRequest> = _requests.asSharedFlow()

    fun focus(request: MapFocusRequest) {
        _requests.tryEmit(request)
    }
}
