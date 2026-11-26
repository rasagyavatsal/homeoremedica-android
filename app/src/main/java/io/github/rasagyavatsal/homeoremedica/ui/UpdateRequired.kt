package io.github.rasagyavatsal.homeoremedica.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Over the whole app, which can't be used until it is updated. */
@Composable
fun UpdateRequired(onUpdate: () -> Unit) {
    Dialog("Update HomeoRemedica", onClose = {}, dismissible = false) {
        BasicText("This version of HomeoRemedica no longer works. Update it to keep using it.", style = dialogText())
        DialogButtons({ modifier ->
            Button("Update", ButtonStyle.Primary, onUpdate, modifier, minHeight = if (LocalCompact.current) 48.dp else 36.dp)
        })
    }
}
