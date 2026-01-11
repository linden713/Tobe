package com.example.tobe.data

data class Quote(
    val text: String
)

object QuoteRepository {
    private val quotes = listOf(
        Quote("纵有疾风起，人生不言弃。"),
        Quote("活着本身，就是一种回应。"),
        Quote("世界很暗，但你还在。"),
        Quote("人生海海，山山而川。"),
        Quote("心有猛虎，细嗅蔷薇。"),
        Quote("且视他人之疑目如盏盏鬼火，大胆去走你的夜路。"),
        Quote("凡是过往，皆为序章。"),
        Quote("万物皆有裂痕，那是光照进来的地方。"),
        Quote("你来人间一趟，你要看看太阳。"),
        Quote("行到水穷处，坐看云起时。")
    )

    fun getRandomQuote(): Quote {
        return quotes.random()
    }
}
