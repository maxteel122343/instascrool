package com.example.model

enum class OverlayUiVersion(
    val code: String,
    val title: String,
    val subtitle: String
) {
    V1("V1", "Barra Inferior", "Cards horizontais e busca rápida na base"),
    V2("V2", "Sidebar Lateral", "Gaveta vertical fixada na borda"),
    V5("V5", "Bottom Sheet", "Painel inferior com redimensionamento fluido"),
    V6("V6", "Fotos Vertical", "Grade vertical limpa (sem interface do Google)"),
    V7("V7", "Fotos Horizontal", "Carrossel deslizante limpo com fotos em tela cheia");

    companion object {
        fun fromCode(code: String): OverlayUiVersion {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: V6
        }
    }
}
