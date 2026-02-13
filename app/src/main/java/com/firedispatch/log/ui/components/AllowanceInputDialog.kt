package com.firedispatch.log.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AllowanceInputDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var allowanceText by remember { mutableStateOf("5000") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("1回出動あたりの支給額")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = allowanceText,
                    onValueChange = {
                        allowanceText = it
                        errorMessage = null
                    },
                    label = { Text("支給額（円）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val allowance = allowanceText.toIntOrNull()
                    if (allowance != null && allowance > 0) {
                        onConfirm(allowance)
                    } else {
                        errorMessage = "有効な金額を入力してください"
                    }
                }
            ) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
