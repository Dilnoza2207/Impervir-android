package com.impervir.agent.feature.postdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.navigation.Navigator

@Composable
fun PostDetailScreen(repository: ImpervirRepository, navigator: Navigator, postId: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("PostDetailScreen") }
}
