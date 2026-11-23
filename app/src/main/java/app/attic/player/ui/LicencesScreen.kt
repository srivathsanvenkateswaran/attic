package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortLicence
import app.attic.player.ui.comfort.ComfortLicences
import app.attic.player.ui.expressive.ExpressiveLicence
import app.attic.player.ui.expressive.ExpressiveLicences
import app.attic.player.ui.model.rememberLicenceModel
import app.attic.player.ui.model.rememberLicencesModel

/** Settings, About, Open-source licences: what Attic is built from, one row per licence. */
@Composable
fun LicencesScreen() {
    val (notices, actions) = rememberLicencesModel()
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveLicences(notices, actions)
        UiStyle.Comfort -> ComfortLicences(notices, actions)
    }
}

/** One entry's notice and licence text. */
@Composable
fun LicenceScreen(id: String) {
    val (state, back) = rememberLicenceModel(id)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveLicence(state, back)
        UiStyle.Comfort -> ComfortLicence(state, back)
    }
}
