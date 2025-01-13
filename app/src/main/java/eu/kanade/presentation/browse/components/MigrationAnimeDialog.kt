package eu.kanade.presentation.browse.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationAnimeDialog(
    onDismissRequest: () -> Unit,
    copy: Boolean,
    animeSet: Int,
    animeSkipped: Int,
    copyAnime: () -> Unit,
    migrateAnime: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    if (copy) {
                        copyAnime()
                    } else {
                        migrateAnime()
                    }
                },
            ) {
                Text(text = stringResource(if (copy) MR.strings.copy else MR.strings.migrate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        text = {
            Text(
                text = pluralStringResource(
                    if (copy) SYMR.plurals.copy_entry else SYMR.plurals.migrate_entry,
                    count = animeSet,
                    animeSet,
                    (if (animeSkipped > 0) " " + stringResource(SYMR.strings.skipping_, animeSkipped) else ""),
                ),
            )
        },
    )
}
