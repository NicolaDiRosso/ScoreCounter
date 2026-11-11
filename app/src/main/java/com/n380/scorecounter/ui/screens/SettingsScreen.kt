package com.n380.scorecounter.ui.screens

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n380.scorecounter.R
import com.n380.scorecounter.ui.components.AnimatedSelectableChip
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.viewmodel.MatchViewModel

/**
 * ====================================================================
 * SCHERMATA IMPOSTAZIONI E PREFERENZE (SettingsScreen)
 * ====================================================================
 * 🧠 ARCHITETTURA E PATTERN COMPOSE (Allineato a CreateMatchScreen):
 * Questa schermata adotta lo stesso design strutturale di [CreateMatchScreen]:
 * 1. TITOLO FISSO SUPERIORE: Intestazione pulita e fissa in cima alla schermata.
 * 2. GRANDE TAVOLO CONTENITIVO (Outer Card): Una Card principale con angoli
 *    arrotondati a 24.dp e colore `surfaceVariant` che racchiude tutte le preferenze.
 * 3. AREA SCORREVOLE INTERNA: Un layout verticale scorrevole (`verticalScroll`)
 *    che contiene tabelle/card più piccole per ogni gruppo di impostazioni.
 * 4. DOCK INFERIORE: Pulsante di chiusura ad alto impatto visivo ancorato in basso.
 * ====================================================================
 */
@Composable
fun SettingsScreen(
    viewModel: MatchViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Prevenzione del doppio click accidentale sul pulsante di chiusura
    var isClosing by remember { mutableStateOf(false) }

    // Stati di apertura per i dropdown menu
    var showThemeDropdown by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showOrientationDropdown by remember { mutableStateOf(false) }
    var showScreenKeepOnDropdown by remember { mutableStateOf(false) }

    // Stati per i dialoghi di personalizzazione passo (+) e (-)
    var showPositiveStepDialog by remember { mutableStateOf(false) }
    var showNegativeStepDialog by remember { mutableStateOf(false) }

    // Stati locali temporanei per le opzioni in attesa di persistenza avanzata
    var confirmQuickActions by remember { mutableStateOf(true) }
    var dynamicLeaderboardSorting by remember { mutableStateOf(false) }

    // Dialogo dedicato per la personalizzazione dell'incremento (+)
    if (showPositiveStepDialog) {
        PositiveStepDialog(
            initialValue = viewModel.longPressPositiveStep,
            onDismiss = { showPositiveStepDialog = false },
            onConfirm = { step -> viewModel.updateLongPressPositiveStep(step) }
        )
    }

    // Dialogo dedicato per la personalizzazione del decremento (-)
    if (showNegativeStepDialog) {
        NegativeStepDialog(
            initialValue = viewModel.longPressNegativeStep,
            onDismiss = { showNegativeStepDialog = false },
            onConfirm = { step -> viewModel.updateLongPressNegativeStep(step) }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .displayCutoutPadding(),
        containerColor = Color.Transparent,

        // ====================================================================
        // DOCK INFERIORE ANCORATO (Pulsante Chiudi)
        // ====================================================================
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = if (isLandscape) 10.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (!isClosing) {
                                isClosing = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 700.dp)
                            .height(if (isLandscape) 52.dp else 64.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.btn_chiudi_impostazioni),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                        )
                        AutoResizedText(
                            text = stringResource(R.string.btn_chiudi_impostazioni),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // ====================================================================
        // STRUTTURA PRINCIPALE (Speculare a CreateMatchScreen)
        // ====================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. IL TITOLO FISSO SUPERIORE
            Spacer(modifier = Modifier.height(16.dp))
            AutoResizedText(
                text = stringResource(R.string.titolo_impostazioni_esteso),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .padding(bottom = 16.dp)
            )

            // 2. IL GRANDE TAVOLO CONTENITIVO (La Card Unica esterna)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .weight(1f)
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                // 3. AREA SCORREVOLE INTERNA (Il tavolo scrollabile con le tabelle figlie)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ------------------------------------------------------------
                    // SEZIONE A: INTERFACCIA E ASPETTO
                    // ------------------------------------------------------------
                    SettingsSectionCard(
                        title = stringResource(R.string.sez_interfaccia_aspetto),
                        icon = Icons.Filled.Palette
                    ) {
                        // 1. TEMA APP
                        val themeLabels = listOf(
                            stringResource(R.string.opt_tema_sistema) to Icons.Filled.BrightnessAuto,
                            stringResource(R.string.opt_tema_scuro) to Icons.Filled.DarkMode,
                            stringResource(R.string.opt_tema_chiaro) to Icons.Filled.LightMode
                        )
                        val safeThemeIndex = viewModel.appTheme.coerceIn(0, themeLabels.size - 1)

                        SettingsDropdownItem(
                            title = stringResource(R.string.opt_tema_app),
                            subtitle = stringResource(R.string.desc_tema_app),
                            currentSelectionText = themeLabels[safeThemeIndex].first,
                            currentSelectionIcon = themeLabels[safeThemeIndex].second,
                            isExpanded = showThemeDropdown,
                            onExpandRequest = { showThemeDropdown = true },
                            onDismissRequest = { showThemeDropdown = false }
                        ) {
                            themeLabels.forEachIndexed { index, (label, icon) ->
                                val isSelected = safeThemeIndex == index
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        showThemeDropdown = false
                                        viewModel.setAppThemePreference(index)
                                    }
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // 2. LINGUA APP
                        val supportedLanguages = listOf(
                            Triple("it", "Italiano", "🇮🇹"),
                            Triple("en", "English", "🇬🇧"),
                            Triple("de", "Deutsch", "🇩🇪"),
                            Triple("es", "Español", "🇪🇸"),
                            Triple("fr", "Français", "🇫🇷"),
                            Triple("hi", "हिन्दी", "🇮🇳"),
                            Triple("ja", "日本語", "🇯🇵")
                        )

                        val currentSystemLang = configuration.locales[0]?.language ?: "it"
                        val effectiveLangCode = if (viewModel.appLanguage == "system") {
                            if (supportedLanguages.any { it.first == currentSystemLang }) currentSystemLang else "it"
                        } else {
                            viewModel.appLanguage
                        }

                        val currentLang = supportedLanguages.firstOrNull { it.first == effectiveLangCode }
                            ?: supportedLanguages[0]

                        SettingsDropdownItem(
                            title = stringResource(R.string.opt_lingua_app),
                            subtitle = stringResource(R.string.desc_lingua_app),
                            currentSelectionText = "${currentLang.second} ${currentLang.third}",
                            currentSelectionIcon = Icons.Filled.Language,
                            isExpanded = showLanguageDropdown,
                            onExpandRequest = { showLanguageDropdown = true },
                            onDismissRequest = { showLanguageDropdown = false }
                        ) {
                            supportedLanguages.forEach { (code, name, flag) ->
                                val isSelected = effectiveLangCode == code
                                DropdownMenuItem(
                                    leadingIcon = { Text(text = flag, fontSize = 18.sp) },
                                    text = {
                                        Text(
                                            text = name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        showLanguageDropdown = false
                                        viewModel.setAppLanguagePreference(code)
                                        val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
                                        localeManager?.applicationLocales = LocaleList.forLanguageTags(code)
                                    }
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // 3. BLOCCO ORIENTAMENTO
                        val orientationOptions = listOf(
                            stringResource(R.string.opt_orientamento_auto) to Icons.Filled.ScreenRotation,
                            stringResource(R.string.opt_orientamento_orizzontale) to Icons.Filled.StayCurrentLandscape,
                            stringResource(R.string.opt_orientamento_verticale) to Icons.Filled.StayCurrentPortrait
                        )
                        val safeOrientationIndex = viewModel.appOrientation.coerceIn(0, orientationOptions.size - 1)

                        SettingsDropdownItem(
                            title = stringResource(R.string.opt_orientamento_schermo),
                            subtitle = stringResource(R.string.desc_orientamento_schermo),
                            currentSelectionText = orientationOptions[safeOrientationIndex].first,
                            currentSelectionIcon = orientationOptions[safeOrientationIndex].second,
                            isExpanded = showOrientationDropdown,
                            onExpandRequest = { showOrientationDropdown = true },
                            onDismissRequest = { showOrientationDropdown = false }
                        ) {
                            orientationOptions.forEachIndexed { index, (label, icon) ->
                                val isSelected = safeOrientationIndex == index
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        showOrientationDropdown = false
                                        viewModel.setAppOrientationPreference(index)
                                    }
                                )
                            }
                        }
                    }

                    // ------------------------------------------------------------
                    // SEZIONE B: SCHERMO E COMPORTAMENTO
                    // ------------------------------------------------------------
                    SettingsSectionCard(
                        title = stringResource(R.string.sez_schermo_comportamento),
                        icon = Icons.Filled.WbSunny
                    ) {
                        // 1. SCHERMO SEMPRE ATTIVO
                        val screenKeepOnOptions = listOf(
                            stringResource(R.string.opt_schermo_solo_partita) to Icons.Filled.SportsEsports,
                            stringResource(R.string.opt_schermo_sempre) to Icons.Filled.Visibility,
                            stringResource(R.string.opt_schermo_disattivato) to Icons.Filled.Close
                        )
                        val safeKeepOnIndex = viewModel.keepScreenOnMode.coerceIn(0, screenKeepOnOptions.size - 1)

                        SettingsDropdownItem(
                            title = stringResource(R.string.opt_schermo_attivo),
                            subtitle = stringResource(R.string.desc_schermo_attivo),
                            currentSelectionText = screenKeepOnOptions[safeKeepOnIndex].first,
                            currentSelectionIcon = screenKeepOnOptions[safeKeepOnIndex].second,
                            isExpanded = showScreenKeepOnDropdown,
                            onExpandRequest = { showScreenKeepOnDropdown = true },
                            onDismissRequest = { showScreenKeepOnDropdown = false }
                        ) {
                            screenKeepOnOptions.forEachIndexed { index, (label, icon) ->
                                val isSelected = safeKeepOnIndex == index
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        showScreenKeepOnDropdown = false
                                        viewModel.setKeepScreenOnPreference(index)
                                    }
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // 2. CONFERMA AZIONI RAPIDE
                        SettingsSwitchItem(
                            title = stringResource(R.string.opt_conferma_azioni),
                            subtitle = stringResource(R.string.desc_conferma_azioni),
                            isChecked = confirmQuickActions,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                confirmQuickActions = checked
                            }
                        )
                    }

                    // ------------------------------------------------------------
                    // SEZIONE C: PERSONALIZZAZIONE GIOCO (Incremento / Decremento)
                    // ------------------------------------------------------------
                    SettingsSectionCard(
                        title = stringResource(R.string.sez_personalizzazione_gioco),
                        icon = Icons.Filled.SportsEsports
                    ) {
                        // 1. INCREMENTO PROLUNGATO (+)
                        SettingsClickableItem(
                            title = stringResource(R.string.opt_incremento_lungo),
                            subtitle = stringResource(R.string.desc_incremento_lungo),
                            currentSelectionText = "+${viewModel.longPressPositiveStep}",
                            currentSelectionIcon = Icons.Filled.Add,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                showPositiveStepDialog = true
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // 2. DECREMENTO PROLUNGATO (-)
                        SettingsClickableItem(
                            title = stringResource(R.string.opt_decremento_lungo),
                            subtitle = stringResource(R.string.desc_decremento_lungo),
                            currentSelectionText = "-${viewModel.longPressNegativeStep}",
                            currentSelectionIcon = Icons.Filled.Remove,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                showNegativeStepDialog = true
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // 3. ORDINAMENTO DINAMICO CLASSIFICA
                        SettingsSwitchItem(
                            title = stringResource(R.string.opt_ordinamento_classifica),
                            subtitle = stringResource(R.string.desc_ordinamento_classifica),
                            isChecked = dynamicLeaderboardSorting,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                dynamicLeaderboardSorting = checked
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ====================================================================
 * DIALOG DI PERSONALIZZAZIONE INCREMENTO (+)
 * ====================================================================
 */
@Composable
private fun PositiveStepDialog(
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var textInput by remember { mutableStateOf(initialValue.toString()) }
    val quickPresets = listOf(1, 2, 5, 10, 15, 20, 50, 100)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.opt_incremento_lungo), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.desc_dialog_passo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val rows = quickPresets.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { preset ->
                                val isSelected = textInput.toIntOrNull() == preset
                                AnimatedSelectableChip(
                                    text = "+$preset",
                                    isSelected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        textInput = preset.toString()
                                    },
                                    height = 42.dp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) textInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_valore_personalizzato)) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.btn_annulla))
                }

                Button(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        val value = textInput.toIntOrNull() ?: initialValue
                        if (value > 0) {
                            onConfirm(value)
                        }
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.btn_applica))
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * ====================================================================
 * DIALOG DI PERSONALIZZAZIONE DECREMENTO (-)
 * ====================================================================
 */
@Composable
private fun NegativeStepDialog(
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var textInput by remember { mutableStateOf(initialValue.toString()) }
    val quickPresets = listOf(1, 2, 3, 5, 10, 15, 20, 50)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.opt_decremento_lungo), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.desc_dialog_passo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val rows = quickPresets.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { preset ->
                                val isSelected = textInput.toIntOrNull() == preset
                                AnimatedSelectableChip(
                                    text = "-$preset",
                                    isSelected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        textInput = preset.toString()
                                    },
                                    height = 42.dp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) textInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_valore_personalizzato)) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.btn_annulla))
                }

                Button(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        val value = textInput.toIntOrNull() ?: initialValue
                        if (value > 0) {
                            onConfirm(value)
                        }
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.btn_applica))
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * ====================================================================
 * COMPONENTE SCHEDA SEZIONE IMPOSTAZIONI (Tabella Figlia)
 * ====================================================================
 */
@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AutoResizedText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            content()
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE VOCE IMPOSTAZIONI CLICCABILE (Per Apertura Dialog Modali)
 * ====================================================================
 */
@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    currentSelectionText: String,
    currentSelectionIcon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            AutoResizedText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AutoResizedText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            ),
            modifier = Modifier.clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = currentSelectionIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = currentSelectionText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE VOCE IMPOSTAZIONI CON TENDINA A DISCESA (Dropdown)
 * ====================================================================
 */
@Composable
private fun SettingsDropdownItem(
    title: String,
    subtitle: String,
    currentSelectionText: String,
    currentSelectionIcon: ImageVector,
    isExpanded: Boolean,
    onExpandRequest: () -> Unit,
    onDismissRequest: () -> Unit,
    dropdownContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandRequest() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            AutoResizedText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AutoResizedText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier.clickable { onExpandRequest() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = currentSelectionIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = currentSelectionText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = onDismissRequest,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                dropdownContent()
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE VOCE IMPOSTAZIONI CON INTERRUTTORE (Switch)
 * ====================================================================
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            AutoResizedText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AutoResizedText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}
