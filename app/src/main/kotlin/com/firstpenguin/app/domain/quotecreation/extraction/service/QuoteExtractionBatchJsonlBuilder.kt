package com.firstpenguin.app.domain.quotecreation.extraction.service

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchModelVersion
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

private val QUOTE_EXTRACTION_PROMPT_GUIDE =
    """
너는 감정 기반 문장 추천 서비스의 책별 인용구형 추천 문장 작성기다.

목표:
- 제공된 책 정보와 웹 검색으로 책의 주제, 정서, 문체, 독자가 기억하는 결을 파악한다.
- 그 책에서 나왔을 법한, 인용구처럼 기억되기 쉬운 짧은 한국어 문장을 최대 3개 작성한다.
- 실제 원문 인용구가 확인되면 원문을 우선하되, 확인이 어렵다는 이유만으로 빈 배열을 반환하지 않는다.
- 원문이 아닌 문장은 책의 핵심 정서와 주제에서 벗어나지 않게 작성한다.
- 본문 발췌로 확인된 원문을 제외하고는 웹 페이지, 책 소개, 리뷰, 홍보 문구를 그대로 베끼지 마라.
- 출력은 JSON schema만 따른다.

[파악 기준]
- 웹 검색과 aladinLink는 책의 주제, 장르, 정서, 독자가 기대하는 분위기를 이해하는 데만 사용한다.
- 확인 가능한 본문 발췌가 있으면 그 문장의 길이, 말맛, 정서적 방향을 우선 참고한다.
- 원문이 아닌 검색 결과나 페이지 문장을 그대로 content에 넣지 않는다.
- 책 제목, 저자명, 줄거리, 등장인물, 사건 설명을 문장에 넣지 않는다.

[작성 기준]
content는 아래 조건을 모두 만족해야 한다.
1. 자연스러운 한국어 문장이다.
2. 20~90자 사이의 한 문장이다.
3. 한 문장만 봐도 뜻이 전달된다.
4. 감정, 생각, 태도, 깨달음, 위로, 성찰, 용기, 관점 전환 중 하나와 연결된다.
5. 특정 줄거리, 등장인물, 장면을 몰라도 사용할 수 있다.
6. 앱 화면에서 단독으로 보여도 자연스럽다.
7. 책 소개문이 아니라, 오래 기억되는 인용구처럼 간결하고 여운이 있다.
8. 너무 흔한 자기계발 문장이나 일반 격언처럼 들리지 않는다.
9. 과하게 교훈적이거나 성공을 압박하지 않는다.

[제외 기준]
아래에 해당하면 반환하지 마라.
- 영어, 러시아어, 일본어 등 외국어 문장
- 원문 확인 없이 웹 페이지 문장을 복사한 문장
- 리뷰, 서평, 홍보, 추천사, 독자 감상, 줄거리 요약, 해설처럼 들리는 문장
- “읽어버린 책”, “대표작”, “이 책은”, “작가는”처럼 책을 평가하거나 소개하는 문장
- 책 제목, 저자명, 출처 설명이 포함된 문장
- 특정 인물명, 가족 호칭, 등장인물 관계를 알아야 이해되는 문장
- 특정 장면 설명, 단순 묘사, 사건 설명, 대사 조각
- 쉼표로 여러 절을 길게 이어 붙인 문장
- 마침표, 물음표, 느낌표 기준으로 2문장 이상인 문장
- 스포일러가 강한 문장

[형식 기준]
- content에는 인용구형 추천 문장만 넣는다.
- 앞에 번호, 책 제목, 저자명, 따옴표를 넣지 않는다.
- 가능하면 서로 다른 결의 문장 3개를 채운다.
- 책 정보가 너무 부족해 책의 고유한 결을 파악할 수 없으면 quotes를 빈 배열로 둔다.
    """.trimIndent()

@Component
class QuoteExtractionBatchJsonlBuilder(
    private val jsonMapper: JsonMapper,
) {
    fun build(books: List<Book>): String =
        books.joinToString("\n") { book ->
            jsonMapper.writeValueAsString(
                mapOf(
                    "custom_id" to "book-${book.id}",
                    "method" to "POST",
                    "url" to "/v1/responses",
                    "body" to quoteExtractionRequestBody(book),
                ),
            )
        }
}

private fun quoteExtractionRequestBody(book: Book): Map<String, Any> =
    mapOf(
        "model" to QuoteBatchModelVersion.QUOTE_EXTRACTION_V1.model,
        "reasoning" to mapOf("effort" to "low"),
        "max_tool_calls" to 1,
        "tools" to listOf(webSearchTool()),
        "tool_choice" to "auto",
        "input" to buildPrompt(book),
        "text" to
            mapOf(
                "format" to quoteExtractionSchema(book),
                "verbosity" to "low",
            ),
    )

private fun webSearchTool(): Map<String, String> =
    mapOf(
        "type" to "web_search",
        "search_context_size" to "low",
    )

private fun buildPrompt(book: Book): String =
    listOf(
        QUOTE_EXTRACTION_PROMPT_GUIDE,
        bookText(book),
    ).joinToString("\n\n")

private fun bookText(book: Book): String =
    """
    [대상 책]
    bookId: ${book.id}
    title: ${book.title}
    author: ${book.author}
    isbn13: ${book.isbn13}
    aladinLink: ${book.aladinLink}
    """.trimIndent()
